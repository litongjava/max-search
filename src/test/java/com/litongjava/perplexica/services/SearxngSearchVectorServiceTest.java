package com.litongjava.perplexica.services;

import java.util.List;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.perplexica.config.AdminAppConfig;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.json.JsonUtils;

public class SearxngSearchVectorServiceTest {

  @Test
  public void computeSimilarity() {
    TioBootTest.runWith(AdminAppConfig.class);
    // 4.007477407023278 有答案
    //String quesiton = "first day sjsu spring 2025";
    String question = "Advertising, Area of Specialization in Creative Track, BS (2024-2025) 4 year";
    List<WebPageContent> computeSimilarity = Aop.get(SearxngSearchVectorService.class).computeSimilarity(question);

    System.out.println(JsonUtils.toJson(computeSimilarity));

  }
}
