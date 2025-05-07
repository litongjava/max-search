package com.litongjava.max.search.callback;

import java.io.IOException;
import java.util.List;

import org.postgresql.util.PGobject;

import com.jfinal.kit.Kv;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.gemini.GeminiCandidateVo;
import com.litongjava.gemini.GeminiChatResponseVo;
import com.litongjava.gemini.GeminiContentResponseVo;
import com.litongjava.gemini.GeminiPartVo;
import com.litongjava.gemini.GeminiUsageMetadataVo;
import com.litongjava.kit.PgObjectUtils;
import com.litongjava.max.search.can.ChatWsStreamCallCan;
import com.litongjava.max.search.model.MaxSearchChatMessage;
import com.litongjava.max.search.vo.ChatParamVo;
import com.litongjava.max.search.vo.ChatWsReqMessageVo;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.max.search.vo.ChatDeltaRespVo;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.sse.SsePacket;
import com.litongjava.tio.http.server.util.SseEmitter;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.utils.json.JsonUtils;
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
  private ChatWsReqMessageVo reqVo;
  private ChatParamVo chatParamVo;
  private long start;
  private Long sessionId;
  private long answerMessageId;
  private GeminiUsageMetadataVo usageMetadata;

  public SearchGeminiSseCallback(ChannelContext channelContext, ChatWsReqMessageVo reqMessageVo, ChatParamVo chatParamVo,
      //
      long start) {
    this.channelContext = channelContext;
    this.reqVo = reqMessageVo;
    this.chatParamVo = chatParamVo;
    Long sessionId = reqVo.getMessage().getChatId();
    this.sessionId = sessionId;
    this.answerMessageId = chatParamVo.getAnswerMessageId();
    this.start = start;
  }

  @Override
  public void onFailure(Call call, IOException e) {
    ChatDeltaRespVo<String> error = ChatDeltaRespVo.error("CHAT_ERROR", e.getMessage());
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
      ChatDeltaRespVo<String> data = ChatDeltaRespVo.error("STREAM_ERROR", message);
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
        ChatDeltaRespVo<String> data = ChatDeltaRespVo.progress(message);
        byte[] jsonBytes = FastJson2Utils.toJSONBytes(data);
        if (reqVo.isSse()) {
          Tio.bSend(channelContext, new SsePacket(jsonBytes));
        } else {
          Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
        }
      }
      StringBuffer completionContent = onSuccess(channelContext, answerMessageId, start, responseBody);
      List<WebPageContent> sources = chatParamVo.getSources();
      PGobject pgObject = PgObjectUtils.json(sources);

      // save user mesasge
      try {
        Row row = Row.by("id", answerMessageId).set("chat_id", sessionId).set("role", "assistant").set("content", completionContent.toString())
            //
            .set("sources", pgObject);
        Db.save(MaxSearchChatMessage.tableName, row);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
      Kv end = Kv.by("type", "messageEnd").set("messageId", answerMessageId);
      byte[] jsonBytes = FastJson2Utils.toJSONBytes(end);
      if (reqVo.isSse()) {
        Tio.bSend(channelContext, new SsePacket(jsonBytes));
      } else {
        Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
      }

      // 关闭连接
      long endTime = System.currentTimeMillis();
      log.info("finish llm in {} (ms),tokens:{}", (endTime - start), JsonUtils.toSkipNullJson(usageMetadata));

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
    ChatWsStreamCallCan.remove(sessionId.toString());
  }

  /**
   * 处理ChatGPT成功响应
   *
   * @param channelContext 通道上下文
   * @param responseBody    响应体
   * @return 完整内容
   * @throws IOException
   */
  public StringBuffer onSuccess(ChannelContext channelContext, Long answerMessageId, Long start, ResponseBody responseBody) throws IOException {
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
          usageMetadata = chatResponse.getUsageMetadata();

          List<GeminiCandidateVo> candidates = chatResponse.getCandidates();
          if (!candidates.isEmpty()) {
            GeminiContentResponseVo content = candidates.get(0).getContent();
            List<GeminiPartVo> parts = content.getParts();
            if (parts != null) {
              GeminiPartVo geminiPartVo = parts.get(0);
              String text = geminiPartVo.getText();
              if (text != null && !text.isEmpty()) {
                completionContent.append(text);
                ChatDeltaRespVo<String> vo = ChatDeltaRespVo.message(answerMessageId, text);
                byte[] jsonBytes = FastJson2Utils.toJSONBytes(vo);
                if (reqVo.isSse()) {
                  Tio.bSend(channelContext, new SsePacket(jsonBytes));
                } else {
                  Tio.bSend(channelContext, new WebSocketResponse(jsonBytes));
                }
              }
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
