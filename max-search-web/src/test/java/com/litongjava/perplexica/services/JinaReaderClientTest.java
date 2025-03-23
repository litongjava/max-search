package com.litongjava.perplexica.services;

import org.junit.Test;

import com.litongjava.jian.reader.JinaReaderClient;
import com.litongjava.tio.utils.environment.EnvUtils;

public class JinaReaderClientTest {

  @Test
  public void test() {
    EnvUtils.load();
    //String key = EnvUtils.getStr("JINA_API_KEY");
    long start = System.currentTimeMillis();
    JinaReaderClient.read("https://www.tio-boot.com/zh/01_tio-boot%20%E7%AE%80%E4%BB%8B/02.html");
    long end = System.currentTimeMillis();
    System.out.println((end - start) + "(ms)");
  }
}
