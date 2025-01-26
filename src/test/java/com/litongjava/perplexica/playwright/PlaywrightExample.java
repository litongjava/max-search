package com.litongjava.perplexica.playwright;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class PlaywrightExample {
  public static void main(String[] args) {
    String url = "https://www.instagram.com/collegebot.ai/";

    // 创建 Playwright 实例
    try (Playwright playwright = Playwright.create()) {
      // 启动 Chromium 浏览器
      BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(false);
      Browser browser = playwright.chromium().launch(launchOptions);

      // 创建新浏览器上下文和页面
      BrowserContext context = browser.newContext();
      Page page = context.newPage();

      // 导航到指定网页
      page.navigate(url);

      // 截取页面截图
      Page.ScreenshotOptions screenshotOptions = new Page.ScreenshotOptions().setPath(Paths.get("example.png"));
      page.screenshot(screenshotOptions);

      // 获取页面内容并写入文件
      String content = page.content();
      try {
        Files.write(Paths.get("remote_page.html"), content.getBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }

      // 提示用户按下回车键以关闭浏览器
      System.out.println("操作完成。按下回车键以关闭浏览器...");
      try {
        System.in.read();
      } catch (IOException e) {
        e.printStackTrace();
      }

      // 关闭浏览器
      browser.close();
    }
  }
}
