package com.litongjava.perplexica.services;

import com.litongjava.gemini.GoogleModels;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

public class GeminiClientTest {
  public static void main(String[] args) {
    EnvUtils.load();
    OpenAiChatResponseVo chatResponse = OpenAiClient.chatWithModel(GoogleModels.GEMINI_2_0_FLASH_EXP, "user", "how are you");
    System.out.println(JsonUtils.toJson(chatResponse));
  }
}
