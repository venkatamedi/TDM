INSERT INTO TASK_EXECUTION_SUMMARY (
    task_id,
    task_execution_id,
    start_execution_time,
    end_execution_time,
    tot_num_of_processed_root_entities,
    tot_num_of_copied_root_entities,
    tot_num_of_failed_root_entities,
    tot_num_of_processed_ref_tables,
    tot_num_of_copied_ref_tables,
    tot_num_of_failed_ref_tables,
    environment_id,
    task_type,
    be_id,
    creation_date,
    version_datetime,
    version_expiration_date,
    update_date,
    execution_status,
    source_env_name,
    source_environment_id,
	task_executed_by
    )
VALUES(?,?,localtimestamp,localtimestamp,?,?,?,?,?,?,?,?,?,localtimestamp,localtimestamp,localtimestamp,localtimestamp,'pending',?,?,?)
 ON CONFLICT ON CONSTRAINT task_execution_summary_pkey DO NOTHING;