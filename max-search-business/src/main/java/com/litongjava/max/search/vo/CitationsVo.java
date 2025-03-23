package com.litongjava.max.search.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitationsVo {
  private String title;
  private String link;

  public CitationsVo(String link) {
    this.link = link;
  }
}
