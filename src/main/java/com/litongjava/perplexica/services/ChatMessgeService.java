package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.openai.chat.ChatMessage;
import com.litongjava.perplexica.model.PerplexicaChatMessage;

public class ChatMessgeService {

  public List<PerplexicaChatMessage> listMessage(Long sessionId) {
    return PerplexicaChatMessage.dao.find("select id,role,content,created_at,sources from $table_name where chat_id=?", sessionId);
  }

  public List<ChatMessage> getHistoryById(Long sessionId) {
    List<ChatMessage> retval = new ArrayList<>();
    List<PerplexicaChatMessage> messages = PerplexicaChatMessage.dao.find("select role,content from $table_name where chat_id=?", sessionId);
    for (PerplexicaChatMessage perplexicaChatMessage : messages) {
      ChatMessage chatMessage = new ChatMessage(perplexicaChatMessage.getRole(), perplexicaChatMessage.getContent());
      retval.add(chatMessage);
    }
    return retval;
  }
}
