package com.litongjava.perplexica.can;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;

public class ChatWsStreamCallCan {
  public static Map<String, Call> callMap = new ConcurrentHashMap<>();

  /**
   * 停止特定会话的推理过程
   *
   * @param id 会话ID
   * @return 被停止的Call对象
   */
  public static Call stop(String id) {
    Call call = callMap.get(id);
    if (call != null && !call.isCanceled()) {
      call.cancel();
      return callMap.remove(id);
    }
    return null;
  }

  /**
   * 移除特定会话的Call对象
   *
   * @param id 会话ID
   * @return 被移除的Call对象
   */
  public static Call remove(String id) {
    return callMap.remove(id);
  }

  /**
   * 添加会话的Call对象
   *
   * @param chatId 会话ID
   * @param call   Call对象
   */
  public static void put(String chatId, Call call) {
    callMap.put(chatId, call);
  }
}
