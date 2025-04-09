package com.litongjava.max.search.services;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.vo.ChatDeltaRespVo;
import com.litongjava.max.search.vo.ChatParamVo;
import com.litongjava.max.search.vo.ChatWsReqMessageVo;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.sse.SsePacket;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import okhttp3.Call;

public class PredictService {
  private GeminiPredictService geminiPredictService = Aop.get(GeminiPredictService.class);
  private DeepSeekPredictService deepSeekPredictService = Aop.get(DeepSeekPredictService.class);

  public Call predict(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, ChatParamVo chatParamVo) {

    long answerMessageId = chatParamVo.getAnswerMessageId();
    ChatDeltaRespVo<String> greeting = ChatDeltaRespVo.reasoning(answerMessageId, " Let me answer the user's question.");
    byte[] greetingBytes = FastJson2Utils.toJSONBytes(greeting);

    if (channelContext != null) {
      if (reqMessageVo.isSse()) {
        Tio.bSend(channelContext, new SsePacket(greetingBytes));

      } else {
        Tio.bSend(channelContext, new WebSocketResponse(greetingBytes));
      }
    }

    return geminiPredictService.predict(channelContext, reqMessageVo, chatParamVo);
  }

}
