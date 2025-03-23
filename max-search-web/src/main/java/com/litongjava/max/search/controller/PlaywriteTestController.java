package com.litongjava.max.search.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.maxkb.playwright.PlaywrightBrowser;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.tio.utils.thread.TioThreadUtils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import lombok.extern.slf4j.Slf4j;

@RequestPath("/playwrite")
@Slf4j
public class PlaywriteTestController {

  LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(false);

  public RespBodyVo newContext() {
    String link1 = "https://studentservices.stanford.edu/calendar/academic-dates/stanford-academic-calendar-2024-2025";
    //显示网页
    for (int i = 0; i < 3; i++) {
      try (Playwright playwright = Playwright.create()) {
        BrowserType chromium = playwright.chromium();
        try (Browser broswer = chromium.launch(launchOptions);) {
          try (BrowserContext context = broswer.newContext()) {
            try (Page page = context.newPage()) {
              //显示窗口
              page.navigate(link1);
              String bodyText = page.innerText("body");
            } catch (Exception e) {
              log.error("Error getting content from {}: {}", link1, e.getMessage(), e);
            }
          }
        }
      }
    }

    try {
      Thread.sleep(200000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return RespBodyVo.ok();
  }

  public RespBodyVo newContext2() {
    String link1 = "https://studentservices.stanford.edu/calendar/academic-dates/stanford-academic-calendar-2024-2025";
    //模拟20个用户
    for (int i = 1; i < 20; i++) {
      TioThreadUtils.submit(() -> {
        //显示网页
        for (int j = 0; j < 3; j++) {
          BrowserContext context = PlaywrightBrowser.acquire();
          try (Page page = context.newPage()) {
            //显示窗口
            page.navigate(link1);
            String bodyText = page.innerText("body");
          } catch (Exception e) {
            log.error("Error getting content from {}: {}", link1, e.getMessage(), e);
          }
          PlaywrightBrowser.release(context);
        }

        try {
          Thread.sleep(200000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      });
    }

    return RespBodyVo.ok();
  }

}
