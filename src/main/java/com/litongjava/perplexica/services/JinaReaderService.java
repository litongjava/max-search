package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jian.reader.JinaReaderClient;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JinaReaderService {
  public static final String cache_table_name = "web_page_cache";
  //使用Guava的Striped锁，设置256个锁段
  private static final Striped<Lock> stripedLocks = Striped.lock(256);

  public List<WebPageContent> spider(List<WebPageContent> pages) {
    for (int i = 0; i < pages.size(); i++) {
      String link = pages.get(i).getUrl();
      try {
        String result = getPageContent(link);
        pages.get(i).setContent(result);
      } catch (Exception e) {
        log.error("Error retrieving task result: {}", e.getMessage(), e);
      }
    }
    return pages;
  }

  public List<WebPageContent> spiderAsync(List<WebPageContent> pages) {
    List<Future<String>> futures = new ArrayList<>();

    for (int i = 0; i < pages.size(); i++) {
      String link = pages.get(i).getUrl();

      Future<String> future = TioThreadUtils.submit(() -> {
        return getPageContent(link);
      });
      futures.add(i, future);
    }
    for (int i = 0; i < pages.size(); i++) {
      Future<String> future = futures.get(i);
      try {
        String result = future.get();
        if (StrUtil.isNotBlank(result)) {
          pages.get(i).setContent(result);
        }
      } catch (InterruptedException | ExecutionException e) {
        log.error("Error retrieving task result: {}", e.getMessage(), e);
      }
    }
    return pages;
  }

  private String getPageContent(String link) {
    // 首先检查数据库中是否已存在该页面内容
    if (Db.exists(cache_table_name, "url", link)) {
      // 假设 content 字段存储了页面内容
      return Db.queryStr("SELECT markdown FROM " + cache_table_name + " WHERE url = ?", link);
    }

    // 获取与链接对应的锁并锁定
    Lock lock = stripedLocks.get(link);
    lock.lock();
    try {
      // 再次检查，防止其他线程已生成内容
      if (Db.exists(cache_table_name, "url", link)) {
        return Db.queryStr("SELECT markdown FROM " + cache_table_name + " WHERE url = ?", link);
      }
      // 使用 Jina Reader Client 获取页面内容
      String markdown = JinaReaderClient.read(link);
      // 将获取到的页面内容保存到数据库
      if (markdown != null && !markdown.isEmpty()) {
        // 构造数据库实体或使用直接 SQL 插入
        Row newRow = new Row();
        newRow.set("id", SnowflakeIdUtils.id()).set("url", link)
            //
            .set("markdown", markdown);
        Db.save(cache_table_name, newRow);
      }
      return markdown;
    } finally {
      lock.unlock();
    }
  }
}
