package com.litongjava.perplexica.services;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.services.WebpageSourceService;
import com.litongjava.max.search.vo.WebPageSource;
import com.litongjava.tio.utils.json.JsonUtils;

public class WebpageSourceServiceTest {

  @Test
  public void testGetJavaVersion() {
    System.out.println(System.getProperty("java.version"));
  }
  @Test
  public void test() {
    WebPageSource webPageSource = Aop.get(WebpageSourceService.class).get("https://www.kapiolani.hawaii.edu/classes/academic-calendar/");
    System.out.println(JsonUtils.toJson(webPageSource));
  }

}
