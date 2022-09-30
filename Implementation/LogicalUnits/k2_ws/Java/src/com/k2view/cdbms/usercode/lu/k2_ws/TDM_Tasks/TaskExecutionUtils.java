package com.k2view.cdbms.usercode.lu.k2_ws.TDM_Tasks;

import com.k2view.cdbms.shared.Db;
import com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils;
import com.k2view.fabric.common.Log;
import com.k2view.fabric.common.Util;
import org.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.k2view.cdbms.shared.user.UserCode.*;
import static com.k2view.cdbms.shared.user.WebServiceUserCode.graphit;
import static com.k2view.cdbms.usercode.common.TDM.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam", "unchecked"})
public class TaskExecutionUtils {
    private static final String DB_FABRIC = "fabric";
    private static String schema = "public";

    static void fnAddTaskPostExecutionProcess(List<Map<String, Object>> postExecutionProcesses, Long taskId) throws Exception {
        String sql = "DELETE FROM \"" + schema + "\".TASKS_POST_EXE_PROCESS WHERE \"" + schema + "\".TASKS_POST_EXE_PROCESS.task_id = " + taskId;
        db("TDM").execute(sql);
        if (postExecutionProcesses == null) return;
        for (Map<String, Object> postExecutionProcess : postExecutionProcesses) {
            db("TDM").execute("INSERT INTO \"" + schema + "\".TASKS_POST_EXE_PROCESS (task_id, process_id,process_name, execution_order) VALUES (?,?,?,?)",
                    taskId,
                    postExecutionProcess.get("process_id"),
                    postExecutionProcess.get("process_name"),
                    postExecutionProcess.get("execution_order"));
        }
    }

    static Object fnExecutionSummaryReport(String i_taskExecutionId, String i_luName) throws Exception {
        fabric().execute("get TDM.?", i_taskExecutionId);

        String taskType = "" + fabric().fetch("select task_type from tasks limit 1").firstValue();

        Object response;

        if ("LOAD".equalsIgnoreCase(taskType) || "RESERVE".equalsIgnoreCase(taskType)) {
            if ("ALL".equalsIgnoreCase(i_luName)) {
                //log.info("Creating report for Load Task");
                response = graphit("LoadSummaryReport.graphit");
            } else {
                Map<String, Object> temp = new HashMap<>();
                temp.put("i_luName", i_luName);
                response = graphit("LoadSummaryReportPerLu.graphit", temp);
            }

        } else {
			//log.info("Creating report for Extract Task");
			if ("ALL".equalsIgnoreCase(i_luName)) {
				//log.info("Creating report for Load Task");
				response = graphit("ExtractSummaryReport.graphit");
			} else {
				Map<String, Object> temp = new HashMap<>();
				temp.put("i_luName", i_luName);
				response = graphit("ExtractSummaryReportPerLu.graphit", temp);
				}
		}

        return wrapWebServiceResults("SUCCESS", null, response);
    }

    static Object fnGetNextTaskExecution(Long taskId) throws Exception {
        String query = "SELECT nextval('tasks_task_execution_id_seq')";
        Db.Rows rows = db("TDM").fetch(query);
        List<String> columnNames = rows.getColumnNames();
        Db.Row row = rows.firstRow();
        if (!row.isEmpty()) {
            return row.cell(0);
        }
        return null;
    }

    static Object fnStopTaskExecution(Long task_execution_id) throws Exception {
        Db.Rows batchIdList = null;
        try {
            //TDM 6.0 - Get the list of migration IDs based on task execution ID, instead of getting one migrate_id as input
            batchIdList = db("TDM").fetch("select fabric_execution_id, execution_status, l.lu_id, lu_name, task_type " +
                    "from task_execution_list l, tasks_logical_units u where task_execution_id = ? " +
                    "and fabric_execution_id is not null and UPPER(execution_status) IN " +
                    "('RUNNING','EXECUTING','STARTED','PENDING','PAUSED','STARTEXECUTIONREQUESTED') " +
                    "and l.task_id = u.task_id and l.lu_id = u.lu_id", task_execution_id);

            db("TDM").execute("UPDATE task_execution_list SET execution_status='stopped' where task_execution_id = ? and execution_status not in ('completed', 'failed')",
                    task_execution_id);
            // TDM 5.1- add a reference handling- update the status of the reference tables to 'stopped'.
            // The cancellation of the jobs for the tables will be handled by the new fabric listener user job for the reference copy.

            db("TDM").execute("UPDATE task_ref_exe_stats set execution_status='stopped', number_of_processed_records = 0 where task_execution_id = ? " +
                    "and execution_status not in ('completed', 'failed')", task_execution_id);

            // TDM 7, set the execution summary to stopped also
            db("TDM").execute("UPDATE task_execution_summary SET execution_status='stopped' where task_execution_id = ? and execution_status not in ('completed', 'failed')",
                    task_execution_id);

            // TDM 5.1- cancel the migrate only if the input migration id is not null
            //TDM 6.0 - Loop over the list of migrate IDs
            for (Db.Row batchInfo : batchIdList) {
                String fabricExecID = "" + batchInfo.get("fabric_execution_id");
                String taskType = ("" + batchInfo.get("task_type")).toLowerCase();
                Long luID = (Long) batchInfo.get("lu_id");
                String luName = "" + batchInfo.get("lu_name");
                String taskExecutionID = "" + task_execution_id;
                ludb().execute("batch_pause '" + fabricExecID + "'");
                // TDM 7.1 Fix, stop execution of reference tables.
                //log.info("fnStopTaskExecution - Stopping the reference Handling for task_execution_id: " + task_execution_id + ", task_type: " + taskType);
                TdmSharedUtils.fnTdmCopyReference(String.valueOf(task_execution_id), taskType);

                if (luID > 0 && ("extract".equals(taskType))) {
                    //log.info("wsStopTaskExecution - Updating task_execution_entities");
                    TdmSharedUtils.fnTdmUpdateTaskExecutionEntities(taskExecutionID, luID, luName);
                }
            }
            return wrapWebServiceResults("SUCCESS", null, null);
        } catch (Exception e) {
            log.error("wsStopTaskExecution", e);
            return wrapWebServiceResults("FAILED", null, null);

        } finally {
            if (batchIdList != null) {
                batchIdList.close();
            }
        }
    }

    static List<Map<String, Object>> fnGetTaskPostExecutionProcesses(Long taskId) throws Exception {
        String query = "SELECT * FROM \"" + schema + "\".TASKS_POST_EXE_PROCESS  WHERE task_id =" + taskId;
        Db.Rows rows = db("TDM").fetch(query);

        List<Map<String, Object>> result = new ArrayList<>();
        List<String> columnNames = rows.getColumnNames();
        for (Db.Row row : rows) {
            ResultSet resultSet = row.resultSet();
            Map<String, Object> rowMap = new HashMap<>();
            for (String columnName : columnNames) {
                rowMap.put(columnName, resultSet.getObject(columnName));
            }
            result.add(rowMap);
        }
        return result;
    }

