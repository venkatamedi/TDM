SELECT global_name, global_value, 'TDM_ENV_GLOBALS' as source
FROM TDM_ENV_GLOBALS env WHERE environment_id in (?) AND env.global_name <> 'TDM_SET_SYNC_OFF'
UNION ALL
SELECT global_name, global_value ,'TASK_GLOBALS' as source FROM TASK_GLOBALS WHERE task_id=?
EXCEPT   
SELECT env.global_name, env.global_value, 'TDM_ENV_GLOBALS' as source
FROM TDM_ENV_GLOBALS env
INNER JOIN TASK_GLOBALS task 
ON env.global_name=task.global_name WHERE task_id=? AND environment_id in (?) AND env.global_name <> 'TDM_SET_SYNC_OFF'