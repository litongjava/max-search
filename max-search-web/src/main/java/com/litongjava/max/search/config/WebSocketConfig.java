package com.litongjava.max.search.config;
import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.max.search.handler.ChatWebSocketHandler;
import com.litongjava.max.search.handler.HelloWebSocketHandler;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.boot.websocket.WebSocketRouter;

@AConfiguration
public class WebSocketConfig {

  @Initialization
  public void config() {
    WebSocketRouter r = TioBootServer.me().getWebSocketRouter();
    r.add("/hello", new HelloWebSocketHandler());
    r.add("/", new ChatWebSocketHandler());
  }
}

