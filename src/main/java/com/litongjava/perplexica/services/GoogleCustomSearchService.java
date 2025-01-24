package com.litongjava.perplexica.services;

import com.litongjava.google.search.GoogleCustomSearchClient;
import com.litongjava.google.search.GoogleCustomSearchResponse;
import com.litongjava.tio.utils.environment.EnvUtils;

public class GoogleCustomSearchService {

  public GoogleCustomSearchResponse search(String ctx, String text) {
    String key = EnvUtils.getStr("GOOGLE_API_KEY");
    //String ctx = EnvUtils.getStr("CSE_ID");
    return GoogleCustomSearchClient.search(key, ctx, text);
  }
}
