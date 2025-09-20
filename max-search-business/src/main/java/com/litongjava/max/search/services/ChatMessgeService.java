package com.litongjava.max.search.services;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.chat.UniChatMessage;
import com.litongjava.max.search.model.MaxSearchChatMessage;

public class ChatMessgeService {

  public List<MaxSearchChatMessage> listMessage(Long sessionId) {
    return MaxSearchChatMessage.dao.find("select id,role,content,created_at,sources from $table_name where chat_id=?", sessionId);
  }

  public List<UniChatMessage> getHistoryById(Long sessionId) {
    List<UniChatMessage> retval = new ArrayList<>();
    List<MaxSearchChatMessage> messages = MaxSearchChatMessage.dao.find("select role,content from $table_name where chat_id=?", sessionId);
    for (MaxSearchChatMessage perplexicaChatMessage : messages) {
      UniChatMessage chatMessage = new UniChatMessage(perplexicaChatMessage.getRole(), perplexicaChatMessage.getContent());
      retval.add(chatMessage);
    }
    return retval;
  }
}
