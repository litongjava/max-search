package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Test;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.services.SpiderService;
import com.litongjava.max.search.vo.CitationsVo;

public class SpiderServiceTest {

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Test
  public void test() {
    String link1 = "https://studentservices.stanford.edu/calendar/academic-dates/stanford-academic-calendar-2024-2025";
    String link2 = "https://approaching.stanford.edu/parents-guardians/academic-calendar";
    String link3 = "https://studentservices.stanford.edu/calendar/academic-dates/previous_calendars/stanford-academic-calendar-2023-24";
    List<CitationsVo> citationList = new ArrayList<>();
    citationList.add(new CitationsVo(link1));
    citationList.add(new CitationsVo(link2));
    citationList.add(new CitationsVo(link3));
    Aop.get(SpiderService.class).spiderAsync(null, 0, citationList);
    
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
