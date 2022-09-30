DROP TABLE IF EXISTS k2masking.tdm_seq_mapping;

USE k2masking;

CREATE TABLE "tdm_seq_mapping" (
    "task_execution_id"  TEXT,
    "iid"                TEXT,
    "lu_type"            TEXT,
    "seq_name"           TEXT,
    "table_name"         TEXT,
    "column_name"        TEXT,
    "source_value"       TEXT,
    "target_value"       TEXT,
	PRIMARY KEY("task_execution_id", "lu_type", "table_name", "seq_name", "iid", "target_value")
);