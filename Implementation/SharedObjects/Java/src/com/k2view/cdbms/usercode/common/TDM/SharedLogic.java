/////////////////////////////////////////////////////////////////////////
// Project Shared Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.common.TDM;

//import com.google.gson.Gson;
import com.k2view.cdbms.exceptions.InstanceNotFoundException;
import com.k2view.cdbms.lut.DbInterface;
import com.k2view.cdbms.shared.Db;
import com.k2view.cdbms.shared.logging.LogEntry;
import com.k2view.cdbms.shared.logging.MsgId;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.desc;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.out;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.type;
import com.k2view.fabric.common.Json;
import com.k2view.fabric.common.Util;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.k2view.cdbms.shared.user.UserCode.*;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.DecisionFunction;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.RootFunction;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.Date;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.fabric.events.*;
import com.k2view.fabric.fabricdb.datachange.TableDataChange;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.fnGetUserPermissionGroup;

@SuppressWarnings({"unused", "DefaultAnnotationParam", "unchecked"})
public class SharedLogic {

	public static final String TDM = "TDM";
	public static final String REF = "REF";
	public static final String TASKS = "TASKS";
	public static final String TASK_EXECUTION_LIST = "task_execution_list";
	public static final String TASK_REF_TABLES = "TASK_REF_TABLES";
	public static final String PRODUCT_LOGICAL_UNITS = "product_logical_units";
	public static final String TASK_REF_EXE_STATS = "TASK_REF_EXE_STATS";
	public static final String TASKS_LOGICAL_UNITS = "tasks_logical_units";
	public static final String TDM_COPY_REFERENCE = "fnTdmCopyReference";
	public static final String PENDING = "pending";
	public static final String RUNNING = "running";
	public static final String WAITING = "waiting";
	public static final String STOPPED = "stopped";
	public static final String RESUME = "resume";

	public static final String FAILED = "failed";
    public  static final String COMPLETED = "completed";
	public static final String GET_CHILDREN_SQL = "WITH RECURSIVE children AS ( " +
			"SELECT lu_name,lu_id,lu_parent_id,lu_parent_name FROM product_logical_units WHERE lu_name=? and be_id=? " +
			"UNION ALL SELECT a.lu_name, a.lu_id, a.lu_parent_id,a.lu_parent_name " +
			"FROM product_logical_units a " +
			"INNER JOIN children b ON a.lu_parent_id = b.lu_id) " +
			"SELECT  string_agg('''' ||  unnest || '''' , ',') FROM children ,unnest(string_to_array(children.lu_name, ',')); ";
	public static final String PARENTS_SQL = "SELECT lu_name  FROM product_logical_units WHERE be_id=? AND lu_parent_id is null";

	@desc("New function for TDM 5.1- this function is called if the open and close separators are populated for the IID.\r\nThe function returns the IID without the separators + the end position of the IID (including the separators) in the UID")
	@out(name = "res", type = Object[].class, desc = "")
	public static Object[] fnRemoveSeparatorsFromIID(String i_UID, String openSeparator, String closeSeprator) throws Exception {
		String instanceID = "";
		int endOfIidIndex = 0;
		
		if(openSeparator != null && !openSeparator.equals(""))
		{
			int indexOfOpenSeparator = i_UID.indexOf(openSeparator);
			int indexOfCloseSeparator = i_UID.indexOf(closeSeprator);
			int startIndex = indexOfOpenSeparator + openSeparator.length();
		
			endOfIidIndex= indexOfCloseSeparator + closeSeprator.length();
			instanceID= i_UID.substring(startIndex,indexOfCloseSeparator);
		}
		return new Object[]{instanceID, endOfIidIndex};
	}

	@desc("output contains Object array \r\n" +
			"position 0 - k2_tdm_eid \r\n" +
			"position 1 - source_env\r\n" +
			"position 2 - instance ID itself\r\n" +
			"position 3 - task name ( in case of versioning )\r\n" +
			"position 4 - timestamp ( in case of versioning )\r\n" +
			"The instance id may be in the following formats : \r\n" +
			"_dev_<entity_id>\r\n" +
			"_dev_<entity_id>_<task_name>_<timestamp>\r\n" +
			"<environment>_<entity_id>\r\n" +
			"<environment>_<entity_id>_<task_name>_<timestamp>")
	@out(name = "rs", type = Object[].class, desc = "")
	public static Object[] fnValidateNdGetInstance() throws Exception {
		String origUid = getInstanceID();
		
		// TDM 7.1 - fix- get out the clone id before the split. For example: SRC_66#params#{"clone_id"=1}|//
		
		Object[] splitCloneId = origUid.split("#params#");
		origUid = "" + splitCloneId[0];
		
		// end of Fix
		
		Object[] splitIID = fnSplitUID(origUid);
		
		return new Object[]{origUid, splitIID[1], splitIID[0], splitIID[2], splitIID[3]};
	}


	@type(RootFunction)
	@out(name = "k2_tdm_eid", type = String.class, desc = "")
	@out(name = "source_env", type = String.class, desc = "")
	@out(name = "IID", type = String.class, desc = "")
	@out(name = "task_name", type = String.class, desc = "")
	@out(name = "timestamp", type = String.class, desc = "")
	public static void fnRtK2TDMRoot(String TDM_INSTANCE_ID) throws Exception {
		yield(fnValidateNdGetInstance());
	}


    @type(RootFunction)
    @out(name = "ENTITY_ID", type = String.class, desc = "")
    public static void fnRootLuParams(String ENTITY_ID) throws Exception {
        if (1 == 2) yield(new Object[]{null});

    }


