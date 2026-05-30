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

import cn.aifei.util.Prop;
import cn.aifei.util.StrUtil;

/**
 * SslConfig
 *
 * @author airhead
 */
public class SslConfig {

  static final String SSL_ENABLE = "feathttp.ssl.enable";
  static final String SSL_PORT = "feathttp.ssl.port";
  static final String SSL_PROTOCOL = "feathttp.ssl.protocol";

  static final String SSL_KEY_STORE_TYPE = "feathttp.ssl.keyStoreType";
  static final String SSL_KEY_STORE = "feathttp.ssl.keyStore";
  static final String SSL_KEY_STORE_PASSWORD = "feathttp.ssl.keyStorePassword";

  static final String SSL_KEY_ALIAS = "feathttp.ssl.keyAlias";
  static final String SSL_KEY_PASSWORD = "feathttp.ssl.keyPassword";

  static final String SSL_CIPHERS = "feathttp.ssl.ciphers";
  static final String SSL_ENABLED_PROTOCOLS = "feathttp.ssl.enabledProtocols";

  // ---------

  protected boolean enable = false;
  protected int port = 443;
  protected String protocol = "TLS";

  protected String keyStoreType;
  protected String keyStore;
  protected String keyStorePassword;

  protected String keyAlias;
  protected String keyPassword;

  protected String[] ciphers = null;
  protected String[] enabledProtocols = null;

  public SslConfig(Prop p) {
    enable = p.getBoolean(SSL_ENABLE, enable);
    port = p.getInt(SSL_PORT, port);
    protocol = p.get(SSL_PROTOCOL, protocol);

    keyStoreType = p.get(SSL_KEY_STORE_TYPE);
    keyStore = p.get(SSL_KEY_STORE);
    keyStorePassword = p.get(SSL_KEY_STORE_PASSWORD);

    keyAlias = p.get(SSL_KEY_ALIAS);
    keyPassword = p.get(SSL_KEY_PASSWORD);

    if (StrUtil.notBlank(p.get(SSL_CIPHERS))) {
      ciphers =
          p.get(SSL_CIPHERS)
              .replace("  ", " ")
              .replace("  ", " ")
              .replace("  ", " ")
              .replace(" ", "")
              .split(":");
    }

    if (StrUtil.notBlank(p.get(SSL_ENABLED_PROTOCOLS))) {
      enabledProtocols =
          p.get(SSL_ENABLED_PROTOCOLS)
              .replace("  ", " ")
              .replace("  ", " ")
              .replace("  ", " ")
              .split(" ");
    }

    if (enable) {
      checkConfig();
    }
  }

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getKeyAlias() {
    return keyAlias;
  }

  public void setKeyAlias(String keyAlias) {
    this.keyAlias = keyAlias;
  }

  public String getKeyPassword() {
    return keyPassword;
  }

  public void setKeyPassword(String keyPassword) {
    this.keyPassword = keyPassword;
  }

  public String getKeyStore() {
    return keyStore;
  }

  public void setKeyStore(String keyStore) {
    this.keyStore = keyStore;
  }

  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  public void setKeyStorePassword(String keyStorePassword) {
    this.keyStorePassword = keyStorePassword;
  }

  public String getKeyStoreType() {
    return keyStoreType;
  }

  public void setKeyStoreType(String keyStoreType) {
    this.keyStoreType = keyStoreType;
  }

  public String[] getCiphers() {
    return ciphers;
  }

  public void setCiphers(String[] ciphers) {
    this.ciphers = ciphers;
  }

  public String[] getEnabledProtocols() {
    return enabledProtocols;
  }

  public void setEnabledProtocols(String[] enabledProtocols) {
    this.enabledProtocols = enabledProtocols;
  }

  protected void checkConfig() {
    if (StrUtil.isBlank(keyStore)) {
      throw new IllegalStateException(SSL_KEY_STORE + " can not be blank");
    }
    if (StrUtil.isBlank(keyStorePassword)) {
      throw new IllegalStateException(SSL_KEY_STORE_PASSWORD + " can not be blank");
    }
  }
}
