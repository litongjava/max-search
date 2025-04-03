package com.litongjava.max.search.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ChatDeltaRespVo<T> {
  private String type;
  private T data;
  private Long messageId;
  private String key;

  public static ChatDeltaRespVo<String> error(String key, String message) {
    return new ChatDeltaRespVo<String>().setType("error").setKey(key).setData(message);
  }

  public static ChatDeltaRespVo<String> progress(String message) {
    return new ChatDeltaRespVo<String>().setType("progress").setData(message);
  }

  public static <T> ChatDeltaRespVo<T> data(String type, T data) {
    return new ChatDeltaRespVo<T>().setType(type).setData(data);

  }

  public static <T> ChatDeltaRespVo<T> keepAlive(Long answerMessageId) {
    return new ChatDeltaRespVo<T>().setType("keep-alive").setMessageId(answerMessageId);
  }

  public static <T> ChatDeltaRespVo<T> message(Long answerMessageId, T data) {
    return new ChatDeltaRespVo<T>().setType("message").setMessageId(answerMessageId).setData(data);
  }

  public static <T> ChatDeltaRespVo<T> reasoning(Long answerMessageId, T data) {
    return new ChatDeltaRespVo<T>().setType("reasoning").setMessageId(answerMessageId).setData(data);
  }

  public static ChatDeltaRespVo<Void> messageEnd(Long answerMessageId) {
    return new ChatDeltaRespVo<Void>().setType("messageEnd").setMessageId(answerMessageId);
  }

}
