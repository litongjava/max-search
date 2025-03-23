package com.litongjava.perplexica.services;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Test;

import com.litongjava.gemini.GeminiPartVo;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.perplexica.config.SearchPlaywrightConfig;
import com.litongjava.tio.boot.testing.TioBootTest;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.FileUtil;

public class PlaywrightServiceTest {

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Test
  public void test() {
    TioBootTest.runWith(SearchPlaywrightConfig.class);
    List<String> links = new ArrayList<>();
    links.add("http://www.ce.cn/xwzx/gnsz/gdxw/202501/28/t20250128_39280413.shtml");
    links.add("https://app.xinhuanet.com/news/article.html?articleId=8bbb90d749df20a4c7b98107f32e3cc9");
    links.add("https://culture.gmw.cn/2025-01/28/content_37826231.htm");

    List<byte[]> images = Aop.get(PlaywrightService.class).convertToPdfAsync(links);

    String googleApiKey = EnvUtils.getStr("GEMINI_API_KEY");
    String mimeType = "image/png";
    List<GeminiPartVo> parts = new ArrayList<>();
    parts.add(new GeminiPartVo("根据材料回答问题,问题是: 2025春晚节目单"));
    for (int i = 0; i < images.size(); i++) {
      byte[] bs = images.get(i);
      FileUtil.writeBytes(bs, new File(i + ".pdf"));
      // String encodeImage = Base64Utils.encodeToString(bs);
      // add images
      // parts.add(new GeminiPartVo(new GeminiInlineDataVo(mimeType, encodeImage)));
    }

//    GeminiContentVo content = new GeminiContentVo("user", parts);
//    GeminiChatRequestVo reqVo = new GeminiChatRequestVo(Collections.singletonList(content));
//    // 2. 同步请求：generateContent
//    GeminiChatResponseVo respVo = GeminiClient.generate(googleApiKey, GoogleGeminiModels.GEMINI_2_0_FLASH_EXP, reqVo);
//    if (respVo != null && respVo.getCandidates() != null) {
//      respVo.getCandidates().forEach(candidate -> {
//        if (candidate.getContent() != null && candidate.getContent().getParts() != null) {
//          candidate.getContent().getParts().forEach(partVo -> {
//            System.out.println("Gemini answer text: " + partVo.getText());
//          });
//        }
//      });
//    }
  }
}
