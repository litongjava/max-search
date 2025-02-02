package com.litongjava.perplexica.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.siliconflow.SiliconFlowConsts;
import com.litongjava.siliconflow.SiliconFlowModels;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

import okhttp3.Response;

public class SiliconFlowDeepSeekTest {
  public static void main(String[] args) {
    // Load OPENAI_API_KEY from configuration
    EnvUtils.load();
    String apiKey = EnvUtils.getStr("SILICONFLOW_API_KEY");

    // Create messages
    List<OpenAiChatMessage> messages = new ArrayList<>();
    messages.add(new OpenAiChatMessage("user", "How are you?"));

    // Create chat request
    OpenAiChatRequestVo chatRequestVo = new OpenAiChatRequestVo();
    chatRequestVo.setStream(false);
    chatRequestVo.setModel(SiliconFlowModels.DEEPSEEK_R1);
    chatRequestVo.setMessages(messages);

    String json = JsonUtils.toSkipNullJson(chatRequestVo);
    System.out.println("Request JSON:\n" + json);

    // Send HTTP request
    try (Response response = OpenAiClient.chatCompletions(SiliconFlowConsts.SELICONFLOW_API_BASE, apiKey, json)) {
      if (response.isSuccessful()) {
        String responseBody = response.body().string();
        OpenAiChatResponseVo chatCompletions = JsonUtils.parse(responseBody, OpenAiChatResponseVo.class);
        System.out.println("Response:\n" + JsonUtils.toSkipNullJson(chatCompletions));
      } else {
        System.err.println("Request failed: " + response);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
