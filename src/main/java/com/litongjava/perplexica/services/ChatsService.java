package com.litongjava.perplexica.services;

import java.util.List;

import com.litongjava.perplexica.model.PerplexicaChatSession;

public class ChatsService {

  public List<PerplexicaChatSession> listChats(Long userId) {
    String sql = "select id,title,focus_mode,created_at,files from perplexica_chat_session where user_id=? order by created_at desc";
    List<PerplexicaChatSession> chats = PerplexicaChatSession.dao.find(sql, userId);
    return chats;
  }

  public PerplexicaChatSession getById(Long id) {
    String sql = "select id,title,focus_mode,created_at,files from perplexica_chat_session where id=?";
    return PerplexicaChatSession.dao.findFirst(sql, id);
  }
}
