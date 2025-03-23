package com.litongjava.perplexica.services;

import com.litongjava.google.search.GoogleCustomSearchClient;
import com.litongjava.google.search.GoogleCustomSearchResponse;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;

public class GoogleCustomSearchClientTest {
  public static void main(String[] args) {
    EnvUtils.load();
    String key = EnvUtils.getStr("GOOGLE_API_KEY");
    String ctx = EnvUtils.getStr("CSE_ID");
    GoogleCustomSearchResponse response = GoogleCustomSearchClient.search(key, ctx, "When is the fist day of sjsu");
    System.out.println(JsonUtils.toJson(response));
  }
}
