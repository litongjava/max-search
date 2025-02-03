package com.litongjava.perplexica.services;

import java.util.List;

import com.litongjava.perplexica.model.PerplexicaChatMessage;

public class ChatMessgeService {

  public List<PerplexicaChatMessage> listMessage(Long sessionId) {
    return PerplexicaChatMessage.dao.find("select id,role,content,created_at,sources from $table_name where chat_id=?", sessionId);
  }
}
