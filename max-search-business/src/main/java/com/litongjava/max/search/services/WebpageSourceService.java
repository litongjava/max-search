package com.litongjava.max.search.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.litongjava.max.search.vo.CitationsVo;
import com.litongjava.max.search.vo.WebPageMetadata;
import com.litongjava.max.search.vo.WebPageSource;
import com.litongjava.tio.utils.hutool.FilenameUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebpageSourceService {

  /**
   * 获取指定URL的网页内容和元数据
   *
   * @param url 目标网页的URL
   * @return 包含网页内容和元数据的WebPageSource对象
   */
  public WebPageSource get(String url) {
    WebPageMetadata metadata = new WebPageMetadata();

    // 使用Jsoup连接并获取HTML文档
    Document document;
    try {
      document = Jsoup.connect(url).get();
    } catch (IOException e) {
      log.error("Error fetching URL {}: {}", url, e.getMessage());
      return null; // 或者根据需求返回一个带有错误信息的WebPageSource
    }

    // 提取网页标题
    String title = document.title();

    // 提取网页正文内容
    String pageContent = document.body().text();

    // 设置元数据
    metadata.setTitle(title);
    metadata.setUrl(url);

    // 创建并返回WebPageSource对象
    WebPageSource webPageSource = new WebPageSource();
    webPageSource.setPageContent(pageContent);
    webPageSource.setMetadata(metadata);

    return webPageSource;
  }

  /**
   * 并行获取多个URL的网页内容和元数据
   *
   * @param citations 目标网页的URL列表
   * @return 包含多个WebPageSource对象的列表
   */
  public List<WebPageSource> getList(List<String> citations) {

    // List<Future<WebPageSource>> futures = new ArrayList<>(citations.size());
    List<WebPageSource> sources = new ArrayList<>(citations.size());
    for (String url : citations) {
      WebPageSource webPageSource = new WebPageSource();
      WebPageMetadata metadata = new WebPageMetadata();
      String baseName = FilenameUtils.getBaseName(url);
      metadata.setUrl(url);
      metadata.setTitle(baseName);
      webPageSource.setMetadata(metadata);
      sources.add(webPageSource);
    }
    //    for (String url : citations) {
    //      Callable<WebPageSource> task = () -> {
    //        WebPageMetadata metadata = new WebPageMetadata();
    //        if (url.endsWith(".pdf")) {
    //          WebPageSource webPageSource = new WebPageSource();
    //          String baseName = FilenameUtils.getBaseName(url);
    //          metadata.setUrl(url);
    //          metadata.setTitle(baseName);
    //          webPageSource.setMetadata(metadata);
    //          return webPageSource;
    //        } else {
    //          return get(url);
    //        }
    //      };
    //      Future<WebPageSource> submit = TioThreadUtils.submit(task);
    //      futures.add(submit);
    //    }
    //
    //    for (Future<WebPageSource> future : futures) {
    //      try {
    //        WebPageSource source = future.get();
    //        if (source != null) {
    //          sources.add(source);
    //        }
    //      } catch (InterruptedException e) {
    //        log.error("Task was interrupted: {}", e.getMessage());
    //        Thread.currentThread().interrupt(); // 重设中断状态
    //      } catch (ExecutionException e) {
    //        log.error("Error executing task: {}", e.getCause().getMessage());
    //      }
    //    }

    return sources;
  }

  public List<WebPageSource> getListWithCitationsVo(List<CitationsVo> citationList) {
    List<WebPageSource> sources = new ArrayList<>(citationList.size());
    for (CitationsVo vo : citationList) {
      WebPageSource webPageSource = new WebPageSource();
      WebPageMetadata metadata = new WebPageMetadata();
      metadata.setUrl(vo.getLink());
      metadata.setTitle(vo.getTitle());
      webPageSource.setMetadata(metadata);
      sources.add(webPageSource);
    }
    return sources;
  }
}
