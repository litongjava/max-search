package com.litongjava.max.search.handler;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.services.SearchSuggestionQuesionService;
import com.litongjava.max.search.vo.SearchChatMesageVo;
import com.litongjava.max.search.vo.WebPageSource;
import com.litongjava.openai.chat.ChatMessage;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.MimeType;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.FastJson2Utils;

public class SearchSuggestionQuesionHandler {

  public HttpResponse index(HttpRequest request) {
    String bodyString = request.getBodyString();
    HttpResponse response = TioRequestContext.getResponse();
    CORSUtils.enableCORS(response);
    if (StrUtil.isBlank(bodyString)) {
      return Resps.fail(response, "request body can not be empty");
    }
    JSONObject jsonObject = FastJson2Utils.parseObject(bodyString);
    JSONArray chatHistory = jsonObject.getJSONArray("chatHistory");
    if (chatHistory == null) {
      return Resps.fail(response, "chatHistory can not be empty");
    }
    List<SearchChatMesageVo> searchChatMessages = chatHistory.toJavaList(SearchChatMesageVo.class);
    List<ChatMessage> chatMessages = new ArrayList<>();
    for (SearchChatMesageVo searchChatMesageVo : searchChatMessages) {
      String role = searchChatMesageVo.getRole();

      List<WebPageSource> sources = searchChatMesageVo.getSources();
      if (sources != null && sources.size() > 0) {
        StringBuffer stringBuffer = new StringBuffer();
        for (WebPageSource source : sources) {
          String pageContent = source.getPageContent();
          if (pageContent != null) {
            stringBuffer.append("source url:").append(source.getMetadata().getUrl()).append("content:").append(pageContent).append("  \r\n");
          }
        }
        String sourcesStr = stringBuffer.toString();
        if (StrUtil.isNotBlank(sourcesStr)) {
          chatMessages.add(new ChatMessage(role, sourcesStr));
        }

      }
      String content = searchChatMesageVo.getContent();
      if (StrUtil.isNotBlank(content)) {
        chatMessages.add(new ChatMessage(role, content));
      }
    }

    String generated = Aop.get(SearchSuggestionQuesionService.class).generate(chatMessages);
    if (generated != null) {
      String charset = "utf-8";
      response.setString(generated, charset, MimeType.TEXT_PLAIN_JSON.toString() + ";charset=" + charset);
    }
    return response;
  }
}
