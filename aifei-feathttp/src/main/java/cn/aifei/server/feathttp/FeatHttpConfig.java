/*
 * Copyright 2011-2035 詹波 (aifei.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aifei.server.feathttp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import cn.aifei.core.Aifei;
import cn.aifei.server.feathttp.ssl.SslConfig;
import cn.aifei.util.PathUtil;
import cn.aifei.util.Prop;
import cn.aifei.util.PropKit;
import cn.aifei.util.StrUtil;

/**
 * FeatHttpConfig
 */
public class FeatHttpConfig {

    static final String FEATHTTP_CONFIG                 = "feathttp.txt";

    static final String PORT                            = "feathttp.port";
    static final String HOST                            = "feathttp.host";

    static final String RESOURCE_PATH                   = "feathttp.resourcePath";

    static final String THREAD_NUM                      = "feathttp.threadNum";

    static final String GZIP_ENABLE                     = "feathttp.gzip.enable";
    static final String GZIP_LEVEL                      = "feathttp.gzip.level";
    static final String GZIP_MIN_LENGTH                 = "feathttp.gzip.minLength";

    static final String HTTP2_ENABLE                    = "feathttp.http2.enable";

    // ssl 模式下 http 请求是否跳转到 https
    static final String HTTP_TO_HTTPS                   = "feathttp.http.toHttps";
    // ssl 模式下 http 请求跳转到 https 使用的状态码
    static final String HTTP_TO_HTTPS_STATUS_CODE       = "feathttp.http.toHttpsStatusCode";
    // ssl 模式下是否关闭 http
    static final String HTTP_DISABLE                    = "feathttp.http.disable";

    static final String SERVER_NAME                     = "feathttp.serverName";

    static final String READ_BUFFER_SIZE                = "feathttp.readBufferSize";
    static final String WRITE_BUFFER_SIZE               = "feathttp.writeBufferSize";

    static final String PRINT_SERVER_URLS               = "feathttp.printServerUrls";

    // ----------------------------------------------------------------------------

    protected int port                          = 80;
    protected String host                       = "0.0.0.0";

    protected String resourcePath               = "webapp, src/main/webapp, WebRoot, WebContent";

    protected Integer threadNum                 = null;

    protected boolean gzipEnable                = false;
    protected int gzipLevel                     = Deflater.DEFAULT_COMPRESSION;
    protected int gzipMinLength                 = 1024;

    protected Boolean http2Enable               = null;

    protected SslConfig sslConfig               = null;
    protected boolean httpToHttps               = false;
    protected int httpToHttpsStatusCode         = 302;
    protected boolean httpDisable               = false;

    protected String serverName                 = null;

    protected Integer readBufferSize            = null;
    protected Integer writeBufferSize           = null;

    protected boolean printServerUrls           = true;

    protected ClassLoader classLoader;
    protected Prop p;

    public FeatHttpConfig() {
        loadProp(FEATHTTP_CONFIG, false);
        init();
    }

    public FeatHttpConfig(String feathttpConfig) {
        loadProp(feathttpConfig.trim(), true);
        init();
    }

