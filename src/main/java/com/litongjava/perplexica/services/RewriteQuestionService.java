package com.litongjava.perplexica.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jfinal.template.Template;
import com.litongjava.gemini.GeminiClient;
import com.litongjava.gemini.GoogleGeminiModels;
import com.litongjava.openai.chat.ChatMessage;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.utils.environment.EnvUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RewriteQuestionService {

  public String rewrite(String question, List<ChatMessage> messages) {

    // 1.渲染模版
    Template template = PromptEngine.getTemplate("rewrite_question_prompt.txt");

    Map<String, Object> values = new HashMap<>();
    if (messages != null) {
      values.put("messages", messages);
    }
    values.put("query", question);
    String prompt = template.renderToString(values);

    log.info("prompt:{}", prompt);

    // 2.大模型推理
    //String content = openAi(prompt);
    String content = gemini(prompt);
    // 3.返回推理结果
    return content;
  }

  private String gemini(String prompt) {
    String content;
    String apiKey = EnvUtils.getStr("GEMINI_API_KEY");
    content = GeminiClient.chatWithModel(apiKey, GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, "user", prompt);
    return content;
  }

  private String openAi(String prompt) {
    OpenAiChatResponseVo chatCompletions = OpenAiClient.chatWithRole("system", prompt);
    String content = chatCompletions.getChoices().get(0).getMessage().getContent();
    return content;
  }
}