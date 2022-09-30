/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.TDM_ReserveEntities;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.k2_ws.*;
import com.k2view.fabric.api.endpoint.Endpoint.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.wrapWebServiceResults;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.fnIsOwner;
import static com.k2view.cdbms.usercode.common.TDM.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.k2_ws.TDM_Tasks.TaskExecutionUtils.fnInsertActivity;
import static com.k2view.cdbms.usercode.lu.k2_ws.TDM_Tasks.TaskExecutionUtils.fnValidateReservedEntities;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.fnGetUserEnvs;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.fnGetUserPermissionGroup;


@SuppressWarnings({"unused", "DefaultAnnotationParam", "unchecked"})
public class Logic extends WebServiceUserCode {

	@desc("Get all reserved entities related to the user's envrironments.")
	@webService(path = "getReservedEntities", verb = {MethodType.GET}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON}, elevatedPermission = true)
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": [\r\n" +
			"    {\r\n" +
			"      \"allow_edit\": false,\r\n" +
			"      \"target_entity_id\": \"1\",\r\n" +
			"      \"reserve_notes\": null,\r\n" +
			"      \"task_execution_id\": 107,\r\n" +
			"      \"expiration_date\": \"2022-02-28 09:57:45.684\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"reserve_date\": \"2022-02-16 12:08:33.879\",\r\n" +
			"      \"task_title\": \"load1\",\r\n" +
			"      \"reserve_consumers\": null,\r\n" +
			"      \"reserve_owner\": \"tali\",\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"reserve_tags\": null\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"allow_edit\": false,\r\n" +
			"      \"target_entity_id\": \"2\",\r\n" +
			"      \"reserve_notes\": null,\r\n" +
			"      \"task_execution_id\": 107,\r\n" +
			"      \"expiration_date\": \"2022-02-28 09:57:45.721\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"reserve_date\": \"2022-02-13 12:08:33.866\",\r\n" +
			"      \"task_title\": \"load1\",\r\n" +
			"      \"reserve_consumers\": null,\r\n" +
			"      \"reserve_owner\": \"tali\",\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"reserve_tags\": null\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"allow_edit\": true,\r\n" +
			"      \"target_entity_id\": \"190\",\r\n" +
			"      \"reserve_notes\": null,\r\n" +
			"      \"task_execution_id\": 178,\r\n" +
			"      \"expiration_date\": \"2022-02-28 09:57:45.620\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"reserve_date\": \"2022-02-17 16:38:30.876\",\r\n" +
			"      \"task_title\": \"load+reserve\",\r\n" +
			"      \"reserve_consumers\": null,\r\n" +
			"      \"reserve_owner\": \"taha\",\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"reserve_tags\": null\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"allow_edit\": true,\r\n" +
			"      \"target_entity_id\": \"121\",\r\n" +
			"      \"reserve_notes\": null,\r\n" +
			"      \"task_execution_id\": 178,\r\n" +
			"      \"expiration_date\": \"2022-02-28 09:57:45.646\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"reserve_date\": \"2022-02-17 16:38:30.894\",\r\n" +
			"      \"task_title\": \"load+reserve\",\r\n" +
			"      \"reserve_consumers\": null,\r\n" +
			"      \"reserve_owner\": \"taha\",\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"reserve_tags\": null\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"allow_edit\": true,\r\n" +
			"      \"target_entity_id\": \"191\",\r\n" +
			"      \"reserve_notes\": null,\r\n" +
			"      \"task_execution_id\": 178,\r\n" +
			"      \"expiration_date\": \"2022-02-28 09:57:45.578\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"reserve_date\": \"2022-02-17 16:38:30.956\",\r\n" +
			"      \"task_title\": \"load+reserve\",\r\n" +
			"      \"reserve_consumers\": null,\r\n" +
			"      \"reserve_owner\": \"taha\",\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"reserve_tags\": null\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"allow_edit\": true,\r\n" +
			"      \"target_entity_id\": \"195\",\r\n" +
			"      \"reserve_notes\": null,\r\n" +
			"      \"task_execution_id\": 204,\r\n" +
			"      \"expiration_date\": \"2022-02-28 13:12:35.977\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"reserve_date\": \"2022-02-20 13:12:35.981\",\r\n" +
			"      \"task_title\": \"load+reserve\",\r\n" +
			"      \"reserve_consumers\": null,\r\n" +
			"      \"reserve_owner\": \"taha\",\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"reserve_tags\": null\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"allow_edit\": false,\r\n" +
			"      \"target_entity_id\": \"1190\",\r\n" +
			"      \"reserve_notes\": null,\r\n" +
			"      \"task_execution_id\": 247,\r\n" +
			"      \"expiration_date\": \"2022-02-28 09:49:44.302\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"reserve_date\": \"2022-02-23 09:49:44.302\",\r\n" +
			"      \"task_title\": \"reserve3\",\r\n" +
			"      \"reserve_consumers\": null,\r\n" +
			"      \"reserve_owner\": \"admin\",\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"reserve_tags\": null\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"allow_edit\": false,\r\n" +
			"      \"target_entity_id\": \"1191\",\r\n" +
			"      \"reserve_notes\": null,\r\n" +
			"      \"task_execution_id\": 248,\r\n" +
			"      \"expiration_date\": \"2022-04-04 09:53:17.035\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"reserve_date\": \"2022-02-23 09:53:17.035\",\r\n" +
			"      \"task_title\": \"reserve3\",\r\n" +
			"      \"reserve_consumers\": null,\r\n" +
			"      \"reserve_owner\": \"admin\",\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"reserve_tags\": null\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"allow_edit\": true,\r\n" +
			"      \"target_entity_id\": \"1192\",\r\n" +
			"      \"reserve_notes\": null,\r\n" +
			"      \"task_execution_id\": 254,\r\n" +
			"      \"expiration_date\": \"2022-02-28 13:13:05.039\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"reserve_date\": \"2022-02-23 13:13:05.056\",\r\n" +
			"      \"task_title\": \"reserve4\",\r\n" +
			"      \"reserve_consumers\": null,\r\n" +
			"      \"reserve_owner\": \"taha\",\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"reserve_tags\": null\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"allow_edit\": true,\r\n" +
			"      \"target_entity_id\": \"1193\",\r\n" +
			"      \"reserve_notes\": null,\r\n" +
			"      \"task_execution_id\": 255,\r\n" +
			"      \"expiration_date\": \"2022-02-28 14:25:11.480\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"reserve_date\": \"2022-02-23 14:25:11.505\",\r\n" +
			"      \"task_title\": \"reserve4\",\r\n" +
			"      \"reserve_consumers\": null,\r\n" +
			"      \"reserve_owner\": \"taha\",\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"reserve_tags\": null\r\n" +
			"    }\r\n" +
			"  ],\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsGetReservedEntities(String entityId) throws Exception {
		boolean adminInd = false;
		boolean envOwnerFound = false;
		boolean envTesterFound = false;
		String permissionGroup = fnGetUserPermissionGroup("");
		String sqlAllowEdit = "";
		String sqlOwnerEnvList = "";
		String sqlTesterEnvList = "";
		
		String sql = "SELECT e.environment_name, t.task_title, re.task_execution_id, be.be_name, " +
					"re.entity_id as target_entity_id, re.reserve_owner, re.start_datetime as reserve_date, " +
					"re.end_datetime as expiration_date,  reserve_owner, reserve_consumers, reserve_notes, reserve_tags, ";
		 
		String sqlFromWhere = "FROM TDM_RESERVED_ENTITIES re, TASKS t, ENVIRONMENTS e, BUSINESS_ENTITIES be " +
					"WHERE re.task_id = t.task_id AND t.environment_id = e.environment_id AND e.environment_status = 'Active' " +
					"AND t.be_id = be.be_id and (re.end_datetime is null or re.end_datetime >= timezone('UTC', now())) ";
		
		if(entityId != null && !"".equals(entityId)) {
			sqlFromWhere += " and entity_id = '" + entityId + "' ";
		}
		List<String> testerEnvs = new ArrayList<>();
		List<String> ownerEnvs = new ArrayList<>();
		
		if (!"admin".equalsIgnoreCase(permissionGroup)) {
		
			List<Map<String, Object>> allEnvsList = fnGetUserEnvs();
			List<Map<String, Object>> allTargetEnvs;
			//log.info("------- allEnvsList Size: " + allEnvsList.size());
			if("owner".equalsIgnoreCase(permissionGroup)) {
				//log.info("wsGetReservedEntities - OWNER");
				for (Map<String, Object> envsGroup : allEnvsList) {
					if (envsGroup.get("target environments") == null) {
						continue;
					}
					allTargetEnvs = (List<Map<String, Object>>) (envsGroup.get("target environments"));
					if (allTargetEnvs == null || allTargetEnvs.isEmpty()) {
						log.error("No Target Env is found");
						return wrapWebServiceResults("FAILED", "The list of Target Environments is empty for owner", null);
					} else {
						for (Map<String, Object> env : allTargetEnvs) {
							if ("user".equals(env.get("assignment_type").toString())) {
								testerEnvs.add(env.get("environment_id").toString());
							} else {
								ownerEnvs.add(env.get("environment_id").toString());
							}
						}
					}
				}
			} else {
				//log.info("wsGetReservedEntities - TESTER");
				for (Map<String, Object> envsGroup : allEnvsList) {
					//log.info("wsGetReservedEntities - looping on Envs");
					//log.info("wsGetReservedEntities -envsGroup: " + envsGroup);
					if (envsGroup.get("target environments") == null) {
						continue;
					}
					allTargetEnvs = (List<Map<String, Object>>) (envsGroup.get("target environments"));
		
					if (allTargetEnvs == null || allTargetEnvs.isEmpty()) {
						log.error("No Target Env is found");
						return wrapWebServiceResults("FAILED", "The list of Target Environments is empty for User", null);
					} else {
						for (Map<String, Object> env : allTargetEnvs) {
							//log.info("wsGetReservedEntities - assignment_type: " + env.get("assignment_type"));
							if ("user".equals(env.get("assignment_type").toString())) {
								testerEnvs.add(env.get("environment_id").toString());
							}
						}
					}
				}
			}
		
		} else {// In case of admin, the user is allowed to edit every reservation
			sqlAllowEdit = " true as allow_edit ";
			adminInd = true;
		}
		
		if (ownerEnvs.size() > 0) {
			envOwnerFound = true;
			sqlOwnerEnvList = " AND t.environment_id in (" + String.join(",", ownerEnvs) + ")";
			sqlAllowEdit = " true as allow_edit ";
		} 
		
		if (testerEnvs.size() > 0) {
			testerEnvs.removeAll(ownerEnvs);
			envTesterFound = true;
			sqlTesterEnvList = " AND t.environment_id in (" + String.join(",", testerEnvs) + ")";
			String userName = sessionUser().name();
			sqlAllowEdit = "CASE WHEN re.reserve_owner = '" + userName + "' THEN true ELSE FALSE END AS allow_edit ";
		} 
		
		String limit = " order by task_execution_id desc ";
		if (!"0".equals(GET_RESERVED_ENTITIES_LIMIT)) {
			limit += " LIMIT " + GET_RESERVED_ENTITIES_LIMIT;
		}
		
		Db.Rows reservedList = null;
		if (adminInd) {
			sql += sqlAllowEdit + sqlFromWhere + limit;
			reservedList =  db("TDM").fetch(sql);
		} else {
			if (envOwnerFound) { 
				sql += sqlAllowEdit + sqlFromWhere + sqlOwnerEnvList + limit;
				reservedList =  db("TDM").fetch(sql);
			} else{
				if(envTesterFound) {
					sql +=  sqlAllowEdit + sqlFromWhere + sqlTesterEnvList + limit;
					reservedList =  db("TDM").fetch(sql);
				}
			}
		}
		
		//log.info("wsGetReservedEntities - sql: " + sql);
		ArrayList<Map> rows = new ArrayList<>();
		if (adminInd || envOwnerFound || envTesterFound) {
			//log.info("wsGetReservedEntities - before looping over records");
			// convert iterable to serializable object
			if (reservedList.resultSet().isBeforeFirst()) {
				//log.info("wsGetReservedEntities - there are inputs");
				reservedList.forEach(row -> {
					HashMap copy = new HashMap();
					copy.putAll(row);
					rows.add(copy);
				});
			}
			return wrapWebServiceResults("SUCCESS", null,rows);
		} else {
			return wrapWebServiceResults("FAILED", "No Target Environment found for User", null);
		}
	}


	@desc("Release the input entity list. The release activity deletes the entities from the TDM reservation table (TDM_RESERVED_ENTITIES).\r\n" +
			"\r\n" +
			"Each entity must have the following parameters:\r\n" +
			"- \"environment_name\": the name of the environment where the entity is reserved,\r\n" +
			"- \"be_name\": the name of the Business Entity of the entity ID,\r\n" +
			"- \"target_entity_id\": the entity ID.\r\n" +
			"\r\n" +
			"Request BODY example: \r\n" +
			" \r\n" +
			"{\r\n" +
			"  \"listOfEntities\": [\r\n" +
			"    {\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"be_name\":\"Customer\",\r\n" +
			"      \"target_entity_id\":\"4984\"\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"be_name\":\"Customer\",\r\n" +
			"      \"target_entity_id\":\"480\"\r\n" +
			"    }\r\n" +
			"  ]\r\n" +
			"}")
	@webService(path = "release/listOfEntities", verb = {MethodType.POST}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON}, elevatedPermission = true)
	public static Object wsReleaseReservedEntities(@param(required=true) List<Map<String,String>> listOfEntities) throws Exception {
		HashMap<String, Object> response = new HashMap<>();
		List<HashMap<String, Object>> result = new ArrayList<>();
		String errorCode =  "SUCCESS";
		String message = null;
		String listOfFailed = "";
		String userId = sessionUser().name();
		String permissionGroup = fnGetUserPermissionGroup("");
		
		for (Map<String, String> entityInfo : listOfEntities) {
			String envID = "" + db("TDM").fetch("SELECT environment_id FROM environments " +
					"WHERE environment_name =? AND environment_status = 'Active'", entityInfo.get("environment_name")).firstValue();
			String beID = "" + db("TDM").fetch("SELECT be_id FROM business_entities " +
					"WHERE be_name =? AND be_status = 'Active'", entityInfo.get("be_name")).firstValue();
		
			HashMap<String, Object> map = fnReleaseReservedEntity("" + entityInfo.get("target_entity_id"), envID, beID, "");
			String returnedCode = "" + map.get("ErrorCode");
			if (!"SUCCESS".equals(returnedCode)) {
				result.add(map);
				if ("".equals(listOfFailed)) {
					listOfFailed = "" + map.get("id");
				} else {
					listOfFailed += ", " + map.get("id");
				}
			}
		}
		
		if (result.size() >= listOfEntities.size()) {
			errorCode = "FAILED";
			message = "The release of all entities had failed";
		} else {
					String activityDesc = "Reserved Entities were released";
					try {
						fnInsertActivity("release", "Reserved Entities", activityDesc);
					}
					catch(Exception e){
						log.error(e.getMessage());
					}
			if (result.size() > 0) {
				message = "The release of the following entities had failed: " + listOfFailed;
			}
		}
			
		
		response.put("result", result);
		response.put("errorCode", errorCode);
		response.put("message", message);
		
		return response;
	}




	@desc("Add a note to the input list of reserved entities.\r\n" +
			"Each entity must have the following parameters:\r\n" +
			"- \"environment_name\": the name of the environment where the entity is reserved,\r\n" +
			"- \"be_name\": the name of the Business Entity of the entity ID,\r\n" +
			"- \"target_entity_id\": the entity ID.\r\n" +
			"\r\n" +
			"Request BODY example: \r\n" +
			" \r\n" +
			"{\r\n" +
			"  \"newNote\": \"Testing the entity reservation!\",\r\n" +
			"  \"listOfEntities\": [\r\n" +
			"    {\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"be_name\":\"Customer\",\r\n" +
			"      \"target_entity_id\":\"4984\"\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"be_name\":\"Customer\",\r\n" +
			"      \"target_entity_id\":\"480\"\r\n" +
			"    }\r\n" +
			"  ]\r\n" +
			"}")
	@webService(path = "addNote", verb = {MethodType.POST}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON}, elevatedPermission = true)
	public static Object wsAddNoteToReservedEntities(@param(required=true) String newNote, @param(required=true) List<Map<String,String>> listOfEntities) throws Exception {
		//log.info("Starting wsAddNoteToReservedEntities");
		HashMap<String, Object> response = new HashMap<>();
		List<HashMap<String, Object>> result = new ArrayList<>();
		String errorCode =  "SUCCESS";
		String message = null;
		String userId = sessionUser().name();
		String permissionGroup = fnGetUserPermissionGroup("");
		
		String origUpdateSql = "UPDATE TDM_RESERVED_ENTITIES SET reserve_notes=? WHERE entity_id=? AND be_id =? AND env_id =? ";
		
		//Sort input list based on Environment
		Collections.sort(listOfEntities, new Comparator<Map<String, String>>() {
			public int compare(final Map<String, String> o1, final Map<String, String> o2) {
				if (o1.get("environment_name").compareTo(o2.get("environment_name")) == 0) {
					return o1.get("be_name").compareTo(o2.get("be_name"));
				} else {
					return o1.get("environment_name").compareTo(o2.get("environment_name"));
				}
			}
		});
		
				String currentEnvID = "-1";
		Boolean isOwner = false;
		Boolean isTester = false;
		String updateSql = origUpdateSql;
		String returnClause = " returning entity_id";
		
		for (Map<String, String> entityInfo : listOfEntities) {
			updateSql = origUpdateSql;
			String envID = "" + db("TDM").fetch("SELECT environment_id FROM environments " +
						"WHERE environment_name =? AND environment_status = 'Active'", entityInfo.get("environment_name")).firstValue();
			String beID = "" + db("TDM").fetch("SELECT be_id FROM business_entities " +
						"WHERE be_name =? AND be_status = 'Active'", entityInfo.get("be_name")).firstValue();
			String entityID = entityInfo.get("target_entity_id");
			
			if (!currentEnvID.equals(envID)) {
				//New Environment ID, check if the user is allowed to update it
				if ("owner".equalsIgnoreCase(permissionGroup)) {
					//Check if the user is an owner of the environment, if not treat the user as tester
					if(fnIsOwner(envID)) {
						isOwner = true;
					} else {
						isOwner = false;
					}
				}
				//If the user is not an admin or owner, then add a condition 
				//to check if the user is the owner of the reservation
				if (!isOwner && !"admin".equalsIgnoreCase(permissionGroup)) {
					updateSql += " AND reserve_owner =?";
					isTester = true;
				} else {
					isTester = false;
				}
			}
			
			//Update record
			String updatedEntityID = "";
			if (isTester) {
				updatedEntityID = "" + db("TDM").fetch(updateSql + returnClause, newNote, entityID, beID, envID, userId).firstValue();
			} else {
				updatedEntityID = "" + db("TDM").fetch(updateSql + returnClause, newNote, entityID, beID, envID).firstValue();
			}
			
			//if record was not updated
			if(!entityID.equals(updatedEntityID)) {
				HashMap<String, Object> map = new HashMap<>();
				map.put("id", entityID);
				result.add(map);
			}	
		}
		
		if (result.size() >= listOfEntities.size()) {
			errorCode = "FAILED";
			message = "The update to all entities had failed";
		} else if (result.size() > 0) {
			message = "Not all entities were updated";
		}
		
		response.put("result", result);
		response.put("errorCode", errorCode);
		response.put("message", message);
		
		return response;
	}


	@desc("Update the reservation period of the input entity list: update the reservation's expiration date with the input date. If the user is added to the environment as a tester,  the new end date cannot exceed the limitation of the testers reservation period defined in tdm_general_parameters TDM table. For example, if the maximum reservation period is 10 days and the entity has been reserved on 10-MAR-22, the maximum expiration date on this entity can be set to 20-MAR-22.\r\n" +
			"\r\n" +
			"Each entity must have the following parameters:\r\n" +
			"- \"environment_name\": the name of the environment where the entity is reserved,\r\n" +
			"- \"be_name\": the name of the Business Entity of the entity ID,\r\n" +
			"- \"target_entity_id\": the entity ID.\r\n" +
			"\r\n" +
			"The newEndDate must be populated with the following format: 'YYYYMMDDHHMMSS'.\r\n" +
			"\r\n" +
			"\r\n" +
			"Request BODY examples: \r\n" +
			"\r\n" +
			"{\r\n" +
			"  \"newEndDate\": \"20220210000000\",\r\n" +
			"  \"listOfEntities\": [\r\n" +
			"    {\r\n" +
			"      \"environment_name\": \"TAR\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"target_entity_id\": \"3\"\r\n" +
			"    }\r\n" +
			"  ]\r\n" +
			"}\r\n" +
			"\r\n" +
			"{\r\n" +
			"  \"newEndDate\": \"20220328235900\",\r\n" +
			"  \"listOfEntities\": [{\"environment_name\":\"TAR\",\"be_name\":\"Customer\",\"target_entity_id\":\"192\"},{\"environment_name\":\"TAR\",\"be_name\":\"Customer\",\"target_entity_id\":\"3097\"}]\r\n" +
			"}")
	@webService(path = "extend", verb = {MethodType.POST}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON}, elevatedPermission = true)
	public static Object wsExtendReservedEntities(@param(required=true) String newEndDate, @param(required=true) List<Map<String,String>> listOfEntities) throws Exception {
		HashMap<String, Object> response = new HashMap<>();
		List<HashMap<String, Object>> result = new ArrayList<>();
		String errorCode =  "SUCCESS";
		String message = "";
		String listOfFailed = "";
		String maxOverrideList = "";
		String userId = sessionUser().name();
		String permissionGroup = fnGetUserPermissionGroup("");
		
		String getMaxDaysSql = "SELECT param_value from tdm_general_parameters where param_name = 'MAX_RESERVATION_DAYS_FOR_TESTER'";
		
		String maxReserveDays = "" + db("TDM").fetch(getMaxDaysSql).firstValue();
		
		String getMaxEndDateSql = "SELECT TO_CHAR(start_datetime + INTERVAL '" + maxReserveDays + 
				" day', 'YYYYMMDDHH24MISS') as maxEndDate from TDM_RESERVED_ENTITIES WHERE entity_id=? AND be_id =? AND env_id =? ";
		
		String origUpdateSql = "UPDATE TDM_RESERVED_ENTITIES SET end_datetime=TO_TIMESTAMP(?, 'YYYYMMDDHH24MISS') WHERE entity_id=? AND be_id =? AND env_id =? ";
		
		//Sort input list based on Environment
		Collections.sort(listOfEntities, new Comparator<Map<String, String>>() {
			public int compare(final Map<String, String> o1, final Map<String, String> o2) {
				if (o1.get("environment_name").compareTo(o2.get("environment_name")) == 0) {
					return o1.get("be_name").compareTo(o2.get("be_name"));
				} else {
					return o1.get("environment_name").compareTo(o2.get("environment_name"));
				}
			}
		});
		
		String origNewEndDate = newEndDate;
		String currentEnvID = "-1";
		String updateSql = origUpdateSql;
		String returnClause = " returning entity_id";
		
		for (Map<String, String> entityInfo : listOfEntities) {
			Boolean isOwner = false;
			Boolean isTester = false;
			updateSql = origUpdateSql;
			newEndDate = origNewEndDate;
			
			String envID = "" + db("TDM").fetch("SELECT environment_id FROM environments " +
						"WHERE environment_name =? AND environment_status = 'Active'", entityInfo.get("environment_name")).firstValue();
			String beID = "" + db("TDM").fetch("SELECT be_id FROM business_entities " +
						"WHERE be_name =? AND be_status = 'Active'", entityInfo.get("be_name")).firstValue();
			String entityID = entityInfo.get("target_entity_id");
			
			if (!currentEnvID.equals(envID)) {
				//New Environment ID, check if the user is allowed to update it
				if ("owner".equalsIgnoreCase(permissionGroup)) {
					//Check if the user is an owner of the environment, if not treat the user as tester
					if(fnIsOwner(envID)) {
						isOwner = true;
					} else {
						isOwner = false;
					}
				}
				//log.info("getMaxEndDateSql: " + getMaxEndDateSql);
				//log.info("Inputs: entityID: " + entityID + ", beID: " + beID + ", envID: " + envID);
				//If the user is not an admin or owner, then add a condition 
				//to check if the user is the owner of the reservation 
				// Also check if the new end date do not exceed the reservation period allowed for tester
				
				if (!isOwner && !"admin".equalsIgnoreCase(permissionGroup)) {
					if (newEndDate == null || "".equals(newEndDate)) {
						errorCode = "FAILED";
						message = "The Expiration Date cannot be empty for entities reserved by a tester";
						response.put("result", result);
						response.put("errorCode", errorCode);
						response.put("message", message);
		
						return response;
					}
					String maxEndDate = "" + db("TDM").fetch(getMaxEndDateSql, entityID, beID, envID).firstValue();
					
					//log.info("newEndDate: " + newEndDate + ", maxEndDate: " + maxEndDate + ", compare: " + newEndDate.compareTo(maxEndDate));
					if(newEndDate.compareTo(maxEndDate) > 0) {
						if (maxOverrideList == "") {
							maxOverrideList = entityID;
						} else {
							maxOverrideList += ", " + entityID;
							
						}
						
						newEndDate = maxEndDate;
					}
					
					updateSql += " AND reserve_owner =?";
					 isTester = true;
				} else {
					isTester = false;
				}
		
			}
			
			//log.info("updateSql: " + updateSql + returnClause + ", newEndDate: " + newEndDate);
			//Update record
			String updatedEntityID = "";
			if (isTester) {
				updatedEntityID = "" + db("TDM").fetch(updateSql + returnClause, newEndDate, entityID, beID, envID, userId).firstValue();
			} else {
				updatedEntityID = "" + db("TDM").fetch(updateSql + returnClause, newEndDate, entityID, beID, envID).firstValue();
			}
		
			//if record was not updated
			if(!entityID.equals(updatedEntityID)) {
				HashMap<String, Object> map = new HashMap<>();
				map.put("id", entityID);
				result.add(map);
				if ("".equals(listOfFailed)) {
					listOfFailed = "" + entityID;
				} else {
					listOfFailed += ", " + entityID;
				}
			}
				
		}
		
		if (result.size() >= listOfEntities.size()) {
			errorCode = "FAILED";
			message = "The update to all entities had failed";
		} else if (result.size() > 0) {
			message = "The update of the following entities had failed: " + listOfFailed + ".";
		}
		
		if (!"".equals(maxOverrideList)) {
			if (!"".equals(message)) {
				message += "\n";
			}
			message += "The following entities had new end date exceeding max allowed, extend to max: " + maxOverrideList;
		}
		response.put("result", result);
		response.put("errorCode", errorCode);
		response.put("message", message);
		
		return response;
	}




	@desc("Validate if the given retention period on the input entity list and environment does not exceed the max retention period allowed for user. An admin user of the environment's owner can set an unlimited retention period.\r\n" +
			"\r\n" +
			"If the entities are already reserved by the user, check the new expiration date against the existing start reservation date. Else, check the retention period against the current Datetime.\r\n" +
			"\r\n" +
			"The validated retention period can be populated either by the combination of retentionUnit and retentionValue input parameters or by populating the newEndDateTime parameter. If the retention unit and values are populated, validate the reservation period based on these parameter. Else, validate the reservation period based on the input newEndDateTime.\r\n" +
			"\r\n" +
			"\r\n" +
			"Request BODY examples:\r\n" +
			"\r\n" +
			"Example 1: populate the retention unit and value parameters:\r\n" +
			"\r\n" +
			"{\r\n" +
			"\t\"retentionUnit\": \"Days\",\r\n" +
			"\t\"retentionValue\": 8,\r\n" +
			"\t\"listOfEntities\": [\r\n" +
			"\t{\r\n" +
			"\t\t\"environment_name\": \"TAR\",\r\n" +
			"\t\t\"be_name\": \"Customer\",\r\n" +
			"\t\t\"target_entity_id\": \"1\"\r\n" +
			"\t},\r\n" +
			"\t{\r\n" +
			"\t\t\"environment_name\": \"TAR\",\r\n" +
			"\t\t\"be_name\": \"Customer\",\r\n" +
			"\t\t\"target_entity_id\": \"2\"\r\n" +
			"\t}\r\n" +
			"\t]\r\n" +
			"}\r\n" +
			"\r\n" +
			"Example 2: populate the new newEndDateTime parameter:\r\n" +
			"\r\n" +
			"{\r\n" +
			"\t\"newEndDateTime\": \"20220218000000\",\r\n" +
			"\t\"listOfEntities\": [\r\n" +
			"\t{\r\n" +
			"\t\t\"environment_name\": \"TAR\",\r\n" +
			"\t\"be_name\": \"Customer\",\r\n" +
			"\t\"target_entity_id\": \"1\"\r\n" +
			"\t},\r\n" +
			"\t{\r\n" +
			"\t\"environment_name\": \"TAR\",\r\n" +
			"\t\"be_name\": \"Customer\",\r\n" +
			"\t\"target_entity_id\": \"2\"\r\n" +
			"\t}\r\n" +
			"\t]\r\n" +
			"}")
	@webService(path = "validatereserveretentionperiod", verb = {MethodType.POST}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON}, elevatedPermission = true)
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": [\r\n" +
			"    {\r\n" +
			"      \"id\": \"1\"\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"id\": \"2\"\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"id\": \"3\"\r\n" +
			"    }\r\n" +
			"  ],\r\n" +
			"  \"errorCode\": \"FAIL\",\r\n" +
			"  \"message\": \"The validation had failed for all entities due to: The new expiration date will exceed the retention period allowed for a tester.\\nThe following entities are reserved by other users: 3.\"\r\n" +
			"}")
	public static Object wsValidateReserveRetentionPeriod(String retentionUnit, Integer retentionValue, String newEndDateTime, @param(required=true) List<Map<String,String>> listOfEntities) throws Exception {
		List<HashMap<String, Object>> result = new ArrayList<>();
		String errorCode =  "SUCCESS";
		String message = "";
		String message2 = "";
		String listOfFailed = "";
		String listOfReserved = "";
		String userId = sessionUser().name();
		String permissionGroup = fnGetUserPermissionGroup("");
		Boolean checkRetentionInfo = false;
		Integer maxRetentionInSeconds = 0;
		Integer givenRetentionInSeconds = 0;
		
		if (!"admin".equalsIgnoreCase(permissionGroup)) {
			float maxReserveDays = Float.parseFloat("" + db(TDM).fetch("SELECT param_value from tdm_general_parameters " + 
												"where param_name = 'MAX_RESERVATION_DAYS_FOR_TESTER'").firstValue());
			if (retentionUnit != null && !"".equals(retentionUnit) && retentionValue != null && !"".equals(retentionValue)) {
				checkRetentionInfo = true;
				maxRetentionInSeconds = getRetention("Days", maxReserveDays);
				givenRetentionInSeconds = getRetention(retentionUnit, (float)retentionValue);
		
			}
			//log.info("userId: " + userId);
			for (Map<String, String> entityInfo : listOfEntities) {
				String envID = "" + db(TDM).fetch("SELECT environment_id FROM environments " +
					"WHERE environment_name =? AND environment_status = 'Active'", entityInfo.get("environment_name")).firstValue();
				String beID = "" + db(TDM).fetch("SELECT be_id FROM business_entities " +
					"WHERE be_name =? AND be_status = 'Active'", entityInfo.get("be_name")).firstValue();
				String entityID = entityInfo.get("target_entity_id");
				
				if ("owner".equalsIgnoreCase(permissionGroup)) {
					if(fnIsOwner(envID)) {
						continue;
					}
				}
			
				if(givenRetentionInSeconds > maxRetentionInSeconds) {
					return wrapWebServiceResults("FAILED", "The retention period exceeds the retention period of Environment: " + 
						entityInfo.get("environment_name") + " allowed for a tester", null);
				}
				//log.info("Handling entity_id: " + entityID);
				String query = "SELECT to_char(start_datetime, 'YYYYMMDDhh24MISS') AS start_datetime, to_char(end_datetime, 'YYYYMMDDhh24MISS') AS end_datetime, reserve_owner " +
					"FROM TDM_RESERVED_ENTITIES WHERE entity_id = ? AND be_id = ? AND env_id = ?  ";
		
				//log.info("query: " + query);
				Db.Row row = db(TDM).fetch(query, entityID, beID, envID).firstRow();
				
				String startDateTime = "" + row.get("start_datetime");
				String endtDateTime = "" + row.get("end_datetime");
				String reserveOwner = "" + row.get("reserve_owner");
				
				//log.info("checkRetentionInfo: " + checkRetentionInfo);
				if (userId.equals(reserveOwner)) {
					if (checkRetentionInfo && startDateTime != null && !"null".equals(startDateTime)) {
						String sql = "SELECT DATE_PART('day', (to_timestamp('" + endtDateTime + "', 'YYYYMMDDhh24MISS') " + 
							" + INTERVAL '" + retentionValue + " " + retentionUnit + "') - " +
							"to_timestamp('" + startDateTime + "', 'YYYYMMDDhh24MISS') ) ";
						//log.info("sql for retention period: " + sql);
						float diffInDays = Float.parseFloat("" + db(TDM).fetch(sql).firstValue());
						//log.info("maxReserveDays: " + maxReserveDays + ", diffInDays: " + diffInDays);
						if (diffInDays > maxReserveDays) {
							HashMap<String, Object> map = new HashMap<>();
							map.put("id", entityID);
							result.add(map);
							if ("".equals(listOfFailed)) {
								listOfFailed = "" + entityID;
							} else {
								listOfFailed += ", " + entityID;
							}
						}
						
					}
		
					if (!checkRetentionInfo) {
						if (newEndDateTime != null && !"".equals(newEndDateTime)) {
							String sql = "SELECT DATE_PART('day', to_timestamp('" + newEndDateTime + "', 'YYYYMMDDhh24MISS') - ";
							
							if (startDateTime != null && !"null".equals(startDateTime)) {
								sql += "to_timestamp('" + startDateTime + "', 'YYYYMMDDhh24MISS') ) ";
							} else {
								sql += "now())";
							}
							
							//log.info("sql for new End datetime: " + sql);
							float diffInDays = Float.parseFloat("" + db(TDM).fetch(sql).firstValue());
							if (diffInDays > maxReserveDays) {
								HashMap<String, Object> map = new HashMap<>();
								map.put("id", entityID);
								result.add(map);
								if ("".equals(listOfFailed)) {
									listOfFailed = "" + entityID;
								} else {
									listOfFailed += ", " + entityID;
								}
							} 
						}
					}
				} else {
					HashMap<String, Object> map = new HashMap<>();
					map.put("id", entityID);
					result.add(map);
					if ("".equals(listOfReserved)) {
						listOfReserved = "" + entityID;
					} else {
						listOfReserved += ", " + entityID;
					}
				}
				
			}
			
			if (!"".equals(listOfFailed)) {
				message = "The new expiration date will exceed the retention period allowed for a tester.";
			}
			if (!"".equals(listOfReserved)) {
				message2 = "The following entities are reserved by other users: " + listOfReserved + ".";
			}
			
			
			if (!"".equals(message2)) {
				message = ("".equals(message)) ? message = message2 : message + "\n" + message2;
			}
			
			if (result.size() >= listOfEntities.size()) {
				errorCode = "FAILED";
				message = "The validation had failed for all entities due to: " + message;
			} else if (result.size() > 0) {
				errorCode = "FAILED";
				message = "The validation had failed for the following entities: " + listOfFailed + ", due to: " + message;
			} else {
				message = "";
			}
			
			return wrapWebServiceResults(errorCode, message, result);
		}
		
		return wrapWebServiceResults("SUCCESS", null, null);
	}


	@desc("Validate each input entity ID if it is reserved for another user in the input environment. If the entity is reserved for another user in the environment, return a failure on the entity.\r\n" +
			"\r\n" +
			"Request BODY example:\r\n" +
			"\r\n" +
			"{\r\n" +
			"  \"beID\": \"1\",\r\n" +
			"  \"envID\": \"2\",\r\n" +
			"  \"listOfEntities\": [\r\n" +
			"    {\r\n" +
			"      \"target_entity_id\": \"1\"\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"target_entity_id\": \"3\"\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"target_entity_id\": \"4\"\r\n" +
			"    }\r\n" +
			"  ]\r\n" +
			"}")
	@webService(path = "validateReservedEntitiesList", verb = {MethodType.POST}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON}, elevatedPermission = true)
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": {\r\n" +
			"    \"listOfEntities\": [\r\n" +
			"      {\r\n" +
			"        \"reserve_message\": \"\",\r\n" +
			"        \"start_datetime\": \"2022-01-23 01:00:00.000\",\r\n" +
			"        \"end_datetime\": \"2022-02-10 00:00:00.000\",\r\n" +
			"        \"reserve_ind\": \"true\",\r\n" +
			"        \"entity_id\": \"3\",\r\n" +
			"        \"reserve_owner\": \"admin\"\r\n" +
			"      },\r\n" +
			"      {\r\n" +
			"        \"reserve_message\": \"Entity reserved by other User: taha\",\r\n" +
			"        \"start_datetime\": \"2022-02-06 01:00:00.000\",\r\n" +
			"        \"end_datetime\": \"2022-02-16 01:00:00.000\",\r\n" +
			"        \"reserve_ind\": \"true\",\r\n" +
			"        \"entity_id\": \"4\",\r\n" +
			"        \"reserve_owner\": \"taha\"\r\n" +
			"      },\r\n" +
			"      {\r\n" +
			"        \"reserve_message\": \"\",\r\n" +
			"        \"start_datetime\": null,\r\n" +
			"        \"end_datetime\": null,\r\n" +
			"        \"reserve_ind\": \"false\",\r\n" +
			"        \"entity_id\": \"1\",\r\n" +
			"        \"reserve_owner\": null\r\n" +
			"      }\r\n" +
			"    ],\r\n" +
			"    \"entityReservedByOtherInd\": true\r\n" +
			"  },\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsValidateReservedEntities(@param(required=true) String beID, @param(required=true) String envID, @param(required=true) List<Map<String,String>> listOfEntities) throws Exception {
		ArrayList<String> entitiesArray = new ArrayList<>();
		
		for (Map<String, String> entityInfo : listOfEntities) {
			entitiesArray.add(entityInfo.get("target_entity_id"));
		}
		try {
			Map<String, Object> result = fnValidateReservedEntities(beID, envID, entitiesArray);
			List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("listOfEntities");
			String message = (String)result.get("message");
			
			return wrapWebServiceResults("SUCCESS", message, list);
			
		} catch (Exception e) {
			return wrapWebServiceResults("FAILED", e.getMessage(), null);
		}
	}


	@desc("A tester user can reserve a limited number of entities per Business Entity and environment. The maximum number of reserved entities is set in the Environment permission set attached to the user. Admin and environment owners can reserve an unlimited number of entities in the environment. The API performs the following validation if the user is not an admin user and is not the owner of the environment:\r\n" +
			"\r\n" +
			"Get the Write permission set attached to the user. If the user does not have a Write permission set in the environment, an exception is thrown by the API.\r\n" +
			"\r\n" +
			"Sum the input number of entities with the number of entities that are already reserved by the user in the environment (if they exist). Validate that the the total number of reserved entities does not exceed the user's permissions.\r\n" +
			"\r\n" +
			"Example:\r\n" +
			"\r\n" +
			"- The user can reserve up to 70 customers on ST1.\r\n" +
			"\r\n" +
			"- The user already has 40 customers reserved on ST1.\r\n" +
			"\r\n" +
			"- The user asks to reserve 35 entities in the task: 40+35 = 75.\r\n" +
			"\r\n" +
			"- The API returns an error since the user cannot exceed a total of 70 customers (40 + 30) in the task.")
	@webService(path = "validatenoofreserved", verb = {MethodType.GET}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON}, elevatedPermission = true)
	public static Object wsValidateNoOfReservedEntities(@param(required=true) Integer noOfEntities, @param(required=true) String envName, @param(required=true) String beName) throws Exception {
		HashMap<String, Object> response = new HashMap<>();
		String message = null;
		String errorCode = "SUCCESS";
		String permissionGroup = fnGetUserPermissionGroup("");
		Boolean adminOrEnvOnwer = false;
		
		String envID = "" + db(TDM).fetch("SELECT environment_id FROM environments " +
					"WHERE environment_name =? AND environment_status = 'Active'", envName).firstValue();
		
		if ("admin".equals(permissionGroup)) {
			adminOrEnvOnwer = true;
		}
		if ("owner".equals(permissionGroup)) {
		
			if(fnIsOwner(envID)) {
				adminOrEnvOnwer = true;
			}
		}
				
		if (adminOrEnvOnwer) {
			response.put("result",null);
			response.put("errorCode",errorCode);
			response.put("message", message);
			return response;
		}
		
		String reserveLimit_sql = "select allowed_number_of_reserved_entities from environment_roles " +
			"where role_id = ? and environment_id = ?;";
		String getUserReserveCnt_sql = "select count(1) from tdm_reserved_entities " + 
			"where env_id = ? and be_id = ? and reserve_owner = ? and end_datetime > CURRENT_TIMESTAMP";
		
		List<Map<String,Object>> targetRolesList = new ArrayList<>();
		List<Map<String,Object>> rolesList = fnGetUserEnvs();
		//log.info("------- Size: " + rolesList.size());
		for (Map<String, Object> envType : rolesList) {
			if (envType.get("target environments") != null) {
				targetRolesList = (List<Map<String, Object>>) (envType.get("target environments"));
			}
		}
		if(targetRolesList == null || targetRolesList.isEmpty()) {
				throw new Exception("Environment does not exist or user has no write permission on this environment");
		}
		Boolean envFound = false;
		for (Map<String, Object> role : targetRolesList) {
			if (envName.equals(role.get("environment_name"))) {
				envFound = true;
				String roleId = "" + role.get("role_id");
				String envId = "" + db("TDM").fetch("select environment_id from environments where environment_name = ? and environment_status = 'Active'", envName).firstValue();
				String beId = "" + db("TDM").fetch("select be_id from business_entities where be_name = ? and be_status = 'Active'", beName).firstValue();
				//log.info("roleId: " + roleId + ", envId: " + envId + ", beId: " + beId);
				Long reserveLimit = (Long)db("TDM").fetch(reserveLimit_sql, roleId, envId).firstValue();
				if (reserveLimit.intValue() < noOfEntities) {
					message = "The number of entities to be reserved will exceed the number of entities allowed";
					errorCode = "FAILED";
				} else {
					String userId = sessionUser().name();
					Long entCount = (Long)db("TDM").fetch(getUserReserveCnt_sql, envId, beId, userId).firstValue();
					if (entCount + noOfEntities > reserveLimit) {
						message = "The number of entities to be reserved for the user will exceed the number of entities allowed";
						errorCode = "FAILED";
					}
				}
			}
		}
		
		if (!envFound) {
			message = "The user has no write permissions on this environment";
			errorCode = "FAILED";
		}
		response.put("result",null);
		response.put("errorCode",errorCode);
		response.put("message", message);
		return response;
	}

}
