package com.litongjava.perplexica.callback;

import java.io.IOException;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.openai.chat.ChatResponseDelta;
import com.litongjava.openai.chat.Choice;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.perplexica.can.ChatWsStreamCallCan;
import com.litongjava.perplexica.model.MaxSearchChatMessage;
import com.litongjava.perplexica.vo.ChatWsRespVo;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.server.util.SseEmitter;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

@Slf4j
public class DeepSeekSseCallback implements Callback {
  private ChannelContext channelContext;
  private Long sessionId;
  private Long messageId;
  private Long answerMessageId;
  private Long start;

  public DeepSeekSseCallback(ChannelContext channelContext, Long sessionId, Long messageId, Long answerMessageId, Long start) {
    this.channelContext = channelContext;
    this.sessionId = sessionId;
    this.messageId = messageId;
    this.answerMessageId = answerMessageId;
    this.start=start;
  }

  @Override
  public void onFailure(Call call, IOException e) {
    ChatWsRespVo<String> error = ChatWsRespVo.error("CHAT_ERROR", e.getMessage());
    WebSocketResponse packet = WebSocketResponse.fromJson(error);
    Tio.bSend(channelContext, packet);
    ChatWsStreamCallCan.remove(sessionId + "");
    SseEmitter.closeSeeConnection(channelContext);
  }

  @Override
  public void onResponse(Call call, Response response) throws IOException {
    if (!response.isSuccessful()) {
      String string = response.body().string();
      String message = "Chat model response an unsuccessful message:" + string;
      log.error("message:{}", message);
      ChatWsRespVo<String> data = ChatWsRespVo.error("STREAM_ERROR", message);
      WebSocketResponse webSocketResponse = WebSocketResponse.fromJson(data);
      Tio.bSend(channelContext, webSocketResponse);
      return;
    }

    try (ResponseBody responseBody = response.body()) {
      if (responseBody == null) {
        String message = "response body is null";
        log.error(message);
        ChatWsRespVo<String> data = ChatWsRespVo.progress(message);
        WebSocketResponse webSocketResponse = WebSocketResponse.fromJson(data);
        Tio.bSend(channelContext, webSocketResponse);
        return;
      }
      StringBuffer completionContent = onResponseSuccess(channelContext, answerMessageId, start, responseBody);
   // save user mesasge
      new MaxSearchChatMessage().setId(answerMessageId).setChatId(sessionId)
          //
          .setRole("assistant").setContent(completionContent.toString())
          //
          .save();
      
      Kv end = Kv.by("type", "messageEnd").set("messageId", answerMessageId);
      Tio.bSend(channelContext, WebSocketResponse.fromJson(end));

      // 关闭连接
      long endTime = System.currentTimeMillis();
      log.info("finish llm in {} (ms)", (endTime - start));

      //log.info("completionContent:{}", completionContent);
      if (completionContent != null && !completionContent.toString().isEmpty()) {
        //函数调用处理
      }
    }
    ChatWsStreamCallCan.remove(sessionId + "");
  }

  /**
   * 处理ChatGPT成功响应
   *
   * @param channelContext 通道上下文
   * @param responseBody    响应体
   * @return 完整内容
   * @throws IOException
   */
  public StringBuffer onResponseSuccess(ChannelContext channelContext, Long answerMessageId, Long start, ResponseBody responseBody) throws IOException {
    StringBuffer completionContent = new StringBuffer();
    BufferedSource source = responseBody.source();
    String line;

    while ((line = source.readUtf8Line()) != null) {
      if (line.length() < 1) {
        continue;
      }
      // 处理数据行
      if (line.length() > 6) {
        String data = line.substring(6);
        if (data.endsWith("}")) {
          OpenAiChatResponseVo chatResponse = FastJson2Utils.parse(data, OpenAiChatResponseVo.class);
          List<Choice> choices = chatResponse.getChoices();
          if (!choices.isEmpty()) {
            ChatResponseDelta delta = choices.get(0).getDelta();
            String part = delta.getContent();
            if (part != null && !part.isEmpty()) {
              completionContent.append(part);
              ChatWsRespVo<String> vo = ChatWsRespVo.message(answerMessageId, part);
              Tio.bSend(channelContext, WebSocketResponse.fromJson(vo));
            }

            String reasoning_content = delta.getReasoning_content();
            if (reasoning_content != null && !reasoning_content.isEmpty()) {
              ChatWsRespVo<String> vo = ChatWsRespVo.message(answerMessageId, reasoning_content);
              Tio.bSend(channelContext, WebSocketResponse.fromJson(vo));
            }
          }
        } else if (": keep-alive".equals(line)) {
          ChatWsRespVo<String> vo = ChatWsRespVo.keepAlive(answerMessageId);
          WebSocketResponse websocketResponse = WebSocketResponse.fromJson(vo);
          if (channelContext != null) {
            Tio.bSend(channelContext, websocketResponse);
          }
        } else {
          log.info("Data does not end with }:{}", line);
          //{"type":"messageEnd","messageId":"654b8bdb25e853"}

        }
      }
    }
    return completionContent;
  }

}
