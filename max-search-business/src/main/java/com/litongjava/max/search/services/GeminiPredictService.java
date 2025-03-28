package com.litongjava.max.search.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.litongjava.gemini.GeminiChatRequestVo;
import com.litongjava.gemini.GeminiChatResponseVo;
import com.litongjava.gemini.GeminiClient;
import com.litongjava.gemini.GeminiContentVo;
import com.litongjava.gemini.GeminiPartVo;
import com.litongjava.gemini.GeminiSystemInstructionVo;
import com.litongjava.gemini.GoogleGeminiModels;
import com.litongjava.max.search.callback.SearchGeminiSseCallback;
import com.litongjava.max.search.consts.FocusMode;
import com.litongjava.max.search.vo.ChatParamVo;
import com.litongjava.max.search.vo.ChatWsReqMessageVo;
import com.litongjava.max.search.vo.ChatWsRespVo;
import com.litongjava.openai.chat.ChatMessage;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;

@Slf4j
public class GeminiPredictService {
  public Call predict(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo,
      //
      ChatParamVo chatParamVo) {
    Long sessionId = reqMessageVo.getMessage().getChatId();
    Long quesitonMessageId = reqMessageVo.getMessage().getMessageId();
    String content = reqMessageVo.getMessage().getContent();

    Long answerMessageId = chatParamVo.getAnswerMessageId();
    String inputPrompt = chatParamVo.getSystemPrompt();

    // log.info("webSearchResponsePrompt:{}", inputPrompt);

    List<GeminiContentVo> contents = new ArrayList<>();
    // 1. 如果有对话历史，则构建 role = user / model 的上下文内容
    List<ChatMessage> history = chatParamVo.getHistory();
    if (history != null) {

      if (history != null && history.size() > 0) {
        for (int i = 0; i < history.size(); i++) {
          ChatMessage chatMessage = history.get(i);
          String role = chatMessage.getRole();
          if ("human".equals(role)) {
            role = "user";
          } else {
            role = "model";
          }
          contents.add(new GeminiContentVo(role, chatMessage.getContent()));
        }
      }

    }

    GeminiChatRequestVo reqVo = new GeminiChatRequestVo(contents);
    // 2. 将 Prompt 塞到 role = "model" 的内容中
    String focusMode = reqMessageVo.getFocusMode();
    if (FocusMode.webSearch.equals(focusMode)) {
      if (inputPrompt != null) {
        GeminiPartVo part = new GeminiPartVo(inputPrompt);
        GeminiContentVo system = new GeminiContentVo("model", Collections.singletonList(part));
        contents.add(system);
      }
      //Content with system role is not supported.
      //Please use a valid role: user, model.
      // 3. 再将用户问题以 role = "user" 的形式添加
      contents.add(new GeminiContentVo("user", content + ". You must reply using the my this message language."));

    } else if (FocusMode.translator.equals(focusMode)) {
      GeminiPartVo geminiPartVo = new GeminiPartVo(inputPrompt);
      GeminiSystemInstructionVo geminiSystemInstructionVo = new GeminiSystemInstructionVo();
      geminiSystemInstructionVo.setParts(geminiPartVo);
      reqVo.setSystem_instruction(geminiSystemInstructionVo);

      contents.add(new GeminiContentVo("user", content));
      log.info("json:{}", JsonUtils.toSkipNullJson(reqVo));
    }

    // 5. 向前端通知一个空消息，标识搜索结束，开始推理
    //{"type":"message","data":"", "messageId": "32fcbbf251337c"}
    ChatWsRespVo<String> chatVo = ChatWsRespVo.message(answerMessageId, "");
    WebSocketResponse websocketResponse = WebSocketResponse.fromJson(chatVo);
    if (channelContext != null) {
      Tio.bSend(channelContext, websocketResponse);
    }
    long start = System.currentTimeMillis();
    // 6. 流式/一次性获取结果
    Call call = null;
    if (channelContext != null) {
      Callback callback = new SearchGeminiSseCallback(channelContext, sessionId, quesitonMessageId, answerMessageId, start);
      call = GeminiClient.stream(GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, reqVo, callback);
    } else {
      GeminiChatResponseVo vo = GeminiClient.generate(GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, reqVo);
      log.info(vo.getCandidates().get(0).getContent().getParts().get(0).getText());
    }

    return call;
  }
}
