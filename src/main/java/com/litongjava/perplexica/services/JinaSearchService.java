package com.litongjava.perplexica.services;

import com.litongjava.jian.search.JinaSearchClient;
import com.litongjava.jian.search.JinaSearchRequest;
import com.litongjava.model.http.response.ResponseVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JinaSearchService {

  public String search(String keywords) {
    JinaSearchRequest jinaSearchRequest = new JinaSearchRequest();
    jinaSearchRequest.setCount(6);
    jinaSearchRequest.setQ(keywords);
    //Remove All Images
    jinaSearchRequest.setXRetainImages("none");
    ResponseVo vo = JinaSearchClient.search(jinaSearchRequest);
    String bodyString = vo.getBodyString();
    if (vo.isOk()) {
      return bodyString;
    } else {
      int code = vo.getCode();
      throw new RuntimeException("code:" + code + " body:" + bodyString);
    }
  }
}
