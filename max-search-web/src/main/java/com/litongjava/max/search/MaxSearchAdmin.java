package com.litongjava.max.search;

import com.litongjava.annotation.AComponentScan;
import com.litongjava.tio.boot.TioApplication;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Playwright;

@AComponentScan
public class MaxSearchAdmin {
  public static void main(String[] args) {
    boolean download = false;
    for (String string : args) {
      if ("--download".equals(string)) {
        download = true;
        break;
      }
    }
    if (download) {
      System.out.println("download start");
      LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(true); // 使用无头模式
      try (Playwright playwright = Playwright.create(); Browser launch = playwright.chromium().launch(launchOptions)) {
      }
      System.out.println("download end");
    } else {
      long start = System.currentTimeMillis();
      //HotSwapResolver.addSystemClassPrefix("om.litongjava.perplexica.vo.");
      TioApplication.run(MaxSearchAdmin.class, args);
      long end = System.currentTimeMillis();
      System.out.println((end - start) + "ms");
    }
  }
}