package com.k2view.cdbms.usercode.lu.k2_ws.TDM_Tasks;

import com.k2view.cdbms.shared.Db;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils;
import com.k2view.fabric.common.Log;
import com.k2view.fabric.common.Util;
import org.json.JSONObject;
import com.k2view.fabric.common.Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.k2view.cdbms.shared.user.UserCode.*;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.fnIsAdminOrOwner;

@SuppressWarnings({"unused", "DefaultAnnotationParam", "unchecked"})
public class TaskValidationsUtils {

    public static final Log log = Log.a(UserCode.class);

    static int fnValidateNumberOfReadEntities(Integer numberOfEntities, String role_id, String sourceEnvName) throws Exception {
        return fnValidateNumberOfEntities(numberOfEntities, role_id, "allowed_number_of_entities_to_read", sourceEnvName);
    }

    static int fnValidateNumberOfCopyEntities(Integer numberOfEntities, String role_id, String targetEnvName) throws Exception {
        return fnValidateNumberOfEntities(numberOfEntities, role_id, "allowed_number_of_entities_to_copy", targetEnvName);
    }

    private static int fnValidateNumberOfEntities(Integer numberOfEntities, String role_id, String columnName, String envName) throws Exception {
        if (numberOfEntities == -1 || "admin".equalsIgnoreCase(role_id) || "owner".equalsIgnoreCase(role_id)) {
            return 1;
        }
		//log.info("Inputs: numberOfEntities: " + numberOfEntities + ", role_id: " + role_id +", columnName: " + columnName + ", envName: " + envName);
		String numberOfEntities_sql = "select " + columnName + " as number_of_entities from environments e, environment_roles r where " + 
										"e.environment_name = ? and lower(e.environment_status) = 'active' and " + 
										"e.environment_id = r.environment_id and r.role_id = ? ";
		//log.info("numberOfEntities_sql: " + numberOfEntities_sql);
        Long num = (Long)UserCode.db("TDM").fetch(numberOfEntities_sql, envName, role_id).firstValue();
		
		if (numberOfEntities == null) numberOfEntities = 0;
		if (num >= numberOfEntities) {
			//The role always handling the given number of entities
			return num.intValue();
		}
	
		//If this point is reached then the task is trying to process more entities than allowed for the user
        return -1;
    }

    static Boolean fnValidateParallelExecutions(Long taskId, Map<String, Object> overrideParameters) throws Exception {
        // If overrideParameters is null and already exists an execution without override parameters -> don't add a new execution
        if (overrideParameters.isEmpty()) {
            String sql = "select count(*) from task_execution_list tl where tl.task_id = ? And UPPER(tl.execution_status) IN ('RUNNING','EXECUTING','STARTED','PENDING') and not exists (select 1 from task_execution_override_attrs o where tl.task_execution_id = o.task_execution_id)";
            Object count = UserCode.db("TDM").fetch(sql, taskId).firstValue();
            if (Integer.parseInt(count.toString()) > 0) return false;
        } else {
            // If already exists an execution with the same override parameters -> don't add a new execution
            String leftSideContainmentTest_sql = "select array_to_string(array_agg(o.task_execution_id), ',') from task_execution_list tl, task_execution_override_attrs o where tl.task_id = ? " +
                    "and UPPER(tl.execution_status) IN ('RUNNING','EXECUTING','STARTED','PENDING') and " +
                    "tl.task_execution_id = o.task_execution_id " +
                    "and o.override_parameters::JSONB @> (?)::jsonb";
            String params_str = new JSONObject(overrideParameters).toString();
            String executionIds = (String) UserCode.db("TDM").fetch(leftSideContainmentTest_sql, taskId, params_str).firstValue();
            String rightSideContainmentTest_sql = "select count(*) from task_execution_override_attrs o where o.task_id = (?) AND (?)::jsonb @> o.override_parameters::JSONB AND o.task_execution_id IN (" + executionIds + ")";
            Object count = UserCode.db("TDM").fetch(rightSideContainmentTest_sql, taskId, params_str).firstValue();
            if (Integer.parseInt(count.toString()) > 0) {
                return false;
            }
        }
        return true;
    }

