package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.deepseek.DeepSeekConst;
import com.litongjava.deepseek.DeepSeekModels;
import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

public class DeepSeekClientTest {

  public static void main(String[] args) {
    EnvUtils.load();
    List<OpenAiChatMessage> messages = new ArrayList<>();
    messages.add(new OpenAiChatMessage("user", "Hi"));

    OpenAiChatRequestVo chatRequestVo = new OpenAiChatRequestVo()
        //
        .setModel(DeepSeekModels.DEEPSEEK_REASONER)
        //
        .setMessages(messages).setMax_tokens(8000);

    String apiKey = EnvUtils.getStr("DEEPSEEK_API_KEY");
    OpenAiChatResponseVo chatResponse = OpenAiClient.chatCompletions(DeepSeekConst.BASE_URL, apiKey, chatRequestVo);
    System.out.println(JsonUtils.toSkipNullJson(chatResponse));
  }
}
