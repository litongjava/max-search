package com.litongjava.perplexica.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jfinal.kit.Kv;
import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.perplexica.model.MaxSearchChatMessage;
import com.litongjava.perplexica.services.ChatMessgeService;
import com.litongjava.perplexica.services.ChatsService;
import com.litongjava.perplexica.vo.WebPageSource;
import com.litongjava.tio.utils.json.FastJson2Utils;

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
    List<MaxSearchChatMessage> listMessage = chatMessgeService.listMessage(id);
    List<Map<String, Object>> newMessages = new ArrayList<>();
    for (MaxSearchChatMessage perplexicaChatMessage : listMessage) {
      Map<String, Object> map = perplexicaChatMessage.toMap();
      String sourcesStr = perplexicaChatMessage.getSources();
      if (sourcesStr != null) {
        JSONArray jsonArray = FastJson2Utils.parseArray(sourcesStr);
        List<WebPageSource> sources = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
          JSONObject jsonObject = jsonArray.getJSONObject(i);
          String title = jsonObject.getString("title");
          String url = jsonObject.getString("url");
          String content = jsonObject.getString("content");
          WebPageSource webPageSource = new WebPageSource(title, url,content);
          sources.add(webPageSource);
        }
        map.put("sources", sources);
      }
      newMessages.add(map);
    }
    kv.set("messages", newMessages);
    return kv;
  }
}
