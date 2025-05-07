package com.litongjava.max.search.services;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;
import com.jfinal.kit.Kv;
import com.litongjava.db.activerecord.Db;
import com.litongjava.gemini.GoogleGeminiModels;
import com.litongjava.google.search.GoogleCustomSearchResponse;
import com.litongjava.google.search.SearchResultItem;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.callback.SearchGeminiSseCallback;
import com.litongjava.max.search.callback.SearchPerplexiticySeeCallback;
import com.litongjava.max.search.can.ChatWsStreamCallCan;
import com.litongjava.max.search.consts.FocusMode;
import com.litongjava.max.search.consts.SearchTableNames;
import com.litongjava.max.search.model.MaxSearchChatMessage;
import com.litongjava.max.search.model.MaxSearchChatSession;
import com.litongjava.max.search.vo.ChatDeltaRespVo;
import com.litongjava.max.search.vo.ChatParamVo;
import com.litongjava.max.search.vo.ChatReqMessage;
import com.litongjava.max.search.vo.ChatWsReqMessageVo;
import com.litongjava.max.search.vo.CitationsVo;
import com.litongjava.max.search.vo.WebPageSource;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.openai.chat.ChatMessage;
import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.consts.PerplexityConstants;
import com.litongjava.openai.consts.PerplexityModels;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.sse.SsePacket;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tio.utils.tag.TagUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;

@Slf4j
public class MaxChatService {
  private static final Striped<Lock> sessionLocks = Striped.lock(1024);
  private GeminiPredictService geminiPredictService = Aop.get(GeminiPredictService.class);
  private MaxSearchService maxSearchService = Aop.get(MaxSearchService.class);
  private MaxSearchSummaryQuestionService summaryQuestionService = Aop.get(MaxSearchSummaryQuestionService.class);
  private ChatMessgeService chatMessgeService = Aop.get(ChatMessgeService.class);
  private WebpageSourceService webpageSourceService = Aop.get(WebpageSourceService.class);

