package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.perplexica.callback.DeepSeekSseCallback;
import com.litongjava.perplexica.vo.ChatWsReqMessageVo;
import com.litongjava.perplexica.vo.ChatWsRespVo;
import com.litongjava.siliconflow.SiliconFlowConsts;
import com.litongjava.siliconflow.SiliconFlowModels;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;

@Slf4j
public class DeepSeekPredictService {

  public Call predict(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, Long sessionId, Long messageQuestionId, long answerMessageId, String content, String inputPrompt) {

    List<OpenAiChatMessage> contents = new ArrayList<>();
    if (inputPrompt != null) {
      contents.add(new OpenAiChatMessage("system", inputPrompt));
      log.info("deepkseek:{}", inputPrompt);
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

    // 5. 向前端通知一个空消息，标识搜索结束，开始推理
    //{"type":"message","data":"", "messageId": "32fcbbf251337c"}
    ChatWsRespVo<String> chatVo = ChatWsRespVo.message(answerMessageId, "");
    WebSocketResponse websocketResponse = WebSocketResponse.fromJson(chatVo);
    if (channelContext != null) {
      Tio.bSend(channelContext, websocketResponse);
      
      chatVo = ChatWsRespVo.message(answerMessageId, "start thinking...");
      websocketResponse = WebSocketResponse.fromJson(chatVo);
      
      Tio.bSend(channelContext, websocketResponse);
    }
    
    long start = System.currentTimeMillis();

    Callback callback = new DeepSeekSseCallback(channelContext, sessionId, messageQuestionId, answerMessageId, start);
    String apiKey = EnvUtils.getStr("SILICONFLOW_API_KEY");
    Call call = OpenAiClient.chatCompletions(SiliconFlowConsts.SELICONFLOW_API_BASE, apiKey, chatRequestVo, callback);
    return call;
  }

  
}
