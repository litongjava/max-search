package com.litongjava.perplexica.model;

import com.litongjava.perplexica.model.base.BasePerplexicaChatMessage;

/**
 * Generated by java-db.
 */
public class PerplexicaChatMessage extends BasePerplexicaChatMessage<PerplexicaChatMessage> {
  private static final long serialVersionUID = 1L;
	public static final PerplexicaChatMessage dao = new PerplexicaChatMessage().dao();
	/**
	 * 
	 */
  public static final String tableName = "perplexica_chat_message";
  public static final String primaryKey = "id";
  //java.lang.Long 
  public static final String id = "id";
  //java.lang.Long 
  public static final String chatId = "chat_id";
  //java.lang.String 
  public static final String role = "role";
  //java.lang.String 
  public static final String content = "content";
  //java.lang.String 
  public static final String rewrited = "rewrited";
  //java.lang.String 
  public static final String sources = "sources";
  //java.lang.String 
  public static final String metadata = "metadata";
  //java.util.Date 
  public static final String createdAt = "created_at";
  //java.lang.String 
  public static final String remark = "remark";
  //java.lang.String 
  public static final String creator = "creator";
  //java.util.Date 
  public static final String createTime = "create_time";
  //java.lang.String 
  public static final String updater = "updater";
  //java.util.Date 
  public static final String updateTime = "update_time";
  //java.lang.Integer 
  public static final String deleted = "deleted";
  //java.lang.Long 
  public static final String tenantId = "tenant_id";

  @Override
  protected String _getPrimaryKey() {
    return primaryKey;
  }

  @Override
  protected String _getTableName() {
    return tableName;
  }
}

