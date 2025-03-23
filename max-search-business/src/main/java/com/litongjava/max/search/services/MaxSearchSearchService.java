package com.litongjava.max.search.services;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.model.web.WebPageContent;
import com.litongjava.searxng.SearxngResult;
import com.litongjava.searxng.SearxngSearchClient;
import com.litongjava.searxng.SearxngSearchParam;
import com.litongjava.searxng.SearxngSearchResponse;

public class MaxSearchSearchService {

  /**
   * 对外暴露的搜索接口，使用 Tavily Search 进行搜索
   * @param quesiton 用户输入的问题或查询内容
   * @return 搜索到的网页内容列表
   */
  public List<WebPageContent> search(String quesiton) {
    return useTavilySearch(quesiton);
  }

  /**
   * 使用 Tavily Search API 进行搜索，返回详细内容
   * 该方法内部调用 SearxngSearchClient.search() 接口，
   * 将返回的 SearxngResult 结果转换为 WebPageContent 对象
   *
   * @param quesiton 用户查询的关键词
   * @return 包含标题、链接以及详细内容的网页内容列表
   */
  public List<WebPageContent> useTavilySearch(String quesiton) {
    // 使用 Tavily Search 的实现方式，直接调用 SearxngSearchClient.search() 方法
    SearxngSearchResponse searchResponse = SearxngSearchClient.search(quesiton);
    List<SearxngResult> results = searchResponse.getResults();
    List<WebPageContent> webPageContents = new ArrayList<>();

    for (SearxngResult searxngResult : results) {
      String title = searxngResult.getTitle();
      String url = searxngResult.getUrl();

      // 构造 WebPageContent 对象，并设置详细内容
      WebPageContent webpageContent = new WebPageContent(title, url);
      webpageContent.setContent(searxngResult.getContent());

      webPageContents.add(webpageContent);
    }
    return webPageContents;
  }

  /**
   * 使用 SearxNG 搜索参数方式进行搜索
   * 可作为备用实现，此方法先设置搜索格式和查询关键词，再调用搜索接口
   *
   * @param quesiton 用户查询的关键词
   * @return 搜索到的网页内容列表
   */
  public List<WebPageContent> useSearchNg(String quesiton) {
    SearxngSearchParam searxngSearchParam = new SearxngSearchParam();
    searxngSearchParam.setFormat("json");
    searxngSearchParam.setQ(quesiton);

    SearxngSearchResponse searchResponse = SearxngSearchClient.search(searxngSearchParam);
    List<SearxngResult> results = searchResponse.getResults();
    List<WebPageContent> webPageContents = new ArrayList<>();
    for (SearxngResult searxngResult : results) {
      String title = searxngResult.getTitle();
      String url = searxngResult.getUrl();
      WebPageContent webpageContent = new WebPageContent(title, url);
      webpageContent.setContent(searxngResult.getContent());
      webPageContents.add(webpageContent);
    }
    return webPageContents;
  }
}