    static Map<String, String> fnValidateSourceEnvForTask(Map<String, Object> be_lus, Integer refCount, String selection_method,
                                                          String sync_mode, Boolean version_ind, String task_type,
                                                          Map<String, Object> envDetails) throws Exception {

        Object res = null;
        Boolean ownerOrAdminRole;
        ArrayList<String> lusList;
        String env_id, role_id, beId;
        Map<String, String> errorMessages = new HashMap<>();
		//log.info("fnValidateSourceEnvForTask - selection_method: " + selection_method);
        String beAndLus_sql = "Select 1 From (Select Array_Agg(p.lu_id) As lu_list From environment_products ep Inner Join product_logical_units p " +
                "On ep.product_id = p.product_id Where p.be_id = (?) And ep.environment_id = (?) And Lower(ep.status) = 'active') lu Where 1 = 1 ";
        String reference_sql = "select environment_id from environment_roles where role_id = ?  and allowed_refresh_reference_data = true;";
        String syncMode_sql = "select environment_id from environment_roles where role_id = ?  and allowed_request_of_fresh_data = true;";
        String versioning_sql = "select environment_id from environment_roles where role_id = ?  and allowed_entity_versioning = true;";


        beId = (String) be_lus.get("be_id");
        lusList = (ArrayList<String>) be_lus.get("LU List");
        for (String lu_str : lusList) {
            beAndLus_sql += "and " + lu_str + "=ANY(lu.lu_list) ";
        }

        env_id = "" + envDetails.get("environment_id");
        role_id = "" + envDetails.get("role_id");
		//log.info("fnValidateSourceEnvForTask - selection_method: " + selection_method);
        ownerOrAdminRole = ("admin".equalsIgnoreCase(role_id) || "owner".equalsIgnoreCase(role_id));
        //log.info("fnValidateSourceEnvForTask - role_id: " + role_id);
        //check if source env satisfy BE and LUs filtering
        res = UserCode.db("TDM").fetch(beAndLus_sql, beId, env_id).firstValue();
        if (res == null)
            errorMessages.put("BEandLUs", "The user cannot start the task with the specified logical units and business entity in source environment.");

        if ("extract".equalsIgnoreCase(task_type)) {
            // check if source env satisfy reference filtering
            if (refCount != null && refCount > 0 && !ownerOrAdminRole) {
                res = UserCode.db("TDM").fetch(reference_sql, role_id).firstValue();
                if (res == null)
                    errorMessages.put("reference", "The user has no permissions to run tasks on Reference tables on source environment");
            }
            //check if source env satisfy selection method filtering
            if ("ALL".equalsIgnoreCase(selection_method) && !ownerOrAdminRole) {
				//log.info("fnValidateSourceEnvForTask - User is not allowed to run Extract ALL");
                errorMessages.put("selectionMethod", "The User has no permissions to run 'Extract ALL' on the task's source environment. Only admin and owner users are allowed to execute 'Extract ALL'");
            }
            // in case task type is load, "selection method" and "reference" filtering are not relevant(not required)
        }

        //check if source env satisfy sync mode  filtering
        //log.info("fnValidateSourceEnvForTask - sync_mode: " + sync_mode);
        if (sync_mode != null && "FORCE".equalsIgnoreCase(sync_mode) && !ownerOrAdminRole) {
           // log.info("fnValidateSourceEnvForTask - Check if user allowed to force sync");
            res = UserCode.db("TDM").fetch(syncMode_sql, role_id).firstValue();
            if (res == null) {
                //log.info("fnValidateSourceEnvForTask - user not allowed to force sync");
                errorMessages.put("syncMode", "the user has no permissions to ask to always sync the data from the source.");
            } 
			//else {
            //    log.info("fnValidateSourceEnvForTask - res: " + res);
            //}
        }

        //check if source env satisfy versioning filtering
        if (version_ind != null && version_ind && !ownerOrAdminRole) {
            res = UserCode.db("TDM").fetch(versioning_sql, role_id).firstValue();
            if (res == null)
                errorMessages.put("versioning", "The user has no permissions to run Data Versioning tasks on the task's source environment");
        }

        //if (!errorMessages.isEmpty())  => test fails
        return errorMessages;
    }

