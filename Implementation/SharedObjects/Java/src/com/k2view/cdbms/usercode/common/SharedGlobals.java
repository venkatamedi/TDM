/////////////////////////////////////////////////////////////////////////
// Shared Globals
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.common;

import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;

public class SharedGlobals {




	@desc("Maximum values of combo box input object")
	@category("TDM")
	public static String COMBO_MAX_COUNT = "49";


	@desc("Indicator to delete the instance to target DB")
	@category("TDM")
	public static String TDM_DELETE_BEFORE_LOAD = "false";
	@desc("Indicator to insert the instance to target DB")
	@category("TDM")
	public static String TDM_INSERT_TO_TARGET = "false";

	@category("TDM")
	public static String TDM_SYNC_SOURCE_DATA = "true";


	@desc("Target product version to override by task execution process")
	@category("TDM")
	public static String TDM_TARGET_PRODUCT_VERSION = "false";
	@desc("Source product version to override by task execution process")
	@category("TDM")
	public static String TDM_SOURCE_PRODUCT_VERSION = "false";

	@category("TDM")
	public static String TDM_REPLACE_SEQUENCES = "false";

	@category("TDM")
	public static String TDM_TASK_EXE_ID = "0";

	@category("TDM")
	public static String MASK_FLAG = "0";
	@category("TDM")
	public static String TDM_SOURCE_ENVIRONMENT_NAME = "SRC";
	@category("TDM")
	public static String TDM_TAR_ENV_NAME = "TAR";
	@category("TDM")
	public static String TDM_SYNTHETIC_DATA = "false";
	@category("TDM")
	public static String TDM_TASK_ID = "0";
	@category("TDM")
	public static String TDM_VERSION_DATETIME = "19700101000000";
	@category("TDM")
	public static String TDM_VERSION_NAME = "";


	@desc("Indicator to mark the task as dataflux or not")
	@category("TDM")
	public static String TDM_DATAFLUX_TASK = "false";

	@desc("The TTL for external entity list for non jdbc source DB")
	@category("TDM")
	public static String TDM_EXTERNAL_ENTITY_LIST_TTL = "2592000";

	@desc("The maximum number of entities to be returned to be displayed in list of entities")
	@category("TDM")
	public static String MAX_NUMBER_OF_ENTITIES_IN_LIST = "100";

	@desc("Each Instance can have a TTL, this global holds the type of the TTL and it can have one of the following values:Minutes, Hours, Days, Weeks, or Years")
	@category("TDM")
	public static String TDM_LU_RETENTION_PERIOD_TYPE = "Days";
	@desc("The value of the TTL based on the type defined in TDM_LU_RETENTION_PERIOD_TYPE. Populate this global with zero or empty value to avoid setting a TTL on the TDM LUIs.")
	@category("TDM")
	public static String TDM_LU_RETENTION_PERIOD_VALUE = "10";


	@desc("The max number of retrieved entities from TDM_RESERVED_ENTITIES, if set to zero then no limit")
	@category("TDM")
	public static String GET_RESERVED_ENTITIES_LIMIT = "0";

	@category("TDM_DEBUG")
	public static String USER_NAME = "admin";
	@category("TDM_DEBUG")
	public static String USER_FABRIC_ROLES = "admin";
	@category("TDM_DEBUG")
	public static String USER_PERMISSION_GROUP = "admin";
	@category("TDM_DEBUG")
	public static String TDM_RESERVE_IND = "false";
	@category("TDM_DEBUG")
	public static String RESERVE_RETENTION_PERIOD_TYPE = "Days";
	@category("TDM_DEBUG")
	public static String RESERVE_RETENTION_PERIOD_VALUE = "10";
	@category("TDM_DEBUG")
	public static String BE_ID = "0";
	@category("TDM_DEBUG")
	public static String TASK_TYPE = "EXTRACT";
	@category("TDM_DEBUG")
	public static String enable_masking = "true";
	@category("TDM_DEBUG")
	public static String enable_sequences = "false";

	@category("TDM")
	public static String TDM_REF_UPD_SIZE = "10000";

}
