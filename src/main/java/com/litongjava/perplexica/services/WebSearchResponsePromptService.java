package com.litongjava.perplexica.services;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.jian.search.JinaSearchClient;
import com.litongjava.jian.search.JinaSearchRequest;
import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.perplexica.consts.WebSiteNames;
import com.litongjava.perplexica.vo.ChatWsRespVo;
import com.litongjava.perplexica.vo.WebPageSource;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSearchResponsePromptService {
  public String genInputPrompt(ChannelContext channelContext, String content, Boolean copilotEnabled,
      //
      Long messageId, Long answerMessageId, String from) {

    String inputPrompt = null;
    if (copilotEnabled != null && copilotEnabled) {
      // 1. 进行搜索（可选：SearxNG 或 Jina）
      //String markdown = searchWithJina(channelContext, content, messageId, answerMessageId, from);
      String markdown = searchWithSearxNg(channelContext, content, messageId, answerMessageId, from);
      // 2. 向前端通知一个空消息，标识搜索结束，开始推理
      //{"type":"message","data":"", "messageId": "32fcbbf251337c"}
      ChatWsRespVo<String> vo = ChatWsRespVo.message(answerMessageId, "");
      WebSocketResponse websocketResponse = WebSocketResponse.fromJson(vo);
      if (channelContext != null) {
        Tio.bSend(channelContext, websocketResponse);
      }

      String isoTimeStr = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
      // 3. 使用 PromptEngine 模版引擎填充提示词
      Kv kv = Kv.by("date", isoTimeStr).set("context", markdown);
      inputPrompt = PromptEngine.renderToString("WebSearchResponsePrompt.txt", kv);
    }
    return inputPrompt;
  }

  private String searchWithSearxNg(ChannelContext channelContext, String content, Long messageId, Long answerMessageId, String from) {
    List<WebPageContent> webPageContents = Aop.get(SearxngSearchService.class).search(content, true, 6);
    sendSources(channelContext, answerMessageId, webPageContents);
    StringBuffer markdown = new StringBuffer();
    for (int i = 0; i < webPageContents.size(); i++) {
      WebPageContent webPageContent = webPageContents.get(i);
      markdown.append("source " + i + " " + webPageContent.getContent());
    }
    return markdown.toString();
  }

  public String searchWithJina(ChannelContext channelContext, String content, String messageId, long answerMessageId, String from) {
    // 1.拼接请求
    JinaSearchRequest jinaSearchRequest = new JinaSearchRequest();

    jinaSearchRequest.setQ(content);

    if (WebSiteNames.BERKELEY.equals(from)) {
      jinaSearchRequest.setXSite("berkeley.edu");

    } else if (WebSiteNames.HAWAII.equals(from)) {
      jinaSearchRequest.setXSite("hawaii.edu");

    } else if (WebSiteNames.SJSU.equals(from)) {
      jinaSearchRequest.setXSite("sjsu.edu");

    } else if (WebSiteNames.STANFORD.equals(from)) {
      jinaSearchRequest.setXSite("stanford.edu");
    }

    //2.搜索
    ResponseVo searchResponse = JinaSearchClient.search(jinaSearchRequest);

    String markdown = searchResponse.getBodyString();
    if (!searchResponse.isOk()) {
      log.error(markdown);
      ChatWsRespVo<String> error = ChatWsRespVo.error(markdown, messageId);
      WebSocketResponse packet = WebSocketResponse.fromJson(error);
      if (channelContext != null) {
        Tio.bSend(channelContext, packet);
      }
      return null;
    }

    List<WebPageContent> webPageContents = JinaSearchClient.parse(markdown);
    sendSources(channelContext, answerMessageId, webPageContents);
    return markdown;
  }

  private void sendSources(ChannelContext channelContext, Long answerMessageId, List<WebPageContent> webPageContents) {
    if (channelContext != null) {
      List<WebPageSource> sources = new ArrayList<>();

      for (WebPageContent webPageConteont : webPageContents) {
        sources.add(new WebPageSource(webPageConteont.getTitle(), webPageConteont.getUrl(), webPageConteont.getContent()));
      }
      //返回sources
      ChatWsRespVo<List<WebPageSource>> chatRespVo = new ChatWsRespVo<>();
      chatRespVo.setType("sources").setData(sources).setMessageId(answerMessageId);
      WebSocketResponse packet = WebSocketResponse.fromJson(chatRespVo);
      Tio.bSend(channelContext, packet);
    }
  }
}