    static Map<String, String> fnValidateTargetEnvForTask(Map<String, Object> be_lus, Integer refCount, String selection_method,
                                                          Boolean version_ind, Boolean replace_sequences, Boolean delete_before_load,
                                                          String task_type, Boolean reserve_ind, int noOfEntities, Map<String, Object> envDetails) throws Exception {
        Map<String, String> errorMessages = new HashMap<>();
        Object res = null;
        Boolean ownerOrAdminRole;
        ArrayList<String> lusList;
        String env_id, role_id, beId;
        //log.info("fnValidateTargetEnvForTask - selection_method: " + selection_method);
        String beAndLus_sql = "Select 1 From (Select Array_Agg(p.lu_id) As lu_list From environment_products ep Inner Join product_logical_units p " +
                "On ep.product_id = p.product_id Where p.be_id = (?) And ep.environment_id = (?) And Lower(ep.status) = 'active') lu Where 1 = 1 ";
        String reference_sql = "select environment_id from environment_roles where role_id = ?  and allowed_refresh_reference_data = true;";
        String syntheticData_sql = "select environment_id from environment_roles where role_id = ?  and allowed_creation_of_synthetic_data = true;";
        String randomSelection_sql = "select environment_id from environment_roles where role_id = ?  and allowed_random_entity_selection = true;";
        String versioning_sql = "select environment_id from environment_roles where role_id = ?  and allowed_entity_versioning = true;";
        String replaceSequence_sql = "select environment_id from environment_roles where role_id = ?  and allowed_replace_sequences = true;";
        String deleteBeforeLoad_sql = "select environment_id from environment_roles where role_id = ?  and  allowed_delete_before_load = true;";
		String reverseEntity_sql = "select environment_id from environment_roles where role_id = ?  and allowed_number_of_reserved_entities > 0;";
		String reserveLimit_sql = "select allowed_number_of_reserved_entities from environment_roles where role_id = ? and environment_id = ?;";
		String getUserReserveCnt_sql = "select count(1) from tdm_reserved_entities where env_id = ? and be_id = ? and reserve_owner = ? and " +
			"end_datetime > CURRENT_TIMESTAMP";

        beId = (String) be_lus.get("be_id");
        lusList = (ArrayList<String>) be_lus.get("LU List");
        for (String lu_str : lusList) {
            beAndLus_sql += "and " + lu_str + "=ANY(lu.lu_list) ";
        }

        env_id = "" + envDetails.get("environment_id");
        role_id = "" + envDetails.get("role_id");
        ownerOrAdminRole = ("admin".equalsIgnoreCase(role_id) || "owner".equalsIgnoreCase(role_id));
        //log.info("fnValidateTargetEnvForTask - role_id: " + role_id);
        //check if target env satisfy BE and LUs filtering
        res = UserCode.db("TDM").fetch(beAndLus_sql, beId, env_id).firstValue();
        if (res == null)
            errorMessages.put("BEandLUs", "The user cannot start the task with the specified logical units and business entity in target environment.");

        //check if target env satisfy reference filtering
        if (refCount != null && refCount > 0 && !ownerOrAdminRole) {
            res = UserCode.db("TDM").fetch(reference_sql, role_id).firstValue();
            if (res == null)
                errorMessages.put("reference", "The user has no permissions to run tasks on Reference tables on target environment");
        }


        //check if target env satisfy selection method filtering
        if (selection_method != null) {
            if ("R".equalsIgnoreCase(selection_method) && !ownerOrAdminRole) {
                res = UserCode.db("TDM").fetch(randomSelection_sql, role_id).firstValue();
                if (res == null)
                    errorMessages.put("selectionMethod", "The User has no permissions to run the task's selection method on the task's target environment");
            } else if ("S".equalsIgnoreCase(selection_method) && !ownerOrAdminRole) {
                res = UserCode.db("TDM").fetch(syntheticData_sql, role_id).firstValue();
                if (res == null)
                    errorMessages.put("selectionMethod", "The User has no permissions to run the task's selection method on the task's target environment");
            } else if ("ALL".equalsIgnoreCase(selection_method) && !ownerOrAdminRole)
                errorMessages.put("selectionMethod", "The User has no permissions to run 'Load ALL' on the task's source environment. Only admin and owner users are allowed to execute 'Load ALL'");
        }

        //check if target env satisfy versioning filtering
        if (version_ind != null && version_ind && !ownerOrAdminRole) {
            res = UserCode.db("TDM").fetch(versioning_sql, role_id).firstValue();
            if (res == null)
                errorMessages.put("versioning", "The user has no permissions to run Data Versioning tasks on the task's target environment");
        }

        //check if target env satisfy replace sequence filtering
        if (replace_sequences != null && replace_sequences && !ownerOrAdminRole) {
            res = UserCode.db("TDM").fetch(replaceSequence_sql, role_id).firstValue();
            if (res == null)
                errorMessages.put("replaceSequence", "The user has no permissions to replace the entities sequences.");
        }

        //check if target env satisfy delete before load filtering
        if (delete_before_load != null && delete_before_load && !ownerOrAdminRole) {
            res = UserCode.db("TDM").fetch(deleteBeforeLoad_sql, role_id).firstValue();
            if (res == null)
                errorMessages.put("deleteBeforeLoad", "The user has no permissions to delete entities from the target.");
        }

		//check if target env satisfy reserve entities filtering
        if (reserve_ind != null && reserve_ind && !ownerOrAdminRole) {
            res = UserCode.db("TDM").fetch(reverseEntity_sql, role_id).firstValue();
            if (res == null) {
                errorMessages.put("reserveEntities", "The user has no permissions to reserve entities on the task's target environment");
			} else {
				if (noOfEntities > 0) {
					Long reserveLimit = (Long)UserCode.db("TDM").fetch(reserveLimit_sql, role_id, env_id).firstValue();
					if (reserveLimit.intValue() < noOfEntities) {
						errorMessages.put("Number of reserved entities", "The number of entities to be reserved will exceed the number of entities allowed");
					} else {
						String userId = sessionUser().name();
						Long entCount = (Long)UserCode.db("TDM").fetch(getUserReserveCnt_sql, env_id, beId, userId).firstValue();
						if (entCount + noOfEntities > reserveLimit) {
							errorMessages.put("Number of reserved entities for user", "The number of entities to be reserved for the user will exceed the number of entities allowed");
						}
					}
					
				}
				
			}
        }
        return errorMessages;
    }

