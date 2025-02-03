package com.litongjava.perplexica.services;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;
import com.jfinal.kit.Kv;
import com.litongjava.db.activerecord.Db;
import com.litongjava.gemini.GeminiClient;
import com.litongjava.gemini.GoogleGeminiModels;
import com.litongjava.google.search.GoogleCustomSearchResponse;
import com.litongjava.google.search.SearchResultItem;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.constants.PerplexityConstants;
import com.litongjava.openai.constants.PerplexityModels;
import com.litongjava.perplexica.callback.DeepSeekChatWebsocketCallback;
import com.litongjava.perplexica.callback.GoogleChatWebsocketCallback;
import com.litongjava.perplexica.callback.PplChatWebsocketCallback;
import com.litongjava.perplexica.can.ChatWsStreamCallCan;
import com.litongjava.perplexica.consts.PerTableNames;
import com.litongjava.perplexica.model.PerplexicaChatSession;
import com.litongjava.perplexica.vo.ChatReqMessage;
import com.litongjava.perplexica.vo.ChatWsReqMessageVo;
import com.litongjava.perplexica.vo.ChatWsRespVo;
import com.litongjava.perplexica.vo.CitationsVo;
import com.litongjava.perplexica.vo.WebPageSource;
import com.litongjava.siliconflow.SiliconFlowConsts;
import com.litongjava.siliconflow.SiliconFlowModels;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tio.utils.tag.TagUtils;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;

@Slf4j
public class LLmAiWsChatSearchService {
  private static final Striped<Lock> sessionLocks = Striped.lock(64);

  /**
   * 使用搜索模型处理消息
  */
  public void processMessageBySearchModel(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo) {
    Long userId = reqMessageVo.getUserId();
    ChatReqMessage message = reqMessageVo.getMessage();
    Long sessionId = message.getChatId();
    Long messageId = message.getMessageId();
    String content = message.getContent();
    // create chat or save message
    if (!Db.exists(PerTableNames.perplexica_chat_session, "id", sessionId)) {
      Lock lock = sessionLocks.get(sessionId);
      lock.lock();
      try {
        new PerplexicaChatSession().setId(sessionId).setUserId(userId).save();
      } finally {
        lock.unlock();
      }

    }

    String from = channelContext.getString("FROM");
    WebSearchResponsePromptService webSearchResponsePromptService = Aop.get(WebSearchResponsePromptService.class);
    long answerMessageId = SnowflakeIdUtils.id();
    String inputPrompt = webSearchResponsePromptService.genInputPrompt(channelContext, content, reqMessageVo.getCopilotEnabled(),
        //
        messageId, answerMessageId, from);
    GeminiSearchPredictService geminiSearchPredictService = Aop.get(GeminiSearchPredictService.class);
    Call call = geminiSearchPredictService.predictWithGemini(channelContext, reqMessageVo, sessionId, messageId, answerMessageId, content, inputPrompt);

    if (call != null) {
      ChatWsStreamCallCan.put(sessionId.toString(), call);
    }
  }

  private Call predictWithDeepSeek(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, String sessionId, String messageId, long answerMessageId, String content, String inputPrompt) {
    log.info("webSearchResponsePrompt:{}", inputPrompt);

    List<OpenAiChatMessage> contents = new ArrayList<>();
    if (inputPrompt != null) {
      contents.add(new OpenAiChatMessage("system", inputPrompt));
    }

    List<List<String>> history = reqMessageVo.getHistory();
    if (history != null && history.size() > 0) {
      for (int i = 0; i < history.size(); i++) {
        String role = history.get(i).get(0);
        String message = history.get(i).get(1);
        if ("human".equals(role)) {
          role = "user";
        } else {
          role = "assistant";
        }
        contents.add(new OpenAiChatMessage(role, message));
      }
    }

    contents.add(new OpenAiChatMessage("user", content));

    OpenAiChatRequestVo chatRequestVo = new OpenAiChatRequestVo().setModel(SiliconFlowModels.DEEPSEEK_R1)
        //
        .setMessages(contents);
    chatRequestVo.setStream(true);

    long start = System.currentTimeMillis();

    Callback callback = new DeepSeekChatWebsocketCallback(channelContext, sessionId, messageId, answerMessageId, start);
    GeminiClient.debug = true;

    String apiKey = EnvUtils.getStr("SILICONFLOW_API_KEY");
    Call call = OpenAiClient.chatCompletions(SiliconFlowConsts.SELICONFLOW_API_BASE, apiKey, chatRequestVo, callback);
    return call;

  }

