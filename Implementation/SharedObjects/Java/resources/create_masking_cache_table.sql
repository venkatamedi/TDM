CREATE KEYSPACE k2masking WITH REPLICATION = {
    'class' : 'SimpleStrategy',
    'replication_factor' : 1
};

USE k2masking;

CREATE TABLE "masking_cache" (
	"environment"	        TEXT,
	"execution_id"	        TEXT,
	"masking_id"	        TEXT,
	"instance_id"	        TEXT,
	"clone_id"	            TEXT,
	"original_value_hash"	TEXT,
	"masked_value"	        TEXT,
	PRIMARY KEY("environment", "execution_id", "masking_id", "instance_id", "clone_id", "original_value_hash")
);

CREATE TABLE "uniqueness" (
	"environment"	TEXT,
	"execution_id"	TEXT,
	"masking_id"	TEXT,
	"masked_value"	TEXT,
	PRIMARY KEY("environment", "execution_id", "masking_id", "masked_value")
);

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
