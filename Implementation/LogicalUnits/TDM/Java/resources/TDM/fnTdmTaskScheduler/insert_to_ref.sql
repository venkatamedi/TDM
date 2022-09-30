INSERT INTO TASK_REF_EXE_STATS(
    task_id,
    task_execution_id,
    task_ref_table_id,
    ref_table_name,
    update_date,
    execution_status
    )
SELECT task_id, ?, task_ref_table_id, ref_table_name, localtimestamp, 'pending' from TASK_REF_TABLES WHERE task_id = ? and lu_name = ?