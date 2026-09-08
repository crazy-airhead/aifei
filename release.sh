#!/usr/bin/env bash
#
# release.sh —— aifei 一键发布到 Maven Central（Central Portal 流程）
#
# 流程: 前置检查 → JDK 8 构建/签名/上传 → 等待校验(VALIDATED) → 确认 → 发布 → 等待 PUBLISHED
#
# 用法:
#   ./release.sh                  交互式完整发布（正式发布前会要求确认）
#   ./release.sh --yes            跳过确认（CI / 非交互环境；发布不可逆，请慎用）
#   ./release.sh status <id>      查询指定 deployment 状态
#   ./release.sh drop <id>        丢弃一个 VALIDATED/FAILED 的 deployment
#
# 密钥安全（本脚本可安全提交到仓库，不含任何密钥）:
#   - Central Token:   运行时从 ~/.m2/settings.xml 的 <server id="aifei"> 读取，
#                      且仅写入 600 权限的临时文件，经 curl -H @文件 传递，不进命令行/不回显
#   - GPG 口令:        无需本脚本处理，maven-gpg-plugin 自动读取 settings.xml 里
#                      <server id="gpg.passphrase"> 的口令
#   - 注意: 若在 Portal 重新生成过 Token，记得同步更新 ~/.m2/settings.xml
#
set -euo pipefail

# ---------------- 可按需调整的公共信息（非密钥） ----------------
GPG_KEY="${GPG_KEY:-051ECC562CC746FA72B42AF75DCE24021229C603}"  # 签名密钥指纹（公开信息）
SERVER_ID="aifei"                        # settings.xml 中 Central 凭据的 server id
SETTINGS_XML="${HOME}/.m2/settings.xml"
API_BASE="https://central.sonatype.com/api/v1/publisher"
KEY_SERVER="https://keyserver.ubuntu.com"
REPO1_BASE="https://repo1.maven.org/maven2"
GROUP_PATH="io/github/crazy-airhead"
VALIDATE_TIMEOUT_MIN=10                  # 等待校验的最长分钟数
PUBLISH_TIMEOUT_MIN=30                   # 等待同步到中央仓库的最长分钟数

ASSUME_YES=0
HDR_FILE=""
MVN_LOG=""

die()  { printf '\n❌ %s\n' "$*" >&2; exit 1; }
info() { printf '==> %s\n' "$*"; }

cleanup() { [ -n "$HDR_FILE" ] && rm -f "$HDR_FILE"; }
trap cleanup EXIT

# ---------------- 从 settings.xml 提取 Bearer Token（base64(user:pass)） ----------------
load_token() {
    [ -f "$SETTINGS_XML" ] || die "未找到 $SETTINGS_XML，请先配置 <server id=\"$SERVER_ID\">"
    python3 - "$SETTINGS_XML" "$SERVER_ID" <<'PY'
import sys, base64
import xml.etree.ElementTree as ET

path, server_id = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()
for server in root.findall(".//{*}server"):
    if server.findtext("{*}id") == server_id:
        user = server.findtext("{*}username")
        pwd = server.findtext("{*}password")
        if user and pwd:
            print(base64.b64encode(f"{user}:{pwd}".encode()).decode())
            sys.exit(0)
        sys.exit(f"server id={server_id} 缺少 username/password")
sys.exit(f"settings.xml 中未找到 server id={server_id}")
PY
}

# Token 写入 600 权限临时文件，后续 curl 一律 -H @"$HDR_FILE"
init_auth_header() {
    local token
    token=$(load_token) || die "读取 Central Token 失败"
    HDR_FILE=$(mktemp "${TMPDIR:-/tmp}/central-auth.XXXXXX")
    chmod 600 "$HDR_FILE"
    printf 'Authorization: Bearer %s\n' "$token" > "$HDR_FILE"
}

api_status() { # $1=deploymentId → 输出 JSON
    curl -sS -X POST -H @"$HDR_FILE" --max-time 30 "$API_BASE/status?id=$1"
}

state_of() {   # $1=deploymentId → 仅输出 deploymentState
    api_status "$1" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("deploymentState",""))' 2>/dev/null
}

wait_state() { # $1=id  $2=目标状态  $3=超时分钟
    local id=$1 target=$2 timeout_min=$3 n=0 max=$((timeout_min * 4))
    while :; do
        local state; state=$(state_of "$id") || state=""
        [ "$state" = "$target" ] && return 0
        if [ "$state" = "FAILED" ]; then
            api_status "$id" | python3 -m json.tool || true
            die "deployment $id 校验失败（FAILED）"
        fi
        n=$((n + 1)); [ "$n" -gt "$max" ] && die "等待 $target 超时（${timeout_min} 分钟，当前 $state）"
        printf '    … %s（第 %d 次，每 15 秒轮询）\n' "$state" "$n"
        sleep 15
    done
}

