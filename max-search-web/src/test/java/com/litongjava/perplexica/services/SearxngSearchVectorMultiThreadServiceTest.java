package com.litongjava.perplexica.services;

import java.util.List;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.config.AdminAppConfig;
import com.litongjava.max.search.services.SearxngSearchVectorMultiThreadService;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.tio.boot.testing.TioBootTest;

public class SearxngSearchVectorMultiThreadServiceTest {

  @Test
  public void computeSimilarity() {
    TioBootTest.runWith(AdminAppConfig.class);
    // 4.007477407023278 有答案
    //String quesiton = "first day sjsu spring 2025";
    String question = "《哪吒之魔童闹海》 总票房数量是多少";
    long start = System.currentTimeMillis();
    
    //1266 毫秒
    List<WebPageContent> computeSimilarity = Aop.get(SearxngSearchVectorMultiThreadService.class).computeSimilarity(question);
    long end = System.currentTimeMillis();
    System.out.println((end-start)+"(ms)");
    
  }
}