	public static void fnEnrichmentLuParams() throws Exception {
		//*****************************************************//
		//    Function used to update the table LU_PARAMS      //
		//                                                     //
		//This function reads the parameters                   //
		//configured in the translator trnLuParams -           //
		//execute the query associated -                       //
		//and finally run and update statement into LU_PARAMS  //
		//                                                     //
		//*****************************************************//
		
		Db ciTDM = db("TDM");
		//ResultSetWrapper rs = null;
		Set<String> pgColsList = new HashSet<>();
		Set<String> luColsList = new HashSet<>();
		String tblName = getLuType().luName.toLowerCase() + "_params";
		String tblNameFabric = getLuType().luName + ".LU_PARAMS";
		String tblInfoSql = "select column_name from INFORMATION_SCHEMA.COLUMNS where table_name = ?";
		String luParamTblSql = "DESCRIBE TABLE " + tblNameFabric;
		StringBuilder sbCreStmt = new StringBuilder().append("CREATE TABLE IF NOT EXISTS " + tblName + "(");
		StringBuilder sbAltStmtAdd = new StringBuilder().append("ALTER TABLE " + tblName + " ADD COLUMN IF NOT EXISTS ");
		StringBuilder sbAltStmtRem = new StringBuilder().append("ALTER TABLE " + tblName + " DROP COLUMN IF EXISTS ");
		String LuName = ("" + getLuType().luName).toUpperCase();
		String prefix = "";
		
		Object[] insRs = fnValidateNdGetInstance();
		Db.Rows rows= null;
		
		boolean tdmLuParamAltered = false;
		if (!inDebugMode()) {
			ciTDM.beginTransaction();
		}
		
		//add a drop of the view of the related BEs when the <LU_NAME>_PARAMS TDMDB table is altered
		try{
		    if (!inDebugMode())
		    {
				rows= ciTDM.fetch(tblInfoSql, tblName);
				for (Db.Row row:rows) {
					if(row.get("column_name") != null)
						pgColsList.add((row.get("column_name") + ""));
				}
				
				rows = ludb().fetch(luParamTblSql);
				for (Db.Row row : rows)
		    	{
					String columnName = "";
					columnName= row.get("column") +"";
			        if(columnName.toLowerCase().equalsIgnoreCase("ENTITY_ID") ||
			                columnName.toLowerCase().equalsIgnoreCase("SOURCE_ENVIRONMENT"))
			        {
			            sbCreStmt.append(prefix + columnName.toLowerCase() + " TEXT");
			        }else if (!columnName.equals(""))
			        {
			            sbCreStmt.append(prefix + "\"" + LuName + "." + columnName.toUpperCase() + "\" TEXT ");
		        	}
		        	luColsList.add(columnName.toUpperCase());
			        prefix = ",";
		    	}
		    } else //in Debug Mode
			{
				rows = fabric().fetch(luParamTblSql);
			    
				for (Db.Row row : rows)
			    {
					String columnName = "";
					columnName= row.get("column") + "";
			        if(columnName.toLowerCase().equalsIgnoreCase("ENTITY_ID") ||
			                columnName.toLowerCase().equalsIgnoreCase("SOURCE_ENVIRONMENT"))
			        {
			            sbCreStmt.append(prefix + columnName.toLowerCase() + " TEXT");
			        }else if (!columnName.equals(""))
			        {
			            sbCreStmt.append(prefix + "\"" + LuName + "." + columnName.toUpperCase() + "\" TEXT ");
			        }
			        luColsList.add(columnName.toUpperCase());
			        prefix = ",";
			    }
			}
				
		    sbCreStmt.append(", CONSTRAINT " + tblName + "_pkey PRIMARY KEY (entity_id, source_environment))");
		
		}finally
		{
		   if(rows != null) rows.close();
			
		}
		if(pgColsList.size() == 0){//If its first time
		    if(!inDebugMode()){
				try {
					//log.info("Running Create table: " + sbCreStmt.toString());
					ciTDM.execute(sbCreStmt.toString());
				}catch (Exception e) {
					//log.error("fnEnrichmentLuParams - Error Message: " + e.getMessage());
					if (e.getMessage().toString().contains("duplicate key value violates unique constraint")) {
						log.warn("fnEnrichmentLuParams - Paramaters table " + tblName + " already exists, no need to create it");
						ciTDM.execute("rollback");
					}
		    	}
		    }
		}else{
		    //Check if PG params table has all the LUDB params table columns  - if not --> ADD columns to the PG params table
		    prefix = "";
		    boolean runCmnd = false;
		
		    for(String mapEnt : luColsList){
		        if( !mapEnt.equals("ENTITY_ID") && !mapEnt.equals("SOURCE_ENVIRONMENT") ){
		            mapEnt = LuName + "." + mapEnt ;
		        }
		
		        if(!pgColsList.contains(mapEnt) && !"ENTITY_ID".equals(mapEnt) && !"SOURCE_ENVIRONMENT".equals(mapEnt)){
		
		            sbAltStmtAdd.append(prefix + "\"" + mapEnt + "\" TEXT");
		            prefix = ", ADD COLUMN IF NOT EXISTS ";
		            runCmnd = true;
		
		            //set the tdmLuParamAltered indicator to true
		            tdmLuParamAltered= true;
		
		        }
		    }
		
		    if(runCmnd && !inDebugMode()) {
				ciTDM.execute(sbAltStmtAdd.toString());
			}
		    //Check if LUDB params table is missing columns that PG params table has  - if yes --> drop those PG columns
		    prefix = "";
		    runCmnd = false;
		    for(String mapEnt : pgColsList){
		        mapEnt=mapEnt.replace(LuName+".","");
		
		        if(!luColsList.contains(mapEnt) && !"entity_id".equals(mapEnt) && !"source_environment".equals(mapEnt)){
		
		            sbAltStmtRem.append(prefix + "\"" + LuName + "." + mapEnt + "\"");
		            prefix = ", DROP COLUMN IF EXISTS ";
		            runCmnd = true;
		
		            //set the tdmLuParamAltered indicator to true
		            tdmLuParamAltered= true;
		        }
		    }
		
		    if(runCmnd && !inDebugMode()) {
				ciTDM.execute(sbAltStmtRem.toString());
			}
		}
		
		//drop the view from the TDMDB if that LU table was altered
		if(tdmLuParamAltered)
		{
		    // Get the related BEs of the logical unit
		    String sql_be_list = "SELECT be.be_name FROM product_logical_units lu, business_entities be " +
		            "WHERE lower(lu.lu_name) = ? and lu.be_id = be.be_id and be.be_status = 'Active'";
		
		    if (!inDebugMode()) {
		        String env_name = ""+ insRs[1];
		
				Db.Rows rsBeList = ciTDM.fetch(sql_be_list, getLuType().luName.toLowerCase());
				for (Db.Row beRow : rsBeList) {
		            // DROP the view for the BE if exists
					String viewName = "lu_relations_" + beRow.get("be_name") + "_" + env_name;
					ciTDM.execute("DROP MATERIALIZED VIEW IF EXISTS \"" + viewName + "\"");
		        }
			
				if (rsBeList != null) rsBeList.close();
		    }
		
		} // end  if(tdmLuParamAltered)
		
		// TDM 6.0 - In case of versioning extact, the paramters are not supported, 
		//therefore the LU_PARAM will be populated only if the VERSOIN_NAME from instanceID will be empty
		if (insRs[2] != "") // If version_name is not null then populate the LU_PARAM tables
		{
			// Reading the translation data and gettig th queries
			Map<String,Map<String,String>> data = getTranslationsData("trnLuParams");
			StringBuilder sqlUpdateTDM = new StringBuilder().append(" ON CONFLICT ON CONSTRAINT " + tblName + "_pkey Do update set ");
			StringBuilder sqlInsertTDM = new StringBuilder().append("insert into " + tblName + "(");
			StringBuilder sqlInsertTDMBind = new StringBuilder().append(" values (");
			
			// Tali- 29-Nov-18- add a stringInsertFabricLuParam to insert the columns without the concatenation of the lu name
			StringBuilder stringInsertFabricLuParam = new StringBuilder().append("insert into " + tblName + "(");
			
			//Check if we have elements in the Translator
			if(data.size() > 0){
			
			    //Parameters that will be used for Update
			    Object[] params = new Object[data.size()];
			    //Counter to insert the parameters in the correct position
			    int i=0;
			
			    //Getting the values from the LU
			    prefix = "";
			    for(String index: data.keySet()){
			
			        //String which contains the values returned by the query
			        StringBuilder values = new StringBuilder();
			
			        Map<String,String> valMap = data.get(index);
			    
					// Ticket #23164- add a trim of leading and trailing spaces for the column name 				
			        String luParamColName = valMap.get("COLUMN_NAME").trim();
			
			        //String columnName = "\"" + LuName + "."+ (valMap.get("COLUMN_NAME")).toUpperCase() + "\"";
			        String columnName = "\"" + LuName + "."+ luParamColName.toUpperCase() + "\"";
					String sql = valMap.get("SQL");
				
					//log.info("LU Param column name: " +  columnName + ", sql: " + sql);
			
			        //Check if SQL query contains distinct and add it if not
			        if (!sql.contains("distinct")){
			            sql = sql.replace("select","select distinct");
			        }
			
			        // Tali- 29-Nov-18- building the insert intot he ludb (fabric)
			        stringInsertFabricLuParam.append(prefix + luParamColName );
			
			        //Building the update statement
			        sqlUpdateTDM.append(prefix + columnName+" = ?");
			        sqlInsertTDM.append(prefix + columnName );
			        //Execution of the query
			        Object[] valuesArr = null;
					Db.Rows rs1 = null;
					rs1 = ludb().fetch(sql);
			        values.append("{");
		
					for (Db.Row row : rs1) {
						//Skip null values
						if (row.cell(0) != null) {
							String val = "" + row.cell(0);
							val.replace("\"", "\\\"");
							values.append("\""+ val +"\",");
			            }
			        }
			
			        //Check if the last element is a comma and remove it
			        if (values.lastIndexOf(",") == values.length()-1){
			        	values.deleteCharAt(values.lastIndexOf(","));
			        }
			        values.append("}");
			
			        //If no values, set NULL
			        if (values.toString().equals("{}")){
				        params[i] = null;
			        }else{
			            params[i] = values.toString();
			        }
			        sqlInsertTDMBind.append(prefix + " ? ");
			        i++;
		
			        prefix = ",";
					if(rs1 != null) rs1.close();
			    }
			
			    Object[] finParams = new Object[params.length * 2 + 2];
			    Object[] finParamsLu = new Object[params.length + 2];
			    for (i = 0; i < params.length; i++) {
			        finParams[i] = params[i];
			        finParamsLu[i] = params[i];
			    }
			
			    finParams[data.size()] = insRs[2];
			    finParams[data.size() + 1] = insRs[1];
			
			    finParamsLu[data.size()] = insRs[2];
			    finParamsLu[data.size() + 1] = insRs[1];
			
			    for (i = 0; i < params.length; i++) {
			        finParams[params.length + 2 + i] = params[i];
			    }
			
			    // Tali- 29-Nov-18- add a stringInsertFabricLuParam to insert the columns without the concatenation of the lu name
			    stringInsertFabricLuParam.append(", entity_id, source_environment) ");
			
			    sqlInsertTDM.append(", entity_id, source_environment) ");
			    sqlInsertTDMBind.append(", ?, ?) ");
			
			
			    //log.info("Insert into LU params- TDMDB :"+ sqlInsertTDM.toString() + sqlInsertTDMBind.toString() + sqlUpdateTDM.toString());
			
			    if(!inDebugMode()){
					ciTDM.execute(sqlInsertTDM.toString() + sqlInsertTDMBind.toString() + sqlUpdateTDM.toString(),finParams);
			
			    }
				
				// Tali- TDM 5.5- fix- add concatenate the lu_name to the table name
				ludb().execute(stringInsertFabricLuParam.toString().replace(tblName, tblNameFabric).replace("insert", "insert or replace ") + sqlInsertTDMBind.toString(), finParamsLu);
			}
			else {//no parameters defined - inserting only entity_id and source_environment values
			    Object[] bind_for_no_params = new Object[2];
			    bind_for_no_params[0] = insRs[2];
			    bind_for_no_params[1] = insRs[1];
			
			    // TALI- fix- 2-Dec-18- add on conflit on constraint do nothing to avoid a violation of a PK if the entity already exists in the params table
			    if(!inDebugMode()){
					ciTDM.execute("insert into " + tblName + " (ENTITY_ID, SOURCE_ENVIRONMENT) values (?,?)" + " ON CONFLICT ON CONSTRAINT " + tblName + "_pkey DO NOTHING", bind_for_no_params);
			    }
				
				// Tali- TDM 5.5- fix- add concatenate the lu_name to the table name
				ludb().execute("insert or replace into "+ tblNameFabric + " (ENTITY_ID, SOURCE_ENVIRONMENT) values (?,?)", bind_for_no_params);
			}
			
			if (!inDebugMode()) {
				ciTDM.commit();
			}
			// In case JSON is needed
			//Create a stream to hold the output
			//ByteArrayOutputStream baos = new ByteArrayOutputStream();
			//PrintStream ps = new PrintStream(baos);
			// Save the old output
			//PrintStream old = System.out;
			// Use the special stream
			//System.setOut(ps);
			//Convert the ResultSetWrapper to JSON string
			//com.k2view.cdbms.shared.Utils.writeJSON(rs,System.out,true);
			//System.out.flush();
			//System.setOut(old);
			//Get the value from the new ByteArrayOutputStream
		
		}
	}


