package com.litongjava.max.search.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

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
import com.litongjava.tio.utils.thread.TioThreadUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearxngSearchVectorMultiThreadService {

  // 设置使用的 embedding 模型
  private static final String MODEL_NAME = OpenAiModels.TEXT_EMBEDDING_3_LARGE;

  public List<WebPageContent> computeSimilarity(String question) {
    // 记录总耗时开始时间
    long totalStartTime = System.currentTimeMillis();

    // 1. 调用 Searxng 搜索，获取搜索结果列表
    long searchStartTime = System.currentTimeMillis();
    SearxngSearchParam searxngSearchParam = new SearxngSearchParam();
    searxngSearchParam.setFormat("json");
    searxngSearchParam.setQ(question);

    SearxngSearchResponse searchResponse = SearxngSearchClient.search(searxngSearchParam);
    List<SearxngResult> results = searchResponse.getResults();
    long searchEndTime = System.currentTimeMillis();
    log.info("Searxng搜索耗时：{} 毫秒", (searchEndTime - searchStartTime));

    List<WebPageContent> webPageContents = new ArrayList<>();
    // 使用 Map 保存 description 的 md5 与对应的 WebPageContent（若相同描述出现多次，可扩展为 List）
    Map<String, WebPageContent> md5ToContentMap = new HashMap<>();
    for (SearxngResult searxngResult : results) {
      String title = searxngResult.getTitle();
      String url = searxngResult.getUrl();
      // 将搜索结果的 content 赋值到 description 字段
      String description = searxngResult.getContent();
      WebPageContent wpc = new WebPageContent(title, url, description);
      webPageContents.add(wpc);
      if (description != null && !description.trim().isEmpty()) {
        String md5 = Md5Utils.getMD5(description);
        md5ToContentMap.put(md5, wpc);
      }
    }

    // 2. 获取问题文本对应的向量 id
    long questionVectorStartTime = System.currentTimeMillis();
    KbEmbeddingService maxKbEmbeddingService = new KbEmbeddingService();
    Long questionVectorId = maxKbEmbeddingService.getVectorId(question, MODEL_NAME);
    long questionVectorEndTime = System.currentTimeMillis();
    log.info("问题文本向量计算耗时：{} 毫秒", (questionVectorEndTime - questionVectorStartTime));

    // 3. 检查所有搜索结果描述对应的向量 id 是否存在，如不存在则并发计算并插入 DB
    long descVectorStartTime = System.currentTimeMillis();
    List<String> md5List = new ArrayList<>(md5ToContentMap.keySet());
    List<String> missingMd5List = new ArrayList<>();
    List<Long> description_vector_ids = new ArrayList<>();

    for (String md5 : md5List) {
      // 查询数据库中是否存在该描述的向量 id
      String querySql = "SELECT description_vector_id FROM max_kb_web_page_description WHERE description_md5 = ?";
      Long descVectorId = Db.queryLong(querySql, md5);
      if (descVectorId == null) {
        missingMd5List.add(md5);
      } else {
        description_vector_ids.add(descVectorId);
      }
    }

    if (!missingMd5List.isEmpty()) {
      List<Future<Long>> futureList = new ArrayList<>();
      // 对于每个缺失的描述，根据 md5 找到对应的 WebPageContent，计算向量，并插入记录
      for (String md5 : missingMd5List) {
        WebPageContent wpc = md5ToContentMap.get(md5);
        String description = wpc.getDescription();
        Future<Long> future = TioThreadUtils.submit(() -> {
          // 调用 embedding 服务计算向量 id
          Long vectorId = maxKbEmbeddingService.getVectorId(description, MODEL_NAME);
          // 插入记录到 max_kb_web_page_description 表
          Long id = SnowflakeIdUtils.id();
          Row row = new Row().set("id", id).set("description", description).set("description_md5", md5).set("description_vector_id", vectorId);
          Db.save("max_kb_web_page_description", row);
          return vectorId;
        });
        futureList.add(future);
      }
      // 等待所有任务完成
      for (Future<Long> future : futureList) {
        try {
          Long vectorId = future.get();
          description_vector_ids.add(vectorId);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    long descVectorEndTime = System.currentTimeMillis();
    log.info("描述向量计算并存储耗时：{} 毫秒", (descVectorEndTime - descVectorStartTime));

    // 4. 统一查询：通过一条 SQL 计算所有描述与问题向量的相似度
    long similarityStartTime = System.currentTimeMillis();
    // 先构造 IN 子句，动态生成占位符（?,?,...?）
    StringBuilder inClause = new StringBuilder();
    for (int i = 0; i < md5List.size(); i++) {
      inClause.append("?");
      if (i < md5List.size() - 1) {
        inClause.append(",");
      }
    }

    String simSql = "SELECT ec.md5 AS description_md5, 1 - (q.v <=> ec.v) AS similarity " + "FROM max_kb_embedding_cache ec " + "CROSS JOIN (SELECT v FROM max_kb_embedding_cache WHERE id = ?) q "
        + "WHERE ec.id = ANY(?) " + "ORDER BY similarity DESC";
    // 执行 SQL 查询，返回的结果为 List<Row>
    Long[] array = description_vector_ids.toArray(new Long[0]);
    //log.info("执行相似度SQL: {} 参数: questionVectorId={}, description_vector_ids={}", simSql, questionVectorId, array);
    List<Row> similarityRows = Db.find(simSql, questionVectorId, array);
    long similarityEndTime = System.currentTimeMillis();
    log.info("相似度计算耗时：{} 毫秒", (similarityEndTime - similarityStartTime));

    // 5. 将查询结果对应的相似度赋值到各个 WebPageContent 对象中
    for (Row row : similarityRows) {
      String md5 = row.getString("description_md5");
      Double similarity = row.getDouble("similarity");
      WebPageContent wpc = md5ToContentMap.get(md5);
      if (wpc != null) {
        wpc.setSimilarity(similarity);
      }
    }

    long totalEndTime = System.currentTimeMillis();
    log.info("总耗时：{} 毫秒", (totalEndTime - totalStartTime));
    return webPageContents;
  }
}
