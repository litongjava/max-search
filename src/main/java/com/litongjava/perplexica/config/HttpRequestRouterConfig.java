package com.litongjava.perplexica.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.perplexica.handler.OpenAiV1ProxyHandler;
import com.litongjava.perplexica.handler.SearxngSearchHandler;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.server.router.HttpRequestRouter;

@AConfiguration
public class HttpRequestRouterConfig {

  @Initialization
  public void config() {
    // 获取router
    HttpRequestRouter r = TioBootServer.me().getRequestRouter();

    // 创建handler
    // IndexRequestHandler indexRequestHandler = new IndexRequestHandler();
    // 添加action
    // r.add("/*", indexRequestHandler::index);
    OpenAiV1ProxyHandler openaiV1ChatHandler = new OpenAiV1ProxyHandler();
    r.add("/openai/v1/models", openaiV1ChatHandler::models);
    r.add("/openai/v1/chat/completions", openaiV1ChatHandler::completions);
    //r.add("openai/v1/embeddings", embeddingV1Handler::embedding);
    
    SearxngSearchHandler searxngSearchHandler = new SearxngSearchHandler();
    r.add("/search", searxngSearchHandler::search);
    r.add("/api/v1/search", searxngSearchHandler::search);
  }
}
