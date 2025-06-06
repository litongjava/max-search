package com.litongjava.max.search.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class WebPageSource {
  private String pageContent;
  private WebPageMetadata metadata;

  public WebPageSource(String title, String url, String content) {
    WebPageMetadata webPageMetadata = new WebPageMetadata(title, url);
    this.pageContent = content;
    this.metadata = webPageMetadata;
  }

  public WebPageSource(String title, String url) {
    WebPageMetadata webPageMetadata = new WebPageMetadata(title, url);
    this.metadata = webPageMetadata;
  }
}