	public static void fnEnrichmentChildLink() throws Exception {
		/*
		This function will populate the parent-child link table in postgress
		added by: sereenm
		*/
		//trnChildLink translation will include the child LU Name and the sql query which will return the child entities
		Map<String, Map<String, String>> trnChildLinkVals = getTranslationsData("trnChildLink");
		Db ciTDM = db("TDM");
		String uid = getInstanceID();
		//log.info("Running - fnEnrichmentChildLink for instance: " + uid);
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
		
		//TDM 7.2 - divide the uid to instance Id and Parameters if the parameters exist.
		Object[] splitCloneId = uid.split("#params#");
		
		uid = "" + splitCloneId[0];
		String params = "";
		String cloneId = "";
		String cloneIdPattern = "\\{\"clone_id\"=(.*?)\\}";
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(cloneIdPattern);
		
		if (splitCloneId.length > 1) {
			params = "" + splitCloneId[1];
		
			//log.info("fnEnrichmentChildLink - params: " + params);
			
			java.util.regex.Matcher matcher = pattern.matcher(params);
			if (matcher.find()) {
				cloneId = matcher.group(1);
			}
		}
		//log.info("fnEnrichmentChildLink after removing params: " + uid);
		//log.info("fnEnrichmentChildLink - cloneId: " + cloneId);
		
		Object[] splitUID = fnSplitUID(uid);
		String instanceId = "" + splitUID[0];
		String srcEnv = "" + splitUID[1];
		//TDM 6.0 - VERSION_NAME and VERSION_DATETIME are part of the new Primary key
		String verName = "" + splitUID[2];
		String verDateTime = "" + splitUID[3];
		Date verDateTimeDate = null;
		
		if (!"".equals(verDateTime)) {
			verDateTimeDate = formatter.parse(verDateTime);
			verDateTime = sdf.format(verDateTimeDate);
		}
		
		//log.info("fnEnrichmentChildLink - srcEnv: " + srcEnv + ", instanceId: "  + instanceId + ", verName: " + verName + ", verDateTime: " + verDateTime);
		String parentLU = getLuType().luName;
		
		Set<String> keyset = trnChildLinkVals.keySet();
		
		Db.Rows childEIDs = null;
		Db.Rows childTarEIDs = null;
		//Get the LU Name, as the tdm_lu_type_relation_eid is part of more than one LU
		String tableName = parentLU + ".tdm_lu_type_relation_eid";
		String tableNameTar = parentLU + ".tdm_lu_type_rel_tar_eid";
		//log.info("fnEnrichmentChildLink - Fabric Table: " + tableName);
		if (!inDebugMode()) {
			ciTDM.beginTransaction();
		}
		//TALI- Fix ticket #9523- delete the parent IID records, if exist, before the insert
		//Add the LU Name to the table name
		// TDM 6.0 - VERSION_NAME and VERSION_DATETIME are part of the new Primary key and are added to the where clause
		//String DELETE_SQL = "delete from tdm_lu_type_relation_eid where source_env = ? and lu_type_1 = ? and lu_type1_eid = ? ";
		String verAddition = verName.equals("") ? "and version_name = ''" : "and version_name = ? and version_datetime = ?";
		String DELETE_SQL = "delete from tdm_lu_type_relation_eid where source_env = ? and lu_type_1 = ? and lu_type1_eid = ? and lu_type_2 = ?" + 
					verAddition;
		
		//TDM 7 - Handle TDM_LU_TYPE_REL_TAR_EID table 
			// Fix the query- add the child LU to the condition
		String DELETE_TAR_SQL = "delete from tdm_lu_type_rel_tar_eid where target_env = ? and lu_type_1 = ? and lu_type1_eid = ? and lu_type_2 = ?";
		String targetEnv = "" + ludb().fetch("SET " + parentLU + ".TDM_TAR_ENV_NAME").firstValue();
		
		String currDate = sdf.format(new java.util.Date());
		
		//log.info("after the delete");
		String taskExecID = "" + fabric().fetch("set TDM_TASK_EXE_ID").firstValue();
		for (String key : keyset) {
			// TDM 7.3 - 17/01/22 - Check if the child LU is part of the task, if it is not part of the task no need to populate its data
			
			String validateLuSql = "SELECT count(1) " +
						"FROM task_execution_list t, product_logical_units parent, product_logical_units child " + 
		                "WHERE t.task_Execution_id = ? and t.be_id  = parent.be_id and parent.lu_id = t.parent_lu_id " +
		                "and parent.lu_name = ? and t.be_id = child.be_id and child.lu_id = t.lu_id and child.lu_name = ?";
		
			Long cntLu = (Long)ciTDM.fetch(validateLuSql, taskExecID, parentLU, key).firstValue();
			
			if (cntLu == 0) {
				// The child LU (key) is not part of the task, therefore continue to the next child LU
				continue;
			}
		
				//TDM 7.2 - The tdm_lu_type_relation_eid table should be handled (delete old data and load new data) only if handling one instance (no cloning) 
		// or handling the first clone only - in case of cloning there is no need to delete and reinsert the same data per clone, it should
		// be done only once.
		// For tdm_lu_type_rel_tar_eid table, the data should be handled for each clone as it is based on target values.
		
		if ("".equals(cloneId) || "1".equals(cloneId)) {
			if (verName.equals("")) {
					ciTDM.execute(DELETE_SQL,srcEnv, parentLU, instanceId, key);
				} else {
					ciTDM.execute(DELETE_SQL,srcEnv, parentLU, instanceId, key, verName, verDateTime);
			}
		}
		
			//log.info ("fnEnrichmentChildLink - key: " + key + " for instance: " + uid);
			Map<String, String> mapVal = trnChildLinkVals.get(key);
			String sql = mapVal.get("child_lu_eid_sql");
			String sqlTar = mapVal.get("child_lu_tar_eid_sql");
		    
			//log.info("Getting child EIDS for key: " + key + ", with sql: " + sql);
			childEIDs = ludb().fetch(sql);
		
			//TDM 7.2 - The tdm_lu_type_relation_eid table should be handled (delete old data and load new data) only if handling one instance (no cloning) 
			// or handling the first clone only - in case of cloning there is no need to delete and reinsert the same data per clone, it should
			// be done only once.
			// For tdm_lu_type_rel_tar_eid table, the data should be handled for each clone as it is based on target values.
		
			if ("".equals(cloneId) || "1".equals(cloneId)) {		
				for (Db.Row row : childEIDs) {
				
					//log.info("Adding child record for instance: " + uid + " and LU Type: " + key + ". child instance: " + row.cell(0));
		    	
					// TDM 6.0 - VERSION_NAME and VERSION_DATETIME are added to the table
					Object[] values;
					Object[] valuesLUDB;
					if (verName.equals("")) {
						values = new Object[]{srcEnv, parentLU, key, instanceId, row.cell(0), currDate, currDate};
					} else {
						values = new Object[]{srcEnv, parentLU, key, instanceId, row.cell(0), currDate, verName, verDateTime, currDate};
					}
		
					// TDM 6.0 - VERSION_NAME and VERSION_DATETIME are added to the table
					if (verName.equals("")) {
						valuesLUDB = new Object[]{srcEnv, parentLU, key, instanceId, row.cell(0), currDate, "''", "''"};
					} else {
						valuesLUDB = new Object[]{srcEnv, parentLU, key, instanceId, row.cell(0), currDate, verName, verDateTime};
					}
					
					// TDM 6.0 - VERSION_NAME and VERSION_DATETIME are added to the table
					//log.info("Inserting into Fabric to tdm_lu_type_relation_eid table for lu type: " + key);
					ludb().execute("insert or replace into " + tableName + "(source_env,lu_type_1,lu_type_2,lu_type1_eid,lu_type2_eid,creation_date,version_name,version_datetime) values(?,?,?,?,?,?,?,?)",valuesLUDB);
			
					if (!inDebugMode()) {
						// TDM 6.0 - VERSION_NAME and VERSION_DATETIME are added to the table
						if (verName.equals("")) {
							//log.info("Inserting into TDM - tdm_lu_type_relation_eid table for lu type: " + key);
							ciTDM.execute("insert into tdm_lu_type_relation_eid(source_env,lu_type_1,lu_type_2,lu_type1_eid,lu_type2_eid,creation_date) values(?,?,?,?,?,?) ON CONFLICT ON CONSTRAINT tdm_lu_type_relation_eid_pk DO update set creation_date = ?", values);
						} else {
							//log.info("Inserting into TDM - tdm_lu_type_relation_eid table with version Data for lu type: " + key);
							ciTDM.execute("insert into tdm_lu_type_relation_eid(source_env,lu_type_1,lu_type_2,lu_type1_eid,lu_type2_eid,creation_date,version_name,version_datetime) " +
								"values(?,?,?,?,?,?,?,?) ON CONFLICT ON CONSTRAINT tdm_lu_type_relation_eid_pk DO update set creation_date = ?", values);
						}
					}
				}
			}
			
			//TDM 7 - In case of delete from target, the TDM_LU_TYPE_REL_TAR_EID table should be updated 
			if (fnDecisionDeleteFromTarget()) {
				//log.info("TEST- deleting tdm_lu_type_rel_Tar_eid TDM table for parent LU: " + parentLU+ ", Parent ID: " +instanceId + ", and child LU: " + key );
				ciTDM.execute(DELETE_TAR_SQL, targetEnv, parentLU, instanceId, key);
				
				childTarEIDs = ludb().fetch(sqlTar);
				for (Db.Row row : childTarEIDs) {
					
		    		Object[] values =  new Object[]{targetEnv, parentLU, key, instanceId, row.cell(0), currDate};
			
					ludb().execute("insert or replace into " + tableNameTar + "(target_env,lu_type_1,lu_type_2,lu_type1_eid,lu_type2_eid,creation_date) values(?,?,?,?,?,?)",values);
		
					if (!inDebugMode()) {
						ciTDM.execute("insert into tdm_lu_type_rel_tar_eid(target_env,lu_type_1,lu_type_2,lu_type1_eid,lu_type2_eid,creation_date) values(?,?,?,?,?,?)", values);
					}
				}
			}
		
		}
		
		if (childEIDs != null) {
			childEIDs.close();	
		}
		if (!inDebugMode()) {
			ciTDM.commit();
		}
	}


