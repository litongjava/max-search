package com.litongjava.perplexica.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SearchSimpleResult {
  private String title;
  private String link;
  private String snippet;
}
