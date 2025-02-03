package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.searxng.SearxngResult;
import com.litongjava.searxng.SearxngSearchClient;
import com.litongjava.searxng.SearxngSearchParam;
import com.litongjava.searxng.SearxngSearchResponse;

public class SearxngSearchService {

  public List<WebPageContent> search(String q, Boolean fetch, Integer limit) {
    SearxngSearchParam searxngSearchParam = new SearxngSearchParam();
    searxngSearchParam.setFormat("json");
    searxngSearchParam.setQ(q);
    return this.search(searxngSearchParam, fetch, limit);
  }

  public List<WebPageContent> search(SearxngSearchParam param, Boolean fetch, Integer limit) {
    SearxngSearchResponse searchResponse = SearxngSearchClient.search(param);
    List<SearxngResult> results = searchResponse.getResults();
    List<WebPageContent> pages = new ArrayList<>();
    for (SearxngResult searxngResult : results) {
      String title = searxngResult.getTitle();
      String url = searxngResult.getUrl();
      String content = searxngResult.getContent();
      pages.add(new WebPageContent(title, url, content));
    }
    if (fetch != null && fetch) {
      if (limit == null) {
        pages = Aop.get(PlaywrightService.class).spiderAsync(pages);
      } else {
        pages = Aop.get(AiRankerService.class).filter(pages, param.getQ(), limit);
      }

      //pages = Aop.get(PlaywrightService.class).spiderAsync(pages);
      //或者替换为使用 Jina Reader API 读取页面内容
      pages = Aop.get(JinaReaderService.class).spiderAsync(pages);
    }
    return pages;
  }
}