	@out(name = "res", type = Object[].class, desc = "")
	public static Object[] fnSplitUID(String uid) throws Exception {
		// TDM 5.1- fix the function to support also dataflux mode when the instance id also has version name  +datetime
		// In addition- remove the open and close separators
		String instanceId = "";
		String envName = "";
		String separator = "";
		String iidOpenSeparator = "";
		String iidCloseSeparator = "";
		// TDM 6.0 - The version name (task name) and version datetime should be returned
		String versionName = "";
		String versionDateTime = "";
				
		String iidSeparator = "" + db("TDM").fetch("Select param_value from tdm_general_parameters where param_name = 'iid_separator'").firstValue();
        //separator = !Util.isEmpty(iidSeparator) ? iidSeparator : "_";
		if ( !Util.isEmpty(iidSeparator) && !"null".equals(iidSeparator) ) {
			separator = iidSeparator;
		} else {
			separator = "_";
		}
		//log.info("fnSplitUID: separator: " + separator);
		// TDM 5.1- get open and close separators for the instanceId. If they exist- get the instanceId according the open and close separators
		//Set the SQL parameter
		
		Object[] separators = fnGetIIdSeparatorsFromTDM();
		//log.info("fnSplitUID - Input UID: " + uid);
		//log.info("fnSplitUID - separators[0]: " + separators[0] + ", separators[1]: " + separators[1]);
		if(separators[0] != null && separators[0].toString() != null && !separators[0].toString().isEmpty() )
			iidOpenSeparator = separators[0].toString();
		
		if(separators[1] != null && separators[1].toString() != null && !separators[1].toString().isEmpty())
			iidCloseSeparator = separators[1].toString();
		
		if (uid.startsWith("_dev")) {
			uid = uid.replaceFirst("_dev_", "dev_");
			envName = "_dev";
		}
			
		try {
		 	
				String[] split_uid = uid.split(separator);
			
				if(envName.equals(""))
		        	envName = split_uid[0].toString();
			
				// Check if the open and close separators are populated
				if(iidOpenSeparator.equals("") || uid.indexOf(iidOpenSeparator)== -1 || iidCloseSeparator.equals("") || uid.indexOf(iidCloseSeparator)== -1) // the separators for the IID are not defined in the TDM DB or in the input UID
				{
					//log.info("fnSplitUID - No Separators");
					instanceId = split_uid[1].toString();
					//log.info("fnSplitUID - instanceId: " + instanceId);
					//TDM 6.0, get the version name and datetime
					if (split_uid.length==4) {  //entity in the format of <environment>_<entity_id>_<task_name>_<timestamp>
						versionName = split_uid[2].toString();
						versionDateTime = split_uid[3].toString();
					}
				}
				else // the open and close separators are populated. Both of them have to be poplated. You cannot populate just one of them
				{
					//log.info("fnSplitUID - There are separators");
					Object[] IID_info = fnRemoveSeparatorsFromIID(uid,  iidOpenSeparator, iidCloseSeparator);     
					instanceId = IID_info[0].toString();
					//TDM 6.0, get the version name and datetime
					int pos = Integer.valueOf("" + IID_info[1]);
					// if the uid is longer than the end position of the instance id including the close separator
					if (uid.length() > pos) {
						// Add 1 to jump to the beginning of the task name
						String[] split_version = (uid.substring(pos + 1)).split(separator);
						versionName = split_version[0];
						versionDateTime = split_version[1];
					}
				}
		} catch (Exception e) {
		    if (e.getMessage().toString().contains("String index out of range"))
		        throw new Exception("Environment Name Is Missing, String index out of range");
		    else
		        throw new Exception(e.getMessage());
		}
		finally
		{
			// If the input uid does not have _ separator
			if(uid.indexOf(separator) == -1)
				 throw new Exception("Environment Name Is Missing, Underscore not found");
		
		}
		
		//log.info("fnSplitUID - Output: instanceID: " + instanceId + ", envName: " + envName + ", versionName: " + versionName + ", versionDateTime: " + versionDateTime);
		return new Object[]{instanceId, envName, versionName, versionDateTime};
	}

