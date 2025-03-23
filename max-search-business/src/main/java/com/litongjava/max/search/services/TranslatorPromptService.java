package com.litongjava.max.search.services;

import com.litongjava.template.PromptEngine;
import com.litongjava.tio.core.ChannelContext;

// @Slf4j
public class TranslatorPromptService {
  public String genInputPrompt(ChannelContext channelContext, String content, Boolean copilotEnabled,
      //
      Long messageId, Long answerMessageId, String from) {

    return PromptEngine.renderToString("translator_prompt.txt");
    // 根据文本内容设置源语言和目标语言
    //    String srcLang;
    //    String destLang;
    //
    //    if (ChineseUtils.containsChinese(content)) {
    //      srcLang = "Chinese";
    //      destLang = "English";
    //    } else {
    //      srcLang = "English";
    //      destLang = "Chinese";
    //    }
    //    Kv set = Kv.by("src_lang", srcLang).set("dst_lang", destLang);
    //    set.set("source_text", content);
    //    String renderToString = PromptEngine.renderToString("translator_prompt.txt", set);
    //    log.info("prompt:{}", renderToString);
    //    return renderToString;
  }
}