    static void fnSaveRefExeTablestoTask(Long task_id, Long taskExecutionId) throws Exception {
        List<Map<String, Object>> refs = (List<Map<String, Object>>) fnGetTaskReferenceTable(task_id);
        List<Map<String, Object>> refData = refs;
        if (refData.size() == 0) {
            return;
        }
        for (Map<String, Object> ref : refData) {
            String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.now());

            String query = "INSERT INTO \"" + schema + "\".task_ref_exe_stats (task_id,task_execution_id,task_ref_table_id, ref_table_name, update_date, execution_status) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            db("TDM").execute(query,
                    task_id,
                    taskExecutionId,
                    ref.get("task_ref_table_id"),
                    ref.get("ref_table_name"),
                    now,
                    "pending");
        }

    }

    static Object fnGetTaskReferenceTable(Long task_id) throws Exception {
        String query = "SELECT * FROM \"" + schema + "\".task_ref_tables where task_id = " + task_id;
        Db.Rows rows = db("TDM").fetch(query);
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> columnNames = rows.getColumnNames();
        for (Db.Row row : rows) {
            ResultSet resultSet = row.resultSet();
            Map<String, Object> rowMap = new HashMap<>();
            for (String columnName : columnNames) {
                rowMap.put(columnName, resultSet.getObject(columnName));
            }
            result.add(rowMap);
        }
        return result;
    }

    private static Object fnGetTaskExecSeqVal(String task_execution_id) throws Exception {
        //Sereen - fix : tdm_seq_mapping PG table is deleted so we fetch the data from tdm_seq_mapping fabric table
        //DBExecute(DB_FABRIC, "set sync off", null);
        //DBExecute(DB_FABRIC, "get TDM." + task_execution_id, null);
        ludb().execute("get TDM." + task_execution_id);
        String sql = "SELECT entity_target_id , lu_type, source_env, table_name, column_name, source_id, target_id, is_instance_id FROM tdm_seq_mapping";
        Db.Rows rows = db(DB_FABRIC).fetch(sql);
        return rows;
    }

    static Object fnResumeTaskExecution(Long task_execution_id) throws Exception {
        Boolean success_ind = true;
        Db.Rows batchIdList = null;
        try {
            //log.info("fnResumeTaskExecution - Starting");
            //TDM 6.0 - Get the list of migration IDs based on task execution ID, instead of getting one migrate_id as input
            batchIdList = db("TDM").fetch("select fabric_execution_id, execution_status, selection_method, l.task_type from task_execution_list l, tasks t " +
                    "where task_execution_id = ? and l.task_id = t.task_id and selection_method <> 'REF'" +
                    "and fabric_execution_id is not null", task_execution_id);

            db("TDM").execute("UPDATE task_execution_list SET execution_status='running' where fabric_execution_id is not null " +
                            "and lower(execution_status) = 'stopped' and task_execution_id = ?",
                    task_execution_id);

            // TDM 7, set the status in execution summary to running
            db("TDM").execute("UPDATE task_execution_summary SET execution_status='running' where task_execution_id = ? and execution_status = 'stopped'",
                    task_execution_id);
            db("TDM").execute("UPDATE task_execution_list SET execution_status='pending' where fabric_execution_id is null and task_execution_id = ? " +
                            "and lower(execution_status) = 'stopped' and task_id in (select task_id from tasks where lower(selection_method) <>'ref')",
                    task_execution_id);

            // TDM 5.1- add a reference handling- update the status of the reference tables to 'resume'.
            // The resume of the jobs for the tables will be handled by the new fabric listener user job for the reference copy.

            db("TDM").execute("UPDATE task_execution_list l SET execution_status='pending' where fabric_execution_id is null and task_execution_id = ? " +
                            "and (task_execution_id, lu_id) in (select task_execution_id, lu_id from task_ref_exe_stats r,  tasks_logical_units u, task_ref_tables s " +
                            "where l.task_execution_id = r.task_execution_id and l.lu_id = u.lu_id " +
                            "and l.task_id = u.task_id and u.lu_name = s.lu_name and s.task_ref_table_id = r.task_ref_table_id and s.task_id = l.task_id " +
                            "and lower(r.execution_status) = 'stopped')",
                    task_execution_id);
            db("TDM").execute("UPDATE task_ref_exe_stats set execution_status= 'resume' where task_execution_id = ? and lower(execution_status) = 'stopped'", task_execution_id);

            // TDM 5.1- cancel the migrate only if the input migration id is not null
            //TDM 6.0 - Loop over the list of migrate IDs
            for (Db.Row batchInfo : batchIdList) {
                fabric().execute("delete instance TDM.?", task_execution_id);
                db("TDM").execute("UPDATE task_execution_list SET synced_to_fabric = FALSE WHERE task_execution_id = ?", task_execution_id);
                fabric().execute("batch_retry '" + batchInfo.get("fabric_execution_id") + "'");
                // TDM 7.1 Fix, resume execution of reference tables.
                //log.info("fnResumeTaskExecution - Resume Reference");
                String taskType = ("" + batchInfo.get("task_type")).toLowerCase();
                TdmSharedUtils.fnTdmCopyReference(String.valueOf(task_execution_id), taskType);

            }
        } catch (Exception e) {
            success_ind = false;
            log.error("wsResumeTaskExecution: " + e);

        } finally {
            if (batchIdList != null) {
                batchIdList.close();
            }
        }

        return wrapWebServiceResults((success_ind ? "SUCCESS" : "FAILED"), null, success_ind);
    }

    static void fnStartTaskExecutions(List<Map<String, Object>> taskExecutions, Long taskExecutionId, String srcEnvName, Long tarEnvId, Long srcEnvId, String executionNote) throws Exception {
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        for (Map<String, Object> entry : taskExecutions) {
            String query = "INSERT INTO \"" + schema + "\".task_execution_list " +
                    "(task_id, task_execution_id, creation_date, be_id, environment_id, product_id, product_version, lu_id, " +
                    "data_center_name ,execution_status,parent_lu_id,source_env_name, task_executed_by, task_type, version_datetime, source_environment_id, process_id, execution_note) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String username = sessionUser().name();
			String userRoles = String.join(",", sessionUser().roles());
			String executedBy = new StringBuilder().append(username).append("##").append(userRoles).toString();
			//log.info("fnStartTaskExecutions - executedBy: " + executedBy);
            db("TDM").execute(query,
                    entry.get("task_id"),
                    taskExecutionId,
                    now,
                    entry.get("be_id"),
                    tarEnvId != null ? tarEnvId : entry.get("environment_id"),
                    entry.get("product_id"),
                    entry.get("product_version"),
                    entry.get("lu_id"),
                    entry.get("data_center_name"),
                    "Pending",
                    entry.get("lu_parent_id"),
                    srcEnvName != null ? srcEnvName : entry.get("source_env_name"),
                    executedBy,
                    entry.get("task_type"),
                    now,
                    srcEnvId != null ? srcEnvId : entry.get("source_environment_id"),
                    entry.get("process_id"),
					executionNote != null ? executionNote : null);
        }
    }


    static Object fnGetNumberOfMatchingEntities(String whereStmt, String sourceEnvName, Long beID) throws Exception {
        String sourceEnv = !Util.isEmpty(sourceEnvName) ? sourceEnvName : "_dev";
        String getEntitiesSql = TdmSharedUtils.generateListOfMatchingEntitiesQuery(beID, whereStmt, sourceEnv);
        Db tdmDB = db("TDM");
        Db.Rows rows = tdmDB.fetch("SELECT COUNT(entity_id) FROM (" + getEntitiesSql + " ) AS final_count");
        return wrapWebServiceResults("SUCCESS", null, rows.firstValue());
    }

    static HashMap<String, Object> fnMigrateStatusWs(String migrateId, List<String> runModes) throws Exception {
        HashMap<String, Object> result = new HashMap<>();
        for (String runMode : runModes) {
            Object data = fnGetBatchStats(migrateId, runMode);
            List<HashMap<String, Object>> results = new ArrayList<>();
            HashMap<String, Object> newData = new HashMap<>();
            if (runMode.equals("S")) {
                String[] columnName = new String[]{"Status", "Ent./sec (avg.)", "Ent./sec (pace)", "Start time (UTC)", "Duration", "End time (UTC)",
                        "Name", "Succeeded", "Failed", "Added", "Updated", "Unchanged", "Total", "Level", "Remaining dur.", "Remaining", "% Completed"};
                for (HashMap<String, Object> row : (List<HashMap>) ((HashMap<String, Object>) data).get("result")) {
                    row.put("Start time (UTC)", row.get("Start time"));
                    row.put("End time (UTC)", row.get("End time"));
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("columns", row);
                    results.add(map);
                }
                newData.put("results", results);
                newData.put("columnsNames", columnName);
                result.put(runMode, newData);
            } else {
                result.put(runMode, ((HashMap<String, Object>) data).get("result"));
            }
        }
        return result;
    }


    private static Object fnGetBatchStats(String i_batchId, String i_runMode) throws Exception {
        return wrapWebServiceResults("SUCCESS", null, TdmSharedUtils.fnBatchStats(i_batchId, i_runMode));
    }

    static Object fnExtractRefStats(String i_taskExecutionId, String i_runMode) throws Exception {
        Object rs = null;

        if (i_runMode.equals("S")) {

            //log.info("call to fnGetReferenceSummaryData");
            Map<String, Object> refSummaryStatsBuf = fnGetReferenceSummaryData(i_taskExecutionId);

            // Convert the dates to strings
            refSummaryStatsBuf.forEach((key, value) -> {
                String minStartDate = "";
                String maxEndDate = "";
                Map<String, Object> refSummaryStats = (HashMap) value;

                if (refSummaryStats.get("minStartExecutionDate") != null)
                    minStartDate = refSummaryStats.get("minStartExecutionDate").toString();

                if (refSummaryStats.get("maxEndExecutionDate") != null)
                    maxEndDate = refSummaryStats.get("maxEndExecutionDate").toString();

                refSummaryStats.put("minStartExecutionDate", minStartDate);
                refSummaryStats.put("maxEndExecutionDate", maxEndDate);

                //log.info("after calling fnGetReferenceSummaryData");
            });
            rs = refSummaryStatsBuf;
        } else if (i_runMode.equals("D")) {
            rs = fnGetReferenceDetailedData(i_taskExecutionId);
        }

        // convert iterable to serializable object
        if (rs instanceof Db.Rows) {
            ArrayList<Map> rows = new ArrayList<>();
            ((Db.Rows) rs).forEach(row -> {
                HashMap copy = new HashMap();
                copy.putAll(row);
                rows.add(copy);
            });
            rs = rows;
        }

        return wrapWebServiceResults("SUCCESS", null, rs);
    }

    static int getAllowedEntitySize(Integer entityListSize, Integer numberOfRequestedEntites) {
        int allowedEntitySize = -1;
        if (entityListSize > 0) {
            allowedEntitySize = entityListSize;
            if (numberOfRequestedEntites > 0 && numberOfRequestedEntites != entityListSize) {
                log.warn("The number of entities is set based on the overridden entity list, the given number of entities will be ignored.");
            }
        } else if (numberOfRequestedEntites > 0) {
            allowedEntitySize = numberOfRequestedEntites;
        }
        return allowedEntitySize;
    }

    static Map<String, Object> fnCheckMigrateStatusForEntitiesList(String entities_list, String task_execution_id, String lu_list) throws Exception {
        Map<String, Object> entity_mig_status = new LinkedHashMap<String, Object>();
        String[] entities_list_arr = entities_list.split(",");
        String[] lu_list_arr = lu_list.split(",");
        String entities_list_for_qry = "";
        String lu_list_for_qry = "";

        //create a string with added single quotes to each entity in the entities list + preliminary mark every entity in the list as exists,
        //since the entity was already validated against the root LU in the GUI
        for (int i = 0; i < entities_list_arr.length; i++) {
            entity_mig_status.put(entities_list_arr[i].trim(), "true");
            entities_list_for_qry += "'" + entities_list_arr[i].trim() + "',";
        }
        // remove last ,
        entities_list_for_qry = entities_list_for_qry.substring(0, entities_list_for_qry.length() - 1);

        for (int i = 0; i < lu_list_arr.length; i++) {
            lu_list_for_qry += "'" + lu_list_arr[i].trim() + "',";
        }
        // remove last ,
        lu_list_for_qry = lu_list_for_qry.substring(0, lu_list_for_qry.length() - 1);

        fabric().execute("GET TDM.?", task_execution_id);

        String getStatusesSql = "select distinct be_root_entity_id from task_execution_link_entities " +
                "where be_root_entity_id in (" + entities_list_for_qry + ") and lu_name in (" + lu_list_for_qry + ") and execution_status <> 'completed'";

        Db.Rows getStatuses = fabric().fetch(getStatusesSql);
        //Only entities that failed on any of the LUs will be returned, therefore for all the returned entities set the status to false
        for (Db.Row entityStatus : getStatuses) {
            entity_mig_status.put("" + entityStatus.cell(0), "false");
        }

        return wrapWebServiceResults("SUCCESS", null, entity_mig_status);
    }

    static Boolean fnTestTaskInterfaces(Long task_id, Boolean forced, Long srcEnvId, Long tarEnvId) throws Exception {
        if (task_id != null && forced == true) return true;
        fnTestEnvTargetInterfaces(task_id, tarEnvId, forced);
        fnTestEnvSourceInterfaces(task_id, srcEnvId);
        return true;
    }

    static Boolean fnTestEnvTargetInterfaces(Long taskId, Long tarEnvId, Boolean forced) throws Exception {
        List<Map<String, Object>> taskData;
        try {
            taskData = fnGetTaskEnvData(taskId, tarEnvId, false);
        } catch (Exception e) {
            throw new Exception("Failed to get Task data for target env");
        }
        if (taskData.get(0).get("task_type").equals("EXTRACT")) return true;

        try {
            return fnTestInterfacesForEnvProduct(taskData.get(0).get("environment_name") != null ? taskData.get(0).get("environment_name").toString() : null);
        } catch (Exception e) {
            if (e.getMessage().indexOf("interfaceFailed;") == 0) {
                throw new Exception("The test connection of " + e.getMessage().substring(16) + " failed. Please check the connection details of target environment " + taskData.get(0).get("environment_name"));
            } else throw new Exception("Failed to test target env interfaces");
        }
    }

    static Boolean fnTestInterfacesForEnvProduct(String source) throws Exception {
        Map<String, String> data;
        try {
            data = (Map<String, String>) ((Map<String, Object>) fnTestConnectionForEnv(source)).get("result");
        } catch (Exception e) {
            throw new Exception("Failed to get interfaces for env from fabric");
        }

        List<String> errorInterfaces = new ArrayList<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (data.get(entry.getKey()).equals("false"))
                errorInterfaces.add(entry.getKey());
        }

        if (errorInterfaces.size() == 0) {
            return true;
        } else throw new Exception("interfaceFailed;" + errorInterfaces.toString());
    }


    static List<Map<String, Object>> fnGetTaskEnvData(Long task_id, Long envId, Boolean source) throws Exception {
        String sql = "SELECT environments.environment_name, * FROM \"" + schema + "\".tasks " +
                "INNER JOIN \"" + schema + "\".tasks_logical_units " +
                "ON (\"" + schema + "\".tasks.task_id = \"" + schema + "\".tasks_logical_units.task_id) " +
                "INNER JOIN \"" + schema + "\".product_logical_units " +
                "ON (\"" + schema + "\".product_logical_units.lu_id = \"" + schema + "\".tasks_logical_units.lu_id ) " +
                "INNER JOIN \"" + schema + "\".environments " +
                (source ?
                        "ON (\"" + schema + "\".environments.environment_id =" + (envId != null ? envId + ") " : "\"" + schema + "\".tasks.source_environment_id ) ") :
                        "ON (\"" + schema + "\".environments.environment_id =" + (envId != null ? envId + ") " : "\"" + schema + "\".tasks.environment_id ) ")) +
                "INNER JOIN \"" + schema + "\".environment_products " +
                "ON (\"" + schema + "\".environment_products.status = \'Active\' " +
                "AND \"" + schema + "\".environment_products.product_id = \"" + schema + "\".product_logical_units.product_id " +
                (source ?
                        "AND (\"" + schema + "\".environment_products.environment_id =" + (envId != null ? envId + ")) " : "\"" + schema + "\".tasks.source_environment_id )) ") :
                        "AND (\"" + schema + "\".environment_products.environment_id =" + (envId != null ? envId + ")) " : "\"" + schema + "\".tasks.environment_id )) ")) +
                "WHERE tasks.task_id = " + task_id;
        Db.Rows rows = db("TDM").fetch(sql);

        List<Map<String, Object>> result = new ArrayList<>();
        List<String> columnNames = rows.getColumnNames();
        for (Db.Row row : rows) {
            ResultSet resultSet = row.resultSet();
            Map<String, Object> rowMap = new HashMap<>();
            for (String columnName : columnNames) {
                rowMap.put(columnName, resultSet.getObject(columnName));
            }
            result.add(rowMap);
        }
        return result;
    }

    static Boolean fnTestEnvSourceInterfaces(Long task_id, Long srcEnvId) throws Exception {
        List<Map<String, Object>> taskData = null;
        try {
            taskData = fnGetTaskEnvData(task_id, srcEnvId, true);
        } catch (Exception e) {
            throw new Exception("Failed to get task source env data");
        }

        if (taskData != null) {
            for (int i = 0; i < taskData.size(); i++) {
                try {
                    fnTestInterfacesForEnvProduct(taskData.get(i).get("environment_name").toString());
                } catch (Exception e) {
                    if (e.getMessage().indexOf("interfaceFailed;") == 0) {
                        String errMessage = "The test connection of " + e.getMessage().substring(16) + " failed. Please check the connection details of source environment " + taskData.get(0).get("environment_name");
                        //if (taskData.get("task_type").toString().equals("EXTRACT"))  throw new Exception(errMessage);
                        try {
                            return fnTestTaskEnvGlobals(task_id, (Long) taskData.get(0).get("environment_id"));
                        } catch (Exception exc) {
                            throw new Exception(errMessage);
                        }
                    }
                }
            }
        }
        return true;
    }

    static Boolean fnTestTaskEnvGlobals(Long task_id, Long environment_id) throws Exception {
        Map<String, Object> data = null;
        try {
            data = fnGetTaskEnvGlobals(task_id, environment_id);
        } catch (Exception e) {
            throw new Exception("Failed to get globals for task");
        }
        if (data == null) throw new Exception("tdm_set_sync_off doesn't exist in task/env globals");

        Map<String, Object> tdmSetGlobalTask = null;
        for (Map<String, Object> global : (List<Map<String, Object>>) data.get("globals")) {
            if (global.get("global_name") != null && ((String) global.get("global_name")).equals("tdm_set_sync_off")) {
                tdmSetGlobalTask = global;
                break;
            }
        }

        if (tdmSetGlobalTask != null) {
            if (tdmSetGlobalTask.get("global_value") != null && ((String) tdmSetGlobalTask.get("global_value")).equals("true")) {
                return true;
            } else {
                throw new Exception("tdm_set_sync_off is not true in task globals");
            }
        }


        Map<String, Object> tdmSetGlobalEnv = null;
        for (Map<String, Object> global : (List<Map<String, Object>>) data.get("env_globals")) {
            if (global.get("global_name") != null && ((String) global.get("global_name")).equals("tdm_set_sync_off")) {
                tdmSetGlobalEnv = global;
                break;
            }
        }

        if (tdmSetGlobalEnv != null) {
            if (tdmSetGlobalEnv.get("global_value") != null && ((String) tdmSetGlobalEnv.get("global_value")).equals("true")) {
                return true;
            } else {
                throw new Exception("tdm_set_sync_off is not true in env globals");
            }
        }

        throw new Exception("tdm_set_sync_off doesn't exist in task/env globals");
    }

    static Map<String, Object> fnGetTaskEnvGlobals(Long task_id, Long env_id) throws Exception {
        Map<String, Object> ans = new HashMap<>();

        String query = "SELECT * FROM \"" + schema + "\".task_globals " +
                "WHERE task_globals.task_id = " + task_id;
        Db.Rows rows = db("TDM").fetch(query);

        List<Map<String, Object>> result = new ArrayList<>();
        List<String> columnNames = rows.getColumnNames();
        for (Db.Row row : rows) {
            ResultSet resultSet = row.resultSet();
            Map<String, Object> rowMap = new HashMap<>();
            for (String columnName : columnNames) {
                rowMap.put(columnName, resultSet.getObject(columnName));
            }
            result.add(rowMap);
        }

        for (Map<String, Object> global : result) {
            global.put("global_name", ((String) global.get("global_name")).toLowerCase());
            global.put("global_value", ((String) global.get("global_value")).toLowerCase());
        }

        ans.put("globals", result);

        query = "SELECT * FROM \"" + schema + "\".tdm_env_globals " +
                "WHERE tdm_env_globals.environment_id = " + env_id;

        Db.Rows envGlobalsrows = db("TDM").fetch(query);

        List<Map<String, Object>> envResult = new ArrayList<>();
        columnNames = envGlobalsrows.getColumnNames();
        for (Db.Row row : envGlobalsrows) {
            ResultSet resultSet = row.resultSet();
            Map<String, Object> rowMap = new HashMap<>();
            for (String columnName : columnNames) {
                rowMap.put(columnName, resultSet.getObject(columnName));
            }
            envResult.add(rowMap);
        }

        for (Map<String, Object> global : envResult) {
            global.put("global_name", ((String) global.get("global_name")).toLowerCase());
            global.put("global_value", ((String) global.get("global_value")).toLowerCase());
        }

        ans.put("env_globals", envResult);
        return ans;
    }

    static Object fnIsTaskRunning(Long taskId) throws Exception {
        String sql = "SELECT count(*) FROM \"" + schema + "\".task_execution_list " +
                "WHERE task_id = " + taskId + " AND " +
                "(lower(execution_status) <> \'failed\' AND lower(execution_status) <> \'completed\' AND " +
                "lower(execution_status) <> \'stopped\' AND lower(execution_status) <> \'killed\')";
        Db.Rows rows = db("TDM").fetch(sql);
        return rows.firstRow().cell(0);
    }

    static Boolean fnIsTaskActive(Long taskId) throws Exception {
        String query = "SELECT * FROM \"" + schema + "\".tasks " +
                "WHERE task_id = " + taskId + " AND " +
                "task_status = \'Active\'";
        Db.Rows rows = db("TDM").fetch(query);
        if (!rows.firstRow().isEmpty()) {
            return true;
        } else {
            throw new Exception("This task was changed and is currently inactive. Please refresh the page first to execute the task.");
        }
    }

    static List<Map<String, Object>> fnGetActiveTaskForActivation(Long taskId) throws Exception {
        String clientQuery = "SELECT *, " +
                "( SELECT COUNT(*) FROM task_ref_tables WHERE task_ref_tables.task_id = tasks.task_id ) AS refcount " +
                "FROM tasks " +
                "INNER JOIN tasks_logical_units " +
                "ON (tasks.task_id = tasks_logical_units.task_id) " +
                "INNER JOIN product_logical_units " +
                "ON (product_logical_units.lu_id = tasks_logical_units.lu_id ) " +
                "INNER JOIN environment_products " +
                "ON (environment_products.status = \'Active\' " +
                "AND environment_products.product_id = product_logical_units.product_id " +
                "AND (environment_products.environment_id = tasks.environment_id " +
                "OR (tasks.environment_id IS NULL " +
                "AND environment_products.environment_id = tasks.source_environment_id ))) " +
                "WHERE tasks.task_id = " + taskId;
        //log.info(clientQuery);
        Db.Rows rows = db("TDM").fetch(clientQuery);

        List<Map<String, Object>> executions = new ArrayList<>();
        List<String> columnNames = rows.getColumnNames();
        for (Db.Row row : rows) {
            ResultSet resultSet = row.resultSet();
            Map<String, Object> execution = new HashMap<>();
            for (String columnName : columnNames) {
                execution.put(columnName, resultSet.getObject(columnName));
            }
            execution.put("process_id", 0);
            executions.add(execution);
        }

        try {
            List<Map<String, Object>> data = TaskExecutionUtils.fnGetTaskPostExecutionProcesses(taskId);
            Map<String, Object> execution = new HashMap(executions.get(0));
            execution.put("lu_id", 0);
            execution.put("product_id", 0);
            execution.put("product_version", 0);
            for (Map<String, Object> item : data) {
                Map<String, Object> newItem = new HashMap<>(execution);
                newItem.put("process_id", item.get("process_id"));
                newItem.put("process_name", item.get("process_name"));
                newItem.put("execution_order", item.get("execution_order"));
                executions.add(newItem);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return executions;
    }

    static void fnCreateSummaryRecord(Map<String, Object> taskExecution, Long taskExecutionId, String srcEnvName, Long tarEnvId, Long srcEnvId) throws Exception {
        Map<String, Object> entry = taskExecution;
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        String username = sessionUser().name();

        String query = "INSERT INTO \"" + schema + "\".task_execution_summary " +
                "(task_execution_id, task_id , task_type, creation_date, be_id, environment_id, execution_status, start_execution_time, end_execution_time," +
                " tot_num_of_processed_root_entities, tot_num_of_copied_root_entities, tot_num_of_failed_root_entities, tot_num_of_processed_ref_tables, tot_num_of_copied_ref_tables," +
                " tot_num_of_failed_ref_tables, source_env_name, source_environment_id, fabric_environment_name, task_executed_by, version_datetime, version_expiration_date, update_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        db("TDM").execute(query,
                taskExecutionId,
                entry.get("task_id"),
                entry.get("task_type"),
                now,
                entry.get("be_id"),
                tarEnvId != null ? tarEnvId : entry.get("environment_id"),
                "Pending",
                entry.get("start_execution_time"),
                entry.get("end_execution_time"),
                entry.get("tot_num_of_processed_root_entities"),
                entry.get("tot_num_of_copied_root_entities"),
                entry.get("tot_num_of_failed_root_entities"),
                entry.get("tot_num_of_processed_ref_tables"),
                entry.get("tot_num_of_copied_ref_tables"),
                entry.get("tot_num_of_failed_ref_tables"),
                srcEnvName != null ? srcEnvName : entry.get("source_env_name"),
                srcEnvId != null ? srcEnvId : entry.get("source_environment_id"),
                entry.get("fabric_environment_name"),
                username,
                entry.get("version_datetime"),
                entry.get("version_expiration_date"),
                entry.get("update_date"));
    }

    public static void fnInsertActivity(String action, String entity, String description) throws Exception {
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        String username = sessionUser().name();
        String userId = username;
        String sql = "INSERT INTO \"" + schema + "\".activities " +
                "(date, action, entity, user_id, username, description) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        db("TDM").execute(sql, now, action, entity, userId, username, description);
    }

    static void fnPostTaskLogicalUnits(Long taskId, List<Map<String, Object>> logicalUnits) throws Exception {
        String sql = "DELETE FROM \"" + schema + "\".tasks_logical_units WHERE \"" + schema + "\".tasks_logical_units.task_id = " + taskId;
        db("TDM").execute(sql);
        if (logicalUnits != null) {
            for (Map<String, Object> logicalUnit : logicalUnits) {
                db("TDM").execute("INSERT INTO \"" + schema + "\".tasks_logical_units (task_id, lu_id,lu_name) VALUES ( ?, ?, ?)",
                        taskId, logicalUnit.get("lu_id"), logicalUnit.get("lu_name"));
            }
        }
    }

    static void fnSaveRefTablestoTask(Long taskId, List<Map<String, Object>> refList) {
        try {
            for (Map<String, Object> ref : refList) {
                String sql = "INSERT INTO \"" + schema + "\".task_ref_tables (task_id, ref_table_name,lu_name, schema_name, interface_name, update_date) VALUES (?, ?, ?, ?, ?, ?)";
                db("TDM").execute(sql,
                        taskId, ref.get("reference_table_name") != null ? ref.get("reference_table_name") : ref.get("ref_table_name"),
                        ref.get("logical_unit_name") != null ? ref.get("logical_unit_name") : ref.get("lu_name"),
                        ref.get("schema_name"),
                        ref.get("interface_name"),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                                .withZone(ZoneOffset.UTC)
                                .format(Instant.now()));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    static Db.Rows fnGetRefTableForLoadWithoutVersion(List<String> logicalUnits) throws Exception {
        String lus = "";
        for (String lu : logicalUnits) lus += "'" + lu + "',";
        lus = lus.substring(0, lus.length() - 1);
        String sql = "Select distinct ref.lu_name, ref.ref_table_name, ref.schema_name, ref.interface_name " +
                "from TASK_REF_TABLES ref, TASK_REF_EXE_STATS exe, task_execution_list l " +
                "Where ref.task_ref_table_id  = exe.task_ref_table_id " +
                "And ref.lu_name in (" + lus + ") " +
                "And exe.execution_status = \'completed\' " +
                "and exe.task_execution_id = l.task_execution_id " +
                "and lower(l.execution_status) = \'completed\' " +
                "and l.version_expiration_date is null;";
        Db.Rows rows = db("TDM").fetch(sql);
        return rows;
    }

    static List<Map<String, Object>> fnGetRefTableForLoadWithVersion(List<String> logicalUnits) throws Exception {
        String lus = "";
        for (String lu : logicalUnits) lus += "'" + lu + "',";
        lus = lus.substring(0, lus.length() - 1);
        String sql = "Select distinct ref.lu_name, ref.ref_table_name, ref.schema_name, ref.interface_name , exe.execution_status , exe.task_execution_id " +
                "from task_ref_tables ref, task_ref_exe_stats exe , task_execution_list l " +
                "Where ref.ref_table_name  = exe.ref_table_name " +
                "And ref.lu_name in (" + lus + ") " +
                "And exe.execution_status = \'completed\' " +
                "and exe.task_execution_id = l.task_execution_id " +
                "and lower(l.execution_status) = \'completed\' " +
                "and l.version_expiration_date > CURRENT_TIMESTAMP AT TIME ZONE \'UTC\'";
        Db.Rows rows = db("TDM").fetch(sql);

        List<Map<String, Object>> result = new ArrayList<>();
        List<String> columnNames = rows.getColumnNames();
        for (Db.Row row : rows) {
            ResultSet resultSet = row.resultSet();
            Map<String, Object> rowMap = new HashMap<>();
            for (String columnName : columnNames) {
                rowMap.put(columnName, resultSet.getObject(columnName));
            }
            result.add(rowMap);
        }

        for (Map<String, Object> row : result) {
            String id = row.get("lu_name") + "_" + row.get("ref_table_name") + "_" +
                    row.get("schema_name") + "_" + row.get("interface_name");
            row.put("tId", id);
        }


        for (int i = 0; i < result.size(); i++) {
            for (int j = i + 1; j < result.size(); j++) {
                if (result.get(i).get("tId").toString().equals(result.get(j).get("tId").toString())) {
                    result.remove(j);
                    j--;
                }
            }
        }

        return result;
    }


    static List<Map<String, Object>> fnGetVersionForLoadRef(List<String> refList, String source_env_name, String fromDate, String toDate) throws Exception {

		String versionDatesCond = "";
		if (fromDate != null && !"".equals(fromDate) && toDate != null && "".equals(toDate)) {
			versionDatesCond = "and l.version_datetime::date >= '" + fromDate + "' and l.version_datetime::date <= '" + toDate + "' ";
		}
		
        String query = "SELECT t.task_title as version_name, t.task_id, l.task_execution_id, t.task_last_updated_by, l.task_execution_id, l.fabric_execution_id,  " +
                "CASE when t.selection_method= \'ALL\' then  \'ALL\' " +
                "when t.selection_method= \'REF\' then \'REF\' " +
                "else \'Selected Entities\' END version_Type, " +
                "l.version_datetime, lu.lu_name, l.num_of_copied_entities as num_of_succeeded_entities, l.num_of_failed_entities, l.execution_note " +
                "FROM tasks t, task_execution_list l, tasks_logical_units lu, " +
                "(select  array_agg(lower(e.ref_table_name)) ref_list, array_agg(distinct lower(t.lu_name))  lu_list, task_execution_id " +
                "from task_ref_exe_stats e, task_ref_tables t where e.task_ref_table_id = t.task_ref_table_id and e.execution_status = \'completed\' " +
                "group by task_execution_id) ref " +
                "where lower(t.task_Type) = \'extract\'  " +
                "and t.task_id = l.task_id " +
                "and t.source_env_name = \'" + source_env_name + "\' " +
                "and lower(l.execution_status) = \'completed\' " +
                versionDatesCond +
                "and l.version_expiration_date > CURRENT_TIMESTAMP AT TIME ZONE \'UTC\' " +
                "and l.task_execution_id = ref.task_execution_id " +
                "and l.lu_id = lu.lu_id and l.task_id = lu.task_id ";

        for (String ref : refList) {
            query = query + "and lower(\'" + ref.toLowerCase() + "\') = ANY(ref_list) ";
        }
        Db.Rows rows = db("TDM").fetch(query);

        List<Map<String, Object>> result = new ArrayList<>();
        List<String> columnNames = rows.getColumnNames();
        for (Db.Row row : rows) {
            ResultSet resultSet = row.resultSet();
            Map<String, Object> rowMap = new HashMap<>();
            for (String columnName : columnNames) {
                rowMap.put(columnName, resultSet.getObject(columnName));
            }
            result.add(rowMap);
        }
        return result;
    }


    static List<Map<String, Object>> fnGetLogicalUnitsForSourceAndTargetEnv(Long targetEnvId, Long srcEnvId) throws Exception {

        List<Map<String, Object>> result = new ArrayList<>();
        String query = "SELECT * FROM \"" + schema + "\".product_logical_units " +
                "INNER JOIN \"" + schema + "\".products " +
                "ON (\"" + schema + "\".product_logical_units.product_id = \"" + schema + "\".products.product_id AND \"" + schema + "\".products.product_status = \'Active\') " +
                "INNER JOIN \"" + schema + "\".environment_products " +
                "ON (\"" + schema + "\".product_logical_units.product_id = \"" + schema + "\".environment_products.product_id AND \"" + schema + "\".environment_products.status = \'Active\') " +
                "WHERE environment_id = " + srcEnvId + " OR environment_id = " + targetEnvId;
        Db.Rows rows = db("TDM").fetch(query);

        List<String> columnNames = rows.getColumnNames();
        for (Db.Row row : rows) {
            ResultSet resultSet = row.resultSet();
            Map<String, Object> rowMap = new HashMap<>();
            for (String columnName : columnNames) {
                rowMap.put(columnName, resultSet.getObject(columnName));
            }
            result.add(rowMap);
        }


        for (int i = 0; i < result.size(); i++) {
            Map<String, Object> row = result.get(i);
            searchEqual:
            for (int j = i + 1; j < result.size(); j++) {
                Map<String, Object> otherRow = result.get(j);
                if (otherRow.get("lu_name").equals(row.get("lu_name"))) {
                    for (String columnName : columnNames) {
                        if (row.get(columnName) == null && otherRow.get(columnName) == null) continue;
                        if (row.get(columnName) == null || otherRow.get(columnName) == null) continue searchEqual;
                        if (!"lu_name".equals(columnName) && (!otherRow.get(columnName).toString().equals(row.get(columnName).toString())))
                            continue searchEqual;
                    }
                    result.remove(j);
                    j--;
                }
            }
        }


        return result;
    }

    static Map<String, Object> fnGetVersionsForLoad(String entitiesList, Long be_id, String source_env_name, String fromDate, String toDate, List<Map<String, Object>> lu_list, String target_env_name) throws Exception {
        Map<String, Object> result = new HashMap<>();
		String clientQuery = "";
        String logicalUnitList = "";
        for (Map<String, Object> lu : lu_list) {
            logicalUnitList += ("'" + lu.get("lu_name") + "',");
        }
        if (logicalUnitList != "") logicalUnitList = logicalUnitList.substring(0, logicalUnitList.length() - 1);

        String logicalUnitListEqual = "";
        for (Map<String, Object> lu : lu_list) {
            logicalUnitListEqual = logicalUnitListEqual + " and lower('" + lu.get("lu_name") + "') = any(lu_list) ";
        }
	
		String versionDatesCond = "";
		if (fromDate != null && !"".equals(fromDate) && toDate != null && !"".equals(toDate)) {
			versionDatesCond = "and l.version_datetime::date >= '" + fromDate + "' and l.version_datetime::date <= '" + toDate + "' ";
		}

        clientQuery = "with lu_list as (select l.task_execution_id, array_agg(lower(lu.lu_name)) lu_list " +
                "from task_execution_list l, product_logical_units lu where l.lu_id = lu.lu_id and lower(l.execution_status) = 'completed'  " +
                "group by task_execution_id) " +
                "select distinct t1.task_title version_name, t1.task_id, l1.task_execution_id, t1.task_last_updated_by, " +
                "CASE when t1.selection_method='ALL' then  'ALL' else  'Selected Entities' END version_Type, " +
                "l1.num_of_processed_entities number_of_extracted_entities, l1.version_datetime , l1.task_execution_id, tlu.lu_name, l1.fabric_execution_id, " +
                "CASE when plu.lu_parent_id is null then 'Y' else 'N' END root_indicator, " +
				"l1.num_of_copied_entities as num_of_succeeded_entities, l1.num_of_failed_entities, l1.execution_note, " +
				"ROW_NUMBER () OVER (PARTITION BY t1.task_title, l1.lu_id ORDER BY l1.task_execution_id) version_no " +
                "from task_execution_list l1, tasks t1, tasks_logical_units tlu, product_logical_units plu where  " +
                "(t1.task_title, t1.task_id, l1.version_datetime, l1.task_execution_id, l1.lu_id) in  " +
                "(SELECT distinct t.task_title as version_name, t.task_id, l.version_datetime , l.task_execution_id, l.lu_id FROM tasks t, task_execution_list l, lu_list  " +
                " where lower(t.task_Type) = 'extract' and t.task_id = l.task_id " +
                "and t.source_env_name = '" + source_env_name + "' " +
                "and lower(l.execution_status) = 'completed' " +
                versionDatesCond +
                "and l.version_expiration_date > CURRENT_TIMESTAMP AT TIME ZONE 'UTC' " +
                "and l.lu_id in (select lu_id from tasks_logical_units u " +
                "where l.task_id = u.task_id and u.lu_name in " +
                "(" + logicalUnitList + "))" +
                "and l.task_execution_id = lu_list.task_execution_id" +
                logicalUnitListEqual + ")" +
                "and t1.task_id = l1.task_id and lower(l1.execution_status) = 'completed' " +
                "and l1.task_id = tlu.task_id and l1.lu_id = tlu.lu_id " +
                "and l1.lu_id = plu.lu_id " +
                "and (plu.lu_parent_id is not null or l1.num_of_copied_entities > 0 ";

		// TDM 7.4 - 13-Jan-22 - Check the entity list against the task_execution_entities table, to support 2 things:
		// 1. Validate that the entities were extracted successfully at least at Root Lu Level
		// 2. In case of override task, we need to check the entities against the entities that were handled at run time and not the entities at task creation time.
	
        /*if (entitiesList != null) {
            if (entitiesList.length() > 0) {
                String[] numberOfEntitiesArray = entitiesList.split(",");
                clientQuery = clientQuery +
                        "and ((t1.selection_method='ALL' or " + be_id + " != plu.be_id) or (";
                for (int i = 0; i < numberOfEntitiesArray.length; i++) {
                    clientQuery = clientQuery + " ('" + numberOfEntitiesArray[i] + "' = ANY(string_to_array(selection_param_value, ',')))";
                    if (i < numberOfEntitiesArray.length - 1) {
                        clientQuery = clientQuery + " and ";
                    }
                }
                clientQuery += "))";
            }
        }*/
	
		if (entitiesList != null && entitiesList.length() > 0) {
			int  numOfEntities = StringUtils.countMatches(entitiesList, ",") + 1;
			clientQuery +=
					" and (" + be_id + " != plu.be_id or (";
			clientQuery += numOfEntities + " = (select count(1) from task_execution_entities te " +
					"where te.task_execution_id = cast (l1.task_execution_id as text) and te.lu_name = plu.lu_name and plu.lu_parent_id is null " +
					"and te.iid = ANY(string_to_array('" + entitiesList + "', ',')) and execution_status = 'completed'))) ) ";
        } else {
			clientQuery += ")";
		}
		// End of TDM 7.4 change
		
        clientQuery += " order by task_id, task_execution_id;";
		//log.info("fnGetVersionsForLoad - clientQuery: " + clientQuery);
        Db.Rows rows = db("TDM").fetch(clientQuery);

		//TDM7.4 - 26/1/2022 - Check for each entity if it is reserved by other user.
		
		result.put("ListOfVersions", rows);
		Map<String, Object> validation = new HashMap<>();
		if (entitiesList != null && entitiesList.length() > 0) {
			//log.info("target_env_name: " + target_env_name);
			String envID = "" + db("TDM").fetch("SELECT environment_id from environments where environment_name = ? and environment_status = 'Active'",
				target_env_name).firstValue();
			String[] strings = entitiesList.split(",");
			ArrayList<String> entities = new ArrayList<>();
			Collections.addAll(entities, strings); 
			String userName = ""; //The function will use calling user name
			
			validation = fnValidateReservedEntities(Long.toString(be_id), envID, entities);
		}
		
		result.put("EntityReservationValidations", validation);
        return result;
	}


    static List<Map<String, Object>> fnGetRootLUs(String taskExecutionId) throws Exception {
        String sql = "select t.task_execution_id, lu_name ,l.lu_id, " +
                "(select count(*) from task_Execution_list t " +
                "where parent_lu_id = l.lu_id and t.task_execution_id = \'" + taskExecutionId + "\')," +
                "case when (num_of_failed_entities>0 or num_of_failed_ref_tables> 0) " +
                "then \'failed\' else \'completed\' end lu_status from task_Execution_list t, " +
                "(select lu_id, lu_name from product_logical_units) l " +
                "where t.task_execution_id =\'" + taskExecutionId + "\' and " +
                "t.parent_lu_id is null and t.lu_id = l.lu_id";
        Db.Rows rows = db("TDM").fetch(sql);

        List<Map<String, Object>> result = new ArrayList<>();
        List<String> columnNames = rows.getColumnNames();
        for (Db.Row row : rows) {
            ResultSet resultSet = row.resultSet();
            Map<String, Object> rowMap = new HashMap<>();
            for (String columnName : columnNames) {
                rowMap.put(columnName, resultSet.getObject(columnName));
            }
            result.add(rowMap);
        }
        return result;
    }

    static List<Map<String, Object>> fnUpdateFailedLUsInTree(List<Map<String, Object>> rootLUs, Map<String, Object> failedEntities) {
        List<Map<String, Object>> tree = new ArrayList<>();

        Object entitiesList = failedEntities.get("entitiesList");
        if (failedEntities != null && ((List) entitiesList).size() > 0) {
            for (Map<String, Object> entity : (List<Map<String, Object>>) entitiesList) {
                List<Object> fullPathError = (List<Object>) entity.get("Full Error Path");
                tree = fnBuildTreeFromFullPathError(fullPathError, tree);
            }
        }

        for (Map<String, Object> rootLU : rootLUs) {
            Map<String, Object> found = null;
            for (Map<String, Object> e : tree) {
                if (e.get("lu_name").toString().equals(rootLU.get("lu_name").toString())) {
                    found = e;
                    break;
                }
            }

            if (found != null) {
                found.put("isRoot", true);
                found.put("count", rootLU.get("count"));
                found.put("errorInPath", true);
            } else {
                rootLU.put("isRoot", true);
                tree.add(rootLU);
            }
        }

        for (Map<String, Object> node : tree) {
            fnTreeIterate(node);
        }
        return tree;
    }

    static void fnTreeIterate(Map<String, Object> current) {
        List<Map<String, Object>> currentChildren = (List<Map<String, Object>>) current.get("children");
        if (currentChildren == null || currentChildren.size() == 0) {
            current.put("collapsed", true);
            if (current.get("count") != null && Long.parseLong(current.get("count").toString()) > 0) {
                current.put("hasChildren", true);
            } else {
                current.put("hasChildren", false);
            }
            return;
        }
        for (int i = 0, len = currentChildren.size(); i < len; i++) {
            fnTreeIterate(currentChildren.get(i));
        }
    }

    static List<Map<String, Object>> fnBuildTreeFromFullPathError(List<Object> list, List<Map<String, Object>> roots) {
        Map<String, Integer> map = new HashMap<>();
        Map<String, Object> node;

        for (int i = 0; i < list.size(); i += 1) {
            map.put(((Map<String, Object>) list.get(i)).get("luName").toString(), i); // initialize the map
            ((Map<String, Object>) list.get(i)).put("children", fnGetNodeChildren(roots, list, i)); // initialize the children
        }

        for (int i = 0; i < list.size(); i += 1) {
            node = ((Map<String, Object>) list.get(i));

            if (node.get("parentLuName") != null && !node.get("parentLuName").toString().equals("") && !node.get("parentLuName").toString().equals(node.get("luName").toString())) {
                // if you have dangling branches check that map[node.parentId] exists
                List<Map<String, Object>> children = (List<Map<String, Object>>) ((Map<String, Object>) list.get(map.get(node.get("parentLuName")))).get("children");

                Map<String, Object> found = null;
                for (Map<String, Object> e : children) {
                    if (e.get("lu_name").toString().equals(node.get("luName").toString())) {
                        found = e;
                        break;
                    }
                }

                if (found == null) {
                    HashMap<String, Object> nodeMap = new HashMap<>();
                    nodeMap.put("lu_name", node.get("luName"));
                    nodeMap.put("children", node.get("children") != null ? node.get("children") : new ArrayList<>());
                    nodeMap.put("collapsed", true);
                    nodeMap.put("hasChildren", true);
                    children.add(nodeMap);
                }
            } else {
                Map<String, Object> found = null;
                for (Map<String, Object> e : roots) {
                    if (e.get("lu_name").toString().equals(node.get("luName"))) {
                        found = e;
                        break;
                    }
                }
                if (found == null) {
                    HashMap<String, Object> nodeMap = new HashMap<>();
                    nodeMap.put("lu_name", node.get("luName"));
                    nodeMap.put("children", node.get("children") != null ? node.get("children") : new ArrayList<>());
                    nodeMap.put("collapsed", true);
                    nodeMap.put("hasChildren", true);
                    roots.add(nodeMap);
                }
            }
        }

        node = roots.get(0);
        while (node != null) {
            List<Map<String, Object>> nodeChildren = (List<Map<String, Object>>) node.get("children");
            if (nodeChildren.size() > 0 && list.size() > 1) {
                node = nodeChildren.get(0);
            } else {
                node.put("lu_status", "failed");
                node = null;
            }
        }
        return roots;
    }

    static List<Map<String, Object>> fnGetNodeChildren(List<Map<String, Object>> roots, List<Object> list, int index) {
        List<Map<String, Object>> treeNode = roots;
        for (int i = 0; i <= index; i++) {
            Map<String, Object> treeNodeMap = null;
            for (Map<String, Object> e : treeNode) {
                if (e.get("lu_name").toString().equals(((Map<String, Object>) list.get(i)).get("luName")))
                    treeNodeMap = e;
            }
            if (treeNodeMap == null) return new ArrayList<>();
            treeNode = treeNodeMap.get("children") != null ? (List<Map<String, Object>>) treeNodeMap.get("children") : new ArrayList<>();
        }
        return treeNode != null ? treeNode : new ArrayList<>();
    }

    private static Object fnTestConnectionForEnv(String env) throws Exception {
        Log log = Log.a(com.k2view.cdbms.usercode.lu.k2_ws.TDM_Tasks.Logic.class);

        if (Util.isEmpty(env)) {
            env = "_dev";
        }

        fabric().execute("set environment='" + env + "';");

        Map<Object, Object> connResMap = new HashMap<>();
        fabric().fetch("test_connection active=true;").forEach(i -> {
			if (!"custom".equalsIgnoreCase("" + i.get("type"))) {
	            connResMap.put(i.get("interface"), "" + i.get("passed"));
			}
        });

        return wrapWebServiceResults("SUCCESS", null, connResMap);
    }

    static List<Map<String, Object>> fnGetUserRoleIdsAndEnvTypeByEnvName(String envName) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        String userId = sessionUser().name();
        String permissionGroup = fnGetUserPermissionGroup("");
		
		String fabricRoles = String.join(",", sessionUser().roles());
	
		//log.info("fnGetUserRoleIdsAndEnvTypeByEnvName - fabricRoles: " + fabricRoles);
        if ("admin".equalsIgnoreCase(permissionGroup)) {
            String allEnvs = "Select env.environment_id,env.environment_name,\n" +
                    "  Case When env.allow_read = True And env.allow_write = True Then 'BOTH'\n" +
                    "    When env.allow_write = True Then 'TARGET' Else 'SOURCE'\n" +
                    "  End As environment_type,\n" +
                    "  'admin' As role_id,\n" +
                    "  'admin' As assignment_type\n" +
                    "From environments env\n" +
                    "Where env.environment_status = 'Active' and env.environment_name=(?)";
            Db.Rows rows = db("TDM").fetch(allEnvs, envName);
            List<String> columnNames = rows.getColumnNames();
            for (Db.Row row : rows) {
                ResultSet resultSet = row.resultSet();
                Map<String, Object> rowMap = new HashMap<>();
                for (String columnName : columnNames) {
                    rowMap.put(columnName, resultSet.getObject(columnName));
                }
                results.add(rowMap);
            }

        } else {
            String sql = "select env.environment_id, env.environment_name, " +
					"CASE when r.allow_read = true and r.allow_write = true THEN 'BOTH' when r.allow_write = true THEN 'TARGET' ELSE 'SOURCE' END environment_type, " +
					"r.role_id, 'user' as assignment_type " +
                    "from environments env, environment_roles r, environment_role_users u " +
                    "where env.environment_id = r.environment_id " +
                    "and lower(r.role_status) = 'active' " +
                    "and r.role_id = u.role_id " +
                    "and ( (u.user_id = (?) and u.user_type = 'ID' or (lower(u.username) = 'all' and u.environment_id not in " + 
					"(select u1.environment_id from environment_role_users u1 where u1.user_id= (?) and u.user_type = 'ID' ))) " +
					"or (u.user_id = ANY (string_to_array(?, ',')) and u.user_type = 'GROUP' or (lower(u.username) = 'all' and u.environment_id not in " + 
					"(select u1.environment_id from environment_role_users u1 where u1.user_id = ANY (string_to_array(?, ',')) and u.user_type = 'GROUP' ))) ) " +
                    "and env.environment_status = 'Active' and env.environment_name=(?)";
            Db.Rows rows = db("TDM").fetch(sql, userId, userId, fabricRoles, fabricRoles, envName);

            List<String> columnNames = rows.getColumnNames();
            for (Db.Row row : rows) {
                ResultSet resultSet = row.resultSet();
                Map<String, Object> rowMap = new HashMap<>();
                for (String columnName : columnNames) {
                    rowMap.put(columnName, resultSet.getObject(columnName));
                }
                results.add(rowMap);
            }

            String query1 = "select env.environment_id, env.environment_name, " +
					"CASE when env.allow_read = true and env.allow_write = true THEN 'BOTH' when env.allow_write = true THEN 'TARGET' ELSE 'SOURCE' END environment_type, " + 
					"'owner' as role_id, 'owner' as assignment_type " +
                    "from environments env, environment_owners o " +
                    "where env.environment_id = o.environment_id " +
                    "and ( (o.user_id = (?) and o.user_type ='ID') " + 
					"or (o.user_id = ANY (string_to_array(?, ',')) and o.user_type ='GROUP') ) " +
                    "and env.environment_status = 'Active' and env.environment_name=(?)";
            rows = db("TDM").fetch(query1, userId, fabricRoles, envName);
            columnNames = rows.getColumnNames();
            for (Db.Row row : rows) {
                ResultSet resultSet = row.resultSet();
                Map<String, Object> rowMap = new HashMap<>();
                for (String columnName : columnNames) {
                    rowMap.put(columnName, resultSet.getObject(columnName));
                }
                results.add(rowMap);
            }
        }
        return results;
    }

    static void fnSaveTaskOverrideParameters(Long taskId, Map<String, Object> overrideParameters, Long taskExecutionId) throws Exception {
        String sql = "INSERT INTO task_execution_override_attrs (task_id,override_parameters,task_execution_id) VALUES (?,?,?)";
        String params_str = new JSONObject(overrideParameters).toString();
        db("TDM").execute(sql, taskId, params_str, taskExecutionId);
    }

	public static Map<String, Object> fnValidateReservedEntities(String beID, String envID, ArrayList<String> entitiesList) throws Exception {
		Map<String, Object> result = new HashMap<>();
		List<Map<String, Object>> entityList = new ArrayList<>();
		String userName = sessionUser().name();
		Set<String> fabricRolesSet = sessionUser().roles();
		//Boolean adminOrOwner = fnIsAdminOrOwner(envID, userName);
		String message  = "";
		
		/*if (adminOrOwner) {
			//log.info("The user: " + userName + " is an admin Or owner");
			result.put("listOfEntities", entityList);
			result.put("message", message);
			return result;
		}*/
		
		//log.info("The user: " + userName + " is tester");
		String entities_list_for_qry = "";
		//ArrayList<String> releasedEntitiesList = entitiesList;
		
		for (int i = 0; i < entitiesList.size(); i++) {
            entities_list_for_qry += "'" + entitiesList.get(i).trim() + "',";
        }
        // remove last ,
        entities_list_for_qry = entities_list_for_qry.substring(0, entities_list_for_qry.length() - 1);

		//log.info("fnSaveTaskOverrideParameters - beID: " + beID + ", envID: " + envID);
		String query = "SELECT * FROM TDM_RESERVED_ENTITIES WHERE " +
				"be_id =? AND env_id =? AND entity_id in (" + entities_list_for_qry + ") And  reserve_owner != ? " +
				"AND CURRENT_TIMESTAMP >= start_datetime AND (end_datetime IS NULL OR CURRENT_TIMESTAMP < end_datetime)";
		
		//log.info("-------- query: " + query);
		try {
			Db.Rows reservedEntities = db("TDM").fetch(query, beID, envID, userName);
		
			for (Db.Row row : reservedEntities) {
				String entityID = "" + row.get("entity_id");
				String owner = "" + row.get("reserve_owner");
				//remove entity from released Entities as it is not reserved
				//releasedEntitiesList.remove(entityID);
			
				Map<String, Object> reservedRec = new HashMap<>();
				reservedRec.put("entity_id", entityID);
				reservedRec.put("reserve_owner", owner);
				reservedRec.put("start_datetime", row.get("start_datetime"));
				reservedRec.put("end_datetime", row.get("end_datetime"));
				message += ("".equals(message)) ? entityID : "," + entityID;
				
				entityList.add(reservedRec);
			}
			
			/*for (String releasedEntity : releasedEntitiesList) {
				Map<String, Object> releasedRec = new HashMap<>();
				releasedRec.put("entity_id", releasedEntity);
				releasedRec.put("reserve_ind", "false");
				releasedRec.put("reserve_owner", null);
				releasedRec.put("start_datetime", null);
				releasedRec.put("end_datetime", null);
				releasedRec.put("reserve_message", "");
				
				entityList.add(releasedRec);
			}*/
			
		} catch(Exception e) {
			 e.printStackTrace();
			throw new Exception("Validation of entity list had failed with error: " + e.getMessage());
		}
		
		if (!"".equals(message)) {
			message = "Entities reserved by other User: " + message;	
		}
		result.put("listOfEntities", entityList);
		result.put("message", message);
		return result;
	}
	
	public static void createTaskGlobals(Long taskId, String luName, String globalName, String globalValue) throws Exception {
		if (luName != null && !"".equals(luName) && !"ALL".equals(luName)) {
			globalName = luName + "." + globalName;
		}
		String sql = "INSERT INTO task_globals (task_id, global_name, global_value) VALUES (?, ?, ?)";
		db("TDM").execute(sql, taskId, globalName, globalValue);
	}

	public static Db.Rows fnGetTasks(String task_ids) throws Exception {
		String sql= "SELECT tasks.*,environments.*,business_entities.*,environment_owners.user_name as owner,environment_owners.user_type as owner_type,environment_role_users.username as tester,environment_role_users.user_type as tester_type,environment_role_users.role_id  as role_id_orig, tasks.sync_mode," +
			"( SELECT COUNT(*) FROM task_execution_list WHERE task_execution_list.task_id = tasks.task_id AND" +
			" ( UPPER(task_execution_list.execution_status)" +
			"  IN ('RUNNING','EXECUTING','STARTED','PENDING','PAUSED','STARTEXECUTIONREQUESTED'))) AS executioncount, " +
			" ( SELECT COUNT(*) FROM task_ref_tables WHERE task_ref_tables.task_id = tasks.task_id ) AS refcount,  " +
			" ( SELECT string_agg(process_name::text, ',') FROM TASKS_POST_EXE_PROCESS WHERE TASKS_POST_EXE_PROCESS.task_id = tasks.task_id ) AS processnames " +
			" FROM tasks LEFT JOIN environments" +
			" ON (tasks.environment_id = environments.environment_id) LEFT JOIN business_entities ON" +
			" (tasks.be_id = business_entities.be_id) LEFT JOIN environment_owners ON" +
			" (tasks.environment_id = environment_owners.environment_id) " +
			" LEFT JOIN environment_role_users ON" +
			" (tasks.environment_id = environment_role_users.environment_id)";
		
		if(task_ids!=null&&task_ids.length()>0) {
			sql+= " WHERE tasks.task_id in (" + task_ids + ")";
		}
	
		Db.Rows result = db("TDM").fetch(sql);
		return result;
	}
	
	//This function returns the details of the child records of the given entity. It is a recursive function that iterates till the leaves and go back, each child record will be added to the parent's children list.
	public static Map<String,Object> fnGetChildHierarchy(String i_luName, String i_targetEntityID) throws Exception {
		String entityStatus = "completed";
		LinkedHashMap<String,Object> o_childHirarchyData =  new LinkedHashMap<>();
		LinkedHashMap<String,Object> innerChild =  new LinkedHashMap<>();
		List<Object> childData = new ArrayList<>();
		
		//log.info("fnGetChildHierarchy inputs - Lu Name: " + i_luName + ", target ID: " + i_targetEntityID);
		
		String sqlGetEntityData = "select lu_name luName, target_entity_id targetId, entity_id sourceId, " +
			"execution_status entityStatus, parent_lu_name parentLuName, TARGET_PARENT_ID parentTargetId, root_entity_status luStatus from TDM.task_Execution_link_entities " +
			"where lu_name= ? and target_entity_id = ?";
		
		String sqlGetChildren = "select lu_name, target_entity_id from TDM.task_Execution_link_entities " +
			"where parent_lu_name= ? and target_parent_id = ?";
		
		Db.Rows childRecs = fabric().fetch(sqlGetChildren, i_luName, i_targetEntityID);
		
		ArrayList<String[]> childrenRecs = new ArrayList<>();
		
		//Get the list of direct children of the current (input) entity if such exists
		for (Db.Row childRec : childRecs) {
			String[] childInfo = {"" + childRec.get("lu_name"), "" + childRec.get("target_entity_id")};
			childrenRecs.add(childInfo);
		}
		
		if (childRecs != null) {
			childRecs.close();
		}
		//log.info("fnGetChildHierarchy - Number of child Recs: " + childrenRecs.size());
		//Loop over the direct children entities and call this function to get for each child it own children
		for (int i=0; i< childrenRecs.size(); i++) {
			
			String[] child = childrenRecs.get(i);
			String childLuName = child[0];
			String childTargetId = child[1];
			
			//log.info("fnGetChildHierarchy - Child Rec: Lu Name: " + childLuName + ", target ID: " + childTargetId);
			if (childLuName != null && !childLuName.isEmpty()) {
				//a recursive call to this function until we get to a leaf entity
				innerChild = (LinkedHashMap<String, Object>)fnGetChildHierarchy(childLuName, childTargetId);
						
				childData.add(innerChild);
			}
		}
		// This point is reached, either the current entity has no children (leaf) or all it direct children (and their own children) were
		// already processed.
		Db.Row entityRec = (fabric().fetch(sqlGetEntityData, i_luName, i_targetEntityID)).firstRow();
		
		o_childHirarchyData.put("luName", "" + entityRec.get("luName"));
		o_childHirarchyData.put("targetId", "" + entityRec.get("targetId"));
		
		//Get instance ID from entity id
		//log.info("fnGetChildHierarchy - entity_id: " + entityRec.get("sourceId"));
		Object[] splitId = fnSplitUID("" + entityRec.get("sourceId"));
		String instanceId = "" + splitId[0];
		o_childHirarchyData.put("sourceId", "" + instanceId);
		
		o_childHirarchyData.put("entityStatus", "" + entityRec.get("entityStatus"));
		o_childHirarchyData.put("parentLuName", "" + entityRec.get("parentLuName"));
		o_childHirarchyData.put("parentTargetId", "" + entityRec.get("parentTargetId"));
		
		//log.info("fnGetChildHierarchy - Adding children Data, size: " + childData.size());
		if (childData.size() > 0){
			o_childHirarchyData.put("children", childData);
		}
		
		//log.info("fnGetChildHierarchy - LU status: " + entityStatus);
		o_childHirarchyData.put("luStatus",  "" + entityRec.get("entityStatus"));
		
		return o_childHirarchyData;
	}


	//This function returns the details of the ancestors records of the given entity, if such exists. It is a recursive function, that travels until  it reaches the root and goes back. Each Ancestor will get the data of its children and will added it to its record as children data, The final data prepared for the root entity, will be eventually returned from this function
	public static Map<String,Object> fnGetParentHierarchy(String i_luName, String i_targetEntityID, Object i_children) throws Exception {
		String entityStatus = "completed";
		LinkedHashMap<String,Object> currentEntity =  new LinkedHashMap<>();
		LinkedHashMap<String,Object> upperParent =  new LinkedHashMap<>();
		
		//LinkedHashMap<String,Object> childrenRecs = new LinkedHashMap<>();
		List<Object> childrenRecs = new ArrayList<>();
		
		if (i_children != null){
			childrenRecs.add(i_children);
		}
		
		//log.info("fnGetParentHierarchy inputs - Lu Name: " + i_luName + ", target ID: " + i_targetEntityID);
		
		String sqlGetEntityData = "select lu_name luName, target_entity_id targetId, entity_id sourceId, " +
			"execution_status entityStatus, parent_lu_name parentLuName, TARGET_PARENT_ID parentTargetId, root_entity_status luStatus " +
			"from TDM.task_Execution_link_entities where lu_name= ? and target_entity_id = ?";
		
		String sqlGetParent = "select parent_lu_name, target_parent_id from TDM.task_Execution_link_entities " +
			"where lu_name= ? and target_entity_id = ? limit 1";
		
		//Get the data of the current (input) entity
		Db.Row entityRec = (fabric().fetch(sqlGetEntityData, i_luName, i_targetEntityID)).firstRow();
		
		currentEntity.put("luName", "" + entityRec.get("luName"));
		currentEntity.put("targetId", "" + entityRec.get("targetId"));
		
		//Get instance ID from entity id
		//log.info("fnGetParentHierarchy - entity_id: " + entityRec.get(""));
		Object[] splitId = fnSplitUID("" + entityRec.get("sourceId"));
		String instanceId = "" + splitId[0];
		currentEntity.put("sourceId", "" + instanceId);
		
		currentEntity.put("entityStatus", "" + entityRec.get("entityStatus"));
		currentEntity.put("parentLuName", "" + entityRec.get("parentLuName"));
		currentEntity.put("parentTargetId", "" + entityRec.get("parentTargetId"));
			
		currentEntity.put("children",childrenRecs);
		
		//log.info("fnGetParentHierarchy - LU status: " + entityStatus);
		currentEntity.put("luStatus", "" + entityRec.get("luStatus"));
		
		String parentLuName = "";
		String parentTargetId = "";
		
		//Get the parent record, as each entity can have only one parent, only one row wil be returned
		Db.Row parentRec = fabric().fetch(sqlGetParent, i_luName, i_targetEntityID).firstRow();
		if (!parentRec.isEmpty()) {
				parentLuName = "" + parentRec.get("parent_lu_name");
				parentTargetId = "" + parentRec.get("target_parent_id");
		}
		//log.info("fnGetParentHierarchy - parent Rec: Lu Name: " + parentLuName + ", Parent target ID: " + parentTargetId);
		
		if ( parentLuName != null && !"".equals(parentLuName) ) {
			
			//log.info("fnGetParentHierarchy - parent Rec: Lu Name: " + parentLuName + ", Parent target ID: " + parentTargetId);
			//a recursive call to this function until we get to a leaf entity
			upperParent = (LinkedHashMap<String, Object>)fnGetParentHierarchy(parentLuName, parentTargetId, currentEntity);
			//log.info("fnGetParentHierarchy - parent Status: " + upperParent.get("luStatus"));
			
		}
		
		//No parent, therefore a root, and return the root data, as it already includes the data of all the children
		if (upperParent == null || upperParent.isEmpty()){
			return currentEntity;
		}
		
		//Return the parent, so eventually return the root record with the whole hierarchy data
		return upperParent;
	}
}