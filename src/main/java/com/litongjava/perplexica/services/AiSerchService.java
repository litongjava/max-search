package com.litongjava.perplexica.services;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;
import com.litongjava.db.activerecord.Db;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.kit.PgObjectUtils;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.perplexica.vo.ChatParamVo;
import com.litongjava.perplexica.vo.ChatWsReqMessageVo;
import com.litongjava.perplexica.vo.ChatWsRespVo;
import com.litongjava.perplexica.vo.WebPageSource;
import com.litongjava.searxng.SearxngResult;
import com.litongjava.searxng.SearxngSearchClient;
import com.litongjava.searxng.SearxngSearchParam;
import com.litongjava.searxng.SearxngSearchResponse;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.RequestHeaderKey;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import okhttp3.Call;

public class AiSerchService {
  GeminiPredictService geminiPredictService = Aop.get(GeminiPredictService.class);
  public boolean spped = true;

  public Call search(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, ChatParamVo chatParamVo) {
    Boolean copilotEnabled = reqMessageVo.getCopilotEnabled();
    String content = reqMessageVo.getMessage().getContent();
    Long questionMessageId = reqMessageVo.getMessage().getMessageId();
    long answerMessageId = chatParamVo.getAnswerMessageId();

    String inputPrompt = null;
    if (copilotEnabled != null && copilotEnabled) {
      String quesiton = null;
      if (chatParamVo.getRewrited() != null) {
        quesiton = chatParamVo.getRewrited();
      } else {
        quesiton = content;
      }

      // 1. 进行搜索（可选：SearxNG
      SearxngSearchParam searxngSearchParam = new SearxngSearchParam();
      searxngSearchParam.setFormat("json");
      searxngSearchParam.setQ(quesiton);

      SearxngSearchResponse searchResponse = SearxngSearchClient.search(searxngSearchParam);
      List<SearxngResult> results = searchResponse.getResults();
      List<WebPageContent> webPageContents = new ArrayList<>();
      for (SearxngResult searxngResult : results) {
        String title = searxngResult.getTitle();
        String url = searxngResult.getUrl();
        WebPageContent webpageContent = new WebPageContent(title, url);
        webpageContent.setContent(searxngResult.getContent());
        webPageContents.add(webpageContent);
      }

      if (!spped) {
        //String renderToString = PromptEngine.renderToString("web_answer_prompt.txt");
        webPageContents = Aop.get(AiRankerService.class).filter(webPageContents, quesiton, 6);
        //pages = Aop.get(PlaywrightService.class).spiderAsync(pages);
        //或者替换为使用 Jina Reader API 读取页面内容
        webPageContents = Aop.get(JinaReaderService.class).spiderAsync(webPageContents);
      }

      chatParamVo.setSources(webPageContents);
      //update sources
      PGobject pgObject = PgObjectUtils.json(webPageContents);
      Db.updateBySql("update perplexica_chat_message set sources=? where id=?", pgObject, questionMessageId);

      if (channelContext != null) {
        List<WebPageSource> sources = new ArrayList<>();

        for (WebPageContent webPageConteont : webPageContents) {
          sources.add(new WebPageSource(webPageConteont.getTitle(), webPageConteont.getUrl(), webPageConteont.getContent()));
        }

        String host = channelContext.getString(RequestHeaderKey.Host);
        if (host == null) {
          host = "//127.0.0.1";
        } else {
          host = "//" + host;
        }
        sources.add(new WebPageSource("All Sources", host + "/sources/" + questionMessageId));
        //返回sources
        ChatWsRespVo<List<WebPageSource>> chatRespVo = new ChatWsRespVo<>();
        chatRespVo.setType("sources").setData(sources).setMessageId(answerMessageId);
        WebSocketResponse packet = WebSocketResponse.fromJson(chatRespVo);
        Tio.bSend(channelContext, packet);
      }

      StringBuffer markdown = new StringBuffer();
      for (int i = 0; i < webPageContents.size(); i++) {
        WebPageContent webPageContent = webPageContents.get(i);
        markdown.append("source " + (i + 1) + " " + webPageContent.getContent());
      }

      // save to hisotry
      String isoTimeStr = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
      // 3. 使用 PromptEngine 模版引擎填充提示词
      Kv kv = Kv.by("date", isoTimeStr).set("context", markdown);
      inputPrompt = PromptEngine.renderToString("WebSearchResponsePrompt.txt", kv);
    }
    chatParamVo.setInputPrompt(inputPrompt);
    return geminiPredictService.predict(channelContext, reqMessageVo, chatParamVo);
  }
}
