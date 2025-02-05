package com.litongjava.perplexica.vo;

import java.util.List;

import com.litongjava.model.web.WebPageContent;
import com.litongjava.openai.chat.ChatMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatParamVo {
  private String rewrited;
  private String inputPrompt;
  private List<ChatMessage> history;
  private String from;
  private long answerMessageId;
  private List<WebPageContent> sources;
}
