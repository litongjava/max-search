package com.litongjava.max.search.vo;

import java.util.List;

import com.litongjava.chat.ChatMessage;
import com.litongjava.model.web.WebPageContent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatParamVo {
  private String rewrited;
  private String systemPrompt;
  private List<ChatMessage> history;
  private String from;
  private long answerMessageId;
  private List<WebPageContent> sources;
}
