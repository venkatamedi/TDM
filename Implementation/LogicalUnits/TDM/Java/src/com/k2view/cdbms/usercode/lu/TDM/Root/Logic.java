/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.TDM.Root;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.usercode.lu.TDM.*;

import com.k2view.cdbms.usercode.lu.TDM.TDM.*;
//import org.omg.CORBA.Object;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.TDM.Globals.*;
import static com.k2view.cdbms.usercode.common.TDM.SharedLogic.*;
import com.k2view.cdbms.shared.Globals;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.fabric.fabricdb.datachange.TableDataChange;
import com.k2view.fabric.events.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam", "unchecked"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "TASK_EXECUTION_ID", type = void.class, desc = "")
	public static void funRootExecID(String TASK_EXECUTION_ID) throws Exception {
		// TDM 7.4 0 28-Feb-22 - Set TTL for TDM LU Instance
		if (!TDM_LU_RETENTION_PERIOD_VALUE.isEmpty() && Long.valueOf(TDM_LU_RETENTION_PERIOD_VALUE) > 0) {
			Integer ttl = getRetention(TDM_LU_RETENTION_PERIOD_TYPE, Float.valueOf(TDM_LU_RETENTION_PERIOD_VALUE));
			fabric().execute("set INSTANCE_TTL = " + ttl);
		} else {
			fabric().execute("set INSTANCE_TTL = " + 0);
		}
		
		// TALI- TDM 5.5- 25-Sep-19- add a validation to verify that the task is completed
		
		String sql = "SELECT count(*) FROM task_execution_list out " + 
		"where task_execution_id = ? and exists (select 1 from task_execution_list tbl " +
		"where tbl.task_execution_id = out.task_execution_id " +
		"and tbl.execution_status not in ('stopped','completed','failed','killed'))";
		
		String cnt= db("TDM").fetch(sql, TASK_EXECUTION_ID).firstValue().toString();
		
		if(cnt != null)
		{
			if(Integer.parseInt(cnt) > 0 ) // if the task is not completed yet- reject the entity
				{
					rejectInstance("Task Execution ID " + TASK_EXECUTION_ID + " is not completed yet " );
				}
		}
		
		yield(new Object[]{TASK_EXECUTION_ID});
	}

	@desc("Tali- 4-Dec-18- add target_entity_id as input to support clone\r\n" +
			"Tali- 5-Dec-18- fix the function for delete only task to make the search on the target relation table (tdm_lu_type_rel_tar_eid)\r\n" +
			"Tali- 19-June-19- add id_type as input. Populate the table only if id_type is not REFERENCE")
	@type(RootFunction)
	@out(name = "LU_NAME", type = String.class, desc = "")
	@out(name = "PARENT_LU_NAME", type = String.class, desc = "")
	@out(name = "ENTITY_ID", type = String.class, desc = "")
	@out(name = "TARGET_ENTITY_ID", type = String.class, desc = "")
	@out(name = "PARENT_ENTITY_ID", type = String.class, desc = "")
	@out(name = "BE_ROOT_ENTITY_ID", type = String.class, desc = "")
	@out(name = "TARGET_ROOT_ENTITY_ID", type = String.class, desc = "")
	@out(name = "EXECUTION_STATUS", type = String.class, desc = "")
	@out(name = "TARGET_PARENT_ID", type = String.class, desc = "")
	@out(name = "ROOT_LU_NAME", type = String.class, desc = "")
	@out(name = "VERSION_NAME", type = String.class, desc = "")
	@out(name = "VERSION_DATETIME", type = String.class, desc = "")
	public static void fnPopTaskExecutionLinkEntities(String LU_NAME, String ENTITY_ID, String EXECUTION_STATUS, String target_entity_id, String id_type) throws Exception {
		if (1==2) {
		yield(new Object[]{null});
		}
	}


	@out(name = "countParent", type = Integer.class, desc = "")
	public static Integer fnGetCount(String sql, String parentLuName, String beID) throws Exception {
		//Long countParent = (Long) DBSelectValue("TDM", sql, new Object[]{parentLuName, beID});
		Long countParent = (Long) db("TDM").fetch(sql, parentLuName, beID).firstValue();
		Integer count = countParent.intValue();
		return count;
	}


	@type(RootFunction)
	@out(name = "source_env", type = String.class, desc = "")
	@out(name = "lu_type_1", type = String.class, desc = "")
	@out(name = "lu_type_2", type = String.class, desc = "")
	@out(name = "lu_type1_eid", type = String.class, desc = "")
	@out(name = "lu_type2_eid", type = String.class, desc = "")
	@out(name = "creation_date", type = String.class, desc = "")
	@out(name = "version_name", type = String.class, desc = "")
	@out(name = "version_datetime", type = String.class, desc = "")
	public static void fnPop_tdm_lu_type_relation_eid(String LU_NAME, String LU_EID, String SOURCE_ENV_NAME) throws Exception {
		if (1==2) {
			yield (null);
		}
	}


	@desc("Get the list of LUs, related to the task. The list is taaken from task_execution_entities")
	@type(RootFunction)
	@out(name = "TASK_EXECUTION_ID", type = String.class, desc = "")
	@out(name = "LU_LIST", type = String.class, desc = "")
	public static void fnRootPopTaskLuList(String TASK_EXECUTION_ID) throws Exception {
		// Tali- 2-Dec-12- get the list of LUs, related to the task execution
		String taskLuNamesSql = "SELECT DISTINCT LU_NAME FROM TASK_EXECUTION_ENTITIES";
		Db.Rows rows = ludb().fetch(taskLuNamesSql);
		StringBuilder luList = new StringBuilder();
		String prefix = "";
		for (Db.Row row : rows) {
		
			luList.append(prefix + "'" + row.cells()[0] + "'");
			prefix = ",";
		}
		
		String luListStr = "( " + luList.toString() + " )";
		
		//log.info("lu list: " + luListStr);
		
		if (rows != null) {
			rows.close();
		}
		yield(new Object[]{TASK_EXECUTION_ID, luListStr});
	}


	@type(RootFunction)
	@out(name = "target_env", type = String.class, desc = "")
	@out(name = "lu_type_1", type = String.class, desc = "")
	@out(name = "lu_type_2", type = String.class, desc = "")
	@out(name = "lu_type1_eid", type = String.class, desc = "")
	@out(name = "lu_type2_eid", type = String.class, desc = "")
	@out(name = "creation_date", type = String.class, desc = "")
	public static void fnRootPopTdmLuTypeRelTarEidForDeleteOnly(Integer load_entity, Integer delete_before_load) throws Exception {
		// Get the records from the original insert task for a delete only task
		if (load_entity == 0 && delete_before_load == 1)
		{
			// Get one of the tagret entity IDs of the current task from task_execution_entities
			//String luName = "";
			//String tagetEntityID = "";
			//String targetEnv = "";
			String taskExecutionID = getInstanceID();
			String versionName = "";
			String versionDateTime = "";
		
		// Get selected version name and datetime from tasks
			String sqlTasks = "SELECT selected_version_task_name, selected_version_datetime FROM TASKS";
			Db.Row verData = fabric().fetch(sqlTasks).firstRow();
			//
			
			//log.info("fnRootPopTdmLuTypeRelTarEidForDeleteOnly - before - versionName: " + versionName + ", versionDateTime: " + versionDateTime);
			if(!verData.isEmpty()) {
				if (!"null".equals(verData.get("selected_version_task_name"))) {
					//log.info("fnRootPopTdmLuTypeRelTarEidForDeleteOnly - verData: <" + verData.get("selected_version_task_name") + ">");
					versionName = "" + verData.get("selected_version_task_name");
					versionDateTime = "" + verData.get("selected_version_datetime");
				}
			}
			
			if ("null".equals(versionName)) {
				versionName = "";
				versionDateTime = "";
			}
			//log.info("fnRootPopTdmLuTypeRelTarEidForDeleteOnly - versionName: " + versionName + ", versionDateTime: " + versionDateTime);
			
			Db.Rows rows2 = null;
			
			if ("".equals(versionName)) {	
				String sql2 = "SELECT rel.* " +
						"FROM tdm_lu_type_rel_tar_eid rel, task_Execution_entities parent,  task_Execution_entities child, environments e " +
						"WHERE parent.task_Execution_id = ? AND parent.task_Execution_id = child.task_Execution_id " +
						"AND CAST(e.environment_id as text) = parent.env_id AND rel.target_env = e.environment_name " +
						"AND rel.lu_Type_1 = parent.lu_name " +
						"AND rel.lu_Type1_eid= parent.target_entity_id AND rel.lu_type_2 = child.lu_name " +
						"AND rel.lu_type2_eid = child.target_entity_id AND parent.version_name = '' ";
				rows2 = db("TDM").fetch(sql2, taskExecutionID);
			} else {
				String sql2 = "SELECT rel.* " +
						"FROM tdm_lu_type_rel_tar_eid rel, task_Execution_entities parent,  task_Execution_entities child, environments e " +
						"WHERE parent.task_Execution_id = ? AND parent.task_Execution_id = child.task_Execution_id " +
						"AND CAST(e.environment_id as text) = parent.env_id AND rel.target_env = e.environment_name " +
						"AND rel.lu_Type_1 = parent.lu_name " +
						"AND rel.lu_Type1_eid= parent.target_entity_id AND rel.lu_type_2 = child.lu_name " +
						"AND rel.lu_type2_eid = child.target_entity_id AND parent.version_name=? " +
						"AND to_char(parent.version_datetime, 'YYYYMMDDHH24MISS')=?";
				rows2 = db("TDM").fetch(sql2, taskExecutionID, versionName, versionDateTime);
			}
			for (Db.Row row : rows2) {
				yield(row.cells());
			}
			if (rows2 != null) {
				rows2.close();
			}
				
		}
	}


	@type(RootFunction)
	@out(name = "task_execution_id", type = String.class, desc = "")
	@out(name = "task_id", type = String.class, desc = "")
	@out(name = "lu_id", type = String.class, desc = "")
	@out(name = "lu_name", type = String.class, desc = "")
	@out(name = "parent_lu_id", type = String.class, desc = "")
	@out(name = "parent_lu_name", type = String.class, desc = "")
	@out(name = "version_ind", type = String.class, desc = "")
	@out(name = "version_name", type = String.class, desc = "")
	@out(name = "version_datetime", type = String.class, desc = "")
	@out(name = "version_expiration_date", type = String.class, desc = "")
	@out(name = "execution_status", type = String.class, desc = "")
	@out(name = "start_execution_time", type = String.class, desc = "")
	@out(name = "end_execution_time", type = String.class, desc = "")
	@out(name = "num_of_processed_entities", type = String.class, desc = "")
	@out(name = "num_of_copied_entities", type = String.class, desc = "")
	@out(name = "num_of_failed_entities", type = String.class, desc = "")
	@out(name = "data_center_name", type = String.class, desc = "")
	@out(name = "num_of_processed_ref_tables", type = String.class, desc = "")
	@out(name = "num_of_copied_ref_tables", type = String.class, desc = "")
	@out(name = "num_of_failed_ref_tables", type = String.class, desc = "")
	@out(name = "fabric_execution_id", type = String.class, desc = "")
	@out(name = "task_executed_by", type = String.class, desc = "")
	@out(name = "process_id", type = String.class, desc = "")
	@out(name = "process_name", type = String.class, desc = "")
	public static void fnPopTaskExecutionList(String task_execution_id) throws Exception {
		String lu_id = "";
		String parent_lu_id = "";
		String task_id = "";
		
		String lu_name = "";
		String parent_lu_name = "";
		String version_ind = "";
		String version_name = "";
		String data_center_name = "";
		
		String version_datetime = "";
		String version_expiration_date = "";
		String execution_status = "";
		String start_execution_time = "";
		String end_execution_time = "";
		String num_of_processed_entities = "";
		String num_of_copied_entities = "";
		String num_of_failed_entities = "";
		String num_of_processed_ref_tables = "";
		String num_of_copied_ref_tables = "";
		String num_of_failed_ref_tables = "";
		String fabric_execution_id = "";
		String task_executed_by = "";
		String process_id = "";
		String process_name = "";
		
		Db ciTDM = db("TDM");
		Db.Rows tdmTaslExecList = ciTDM.fetch("SELECT * FROM task_execution_list where task_execution_id = ?", task_execution_id);
		
		for (Db.Row tdmTaskExecRec: tdmTaslExecList) {
			lu_id = "" + tdmTaskExecRec.get("lu_id");
			process_id = "" + tdmTaskExecRec.get("process_id");
			parent_lu_id = "" + tdmTaskExecRec.get("parent_lu_id");
			task_id = "" + tdmTaskExecRec.get("task_id");
			data_center_name = "" + tdmTaskExecRec.get("data_center_name");
		
			
			// LU Name and Parent LU Name will be populated only if the LU_ID is not zero, otherwise the Process Name will be populated
			if (!"0".equals(lu_id)) {
				Db.Row luNames = ciTDM.fetch("SELECT lu_name, lu_parent_name from product_logical_units where lu_id = ?", Integer.valueOf(lu_id)).firstRow();
				lu_name = "" + luNames.get("lu_name");
				parent_lu_name = "" + luNames.get("lu_parent_name");
			} else {
				 process_name = "" + ciTDM.fetch("SELECT process_name from tasks_post_exe_process where task_id = ? and process_id = ?", 
					Integer.valueOf(task_id), Integer.valueOf(process_id)).firstValue();
			}
			
			Db.Row taskInfo = ciTDM.fetch("SELECT version_ind, task_title from tasks where task_id = ?", task_id).firstRow();
			version_ind = "" + taskInfo.get("version_ind");
			if ("true".equalsIgnoreCase(version_ind)) {
				version_name = "" + taskInfo.get("task_title");
			}
			
			version_datetime = "" + tdmTaskExecRec.get("version_datetime");
			version_expiration_date = "" + tdmTaskExecRec.get("version_expiration_date");
			execution_status = "" + tdmTaskExecRec.get("execution_status");
			start_execution_time = "" + tdmTaskExecRec.get("start_execution_time");
			end_execution_time = "" + tdmTaskExecRec.get("end_execution_time");
			num_of_processed_entities = "" + tdmTaskExecRec.get("num_of_processed_entities");
			num_of_copied_entities = "" + tdmTaskExecRec.get("num_of_copied_entities");
			execution_status = "" + tdmTaskExecRec.get("execution_status");
			start_execution_time = "" + tdmTaskExecRec.get("start_execution_time");
			end_execution_time = "" + tdmTaskExecRec.get("end_execution_time");
			num_of_processed_entities = "" + tdmTaskExecRec.get("num_of_processed_entities");
			num_of_copied_entities = "" + tdmTaskExecRec.get("num_of_copied_entities");
			num_of_failed_entities = "" + tdmTaskExecRec.get("num_of_failed_entities");
			num_of_processed_ref_tables = "" + tdmTaskExecRec.get("num_of_processed_ref_tables");
			num_of_copied_ref_tables = "" + tdmTaskExecRec.get("num_of_copied_ref_tables");
			num_of_failed_ref_tables = "" + tdmTaskExecRec.get("num_of_failed_ref_tables");
			fabric_execution_id = "" + tdmTaskExecRec.get("fabric_execution_id");
			String[] userData = tdmTaskExecRec.get("task_executed_by").toString().split("##");
			
			task_executed_by = userData[0];
			
			yield(new Object[]{task_execution_id,task_id,lu_id,lu_name, parent_lu_id, parent_lu_name, version_ind,version_name,version_datetime,
				version_expiration_date,execution_status,start_execution_time,end_execution_time,num_of_processed_entities,
				num_of_copied_entities,num_of_failed_entities,data_center_name,num_of_processed_ref_tables,num_of_copied_ref_tables,
				num_of_failed_ref_tables,fabric_execution_id, task_executed_by,process_id,process_name});
		
		}
		
		if (tdmTaslExecList != null) {
			tdmTaslExecList.close();
		}
	}






	@desc("This function populates the TASK_EXE_ERROR_DETAILED")
	@type(RootFunction)
	@out(name = "task_execution_id", type = String.class, desc = "")
	@out(name = "lu_name", type = String.class, desc = "")
	@out(name = "entity_id", type = String.class, desc = "")
	@out(name = "iid", type = String.class, desc = "")
	@out(name = "target_entity_id", type = String.class, desc = "")
	@out(name = "error_category", type = String.class, desc = "")
	@out(name = "error_code", type = String.class, desc = "")
	@out(name = "error_message", type = String.class, desc = "")
	@out(name = "creation_date", type = String.class, desc = "")
	@out(name = "flow_name", type = String.class, desc = "")
	@out(name = "stage_name", type = String.class, desc = "")
	@out(name = "actor_name", type = String.class, desc = "")
	@out(name = "actor_parameters", type = String.class, desc = "")
	public static void fnPopTaskExeErrDetailed(String task_execution_id) throws Exception {
		String taskType = "" + fabric().fetch("select task_type from tasks where task_execution_id = ?", task_execution_id).firstValue();
		
		Db.Rows errTableData = db("TDM").fetch("select distinct task_execution_id,lu_name,entity_id,iid,target_entity_id, error_category, error_code, " +
			"error_message, creation_date, flow_name, stage_name, actor_name, actor_parameters " +
			"from TASK_EXE_ERROR_DETAILED where TASK_EXECUTION_ID = ?", task_execution_id);
		
		if ("extract".equalsIgnoreCase(taskType)) {
			for (Db.Row errRec : errTableData) {
				yield(errRec.cells());	
			}
		
		// In case of Load tasks and the instance is not a reference table, the target_entity_id and iid should be set correctly,
		// as Load tasks initiate these 2 fields with the same value as entity_id
		} else {// Load Task
			for (Db.Row errRec : errTableData) {
				String luName = "" + errRec.get("lu_name");
				String entityId = "" + errRec.get("entity_id");
				String iid = "" + errRec.get("iid");
				String targetEntId = "" + errRec.get("target_entity_id");
				String errorCat = "" + errRec.get("error_category");
				String errorCode = "" + errRec.get("error_code");
				String errorMsg = "" + errRec.get("error_message");
				String createDate = ""  + errRec.get("creation_date");
				String flowName = ""  + errRec.get("flow_name");
				String stageName = ""  + errRec.get("stage_name");
				String actorName = ""  + errRec.get("actor_name");
				String actorParams = ""  + errRec.get("actor_parameters");
				
				Db.Row entityRec = fabric().fetch("select ID_TYPE, TARGET_ENTITY_ID, IID from task_execution_entities where " + 
					"TASK_EXECUTION_ID = ? AND LU_NAME = ? AND ENTITY_ID = ?", task_execution_id, luName, entityId).firstRow();
				
				String idType = "" + entityRec.get("ID_TYPE");
		
				//log.info("fnPopTaskExeErrDetailed - idType: " + idType + ", iid: " + iid + ", targetEntId: " + targetEntId);
				
				//log.info("fnPopTaskExeErrDetailed - Original Value: IID: " + errRec.get("iid"));
				//log.info("fnPopTaskExeErrDetailed - Original Value: target_entity_id: " + errRec.get("target_entity_id"));
				if ("entity".equalsIgnoreCase(idType)) {
					iid = "" + entityRec.get("IID");
					targetEntId = "" + entityRec.get("TARGET_ENTITY_ID");
				}
				
				yield(new Object[]{task_execution_id,luName,entityId,iid,targetEntId,errorCat,errorCode,errorMsg,createDate,flowName,stageName,actorName,actorParams});
			}
		
		}
		
		if (errTableData != null) {
			errTableData.close();
		}
	}

}