  /**
   * 使用搜索模型处理消息
  */
  public void dispatch(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo) {

    long answerMessageId = SnowflakeIdUtils.id();

    ChatReqMessage message = reqMessageVo.getMessage();
    Long userId = reqMessageVo.getUserId();
    Long sessionId = message.getChatId();
    Long messageQuestionId = message.getMessageId();
    String content = message.getContent();

    ChatDeltaRespVo<String> chatVo = ChatDeltaRespVo.message(answerMessageId, "");
    byte[] jsonBytes = FastJson2Utils.toJSONBytes(chatVo);
    if (channelContext != null) {
      if (reqMessageVo.isSse()) {
        Tio.bSend(channelContext, new SsePacket(jsonBytes));

      } else {
        Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
      }
    }

    ChatDeltaRespVo<String> greeting = ChatDeltaRespVo.reasoning(answerMessageId, "Let me think about the user's question.");
    byte[] greetingBytes = FastJson2Utils.toJSONBytes(greeting);

    if (channelContext != null) {
      if (reqMessageVo.isSse()) {
        Tio.bSend(channelContext, new SsePacket(greetingBytes));

      } else {
        Tio.bSend(channelContext, new WebSocketResponse(greetingBytes));
      }
    }

    ChatParamVo chatParamVo = new ChatParamVo();
    chatParamVo.setAnswerMessageId(answerMessageId);
    // create chat or save message
    String focusMode = reqMessageVo.getFocusMode();
    if (!Db.exists(SearchTableNames.max_search_chat_session, "id", sessionId)) {
      Lock lock = sessionLocks.get(sessionId);
      lock.lock();
      try {
        TioThreadUtils.execute(() -> {
          String summary = summaryQuestionService.summary(content);
          new MaxSearchChatSession().setId(sessionId).setUserId(userId).setTitle(summary).setFocusMode(focusMode).save();
        });
      } finally {
        lock.unlock();
      }
    }

    // query history
    List<ChatMessage> history = chatMessgeService.getHistoryById(sessionId);
    chatParamVo.setHistory(history);

    if (content.length() > 30 || history.size() > 0) {
      if (!content.startsWith("Summary: http")) {
        String rewrited = Aop.get(RewriteQuestionService.class).rewrite(content, history);
        log.info("rewrite to:{}", rewrited);
        chatParamVo.setRewrited(rewrited);
        if (channelContext != null) {
          Kv end = Kv.by("type", "rewrited").set("content", rewrited);
          jsonBytes = FastJson2Utils.toJSONBytes(end);
          if (reqMessageVo.isSse()) {
            Tio.bSend(channelContext, new SsePacket(jsonBytes));
          } else {
            Tio.bSend(channelContext, WebSocketResponse.fromBytes(jsonBytes));
          }
        }
      }
    }

    // save user mesasge
    new MaxSearchChatMessage().setId(messageQuestionId).setChatId(sessionId)
        //
        .setRole("user").setContent(content).save();

    String from = channelContext.getString("FROM");
    chatParamVo.setFrom(from);

    Boolean copilotEnabled = reqMessageVo.getCopilotEnabled();
    Call call = null;

    log.info("focusMode:{},{}", userId, focusMode);
    if (FocusMode.webSearch.equals(focusMode)) {
      call = maxSearchService.search(channelContext, reqMessageVo, chatParamVo);

    } else if (FocusMode.rag.equals(focusMode)) {
      MaxWebSiteAugmentedService maxRetrieveService = Aop.get(MaxWebSiteAugmentedService.class);
      call = maxRetrieveService.index(channelContext, reqMessageVo, chatParamVo);

    } else if (FocusMode.translator.equals(focusMode)) {
      String inputPrompt = Aop.get(TranslatorPromptService.class).genInputPrompt(channelContext, content, copilotEnabled, messageQuestionId, messageQuestionId, from);
      chatParamVo.setSystemPrompt(inputPrompt);
      call = geminiPredictService.predict(channelContext, reqMessageVo, chatParamVo);

    } else if (FocusMode.deepSeek.equals(focusMode)) {
      Aop.get(DeepSeekPredictService.class).predict(channelContext, reqMessageVo, chatParamVo);

    } else if (FocusMode.mathAssistant.equals(focusMode)) {
      String inputPrompt = PromptEngine.renderToString("math_assistant_prompt.txt");
      chatParamVo.setSystemPrompt(inputPrompt);
      Aop.get(DeepSeekPredictService.class).predict(channelContext, reqMessageVo, chatParamVo);

    } else if (FocusMode.writingAssistant.equals(focusMode)) {
      String inputPrompt = PromptEngine.renderToString("writing_assistant_prompt.txt");
      chatParamVo.setSystemPrompt(inputPrompt);
      Aop.get(DeepSeekPredictService.class).predict(channelContext, reqMessageVo, chatParamVo);
    } else {
      chatVo = ChatDeltaRespVo.message(answerMessageId, "Sorry Developing");
      jsonBytes = FastJson2Utils.toJSONBytes(chatVo);
      if (channelContext != null) {
        if (reqMessageVo.isSse()) {
          Tio.bSend(channelContext, new SsePacket(jsonBytes));
        } else {
          Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
        }

        Kv end = Kv.by("type", "messageEnd").set("messageId", answerMessageId);
        jsonBytes = FastJson2Utils.toJSONBytes(end);

        if (reqMessageVo.isSse()) {
          Tio.bSend(channelContext, new SsePacket(jsonBytes));
        } else {
          Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
        }
      }
    }

    if (call != null) {
      ChatWsStreamCallCan.put(sessionId.toString(), call);
    }
  }

  public Call google(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, ChatParamVo chatParamVo,
      //
      long start) {
    ChatReqMessage message = reqMessageVo.getMessage();
    String content = message.getContent();
    Long messageId = message.getMessageId();
    boolean isSSE = reqMessageVo.isSse();
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
        ChatDeltaRespVo<String> vo = ChatDeltaRespVo.message(answerMessageId, "");
        Tio.bSend(channelContext, WebSocketResponse.fromJson(vo));
        vo = ChatDeltaRespVo.message(messageId, "Sorry,not found");
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

      List<WebPageSource> sources = webpageSourceService.getListWithCitationsVo(citationList);
      ChatDeltaRespVo<List<WebPageSource>> chatRespVo = new ChatDeltaRespVo<>();
      chatRespVo.setType("sources").setData(sources).setMessageId(answerMessageId);
      byte[] jsonBytes = FastJson2Utils.toJSONBytes(chatRespVo);
      if (channelContext != null) {
        if (isSSE) {
          Tio.bSend(channelContext, new SsePacket(jsonBytes));
        } else {
          Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
        }
      }
    }

    //{"type":"message","data":"", "messageId": "32fcbbf251337c"}
    ChatDeltaRespVo<String> vo = ChatDeltaRespVo.message(answerMessageId, "");
    byte[] jsonBytes = FastJson2Utils.toJSONBytes(vo);
    if (channelContext != null) {
      if (isSSE) {
        Tio.bSend(channelContext, new SsePacket(jsonBytes));
      } else {
        Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));

      }
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

    Callback callback = new SearchGeminiSseCallback(channelContext, reqMessageVo, chatParamVo, start);
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

    Callback callback = new SearchPerplexiticySeeCallback(channelContext, sessionId, messageId, start);
    Call call = OpenAiClient.chatCompletions(PerplexityConstants.SERVER_URL, pplApiKey, chatRequestVo, callback);
    return call;
  }
}
