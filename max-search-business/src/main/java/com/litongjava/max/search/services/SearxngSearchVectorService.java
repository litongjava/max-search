package com.litongjava.max.search.services;

import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PGobject;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.maxkb.service.kb.KbEmbeddingService;
import com.litongjava.model.web.WebPageContent;
import com.litongjava.openai.consts.OpenAiModels;
import com.litongjava.searxng.SearxngResult;
import com.litongjava.searxng.SearxngSearchClient;
import com.litongjava.searxng.SearxngSearchParam;
import com.litongjava.searxng.SearxngSearchResponse;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

public class SearxngSearchVectorService {

  // 请根据需要设置模型名称（例如 OpenAI 的 embedding 模型）
  private static final String MODEL_NAME = OpenAiModels.TEXT_EMBEDDING_3_LARGE;

  public List<WebPageContent> computeSimilarity(String question) {
    // 1. 先进行搜索
    SearxngSearchParam searxngSearchParam = new SearxngSearchParam();
    searxngSearchParam.setFormat("json");
    searxngSearchParam.setQ(question);

    SearxngSearchResponse searchResponse = SearxngSearchClient.search(searxngSearchParam);
    List<SearxngResult> results = searchResponse.getResults();
    List<WebPageContent> webPageContents = new ArrayList<>();
    for (SearxngResult searxngResult : results) {
      String title = searxngResult.getTitle();
      String url = searxngResult.getUrl();
      // 此处将搜索结果的 content 赋值到 description 字段
      webPageContents.add(new WebPageContent(title, url, searxngResult.getContent()));
    }

    // 2. 获取问题的向量
    KbEmbeddingService maxKbEmbeddingService = new KbEmbeddingService();
    PGobject questionVector = maxKbEmbeddingService.getVector(question, MODEL_NAME);

    // 3. 对每个搜索结果，计算问题和 description 的相似度
    for (WebPageContent wpc : webPageContents) {
      String description = wpc.getDescription();
      if (description == null || description.trim().isEmpty()) {
        continue;
      }
      // 根据 description 计算 md5
      String md5 = Md5Utils.getMD5(description);

      // 尝试从 max_kb_web_page_description 表中查询 description 对应的向量
      String querySql = "SELECT descriptiont_vector FROM max_kb_web_page_description WHERE description_md5 = ?";
      PGobject descVector = Db.queryFirst(querySql, md5);

      // 如果不存在，则调用 embedding 服务计算向量，并插入记录
      if (descVector == null) {
        descVector = maxKbEmbeddingService.getVector(description, MODEL_NAME);
        long id = SnowflakeIdUtils.id();
        Row row = Row.by("id", id).set("description", description).set("description_md5", md5).set("descriptiont_vector", descVector);

        Db.save("max_kb_web_page_description", row);
      }

      // 4. 使用 PostgreSQL 计算相似度
      String simSql = "SELECT 1-(? <=> ?) AS similarity";
      Double similarity = Db.queryDouble(simSql, questionVector, descVector);
      wpc.setSimilarity(similarity);
    }
    return webPageContents;
  }
}
