package com.litongjava.perplexica.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.litongjava.openai.chat.ChatResponseDelta;
import com.litongjava.openai.chat.Choice;
import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.siliconflow.SiliconFlowConsts;
import com.litongjava.siliconflow.SiliconFlowModels;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.FastJson2Utils;
import com.litongjava.tio.utils.json.JsonUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

@Slf4j
public class SiliconFlowDeepSeekStreamTest {
  public static void main(String[] args) {
    // 从配置中加载 OPENAI_API_KEY
    EnvUtils.load();
    String apiKey = EnvUtils.getStr("SILICONFLOW_API_KEY");

    // 创建消息列表
    List<OpenAiChatMessage> messages = new ArrayList<>();
    messages.add(new OpenAiChatMessage("user", "How are you?"));

    // 创建聊天请求
    OpenAiChatRequestVo chatRequestVo = new OpenAiChatRequestVo();
    chatRequestVo.setStream(true);
    chatRequestVo.setModel(SiliconFlowModels.DEEPSEEK_R1);
    chatRequestVo.setMessages(messages);

    String json = JsonUtils.toSkipNullJson(chatRequestVo);
    System.out.println("请求 JSON:\n" + json);

    Call call = OpenAiClient.chatCompletions(SiliconFlowConsts.SELICONFLOW_API_BASE, apiKey, json, new Callback() {

      @Override
      public void onResponse(Call arg0, Response response) throws IOException {
        if (!response.isSuccessful()) {
          String string = response.body().string();
          String message = "Chat model response an unsuccessful message:" + string;
          log.error("message:{}", message);
          return;
        }

        onResponseSuccess(response);

      }

      @Override
      public void onFailure(Call arg0, IOException arg1) {
      }
    });
    log.info("call:{}", call);
  }
  
  private static void onResponseSuccess(Response response) throws IOException {
    try (ResponseBody responseBody = response.body()) {
      StringBuffer completionContent = new StringBuffer();
      BufferedSource source = responseBody.source();
      String line;

      while ((line = source.readUtf8Line()) != null) {
        if (line.length() < 1) {
          continue;
        }
        // 处理数据行
        if (line.length() > 6) {
          String data = line.substring(6);
          if (data.endsWith("}")) {
            OpenAiChatResponseVo chatResponse = FastJson2Utils.parse(data, OpenAiChatResponseVo.class);
            List<Choice> choices = chatResponse.getChoices();
            if (!choices.isEmpty()) {
              ChatResponseDelta delta = choices.get(0).getDelta();
              String part = delta.getContent();
              if (part != null && !part.isEmpty()) {
                completionContent.append(part);
              }
              String reasoning_content = delta.getReasoning_content();
              if (reasoning_content != null && !reasoning_content.isEmpty()) {
                log.info("reasoning_content:{}",reasoning_content);
              }
            }
          } else if (": keep-alive".equals(line)) {
            log.info("keep-alive");
          } else {
            log.info("Data does not end with }:{}", line);

          }
        }
      }
      log.info("completionContent:{}",completionContent);
    }
  }
}