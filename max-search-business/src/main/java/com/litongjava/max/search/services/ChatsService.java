package com.litongjava.max.search.services;

import java.util.List;

import com.litongjava.max.search.model.MaxSearchChatSession;

public class ChatsService {

  public List<MaxSearchChatSession> listChats(Long userId) {
    String sql = "select id,title,focus_mode,created_at,files from max_search_chat_session where user_id=? order by created_at desc";
    List<MaxSearchChatSession> chats = MaxSearchChatSession.dao.find(sql, userId);
    return chats;
  }

  public MaxSearchChatSession getById(Long id) {
    String sql = "select id,title,focus_mode,created_at,files from max_search_chat_session where id=?";
    return MaxSearchChatSession.dao.findFirst(sql, id);
  }
}
