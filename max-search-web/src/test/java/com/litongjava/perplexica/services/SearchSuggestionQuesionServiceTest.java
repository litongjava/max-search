package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.litongjava.chat.UniChatMessage;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.max.search.config.EnjoyEngineConfig;
import com.litongjava.max.search.services.SearchSuggestionQuesionService;
import com.litongjava.tio.utils.environment.EnvUtils;

public class SearchSuggestionQuesionServiceTest {

  @Test
  public void test() {
    // 加载环境变量或配置信息
    EnvUtils.load();
    // 配置模板引擎
    new EnjoyEngineConfig().config();

    // 构造一段对话历史
    List<UniChatMessage> histories = new ArrayList<>();
    histories.add(UniChatMessage.buildUser("How was the professor Tong Li"));

    // 调用 SearchSuggestionQuesionService 进行建议问生成
    String generate = Aop.get(SearchSuggestionQuesionService.class).generate(histories);

    // 输出结果
    System.out.println(generate);
  }
}
