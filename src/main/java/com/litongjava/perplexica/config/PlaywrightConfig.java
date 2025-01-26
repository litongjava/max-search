package com.litongjava.perplexica.config;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.hook.HookCan;
import com.litongjava.perplexica.instance.PlaywrightBrowser;

import lombok.extern.slf4j.Slf4j;

@AConfiguration
@Slf4j
public class PlaywrightConfig {

  @Initialization
  public void config() {
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