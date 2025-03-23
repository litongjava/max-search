package com.litongjava.perplexica.callback;

import java.io.IOException;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.openai.chat.ChatResponseDelta;
import com.litongjava.openai.chat.Choice;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.perplexica.can.ChatWsStreamCallCan;
import com.litongjava.perplexica.model.MaxSearchChatMessage;
import com.litongjava.perplexica.vo.ChatParamVo;
import com.litongjava.perplexica.vo.ChatWsReqMessageVo;
import com.litongjava.perplexica.vo.ChatWsRespVo;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.sse.SsePacket;
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
  private ChatWsReqMessageVo reqVo;
  private ChatParamVo chatParamVo;
  private long start;
  private Long sessionId;
  private long answerMessageId;

  public DeepSeekSseCallback(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, ChatParamVo chatParamVo, long start) {
    this.channelContext = channelContext;
    this.reqVo = reqMessageVo;
    this.chatParamVo = chatParamVo;
    this.start = start;
    Long sessionId = reqVo.getMessage().getChatId();
    this.sessionId = sessionId;
    this.answerMessageId = chatParamVo.getAnswerMessageId();
  }

  @Override
  public void onFailure(Call call, IOException e) {
    ChatWsRespVo<String> error = ChatWsRespVo.error("CHAT_ERROR", e.getMessage());
    byte[] jsonBytes = FastJson2Utils.toJSONBytes(error);
    if (reqVo.isSse()) {
      Tio.bSend(channelContext, new SsePacket(jsonBytes));
      ChatWsStreamCallCan.remove(sessionId + "");
      SseEmitter.closeSeeConnection(channelContext);

    } else {
      WebSocketResponse packet = new WebSocketResponse(jsonBytes);
      Tio.bSend(channelContext, packet);
    }

  }

  @Override
  public void onResponse(Call call, Response response) throws IOException {
    if (!response.isSuccessful()) {
      String string = response.body().string();
      String message = "Chat model response an unsuccessful message:" + string;
      log.error("message:{}", message);
      ChatWsRespVo<String> data = ChatWsRespVo.error("STREAM_ERROR", message);
      byte[] jsonBytes = FastJson2Utils.toJSONBytes(data);
      if (reqVo.isSse()) {
        Tio.bSend(channelContext, new SsePacket(jsonBytes));
      } else {
        Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
      }
      return;
    }

    try (ResponseBody responseBody = response.body()) {
      if (responseBody == null) {
        String message = "response body is null";
        log.error(message);
        ChatWsRespVo<String> data = ChatWsRespVo.progress(message);
        byte[] jsonBytes = FastJson2Utils.toJSONBytes(data);
        if (reqVo.isSse()) {
          Tio.bSend(channelContext, new SsePacket(jsonBytes));
        } else {
          Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
        }

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
      byte[] jsonBytes = FastJson2Utils.toJSONBytes(end);
      if (reqVo.isSse()) {
        Tio.bSend(channelContext, new SsePacket(jsonBytes));
      } else {
        Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
      }

      // 关闭连接
      long endTime = System.currentTimeMillis();
      log.info("finish llm in {} (ms)", (endTime - start));

      //log.info("completionContent:{}", completionContent);
      if (completionContent != null && !completionContent.toString().isEmpty()) {
        //函数调用处理
      }
    }
    ChatWsStreamCallCan.remove(sessionId + "");
    if (reqVo.isSse()) {
      // 手动移除连接
      SseEmitter.closeSeeConnection(channelContext);
    }
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
            
            String reasoning_content = delta.getReasoning_content();
            if (reasoning_content != null && !reasoning_content.isEmpty()) {
              ChatWsRespVo<String> vo = ChatWsRespVo.reasoning(answerMessageId, reasoning_content);
              byte[] jsonBytes = FastJson2Utils.toJSONBytes(vo);
              if (reqVo.isSse()) {
                Tio.bSend(channelContext, new SsePacket(jsonBytes));
              } else {
                Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
              }
            }
            
            
            String part = delta.getContent();
            if (part != null && !part.isEmpty()) {
              completionContent.append(part);
              ChatWsRespVo<String> vo = ChatWsRespVo.message(answerMessageId, part);
              byte[] jsonBytes = FastJson2Utils.toJSONBytes(vo);
              if (reqVo.isSse()) {
                Tio.bSend(channelContext, new SsePacket(jsonBytes));
              } else {
                Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
              }
            }
        
          }
        } else if (": keep-alive".equals(line)) {
          ChatWsRespVo<String> vo = ChatWsRespVo.keepAlive(answerMessageId);
          byte[] jsonBytes = FastJson2Utils.toJSONBytes(vo);
          if (reqVo.isSse()) {

          } else {
            Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
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
