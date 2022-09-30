package com.k2view.cdbms.usercode.lu.TDM;

import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.k2view.fabric.common.Log;
import com.k2view.fabric.common.Util;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.ZonedDateTime;

import static com.cronutils.model.CronType.QUARTZ;
import static com.k2view.cdbms.shared.user.UserCode.db;
import static com.k2view.cdbms.shared.user.UserCode.getLuType;
import static com.k2view.cdbms.shared.user.UserCode.*;

public class TdmTaskScheduler {

    public static final Log log = Log.a(TdmTaskScheduler.class);
    public static final String TDM = "TDM";

    public static void fnTdmTaskScheduler() throws IOException, SQLException {
        log.info("----------------- Starting tdmTaskScheduler -------------------");
        String activeTasksQuery = new String(getLuType().loadResource("TDM/fnTdmTaskScheduler/query_get_active_tasks.sql"));
        String productQuery = new String(getLuType().loadResource("TDM/fnTdmTaskScheduler/query_get_product_lus.sql"));
        String insertToTaskExecutionQuery = new String(getLuType().loadResource("TDM/fnTdmTaskScheduler/insert_to_task_execution.sql"));
        String insertToTaskExecutionDFQuery = new String(getLuType().loadResource("TDM/fnTdmTaskScheduler/insert_to_task_execution_df.sql"));
        String insertPostToTaskExecutionQuery = new String(getLuType().loadResource("TDM/fnTdmTaskScheduler/insert_post_to_task_execution.sql"));
        String insertToSummaryQuery = new String(getLuType().loadResource("TDM/fnTdmTaskScheduler/insert_to_summary.sql"));
        String insertToRefQuery = new String(getLuType().loadResource("TDM/fnTdmTaskScheduler/insert_to_ref.sql"));

        db(TDM).fetch(activeTasksQuery).forEach(row -> {
            ResultSet resultSet = row.resultSet();
            Long taskID = Util.rte(() -> resultSet.getLong("task_id"));
            Long beID = Util.rte(() -> resultSet.getLong("be_id"));
            String taskType = Util.rte(() -> resultSet.getString("task_type"));
            String cronExpression = Util.rte(() -> resultSet.getString("scheduler"));
            String environmentID = Util.rte(() -> resultSet.getString("environment_id"));
            String sourceEnvName = Util.rte(() -> resultSet.getString("source_env_name"));
            String sourceEnvID = Util.rte(() -> resultSet.getString("source_environment_id"));
            String versionInd = Util.rte(() -> resultSet.getString("version_ind"));
            //Long numOfProcessedEntities = Util.rte(() -> resultSet.getLong("num_of_entities"));
            Timestamp schedulingEndDate = Util.rte(() -> resultSet.getTimestamp("scheduling_end_date"));
            Timestamp localTime = (Timestamp) Util.rte(() -> db("TDM").fetch("SELECT localtimestamp").firstValue());
			String taskCreatedBy = Util.rte(() -> resultSet.getString("task_created_by"));
            //log.info("task with id: " + taskID + " beid: " + beID + " tasktype: " + taskType + " cronexpression: " + cronExpression);
            if(schedulingEndDate != null && localTime.compareTo(schedulingEndDate) > 0){
                //log.info(" updating task to immediate.....");
                Util.rte(() ->db(TDM).execute("UPDATE TASKS SET scheduler = ?, scheduling_end_date = ?, task_last_updated_by = ? WHERE  task_id = ?", "immediate", null, "TDM scheduler", taskID));
                return;
            }

            ExecutionTime executionTime = ExecutionTime.forCron(new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ)).parse(cronExpression));
            ZonedDateTime now = ZonedDateTime.now();
            Duration timeToNextExecution = executionTime.timeToNextExecution(now).get();
            //log.info("time to next execution " + timeToNextExecution.getSeconds() + " to minutes " + timeToNextExecution.toMinutes());
            // Check if the task is due to run now
            if(executionTime.isMatch(now) || (timeToNextExecution.toMinutes() == 0 && timeToNextExecution.getSeconds() <= 10)){
                log.info(" ----------------- adding task to task_execution_list table ----------------- ");

                Long taskExecutionID = (Long) Util.rte(() -> db(TDM).fetch("SELECT NEXTVAL('tasks_task_execution_id_seq')").firstValue());
                //log.info("running product query with: taskID: " + taskID + " envID: "  + environmentID + "sourseEnvName: " + sourceEnvName);
                Util.rte(() -> db(TDM).fetch(productQuery, taskID, environmentID).forEach(r ->
                {
                    ResultSet luRow = r.resultSet();
                    String productVersion = Util.rte(() -> luRow.getString( "product_version"));
                    String dataCenterName = Util.rte(() -> luRow.getString("data_center_name"));
                    Long luID   = Util.rte(() -> luRow.getLong("lu_id"));
                    String luName   = Util.rte(() -> luRow.getString("lu_name"));
                    Long productID   = Util.rte(() -> luRow.getLong("product_id"));

                    Long parentLuID = (Long) Util.rte(() -> db(TDM).fetch("SELECT lu_parent_id FROM product_logical_units WHERE be_id=? AND lu_id=?", beID, luID)).firstValue();
                    log.info(" ----------------- adding task  ----------------- " + taskExecutionID + " luID: " + luID);
                    String insertQuery = versionInd.equals("t") ? insertToTaskExecutionDFQuery : insertToTaskExecutionQuery;

                    Util.rte(() ->
                            db(TDM).execute(insertQuery,
                                    taskID,
                                    taskExecutionID,
                                    beID,
                                    environmentID,
                                    productID,
                                    productVersion,
                                    luID,
                                    dataCenterName,
                                    0,
                                    parentLuID,
                                    sourceEnvID,
                                    sourceEnvName,
                                    taskType,
									taskCreatedBy));

                    //post execution
                    Util.rte(() -> db(TDM).fetch("select * from tasks_post_exe_process where task_id = ?", taskID).forEach( s -> {
                        log.info(" ----------------- adding post execution task to task_execution_list table ----------------- ");
                        ResultSet res = s.resultSet();
                        String processID = Util.rte(() -> res.getString("process_id"));
                        Util.rte(() ->
                                db(TDM).execute(insertPostToTaskExecutionQuery,
                                        taskID,
                                        taskExecutionID,
                                        beID,
                                        environmentID,
                                        productID,
                                        productVersion,
                                        0,
                                        dataCenterName,
                                        0,
                                        parentLuID,
                                        sourceEnvID,
                                        sourceEnvName,
                                        taskType,
                                        processID
                                ));
                    }));

                    //insert to ref
                    Util.rte(() -> db(TDM).execute(insertToRefQuery, taskExecutionID, taskID, luName));

                    // insert to summary
                    Util.rte(() -> db(TDM).execute(insertToSummaryQuery, taskID, taskExecutionID, 0, 0, 0, 0, 0, 0, environmentID, taskType, beID, sourceEnvName, sourceEnvID, taskCreatedBy));
                }));
            }
        });
    }
}
