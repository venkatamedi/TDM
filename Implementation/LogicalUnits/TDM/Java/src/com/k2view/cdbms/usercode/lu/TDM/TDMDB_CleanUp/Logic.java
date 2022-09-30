/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.TDM.TDMDB_CleanUp;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

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

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.TDM.Globals.*;
import com.k2view.fabric.fabricdb.datachange.TableDataChange;
import com.k2view.fabric.events.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@desc("The function will be  called by user job to clean up the TDMDB tables\r\n" +
			" based on retention period given in TDMDB table tdm_general_parameters")
	@type(UserJob)
	public static void TDMDB_CleanUp() throws Exception {
		String paramName = "";
		String paramValue = "";
		String retentionDate = "";
		
		//Set the SQL parameter
		String queryString = "SELECT param_value FROM tdm_general_parameters where UPPER(param_name) = 'CLEANUP_RETENTION_PERIOD'";
		
		//Execute the query
		String retentionPeriod = "" + db("TDM").fetch(queryString).firstValue();
		
		if (retentionPeriod == null || "null".equalsIgnoreCase(retentionPeriod) || "".equals(retentionPeriod)) {
			throw new Exception("tdm_general_parameters is missing parameter for CleanUp process");
		}
		
		queryString = "SELECT date_trunc('day', NOW() - interval ' " + retentionPeriod + " MONTH')";
		
		retentionDate = "" + db("TDM").fetch(queryString).firstValue();
		
		//log.info("retentionDate = " + retentionDate);		
		//Loop on the translation table and run each delete statement
		// Rebuild the translation table as sorted map to delete the tables in the right order
		Map<String,Map<String, String>> trnClnUpListValues = getTranslationsData("trnTDMCleanUp");
		SortedMap<String, Map<String, String>> trnClnUpSortedMap = new TreeMap<>();
		
		for(String index: trnClnUpListValues.keySet()){
			//log.info("TDMDB_CleanUp - index: " + index + ", tableOrder: " + index.substring(0, index.indexOf("@")) + 
			//		", tableSeq: " + index.substring(index.indexOf("@") + 3));
			int tableOrder = Integer.parseInt(index.substring(0, index.indexOf("@")));
			int tableSeq = Integer.parseInt(index.substring(index.indexOf("@") + 3));
			String key = String.format("%02d%02d",tableOrder, tableSeq );
			//log.info("TDMDB_CleanUp - New Key" + key);
			trnClnUpSortedMap.put(key, trnClnUpListValues.get(index));
		}
		
		for(String index: trnClnUpSortedMap.keySet()){
			Map<String, String> valMap = trnClnUpSortedMap.get(index);
			
			String active = valMap.get("CLEANUP_IND");
			//If the entry in translation table is inactive, then the statement should not run, and continue to the next one
			if(active.equalsIgnoreCase("false")){
				log.warn("TDMDB_CleanUp - Disabled table: " + 
								  valMap.get("TABLE_NAME") + ", will not be cleaned up");
				continue;
			}
			String statement = valMap.get("CLEANUP_STATEMENT");
				
			//count the number of ? in the statement, to add the retention date to all of them
			int  numOfParams = StringUtils.countMatches(statement, "?");
			Object[] queryParam = new Object[numOfParams];
			for (int i = 0; i < numOfParams; i++) {
				queryParam[i] = retentionDate;
			}
		
			//Execute the query	
			try {
				//log.info("Running Statement: " + statement);
				db("TDM").execute(statement, queryParam);
			} 
			catch (Exception e) {
				
				log.error("TDMDB_Cleanup - Failed to clean Table: " + 
			              valMap.get("TABLE_NAME") + " with Retention Date: " + retentionDate, e);
				continue;
			}
		}
	}
	
}
