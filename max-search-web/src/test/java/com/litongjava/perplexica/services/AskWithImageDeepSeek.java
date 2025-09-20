package com.litongjava.perplexica.services;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.litongjava.openai.chat.ChatMessageContent;
import com.litongjava.openai.chat.ChatRequestImage;
import com.litongjava.openai.chat.ChatResponseMessage;
import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.siliconflow.SiliconFlowConsts;
import com.litongjava.siliconflow.SiliconFlowModels;
import com.litongjava.tio.utils.base64.Base64Utils;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.http.ContentTypeUtils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.hutool.ResourceUtil;
import com.litongjava.tio.utils.json.JsonUtils;

public class AskWithImageDeepSeek {

  @Test
  public void imageToMarkDown() {
    EnvUtils.load();
    String apiKey = EnvUtils.getStr("SILICONFLOW_API_KEY");

    String prompt = "Convert the image to text and just output the text.";

    String filePath = "images/200-dpi.png";
    URL url = ResourceUtil.getResource(filePath);
    byte[] imageBytes = FileUtil.readBytes(url);
    String suffix = FilenameUtils.getSuffix(filePath);
    String mimeType = ContentTypeUtils.getContentType(suffix);

    String imageBase64 = Base64Utils.encodeImage(imageBytes, mimeType);

    ChatRequestImage chatRequestImage = new ChatRequestImage();
    chatRequestImage.setUrl(imageBase64);

    List<ChatMessageContent> multiContents = new ArrayList<>();
    multiContents.add(new ChatMessageContent(chatRequestImage));
    multiContents.add(new ChatMessageContent(prompt));

    OpenAiChatMessage userMessage = new OpenAiChatMessage();
    userMessage.role("user").multiContents(multiContents);

    List<OpenAiChatMessage> messages = new ArrayList<>();
    messages.add(userMessage);

    OpenAiChatRequestVo chatRequestVo = new OpenAiChatRequestVo();
    //DEEPSEEK_R1 is not a VLM (Vision Language Model). Please use text-only prompts.
    //DeepSeek-V3 not working
    //DEEPSEEK_VL2 working but not well
    chatRequestVo.setModel(SiliconFlowModels.DEEPSEEK_VL2);
    //better for ocr
    chatRequestVo.setMax_tokens(1024).setTemperature(0.7f).setTop_p(0.7f).setFrequency_penalty(0f);

    chatRequestVo.setMessages(messages);
    String json = JsonUtils.toSkipNullJson(chatRequestVo);
    //System.out.println("Request JSON:\n" + json);

    OpenAiChatResponseVo chatResponse = OpenAiClient.chatCompletions(SiliconFlowConsts.SELICONFLOW_API_BASE, apiKey, chatRequestVo);
    ChatResponseMessage responseMessage = chatResponse.getChoices().get(0).getMessage();
    String content = responseMessage.getContent();
    System.out.println("Response Content:\n" + content);
  }
}