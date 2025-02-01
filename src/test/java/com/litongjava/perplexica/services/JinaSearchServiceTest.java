package com.litongjava.perplexica.services;

import org.junit.Test;

import com.litongjava.jian.search.JinaSearchClient;
import com.litongjava.tio.utils.environment.EnvUtils;

public class JinaSearchServiceTest {

  @Test
  public void test() {
    EnvUtils.load();
    //String key = EnvUtils.getStr("JINA_API_KEY");
    String result = JinaSearchClient.search("How can I run deepseek r1 with lama.cpp");
    System.out.println(result);
  }
}