	public static void fnPopINSTANCE_TABLE_COUNT() throws Exception {
		String tableName = getLuType().luName + "._k2_objects_info";
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		String currDate = sdf.format(new Date());
		String countTable = getLuType().luName + ".INSTANCE_TABLE_COUNT";
		
		// Fetching all LU tables
		Db.Rows luTableList = null;
		try {
			luTableList = fabric().fetch("SELECT distinct table_name FROM " + tableName + " where table_name != ? and table_name != ?",
					"INSTANCE_TABLE_COUNT", "LU_PARAMS");
		
			for (Db.Row row : luTableList) {
		        // Get table's count
				Integer luTableCnt = (Integer) fabric().fetch("select count(*) from " + row.get("table_name")).firstValue();
		
				fabric().execute("insert or replace into " + countTable + " values (?, ?, ?)", getInstanceID(), row.get("table_name"), luTableCnt);
		        if (!inDebugMode()) {
		
					db("TDM").execute("insert into instance_table_count values (?, ?, ?, ?, ?) ON CONFLICT ON CONSTRAINT instance_table_count_pkey DO " +
						"update set sys_update_date = ? , table_count = ? ",
						getLuType().luName, getInstanceID(), currDate, row.get("table_name"), luTableCnt, currDate, luTableCnt);
				}
		    }
		} finally {
		
			if (luTableList != null) luTableList.close();
		}
	}


    @type(RootFunction)
    @out(name = "output", type = String.class, desc = "")
    public static void fnRootINSTANCE_TABLE_COUNT(String input) throws Exception {
        if (1 == 2) {
            yield(new Object[]{null});
        }

    }


	@type(RootFunction)
	@out(name = "dummy_output", type = String.class, desc = "")
	public static void fnPop_TDM_LU_TYPE_RELATION_EID(String dummy_input) throws Exception {
		if(1 == 2)yield(new Object[] {null});
	}


	@out(name = "refSummaryStats", type = Map.class, desc = "")
	public static Map<String,Object> fnGetReferenceSummaryData(String refTaskExecutionId) throws Exception {
		String selectRefTablesStats = "Select count(*), to_char(min(start_time), 'YYYY-MM-DD HH24:MI:SS'), to_char(max(end_time), 'YYYY-MM-DD HH24:MI:SS'), " +
			"execution_status, lu_name from TASK_REF_EXE_STATS es, TASK_REF_TABLES rt where task_execution_id = ? " + 
			"and es.task_id = rt.task_id and es.task_ref_table_id = rt.task_ref_table_id group by execution_status, lu_name order by lu_name";
		
		//ResultSetWrapper taskExecutionRefTables = null;
		Integer tot_num_tables_to_process=0;
		Integer num_of_processed_ref_tables= 0;
		Integer num_of_copied_ref_tables = 0;
		Integer num_of_failed_ref_tables= 0;
		Integer num_of_processing_tables=0;
		Integer num_of_not_started_tables= 0;
		
		Integer noOfRecords=0;
		
		Map <String, Object> refSummaryStatsBuf = new LinkedHashMap<>();
		
		Db.Rows rows = db("TDM").fetch(selectRefTablesStats, refTaskExecutionId);
		
		
		Date calcMinDate = null;
		Date calcMaxDate = null;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String luName = "";
		String prevLuName = "";
		//reportUserMessage("in fnGetReferenceSummaryData");
		for (Db.Row refRec : rows) {
		
			Map <String, Object> refSummaryStats = new LinkedHashMap<>();
			String refStatus = "";
			String min_start_time = "";
			String max_end_time = "";
				
			calcMinDate = null;
			calcMaxDate = null;
		
			if (refRec.cells()[3] != null)
				refStatus = refRec.cells()[3].toString();
		
			if (refRec.cells()[1] != null)
				min_start_time = refRec.cells()[1].toString();
		
			if (refRec.cells()[2] != null)
				max_end_time = refRec.cells()[2].toString();
		
		
			luName = "" + refRec.cells()[4];
			
			if (!prevLuName.equals(luName)) {
				tot_num_tables_to_process=0;
				num_of_processed_ref_tables= 0;
				num_of_copied_ref_tables = 0;
				num_of_failed_ref_tables= 0;
				num_of_processing_tables=0;
				num_of_not_started_tables= 0;
				
				prevLuName = luName;
			}
			//reportUserMessage("in fnGetReferenceSummaryData- in the loop2");
		
			Date minDate = null;
			Date maxDate = null;
		
			if (!min_start_time.equals("")) {
				minDate = formatter.parse(min_start_time);
				//reportUserMessage("min start date: " + min_start_time + " minDate: " + minDate);
			}
		
			if (!max_end_time.equals("")) {
				maxDate = formatter.parse(max_end_time);
				//log.info("max_end_time: " + max_end_time);
				//log.info("maxDate: " + maxDate);
			}
			
			if (calcMinDate == null || (minDate != null && calcMinDate.after(minDate))) // if the minimum date is null or is later the the minimum date of the record- get the minimum date of the record
			{
				calcMinDate = minDate;
				//reportUserMessage("minDate " + minDate);
			}
			
			if (calcMaxDate == null || (maxDate != null && calcMaxDate.before(maxDate))) // If the max date is null or earlier than the max date of the record- get the max date of the record
			{
				calcMaxDate = maxDate;
			}
			
			//reportUserMessage("type of  refRec[0]: " + refRec[0].getClass() );
			noOfRecords = Integer.parseInt(refRec.cells()[0].toString());
			tot_num_tables_to_process += noOfRecords;
			
			switch (refStatus) {
				case "completed":
					num_of_copied_ref_tables += noOfRecords;
					num_of_processed_ref_tables += noOfRecords;
					break;
				case "waiting":
				case "pending":
					num_of_not_started_tables += noOfRecords;
					break;
		
				case "running":
					num_of_processing_tables += noOfRecords;
					break;
		
				default:
					num_of_failed_ref_tables += noOfRecords;
					num_of_processed_ref_tables += noOfRecords;
			}
		
			if(calcMinDate != null) {
				refSummaryStats.put("minStartExecutionDate", calcMinDate);
			}
			else
			{
				refSummaryStats.put("minStartExecutionDate", "");
			}
			
			if(calcMaxDate != null) {
				refSummaryStats.put("maxEndExecutionDate", calcMaxDate);
				//log.info("calcMaxDate: " + calcMaxDate);
			}
			else
			{
				refSummaryStats.put("maxEndExecutionDate", "");
			}
			
			refSummaryStats.put("totNumOfTablesToProcess", tot_num_tables_to_process);
			refSummaryStats.put("numOfProcessedRefTables", num_of_processed_ref_tables);
			refSummaryStats.put("numOfCopiedRefTables", num_of_copied_ref_tables);
			refSummaryStats.put("numOfFailedRefTables", num_of_failed_ref_tables);
			refSummaryStats.put("numOfProcessingRefTables", num_of_processing_tables);
			refSummaryStats.put("numberOfNotStartedRefTables", num_of_not_started_tables);
				
			refSummaryStatsBuf.put(luName, refSummaryStats);
			
		} // end of for loop on the reference tables
		//}// end of if query returns records
		
		if (rows != null) {
			rows.close();
		}
		return refSummaryStatsBuf;
	}



