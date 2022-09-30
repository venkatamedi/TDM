/////////////////////////////////////////////////////////////////////////
// LU Globals
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.TDM_LIBRARY;

import com.k2view.cdbms.usercode.common.SharedGlobals;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;

public class Globals extends SharedGlobals {

	@desc("The name of the main source LU table or tables. You can populate several tables separated by a comma.")
	@category("TDM")
	public static final String ROOT_TABLE_NAME = "";

	@desc("The column name of the entity id. The name and the order of the root column name must be aligned with root_table_name. For example- if you populate two tables in root_Table_name (separated by a comma), you must populate also two values in this global where the first column referes to the first table name the second column refers to the second table name")
	@category("TDM")
	public static final String ROOT_COLUMN_NAME = "";

	


}
