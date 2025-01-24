package com.litongjava.perplexica.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatWsReqMessageVo {
  private String type;
  private ChatReqMessage message;
  private List<String> files;
  private String focusMode;
  private String optimizationMode;
  private List<List<String>> history;
}
