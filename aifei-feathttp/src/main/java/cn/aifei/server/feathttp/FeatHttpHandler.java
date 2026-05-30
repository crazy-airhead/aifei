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

import cn.aifei.server.Dispatcher;
import java.util.concurrent.CompletableFuture;
import tech.smartboot.feat.core.server.HttpHandler;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.handler.HttpStaticResourceHandler;

/**
 * FeatHttpHandler 连接上游 feat-core 服务器与下游 Dispatcher，下游 Dispatcher 进一步连接 Aifei Handler。
 *
 * @author airhead
 */
public class FeatHttpHandler implements HttpHandler {

  static boolean handleResource = true;
  static HttpStaticResourceHandler staticResourceHandler;

  Dispatcher<HttpRequest, Void, ?, ?> dispatcher;

  /**
   * 设置处理静态资源，默认值为 true。如果设置为 false，可通过在 aifei Handler 链中调用
   * FeatHttpHandler.getStaticResourceHandler() 获取到 HttpStaticResourceHandler 后进行处理
   */
  public static void setHandleResource(boolean handleResource) {
    FeatHttpHandler.handleResource = handleResource;
  }

  /** 供外部获取 staticResourceHandler，方便在 aifei Handler 链条的任意节点处理静态资源 */
  public static HttpStaticResourceHandler getStaticResourceHandler() {
    return staticResourceHandler;
  }

  public void setStaticResourceHandler(HttpStaticResourceHandler handler) {
    this.staticResourceHandler = handler;
  }

  public void init(Dispatcher<HttpRequest, Void, ?, ?> dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Override
  public void handle(HttpRequest request, CompletableFuture<Void> completableFuture)
      throws Throwable {
    if (handleResource && staticResourceHandler != null) {
      String path = request.getRequestURI();
      if (path.indexOf('.') != -1) {
        staticResourceHandler.handle(request, completableFuture);
        return;
      }
    }

    try {
      dispatcher.dispatch(request, null);
    } finally {
      completableFuture.complete(null);
    }
  }

  @Override
  public void handle(HttpRequest request) throws Throwable {
    dispatcher.dispatch(request, null);
  }
}
