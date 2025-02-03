package com.litongjava.perplexica.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatReqMessage {
  private Long messageId;
  private Long chatId;
  private String content;
}
