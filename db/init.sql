drop table if exists max_search_chat_session;
CREATE TABLE max_search_chat_session (
  id bigint PRIMARY KEY, -- 聊天的唯一标识符
  user_id bigint,
  title varchar NOT NULL, -- 聊天标题
  type varchar,
  chat_type int,
  org varchar,
  focus_mode varchar,
  files jsonb,
  metadata jsonb,
  "created_at" "timestamptz"  DEFAULT now(),
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

drop table if exists max_search_chat_message;
CREATE TABLE max_search_chat_message (
  id bigint PRIMARY KEY,
  chat_id bigint,
  role VARCHAR(20) NOT NULL, -- 消息角色，如 user 或 assistant
  content TEXT NOT NULL, -- 消息内容
  rewrited text,
  sources jsonb,
  metadata JSONB,
  "created_at" "timestamptz"  DEFAULT now(),
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

drop table if exists tio_boot_admin_system_upload_file;
CREATE TABLE tio_boot_admin_system_upload_file (
  id BIGINT NOT NULL, -- 文件ID
  md5 VARCHAR(32) NOT NULL, -- 文件的MD5值，用于校验文件一致性
  filename VARCHAR(64) NOT NULL, -- 文件名
  file_size BIGINT NOT NULL, -- 文件大小，单位为字节
  user_id VARCHAR(32), -- 用户ID，标识上传文件的用户
  platform VARCHAR(64) NOT NULL, -- 上传平台（如S3）
  region_name VARCHAR(32), -- 区域名
  bucket_name VARCHAR(64) NOT NULL, -- 存储桶名称
  file_id VARCHAR(64) NOT NULL, -- 文件存储ID
  target_name VARCHAR(64) NOT NULL, -- 文件存储路径
  tags JSON, -- 文件标签，使用JSON格式
  creator VARCHAR(64) DEFAULT '', -- 创建者
  create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间
  updater VARCHAR(64) DEFAULT '', -- 更新者
  update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 更新时间
  deleted SMALLINT NOT NULL DEFAULT 0, -- 删除标志
  tenant_id BIGINT NOT NULL DEFAULT 0, -- 租户ID
  PRIMARY KEY (id) -- 主键
);

create index "index_tio_boot_admin_system_upload_file_md5" on tio_boot_admin_system_upload_file("md5")  


DROP TABLE IF EXISTS "public"."max_kb_dataset";

CREATE TABLE "public"."max_kb_dataset" (
  "id" BIGINT PRIMARY KEY,
  "name" VARCHAR NOT NULL,
  "desc" VARCHAR,
  "type" VARCHAR,
  "embedding_mode_id" BIGINT,
  "meta" JSONB,
  "user_id" BIGINT,
  "remark" VARCHAR(256),
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS "public"."max_kb_application_dataset_mapping";

CREATE TABLE "public"."max_kb_application_dataset_mapping" (
  "id" BIGINT PRIMARY KEY,
  "application_id" BIGINT NOT NULL,
  "dataset_id" BIGINT NOT NULL
);


DROP TABLE IF EXISTS "public"."max_kb_document";

CREATE TABLE "public"."max_kb_document" (
  "id" BIGINT NOT NULL PRIMARY KEY,
  "file_id" BIGINT,
  "user_id" BIGINT,
  "title" VARCHAR,
  "name" VARCHAR NOT NULL,
  "type" VARCHAR NOT NULL,
  "url" VARCHAR,
  "content" text,
  "char_length" INT,
  "status" VARCHAR,
  "is_active" BOOLEAN,
  "meta" JSONB,
  "dataset_id" BIGINT NOT NULL,
  "hit_handling_method" VARCHAR,
  "directly_return_similarity" FLOAT8,
  "paragraph_count" INT,
  "files" JSON,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT NOT NULL DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS "public"."max_kb_paragraph";

CREATE TABLE "public"."max_kb_paragraph" (
  "id" BIGINT PRIMARY KEY,
  "source_id" BIGINT,
  "source_type" VARCHAR,
  "title" VARCHAR,
  "content" VARCHAR NOT NULL,
  "md5" VARCHAR NOT NULL,
  "status" VARCHAR,
  "hit_num" INT,
  "is_active" BOOLEAN,
  "dataset_id" BIGINT NOT NULL,
  "document_id" BIGINT NOT NULL,
  "embedding" VECTOR,
  "meta" JSONB,
  "search_vector" TSVECTOR,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);


DROP TABLE IF EXISTS "public"."max_kb_sentence";

CREATE TABLE "public"."max_kb_sentence" (
  "id" BIGINT PRIMARY KEY,
  "type" INT,
  "content" VARCHAR NOT NULL,
  "md5" VARCHAR NOT NULL,
  "hit_num" INT NOT NULL,
  "dataset_id" BIGINT NOT NULL,
  "document_id" BIGINT NOT NULL,
  "paragraph_id" BIGINT NOT NULL,
  "embedding" VECTOR,
  "meta" JSONB,
  "search_vector" TSVECTOR,
  "creator" VARCHAR(64) DEFAULT '',
  "create_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" VARCHAR(64) DEFAULT '',
  "update_time" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" SMALLINT DEFAULT 0,
  "tenant_id" BIGINT NOT NULL DEFAULT 0
);


DROP TABLE IF EXISTS "public"."max_kb_embedding_cache";

CREATE TABLE "public"."max_kb_embedding_cache" (
  "id" BIGINT PRIMARY KEY,
  "t" TEXT,
  "m" VARCHAR,
  "v" VECTOR,
  "md5" VARCHAR
);

CREATE INDEX "idx_max_kb_embedding_cache_md5" ON "public"."max_kb_embedding_cache" USING btree ("md5");
CREATE INDEX "idx_max_kb_embedding_cache_md5_m" ON "public"."max_kb_embedding_cache" USING btree ("md5", "m");