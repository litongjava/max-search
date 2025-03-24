package com.litongjava.max.search.services;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.max.search.callback.SearchDeepSeekSseCallback;
import com.litongjava.max.search.vo.ChatParamVo;
import com.litongjava.max.search.vo.ChatWsReqMessageVo;
import com.litongjava.max.search.vo.ChatWsRespVo;
import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.sse.SsePacket;
import com.litongjava.tio.utils.SystemTimer;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.websocket.common.WebSocketResponse;
import com.litongjava.volcengine.VolcEngineConst;
import com.litongjava.volcengine.VolcEngineModels;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;

@Slf4j
public class DeepSeekPredictService {

  public Call predict(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, ChatParamVo chatParamVo) {
    String systemPrompt = chatParamVo.getSystemPrompt();
    Long sessionId = reqMessageVo.getMessage().getChatId();
    Long questionMessageId = reqMessageVo.getMessage().getMessageId();
    String content = reqMessageVo.getMessage().getContent();
    Long answerMessageId = chatParamVo.getAnswerMessageId();

    List<OpenAiChatMessage> contents = new ArrayList<>();
    if (systemPrompt != null) {
      contents.add(new OpenAiChatMessage("system", systemPrompt));
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

    OpenAiChatRequestVo chatRequestVo = new OpenAiChatRequestVo().setModel(VolcEngineModels.DEEPSEEK_R1_250120)
        //
        .setMessages(contents);
    chatRequestVo.setStream(true);

    // 5. 向前端通知一个空消息，标识搜索结束，开始推理
    //{"type":"message","data":"", "messageId": "32fcbbf251337c"}
    ChatWsRespVo<String> chatVo = ChatWsRespVo.message(answerMessageId, "");
    byte[] jsonBytes = FastJson2Utils.toJSONBytes(chatVo);
    if (channelContext != null) {
      if (reqMessageVo.isSse()) {
        Tio.bSend(channelContext, new SsePacket(jsonBytes));
      } else {
        Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
      }
    }

    long start = SystemTimer.currTime;
    Callback callback = new SearchDeepSeekSseCallback(channelContext, reqMessageVo, chatParamVo, start);
    String apiKey = EnvUtils.getStr("VOLCENGINE_API_KEY");
    Call call = OpenAiClient.chatCompletions(VolcEngineConst.BASE_URL, apiKey, chatRequestVo, callback);
    return call;
  }

}
