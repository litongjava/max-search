package com.litongjava.perplexica.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.perplexica.handler.OpenAiV1ProxyHandler;
import com.litongjava.perplexica.handler.SearchSuggestionQuesionHandler;
import com.litongjava.perplexica.handler.SearxngSearchHandler;
import com.litongjava.tio.boot.admin.config.TioAdminControllerConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminDbConfiguration;
import com.litongjava.tio.boot.admin.handler.SystemFileTencentCosHandler;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.server.router.HttpRequestRouter;

@AConfiguration
public class AdminAppConfig {

  @Initialization
  public void config() {
    // 配置数据库相关
    new TioAdminDbConfiguration().config();
    //    new TioAdminRedisDbConfiguration().config();
    //    new TioAdminMongoDbConfiguration().config();
    //    new TioAdminSaTokenConfiguration().config();
    //    new TioAdminInterceptorConfiguration().config();
    //    new TioAdminHandlerConfiguration().config();

    // 获取 HTTP 请求路由器
    HttpRequestRouter r = TioBootServer.me().getRequestRouter();
    if (r != null) {
      // 获取文件处理器，并添加文件上传和获取 URL 的接口
      SystemFileTencentCosHandler systemUploadHandler = Aop.get(SystemFileTencentCosHandler.class);
      r.add("/api/system/file/upload", systemUploadHandler::upload);
      r.add("/api/system/file/url", systemUploadHandler::getUrl);

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
      SearchSuggestionQuesionHandler searchSuggestionQuesionHandler = new SearchSuggestionQuesionHandler();
      r.add("/api/suggestions", searchSuggestionQuesionHandler::index);
    }

    // 配置控制器
    new TioAdminControllerConfiguration().config();
  }
}
