SELECT DISTINCT
    tt.task_id
  , tt.be_id
  , tt.task_type
  , tt.creation_date
  , tt.data_center_name
  , tt.environment_id
  , 0 AS parent_lu_id
  , tt.lu_id
  , tt.task_execution_id
  , tt.execution_status parent_lu_status
  , tt.product_id
  , tt.product_version tdm_target_product_version
  , tt.num_of_processed_entities
  , tt.process_id
  , ep.product_version tdm_source_product_version
  , to_char(tt.version_datetime, 'yyyyMMddHH24miss') as version_datetime
  , e.environment_name  as source_environment_name
  , e2.environment_name as target_environment_name
  , SPLIT_PART(tt.task_executed_by, '##', 1) as task_executed_by
  , SPLIT_PART(tt.task_executed_by, '##', 2) as user_roles
FROM
    TASK_EXECUTION_LIST tt
  , environments         e
  , environments         e2
  , environment_products ep
WHERE
    UPPER(ep.status)                 = 'ACTIVE'
    AND UPPER(tt.execution_status)   = 'PENDING'
    AND (
        tt.parent_lu_id is null
        or not exists(
            select 1 from TASK_EXECUTION_LIST par where par.task_execution_id = tt.task_execution_id and par.lu_id = tt.parent_lu_id
        ) or exists(
            select 1 from TASKS t where tt.task_id = t.task_id and t.selection_method = 'REF'
        )
    )
    AND tt.source_environment_id = e.environment_id
    AND e.environment_id         = ep.environment_id
    AND ep.product_id            = tt.product_id
	AND tt.environment_id        = e2.environment_id
	AND tt.lu_id > 0
UNION
SELECT DISTINCT
    tt.task_id
  , tt.be_id
  , tt.task_type
  , tt.creation_date
  , tt.data_center_name
  , tt.environment_id
  , tt.parent_lu_id
  , tt.lu_id
  , tt.task_execution_id
  , p.execution_status parent_lu_status
  , tt.product_id
  , tt.product_version tdm_target_product_version
  , tt.num_of_processed_entities
  , tt.process_id
  , ep.product_version tdm_source_product_version
  , to_char(tt.version_datetime, 'yyyyMMddHH24miss') as version_datetime
  , e.environment_name as source_environment_name
  , e2.environment_name as target_environment_name
  , SPLIT_PART(tt.task_executed_by, '##', 1) as task_executed_by
  , SPLIT_PART(tt.task_executed_by, '##', 2) as user_roles
FROM
    TASK_EXECUTION_LIST tt
  , TASK_EXECUTION_LIST p
  , environments         e
  , environments         e2
  , environment_products ep
WHERE
    UPPER(tt.execution_status)       = 'PENDING'
    AND tt.task_execution_id         = p.task_execution_id
    AND tt.parent_lu_id              = p.lu_id
    AND UPPER(p.execution_status) in ('STOPPED', 'FAILED' , 'KILLED' ,'COMPLETED')
    AND UPPER(ep.status) = 'ACTIVE'
    AND tt.source_environment_id = e.environment_id
    AND e.environment_id         = ep.environment_id
    AND ep.product_id            = tt.product_id
	AND tt.environment_id        = e2.environment_id
	AND tt.lu_id > 0
UNION
SELECT DISTINCT
    tt.task_id
  , tt.be_id
  , tt.task_type
  , tt.creation_date
  , tt.data_center_name
  , tt.environment_id
  , 0 parent_lu_id
  , tt.lu_id
  , tt.task_execution_id
  , '' parent_lu_status
  , tt.product_id
  , tt.product_version tdm_target_product_version
  , tt.num_of_processed_entities
  , tt.process_id
  , '' tdm_source_product_version
  , to_char(tt.version_datetime, 'yyyyMMddHH24miss') as version_datetime
  , tt.source_env_name as source_environment_name
  , e.environment_name as target_environment_name
  , SPLIT_PART(tt.task_executed_by, '##', 1) as task_executed_by
  , SPLIT_PART(tt.task_executed_by, '##', 2) as user_roles
FROM
      TASK_EXECUTION_LIST tt
    , environments         e
WHERE
    UPPER(tt.execution_status)   = 'PENDING'
    AND tt.environment_id            = e.environment_id
    AND tt.process_id > 0