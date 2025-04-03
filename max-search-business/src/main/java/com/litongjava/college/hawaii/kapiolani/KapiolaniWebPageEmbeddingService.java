package com.litongjava.college.hawaii.kapiolani;

import java.util.ArrayList;
import java.util.List;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.model.MaxKbDocument;
import com.litongjava.maxkb.service.kb.MaxKbDocumentSplitService;
import com.litongjava.maxkb.service.kb.MaxKbParagraphSplitService;
import com.litongjava.maxkb.vo.Paragraph;
import com.litongjava.maxkb.vo.ParagraphBatchVo;
import com.litongjava.model.page.Page;

import dev.langchain4j.data.segment.TextSegment;

public class KapiolaniWebPageEmbeddingService {

  MaxKbDocumentSplitService maxKbDocumentSplitService = Aop.get(MaxKbDocumentSplitService.class);

  //
  public void index() {
    int pageNumber = 1;
    int pageSize = 100;
    while (true) {
      Page<Row> page = Db.paginate(pageNumber, pageSize, "select id, url, title, type, markdown", "from hawaii_kapiolani_web_page");
      List<Row> list = page.getList();
      if (list.isEmpty()) {
        break;
      }

      for (Row row : list) {
        Long id = row.getLong("id");
        if (!Db.exists(MaxKbDocument.tableName, "id", id)) {
          one(row, id);
        }
      }

      pageNumber++;
    }
  }

  private void one(Row row, Long id) {
    MaxKbDocument maxKbDocument = new MaxKbDocument();
    String name = row.getString("title");
    String markdown = row.getString("markdown");
    long userId = 1L;
    long dataseetId = 1L;
    maxKbDocument.setId(id).set("url", row.getString("url")).set("title", name)
        //
        .set("name", name).set("type", row.getString("type")).set("content", markdown)
        //
        .set("dataset_id", dataseetId).set("user_id", userId);
    maxKbDocument.save();

    List<TextSegment> segments = maxKbDocumentSplitService.split(markdown);
    List<Paragraph> paragraphs = new ArrayList<>();
    for (TextSegment textSegment : segments) {
      paragraphs.add(new Paragraph(textSegment.text()));
    }

    ParagraphBatchVo paragraphBatchVo = new ParagraphBatchVo().setId(id).setName(name);
    paragraphBatchVo.setParagraphs(paragraphs);

    List<ParagraphBatchVo> list = new ArrayList<>();
    list.add(paragraphBatchVo);

    Aop.get(MaxKbParagraphSplitService.class).batch(userId, dataseetId, list);

  }
}
