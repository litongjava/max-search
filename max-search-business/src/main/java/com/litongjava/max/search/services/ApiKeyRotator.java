package com.litongjava.max.search.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jfinal.kit.Kv;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;

public class ApiKeyRotator {

  private List<Kv> apiKeys;
  private AtomicInteger currentIndex = new AtomicInteger();

  // SQL to load API keys
  private static final String LOAD_KEYS_SQL = "SELECT id, name, api_key FROM gemini_api_key WHERE deleted = 0 ORDER BY id ASC";

  public ApiKeyRotator() {
    if (apiKeys == null) {
      apiKeys = new ArrayList<>();
      List<Row> find = Db.find(LOAD_KEYS_SQL);
      for (Row row : find) {
        apiKeys.add(row.toKv());
      }
    }
  }

  /**
   * Gets the next API key in a round-robin fashion.
   * This method is thread-safe.
   *
   * @return An Optional containing the ApiKeyInfo, or Optional.empty() if no keys are loaded.
   */
  public String getNextKey() {
    int index = currentIndex.getAndIncrement() % apiKeys.size();
    if (index < 0) {
      index = (index % apiKeys.size() + apiKeys.size()) % apiKeys.size();
    }
    return apiKeys.get(index).getStr("api_key");
  }

  public int getLoadedKeysCount() {
    return apiKeys.size();
  }

  public List<String> geApitKeys() {
    List<String> retval = new ArrayList<>();
    for (Kv kv : apiKeys) {
      retval.add(kv.getStr("api_key"));
    }
    return retval;
  }
}