package com.litongjava.max.search.handler;

import com.alibaba.fastjson2.JSONObject;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.consts.WebSiteNames;
import com.litongjava.max.search.services.WsChatService;
import com.litongjava.max.search.vo.ChatSignalVo;
import com.litongjava.max.search.vo.ChatWsReqMessageVo;
import com.litongjava.max.search.vo.ChatWsRespVo;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.RequestHeaderKey;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.websocket.common.WebSocketRequest;
import com.litongjava.tio.websocket.common.WebSocketResponse;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatWebSocketHandler implements IWebSocketHandler {
  public static final String CHARSET = "utf-8";

  /**
   * 握手成功后执行，绑定群组并通知其他用户
   */
  public HttpResponse handshake(HttpRequest httpRequest, HttpResponse response, ChannelContext channelContext) throws Exception {
    return response;
  }

  /**
   * 处理文本消息，并进行消息广播
   */
  public void onAfterHandshaked(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) throws Exception {
    String origin = httpRequest.getOrigin();
    String host = httpRequest.getHost();
    String cesId = null;
    String from = null;
    // Google Custom Search JSON API ID
    if ("https://sjsu.mycounsellor.ai".equals(origin)) {
      cesId = EnvUtils.getStr("SJSU_CSE_ID");
      from = WebSiteNames.SJSU;

    } else if ("https://hawaii.mycounsellor.ai".equals(origin)) {
      cesId = EnvUtils.getStr("HAWAII_CSE_ID");
      from = WebSiteNames.HAWAII;

    } else if ("https://stanford.mycounsellor.ai".equals(origin)) {
      cesId = EnvUtils.getStr("STANFORD_CSE_ID");
      from = WebSiteNames.HAWAII;

    } else if ("https://berkeley.mycounsellor.ai".equals(origin)) {
      cesId = EnvUtils.getStr("BERKELEY_CSE_ID");
      from = WebSiteNames.BERKELEY;
    } else {
      cesId = EnvUtils.getStr("CSE_ID");
      from = WebSiteNames.ALL;
    }
    channelContext.setAttribute("CSE_ID", cesId);
    channelContext.setAttribute("FROM", from);
    channelContext.setAttribute(RequestHeaderKey.Origin, origin);
    channelContext.setAttribute(RequestHeaderKey.Host, host);

    log.info("open:{},{},{}", channelContext.getClientIpAndPort(), from, cesId);
    String json = JsonUtils.toJson(new ChatSignalVo("signal", "open"));
    WebSocketResponse webSocketResponse = WebSocketResponse.fromText(json, CHARSET);
    Tio.send(channelContext, webSocketResponse);
  }

  /**
   * 处理连接关闭请求，进行资源清理
   */
  public Object onClose(WebSocketRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    Tio.remove(channelContext, "客户端主动关闭连接");
    return null;
  }

  /**
   * 处理二进制消息
   */
  public Object onBytes(WebSocketRequest wsRequest, byte[] bytes, ChannelContext channelContext) throws Exception {
    log.info("size:{}", bytes.length);
    return null;
  }

  /**
   * 处理文本消息
   */
  public Object onText(WebSocketRequest wsRequest, String text, ChannelContext channelContext) throws Exception {
    JSONObject reqJsonObject = FastJson2Utils.parseObject(text);
    String type = reqJsonObject.getString("type");
    if ("message".equals(type)) {
      ChatWsReqMessageVo vo = FastJson2Utils.parse(text, ChatWsReqMessageVo.class);
      log.info("message:{}", text);
      try {
        Aop.get(WsChatService.class).dispatch(channelContext, vo);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        ChatWsRespVo<String> error = ChatWsRespVo.error(e.getClass().toGenericString(), e.getMessage());
        WebSocketResponse packet = WebSocketResponse.fromJson(error);
        Tio.bSend(channelContext, packet);
      }

    }
    return null; // 不需要额外的返回值
  }

}