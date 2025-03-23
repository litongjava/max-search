package com.litongjava.perplexica.callback;

import java.io.IOException;
import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.gemini.GeminiCandidateVo;
import com.litongjava.gemini.GeminiChatResponseVo;
import com.litongjava.gemini.GeminiContentResponseVo;
import com.litongjava.gemini.GeminiPartVo;
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
public class SearchGeminiSseCallback implements Callback {
  private ChannelContext channelContext;
  private Long chatId;
  private Long quesitonMessageId;
  private Long answerMessageId;
  private long start;

  public SearchGeminiSseCallback(ChannelContext channelContext, Long sessionId, Long messageId, Long answerMessageId, long start) {
    this.channelContext = channelContext;
    this.chatId = sessionId;
    this.quesitonMessageId = messageId;
    this.answerMessageId = answerMessageId;
    this.start = start;
  }

  @Override
  public void onFailure(Call call, IOException e) {
    ChatWsRespVo<String> error = ChatWsRespVo.error("CHAT_ERROR", e.getMessage());
    WebSocketResponse packet = WebSocketResponse.fromJson(error);
    Tio.bSend(channelContext, packet);
    ChatWsStreamCallCan.remove(chatId.toString());
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
      StringBuffer completionContent = onResponse(channelContext, answerMessageId, start, responseBody);
      // save user mesasge
      new MaxSearchChatMessage().setId(answerMessageId).setChatId(chatId)
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
    ChatWsStreamCallCan.remove(chatId.toString());
  }

  /**
   * 处理ChatGPT成功响应
   *
   * @param channelContext 通道上下文
   * @param responseBody    响应体
   * @return 完整内容
   * @throws IOException
   */
  public StringBuffer onResponse(ChannelContext channelContext, Long answerMessageId, Long start, ResponseBody responseBody) throws IOException {
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
          GeminiChatResponseVo chatResponse = FastJson2Utils.parse(data, GeminiChatResponseVo.class);

          List<GeminiCandidateVo> candidates = chatResponse.getCandidates();
          if (!candidates.isEmpty()) {
            GeminiContentResponseVo content = candidates.get(0).getContent();
            List<GeminiPartVo> parts = content.getParts();
            GeminiPartVo geminiPartVo = parts.get(0);
            String text = geminiPartVo.getText();
            if (text != null && !text.isEmpty()) {
              completionContent.append(text);
              ChatWsRespVo<String> vo = ChatWsRespVo.message(answerMessageId, text);
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
