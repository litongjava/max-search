package com.litongjava.max.search.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jfinal.kit.Kv;
import com.litongjava.max.search.utils.OpenAiResponseUtils;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.openai.client.OpenAiClient;
import com.litongjava.openai.consts.OpenAiModels;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.utils.OkHttpResponseUtils;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.encoder.ChunkEncoder;
import com.litongjava.tio.http.common.sse.ChunkedPacket;
import com.litongjava.tio.http.server.model.HttpCors;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.http.server.util.SseEmitter;
import com.litongjava.tio.utils.json.FastJson2Utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
public class OpenAiV1ProxyHandler {

  public HttpResponse models(HttpRequest request) {
    /**
    {
      "object": "list",
      "data": [
        {
          "id": "dall-e-3",
          "object": "model",
          "created": 1698785189,
          "owned_by": "system"
        },
      ]
    ]
    */
    HttpResponse httpResponse = TioRequestContext.getResponse();
    CORSUtils.enableCORS(httpResponse, new HttpCors());

    Kv model = Kv.create();
    model.set("id", "hawaii.edu");
    model.set("object", "model");
    model.set("created", 1698785189);
    model.set("owned_by", "system");

    List<Kv> models = new ArrayList<>(1);
    models.add(model);

    Kv resp = Kv.create();
    resp.set("object", "list");
    resp.set("data", models);

    return httpResponse.setJson(resp);
  }

  public HttpResponse completions(HttpRequest httpRequest) {
    long start = System.currentTimeMillis();
    HttpResponse httpResponse = TioRequestContext.getResponse();

    String requestURI = httpRequest.getRequestURI();

    Map<String, String> headers = httpRequest.getHeaders();
    String bodyString = httpRequest.getBodyString();
    //log.info("requestURI:{},header:{},bodyString:{}", requestURI, headers, bodyString);
    log.info("bodyString:{}", bodyString);

    // 替换基本的一些值
    //String authorization = EnvUtils.get("OPENAI_API_KEY");
    //headers.put("authorization", "Bearer " + authorization);
    headers.put("host", "api.openai.com");

    Boolean stream = true;
    JSONObject openAiRequestVo = null;
    if (bodyString != null) {
      openAiRequestVo = FastJson2Utils.parseObject(bodyString);
      stream = openAiRequestVo.getBoolean("stream");
      openAiRequestVo.put("model", OpenAiModels.GPT_4O_MINI);
    }

    if (stream != null && stream) {
      if (openAiRequestVo != null) {
        // 告诉默认的处理器不要将消息体发送给客户端,因为后面会手动发送
        httpResponse.setSend(false);
        ChannelContext channelContext = httpRequest.getChannelContext();
        streamResponse(channelContext, httpResponse, headers, openAiRequestVo, start);
      } else {
        return httpResponse.setJson(RespBodyVo.fail("empty body"));
      }

      // test(channelContext);
      // 无需移除
      // Tio.remove(channelContext, "remove");
    } else {
      Response response = OpenAiClient.chatCompletions(headers, openAiRequestVo.toString());
      OkHttpResponseUtils.toTioHttpResponse(response, httpResponse);
      httpResponse.setHasGzipped(true);
      httpResponse.removeHeaders("Transfer-Encoding");
      httpResponse.removeHeaders("Server");
      httpResponse.removeHeaders("Date");
      httpResponse.setHeader("Connection", "close");
      httpResponse.removeHeaders("Set-Cookie");

      long end = System.currentTimeMillis();
      //log.info("finish llm in {} (ms):", (end - start));
    }

    return httpResponse;
  }

  /**
   * 流式请求和响应
   *
   * @param channelContext
   * @param httpResponse
   * @param headers
   * @param start
   */
  private void streamResponse(ChannelContext channelContext, HttpResponse httpResponse, Map<String, String> headers, JSONObject requestBody, long start) {

    OpenAiClient.chatCompletions(headers, requestBody.toString(), new Callback() {

      @Override
      public void onFailure(Call call, IOException e) {
        e.printStackTrace();
        // 直接发送
        httpResponse.setSend(true);
        httpResponse.setJson(RespBodyVo.fail(e.getMessage()));
        Tio.bSend(channelContext, httpResponse);

      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        if (!response.isSuccessful()) {
          httpResponse.setSend(true);
          OkHttpResponseUtils.toTioHttpResponse(response, httpResponse);
          httpResponse.setHasGzipped(true);
          httpResponse.removeHeaders("Content-Length");
          // 响应
          Tio.bSend(channelContext, httpResponse);
          return;
        }
        httpResponse.addServerSentEventsHeader();
        // 60秒后客户端关闭连接
        httpResponse.addHeader(HeaderName.Keep_Alive, HeaderValue.from("timeout=60"));
        httpResponse.addHeader(HeaderName.Transfer_Encoding, HeaderValue.from("chunked"));
        if (!httpResponse.isSend()) { // 不要让处理器发送,我来发送
          // 发送http 响应头,告诉客户端保持连接
          Tio.bSend(channelContext, httpResponse);
        }

        try (ResponseBody responseBody = response.body()) {
          if (responseBody == null) {
            String message = "response body is null";
            log.error(message);
            ChunkedPacket ssePacket = new ChunkedPacket(ChunkEncoder.encodeChunk(message.getBytes()));
            Tio.bSend(channelContext, ssePacket);
            SseEmitter.closeChunkConnection(channelContext);
            return;
          }
          StringBuffer completionContent = new StringBuffer();
          StringBuffer fnCallName = new StringBuffer();
          StringBuffer fnCallArgs = new StringBuffer();

          String line;
          while ((line = responseBody.source().readUtf8Line()) != null) {
            // 必须添加一个回车符号
            byte[] bytes = (line + "\n\n").getBytes();
            if (line.length() < 1) {
              continue;
            }

            // byte[] bytes = body.bytes();
            // 异步拼接
            if (line.length() > 6) {
              String data = line.substring(6, line.length());
              if (data.endsWith("}")) {
                JSONObject parseObject = FastJson2Utils.parseObject(data);
                JSONArray choices = parseObject.getJSONArray("choices");
                if (choices.size() > 0) {
                  String function_call = choices.getJSONObject(0).getJSONObject("delta").getString("function_call");
                  // function_call不发送到前端,只发送content信息
                  if (function_call == null) {
                    ChunkedPacket ssePacket = new ChunkedPacket(ChunkEncoder.encodeChunk(bytes));
                    // 再次向客户端发送sse消息
                    Tio.bSend(channelContext, ssePacket);
                  }
                  OpenAiResponseUtils.extraChoices(completionContent, fnCallName, fnCallArgs, choices);
                }
              } else {
                log.info("data not end with }");
              }
            }
          }
          SseEmitter.closeChunkConnection(channelContext);
        }
      }
    });
  }
}