	@desc("Get the information about the copied and failed entities.\r\n" +
			"This function needs to be used by the new WS- wsGetDetailedListForExtractTask - which replaces wsGetIIDFromK2migrate")
	@out(name = "result", type = Object.class, desc = "")
	public static Object fnGetIIDListForMigration(String fabricExecutionId, Integer entitiesArrarySize) throws Exception {
		ArrayList<String> result = new ArrayList<String>();
		Map <String, Map> Map_Outer = new LinkedHashMap<String, Map>();
		int totNumOfCopiedEntities= 0;
		int totNumOfFailedEntities= 0;
		String limiStr = "";
		// Taha - 10-Sep-19 - TDM v6.0 - Check if the given entitiesArrarySize is null, if so it should be ignored and all records returned
		if(entitiesArrarySize == null || entitiesArrarySize == 0)
		{
			entitiesArrarySize = -1;
		} else {
			limiStr = " limit " + entitiesArrarySize;
		}
		
		List <Object> Copied_entities_list =  new ArrayList<Object>();
		List <Object> Copied_UID_list =  new ArrayList<Object>();
		
		List <Object> Failed_entities_list =  new ArrayList<Object>();
		List <Object> Failed_UID_list =  new ArrayList<Object>();
		
		//get entities with COMPLETED status
		
		Db.Rows mig_details = null;
		//Db.Rows mig_details_completed  = fabric().fetch("migrate_details '" + fabricExecutionId + "' STATUS='COMPLETED'");
		// if the cluster ID is define, add it to the name of the keyspace.
		String clusterID = "" + ludb().fetch("clusterid").firstValue();
		
		if (clusterID == null || clusterID.isEmpty()) {
			String getMigDetSql = "select entityid, status from k2batchprocess.batchprocess_entities_info where bid = ? " + limiStr + " ALLOW FILTERING";
			mig_details = db("DB_CASSANDRA").fetch(getMigDetSql, fabricExecutionId);
		} else {
			String getMigDetSql = "select entityid, status from k2batchprocess_" + clusterID + ".batchprocess_entities_info where bid = ? " + limiStr + " ALLOW FILTERING";
			mig_details = db("DB_CASSANDRA").fetch(getMigDetSql, fabricExecutionId);
		}
			
		for (Db.Row row:mig_details) {
			Map <Object, Object> innerCopiedEntitiesMap = new HashMap <Object, Object>();
			Map <Object, Object> innerCopiedUIDMap = new HashMap <Object, Object>();
			Map <Object, Object> innerFailedEntitiesMap = new HashMap <Object, Object>();
			Map <Object, Object> innerFailedUIDMap = new HashMap <Object, Object>();
			
			// TDM 5.1- fix the split of the iid to get the entityId and support configurable separators for entityId
			String IID = "" + row.get("entityid");
			String UID[] = IID.split("#params#");
			//log.info("fnGetIIDListForMigration - UID[0]: " + UID[0]);
			Object[] split_iid = fnSplitUID(UID[0]);
			String entityId = split_iid[0].toString();
			//log.info("fnGetIIDListForMigration - entityId: " + entityId);
			
			String status = "" + row.get("status");
			
			if ("COMPLETED".equals(status)) {//Get copied entities ( statuses ADDED/UNCHANGED/UPDATED are considered copied successfully
				totNumOfCopiedEntities++;
				if(entitiesArrarySize == -1 || totNumOfCopiedEntities <= entitiesArrarySize)
				{
					innerCopiedEntitiesMap.put(entityId, entityId);
					innerCopiedUIDMap.put(entityId, row.get("entityid"));
					Copied_entities_list.add((Object)innerCopiedEntitiesMap);
					Copied_UID_list.add((Object)innerCopiedUIDMap);
				}
			} else if ("FAILED".equals(status)) {// Get failed entities
				totNumOfFailedEntities++;
				if(entitiesArrarySize == -1 || totNumOfFailedEntities <= entitiesArrarySize)
				{
					innerFailedEntitiesMap.put(entityId, entityId);
					innerFailedUIDMap.put(entityId, row.get("entityid"));
					Failed_entities_list.add((Object)innerFailedEntitiesMap);
					Failed_UID_list.add((Object)innerFailedUIDMap);
				}
			}
		}
		
		if (mig_details != null) {
			mig_details.close();
		}
		
		//add copied entities results to Map_Outer
		LinkedHashMap<String,Object> m1 = new LinkedHashMap<String,Object>();
		//log.info("fnGetIIDListForMigration - Num of Copied Entities: " + totNumOfCopiedEntities);
		m1.put("numOfEntities",totNumOfCopiedEntities);
		m1.put("entitiesList",Copied_entities_list);
		m1.put("UIDList", Copied_UID_list);
		Map_Outer.put("Copied entities per execution",m1);
		
		//add failed entities results to Map_Outer
		LinkedHashMap<String,Object> m2 = new LinkedHashMap<String,Object>();
		//log.info("fnGetIIDListForMigration - Num of Failed Entities: " + totNumOfFailedEntities);
		m2.put("numOfEntities",totNumOfFailedEntities);
		m2.put("entitiesList",Failed_entities_list);
		m2.put("UIDList",Failed_UID_list);
		Map_Outer.put("Failed entities per execution",m2);
		
		return Map_Outer;
	}


	@out(name = "refDetailedStats", type = Object.class, desc = "")
	public static Object fnGetReferenceDetailedData(String refTaskExecutionId) throws Exception {
		//ResultSetWrapper rs =null;
		Db.Rows rows = null;
		
		// Calculate the estimated remaining time for running tasks using the following formula:
		// ((Current time (UTC) – start_time (UTC) )/ number_of_processed_records) * (number_of_records_to_process- number_of_processed_records)
				
		String selectDetailedRefTablesStats = "SELECT rt.lu_name, es.ref_table_name, es.execution_status, es.start_time, es.end_time, " +
			"CASE WHEN execution_status = 'running' and number_of_processed_records > 0 THEN " +
			"to_char(((CURRENT_TIMESTAMP AT TIME ZONE 'UTC' - start_time )/number_of_processed_records) * " +
			"(number_of_records_to_process - number_of_processed_records), 'HH24:MI:SS') " + 
			"ELSE '0' END estimated_remaining_duration, number_of_records_to_process, " +
			"coalesce(number_of_processed_records, 0) as number_of_processed_records, coalesce(error_msg, '') as error_msg " +
			"FROM TASK_REF_EXE_STATS es, task_ref_tables rt where task_execution_id = ? and es.task_id = rt.task_id " +
			"and es.task_ref_table_id = rt.task_ref_table_id"; 
			
			//rs = DBQuery("TDM", selectDetailedRefTablesStats, new Object[]{refTaskExecutionId});
			rows = db("TDM").fetch(selectDetailedRefTablesStats, refTaskExecutionId);
		
		return rows;
	}

