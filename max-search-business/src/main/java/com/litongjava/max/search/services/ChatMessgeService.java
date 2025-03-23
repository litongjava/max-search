package com.litongjava.max.search.services;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.max.search.model.MaxSearchChatMessage;
import com.litongjava.openai.chat.ChatMessage;

public class ChatMessgeService {

  public List<MaxSearchChatMessage> listMessage(Long sessionId) {
    return MaxSearchChatMessage.dao.find("select id,role,content,created_at,sources from $table_name where chat_id=?", sessionId);
  }

  public List<ChatMessage> getHistoryById(Long sessionId) {
    List<ChatMessage> retval = new ArrayList<>();
    List<MaxSearchChatMessage> messages = MaxSearchChatMessage.dao.find("select role,content from $table_name where chat_id=?", sessionId);
    for (MaxSearchChatMessage perplexicaChatMessage : messages) {
      ChatMessage chatMessage = new ChatMessage(perplexicaChatMessage.getRole(), perplexicaChatMessage.getContent());
      retval.add(chatMessage);
    }
    return retval;
  }
}
