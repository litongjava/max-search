package com.litongjava.max.search.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data 
public class SearchChatMesageVo {
  private Long id;
  private String role;
  private String content;
  private Long created_at;
  private List<WebPageSource> sources;
}
