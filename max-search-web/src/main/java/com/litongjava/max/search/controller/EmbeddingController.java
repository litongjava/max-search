package com.litongjava.max.search.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.college.hawaii.kapiolani.KapiolaniWebPageEmbeddingService;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.tio.utils.thread.TioThreadUtils;

@RequestPath("/embedding")
public class EmbeddingController {

  public RespBodyVo kcc() {

    TioThreadUtils.execute(() -> {
      try {
        KapiolaniWebPageEmbeddingService kapiolaniWebPageEmbeddingService = Aop.get(KapiolaniWebPageEmbeddingService.class);
        kapiolaniWebPageEmbeddingService.index();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    return RespBodyVo.ok();
  }
}
