package com.litongjava.perplexica.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ChatWsRespVo<T> {
  private String type;
  private T data;
  private String messageId;
  private String key;

  public static ChatWsRespVo<String> error(String key, String message) {
    return new ChatWsRespVo<String>().setType("error").setKey(key).setData(message);
  }

  public static ChatWsRespVo<String> progress(String message) {
    return new ChatWsRespVo<String>().setType("progress").setData(message);
  }

  public static <T> ChatWsRespVo<T> data(String type, T data) {
    return new ChatWsRespVo<T>().setType(type).setData(data);

  }

  public static <T> ChatWsRespVo<T> keepAlive(String answerMessageId) {
    return new ChatWsRespVo<T>().setType("keep-alive").setMessageId(answerMessageId);
  }
  
  public static <T> ChatWsRespVo<T> message(String answerMessageId,T data) {
    return new ChatWsRespVo<T>().setType("message").setMessageId(answerMessageId).setData(data);
  }

  public static ChatWsRespVo<Void> messageEnd(String answerMessageId) {
    return new ChatWsRespVo<Void>().setType("messageEnd").setMessageId(answerMessageId);
  }
}
