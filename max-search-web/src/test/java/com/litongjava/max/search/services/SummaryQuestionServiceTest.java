package com.litongjava.max.search.services;

import java.io.File;

import org.junit.Test;

import com.litongjava.tio.utils.environment.EnvUtils;

public class SummaryQuestionServiceTest {

  @Test
  public void test() {
    File file = new File(".env");
    System.out.println(file.getAbsolutePath());
    System.out.println(file.exists());
    EnvUtils.load();
    String token = EnvUtils.getStr("TAVILY_API_TOKEN");
    System.out.println(token);
  }

}
