package com.litongjava.perplexica.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.litongjava.maxkb.playwright.PlaywrightBrowser;
import com.litongjava.perplexica.vo.ChatWsRespVo;
import com.litongjava.perplexica.vo.CitationsVo;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.thread.TioThreadUtils;
import com.litongjava.tio.websocket.common.WebSocketResponse;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpiderService {

  public StringBuffer spider(ChannelContext channelContext, long answerMessageId, List<CitationsVo> citationList) {
    ChatWsRespVo<String> vo;
    WebSocketResponse websocketResponse;
    //5.获取内容
    StringBuffer pageContents = new StringBuffer();
    for (int i = 0; i < citationList.size(); i++) {
      String link = citationList.get(i).getLink();
      String suffix = FilenameUtils.getSuffix(link);
      if ("pdf".equals(suffix)) {
        log.info("skip:{}", suffix);
      } else {
        String bodyText = null;
        try {
          bodyText = PlaywrightBrowser.getBodyContent(link);
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          vo = ChatWsRespVo.message(answerMessageId, "Error Failed to get " + link + " " + e.getMessage());
          websocketResponse = WebSocketResponse.fromJson(vo);
          if (channelContext != null) {
            Tio.bSend(channelContext, websocketResponse);
          }
          continue;
        }
        pageContents.append("source " + (i + 1) + " " + bodyText).append("\n\n");
      }
    }
    return pageContents;
  }

  public StringBuffer spiderAsync(ChannelContext channelContext, long answerMessageId, List<CitationsVo> citationList) {
    List<Future<String>> futures = new ArrayList<>();

    for (int i = 0; i < citationList.size(); i++) {
      final int index = i;
      final String link = citationList.get(i).getLink();

      Future<String> future = TioThreadUtils.submit(() -> {
        String suffix = FilenameUtils.getSuffix(link);
        if ("pdf".equalsIgnoreCase(suffix)) {
          log.info("skip:{}", suffix);
          return "";
        } else {
          BrowserContext context = PlaywrightBrowser.acquire();
          try (Page page = context.newPage()) {
            page.navigate(link);
            String bodyText = page.innerText("body");
            return "source " + (index + 1) + " " + bodyText + "\n\n";
          } catch (Exception e) {
            log.error("Error getting content from {}: {}", link, e.getMessage(), e);
            ChatWsRespVo<String> vo = ChatWsRespVo.message(answerMessageId, "Error Failed to get " + link + " " + e.getMessage());
            WebSocketResponse websocketResponse = WebSocketResponse.fromJson(vo);
            if (channelContext != null) {
              Tio.bSend(channelContext, websocketResponse);
            }
            return "";
          } finally {
            PlaywrightBrowser.release(context);
          }
        }
      });
      futures.add(future);
    }

    StringBuffer pageContents = new StringBuffer();
    for (Future<String> future : futures) {
      try {
        String result = future.get();
        if (result != null) {
          pageContents.append(result);
        }
      } catch (InterruptedException | ExecutionException e) {
        log.error("Error retrieving task result: {}", e.getMessage(), e);
      }
    }

    return pageContents;
  }
}
