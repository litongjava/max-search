package com.litongjava.max.search.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.hook.HookCan;
import com.litongjava.maxkb.playwright.PlaywrightBrowser;
import com.litongjava.tio.utils.environment.EnvUtils;

import lombok.extern.slf4j.Slf4j;

//@AConfiguration
@Slf4j
public class SearchPlaywrightConfig {

  @Initialization
  public void config() {
    if (EnvUtils.getBoolean("playwright.enable", true)) {
      // 启动
      log.info("start init playwright");
      PlaywrightBrowser.init();
      log.info("end init playwright");

      // 服务关闭时，自动关闭浏览器和 Playwright 实例
      HookCan.me().addDestroyMethod(() -> {
        PlaywrightBrowser.close();
      });
    }
  }
}