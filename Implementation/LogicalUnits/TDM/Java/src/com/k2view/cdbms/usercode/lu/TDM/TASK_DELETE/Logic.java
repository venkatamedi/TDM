/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.TDM.TASK_DELETE;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

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
import com.k2view.fabric.events.*;
import com.k2view.fabric.fabricdb.datachange.TableDataChange;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@desc("14-Mar-19- add a check if the has a debug mode. Do not delete from source for debug mode.")
	public static void fnDelTaskFromSource() throws Exception {
		// Clean TDM execution tables from TDMDB after the data is populated in TDM LU
		// For TEST mode- you can comment this delete
		String instID = getInstanceID();
		if(!inDebugMode()){
			//db("TDM").execute("Delete from task_execution_entities where task_execution_id = ?", instID);
			//db("TDM").execute("Delete from tdm_seq_mapping where task_execution_id = ?", instID);
			//db("TDM").execute("Delete from task_execution_entities where task_execution_id = ?", instID);
			//db("TDM").execute("Delete from tdm_seq_mapping where task_execution_id = ?", instID);
			//db("TDM").execute("Delete from TASK_EXE_STATS_DETAILED where task_execution_id = ?", instID);
			//db("TDM").execute("Delete from TASK_EXE_ERROR_DETAILED where task_execution_id = ?", instID);
		}		
	}


	@desc("25-Sep-19- new function- update the TDM DB - task_execution_list set synced_to_fabric = TRUE. Originally this was done by a parser.")
	public static void fnUpdateTaskSyncStatus() throws Exception {
		if(!inDebugMode()) {
			String sql = "update task_execution_list set synced_to_fabric = TRUE where task_execution_id = ? ";
			db("TDM").execute(sql, ludb().fetch("SELECT IID('TDM')").firstValue());
		}
	}
	
}