    protected void loadProp(String config, boolean givenConfig) {
        String activeProfiles = System.getProperty(PropKit.getActiveProfilesKey());
        List<String> activeProfileList;
        if (activeProfiles != null) {
            activeProfileList = Arrays.stream(activeProfiles.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        } else {
            activeProfileList = Collections.emptyList();
        }

        p = new Prop();
        if (activeProfileList.isEmpty()) {
            if (givenConfig) {
                p.append(config);
            } else {
                p.appendIfExists(config);
            }
        } else {
            for (String activeProfile : activeProfileList) {
                String file = buildPropFileName(config, activeProfile);
                p.append(file);
            }
        }
    }

    private String buildPropFileName(String fileName, String activeProfile) {
        int index = fileName.lastIndexOf('.');
        if (index > 0) {
            String main = fileName.substring(0, index);
            String ext = fileName.substring(index);
            return main + "-" + activeProfile + ext;
        } else {
            return fileName + "-" + activeProfile;
        }
    }

    protected void init() {
        if (p.isEmpty()) {
            return;
        }

        port = p.getInt(PORT, port);
        host = p.get(HOST, host).trim();

        resourcePath = p.get(RESOURCE_PATH, resourcePath).trim();

        threadNum = buildThreadNum();

        gzipEnable = p.getBoolean(GZIP_ENABLE, gzipEnable);
        gzipLevel = checkGzipLevel(p.getInt(GZIP_LEVEL, gzipLevel));
        gzipMinLength = p.getInt(GZIP_MIN_LENGTH, gzipMinLength);

        http2Enable = p.getBoolean(HTTP2_ENABLE, http2Enable);

        sslConfig = new SslConfig(p);
        httpToHttps = p.getBoolean(HTTP_TO_HTTPS, httpToHttps);
        httpToHttpsStatusCode = p.getInt(HTTP_TO_HTTPS_STATUS_CODE, httpToHttpsStatusCode);
        httpDisable = p.getBoolean(HTTP_DISABLE, httpDisable);

        serverName = p.get(SERVER_NAME);

        readBufferSize = p.getInt(READ_BUFFER_SIZE);
        writeBufferSize = p.getInt(WRITE_BUFFER_SIZE);

        printServerUrls = p.getBoolean(PRINT_SERVER_URLS, printServerUrls);
    }

    protected Integer buildThreadNum() {
        Integer valueFromConfig = p.getInt(THREAD_NUM);
        if (valueFromConfig != null) {
            return valueFromConfig;
        }

        int cpuNum = Runtime.getRuntime().availableProcessors();
        if (PathUtil.notDeployMode()) {
            return 4;
        } else {
            return new Double(Math.ceil(cpuNum * 1.6180339)).intValue();
        }
    }

    protected int checkGzipLevel(int gzipLevel) {
        if (gzipLevel != -1 && (gzipLevel < 1 || gzipLevel > 9)) {
            throw new IllegalArgumentException(GZIP_LEVEL + " 不能配置为 " + gzipLevel + ", 可配置的值为: -1, 1, 2, 3, 4, 5, 6, 7, 8, 9");
        }
        return gzipLevel;
    }

    public void setPort(int port) {
        if (p.getInt(PORT) == null) {
            this.port = port;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + PORT + " = " + p.getInt(PORT));
        }
    }

    public int getPort() {
        return port;
    }

