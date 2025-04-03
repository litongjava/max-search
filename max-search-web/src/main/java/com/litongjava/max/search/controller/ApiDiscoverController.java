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
        .setUrl("https://www.yahoo.com/tech/chinese-research-team-makes-game-103007466.html")
        //
        .setTitle("Chinese research team makes game-changing breakthrough with floating solar tech: 'Our work made significant progress")
        //
        .setThumbnail("https://www.bing.com//th?id=OVFT.J2ES8dqSTKgpQ0RBA2V0XC&pid=News&w=234&h=132&c=14&rs=2&qlt=90")
        //
        .setContent("Our work made significant progress in understanding the hydrodynamic responses of a novel offshore floating photovoltaic ...");
    blogs.add(webPageContent);
    DiscoverResultVo discoverResultVo = new DiscoverResultVo(blogs);
    return discoverResultVo;
    
  }
}
