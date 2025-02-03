package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jian.search.JinaSearchClient;
import com.litongjava.jian.search.JinaSearchRequest;
import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.perplexica.instance.PlaywrightBrowser;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

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
