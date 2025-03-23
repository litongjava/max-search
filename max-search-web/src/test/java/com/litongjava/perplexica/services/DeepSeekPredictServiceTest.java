package com.litongjava.perplexica.services;

import org.junit.Test;

import com.litongjava.tio.utils.environment.EnvUtils;

public class DeepSeekPredictServiceTest {
  @Test
  public void test() {
    EnvUtils.load();
    String string = EnvUtils.get("VOLCENGINE_API_KEY");
    System.out.println(string);
  }

}