  public Call google(ChannelContext channelContext, Long sessionId, Long messageId, String content) {
    String cseId = (String) channelContext.getString("CSE_ID");

    long answerMessageId = SnowflakeIdUtils.id();
    //1.问题重写
    // 省略
    //2.搜索
    GoogleCustomSearchResponse search = Aop.get(GoogleCustomSearchService.class).search(cseId, content);
    List<SearchResultItem> items = search.getItems();
    List<WebPageContent> results = new ArrayList<>(items.size());
    for (SearchResultItem searchResultItem : items) {
      String title = searchResultItem.getTitle();
      String link = searchResultItem.getLink();
      String snippet = searchResultItem.getSnippet();
      WebPageContent searchSimpleResult = new WebPageContent(title, link, snippet);
      results.add(searchSimpleResult);
    }
    //3.选择
    Kv kv = Kv.by("quesiton", content).set("search_result", JsonUtils.toJson(results));
    String fileName = "WebSearchSelectPrompt.txt";
    String prompt = PromptEngine.renderToString(fileName, kv);
    log.info("WebSearchSelectPrompt:{}", prompt);

    String selectResultContent = Aop.get(GeminiService.class).generate(prompt);
    List<String> outputs = TagUtils.extractOutput(selectResultContent);
    String titleAndLinks = outputs.get(0);
    if ("not_found".equals(titleAndLinks)) {
      //{"type":"message","data":"", "messageId": "32fcbbf251337c"}

      if (channelContext != null) {
        ChatWsRespVo<String> vo = ChatWsRespVo.message(answerMessageId, "");
        Tio.bSend(channelContext, WebSocketResponse.fromJson(vo));
        vo = ChatWsRespVo.message(messageId, "Sorry,not found");
        log.info("not found:{}", content);
        Tio.bSend(channelContext, WebSocketResponse.fromJson(vo));
      }

      return null;
    }
    //4.send to client
    String[] split = titleAndLinks.split("\n");
    List<CitationsVo> citationList = new ArrayList<>();
    for (int i = 0; i < split.length; i++) {
      String[] split2 = split[i].split("~~");
      citationList.add(new CitationsVo(split2[0], split2[1]));
    }

    if (citationList.size() > 0) {
      List<WebPageSource> sources = Aop.get(WebpageSourceService.class).getListWithCitationsVo(citationList);
      ChatWsRespVo<List<WebPageSource>> chatRespVo = new ChatWsRespVo<>();
      chatRespVo.setType("sources").setData(sources).setMessageId(answerMessageId);
      WebSocketResponse packet = WebSocketResponse.fromJson(chatRespVo);

      if (channelContext != null) {
        Tio.bSend(channelContext, packet);
      }
    }

    //{"type":"message","data":"", "messageId": "32fcbbf251337c"}
    ChatWsRespVo<String> vo = ChatWsRespVo.message(answerMessageId, "");
    WebSocketResponse websocketResponse = WebSocketResponse.fromJson(vo);
    if (channelContext != null) {
      Tio.bSend(channelContext, websocketResponse);
    }

    StringBuffer pageContents = Aop.get(SpiderService.class).spiderAsync(channelContext, answerMessageId, citationList);
    //6.推理
    String isoTimeStr = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

    kv = Kv.by("date", isoTimeStr).set("context", pageContents.toString());
    String webSearchResponsePrompt = PromptEngine.renderToString("WebSearchResponsePrompt.txt", kv);
    log.info("webSearchResponsePrompt:{}", webSearchResponsePrompt);

    List<OpenAiChatMessage> messages = new ArrayList<>();
    messages.add(new OpenAiChatMessage("assistant", webSearchResponsePrompt));
    messages.add(new OpenAiChatMessage(content));

    OpenAiChatRequestVo chatRequestVo = new OpenAiChatRequestVo().setModel(GoogleGeminiModels.GEMINI_2_0_FLASH_EXP)
        //
        .setMessages(messages).setMax_tokens(3000);
    chatRequestVo.setStream(true);
    long start = System.currentTimeMillis();

    Callback callback = new GoogleChatWebsocketCallback(channelContext, sessionId, messageId, answerMessageId, start);
    Call call = Aop.get(GeminiService.class).stream(chatRequestVo, callback);
    return call;
  }

  @SuppressWarnings("unused")
  private Call ppl(ChannelContext channelContext, String sessionId, String messageId, List<OpenAiChatMessage> messages) {
    OpenAiChatRequestVo chatRequestVo = new OpenAiChatRequestVo().setModel(PerplexityModels.LLAMA_3_1_SONAR_LARGE_128K_ONLINE)
        //
        .setMessages(messages).setMax_tokens(3000).setStream(true);

    log.info("chatRequestVo:{}", JsonUtils.toJson(chatRequestVo));
    String pplApiKey = EnvUtils.get("PERPLEXITY_API_KEY");

    chatRequestVo.setStream(true);
    long start = System.currentTimeMillis();

    Callback callback = new PplChatWebsocketCallback(channelContext, sessionId, messageId, start);
    Call call = OpenAiClient.chatCompletions(PerplexityConstants.SERVER_URL, pplApiKey, chatRequestVo, callback);
    return call;
  }
}
