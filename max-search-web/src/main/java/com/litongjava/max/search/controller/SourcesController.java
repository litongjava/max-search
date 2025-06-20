package com.litongjava.max.search.controller;

import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.db.activerecord.Db;
import com.litongjava.model.web.WebPageContent;

@RequestPath("/sources")
public class SourcesController {

  @Get("/{id}")
  public String source(Long id) {
    String sql = "select sources from max_search_chat_message where id=?";
    String queryStr = Db.queryStr(sql, id);
    
    StringBuffer markdown = new StringBuffer();
    if (queryStr != null) {
      List<WebPageContent> webPages = JSON.parseArray(queryStr, WebPageContent.class);
      for (int i = 0; i < webPages.size(); i++) {
        WebPageContent webPageContent = webPages.get(i);
        markdown.append("source " + (i + 1) + " title: " + webPageContent.getTitle() + "\r\n");
        markdown.append("link " + (i + 1) + " link: " + webPageContent.getUrl() + "\r\n");
        markdown.append("source " + (i + 1) + " content " + webPageContent.getContent() + "\r\n\r\n");
      }
    }
    return markdown.toString();
  }
}
