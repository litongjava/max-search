package com.litongjava.max.search.vo;

import java.util.List;

import com.litongjava.model.web.WebPageContent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscoverResultVo {
  private List<WebPageContent> blogs;
}