	@out(name = "res", type = Object[].class, desc = "")
	public static Object[] fnGetIIdSeparatorsFromTDM() throws Exception {
		String iidOpenSeparator = "";
		String iidCloseSeparator = "";
		
		
		// TDM 5.1- get open and close separators for the instanceId. If they exist- get the instanceId according the open and close separators
		//Set the SQL parameter
		
		String sql = "SELECT UPPER(param_name) as param_name, param_value FROM public.tdm_general_parameters where UPPER(param_name) in ('IID_OPEN_SEPARATOR', 'IID_CLOSE_SEPARATOR')";
		Db.Rows rows = db("TDM").fetch(sql);
		for (Db.Row row:rows){
			if(row.cells()[0].toString().equals("IID_OPEN_SEPARATOR")&& row.cells()[1]!= null && !row.cells()[1].toString().isEmpty() )
			{
				iidOpenSeparator= row.cells()[1].toString();
			}
			else if (row.cells()[0].toString().equals("IID_CLOSE_SEPARATOR") && row.cells()[1]!= null && !row.cells()[1].toString().isEmpty() )
			{
				iidCloseSeparator = row.cells()[1].toString(); 
			}
		}
		
		if(rows != null) {
			rows.close();
		}
		return new Object[]{iidOpenSeparator, iidCloseSeparator};
	}

	@out(name = "instanceID", type = String.class, desc = "")
	@out(name = "envName", type = String.class, desc = "")
	@out(name = "versionName", type = String.class, desc = "")
	@out(name = "versiionDateTime", type = String.class, desc = "")
	public static Object fnSplitUID2(String uid) throws Exception {
		// TDM 5.1- fix the function to support also dataflux mode when the instance id also has version name  +datetime
		// In addition- remove the open and close separators
		String instanceId = "";
		String envName = "";
		
		String separator = "";
		String iidOpenSeparator = "";
		String iidCloseSeparator = "";
		// TDM 6.0 - The version name (task name) and version datetime should be returned
		String versionName = "";
		String versionDateTime = "";
		
		String iidSeparator = "" + db("TDM").fetch("Select param_value from tdm_general_parameters where param_name = 'iid_separator'").firstValue();
        //separator = !Util.isEmpty(iidSeparator) ? iidSeparator : "_";
		if ( !Util.isEmpty(iidSeparator) && !"null".equals(iidSeparator) ) {
			separator = iidSeparator;
		} else {
			separator = "_";
		}
		// TDM 5.1- get open and close separators for the instanceId. If they exist- get the instanceId according the open and close separators
		//Set the SQL parameter
		
		Object[] separators = fnGetIIdSeparatorsFromTDM();
		//log.info("fnSplitUID - Input UID: " + uid);
		//log.info("fnSplitUID - separators[0]: " + separators[0] + ", separators[1]: " + separators[1]);
		if(separators[0] != null && separators[0].toString() != null && !separators[0].toString().isEmpty() )
			iidOpenSeparator = separators[0].toString();
		
		if(separators[1] != null && separators[1].toString() != null && !separators[1].toString().isEmpty())
			iidCloseSeparator = separators[1].toString();
		
		if (uid.startsWith("_dev")) {
			uid = uid.replaceFirst("_dev_", "dev_");
			envName = "_dev";
		}
			
		try {
		 	
				String[] split_uid = uid.split(separator);
			
				if(envName.equals(""))
		        	envName = split_uid[0].toString();
			
				// Check if the open and close separators are populated
				if(iidOpenSeparator.equals("") || uid.indexOf(iidOpenSeparator)== -1 || iidCloseSeparator.equals("") || uid.indexOf(iidCloseSeparator)== -1) // the separators for the IID are not defined in the TDM DB or in the input UID
				{
					//log.info("fnSplitUID - No Separators");
					instanceId = split_uid[1].toString();
					
					//TDM 6.0, get the version name and datetime
					if (split_uid.length==4) {  //entity in the format of <environment>_<entity_id>_<task_name>_<timestamp>
						versionName = split_uid[2].toString();
						versionDateTime = split_uid[3].toString();
					}
				}
				else // the open and close separators are populated. Both of them have to be poplated. You cannot populate just one of them
				{
					//log.info("fnSplitUID - There are separators");
					Object[] IID_info = fnRemoveSeparatorsFromIID(uid,  iidOpenSeparator, iidCloseSeparator);     
					instanceId = IID_info[0].toString();
					//TDM 6.0, get the version name and datetime
					int pos = Integer.valueOf("" + IID_info[1]);
					// if the uid is longer than the end position of the instance id including the close separator
					if (uid.length() > pos) {
						// Add 1 to jump to the beginning of the task name
						String[] split_version = (uid.substring(pos + 1)).split(separator);
						versionName = split_version[0];
						versionDateTime = split_version[1];
					}
				}
			        
		} catch (Exception e) {
		    if (e.getMessage().toString().contains("String index out of range"))
		        throw new Exception("Environment Name Is Missing, String index out of range");
		    else
		        throw new Exception(e.getMessage());
		}
		finally
		{
			// If the input uid does not have _ separator
		
			if(uid.indexOf(separator) == -1)
				 throw new Exception("Environment Name Is Missing, Underscore not found");
		
		}
		
		//log.info("fnSplitUID - Output: instanceID: " + instanceId + ", envName: " + envName + ", versionName: " + versionName + "versionDateTime: " + versionDateTime);
		return new Object[]{instanceId, envName, versionName, versionDateTime};
	}

	@out(name = "luType", type = String.class, desc = "")
	public static String fnGetLuType() throws Exception {
		return getLuType().luName;
	}

