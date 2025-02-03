package com.litongjava.perplexica.controller;

import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.perplexica.model.PerplexicaChatMessage;
import com.litongjava.perplexica.services.ChatMessgeService;
import com.litongjava.perplexica.services.ChatsService;

@RequestPath("/api/chats")
public class ApiChatsController {
  ChatsService chatsService = Aop.get(ChatsService.class);
  ChatMessgeService chatMessgeService = Aop.get(ChatMessgeService.class);

  @Get
  public Kv index(Long userId) {
    Kv kv = Kv.by("chats", chatsService.listChats(userId));
    return kv;
  }

  @Get("/{id}")
  public Kv get(Long id) {
    Kv kv = Kv.by("chat", chatsService.getById(id));
    List<PerplexicaChatMessage> listMessage = chatMessgeService.listMessage(id);
    kv.set("messages", listMessage);
    return kv;
  }
}
