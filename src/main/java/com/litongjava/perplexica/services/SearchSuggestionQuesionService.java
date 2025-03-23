package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.gemini.GeminiChatRequestVo;
import com.litongjava.gemini.GeminiChatResponseVo;
import com.litongjava.gemini.GeminiClient;
import com.litongjava.gemini.GeminiContentVo;
import com.litongjava.gemini.GeminiPartVo;
import com.litongjava.gemini.GeminiSystemInstructionVo;
import com.litongjava.gemini.GoogleGeminiModels;
import com.litongjava.openai.chat.ChatMessage;
import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.constants.OpenAiModels;
import com.litongjava.template.PromptEngine;
import com.litongjava.tio.utils.tag.TagUtils;

public class SearchSuggestionQuesionService {
  private String prompt = PromptEngine.renderToString("suggestion_generator_prompt.txt");

  public String generate(List<ChatMessage> histories) {

    // 调用大模型，传入模型名称、Prompt 与对话历史
    String content = useGemini(prompt, histories);

    // 使用 TagUtils 工具类从返回文本中提取 <suggestions> ... </suggestions> 内容
    return TagUtils.extractSuggestions(content).get(0);
  }

  public String useGemini(String prompt, List<ChatMessage> histories) {

    List<GeminiContentVo> contents = new ArrayList<>();
    // 1. 如果有对话历史，则构建 role = user / model 的上下文内容
    for (int i = 0; i < histories.size(); i++) {
      ChatMessage chatMessage = histories.get(i);
      String role = chatMessage.getRole();
      if ("human".equals(role)) {
        role = "user";
      } else {
        role = "model";
      }
      contents.add(new GeminiContentVo(role, chatMessage.getContent()));
    }

    GeminiChatRequestVo reqVo = new GeminiChatRequestVo(contents);

    GeminiPartVo geminiPartVo = new GeminiPartVo(prompt);
    GeminiSystemInstructionVo geminiSystemInstructionVo = new GeminiSystemInstructionVo();
    geminiSystemInstructionVo.setParts(geminiPartVo);

    reqVo.setSystem_instruction(geminiSystemInstructionVo);
    GeminiChatResponseVo generate = GeminiClient.generate(GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, reqVo);
    return generate.getCandidates().get(0).getContent().getParts().get(0).getText();
  }

  public String useOpenAi(String prompt, List<ChatMessage> histories) {
    List<OpenAiChatMessage> messages = new ArrayList<>();
    for (ChatMessage item : histories) {
      messages.add(new OpenAiChatMessage(item.getRole(), item.getContent()));
    }
    OpenAiChatResponseVo chatResponseVo = OpenAiClient.chatCompletions(OpenAiModels.GPT_4O_MINI, prompt, messages);
    String content = chatResponseVo.getChoices().get(0).getMessage().getContent();
    return content;
  }
}