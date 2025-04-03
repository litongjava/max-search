package com.litongjava.max.search.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatWsReqMessageVo {
  private String type;
  private Long userId;
  private ChatReqMessage message;
  private List<String> files;
  private String focusMode;
  private String domain;
  private Boolean copilotEnabled;
  private String optimizationMode;
  private String host;
  private List<List<String>> history;
  private boolean sse;
}
