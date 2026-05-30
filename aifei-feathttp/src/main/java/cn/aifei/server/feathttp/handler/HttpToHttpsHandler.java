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

package cn.aifei.server.feathttp.handler;

import cn.aifei.server.feathttp.FeatHttpConfig;
import cn.aifei.server.feathttp.ssl.SslConfig;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.common.HttpStatus;

/**
 * http 请求重定向到 https
 *
 * <p>配置方法： feathttp.http.toHttps = true
 *
 * <p>重定向默认使用状态码 302，可配置状态码： feathttp.http.toHttpsStatusCode=301
 *
 * @author airhead
 */
public class HttpToHttpsHandler implements HttpHandler {

  protected String httpsPrefix;
  protected int statusCode;
  protected FeatHttpConfig config;

  public HttpToHttpsHandler(FeatHttpConfig config) {
    this.config = config;
    this.statusCode = config.getHttpToHttpsStatusCode();
  }

  @Override
  public void handle(HttpRequest request) throws Throwable {
    HttpResponse response = request.getResponse();
    String httpsUrl = buildRedirectHttpsUrl(request);

    response.setHttpStatus(statusCode, getStatusText(statusCode));
    response.setHeader("Location", httpsUrl);
    response.setHeader("Connection", "close");
    response.close();
  }

  protected String buildRedirectHttpsUrl(HttpRequest request) {
    if (httpsPrefix == null) {
      buildUrlPrefix();
    }

    String uri = request.getRequestURI();
    String queryString = request.getQueryString();
    if (queryString != null && queryString.length() > 0) {
      StringBuilder ret =
          new StringBuilder(httpsPrefix.length() + uri.length() + 1 + queryString.length());
      ret.append(httpsPrefix).append(uri).append('?').append(queryString);
      return ret.toString();
    } else {
      StringBuilder ret = new StringBuilder(httpsPrefix.length() + uri.length());
      ret.append(httpsPrefix).append(uri);
      return ret.toString();
    }
  }

  protected void buildUrlPrefix() {
    SslConfig sslConfig = config.getSslConfig();
    String ret = "https://localhost";

    if (sslConfig.getPort() != 443) {
      ret = ret + ":" + sslConfig.getPort();
    }

    this.httpsPrefix = ret;
  }

  private String getStatusText(int code) {
    switch (code) {
      case 301:
        return "Moved Permanently";
      case 302:
        return "Found";
      case 303:
        return "See Other";
      case 307:
        return "Temporary Redirect";
      case 308:
        return "Permanent Redirect";
      default:
        return "Found";
    }
  }
}
