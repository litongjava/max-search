package com.litongjava.perplexica.services;

import org.junit.AfterClass;
import org.junit.Test;

import com.litongjava.google.search.GoogleCustomSearchResponse;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

public class GoogleCustomSearchServiceTest {

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Test
  public void test() {
    EnvUtils.load();
    String question = "Advertising, Area of Specialization in Creative Track, BS (2024-2025) 4 year";
    String ctx = EnvUtils.getStr("CSE_ID");
    GoogleCustomSearchResponse search = Aop.get(GoogleCustomSearchService.class).search(ctx, question);
    System.out.println(JsonUtils.toJson(search));
  }

}
