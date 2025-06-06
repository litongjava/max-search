
package com.litongjava.max.search.config;

import com.jfinal.template.Engine;
import com.litongjava.tio.utils.environment.EnvUtils;

public class EnjoyEngineConfig {

  private final String RESOURCE_BASE_PATH = "/enjoy-template/";

  public void config() {
    Engine engine = Engine.use();
    engine.setBaseTemplatePath(RESOURCE_BASE_PATH);
    engine.setToClassPathSourceFactory();
    if (EnvUtils.isDev()) {
      // 支持模板热加载，绝大多数生产环境下也建议配置成 true，除非是极端高性能的场景
      engine.setDevMode(true);
    }

    // 配置极速模式，性能提升 13%
    Engine.setFastMode(true);
    // jfinal 4.9.02 新增配置：支持中文表达式、中文变量名、中文方法名、中文模板函数名
    Engine.setChineseExpression(true);

  }

}