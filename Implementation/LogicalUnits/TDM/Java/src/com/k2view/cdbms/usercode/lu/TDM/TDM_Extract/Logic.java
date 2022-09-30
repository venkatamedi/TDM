/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.TDM.TDM_Extract;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.Globals;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.TDM.*;
import com.k2view.fabric.events.*;
import com.k2view.fabric.fabricdb.datachange.TableDataChange;
import com.k2view.fabric.common.Util;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.TDM.Globals.*;
import static com.k2view.cdbms.usercode.common.TDM.SharedLogic.getRetention;
import static com.k2view.cdbms.usercode.common.TDM.SharedLogic.fnGetIIdSeparatorsFromTDM;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {

		@out(name = "result", type = Map.class, desc = "")
	public static Map<String,String> fnMExtractEntities(String luName, String dcName, String sourceEnvName, String taskName, String versionInd, String entitiesList, String retentionPeriodType, Float retentionPeriodValue, String taskExecutionId, String parentLuName, String versionDateTime, String syncMode, String selectionMethod, Long luId) throws Exception {
		if (!"ON".equalsIgnoreCase(syncMode)) {
			fabric().execute("SET SYNC " + syncMode);
		}
	
		String batchCommand = "";
		String entityList = "";
		String migrationInfo = "";
		Long unixTime = System.currentTimeMillis();
		Long unixTime_plus_retention;
		String versionName = "";
		
		//setGlobals(globals);
		String iidSeparator = "" + db("TDM").fetch("Select param_value from tdm_general_parameters where param_name = 'iid_separator'").firstValue();
		String separator = "";
		if (!Util.isEmpty(iidSeparator) && !"null".equals(iidSeparator)) {
			separator = iidSeparator;
		} else {
			separator = "_";
		}
		// TALI- set version_exp_date to null. We cannot set an empty string for timestamp type of PG
		String version_exp_date = null;
		Db.Rows rs_mig_id = null;
		
		// Tali- return the results in a map
		Map<String, String> migInfo = new LinkedHashMap<>();
		
		// Tali- init timestamp by null
		String timeStamp = null;
		
		// TDM 5.1- support TTL also for regular extract tasks (versioning is false)
		// Check if retention parameters are populated instead of checking the versioning parameter
		
		//Calculate retention date + set TTL
		if (retentionPeriodType != null && !retentionPeriodType.isEmpty() && retentionPeriodValue != null && retentionPeriodValue > 0) {
			// Tali- set the datetime only when versionInd is true (backup tasks)
			// TDM 6.0, in case of versionInd = true, the version_datetime will be received as input
			Integer retention_in_seconds = getRetention(retentionPeriodType, retentionPeriodValue);
			if (versionDateTime != null && !versionDateTime.isEmpty()) {
				timeStamp = versionDateTime;
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
				Date timeStampDate = sdf.parse(timeStamp);
				long millis = timeStampDate.getTime();
				unixTime_plus_retention = (millis / 1000L + retention_in_seconds) * 1000;
				version_exp_date = new SimpleDateFormat("yyyyMMddHHmmss").format(unixTime_plus_retention);
			} else {
				timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(unixTime);
				unixTime_plus_retention = (unixTime / 1000L + retention_in_seconds) * 1000;
				version_exp_date = new SimpleDateFormat("yyyyMMddHHmmss").format(unixTime_plus_retention);
			}
		
			//Set TTL
			ludb().execute("SET INSTANCE_TTL = " + retention_in_seconds);
		
		}
		// end of if input retention parmeters are populated
		// TDM 7.3 - Set version globals in case of VERSION_IND is true
		if (versionInd.equals("true")) {
			versionName = taskName;
			versionDateTime = timeStamp;
			
			fabric().execute("set TDM_VERSION_NAME = '" + versionName + "'");
			fabric().execute("set TDM_VERSION_DATETIME = " + versionDateTime);
		}
		
		//Execute Migrate
		
		// TDM 5.1- add a support of configurable separator for IID
		
		Object[] iidSeparators = fnGetIIdSeparatorsFromTDM();
		String openSeparator = iidSeparators[0].toString();
		String closeSeparator = iidSeparators[1].toString();
		
		if (entitiesList != null && !entitiesList.isEmpty() && !entitiesList.equals("")) { //Extract task is for listed entities
			entityList = entitiesList;
			if ("L".equals(selectionMethod)) {
				if (dcName != null && !dcName.isEmpty()) {
					batchCommand = "batch " + luName + ".(?) FABRIC_COMMAND=\"sync_instance " + luName + ".?\" WITH AFFINITY='" + dcName + "' ASYNC=true";
				} else { // input DC is empty or null
					batchCommand = "batch " + luName + ".(?) FABRIC_COMMAND=\"sync_instance " + luName + ".?\" WITH ASYNC=true";
				}
			} else { // in case of Custom Logic
				if (dcName != null && !dcName.isEmpty()) {
					batchCommand = "batch " + luName + " from DB_CASSANDRA using (?) FABRIC_COMMAND=\"sync_instance " + luName + ".?\" WITH AFFINITY='" + dcName + "' ASYNC=true";
				} else { // input DC is empty or null
					batchCommand = "batch " + luName + " from DB_CASSANDRA using (?) FABRIC_COMMAND=\"sync_instance " + luName + ".?\" WITH ASYNC=true";
				}
			}
		} else { //Extract task is for all the LU ( according to provided query in translation )
			// TDM 5.1 - add an additional input to the translation- source env
		
			// TDM 6.0 - if parentLuName is populated, then this is a child LU,
			// and the list of entities for migration will be created based on the Parent LU
			if (parentLuName != null && !parentLuName.equals("")) {
				//Get the timestamp from the parent record in task_execution_list
				String sqlGetVersionDateTime = "select to_char(version_datetime, 'yyyyMMddHH24miss') from task_execution_list tel, tasks_logical_units tlu " +
						"where tel.task_execution_id = ? and tel.lu_id = tlu.lu_id and tlu.lu_name = ? limit 1";
				
				//log.info("fnMExtractEntities - taskExecutionId: " + taskExecutionId + ", parentLuName: " + parentLuName);
				timeStamp = "" + db("TDM").fetch(sqlGetVersionDateTime, taskExecutionId, parentLuName).firstValue();
		
				String entityIdSelectChildID = "rel.source_env||''" + separator + "''||rel.lu_type2_eid";
		
				if (!openSeparator.equals("")) {
					entityIdSelectChildID = "rel.source_env||''" + separator + "''||''" + openSeparator + "''||rel.lu_type2_eid";
				}
		
				if (!closeSeparator.equals("")) {
					entityIdSelectChildID += "||''" + closeSeparator + "''";
				}
				//log.info("fnMExtractEntities - versionInd: " + versionInd);
				if (versionInd.equals("true")) {
					
					//log.info("fnMExtractEntities - taskName: " + taskName + ", timeStamp: " + timeStamp);
					entityIdSelectChildID += "||''" + separator + taskName + separator + "''||''" + timeStamp + "''";
				}
		
				
				String selSql = "";
		
				if (versionInd.equals("true")) {
		
					selSql = "SELECT ''''||" + entityIdSelectChildID + "||'''' child_entity_id FROM task_execution_entities t, " +
							"tdm_lu_type_relation_eid rel where t.task_execution_id = ''" + taskExecutionId +
							"'' and t.execution_status = ''completed'' and t.lu_name = ''" + parentLuName +
							"'' and t.lu_name = rel.lu_type_1 and rel.lu_type_2 = ''" + luName +
							"'' and rel.version_name = ''" + versionName +
							"'' and to_char(rel.version_datetime, ''yyyyMMddHH24miss'') = ''" + versionDateTime +
							"'' and t.source_env = rel.source_env and t.iid = rel.lu_type1_eid";
				} else {
					selSql = "SELECT ''''||" + entityIdSelectChildID + "||'''' child_entity_id FROM task_execution_entities t, " +
							"tdm_lu_type_relation_eid rel where t.task_execution_id = ''" + taskExecutionId +
							"'' and t.execution_status = ''completed'' and t.lu_name = ''" + parentLuName +
							"'' and t.lu_name = rel.lu_type_1 and rel.lu_type_2 = ''" + luName +
							"'' and rel.version_name = '''' and t.source_env = rel.source_env and t.iid = rel.lu_type1_eid";
				}
				if (dcName != null && !dcName.isEmpty()) {
					batchCommand = "batch " + luName + " from TDM using (?) FABRIC_COMMAND=\"sync_instance " + luName + ".?\" WITH AFFINITY='" + dcName + "' ASYNC=true";
					entityList = selSql;
				} else // input DC is empty or null
				{
					batchCommand = "batch " + luName + " from TDM using (?) FABRIC_COMMAND=\"sync_instance " + luName + ".?\" WITH ASYNC=true";
					entityList = selSql;
				}
		
			} else {
		
				Map <String, String> batchStrings = getCommandForExtractAll(luName, taskExecutionId, sourceEnvName, versionInd, 
														separator, openSeparator, closeSeparator, taskName, timeStamp, dcName, luId);
				batchCommand = batchStrings.get("batchCommand");
				entityList = batchStrings.get("usingClause");
			}
		}
		
		//Set Fabric's active environment to sourceEnvName + Execute batch + get batch_id
		if (sourceEnvName != null && !sourceEnvName.isEmpty()) {
			ludb().execute("set environment='" + sourceEnvName + "'");
		}
		rs_mig_id = ludb().fetch(batchCommand, entityList);
		migrationInfo = "" + rs_mig_id.firstValue();
		
		rs_mig_id.close();
		// Tali- populate the migInfo map
		migInfo.put("fabric_execution_id", migrationInfo);
		
		// TDM 5.1- set the version_datetime and version_expiration_date only of the input version ind is true. This check is required since a retention period can be also set for extract without versioning tasks
		if (versionInd.equals("true")) {
			migInfo.put("version_datetime", timeStamp);
			migInfo.put("version_expiration_date", version_exp_date);
		} else {
			migInfo.put("version_datetime", null);
			migInfo.put("version_expiration_date", null);
		}
		
		return migInfo;
	}
	
	private static Map<String, String> getCommandForExtractAll(
		String luName, String taskExecutionId, String sourceEnvName, String versionInd, String separator, String openSeparator, 
		String closeSeparator, String taskName, String timeStamp, String dcName, Long luId) throws Exception {

		String modified_sql = "";		
		String batchCommand = "";
		Object[] trnMigrateList_input = {luName, sourceEnvName};

		Map<String, String> trnMigrateList_values = getTranslationValues("trnMigrateList", trnMigrateList_input);

		// TDM 5.1- If no translation record was found for the combination of lu name + source env- get the translation with null value of source env as input
		if (trnMigrateList_values.get("interface_name") == null) {
			Object[] trnMigrateList_input2 = {luName, ""};
			trnMigrateList_values = getTranslationValues("trnMigrateList", trnMigrateList_input2);
		}
		if ((trnMigrateList_values.get("interface_name") == null || trnMigrateList_values.get("ig_sql") == null) && trnMigrateList_values.get("external_table_flow") == null) {
			throw new RuntimeException("No entry found for LU_NAME: " + luName + " in translation trnMigrateList");
		}

		String interface_name = (trnMigrateList_values.get("interface_name") == null) ? "" : "" + trnMigrateList_values.get("interface_name");
		String sql = (trnMigrateList_values.get("ig_sql") == null) ? "" : "" + trnMigrateList_values.get("ig_sql").replaceAll("\n", " ");
		String externalTableFlow = (trnMigrateList_values.get("external_table_flow") == null) ? "" : "" + trnMigrateList_values.get("external_table_flow");
		
		if (externalTableFlow == null || externalTableFlow.isEmpty() || "null".equalsIgnoreCase(externalTableFlow)) {
			String splitSQL[] = StringUtils.split(sql.toLowerCase());
			String qry_entity_col = "";
			for (int i = 0; i < splitSQL.length; i++) {
				if (splitSQL[i].equals("from")) {
					qry_entity_col = splitSQL[i - 1].replaceAll("\\s+", "");
					break;
				}
			}
	
			// get original SQL statement "select" including the next SQL command like "distinct"
			String select = StringUtils.substringBefore(sql.toLowerCase(), qry_entity_col);
			String sql_part2 = sql.substring(sql.toLowerCase().indexOf(" from ")).replace("'", "''");
	
			//Using trnMigrateListQueryFormats to support DBs that don't accept || as concatenation operator
	
			// 1-May-19- Fix the function- remove the casting (Miron's fix)
			DbInterface dbObj = com.k2view.cdbms.lut.InterfacesManager.getInstance().getTypedInterface(interface_name, sourceEnvName);
	
			String interface_type = dbObj.jdbcDriver;
			Object[] trnMigrateListQueryFormats_input = {interface_type, versionInd};
			Map<String, String> trnMigrateListQueryFormats_values = getTranslationValues("trnMigrateListQueryFormats", trnMigrateListQueryFormats_input);
			String query_format = trnMigrateListQueryFormats_values.get("query_format");
	
			//Query format is supplied --> modify the query acording to the given query format in trnMigrateListQueryFormats
			if (query_format != null && !query_format.isEmpty()) {
				// TDM 5.1- add the handle of configurable separator for special formats- the separator may need to be added to the trnMigrateListQueryFormats
				String sql_part1 = StringUtils.substringBefore(sql.toLowerCase(), qry_entity_col) + query_format;
	
				if (!openSeparator.equals("") && !closeSeparator.equals("")) // if the open and close separators for the entity id are populated
				{
					StringBuffer sqlStr = new StringBuffer(query_format);
					// Get the substring between source env and entity id
	
					String formatSeparator = query_format.substring(query_format.indexOf("<source_env_name>") + "<source_env_named>".length(), query_format.indexOf("<entity_id>"));
					formatSeparator = formatSeparator.replaceFirst("'" + separator + "'", "");
					String insertOpenStr = "'" + openSeparator + "'" + formatSeparator;
					String insertCloseStr = formatSeparator + "'" + closeSeparator + "'";
					sqlStr.insert(sqlStr.indexOf("<entity_id>"), insertOpenStr);
					sqlStr.insert(sqlStr.indexOf("<entity_id>") + "<entity_id>".length(), insertCloseStr);
					sql_part1 = select + " " + sqlStr.toString();
				}
	
				if (versionInd.equals("true")) {
					//Modify entities to be in the format of <source_env>_<entity_id>_<task_name>_<timestamp> according to supplied query format
					sql_part1 = sql_part1.replace("<source_env_name>", "'" + sourceEnvName + "'");
					sql_part1 = sql_part1.replace("<entity_id>", qry_entity_col);
					sql_part1 = sql_part1.replace("<task_name>", "'" + taskName + "'");
					sql_part1 = sql_part1.replace("<timestamp>", "'" + timeStamp + "'");
					modified_sql = sql_part1.replace("'", "''") + sql_part2;
				} else {
					//Modify entities to be in the format of <source_env>_<entity_id>  according to supplied query format
					sql_part1 = sql_part1.replace("<source_env_name>", "'" + sourceEnvName + "'");
					sql_part1 = sql_part1.replace("<entity_id>", qry_entity_col);
					modified_sql = sql_part1.replace("'", "''") + sql_part2;
				}
			}
			//No query format --> modfiy query by using || concatenation operator
			else {
				// TDM 5.1- concatenate the open and close separators to the qry_entity_col variables
	
				if (!openSeparator.equals(""))
					qry_entity_col = "''" + openSeparator + "''||" + qry_entity_col;
	
				if (!closeSeparator.equals(""))
					qry_entity_col = qry_entity_col + "||''" + closeSeparator + "''";
	
				if (versionInd.equals("true")) { //Modify entities to be in the format of <source_env>_<entity_id>_<task_name>_<timestamp>
					modified_sql = select + " ''" + sourceEnvName + separator + "''||" + qry_entity_col + "||''" + separator + taskName + separator + timeStamp + "''" + sql_part2;
				} else { ////Modify entities to be in the format of <source_env>_<entity_id>
					modified_sql = select + " ''" + sourceEnvName + separator + "''||" + qry_entity_col + sql_part2;
				}
			}
			
		} else { //External Flow was supplied to create the entity list table.
			
			interface_name = "DB_CASSANDRA";
			modified_sql = getCommandForExtractAllCL(luName, externalTableFlow, taskExecutionId, luId, dcName);
			
		}

		if (dcName != null && !dcName.isEmpty()) {
			batchCommand = "batch " + luName + " from " + interface_name + " using (?) FABRIC_COMMAND=\"sync_instance " + luName + ".?\" WITH AFFINITY='" + dcName + "' ASYNC=true";
		} else {// input DC is empty
			batchCommand = "batch " + luName + " from " + interface_name + " using (?) FABRIC_COMMAND=\"sync_instance " + luName + ".?\" WITH ASYNC=true";
		}		
		
		Map<String, String> batchStrings = new HashMap<>(); 
		batchStrings.put("batchCommand", batchCommand);
		batchStrings.put("usingClause", modified_sql);
		return batchStrings;
	}

	private static String getCommandForExtractAllCL(String luName, String externalTableFlow, String taskExecutionId, Long luId, String dcName) throws Exception {
			// TDM 7.5.1 - If the entity List table does not exists create it
			String createEntityListTab = "broadway " + luName + ".createLuExternalEntityListTable luName = " + luName;
			//log.info("createEntityListTab: " + createEntityListTab);
			fabric().execute(createEntityListTab);
			
			String affinity = !Util.isEmpty(dcName) ? "affinity='" + dcName + "'" : "";
			String batchCommand = "BATCH " + luName + ".(CL_"+ luName + "_" + taskExecutionId + ") fabric_command=? with " + affinity + " async=true";
			//log.info("Custom Logic batchCommand: " + batchCommand);

			String broadwayCommand = "broadway " + luName + "."  +  externalTableFlow +  " iid=?, LU_NAME='" + luName + "'";
			//log.info("Custom Logic broadwayCommand: " + broadwayCommand);
			String batchId = "" + fabric().fetch(batchCommand, broadwayCommand).firstValue();
			db("TDM").execute("UPDATE task_execution_list set execution_status = 'STARTEXECUTIONREQUESTED', fabric_execution_id = ? " + 
				"WHERE task_execution_id=? and lu_id = ?", batchId, taskExecutionId, luId);

			String waitForBatch = "broadway " + luName + ".WaitForCustomLogicFlow luName = " + luName + ", batchId = '" + batchId + "'";
			//log.info("Custom Logic waitForBatch: " + waitForBatch);
			Db.Row entityListTableRec = fabric().fetch(waitForBatch).firstRow();
			String entityListTable = "" + entityListTableRec.get("value");
						
			return "select tdm_eid from " + entityListTable + " where task_execution_id = '" +  taskExecutionId + "'";
		
	}

}
