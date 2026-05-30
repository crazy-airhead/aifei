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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import cn.aifei.server.Dispatcher;
import cn.aifei.server.Server;
import cn.aifei.server.feathttp.handler.HttpToHttpsHandler;
import cn.aifei.server.feathttp.ssl.SslBuilder;
import cn.aifei.server.feathttp.util.IpUtil;
import cn.aifei.util.PathUtil;
import cn.aifei.util.StrUtil;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.core.server.handler.HttpStaticResourceHandler;

/**
 * FeatHttpServer
 */
public class FeatHttpServer implements Server<HttpRequest, Void> {

    static final String version = "1.0.0";
    protected HttpServer httpServer;
    protected HttpServer sslHttpServer;
    protected FeatHttpConfig config;
    protected Consumer<FeatHttpConfig> configConsumer;
    protected Consumer<ServerOptions> onStartConsumer;
    protected volatile boolean started = false;

    protected FeatHttpHandler featHttpHandler = new FeatHttpHandler();

    public FeatHttpServer() {
        this.config = new FeatHttpConfig();
    }

    public FeatHttpServer(String feathttpConfig) {
        this.config = new FeatHttpConfig(feathttpConfig);
    }

    /**
     * 定制 FeatHttpHandler 实现
     */
    public FeatHttpServer setFeatHttpHandler(FeatHttpHandler featHttpHandler) {
        this.featHttpHandler = featHttpHandler;
        return this;
    }

    /**
     * config 用于配置 FeatHttpServer
     *
     * <pre>
     * 例子：
     *   new FeatHttpServer().config(uc -> {
     *       uc.setPort(8000);
     *       uc.setGzipEnable(true);
     *       uc.setServerName("Aifei");
     *   });
     * </pre>
     */
    public FeatHttpServer config(Consumer<FeatHttpConfig> configConsumer) {
        this.configConsumer = configConsumer;
        return this;
    }

    public FeatHttpConfig getFeatHttpConfig() {
        return config;
    }

    /**
     * 启动前回调，使用 ServerOptions 对象对 feat HTTP 服务器进行深度配置
     *
     * <pre>
     * 例子：
     *   new FeatHttpServer().onStart(options -> {
     *       options.setIdleTimeout(30000);
     *   });
     * </pre>
     */
    public FeatHttpServer onStart(Consumer<ServerOptions> onStartConsumer) {
        this.onStartConsumer = onStartConsumer;
        return this;
    }

    @Override
    public void init(Dispatcher<HttpRequest, Void, ?, ?> dispatcher) {
        featHttpHandler.init(dispatcher);
    }

    @Override
    public synchronized void start() {
        if (started) {
            return;
        }

        try {
            System.out.println("INFO: aifei-feathttp " + version + ", feat-core " + getFeatVersion() + ", JVM " + System.getProperty("java.version"));
            doStart();
            if (config.isPrintServerUrls()) {
                printServerUrls();
            }
            started = true;

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private String getFeatVersion() {
        try {
            return tech.smartboot.feat.Feat.VERSION;
        } catch (Throwable e) {
            return "unknown";
        }
    }

    protected void printServerUrls() {
        String msg = "Server running at\n";
        msg += " > Local:   http://localhost:" + config.getPort();
        if (config.isSslEnable()) {
            msg += "   https://localhost:" + config.getSslConfig().getPort();
        }
        msg += "\n";

        String host = config.getHost() != null ? config.getHost().trim() : "0.0.0.0";
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            System.out.print(msg);
            return;
        }

        List<String> ipList = IpUtil.getLocalIp();
        for (String ip : ipList) {
            msg += " > Network: http://" + ip + ":" + config.getPort();
            if (config.isSslEnable()) {
                msg += "   https://" + ip + ":" + config.getSslConfig().getPort();
            }
            msg += "\n";
        }
        System.out.print(msg);
    }

    protected void doStart() {
        if (configConsumer != null) {
            configConsumer.accept(config);
            configConsumer = null;
        }

        if (config.isSslEnable()) {
            // SSL 启用：需要两个 HttpServer 实例分别监听 HTTP 和 HTTPS 端口
            doStartWithSsl();
        } else {
            if (config.isHttpToHttps()) {
                System.err.println("http redirect to https needs ssl support");
            }
            doStartHttp();
        }
    }

    protected void doStartHttp() {
        httpServer = new HttpServer(new ServerOptions());
        applyServerOptions(httpServer.options());
        configStaticResources();

        if (onStartConsumer != null) {
            onStartConsumer.accept(httpServer.options());
        }

        httpServer.httpHandler(featHttpHandler);
        httpServer.listen(config.getHost(), config.getPort());
    }

    protected void doStartWithSsl() {
        if (!config.isHttpDisable()) {
            // 创建 HTTP 服务器
            httpServer = new HttpServer(new ServerOptions());
            applyServerOptions(httpServer.options());
            configStaticResources();

            if (onStartConsumer != null) {
                onStartConsumer.accept(httpServer.options());
            }

            if (config.isHttpToHttps()) {
                httpServer.httpHandler(new HttpToHttpsHandler(config));
            } else {
                httpServer.httpHandler(featHttpHandler);
            }
            httpServer.listen(config.getHost(), config.getPort());
        } else {
            configStaticResources();
        }

        // 创建 HTTPS 服务器
        sslHttpServer = new HttpServer(new ServerOptions());
        applyServerOptions(sslHttpServer.options());
        new SslBuilder(sslHttpServer, config).build();
        sslHttpServer.httpHandler(featHttpHandler);
        sslHttpServer.listen(config.getHost(), config.getSslConfig().getPort());
    }

    protected void applyServerOptions(ServerOptions options) {
        if (config.getThreadNum() != null) {
            options.threadNum(config.getThreadNum());
        }
        if (config.getReadBufferSize() != null) {
            options.readBufferSize(config.getReadBufferSize());
        }
        if (config.getWriteBufferSize() != null) {
            options.writeBufferSize(config.getWriteBufferSize());
        }
    }

    protected void configStaticResources() {
        List<String> paths = buildResourcePathList(config.getResourcePath());
        Path appHome = PathUtil.getAppHome();

        for (String path : paths) {
            if (path.startsWith("classpath:")) {
                String classPath = path.substring("classpath:".length());
                if (StrUtil.notBlank(classPath)) {
                    featHttpHandler.setStaticResourceHandler(new HttpStaticResourceHandler(opts -> opts.baseDir("classpath:" + classPath)));
                    return;
                }
            } else {
                Path cur = appHome.resolve(path);
                if (Files.isDirectory(cur)) {
                    featHttpHandler.setStaticResourceHandler(new HttpStaticResourceHandler(opts -> opts.baseDir(cur.toFile().getAbsolutePath())));
                    return;
                }
            }
        }
    }

    private List<String> buildResourcePathList(String resourcePath) {
        List<String> ret = new ArrayList<>();
        String[] resourcePathArray = resourcePath.split(",");
        for (String path : resourcePathArray) {
            if (StrUtil.notBlank(path)) {
                ret.add(path.trim().replace(" ", ""));
            }
        }

        if (!ret.contains("webapp")) {
            ret.add(0, "webapp");
        }
        if (!ret.contains("src/main/webapp")) {
            ret.add(1, "src/main/webapp");
        }
        return ret;
    }

    @Override
    public synchronized void stop() {
        if (!started) {
            return;
        }
        if (sslHttpServer != null) {
            sslHttpServer.shutdown();
        }
        if (httpServer != null) {
            httpServer.shutdown();
        }
        started = false;
    }
}
