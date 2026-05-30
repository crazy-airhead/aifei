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

package cn.aifei.server.feathttp.ssl;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import io.github.smartboot.socket.extension.plugins.SslPlugin;
import io.github.smartboot.socket.extension.ssl.ClientAuth;
import io.github.smartboot.socket.extension.ssl.factory.ServerSSLContextFactory;
import tech.smartboot.feat.core.server.HttpServer;
import cn.aifei.server.feathttp.FeatHttpConfig;

/**
 * SslBuilder
 */
public class SslBuilder {

    protected HttpServer httpServer;
    protected FeatHttpConfig config;
    protected SslConfig sslConfig;

    public SslBuilder(HttpServer httpServer, FeatHttpConfig config) {
        this.httpServer = httpServer;
        this.config = config;
        this.sslConfig = config.getSslConfig();
    }

    public void build() {
        try {
            InputStream keystoreStream = loadKeyStoreStream();
            String keyStorePassword = sslConfig.getKeyStorePassword();
            String keyPassword = sslConfig.getKeyPassword() != null ? sslConfig.getKeyPassword() : keyStorePassword;

            ServerSSLContextFactory sslContextFactory = new ServerSSLContextFactory(keystoreStream, keyStorePassword, keyPassword);
            SslPlugin sslPlugin = new SslPlugin(sslContextFactory, ClientAuth.NONE);

            httpServer.options().addPlugin(sslPlugin);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected InputStream loadKeyStoreStream() throws Exception {
        String keyStorePath = sslConfig.getKeyStore();
        InputStream stream = config.getClassLoader().getResourceAsStream(keyStorePath);
        if (stream == null) {
            stream = Files.newInputStream(Paths.get(keyStorePath));
        }
        if (stream == null) {
            throw new RuntimeException("Could not load keystore: " + keyStorePath);
        }
        return stream;
    }
}
