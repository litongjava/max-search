package com.litongjava.max.search.services;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.vo.ChatParamVo;
import com.litongjava.max.search.vo.ChatWsReqMessageVo;
import com.litongjava.tio.core.ChannelContext;

import okhttp3.Call;

public class PredictService {
  private GeminiPredictService geminiPredictService = Aop.get(GeminiPredictService.class);
  private DeepSeekPredictService deepSeekPredictService = Aop.get(DeepSeekPredictService.class);
  
  public Call predict(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, ChatParamVo chatParamVo) {
    return deepSeekPredictService.predict(channelContext, reqMessageVo, chatParamVo);
  }

}
