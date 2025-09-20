package com.litongjava.max.search.services;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.consts.OptimizationMode;
import com.litongjava.max.search.vo.ChatDeltaRespVo;
import com.litongjava.max.search.vo.ChatParamVo;
import com.litongjava.max.search.vo.ChatWsReqMessageVo;
import com.litongjava.max.search.vo.WebPageSource;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.sse.SsePacket;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;

@Slf4j
public class MaxWebSiteRAGService {
  public PredictService predictService = Aop.get(PredictService.class);
  private AiRankerService aiRankerService = Aop.get(AiRankerService.class);
  private MaxWebsiteRetrieveService maxRetrieveService = Aop.get(MaxWebsiteRetrieveService.class);
  private VectorRankerService vectorRankerService = Aop.get(VectorRankerService.class);
  public boolean spped = true;

  /**
   *
   */
  public Call index(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, ChatParamVo chatParamVo) {
    String optimizationMode = reqMessageVo.getOptimizationMode();
    Boolean copilotEnabled = reqMessageVo.getCopilotEnabled();
    String content = reqMessageVo.getMessage().getContent();
    long answerMessageId = chatParamVo.getAnswerMessageId();

    
    ChatDeltaRespVo<String> greeting = ChatDeltaRespVo.reasoning(answerMessageId, " Retrieve the school website data.");
    byte[] greetingBytes = FastJson2Utils.toJSONBytes(greeting);
    
    if (channelContext != null) {
      if (reqMessageVo.isSse()) {
        Tio.bSend(channelContext, new SsePacket(greetingBytes));

      } else {
        Tio.bSend(channelContext, new WebSocketResponse(greetingBytes));
      }
    }
    
    
    String inputPrompt = null;
    if (copilotEnabled != null && copilotEnabled) {
      String quesiton = null;
      // 如果有问题重写，则优先使用重写后的问题，否则直接使用原始内容
      if (chatParamVo.getRewrited() != null) {
        quesiton = chatParamVo.getRewrited();
      } else {
        quesiton = content;
      }
      long startTime = SystemTimer.currTime;
      List<WebPageContent> webPageContents = maxRetrieveService.search(quesiton);
      long endTime = SystemTimer.currTime;
      log.info("retrived {} elapsed:{}", quesiton, (endTime - startTime) + "(ms)");

      // 根据优化模式对搜索结果进行处理
      JinaReaderService jinaReaderService = Aop.get(JinaReaderService.class);
      if (OptimizationMode.balanced.equals(optimizationMode)) {

        List<WebPageContent> rankedWebPageContents = vectorRankerService.filter(webPageContents, quesiton, 1);
        rankedWebPageContents = jinaReaderService.spider(webPageContents);
        webPageContents.set(0, rankedWebPageContents.get(0));

      } else if (OptimizationMode.quality.equals(optimizationMode)) {
        // 质量模式下先过滤，再异步补全页面内容
        webPageContents = aiRankerService.filter(webPageContents, quesiton, 6);
        webPageContents = jinaReaderService.spiderAsync(webPageContents);
      }

      chatParamVo.setSources(webPageContents);
      // 将搜索结果转换为 JSON 格式保存到数据库中（便于记录历史消息）
      //Db.updateBySql("update max_search_chat_message set sources=? where id=?", pgObject, questionMessageId);

      List<WebPageSource> sources = new ArrayList<>();

      for (WebPageContent webPageConteont : webPageContents) {
        //
        sources.add(new WebPageSource(webPageConteont.getTitle(), webPageConteont.getUrl(), webPageConteont.getContent()));
      }

      String host = reqMessageVo.getHost();
      if (host == null) {
        host = "//127.0.0.1";
      } else {
        host = "//" + host;
      }
      sources.add(new WebPageSource("All Sources", host + "/sources/" + answerMessageId));
      // 返回 sources 数据给客户端
      ChatDeltaRespVo<List<WebPageSource>> chatRespVo = new ChatDeltaRespVo<>();
      chatRespVo.setType("sources").setData(sources).setMessageId(answerMessageId);

      // 通过 WebSocket or sse 返回搜索结果引用信息给客户端
      if (channelContext != null) {
        byte[] jsonBytes = FastJson2Utils.toJSONBytes(chatRespVo);
        if (reqMessageVo.isSse()) {
          Tio.bSend(channelContext, new SsePacket(jsonBytes));
        } else {
          Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
        }
      }

      // 拼接所有搜索结果内容，用于生成提示词
      StringBuffer markdown = new StringBuffer();
      for (int i = 0; i < webPageContents.size(); i++) {
        WebPageContent webPageContent = webPageContents.get(i);
        String sourceContent = webPageContent.getContent();
        if (StrUtil.isBlank(sourceContent)) {
          sourceContent = webPageContent.getDescription();
        }
        String sourceFormat = "source %d %s %s  ";
        markdown.append(String.format(sourceFormat, (i + 1), webPageContent.getUrl(), sourceContent));
      }

      // 使用模板引擎生成提示词，提示词中包含当前日期和搜索结果上下文
      String isoTimeStr = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
      Kv kv = Kv.by("date", isoTimeStr).set("context", markdown);
      inputPrompt = PromptEngine.renderToString("WebSearchResponsePrompt.txt", kv);
      //log.info("SystemPrompt:{}", inputPrompt);
    }
    chatParamVo.setSystemPrompt(inputPrompt);
    return predictService.predict(channelContext, reqMessageVo, chatParamVo);
  }
}
