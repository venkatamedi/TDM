/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.TDM_BusinessEntities;

import com.k2view.cdbms.shared.Db;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.desc;
import com.k2view.fabric.api.endpoint.Endpoint.*;
import com.k2view.fabric.common.Util;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.StreamSupport;

import static com.k2view.cdbms.usercode.common.SharedGlobals.COMBO_MAX_COUNT;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.getFabricResponse;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.wrapWebServiceResults;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.fnGetUserPermissionGroup;
import java.sql.*;
import java.math.*;
import java.io.*;
import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam", "unchecked"})
public class Logic extends WebServiceUserCode {
	
	public static final String schema = "public";
	public static final String BE_ID = "BE_ID";
	public static final String LU_NAME = "LU_NAME";
	public static final String PARAM_NAME = "PARAM_NAME";
	public static final String PARAM_TYPE = "PARAM_TYPE";
	public static final String VALID_VALUES = "VALID_VALUES";
	public static final String LU_PARAMS_TABLE_NAME = "LU_PARAMS_TABLE_NAME";
	public static final String MAX_VALUE = "MAX_VALUE";
	public static final String MIN_VALUE = "MIN_VALUE";
	public static final String LU_SQL = "SELECT product_id as productID, product_name as productName, lu_id as logicalUnitID, lu_name as logicalUnitName FROM product_logical_units WHERE be_id = ? ORDER BY lu_id";
	final static String admin_pg_access_denied_msg = "Access Denied. Please login with administrator privileges and try again";
	public enum PARAM_TYPES{
		COMBO, NUMBER, TEXT;

		public String getName(){
			return this.toString().toLowerCase();
		}
	}

	@desc("Gets the list of all Business Entities from the TDM DB. The API is invoked to populate the list of Business Entities in the Business Entities window.")
	@webService(path = "businessentities", verb = {MethodType.GET}, version = "1", isRaw = false, isCustomPayload = true, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": [\r\n" +
			"    {\r\n" +
			"      \"be_id\": 3,\r\n" +
			"      \"be_status\": \"Active\",\r\n" +
			"      \"be_creation_date\": \"2021-04-18 09:33:49.741\",\r\n" +
			"      \"be_last_updated_date\": \"2021-04-18 09:33:53.342\",\r\n" +
			"      \"be_description\": null,\r\n" +
			"      \"be_created_by\": \"K2View\",\r\n" +
			"      \"be_name\": \"BE\",\r\n" +
			"      \"be_last_updated_by\": \"K2View\"\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"be_id\": 4,\r\n" +
			"      \"be_status\": \"Inactive\",\r\n" +
			"      \"be_creation_date\": \"2021-04-18 09:55:32.912\",\r\n" +
			"      \"be_last_updated_date\": \"2021-04-18 09:55:32.912\",\r\n" +
			"      \"be_description\": \"description\",\r\n" +
			"      \"be_created_by\": \"K2View\",\r\n" +
			"      \"be_name\": \"BE2\",\r\n" +
			"      \"be_last_updated_by\": \"K2View\"\r\n" +
			"    }\r\n" +
			"  ],\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsGetBusinessEntities() throws Exception {
		String sql = "SELECT be_id, be_description, be_name, be_created_by, be_creation_date, be_last_updated_date, be_last_updated_by, be_status FROM \"" + schema + "\".business_entities";
		String errorCode="";
		String message=null;
		
		try{
			Db.Rows rows = db("TDM").fetch(sql);
			errorCode= "SUCCESS";
			List<Map<String,Object>> result=new ArrayList<>();
			Map<String,Object> businessEntity;
			for(Db.Row row:rows) {
				businessEntity=new HashMap<String,Object>();
				businessEntity.put("be_id",Integer.parseInt(row.get("be_id").toString()));
				businessEntity.put("be_description", row.get("be_description"));
				businessEntity.put("be_name", row.get("be_name"));
				businessEntity.put("be_created_by", row.get("be_created_by"));
				businessEntity.put("be_creation_date", row.get("be_creation_date"));
				businessEntity.put("be_last_updated_date", row.get("be_last_updated_date"));
				businessEntity.put("be_last_updated_by", row.get("be_last_updated_by"));
				businessEntity.put("be_status", row.get("be_status"));
				result.add(businessEntity);
			}
			return wrapWebServiceResults(errorCode,message,result);
		}
		catch(Exception e){
			errorCode= "FAILED";
			message= e.getMessage();
			log.error(message);
			return wrapWebServiceResults(errorCode,message,null);
		}
				
	}

	@desc("Creates a New Business Entity.")
	@webService(path = "businessentity", verb = {MethodType.POST}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": {\r\n" +
			"    \"be_id\": 17,\r\n" +
			"    \"be_status\": \"Active\",\r\n" +
			"    \"be_creation_date\": \"2021-04-28 06:45:26.994\",\r\n" +
			"    \"be_last_updated_date\": \"2021-04-28 06:45:26.994\",\r\n" +
			"    \"be_description\": \"test BE\",\r\n" +
			"    \"be_created_by\": \"K2View\",\r\n" +
			"    \"be_name\": \"BE10\",\r\n" +
			"    \"be_last_updated_by\": \"K2View\"\r\n" +
			"  },\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsPostBusinessEntity(String be_name, String be_description) throws Exception {
		
		String permissionGroup = fnGetUserPermissionGroup("");
		if ("admin".equals(permissionGroup)) {
			try {
				String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
						.withZone(ZoneOffset.UTC)
						.format(Instant.now());
				String sql = "INSERT INTO \"" + schema + "\".business_entities (be_name, be_description, be_created_by, be_creation_date, be_last_updated_date, be_last_updated_by, be_status) " +
						"VALUES (?, ?, ?, ?, ?, ?, ?)" +
						"RETURNING be_id,be_name, be_description, be_created_by, be_creation_date, be_last_updated_date, be_last_updated_by, be_status";

				String username = sessionUser().name();
				Db.Row row = db("TDM").fetch(sql, be_name, be_description!=null?be_description:"", username, now, now, username, "Active").firstRow();
				HashMap<String,Object> businessEntity=new HashMap<>();
				businessEntity.put("be_id",Integer.parseInt(row.get("be_id").toString()));
				businessEntity.put("be_name", row.get("be_name"));
				businessEntity.put("be_description", row.get("be_description"));
				businessEntity.put("be_created_by", row.get("be_created_by"));
				businessEntity.put("be_creation_date", row.get("be_creation_date"));
				businessEntity.put("be_last_updated_date", row.get("be_last_updated_date"));
				businessEntity.put("be_last_updated_by", row.get("be_last_updated_by"));
				businessEntity.put("be_status", row.get("be_status"));

				String activityDesc = "Business entity " + be_name + " was created";
				try {
					fnInsertActivity("create", "Business entities", activityDesc);
				}
				catch(Exception e){
					log.error(e.getMessage());
				}

				return wrapWebServiceResults("SUCCESS",null,businessEntity);
			}
			catch (Exception e){
				String message=e.getMessage();
				log.error(message);
				return wrapWebServiceResults("FAILED",message,null);
			}
		}else {
			return wrapWebServiceResults("FAILED",admin_pg_access_denied_msg,null);
		}
	}