    public void setResourcePath(String resourcePath) {
        if (StrUtil.isBlank(resourcePath)) {
            throw new IllegalArgumentException("resourcePath can not be blank");
        }
        if (p.get(RESOURCE_PATH) == null) {
            this.resourcePath = resourcePath;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + RESOURCE_PATH + " = " + p.get(RESOURCE_PATH));
        }
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setHost(String host) {
        if (p.get(HOST) == null) {
            this.host = host;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + HOST + " = " + p.get(HOST));
        }
    }

    public String getHost() {
        return host;
    }

    public void setThreadNum(int threadNum) {
        if (p.getInt(THREAD_NUM) == null) {
            this.threadNum = threadNum;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + THREAD_NUM + " = " + p.getInt(THREAD_NUM));
        }
    }

    public Integer getThreadNum() {
        return threadNum;
    }

    public void setGzipEnable(boolean gzipEnable) {
        if (p.getBoolean(GZIP_ENABLE) == null) {
            this.gzipEnable = gzipEnable;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + GZIP_ENABLE + " = " + p.getBoolean(GZIP_ENABLE));
        }
    }

    public boolean isGzipEnable() {
        return gzipEnable;
    }

    public void setGzipLevel(int gzipLevel) {
        if (p.getInt(GZIP_LEVEL) == null) {
            this.gzipLevel = checkGzipLevel(gzipLevel);
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + GZIP_LEVEL + " = " + p.getInt(GZIP_LEVEL));
        }
    }

    public int getGzipLevel() {
        return gzipLevel;
    }

    public void setGzipMinLength(int gzipMinLength) {
        if (p.getInt(GZIP_MIN_LENGTH) == null) {
            this.gzipMinLength = gzipMinLength;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + GZIP_MIN_LENGTH + " = " + p.getInt(GZIP_MIN_LENGTH));
        }
    }

    public int getGzipMinLength() {
        return gzipMinLength;
    }

    public void setHttp2Enable(boolean http2Enable) {
        if (p.getBoolean(HTTP2_ENABLE) == null) {
            this.http2Enable = http2Enable;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + HTTP2_ENABLE + " = " + p.getBoolean(HTTP2_ENABLE));
        }
    }

    public Boolean getHttp2Enable() {
        return http2Enable;
    }

    public boolean isSslEnable() {
        return sslConfig != null && sslConfig.isEnable();
    }

    public SslConfig getSslConfig() {
        return sslConfig;
    }

    public void setSslConfig(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    public void setHttpToHttps(boolean httpToHttps) {
        if (p.getBoolean(HTTP_TO_HTTPS) == null) {
            this.httpToHttps = httpToHttps;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + HTTP_TO_HTTPS + " = " + p.getBoolean(HTTP_TO_HTTPS));
        }
    }

    public boolean isHttpToHttps() {
        return httpToHttps;
    }

    public void setHttpToHttpsStatusCode(int httpToHttpsStatusCode) {
        if (p.getInt(HTTP_TO_HTTPS_STATUS_CODE) == null) {
            this.httpToHttpsStatusCode = httpToHttpsStatusCode;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + HTTP_TO_HTTPS_STATUS_CODE + " = " + p.getInt(HTTP_TO_HTTPS_STATUS_CODE));
        }
    }

    public int getHttpToHttpsStatusCode() {
        return httpToHttpsStatusCode;
    }

    public void setHttpDisable(boolean httpDisable) {
        if (p.getBoolean(HTTP_DISABLE) == null) {
            this.httpDisable = httpDisable;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + HTTP_DISABLE + " = " + p.getBoolean(HTTP_DISABLE));
        }
    }

    public boolean isHttpDisable() {
        return httpDisable;
    }

    public void setServerName(String serverName) {
        if (p.get(SERVER_NAME) == null) {
            this.serverName = serverName;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + SERVER_NAME + " = " + p.get(SERVER_NAME));
        }
    }

    public String getServerName() {
        if (StrUtil.isBlank(serverName)) {
            return "Aifei " + Aifei.getVersion();
        } else {
            return "disable".equals(serverName.trim()) ? null : serverName.trim();
        }
    }

    public void setReadBufferSize(int readBufferSize) {
        if (p.get(READ_BUFFER_SIZE) == null) {
            this.readBufferSize = readBufferSize;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + READ_BUFFER_SIZE + " = " + p.get(READ_BUFFER_SIZE));
        }
    }

    public Integer getReadBufferSize() {
        return readBufferSize;
    }

    public void setWriteBufferSize(int writeBufferSize) {
        if (p.get(WRITE_BUFFER_SIZE) == null) {
            this.writeBufferSize = writeBufferSize;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + WRITE_BUFFER_SIZE + " = " + p.get(WRITE_BUFFER_SIZE));
        }
    }

    public Integer getWriteBufferSize() {
        return writeBufferSize;
    }

    public void setPrintServerUrls(boolean printServerUrls) {
        if (p.getBoolean(PRINT_SERVER_URLS) == null) {
            this.printServerUrls = printServerUrls;
        } else {
            System.out.println("feathttp-server: 优先使用配置文件中的 " + PRINT_SERVER_URLS + " = " + p.getBoolean(PRINT_SERVER_URLS));
        }
    }

    public boolean isPrintServerUrls() {
        return printServerUrls;
    }

    public void setClassLoader(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader can not be null.");
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            ClassLoader ret = Thread.currentThread().getContextClassLoader();
            classLoader = ret != null ? ret : FeatHttpConfig.class.getClassLoader();
        }
        return classLoader;
    }
}