	static Map<String, String> fnValidateRetentionPeriodParams(Map<String,String> retentionPeriodParams, String validation, String envId) {
		Map<String, String> errorMessages = new HashMap<>();
		Boolean adminOrOwner = Util.rte(() -> fnIsAdminOrOwner(envId, sessionUser().name()));
		Map<String, Object> retentionDefinitions = TdmSharedUtils.fnGetRetentionPeriod();
		Long maxRetentionPeriod = -1L;
		if ("versioning".equals(validation)) {	
			maxRetentionPeriod =  Long.parseLong("" + retentionDefinitions.get("maxRetentionPeriod"));
		} else {
			if (adminOrOwner) {
				maxRetentionPeriod = 0L;
			} else {
				maxRetentionPeriod =  Long.parseLong("" + retentionDefinitions.get("maxReserveDays"));
			}
		}
			ArrayList<Map<String, String>> retentionPeriodTypes = (ArrayList<Map<String, String>>)retentionDefinitions.get("retentionPeriodTypes");
			//log.info("retentionPeriodTypes: " + retentionPeriodTypes);
		
		String unit = retentionPeriodParams.get("unit");
		String value = retentionPeriodParams.get("value");
		
		String unitToDay = "1";
		for (Map<String, String>rec : retentionPeriodTypes) {
			if(unit.equalsIgnoreCase(rec.get("name"))) {
				unitToDay = String.valueOf(rec.get("units"));
				break;
			}
		}
		
		Double retentionValue = Double.parseDouble(value);
		if (!adminOrOwner && retentionValue == 0) {
			errorMessages.put("retention", "The retention period of a tester user cannot be zero");
		}
		if (retentionValue < 0) {
			errorMessages.put("retention", "The retention period is negative");
		} else {
			Double retention = Double.parseDouble(unitToDay) * retentionValue;
			if (maxRetentionPeriod > 0 && retention > maxRetentionPeriod) {
				errorMessages.put("retention", "The retention period exceeds the max retention period for a task");
			}
		}

		return errorMessages;
	}
	
	static Map<String, String> fnValidateVersionExecIdAndGetDetails(Long dataVersionExecId, Map<String,Object> beLUs, String sourceEnvName) throws Exception {
        Map<String, String> result = new HashMap<>();
        Long beId = Long.parseLong("" + beLUs.get("be_id"));
        List<String> luList =(ArrayList<String>)beLUs.get("LU List");
		
		Db.Rows taskList = db("TDM").fetch("SELECT t.task_title, to_char(l.version_datetime, 'YYYYMMDDHH24MISS') as version_datetime, l.execution_status, lu.lu_id " +
                            "FROM tasks t, task_execution_list l, tasks_logical_units lu, environments e " +
							"WHERE l.task_execution_id = ? AND l.task_id = t.task_id AND l.be_id = ? " +
                            "AND l.task_id = lu.task_id AND l.lu_id = lu.lu_id AND e.environment_name = ? and e.environment_status = 'Active' " +
                            "AND e.environment_id = l.source_environment_id",
							dataVersionExecId, beId, sourceEnvName);
		if (!taskList.resultSet().isBeforeFirst()) {
            result.put("errorMessage", "Task Execution ID was not found or it was executed with different BE or Source Environment");
            return result;
        }

		for (Db.Row row : taskList) {
		    String luId = "" + row.get("lu_id");
			
		    if (luList.indexOf(luId) != -1) {
                if ("completed".equalsIgnoreCase("" + row.get("execution_status"))) {
                    luList.remove(luId);
                    result.put("versionName", "" + row.get("task_title"));
                    result.put("versionDatetime", "" + row.get("version_datetime"));
                } else {
                    result.put("errorMessage", "Not ALL the requested LUs had passed successfully in given extract task");
                    return result;
                }
            }
        }
		if(luList.size() > 0) {
            result.put("errorMessage", "Not ALL the requested LUs were part of the given extract task");
            return result;
        }
		return result;
	}
}
