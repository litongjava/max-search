package com.litongjava.max.search.controller;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.annotation.Get;
import com.litongjava.annotation.RequestPath;
import com.litongjava.max.search.vo.DiscoverResultVo;
import com.litongjava.model.web.WebPageContent;

@RequestPath("/api/discover")
public class ApiDiscoverController {

  @Get
  public DiscoverResultVo index() {
    List<WebPageContent> blogs=new ArrayList<>();
    WebPageContent webPageContent = new WebPageContent()
        .setUrl("https://www.kapiolani.hawaii.edu/event-directory/summer-session-2025/")
        //
        .setTitle("Kapiʻolani CC Summer Session: Save Money, Graduate Faster!")
        //
        .setThumbnail("https://www.kapiolani.hawaii.edu/wp-content/uploads/studentgreatlawn2025_1920.jpg")
        //
        .setContent("Summer is the perfect time to fast-track your education and save money! ...");
    blogs.add(webPageContent);
    DiscoverResultVo discoverResultVo = new DiscoverResultVo(blogs);
    return discoverResultVo;
  }
}
