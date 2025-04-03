package com.litongjava.max.search.services;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.common.util.concurrent.Striped;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

public class MaxWebPageService {

  private static final Striped<Lock> locks = Striped.lock(1024);

  public WebPageContent fetch(String url) {
    String sql = "select title,markdown from web_page_cache where url=?";
    Row row = Db.findFirst(sql, url);
    if (row == null) {
      Lock lock = locks.get(url);
      lock.lock();
      row = Db.findFirst(sql, url);
      if (row == null) {
        try {
          Connection connect = Jsoup.connect(url);
          Document document = connect.get();
          String title = document.title();
          String text = document.body().text();
          Row set = Row.by("id", SnowflakeIdUtils.id()).set("url", url).set("title", title).set("type", "html").set("markdown", text);
          Db.save("web_page_cache", set);
          return new WebPageContent().setTitle(title).setContent(text).setUrl(url);
        } catch (IOException e) {
          return new WebPageContent().setTitle("IOException").setContent(e.getMessage()).setUrl(url);
        } finally {
          lock.unlock();
        }
      }
    }

    String title = row.getString("title");
    String markdown = row.getString("markdown");
    return new WebPageContent().setTitle(title).setContent(markdown).setUrl(url);
  }
}