	@desc("Updates the Business Entity's description.")
	@webService(path = "businessentity/{beId}", verb = {MethodType.PUT}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsPutBusinessEntity(String be_description, @param(required=true) Long beId) throws Exception {
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		String permissionGroup = fnGetUserPermissionGroup("");
		if ("admin".equals(permissionGroup)) {
			try{
				String username = sessionUser().name();

				String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
						.withZone(ZoneOffset.UTC)
						.format(Instant.now());
				String sql = "UPDATE \"" + schema + "\".business_entities " +
						"SET be_description=(?)," +
						"be_last_updated_date=(?)," +
						"be_last_updated_by=(?) " +
						"WHERE be_id = " + beId + "RETURNING be_name";

				Db.Row row = db("TDM").fetch(sql, be_description,now,username).firstRow();
				errorCode= "SUCCESS";

				String activityDesc = "Business entity " + row.get("be_name") + " was updated";
				try {
					fnInsertActivity("update", "Business entities", activityDesc);
				}
				catch(Exception e){
					log.error(e.getMessage());
				}

			}
			catch(Exception e){
				errorCode= "FAILED";
				message=e.getMessage();
				log.error(message);
			}

		} else {
			message = admin_pg_access_denied_msg;
			errorCode= "FAILED";
		}
		response.put("errorCode",errorCode);
		response.put("message",message);
		return response;
	}


	@desc("Deletes a Business Entity.")
	@webService(path = "businessentity/{beId}", verb = {MethodType.DELETE}, version = "", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsDeleteBusinessEntity(@param(required=true) Long beId) throws Exception {
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";

		String permissionGroup = fnGetUserPermissionGroup("");
		if ("admin".equals(permissionGroup)) {
			try {
				String sql = "UPDATE \"" + schema + "\".business_entities SET be_status=(?) " +
						"WHERE be_id = " + beId + "  RETURNING be_name";
				Db.Rows rows = db("TDM").fetch(sql, "Inactive");
				Db.Row firstRec = rows.firstRow();
				String beName="";
				if (!firstRec.isEmpty()) beName = "" + firstRec.get("be_name");

				String updateEnvironmentProductsSql = "UPDATE \"public\".environment_products " +
						"SET status= (?) " +
						"from ( " +
						"select product_id, count(product_id) " +
						"from \"public\".product_logical_units " +
						"WHERE be_id = " + beId + "  AND  " +
						"product_id not in (select product_id from \"public\".product_logical_units where be_id <> " + beId + " AND product_id <> -1) " +
						"GROUP BY product_id ) l " +
						"WHERE \"public\".environment_products.status = 'Active' AND l.product_id = \"public\".environment_products.product_id AND l.count = 1";
				db("TDM").execute(updateEnvironmentProductsSql, "Inactive");

				String updateProductLUsSql = "UPDATE \"" + schema + "\".product_logical_units " +
						"SET product_id=(?) " +
						"WHERE be_id = " + beId;
				db("TDM").execute(updateProductLUsSql, -1);

				String updateTasksSql = "UPDATE \"" + schema + "\".tasks " +
						"SET task_status=(?) " +
						"WHERE be_id = " + beId;
				db("TDM").execute(updateTasksSql, "Inactive");

				errorCode="SUCCESS";

				String activityDesc = "Business entity " + beName + " was deleted";
				try {
					fnInsertActivity("delete", "Business entities", activityDesc);
				}
				catch(Exception e){
					log.error(e.getMessage());
				}

			} catch(Exception e){
				message=e.getMessage();
				errorCode= "FAILED";
				log.error(message);
			}
		} else {
			message = admin_pg_access_denied_msg;
			errorCode= "FAILED";
		}
		
		response.put("errorCode",errorCode);
		response.put("message", message);
		return response;
	}



	@desc("Gets a list of deployed Logical Units.")
	@webService(path = "logicalunits", verb = {MethodType.GET}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": [\r\n" +
			"    \"luName1\",\r\n" +
			"    \"luName2\"\r\n" +
			"  ],\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsGetLogicalUnits() throws Exception {
		List<String> result=new ArrayList<>();
		List luList = (List) getFabricResponse("list lut;");
		for(Object e : luList) {
			Object luName = ((Map) e).get("LU_NAME");
			if (!"TDM".equals(luName)) {
				result.add((String)luName);
			}
		}
		return wrapWebServiceResults("SUCCESS", null, result);
	}

	@desc("Gets the list of all available Post Execution Processes that can be added to the Business Entity.")
	@webService(path = "postexecutionprocesses", verb = {MethodType.GET}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"translations\": {},\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsGetPostExecutionProcesses() throws Exception {
		Map<String,Object> response=new HashMap<>();
		Map<String, Map<String, String>> translations;
		try {
			translations = getTranslationsData("trnPostProcessList");
			response.put("errorCode","SUCCESS");
			response.put("message",null);
			response.put("result",translations);
			return response;
		} catch(Exception e){
			response.put("errorCode","FAILED");
			response.put("message",e.getMessage());
			return response;
		}
	}


	@desc("Gets the Logical Units of a Business Entity.")
	@webService(path = "businessentity/{beId}/logicalunits", verb = {MethodType.GET}, version = "", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": [\r\n" +
			"    {\r\n" +
			"      \"lu_description\": \"description\",\r\n" +
			"      \"be_id\": 1,\r\n" +
			"      \"lu_parent_id\": 13,\r\n" +
			"      \"lu_parent_name\": \"parentName\",\r\n" +
			"      \"lu_name\": \"name\",\r\n" +
			"      \"product_id\": 2,\r\n" +
			"      \"lu_id\": 2,\r\n" +
			"      \"lu_dc_name\": \"dcName\",\r\n" +
			"      \"product_name\": \"productName\"\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"lu_description\": \"description\",\r\n" +
			"      \"be_id\": 1,\r\n" +
			"      \"lu_parent_id\": 14,\r\n" +
			"      \"lu_parent_name\": \"parentName\",\r\n" +
			"      \"lu_name\": \"name\",\r\n" +
			"      \"product_id\": 3,\r\n" +
			"      \"lu_id\": 4,\r\n" +
			"      \"lu_dc_name\": \"dcName\",\r\n" +
			"      \"product_name\": \"productName\"\r\n" +
			"    }\r\n" +
			"  ],\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsGetLogicalUnitsPerBusinessEntity(@param(description="A unique identifer of the Business Entity", required=true) Long beId) throws Exception {
		Map<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		String sql="SELECT * FROM \"" + schema + "\".product_logical_units " +
		"WHERE be_id = " + beId;
		
		try{
			Db.Rows rows = db("TDM").fetch(sql);
			List<Map<String,Object>> logicalUnits=new ArrayList<>();
			Map<String,Object> logicalUnit;
			for(Db.Row row:rows) {
				logicalUnit=new HashMap<>();
				logicalUnit.put("lu_name", row.get("lu_name"));
				logicalUnit.put("lu_description",row.get("lu_description"));
				logicalUnit.put("be_id", Long.parseLong(row.get("be_id").toString()));
				logicalUnit.put("lu_parent_id",row.get("lu_parent_id")!=null?Long.parseLong(row.get("lu_parent_id").toString()):null);
				logicalUnit.put("lu_id", Long.parseLong(row.get("lu_id").toString()));
				logicalUnit.put("product_name",row.get("product_name"));
				logicalUnit.put("lu_parent_name",row.get("lu_parent_name"));
				logicalUnit.put("product_id", row.get("product_id")!=null?Long.parseLong(row.get("product_id").toString()):null);
				logicalUnit.put("lu_dc_name",row.get("lu_dc_name"));
				logicalUnits.add(logicalUnit);
			}
			errorCode= "SUCCESS";
			response.put("result",logicalUnits);
		}
		catch(Exception e){
			errorCode= "FAILED";
			message= e.getMessage();
			log.error(message);
		}
		
		response.put("errorCode",errorCode);
		response.put("message",message);
		return response;
	}

	@desc("Updates an LU in a Business Entity: updates the description, parent LU or the DC name LU's settings. Note that if the Logical Unit it not attached to any product, the product_id must be populated by -1 and the product_name is null.\r\n" +
			"\r\n" +
			"Example request body:\r\n" +
			"{\r\n" +
			"\"logicalUnit\": {\r\n" +
			"\"lu_description\": null,\r\n" +
			"\"be_id\": 1,\r\n" +
			"\"lu_parent_id\": null,\r\n" +
			"\"lu_parent_name\": null,\r\n" +
			"\"product_id\": -1,\r\n" +
			"\"lu_dc_name\": null,\r\n" +
			"\"product_name\": null,\r\n" +
			"\"lu_id\": 24,\r\n" +
			"}\r\n" +
			"}")
	@webService(path = "businessentity/{beId}/logicalunit/{luId}", verb = {MethodType.PUT}, version = "", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsUpdateLogicalUnitsInBusinessEntity(@param(description="Business Entity ID", required=true) Long beId, @param(description="LU ID", required=true) Long luId, Map<String,Object> logicalUnit) throws Exception {
		String permissionGroup = fnGetUserPermissionGroup("");
		if (!"admin".equals(permissionGroup)) return wrapWebServiceResults("FAILED",admin_pg_access_denied_msg,null);
		
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		/*
		Map<String,Object> logicalUnit=new HashMap<>();
		logicalUnit.put("lu_id",lu_id);
		logicalUnit.put("lu_parent_id", lu_parent_id);
		logicalUnit.put("lu_parent_name", lu_parent_name);
		logicalUnit.put("lu_description", lu_description);
		logicalUnit.put("product_id", product_id);
		logicalUnit.put("product_name", product_name);
		logicalUnit.put("lu_dc_name", lu_dc_name);
		*/
		try {
			String username = sessionUser().name();
			fnUpdateBusinessEntityDate(beId, username);
		}
		catch(Exception e){
			log.error(e.getMessage());
		}
		
		if (logicalUnit.get("product_id")!=null && !"-1".equals(logicalUnit.get("product_id").toString())) {
			try {
				String username = sessionUser().name();
				Long prodId = Long.parseLong(logicalUnit.get("product_id").toString());
				fnUpdateProductDate(prodId,username);
			} catch(Exception e){
				log.error(e.getMessage());
			}
		}
		
		try{
			fnUpdateLogicalUnit(logicalUnit);
			errorCode = "SUCCESS";
		
			try {
				String activityDesc = "Logical unit " + logicalUnit.get("lu_name") + " was updated";
				fnInsertActivity("update", "Business entities", activityDesc);
			}
			catch(Exception e){
				log.error(e.getMessage());
			}
		
		}
		catch(Exception e){
			log.error(e.getMessage());
			errorCode = "FAILED";
			message = e.getMessage();
		}
		
		response.put("errorCode",errorCode);
		response.put("message", message);
		return response;
	}


	@desc("Updates the Business Entity's Logical Units.\r\n" +
			"Example request body:\r\n" +
			"{\r\n" +
			"  \"product_id\": 1,\r\n" +
			"  \"product_name\": \"PROD\",\r\n" +
			"  \"logicalUnits\": [\r\n" +
			"    {\r\n" +
			"      \"lu_description\": \"updatedDescription\",\r\n" +
			"      \"be_id\": 1,\r\n" +
			"      \"lu_parent_id\": null,\r\n" +
			"      \"lu_parent_name\": null,\r\n" +
			"      \"lu_dc_name\": \"updatedDcName\",   \r\n" +
			"      \"lu_id\": 24,\r\n" +
			"    },{\r\n" +
			"      \"lu_description\": \"updatedDescription\",\r\n" +
			"      \"be_id\": 1,\r\n" +
			"      \"lu_parent_id\": 26,\r\n" +
			"      \"lu_parent_name\": \"parentLuName\",\r\n" +
			"      \"lu_dc_name\": \"updatedDcName\",\r\n" +
			"      \"lu_id\": 23,\r\n" +
			"     }\r\n" +
			"  ]\r\n" +
			"}")
	@webService(path = "businessentity/{beId}/logicalunits", verb = {MethodType.PUT}, version = "", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsUpdateBusinessEntityLogicalUnits(@param(required=true) Long beId, Long product_id, String product_name, List<Map<String,Object>> logicalUnits) throws Exception {
		String permissionGroup = fnGetUserPermissionGroup("");
		if (!"admin".equals(permissionGroup)) return wrapWebServiceResults("FAILED",admin_pg_access_denied_msg,null);
		
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";

		if (product_id!=null && product_id != -1) {
			String username = sessionUser().name();
			
			fnUpdateProductDate(product_id,username);
		}
		for(Map<String,Object> logicalUnit:logicalUnits){
			logicalUnit.put("product_id",product_id);
			logicalUnit.put("product_name",product_name);
			try {
				fnUpdateLogicalUnit(logicalUnit);
				String activityDesc = "Logical unit " + logicalUnit.get("lu_name") + " was updated";
				try {
					fnInsertActivity("update", "Business entities", activityDesc);
				}
				catch(Exception e){
					log.error(e.getMessage());
				}
		
				errorCode="SUCCESS";
			}
			catch (Exception e){
				log.error(e.getMessage());
				errorCode="FAILED";
				message=e.getMessage();
			}
		}
		
		response.put("errorCode",errorCode);
		response.put("message", message);
		return response;
	}

	@desc("Deletes Logical Unit from the selected Business Entity.")
	@webService(path = "businessentity/{beId}/bename/{beName}/logicalunit/{luId}/luname/{luName}", verb = {MethodType.DELETE}, version = "", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsDeleteLogicalUnitForBusinessEntity(@param(required=true) Long luId, @param(required=true) Long beId, @param(required=true) String beName, @param(required=true) String luName) throws Exception {
		String permissionGroup = fnGetUserPermissionGroup("");
		if (!"admin".equals(permissionGroup)) return wrapWebServiceResults("FAILED",admin_pg_access_denied_msg,null);
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		try {
			String username = sessionUser().name();
			fnUpdateBusinessEntityDate(beId, username);
		} catch(Exception e){
			log.error(e.getMessage());
		}
		try {
			fnDeleteLogicalUnit(luId,beId);
			try {
				String activityDesc = "Logical unit " + luName + " of business entity " + beName + " was deleted";
				fnInsertActivity("update", "Business entities", activityDesc);
			} catch(Exception e){
				log.error(e.getMessage());
			}
			errorCode="SUCCESS";
			message="null";
		} catch(Exception e){
			errorCode="FAILED";
			message=e.getMessage();
		}
		
		response.put("errorCode",errorCode);
		response.put("message", message);
		return response;
	}


	@desc("Adds Logical Units to the selected Business Entity.\r\n" +
			"\r\n" +
			"Example request body:\r\n" +
			"{\r\n" +
			"  \"logicalUnits\": [\r\n" +
			"       {\r\n" +
			"      \"lu_description\": \"description\",\r\n" +
			"      \"lu_name\": \"luName\",\r\n" +
			"      \"lu_dc_name\": \"dcName\",\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"lu_description\": \"description\",\r\n" +
			"      \"lu_name\": \"luName2\",\r\n" +
			"      \"lu_dc_name\": \"dcName\",\r\n" +
			"      \"lu_parent\":{\r\n" +
			"               \"logical_unit\":\"luName\"\r\n" +
			"         } \r\n" +
			"    }\r\n" +
			"  ]\r\n" +
			"}")
	@webService(path = "businessentity/{beId}/bename/{beName}/logicalunits", verb = {MethodType.POST}, version = "", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": [\r\n" +
			"    {\r\n" +
			"      \"lu_description\": \"description\",\r\n" +
			"      \"lu_name\": \"luName\",\r\n" +
			"      \"lu_dc_name\": \"dcName\",\r\n" +
			"      \"lu_id\": 25\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"lu_description\": \"description\",\r\n" +
			"      \"lu_name\": \"luName2\",\r\n" +
			"      \"lu_dc_name\": \"dcName\",\r\n" +
			"      \"lu_parent\": {\r\n" +
			"        \"logical_unit\": \"luName\",\r\n" +
			"        \"lu_id\": 25\r\n" +
			"      },\r\n" +
			"      \"lu_id\": 26\r\n" +
			"    }\r\n" +
			"  ],\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsAddLogicalUnitsForBusinessEntity(@param(required=true) Long beId, @param(required=true) String beName, List<Map<String,Object>> logicalUnits) throws Exception {
		String permissionGroup = fnGetUserPermissionGroup("");
		if (!"admin".equals(permissionGroup)) return wrapWebServiceResults("FAILED",admin_pg_access_denied_msg,null);
		
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		List<Map<String,Object>> result;
		try {
			fnAddLogicalUnits(logicalUnits,beId);
			for(Map<String,Object> logicalUnit:logicalUnits){
				try{
					String activityDesc = "Logical unit " + logicalUnit.get("lu_name") + " was added to business entity " + beName;
					fnInsertActivity("update", "Business entities", activityDesc);
				} catch(Exception e){
					log.error(e.getMessage());
				}
			}
		
			errorCode="SUCCESS";
			result=logicalUnits;
			response.put("result",result);
		
		} catch(Exception e){
			message=e.getMessage();
			log.error(message);
			errorCode="FAILED";
		}
		response.put("errorCode",errorCode);
		response.put("message", message);
		return response;
	}


	@desc("Gets the number of active products of a Business Entity.")
	@webService(path = "businessentity/{beId}/productCount", verb = {MethodType.GET}, version = "", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": 7\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsGetProductCountPerBusinessEntity(@param(required=true) Long beId) throws Exception {
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		
		try {
			String sql = "SELECT COUNT(be_id) as cnt FROM \"" + schema + "\".product_logical_units " +
					"WHERE be_id = " + beId + " AND product_id <> -1";
			Db.Rows rows= db("TDM").fetch(sql);
			int result =Integer.parseInt(rows.firstRow().get("cnt").toString());
			errorCode="SUCCESS";
			response.put("result", result);
		} catch(Exception e){
			log.error(e.getMessage());
			errorCode="FAILED";
		}
		response.put("errorCode",errorCode);
		response.put("message", message);
		return response;
	}


	@desc("Deletes all the Business Entitie's tasks and set their status to be Inactive.")
	@webService(path = "businessentity/{beId}/task", verb = {MethodType.DELETE}, version = "", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsDeleteTaskForBusinessEntity(@param(required=true) Long beId) throws Exception {
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		
		try {
			String sql="UPDATE \"" + schema + "\".tasks " +
					"SET task_status=(?) " +
					"WHERE be_id = " + beId;
			db("TDM").execute(sql,"Inactive");
			errorCode="SUCCESS";
		} catch(Exception e){
			message=e.getMessage();
			log.error(message);
			errorCode="FAILED";
		}
		response.put("message",message);
		response.put("errorCode",errorCode);
		return response;
	}


	@desc("Deletes a Post Execution Process from the selected Business Entity.")
	@webService(path = "businessentity/{beId}/bename/{beName}/postexecutionprocess/{process_id}/{name}", verb = {MethodType.DELETE}, version = "", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsDeletePostExecutionForBusinessEntity(@param(required=true) Long beId, @param(required=true) String beName, @param(required=true) Long process_id, @param(required=true) String name) throws Exception {
		String permissionGroup = fnGetUserPermissionGroup("");
		if (!"admin".equals(permissionGroup)) return wrapWebServiceResults("FAILED",admin_pg_access_denied_msg,null);
		
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		
		try {
			String username = sessionUser().name();
			fnUpdateBusinessEntityDate(beId, username);
		} catch(Exception e){
			log.error(message);
		}
		
		try{
			String sql= "DELETE FROM \"" + schema + "\".TDM_BE_POST_EXE_PROCESS WHERE process_id = (?) RETURNING process_id";
			db("TDM").execute(sql,process_id);
		
			try{
				String activityDesc = "Post Execution Process " + name + " of business entity " + beName + " was deleted";
				fnInsertActivity("update", "Business entities", activityDesc);
			}
			catch(Exception e){
				log.error(e.getMessage());
			}
		
			try{
				String updateTasksSql= "UPDATE \"public\".tasks " +
						"SET task_status= (?) " +
						"FROM ( SELECT \"" + schema + "\".TASKS_POST_EXE_PROCESS.task_id FROM \"" + schema + "\".TASKS_POST_EXE_PROCESS " +
						"WHERE \"" + schema + "\".TASKS_POST_EXE_PROCESS.process_id = " + process_id + " ) AS TaskPostExec " +
						"WHERE TaskPostExec.task_id = tasks.task_id ";
				db("tdm").execute(updateTasksSql,"Inactive");
			}
			catch(Exception e){
				log.error(e.getMessage());
			}
		
			errorCode="SUCCESS";
		
		} catch(Exception e){
			message=e.getMessage();
			log.error(message);
			errorCode="FAILED";
		}
		
		response.put("message",message);
		response.put("errorCode",errorCode);
		return response;
	}


	@desc("Adds a Post Execution Process to a Business Entity.")
	@webService(path = "businessentity/{beId}/bename/{beName}/postexecutionprocess", verb = {MethodType.POST}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": {\r\n" +
			"    \"id\": id\r\n" +
			"  },\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsAddPostExecutionForBusinessEntity(@param(required=true) Long beId, @param(required=true) String beName, @param(description="This is the Broadway flow name that needs to run as a post task execution process") String process_name, @param(description="Execution order of the post exeuction process. Several processes canhave the same execution order.") Integer execution_order, String process_description) throws Exception {
		String permissionGroup = fnGetUserPermissionGroup("");
		if (!"admin".equals(permissionGroup)) return wrapWebServiceResults("FAILED",admin_pg_access_denied_msg,null);
		
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		
		try {
			String username = sessionUser().name();
			fnUpdateBusinessEntityDate(beId, username);
		} catch(Exception e){
			log.error(message);
		}
		
		try{
			String sql= "INSERT INTO \"" + schema + "\".TDM_BE_POST_EXE_PROCESS " +
					"(process_name, be_id, execution_order, process_description) " +
					"VALUES (?, ?, ?, ?) RETURNING process_id";
			Db.Row row= db("TDM").fetch(sql,process_name,beId,execution_order,process_description).firstRow();
			HashMap<String,Object> result=new HashMap<>();
			result.put("id", Long.parseLong(row.get("process_id").toString()));
			try{
				String activityDesc = "Post Execution Process " + process_name + " was added to business entity " + beName;
				fnInsertActivity("update", "Business entities", activityDesc);
			}catch(Exception e){
				log.error(e.getMessage());
			}
			errorCode="SUCCESS";
			response.put("result",result);
		}catch(Exception e){
			message = e.getMessage();
			log.error(message);
			errorCode="FAILED";
		}
		
		response.put("message",message);
		response.put("errorCode",errorCode);
		return response;
	}


	@desc("Gets Post Execution Processes by a Business Entity.")
	@webService(path = "businessentity/{beId}/postexecutionprocess", verb = {MethodType.GET}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": [\r\n" +
			"    {\r\n" +
			"      \"process_id\": 1,\r\n" +
			"      \"be_id\": 2,\r\n" +
			"      \"process_name\": \"name\",\r\n" +
			"      \"process_description\": \"description,\r\n" +
			"      \"execution_order\": 1\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"process_id\": 2,\r\n" +
			"      \"be_id\": 2,\r\n" +
			"      \"process_name\": \"name2\",\r\n" +
			"      \"process_description\": \"description2,\r\n" +
			"      \"execution_order\": 2\r\n" +
			"    }\r\n" +
			"  ],\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsGetPostExecutionByBusinessEntity(@param(required=true) Long beId) throws Exception {
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		
		try {
			String sql= "SELECT * FROM \"" + schema + "\".TDM_BE_POST_EXE_PROCESS  WHERE be_id = " + beId;
			Db.Rows rows= db("TDM").fetch(sql);
		
			List<HashMap<String,Object>> result=new ArrayList<>();
			HashMap<String,Object> process;
			for(Db.Row row: rows){
				process=new HashMap<>();
				process.put("process_id", Long.parseLong(row.get("process_id").toString()));
				process.put("process_name", row.get("process_name"));
				process.put("process_description",row.get("process_description"));
				process.put("be_id",row.get("be_id")!=null?Long.parseLong(row.get("be_id").toString()):null);
				process.put("execution_order",row.get("execution_order")!=null?Long.parseLong(row.get("execution_order").toString()):null);
				result.add(process);
			}
			errorCode="SUCCESS";
			response.put("result",result);
		} catch(Exception e){
			message=e.getMessage();
			log.error(message);
			errorCode="FAILED";
		}
		
		response.put("message",message);
		response.put("errorCode",errorCode);
		return response;
	}


	@desc("Updates a Post Execution Process in a Business Entity: updates the description or execution order settings of the Post Execution Process.")
	@webService(path = "businessentity/{beId}/bename/{beName}/postexecutionprocess/{process_id}", verb = {MethodType.PUT}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsUpdatePostExecutionForBusinessEntity(@param(required=true) Long beId, @param(required=true) String beName, @param(required=true) Long process_id, String process_name, Integer execution_order, String process_description) throws Exception {
		String permissionGroup = fnGetUserPermissionGroup("");
		if (!"admin".equals(permissionGroup)) return wrapWebServiceResults("FAILED",admin_pg_access_denied_msg,null);
		
		HashMap<String,Object> response=new HashMap<>();
		String message=null;
		String errorCode="";
		
		try {
			String username = sessionUser().name();
			fnUpdateBusinessEntityDate(beId,username);
		} catch(Exception e){
			log.error(message);
		}
		
		try {
			String sql= "UPDATE \"" + schema + "\".TDM_BE_POST_EXE_PROCESS " +
			"SET process_name=(?), execution_order=(?), process_description=(?)" +
					"WHERE process_id = " + process_id;
		
			db("TDM").execute(sql,process_name, execution_order, process_description);
		
			try {
				String activityDesc = "Post Execution Order " + process_name + " was updated in business entity " + beName;
				fnInsertActivity("update", "Business entities", activityDesc);
			} catch(Exception e){
				log.error(e.getMessage());
			}
		
			errorCode="SUCCESS";
		
		} catch(Exception e){
			message=e.getMessage();
			log.error(message);
			errorCode="FAILED";
		}
		
		response.put("message",message);
		response.put("errorCode",errorCode);
		return response;
	}

	static void fnUpdateBusinessEntityDate(long beId,String username) throws Exception{
		String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
				.withZone(ZoneOffset.UTC)
				.format(Instant.now());
		String sql = "UPDATE \"" + schema + "\".business_entities " +
				"SET be_last_updated_date=(?)," +
				"be_last_updated_by=(?) " +
				"WHERE be_id = " + beId;
		db("TDM").execute(sql,now,username);
	}

	static void fnUpdateLogicalUnit(Map<String,Object> logicalUnit) throws Exception{
		String sql="UPDATE \"" + schema + "\".product_logical_units " +
				"SET lu_parent_id=(?), " +
				"lu_parent_name=(?), " +
				"lu_description=(?), " +
				"product_id=(?), " +
				"product_name=(?), " +
				"lu_dc_name=(?) " +
				"WHERE lu_id = " + logicalUnit.get("lu_id");
		String luParentId = null;
		String luParentName = null;
		String luDescription = null;
		String productId = "-1";
		String productName = null;
		String luDcName = null;
		
		if (logicalUnit.get("lu_parent_id") != null && !"".equalsIgnoreCase("" + logicalUnit.get("lu_parent_id"))) {
			luParentId = "" + logicalUnit.get("lu_parent_id");
		}
		
		if (logicalUnit.get("lu_parent_name") != null && !"".equalsIgnoreCase("" + logicalUnit.get("lu_parent_name"))) {
			luParentName = "" + logicalUnit.get("lu_parent_name");
		}
		
		if (logicalUnit.get("lu_description") != null && !"".equalsIgnoreCase("" + logicalUnit.get("lu_description"))) {
			luDescription = "" + logicalUnit.get("lu_description");
		}
		
		if (logicalUnit.get("product_id") != null && !"".equalsIgnoreCase("" + logicalUnit.get("product_id"))) {
			productId = "" + logicalUnit.get("product_id");
		}
		
		if (logicalUnit.get("product_name") != null && !"".equalsIgnoreCase("" + logicalUnit.get("product_name"))) {
			productName = "" + logicalUnit.get("product_name");
		}
		
		if (logicalUnit.get("lu_dc_name") != null && !"".equalsIgnoreCase("" + logicalUnit.get("lu_dc_name"))) {
			luDcName = "" + logicalUnit.get("lu_dc_name");
		}
		
		db("TDM").execute(sql,
				luParentId,
				luParentName,
				luDescription,
				productId,
				productName,
				luDcName);
	}


	static void fnAddLogicalUnits(List<Map<String,Object>> logicalUnits,long beId) throws Exception{
		for(Map<String,Object> logicalUnit:logicalUnits){
			Map<String,Object> luParent = (Map<String,Object>)logicalUnit.get("lu_parent");
			String sql = "INSERT INTO \"" + schema + "\".product_logical_units " +
					"(lu_name, lu_description, be_id, lu_parent_id, lu_parent_name, product_id,lu_dc_name) " +
					"VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING lu_id,lu_name";

			Db.Rows rows = db("TDM").fetch(sql, logicalUnit.get("lu_name"),
					logicalUnit.get("lu_description"),
					beId,
					(luParent !=null)? luParent.get("lu_id"):null,
					(luParent !=null)? luParent.get("logical_unit"):null,
					-1,
					logicalUnit.get("lu_dc_name"));
			logicalUnit.put("lu_id",rows.firstRow().get("lu_id")) ;
		}
		fn_updateParentLogicalUnits(logicalUnits);
	}

	static void fn_updateParentLogicalUnits(List<Map<String,Object>> logicalUnits) throws Exception{
		List<Map<String,Object>> updatedList=new ArrayList<>();

		for(Map<String,Object> logicalUnit:logicalUnits) {
			Map<String, Object> luParent = (Map<String, Object>) logicalUnit.get("lu_parent");
			if (logicalUnit != null && luParent != null && luParent.get("logical_unit")!=null && luParent.get("lu_id") == null) {
				updatedList.add(logicalUnit);
			}
		}

		if(updatedList.size()==0) return ;

		for(Map<String,Object> logicalUnit:updatedList){
			Map<String, Object> luParent = (Map<String, Object>) logicalUnit.get("lu_parent");
			Map<String,Object> temp=null;
			for(Map<String,Object> lu:logicalUnits){
				if(lu.get("lu_name")!=null&&luParent.get("logical_unit")!=null&&
						lu.get("lu_name").toString().equals(luParent.get("logical_unit").toString())){
					temp=lu;
					break;
				}
			}
			if (temp==null) break;
			luParent.put("lu_id",temp.get("lu_id")) ;
			String sql="UPDATE \"" + schema + "\".product_logical_units " +
					"SET lu_parent_id=(?)" +
					"WHERE lu_id = " + logicalUnit.get("lu_id");
			db("TDM").execute(sql,temp.get("lu_id"));
		}

	}

	static void fnDeleteLogicalUnit(long luId,long beId) throws Exception{
		String deleteLogicalUnitSql= "DELETE FROM \"" + schema + "\".product_logical_units WHERE lu_id = (?) RETURNING product_id";
		Db.Row row = db("TDM").fetch(deleteLogicalUnitSql,luId).firstRow();

		if(row.isEmpty()) return;
		String prodId = "" + row.get("product_id");
		if (prodId != null && !"-1".equals(prodId)) {
			
			String sql = "UPDATE \"public\".environment_products " +
					"SET status= (?) " +
					"WHERE \"public\".environment_products.status = \'Active\' AND \"public\".environment_products.product_id = " + prodId +
					" AND (select count(\"public\".product_logical_units.product_id) " +
					"FROM \"public\".product_logical_units " +
					"WHERE \"public\".product_logical_units.product_id = " + prodId + ") = 0 RETURNING product_id";
			
			row = db("TDM").fetch(sql, "Inactive").firstRow();

			if (!row.isEmpty()) {
				prodId = "" + row.get("product_id");
				 sql = "WITH src AS ( " +
						"UPDATE \"" + schema + "\".tasks_products " +
						"SET task_product_status = (?) " +
						"WHERE \"" + schema + "\".tasks_products.product_id = " + prodId + " " +
						"RETURNING \"" + schema + "\".tasks_products.task_id ) " +
						" " +
						"UPDATE \"" + schema + "\".tasks SET task_status = (?) " +
						"FROM ( SELECT \"" + schema + "\".tasks_products.task_id FROM \"" + schema + "\".tasks_products " +
						"WHERE \"" + schema + "\".tasks_products.task_product_status = \'Active\' " +
						"GROUP BY \"" + schema + "\".tasks_products.task_id " +
						"HAVING COUNT(*) = 1 " +
						"INTERSECT " +
						"SELECT \"" + schema + "\".tasks_products.task_id " +
						"FROM \"" + schema + "\".tasks_products " +
						"WHERE \"" + schema + "\".tasks_products.product_id = " + prodId + " ) AS sq " +
						"WHERE \"" + schema + "\".tasks.task_id = sq.task_id AND \"" + schema + "\".tasks.task_status = \'Active\'";
				db("TDM").fetch(sql, "Inactive", "Inactive");
			}
		}

	}

	@desc("Gets the list of the available parameters for a task based on the task's Business Entity (BE) and source environment.")
	@webService(path = "businessentity/{beId}/sourceEnv/{src_env_name}/parameters", verb = {MethodType.GET}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON})
	@resultMetaData(mediaType = Produce.JSON, example = "{\r\n" +
			"  \"result\": {\r\n" +
			"    \"PATIENT_LU.INSURANCE_PLAN\": {\r\n" +
			"      \"BE_ID\": \"2\",\r\n" +
			"      \"LU_NAME\": \"PATIENT_LU\",\r\n" +
			"      \"PARAM_NAME\": \"PATIENT_LU.INSURANCE_PLAN\",\r\n" +
			"      \"PARAM_TYPE\": \"combo\",\r\n" +
			"      \"VALID_VALUES\": [\r\n" +
			"        \"BASIC\",\r\n" +
			"        \"GOLD\",\r\n" +
			"        \"PLATINUM\",\r\n" +
			"        \"SILVER\"\r\n" +
			"      ],\r\n" +
			"      \"MIN_VALUE\": \"\\\\N\",\r\n" +
			"      \"MAX_VALUE\": \"\\\\N\",\r\n" +
			"      \"LU_PARAMS_TABLE_NAME\": \"patient_lu_params\"\r\n" +
			"    },\r\n" +
			"    \"PATIENT_LU.PAYMENT_ISSUED_DATE\": {\r\n" +
			"      \"BE_ID\": \"2\",\r\n" +
			"      \"LU_NAME\": \"PATIENT_LU\",\r\n" +
			"      \"PARAM_NAME\": \"PATIENT_LU.PAYMENT_ISSUED_DATE\",\r\n" +
			"      \"PARAM_TYPE\": \"text\",\r\n" +
			"      \"VALID_VALUES\": \"\\\\N\",\r\n" +
			"      \"MIN_VALUE\": \"\\\\N\",\r\n" +
			"      \"MAX_VALUE\": \"\\\\N\",\r\n" +
			"      \"LU_PARAMS_TABLE_NAME\": \"patient_lu_params\"\r\n" +
			"    },\r\n" +
			"    \"PATIENT_LU.INV_BALANCE\": {\r\n" +
			"      \"BE_ID\": \"2\",\r\n" +
			"      \"LU_NAME\": \"PATIENT_LU\",\r\n" +
			"      \"PARAM_NAME\": \"PATIENT_LU.INV_BALANCE\",\r\n" +
			"      \"PARAM_TYPE\": \"number\",\r\n" +
			"      \"VALID_VALUES\": \"\\\\N\",\r\n" +
			"      \"MIN_VALUE\": 6,\r\n" +
			"      \"MAX_VALUE\": 30000,\r\n" +
			"      \"LU_PARAMS_TABLE_NAME\": \"patient_lu_params\"\r\n" +
			"    },\r\n" +
			"    \"PATIENT_LU.CITY\": {\r\n" +
			"      \"BE_ID\": \"2\",\r\n" +
			"      \"LU_NAME\": \"PATIENT_LU\",\r\n" +
			"      \"PARAM_NAME\": \"PATIENT_LU.CITY\",\r\n" +
			"      \"PARAM_TYPE\": \"text\",\r\n" +
			"      \"VALID_VALUES\": \"\\\\N\",\r\n" +
			"      \"MIN_VALUE\": \"\\\\N\",\r\n" +
			"      \"MAX_VALUE\": \"\\\\N\",\r\n" +
			"      \"LU_PARAMS_TABLE_NAME\": \"patient_lu_params\"\r\n" +
			"    }\r\n" +
			"  },\r\n" +
			"  \"errorCode\": \"SUCCESS\"\r\n" +
			"}")
	public static Object wsGetParametersForBusinessEntity(@param(required=true) String src_env_name, @param(required=true) Long beId) throws Exception {
		HashMap<String,Object> response=new HashMap<>();
		try {
			Object result = fnGetListOfParamsForBE(beId.toString(),src_env_name);
			response.put("result",((Map<String,Object>)result).get("result"));
			response.put("errorCode","SUCCESS");
		} catch (Exception e){
			response.put("errorCode","FAILED");
			response.put("message", e.getMessage());
		}
		return response;
	}


	@desc("Get the list of active Business Entities. This API is used when creating or editing a TDM task to get the list of available Business Entities for the task.")
	@webService(path = "getActiveBusinessentities", verb = {MethodType.GET}, version = "1", isRaw = false, isCustomPayload = false, produce = {Produce.XML, Produce.JSON}, elevatedPermission = true)
	@resultMetaData(mediaType = Produce.JSON, example = "\"result\": [\r\n" +
			"    {\r\n" +
			"      \"be_id\": 1,\r\n" +
			"      \"be_name\": \"BE\"\r\n" +
			"    },\r\n" +
			"    {\r\n" +
			"      \"be_id\": 3,\r\n" +
			"      \"be_name\": \"bb\"\r\n" +
			"    }\r\n" +
			"  ],\r\n" +
			"  \"errorCode\": \"SUCCESS\",\r\n" +
			"  \"message\": null\r\n" +
			"}")
	public static Object wsGetActiveBusinessentities() throws Exception {
		String sql = "SELECT be_id, be_name FROM business_entities WHERE be_status = 'Active'";
		String errorCode="";
		String message=null;
		
		try{
			Db.Rows rows = db("TDM").fetch(sql);
			errorCode= "SUCCESS";
			List<Map<String,Object>> result=new ArrayList<>();
			Map<String,Object> businessEntity;
			for(Db.Row row:rows) {
				businessEntity=new HashMap<String,Object>();
				businessEntity.put("be_id",Integer.parseInt(row.get("be_id").toString()));
				businessEntity.put("be_name", row.get("be_name"));
				result.add(businessEntity);
			}
			return wrapWebServiceResults(errorCode,message,result);
		}
		catch(Exception e){
			errorCode= "FAILED";
			message= e.getMessage();
			log.error(message);
			return wrapWebServiceResults(errorCode,message,null);
		}
				
	}

	//from logic.TDM
	private static Object fnGetListOfParamsForBE(String beID, String sourceEnvName) throws Exception {
		final String env = Util.isEmpty(sourceEnvName) ? "_dev" : sourceEnvName;

		Db tdmDB = db("TDM");
		Db.Rows luRes = tdmDB.fetch(LU_SQL, beID);

		Map<String, Iterable<Db.Row>> metaDataMap = new HashMap<>();
		for(Db.Row luRow : luRes){
			ResultSet resultSet = luRow.resultSet();
			String logicalUnitName = resultSet.getString("logicalunitname");
			Db.Rows luParamsRes = tdmDB.fetch("SELECT distinct column_name  FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ?", logicalUnitName.toLowerCase() + "_params");
			metaDataMap.put(logicalUnitName, luParamsRes.getResults());
		}
		Map<String, Map<String, Object>> beParametersColumnTypes = new HashMap<>();

		metaDataMap.forEach((luName, colNames) -> {
			StreamSupport.stream(colNames.spliterator(), false).map(col -> Util.rte(() -> col.resultSet().getString("column_name"))).filter(col -> !col.equals("be_id")).filter(col -> !col.equals("entity_id")).filter(col -> !col.equals("source_environment")).forEach(colName -> {
				String[] columnNameArr = colName.split("\\.");
				String logicalUnitName = columnNameArr[0].toUpperCase();
				String beTableName = logicalUnitName + "_PARAMS";
				String colNameUpper = colName.toUpperCase();

				String columnDistinctValueSQL = "SELECT COUNT(DISTINCT \"" + colName + "\") AS \"" + colName + "\" FROM (" +
						"SELECT UNNEST(\"" + colName + "\"::text[]) AS \"" + colName + "\" FROM " + beTableName + " WHERE source_environment='" + env + "') tempTable";

				long columnDistinctValue = Util.rte(() -> (Long) tdmDB.fetch(columnDistinctValueSQL).firstValue());

				// Initiate the parameter that will hold the distinct list of values for the column
				List<String> columnDistinctValues = new ArrayList<>();
				if (columnDistinctValue > 0 && columnDistinctValue <= Integer.parseInt(COMBO_MAX_COUNT)) {
					// Prepare the SQL Statement to retrieve the distinct list of values for that column
					String columnDistinctListSQL = "SELECT DISTINCT UNNEST(\"" + colName + "\"::text[]) AS \"" + colName + "\" FROM " + beTableName + " WHERE source_environment='" + env + "'";

					Iterator<Db.Row> iter = Util.rte(() -> tdmDB.fetch(columnDistinctListSQL).getResults().iterator());
					while (iter.hasNext()) {
						Util.rte(() -> columnDistinctValues.add(iter.next().resultSet().getString(colName)));
					}
					columnDistinctValues.sort(String::compareTo);
					beParametersColumnTypes.put(colNameUpper, Util.map(BE_ID, beID, LU_NAME, luName, PARAM_NAME, colNameUpper, PARAM_TYPE, PARAM_TYPES.COMBO.getName(), VALID_VALUES, columnDistinctValues, MIN_VALUE, "\\N", MAX_VALUE, "\\N", LU_PARAMS_TABLE_NAME, luName.toLowerCase() + "_params"
					));

				} else {
					int colNum = getColNumber(env, tdmDB, colName, beTableName);

					if (colNum == 1) {
						// In case it is, prepare the query to bring the column's min and max values
						String columnMinMaxSQL = getColumnMinMaxSQL(env, colName, beTableName);

						Db.Row columnMinMaxRow = Util.rte(() -> tdmDB.fetch(columnMinMaxSQL).firstRow());
						beParametersColumnTypes.put(colNameUpper, Util.map(BE_ID, beID, LU_NAME, luName, PARAM_NAME, colNameUpper, 
							PARAM_TYPE, PARAM_TYPES.NUMBER.getName(), VALID_VALUES, "\\N", MIN_VALUE, 
							Util.rte(() -> columnMinMaxRow.cell(0)), MAX_VALUE, Util.rte(() -> columnMinMaxRow.cell(1)), LU_PARAMS_TABLE_NAME, luName.toLowerCase() + "_params"
						));
					} else {
						// Mark it as text
						beParametersColumnTypes.put(colNameUpper, Util.map(BE_ID, beID, LU_NAME, luName, PARAM_NAME, colNameUpper, PARAM_TYPE, PARAM_TYPES.TEXT.getName(), VALID_VALUES, "\\N", MIN_VALUE, "\\N", MAX_VALUE, "\\N", LU_PARAMS_TABLE_NAME, luName.toLowerCase() + "_params"
						));
					}
				}
			});
		});
		return wrapWebServiceResults("SUCCESS", null, beParametersColumnTypes);
	}



	@NotNull
	private static String getColumnMinMaxSQL(String env, String colName, String beTableName) {
		return "SELECT MIN(CAST(\"" + colName + "\" AS DOUBLE PRECISION)) AS \"MIN_" + colName + "\", MAX(CAST(\"" + colName + "\" AS DOUBLE PRECISION)) AS \"MAX_" + colName + "\" FROM (SELECT DISTINCT UNNEST(\"" + colName + "\"::text[]) AS \"" + colName + "\" FROM " + beTableName + " WHERE source_environment='" + env + "') tempTable";
	}

	private static Integer getColNumber(String env, Db tdmDB, String colName, String beTableName) {
		return Util.rte(() -> {
			Object value = tdmDB.fetch("SELECT DISTINCT CASE WHEN CAST(\"" + colName + "\" AS TEXT) ~ '^[0-9.]*$' THEN 1 ELSE 0 END isNumber FROM (SELECT DISTINCT UNNEST(\"" + colName + "\"::text[]) AS \"" + colName + "\" FROM " + beTableName + " WHERE source_environment='" + env + "') tempTable ORDER BY  1 ASC LIMIT 1").firstValue();
			return value != null ? (int) value: 0;
		});
	}
	//end from logic.TDM


	static void fnUpdateProductDate(long prodId,String username) throws Exception{
		String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
				.withZone(ZoneOffset.UTC)
				.format(Instant.now());

		String sql = "UPDATE \"" + schema + "\".products SET " +
				"product_last_updated_date=(?)," +
				"product_last_updated_by=(?) " +
				"WHERE product_id = " + prodId;
		db("TDM").execute(sql,now,username);
	}

	static void fnInsertActivity(String action,String entity,String description) throws Exception{
		String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
				.withZone(ZoneOffset.UTC)
				.format(Instant.now());
		String username = sessionUser().name();
		String userId = username;
		String sql= "INSERT INTO \"" + schema + "\".activities " +
				"(date, action, entity, user_id, username, description) " +
				"VALUES (?, ?, ?, ?, ?, ?)";
		db("TDM").execute(sql,now,action,entity,userId,username,description);
	}

}
