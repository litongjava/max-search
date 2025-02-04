package com.litongjava.perplexica.services;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.perplexica.config.AdminAppConfig;
import com.litongjava.perplexica.config.EnjoyEngineConfig;
import com.litongjava.tio.boot.testing.TioBootTest;

public class WebSearchResponsePromptServiceTest {
  @Test
  public void test() {
    TioBootTest.runWith(AdminAppConfig.class, EnjoyEngineConfig.class);
    String userQuestion = "tio-boot简介";

    // 1. 生成 Prompt
    String prompt = Aop.get(WebSearchResponsePromptService.class)
      .genInputPrompt(null, userQuestion, true, null, null, null);

    // 2. 调用大模型进行推理
    GeminiPredictService geminiSearchPredictService = Aop.get(GeminiPredictService.class);
  }
}
