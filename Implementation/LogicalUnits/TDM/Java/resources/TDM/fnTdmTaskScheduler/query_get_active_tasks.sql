WITH task_max_execution_id AS(
SELECT task_id AS max_task_id,lu_id AS max_lu_id,max(task_execution_id) AS max_task_execution_id FROM TASK_EXECUTION_LIST GROUP BY task_id,lu_id
)
SELECT  task_id,
        scheduler,
        be_id,
        environment_id,
        num_of_entities,
        scheduling_end_date,
        source_env_name,
        task_type,
        version_ind,
        source_environment_id,
		task_created_by
FROM TASKS
WHERE UPPER(task_status) = 'ACTIVE'
AND UPPER(task_execution_status) = 'ACTIVE'
AND UPPER(scheduler) != 'IMMEDIATE'
AND task_id NOT IN (SELECT task_id FROM TASK_EXECUTION_LIST,task_max_execution_id WHERE task_id = max_task_id AND task_execution_id = max_task_execution_id AND lu_id = max_lu_id AND UPPER(execution_status) IN ('RUNNING','EXECUTING','STARTED','PENDING','PAUSED','STARTEXECUTIONREQUESTED') OR (end_execution_time >= (current_timestamp at time zone 'utc' + '-1 minute')));