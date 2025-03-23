package com.litongjava.max.search.services;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.model.web.WebPageContent;

public class VectorRankerService {
  public List<WebPageContent> filter(List<WebPageContent> pages, String question, Integer limit) {
    WebPageContent webPageContent = pages.get(0);
    List<WebPageContent> retval = new ArrayList<>();
    retval.add(webPageContent);
    return retval;
  }
}
