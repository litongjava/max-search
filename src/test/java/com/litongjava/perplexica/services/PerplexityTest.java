package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.constants.PerplexityConstants;
import com.litongjava.openai.constants.PerplexityModels;
import com.litongjava.tio.utils.environment.EnvUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerplexityTest {

  @Test
  public void testPerplexityChat() {
    // Load configuration
    EnvUtils.load();

    // Create messages
    List<OpenAiChatMessage> messages = new ArrayList<>();
    messages.add(new OpenAiChatMessage("user", "How are you?"));

    // Create chat request
    OpenAiChatRequestVo chatRequestVo = new OpenAiChatRequestVo().setModel(PerplexityModels.llama_3_1_sonar_small_128k_online).setMessages(messages).setMax_tokens(3000);

    String apiKey = EnvUtils.get("PERPLEXITY_API_KEY");
    log.info("apiKey:{}",apiKey);

    // Send HTTP request to Perplexity server
    OpenAiChatResponseVo chatResponse = OpenAiClient.chatCompletions(PerplexityConstants.SERVER_URL, apiKey, chatRequestVo);
    String content = chatResponse.getChoices().get(0).getMessage().getContent();
    System.out.println("Response Content:\n" + content);
  }
}