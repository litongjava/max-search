package com.litongjava.perplexica.handler;

import com.alibaba.fastjson2.JSONObject;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.perplexica.services.WsChatService;
import com.litongjava.perplexica.vo.ChatWsReqMessageVo;
import com.litongjava.perplexica.vo.ChatWsRespVo;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.sse.SsePacket;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.http.server.util.SseEmitter;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatSSEHandler {
  public HttpResponse chat(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();
    CORSUtils.enableCORS(response);
    ChannelContext channelContext = request.getChannelContext();
    // 设置sse请求头
    response.addServerSentEventsHeader();
    // 手动发送消息到客户端,因为已经设置了sse的请求头,所以客户端的连接不会关闭
    Tio.bSend(channelContext, response);
    response.setSend(false);
    
    String text = request.getBodyString();
    JSONObject reqJsonObject = FastJson2Utils.parseObject(text);
    String type = reqJsonObject.getString("type");
    if ("message".equals(type)) {
      ChatWsReqMessageVo vo = FastJson2Utils.parse(text, ChatWsReqMessageVo.class);
      vo.setSse(true);
      log.info("message:{}", text);
      try {
        Aop.get(WsChatService.class).dispatch(channelContext, vo);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        if(vo.isSse()) {
          ChatWsRespVo<String> error = ChatWsRespVo.error(e.getClass().toGenericString(), e.getMessage());
          byte[] jsonBytes = FastJson2Utils.toJSONBytes(error);
          Tio.bSend(channelContext, new SsePacket(jsonBytes));
          SseEmitter.closeSeeConnection(channelContext);
          
        }else {
          ChatWsRespVo<String> error = ChatWsRespVo.error(e.getClass().toGenericString(), e.getMessage());
          WebSocketResponse packet = WebSocketResponse.fromJson(error);
          Tio.bSend(channelContext, packet);
          
        }
      }
    }
    return response;
  }

}