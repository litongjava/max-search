package com.litongjava.max.search.services;

import java.util.HashMap;
import java.util.Map;

import com.jfinal.template.Template;
import com.litongjava.gemini.GeminiClient;
import com.litongjava.gemini.GoogleModels;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.utils.environment.EnvUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxSearchSummaryQuestionService {

  public String summary(String question) {
    // 1. 渲染模板
    Template template = PromptEngine.getTemplate("summary_question_prompt.txt");
    Map<String, Object> values = new HashMap<>();
    values.put("query", question);
    String prompt = template.renderToString(values);
    // 2. 调用大模型进行推理
    //String content = useOpenAi(prompt);
    log.info("use gemini");
    String content = useGemini(prompt);
    // 3. 判断结果并返回
    if ("not_needed".equals(content)) {
      return question; // 或者根据实际需求，直接返回 "not_needed"
    }
    return content;
  }

  private String useGemini(String prompt) {
    String apiKey = EnvUtils.getStr("GEMINI_API_KEY");
    return GeminiClient.chatWithModel(apiKey, GoogleModels.GEMINI_2_0_FLASH, "user", prompt);
  }

  private String useOpenAi(String prompt) {
    OpenAiChatResponseVo chatCompletions = OpenAiClient.chatWithRole("user", prompt);
    String content = chatCompletions.getChoices().get(0).getMessage().getContent();
    return content;
  }
}
