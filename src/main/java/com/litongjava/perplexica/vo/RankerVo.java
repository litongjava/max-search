package com.litongjava.perplexica.vo;

import java.util.List;

import com.litongjava.model.web.WebPageContent;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RankerVo {
  private List<WebPageContent> pages;
  private String answer;
}
