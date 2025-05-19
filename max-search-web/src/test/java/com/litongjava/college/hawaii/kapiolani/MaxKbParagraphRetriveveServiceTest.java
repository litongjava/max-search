package com.litongjava.college.hawaii.kapiolani;

import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.config.AdminAppConfig;
import com.litongjava.maxkb.service.kb.MaxKbParagraphRetrieveService;
import com.litongjava.maxkb.vo.MaxKbRetrieveResult;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.json.JsonUtils;

public class MaxKbParagraphRetriveveServiceTest {

  @Test
  public void test() {
    TioBootTest.runWith(AdminAppConfig.class);
    Long[] datasetIdArray = { 1L };
    Float similarity = 0.2f;
    Integer top_n = 20;
    String question = "When is the first day of Fall 2025";
    //max_kb_sentence_id
    MaxKbRetrieveResult search = Aop.get(MaxKbParagraphRetrieveService.class).retrieve(datasetIdArray, similarity, top_n, question);
    System.out.println(JsonUtils.toJson(search.getParagraph_list()));
  }
}