# ---------------- 前置检查 ----------------
precheck() {
    info "检查 JDK 8（aifei-proxy 的 cglib 测试在 JDK 17+ 上会失败）"
    if /usr/libexec/java_home -v 1.8 >/dev/null 2>&1; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
        info "JAVA_HOME = $JAVA_HOME"
    else
        die "未找到 JDK 8：/usr/libexec/java_home -v 1.8"
    fi

    info "检查 GPG 签名密钥 $GPG_KEY"
    gpg --list-secret-keys "$GPG_KEY" >/dev/null 2>&1 || die "本机没有该密钥的私钥（gpg --list-secret-keys）"

    info "检查公钥是否已上传 keyserver"
    local fp="0x${GPG_KEY}"
    if [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$KEY_SERVER/pks/lookup?search=$fp&op=vindex")" = "200" ]; then
        info "公钥已在 keyserver 上"
    else
        info "公钥不在 keyserver 上，自动上传…"
        gpg --keyserver "$KEY_SERVER" --send-keys "$GPG_KEY"
        sleep 10
    fi

    if ! (git diff --quiet && git diff --cached --quiet) 2>/dev/null; then
        echo "⚠️  工作区有未提交的修改，发布的将不是仓库中的代码"
    fi
}

# ---------------- 完整发布流程 ----------------
do_release() {
    precheck
    init_auth_header

    info "开始构建并上传（mvn -Pcentral-release clean deploy）"
    MVN_LOG=$(mktemp "${TMPDIR:-/tmp}/aifei-deploy-log.XXXXXX")
    if ! mvn -Pcentral-release "-Dgpg.keyname=$GPG_KEY" clean deploy 2>&1 | tee "$MVN_LOG"; then
        tail -40 "$MVN_LOG"
        die "构建/上传失败，完整日志: $MVN_LOG"
    fi

    local dep_id
    dep_id=$(grep -oE 'deploymentId: [0-9a-f-]{36}' "$MVN_LOG" | tail -1 | awk '{print $2}')
    [ -n "$dep_id" ] || die "未能从构建日志解析 deploymentId，日志保留在: $MVN_LOG"
    rm -f "$MVN_LOG"; MVN_LOG=""

    info "等待 Portal 校验通过（VALIDATED）"
    wait_state "$dep_id" "VALIDATED" "$VALIDATE_TIMEOUT_MIN"

    local version; version=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null | tail -1)
    echo
    echo "============================================================"
    echo "  deployment : $dep_id"
    echo "  坐标       : $GROUP_PATH → $(echo "$GROUP_PATH" | tr '/' '.'):* : $version"
    echo "  ⚠️  正式发布后永久不可撤消、不可修改！"
    echo "============================================================"
    if [ "$ASSUME_YES" -ne 1 ]; then
        [ -t 0 ] || die "非交互环境请使用 --yes"
        read -r -p "确认发布到 Maven Central? 输入 yes 回车确认: " answer
        [ "$answer" = "yes" ] || die "已取消（deployment 保留在 Portal，可稍后手动发布或用 drop 丢弃）"
    fi

    info "发布中（POST /deployment/$dep_id）"
    local code
    code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST -H @"$HDR_FILE" --max-time 60 "$API_BASE/deployment/$dep_id")
    [ "$code" = "204" ] || die "发布请求失败，HTTP $code"

    info "等待同步到 Maven Central（PUBLISHED）"
    wait_state "$dep_id" "PUBLISHED" "$PUBLISH_TIMEOUT_MIN"

    info "验证 repo1.maven.org"
    sleep 15
    local http
    http=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$REPO1_BASE/$GROUP_PATH/aifei/$version/aifei-$version.pom")
    echo
    echo "🎉 发布完成！"
    echo "    浏览目录: $REPO1_BASE/$GROUP_PATH/"
    echo "    搜索页面: https://central.sonatype.com/artifact/$(echo "$GROUP_PATH" | tr '/' '.')/aifei/$version"
    [ "$http" = "200" ] || echo "    （repo1 暂时 404 属正常，新组首次同步可能再延迟几分钟）"
}

# ---------------- 子命令 ----------------
main() {
    case "${1:-}" in
        status)
            [ -n "${2:-}" ] || die "用法: $0 status <deploymentId>"
            init_auth_header
            api_status "$2" | python3 -m json.tool
            ;;
        drop)
            [ -n "${2:-}" ] || die "用法: $0 drop <deploymentId>"
            init_auth_header
            local code
            code=$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE -H @"$HDR_FILE" --max-time 30 "$API_BASE/deployment/$2")
            [ "$code" = "204" ] && info "已丢弃 $2" || die "丢弃失败，HTTP $code"
            ;;
        --yes)
            ASSUME_YES=1; do_release
            ;;
        "")
            do_release
            ;;
        *)
            die "未知参数: $1（支持: --yes | status <id> | drop <id>）"
            ;;
    esac
}

main "$@"
