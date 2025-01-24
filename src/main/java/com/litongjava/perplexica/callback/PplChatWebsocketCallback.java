package com.litongjava.perplexica.callback;

import java.io.IOException;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.openai.chat.ChatResponseDelta;
import com.litongjava.openai.chat.Choice;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.perplexica.can.ChatWsStreamCallCan;
import com.litongjava.perplexica.services.WebpageSourceService;
import com.litongjava.perplexica.vo.ChatWsRespVo;
import com.litongjava.perplexica.vo.WebPageSource;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.server.util.SseEmitter;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tio.websocket.common.WebSocketResponse;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

@Slf4j
public class PplChatWebsocketCallback implements Callback {
  private ChannelContext channelContext;
  private String chatId;
  private String quesitonMessageId;
  private long start;

  public PplChatWebsocketCallback(ChannelContext channelContext, String sessionId, String messageId, long start) {
    this.channelContext = channelContext;
    this.chatId = sessionId;
    this.quesitonMessageId = messageId;
    this.start = start;
  }

  @Override
  public void onFailure(Call call, IOException e) {
    ChatWsRespVo<String> error = ChatWsRespVo.error("CHAT_ERROR", e.getMessage());
    WebSocketResponse packet = WebSocketResponse.fromJson(error);
    Tio.bSend(channelContext, packet);
    ChatWsStreamCallCan.remove(chatId);
    SseEmitter.closeSeeConnection(channelContext);
  }

  @Override
  public void onResponse(Call call, Response response) throws IOException {
    if (!response.isSuccessful()) {
      String message = "Chat model response an unsuccessful message:" + response.body().string();
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
      long answerMessageId = SnowflakeIdUtils.id();
      StringBuffer completionContent = onChatGptResponseSuccess(channelContext, answerMessageId, start, responseBody);
      Kv end = Kv.by("type", "messageEnd").set("messageId", answerMessageId);
      Tio.bSend(channelContext, WebSocketResponse.fromJson(end));

      // 关闭连接
      long endTime = System.currentTimeMillis();
      log.info("finish llm in {} (ms)", (endTime - start));

      log.info("completionContent:{}", completionContent);
      if (completionContent != null && !completionContent.toString().isEmpty()) {
        //        TableResult<Kv> tr = Aop.get(LlmChatHistoryService.class).saveAssistant(answerId, chatId, completionContent.toString());
        //        if (tr.getCode() != 1) {
        //          log.error("Failed to save assistant answer: {}", tr);
        //        } else {
        //          Kv kv = Kv.by("answer_id", answerId);
        //          SsePacket packet = new SsePacket(AiChatEventName.message_id, JsonUtils.toJson(kv));
        //          Tio.bSend(channelContext, packet);
        //        }
      }
    }
    ChatWsStreamCallCan.remove(chatId);
  }

  /**
   * 处理ChatGPT成功响应
   *
   * @param channelContext 通道上下文
   * @param responseBody    响应体
   * @return 完整内容
   * @throws IOException
   */
  public StringBuffer onChatGptResponseSuccess(ChannelContext channelContext, Long answerMessageId, Long start, ResponseBody responseBody) throws IOException {
    StringBuffer completionContent = new StringBuffer();
    BufferedSource source = responseBody.source();
    String line;
    boolean sentCitations = false;

    while ((line = source.readUtf8Line()) != null) {
      if (line.length() < 1) {
        continue;
      }
      // 处理数据行
      if (line.length() > 6) {
        String data = line.substring(6);
        if (data.endsWith("}")) {
          OpenAiChatResponseVo chatResponse = FastJson2Utils.parse(data, OpenAiChatResponseVo.class);
          List<String> citations = chatResponse.getCitations();
          if (citations != null && !sentCitations) {
            List<WebPageSource> sources = Aop.get(WebpageSourceService.class).getList(citations);
            ChatWsRespVo<List<WebPageSource>> chatRespVo = new ChatWsRespVo<>();
            chatRespVo.setType("sources").setData(sources).setMessageId(answerMessageId.toString());
            WebSocketResponse packet = WebSocketResponse.fromJson(chatRespVo);
            Tio.bSend(channelContext, packet);
            sentCitations = true;
            //{"type":"message","data":"", "messageId": "32fcbbf251337c"}
            Kv kv = Kv.by("type", "message").set("messageId", answerMessageId).set("data", "");
            WebSocketResponse websocketResponse = WebSocketResponse.fromJson(kv);
            Tio.bSend(channelContext, websocketResponse);

          }
          List<Choice> choices = chatResponse.getChoices();
          if (!choices.isEmpty()) {
            ChatResponseDelta delta = choices.get(0).getDelta();
            String part = delta.getContent();
            if (part != null && !part.isEmpty()) {
              completionContent.append(part);
              ChatWsRespVo<String> vo = ChatWsRespVo.message(answerMessageId.toString(), part);
              Tio.bSend(channelContext, WebSocketResponse.fromJson(vo));
            }
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
