package com.litongjava.college.hawaii.kapiolani;

import java.util.List;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.config.AdminAppConfig;
import com.litongjava.max.search.services.MaxRetrieveService;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.json.JsonUtils;

public class MaxRetrieveServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(AdminAppConfig.class);
    String question = "When is the first day of Fall 2025";
    List<WebPageContent> search = Aop.get(MaxRetrieveService.class).search(question);
    System.out.println(JsonUtils.toSkipNullJson(search));
  }

}