	@type(DecisionFunction)
	@out(name = "decision", type = Boolean.class, desc = "")
	public static Boolean fnDecisionInsertToTarget() throws Exception {
		String luName = getLuType().luName;
		if(("" + ludb().fetch("SET " + luName + ".TDM_INSERT_TO_TARGET").firstValue()).equals("true"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@type(DecisionFunction)
	@out(name = "decision", type = Boolean.class, desc = "")
	public static Boolean fnDecisionDeleteFromTarget() throws Exception {
		String luName = getLuType().luName;
		if(("" + ludb().fetch("SET " + luName + ".TDM_DELETE_BEFORE_LOAD").firstValue()).equals("true"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@out(name = "firstSyncInd", type = Boolean.class, desc = "")
	public static Boolean fnIsFirstSync() throws Exception {
		return isFirstSync();
	}

	@out(name = "refTableName", type = String.class, desc = "")
	@out(name = "schemaName", type = String.class, desc = "")
	@out(name = "interfaceName", type = String.class, desc = "")
	@out(name = "targetSchemaName", type = String.class, desc = "")
	@out(name = "targetInterfaceName", type = String.class, desc = "")
	public static Object fnGetRefTableData(String luName, String refTableName) throws Exception {
		Map<String,Map<String, String>> trnRefListValues = getTranslationsData("trnRefList");
		
		String trnLuName = "";
		String trnTableName = "";
		String schemaName = "";
		String interfaceName = "";
		String targetRefTableName = "";
		String targetSchemaName = "";
		String targetInterfaceName = "";
		String targetTablePK = "";
		//log.info("fnGetRefTableData - luName: " + luName + ", refTableName: " + refTableName);
		
		for(String index: trnRefListValues.keySet()){
			Map<String, String> valMap = trnRefListValues.get(index);
			
			trnLuName = index.substring(0, index.indexOf("@") );
			trnTableName = valMap.get("reference_table_name");
			//log.info("fnGetRefTableData - trnLuName: " + trnLuName + ", trnTableName: " + trnTableName);
			if (trnTableName.equalsIgnoreCase(refTableName)  && trnLuName.equalsIgnoreCase(luName)) {
				//log.info("fnGetRefTableData - Found table in translation");
				schemaName = valMap.get("schema_name");
				interfaceName = valMap.get("interface_name");
				if (valMap.get("target_ref_table_name") != null && !Util.isEmpty(valMap.get("target_ref_table_name"))) {
					targetRefTableName =  valMap.get("target_ref_table_name");
				} else {
					targetRefTableName = trnTableName;
				}
				targetSchemaName = valMap.get("target_schema_name");
				targetInterfaceName = valMap.get("target_interface_name");
				targetTablePK = valMap.get("table_pk_list");
				
				break;
			}
				
		}
		//log.info("fnGetRefTableData - Returning: targetRefTableName: " + targetRefTableName + ", schemaName: " + schemaName + ", interfaceName: " + interfaceName +
		//		", targetSchemaName: " + targetSchemaName + ", targetInterfaceName: " + targetInterfaceName);
		return new Object[]{targetRefTableName, schemaName, interfaceName, targetSchemaName, targetInterfaceName, targetTablePK};
	}
	
	@desc("Dummy Root function")
	@type(RootFunction)
	@out(name = "dummy_output", type = void.class, desc = "")
	public static void fnDummyPop(String dummy_input) throws Exception {
		if(1 == 2)yield(new Object[] {null});
	}


	@out(name = "globalValue", type = String.class, desc = "")
	public static String getGlobal(String globalName) throws Exception {
		return "" + fabric().fetch("set " + globalName).firstValue();
	}

	public static void fnCheckInsFound() throws Exception {
		// Fix- TDM 7.0.1 - Check the main source LU tables only if the TDM_INSERT_TO_TARGET is true
		String luName = getLuType().luName;

		if (("" + ludb().fetch("SET " + luName + ".TDM_INSERT_TO_TARGET").firstValue()).equals("true")) {

			// Get the list of root tables from the Global
			String[] rootTables = ("" + ludb().fetch("SET " + luName + ".ROOT_TABLE_NAME").firstValue()).split(",");

			// Indicates if any of the root tables have values in it
			boolean instanceExists = false;

			// For every possible root table
			for (String rootTable : rootTables) {
				// If that table has data
				if (!ludb().fetch(String.format("SELECT 1 FROM %s LIMIT 1", rootTable.trim())).firstRow().isEmpty())
					// Indicate the LU is found
					instanceExists = true;
			}


			if (!instanceExists) {
				LogEntry lg = new LogEntry("INSTANCE NOT FOUND!", MsgId.INSTANCE_MISSING);
				lg.luInstance = SharedLogic.fnValidateNdGetInstance()[0] + "";
				lg.luType = getLuType().luName;
				throw new InstanceNotFoundException(lg, null);
			}
		}
	}

	public static void setGlobals(String globals) throws Exception {
		if (!Util.isEmpty(globals)) {
			// Replace gson with k2view's Json
			//Gson gson = new Gson();
			//Map statusData = gson.fromJson(globals, Map.class);
			Map statusData = Json.get().fromJson(globals, Map.class);
			if (!(statusData.isEmpty())) {
				statusData.forEach((key, value) -> {
					try {
						//log.info("setGlobals - setting "+key+"='"+value+ "'");
						fabric().execute("set " + key + "='" + value + "'");
						// TDM 7.1 - Handle Masking and Sequence Broadway Actors flags
						TdmSharedUtils.setBroadwayActorFlags("" + key, "" + value);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				});
			}
		}
	}

		@out(name = "result", type = Integer.class, desc = "")
	public static Integer getRetention(String retentionPeriodType, Float retentionPeriodValue) throws Exception {
		Integer retention_in_seconds = 0;
		
		switch (retentionPeriodType) {
		    case "Minutes":
		        retention_in_seconds = Math.round(retentionPeriodValue * 60);
		        break;
		    case "Hours":
		        retention_in_seconds = Math.round(retentionPeriodValue * 60 * 60);
		        break;
		    case "Days":
		        retention_in_seconds = Math.round(retentionPeriodValue * 60 * 60 * 24);
		        break;
		    case "Weeks":
		        retention_in_seconds = Math.round(retentionPeriodValue * 60 * 60 * 24 * 7);
		        break;
		    case "Years":
		        retention_in_seconds = Math.round(retentionPeriodValue * 60 * 60 * 24 * 365);
		        break;
		}
		return retention_in_seconds;
	}




	@out(name = "result", type = HashMap.class, desc = "")
	public static HashMap<String,Object> fnReleaseReservedEntity(String entityID, String envID, String beID, String userName) throws Exception {
		HashMap<String, Object> result = new HashMap<>();
		String ErrorCode = "SUCCESS";
		String ErrorMessage = "";
		
		log.info("Start fnReleaseReservedEntity for user: " + userName);
		if (userName == null || "".equals(userName)) {
			userName = sessionUser().name();
		}
		String permissionGroup = fnGetUserPermissionGroup(userName);
		
		String deleteSql = "DELETE FROM TDM_RESERVED_ENTITIES WHERE entity_id=? AND be_id =? AND env_id =? ";
		
		//Sort input list based on Environment
		//log.info("listOfEntities: " + listOfEntities);
		
		Boolean isOwner = false;
		Boolean isTester = false;
		
		String returnClause = " returning entity_id";
		//log.info("permissionGroup: " + permissionGroup);
		
		//New Environment ID, check if the user is allowed to update it
		if ("owner".equalsIgnoreCase(permissionGroup)) {
			//log.info("The user is an owner");
			//Check if the user is an owner of the environment, if not treat the user as tester
			if(TdmSharedUtils.fnIsOwner(envID)) {
				isOwner = true;
			} else {
				isOwner = false;
			}
		}
		//If the user is not an admin or owner, then add a condition 
		//to check if the user is the owner of the reservation
		if (!isOwner && !"admin".equalsIgnoreCase(permissionGroup)) {
			//log.info("The user is a tester");
			deleteSql += " AND reserve_owner =? ";
			 isTester = true;
		} else {
			isTester = false;
		}
			
		//log.info("fnReleaseReservedEntity - deleteSql: " + deleteSql + returnClause);
		//Delete record
		String deleteEntityID = "";
		if (isTester) {
			deleteEntityID = "" + db("TDM").fetch(deleteSql + returnClause, entityID, beID, envID, userName).firstValue();
		} else {
			deleteEntityID = "" + db("TDM").fetch(deleteSql + returnClause, entityID, beID, envID).firstValue();
		}
		
		//if record was not deleted
		if(!entityID.equals(deleteEntityID)) {
			//log.info("fnReleaseReservedEntity - entity is not deleted");
			//In case of a tester, check if the entity is reserved for a different user
			String reserveOwner = "" + db("TDM").fetch(
				"SELECT reserve_owner FROM TDM_RESERVED_ENTITIES WHERE entity_id=? AND be_id =? AND env_id =?",
				entityID, beID, envID).firstValue();
			if (reserveOwner == null || "".equals(reserveOwner) ||"null".equals(reserveOwner) ) {
				ErrorMessage = "Entity already Released";
				//log.info("fnReleaseReservedEntity - Entity already Released");
				ErrorCode = "Warning";
			} else if (!reserveOwner.equals(userName)) {
				ErrorMessage = "Entity is reserved to user: " + reserveOwner;
				//log.info("fnReleaseReservedEntity - Entity is reserved to user: " + reserveOwner);
				ErrorCode = "ERROR";
			}
		}
		result.put("id", entityID);
		result.put("ErrorCode", ErrorCode);
		result.put("ErrorMessage", ErrorMessage);
		
		return result;
	}

	@out(name = "result", type = ArrayList.class, desc = "")
	public static ArrayList fnGetRefrenceTables() throws Exception {
		ArrayList trnTableName = new ArrayList<>();
		String field = "";
		Map<String,Map<String, String>> trnRefListValues = getTranslationsData("trnRefList");
		for(String index: trnRefListValues.keySet()){
			Map<String, String> valMap = trnRefListValues.get(index);
			field = valMap.get("reference_table_name");
			trnTableName.add(field);
		}
		return trnTableName;
	}

}
