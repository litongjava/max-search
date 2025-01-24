package com.litongjava.perplexica.controller;

import java.net.URL;

import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.ResourceUtil;

@RequestPath("/api/config")
public class ApiConfigController {

  @Get
  public HttpResponse index() {
    URL url = ResourceUtil.getResource("json/predict_config.json");
    StringBuilder str = FileUtil.readURLAsString(url);
    return TioRequestContext.getResponse().setJson(str.toString());
  }
}
