package com.litongjava.max.search.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.kb.MaxKbParagraphRetrieveService;
import com.litongjava.maxkb.vo.MaxKbRetrieveResult;
import com.litongjava.maxkb.vo.ParagraphSearchResultVo;
import com.litongjava.model.web.WebPageContent;

public class MaxWebsiteRetrieveService {

  public List<WebPageContent> search(String question) {
    Long[] datasetIdArray = { 1L };
    Float similarity = 0.2f;
    Integer top_n = 10;
    MaxKbRetrieveResult step = Aop.get(MaxKbParagraphRetrieveService.class).retrieve(datasetIdArray, similarity, top_n, question);
    List<ParagraphSearchResultVo> searchResults = step.getParagraph_list();

    // 使用 Map 以 paragraph_id 去重（key 为 paragraph_id，value 为对应的 WebPageContent）
    Map<Long, WebPageContent> mergedMap = new HashMap<>();

    // 1. 收集所有需要查询的 paragraph_id（去重）
    Set<Long> paragraphIds = new HashSet<>();
    for (ParagraphSearchResultVo vo : searchResults) {
      paragraphIds.add(vo.getParagraph_id());
    }

    // 2. 一次查询获取所有 paragraph 内容
    Map<Long, String> paragraphContentMap = fetchParagraphContents(paragraphIds);

    // 3. 遍历所有检索结果，根据 paragraph_id 从 map 中获取完整段落内容
    for (ParagraphSearchResultVo vo : searchResults) {
      long paragraphId = vo.getParagraph_id();
      if (!mergedMap.containsKey(paragraphId)) {
        WebPageContent pageContent = new WebPageContent();
        pageContent.setTitle(vo.getDocument_name());
        String document_url = vo.getDocument_url();
        if (document_url != null && !document_url.startsWith("http")) {
          pageContent.setUrl("https://" + document_url);
        } else {
          pageContent.setUrl(document_url);
        }
        pageContent.setDescription(vo.getDataset_name());
        // 从批量查询的结果中获取内容
        String fullContent = paragraphContentMap.get(paragraphId);
        pageContent.setContent(fullContent);
        mergedMap.put(paragraphId, pageContent);
      } else {
        // 如有重复，根据业务逻辑决定是否合并内容
        WebPageContent pageContent = mergedMap.get(paragraphId);
        String currentContent = pageContent.getContent();
        String newContent = paragraphContentMap.get(paragraphId);
        if (!currentContent.contains(newContent)) {
          pageContent.setContent(currentContent + "\n" + newContent);
        }
      }
    }

    return new ArrayList<>(mergedMap.values());
  }

  /**
   * 根据一组 paragraphId 一次性查询所有段落内容
   */
  private Map<Long, String> fetchParagraphContents(Set<Long> paragraphIds) {
    // 构建 IN 查询的占位符
    StringBuilder sql = new StringBuilder("SELECT id, content FROM max_kb_paragraph WHERE id IN (");
    StringJoiner joiner = new StringJoiner(",");
    for (int i = 0; i < paragraphIds.size(); i++) {
      joiner.add("?");
    }
    sql.append(joiner.toString()).append(")");

    // 将 paragraphIds 转换为参数数组
    Object[] params = paragraphIds.toArray();

    // 假设 Db.find 返回的是一个包含 Record 的 List，每个 Record 有 id 和 content 属性
    List<Row> records = Db.find(sql.toString(), params);
    Map<Long, String> contentMap = new HashMap<>();
    for (Row record : records) {
      Long id = record.getLong("id");
      String content = record.getStr("content");
      contentMap.put(id, content);
    }
    return contentMap;
  }
}
