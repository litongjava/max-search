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
import com.litongjava.perplexica.consts.OptimizationMode;
import com.litongjava.perplexica.vo.ChatParamVo;
import com.litongjava.perplexica.vo.ChatWsReqMessageVo;
import com.litongjava.perplexica.vo.ChatWsRespVo;
import com.litongjava.perplexica.vo.WebPageSource;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.RequestHeaderKey;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import okhttp3.Call;

public class AiSearchService {
  private GeminiPredictService geminiPredictService = Aop.get(GeminiPredictService.class);
  private DeepSeekPredictService deepSeekPredictService = Aop.get(DeepSeekPredictService.class);
  public boolean spped = true;

  /**
   * 处理搜索请求
   * 根据当前用户的设置（copilotEnabled、optimizationMode）决定是否进行搜索，
   * 然后调用 Tavily Search（通过 MaxSearchSearchService）获取网页内容，
   * 并进一步生成提示词供后续回答生成使用。
   *
   * @param channelContext 通道上下文，用于返回消息
   * @param reqMessageVo   用户请求消息对象，包含消息内容、用户设置及历史记录
   * @param chatParamVo    对话参数对象，保存问题重写结果、提示词、搜索结果等信息
   * @return 返回用于流式处理回答的 Call 对象
   */
  public Call search(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, ChatParamVo chatParamVo) {
    String optimizationMode = reqMessageVo.getOptimizationMode();
    Boolean copilotEnabled = reqMessageVo.getCopilotEnabled();
    String content = reqMessageVo.getMessage().getContent();
    Long questionMessageId = reqMessageVo.getMessage().getMessageId();
    long answerMessageId = chatParamVo.getAnswerMessageId();

    String inputPrompt = null;
    if (copilotEnabled != null && copilotEnabled) {
      String quesiton = null;
      // 如果有问题重写，则优先使用重写后的问题，否则直接使用原始内容
      if (chatParamVo.getRewrited() != null) {
        quesiton = chatParamVo.getRewrited();
      } else {
        quesiton = content;
      }

      // 使用 MaxSearchSearchService 对 Tavily Search API 进行调用
      List<WebPageContent> webPageContents = Aop.get(MaxSearchSearchService.class).search(quesiton);

      // 根据优化模式对搜索结果进行处理
      if (OptimizationMode.balanced.equals(optimizationMode)) {
        List<WebPageContent> rankedWebPageContents = Aop.get(VectorRankerService.class).filter(webPageContents, quesiton, 1);
        rankedWebPageContents = Aop.get(JinaReaderService.class).spider(webPageContents);
        webPageContents.set(0, rankedWebPageContents.get(0));
      } else if (OptimizationMode.quality.equals(optimizationMode)) {
        // 质量模式下先过滤，再异步补全页面内容
        webPageContents = Aop.get(AiRankerService.class).filter(webPageContents, quesiton, 6);
        webPageContents = Aop.get(JinaReaderService.class).spiderAsync(webPageContents);
      }

      chatParamVo.setSources(webPageContents);
      // 将搜索结果转换为 JSON 格式保存到数据库中（便于记录历史消息）
      PGobject pgObject = PgObjectUtils.json(webPageContents);
      Db.updateBySql("update max_search_chat_message set sources=? where id=?", pgObject, questionMessageId);

      // 通过 WebSocket 返回搜索结果引用信息给客户端
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
        // 返回 sources 数据给客户端
        ChatWsRespVo<List<WebPageSource>> chatRespVo = new ChatWsRespVo<>();
        chatRespVo.setType("sources").setData(sources).setMessageId(answerMessageId);
        WebSocketResponse packet = WebSocketResponse.fromJson(chatRespVo);
        Tio.bSend(channelContext, packet);
      }

      // 拼接所有搜索结果内容，用于生成提示词
      StringBuffer markdown = new StringBuffer();
      for (int i = 0; i < webPageContents.size(); i++) {
        WebPageContent webPageContent = webPageContents.get(i);
        markdown.append("source " + (i + 1) + " " + webPageContent.getContent());
      }

      // 使用模板引擎生成提示词，提示词中包含当前日期和搜索结果上下文
      String isoTimeStr = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
      Kv kv = Kv.by("date", isoTimeStr).set("context", markdown);
      inputPrompt = PromptEngine.renderToString("WebSearchResponsePrompt.txt", kv);
    }
    chatParamVo.setSystemPrompt(inputPrompt);
    return deepSeekPredictService.predict(channelContext, reqMessageVo, chatParamVo);
  }
}
