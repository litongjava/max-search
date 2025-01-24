package com.litongjava.perplexica.instance;

import org.junit.Test;

import com.litongjava.tio.utils.thread.TioThreadUtils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.BrowserType.LaunchOptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlaywrightBrowserTest {
  @Test
  public void test10() {
    String link1 = "https://studentservices.stanford.edu/calendar/academic-dates/stanford-academic-calendar-2024-2025";
    LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(false);
    Playwright playwright = Playwright.create();
    Browser broswer = playwright.chromium().launch(launchOptions);
    BrowserContext context = broswer.newContext();
    //显示网页
    log.info("context:{}", context);
    for (int i = 0; i < 10; i++) {
      try {
        Page page = context.newPage();
        //显示窗口
        page.navigate(link1);
        String bodyText = page.innerText("body");
      } catch (Exception e) {
        log.error("Error getting content from {}: {}", link1, e.getMessage(), e);
      }
    }

    try {
      Thread.sleep(200000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testGetPdf() {
    //com.microsoft.playwright.PlaywrightException: Error {message='net::ERR_ABORTED at https://www.sjsu.edu/provost/docs/Academic-Calendar-2024-25.pdf
    String url = "https://www.sjsu.edu/provost/docs/Academic-Calendar-2024-25.pdf";
    String content = PlaywrightBrowser.getHtml(url);
    System.out.println(content);
  }

  @Test
  public void test3() {
    String link1 = "https://studentservices.stanford.edu/calendar/academic-dates/stanford-academic-calendar-2024-2025";
    String link2 = "https://approaching.stanford.edu/parents-guardians/academic-calendar";
    String link3 = "https://studentservices.stanford.edu/calendar/academic-dates/previous_calendars/stanford-academic-calendar-2023-24";

    TioThreadUtils.submit(() -> {
      try (BrowserContext context = PlaywrightBrowser.acquire();) {
        Thread.sleep(4000);
        try (Page page = context.newPage()) {
          page.navigate(link1);
          String bodyText = page.innerText("body");
          return bodyText;
        } catch (Exception e) {
          log.error("Error getting content from {}: {}", link1, e.getMessage(), e);
          return "";
        }
      }
    });

    TioThreadUtils.submit(() -> {
      try (BrowserContext context = PlaywrightBrowser.acquire()) {
        Thread.sleep(4000);
        try (Page page = context.newPage()) {
          page.navigate(link2);
          String bodyText = page.innerText("body");
          Thread.sleep(4000);
          return bodyText;
        } catch (Exception e) {
          log.error("Error getting content from {}: {}", link1, e.getMessage(), e);
          return "";
        }
      }
    });

    TioThreadUtils.submit(() -> {
      try (BrowserContext context = PlaywrightBrowser.acquire();) {
        Thread.sleep(4000);
        try (Page page = context.newPage()) {
          page.navigate(link3);
          String bodyText = page.innerText("body");
          Thread.sleep(4000);
          return bodyText;
        } catch (Exception e) {
          log.error("Error getting content from {}: {}", link1, e.getMessage(), e);
          return "";
        }
      }
    });

    try {
      Thread.sleep(200000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

}
