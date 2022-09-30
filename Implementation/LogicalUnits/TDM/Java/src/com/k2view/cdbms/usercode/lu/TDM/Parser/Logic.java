/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.TDM.Parser;

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

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "o_Task_Execution_ID", type = Integer.class, desc = "")
	@out(name = "o_ETL_Execution_ID", type = Integer.class, desc = "")
	@out(name = "o_Creation_Date", type = String.class, desc = "")
	@out(name = "o_LU_Name", type = String.class, desc = "")
	@out(name = "o_Env_ID", type = String.class, desc = "")
	@out(name = "o_Entity_Id", type = String.class, desc = "")
	@out(name = "o_ID_Type", type = String.class, desc = "")
	@out(name = "o_Execution_Status", type = String.class, desc = "")
	@out(name = "o_Entity_start_time", type = String.class, desc = "")
	@out(name = "o_Entity_end_time", type = String.class, desc = "")
	@out(name = "o_Be_root_entity_id", type = String.class, desc = "")
	public static void fun_createTdmTaskExecutionEntities(Integer i_dummy) throws Exception {
		Object[] row = {null};
		yield(row);
	}

	
	
	
	
}
