package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.litongjava.gemini.GeminiChatRequestVo;
import com.litongjava.gemini.GeminiChatResponseVo;
import com.litongjava.gemini.GeminiClient;
import com.litongjava.gemini.GeminiContentVo;
import com.litongjava.gemini.GeminiPartVo;
import com.litongjava.gemini.GoogleGeminiModels;
import com.litongjava.perplexica.callback.GoogleChatWebsocketCallback;
import com.litongjava.perplexica.vo.ChatWsReqMessageVo;
import com.litongjava.tio.core.ChannelContext;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;

@Slf4j
public class GeminiSearchPredictService {
  public Call predictWithGemini(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo,
      //
      Long sessionId, Long quesitonMessageId, Long answerMessageId, String content, String inputPrompt) {
    // log.info("webSearchResponsePrompt:{}", inputPrompt);

    List<GeminiContentVo> contents = new ArrayList<>();
    // 1. 如果有对话历史，则构建 role = user / model 的上下文内容
    if (reqMessageVo != null) {
      List<List<String>> history = reqMessageVo.getHistory();
      if (history != null && history.size() > 0) {
        for (int i = 0; i < history.size(); i++) {
          String role = history.get(i).get(0);
          String message = history.get(i).get(1);
          if ("human".equals(role)) {
            role = "user";
          } else {
            role = "model";
          }
          contents.add(new GeminiContentVo(role, message));
        }
      }

    }

    // 2. 将 Prompt 塞到 role = "model" 的内容中
    if (inputPrompt != null) {
      GeminiPartVo part = new GeminiPartVo(inputPrompt);
      GeminiContentVo system = new GeminiContentVo("model", Collections.singletonList(part));
      contents.add(system);
    }
    //Content with system role is not supported.
    //Please use a valid role: user, model.
    // 3. 再将用户问题以 role = "user" 的形式添加
    contents.add(new GeminiContentVo("user", content + ". You must reply using the my language."));
    // 4. 构建请求对象并调用
    GeminiChatRequestVo reqVo = new GeminiChatRequestVo(contents);

    long start = System.currentTimeMillis();

    // 5. 流式/一次性获取结果
    Call call = null;
    if (channelContext != null) {
      Callback callback = new GoogleChatWebsocketCallback(channelContext, sessionId, quesitonMessageId, answerMessageId, start);
      GeminiClient.debug = true;
      call = GeminiClient.stream(GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, reqVo, callback);
    } else {
      GeminiChatResponseVo vo = GeminiClient.generate(GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, reqVo);
      log.info(vo.getCandidates().get(0).getContent().getParts().get(0).getText());
    }

    return call;
  }
}
