package com.litongjava.max.search.controller;

import java.net.URL;

import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.ResourceUtil;

@RequestPath("/api/models")
public class ApiModelController {

  @Get
  public HttpResponse index() {
    URL url = ResourceUtil.getResource("json/openai_model.json");
    String str = FileUtil.readString(url);
    return TioRequestContext.getResponse().setJson(str.toString());
  }
}
