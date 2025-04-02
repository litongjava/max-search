package com.litongjava.college.hawaii.kapiolani;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.config.AdminAppConfig;
import com.litongjava.tio.boot.testing.TioBootTest;

public class KapiolaniWebPageEmbeddingServiceTest {
  @Test
  public void test() {
    TioBootTest.runWith(AdminAppConfig.class);
    KapiolaniWebPageEmbeddingService kapiolaniWebPageEmbeddingService = Aop.get(KapiolaniWebPageEmbeddingService.class);
    kapiolaniWebPageEmbeddingService.index();
  }

}
