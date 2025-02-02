package com.litongjava.perplexica.services;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.litongjava.openai.chat.ChatMesageContent;
import com.litongjava.openai.chat.ChatRequestImage;
import com.litongjava.openai.chat.ChatResponseMessage;
import com.litongjava.openai.chat.OpenAiChatMessage;
import com.litongjava.openai.chat.OpenAiChatRequestVo;
import com.litongjava.openai.chat.OpenAiChatResponseVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.constants.OpenAiModels;
import com.litongjava.tio.utils.encoder.Base64Utils;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.http.ContentTypeUtils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.hutool.ResourceUtil;
import com.litongjava.tio.utils.json.JsonUtils;

public class AskWithImageOpenai {

  @Test
  public void imageToMarkDown() {
    EnvUtils.load();
    String apiKey = EnvUtils.getStr("OPENAI_API_KEY");

    String prompt = "Convert the image to text and just output the text.";

    String filePath = "images/200-dpi.png";
    URL url = ResourceUtil.getResource(filePath);
    byte[] imageBytes = FileUtil.readUrlAsBytes(url);
    String suffix = FilenameUtils.getSuffix(filePath);
    String mimeType = ContentTypeUtils.getContentType(suffix);

    String imageBase64 = Base64Utils.encodeImage(imageBytes, mimeType);

    ChatRequestImage chatRequestImage = new ChatRequestImage();
    chatRequestImage.setDetail("auto");
    chatRequestImage.setUrl(imageBase64);

    List<ChatMesageContent> multiContents = new ArrayList<>();
    multiContents.add(new ChatMesageContent(prompt));
    multiContents.add(new ChatMesageContent(chatRequestImage));

    OpenAiChatMessage userMessage = new OpenAiChatMessage();
    userMessage.role("user").multiContents(multiContents);

    List<OpenAiChatMessage> messages = new ArrayList<>();
    messages.add(userMessage);

    OpenAiChatRequestVo chatRequestVo = new OpenAiChatRequestVo();
    chatRequestVo.setModel(OpenAiModels.GPT_4O_MINI);
    chatRequestVo.setMessages(messages);
    String json = JsonUtils.toSkipNullJson(chatRequestVo);
    System.out.println("Request JSON:\n" + json);

    OpenAiChatResponseVo chatResponse = OpenAiClient.chatCompletions(apiKey, chatRequestVo);
    ChatResponseMessage responseMessage = chatResponse.getChoices().get(0).getMessage();
    String content = responseMessage.getContent();
    System.out.println("Response Content:\n" + content);
  }
}