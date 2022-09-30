/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.TDM_LIBRARY.ParserExecuteJob;

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
import com.k2view.cdbms.usercode.lu.TDM_LIBRARY.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.TDM_LIBRARY.Globals.*;
import com.k2view.fabric.fabricdb.datachange.TableDataChange;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {

	public static final String DB_FABRIC = "fabric";
	
	@type(RootFunction)
	@out(name = "parserName", type = String.class, desc = "")
	@out(name = "currSplitNumber", type = String.class, desc = "")
	public static void fnSplitParser(String luName, Integer engine) throws Exception {
		Set<String> parList = new HashSet<String>();
		int NoOfSplitsTotal = 0;
			int parsersInProcess = 0;
			boolean failedInSelect = false;
			StringJoiner parserInClause = new StringJoiner("','", "'", "'");
			List<com.k2view.cdbms.lut.map.ParserMap> list = getLuType().ludbParserMap.values().stream().filter(com.k2view.cdbms.lut.map.ParserMap::isEnabled).distinct().collect(java.util.stream.Collectors.toList());
			for(com.k2view.cdbms.lut.map.ParserMap parserMap :list) {
				String parserName = parserMap.name;
				String longParserName = luName + "-" + parserName;
				java.util.Map <String,String> rsFromTrn = getTranslationValues("trnParsList", new Object[]{parserName});
				if(rsFromTrn.get("ind") == null){
					continue;
				}
				
				int NoOfSplits = Integer.valueOf(rsFromTrn.get("num_of_spits"));
				NoOfSplitsTotal = NoOfSplitsTotal + NoOfSplits;
				parserInClause.add(longParserName);
				for(int i=0 ; i< NoOfSplits;i++) {
					String currSplitNumber = String.valueOf(i);
					Map<String, String> args = new HashMap<>();
					args.put("NoOfSplits", String.valueOf(NoOfSplits));
					args.put("currSplitNumber",currSplitNumber );
					//startParser(luName, parserName, args);
					fabric().execute("startparser " + luName + " " + parserName + " ARGS='" + args + "'");
					parList.add(luName + "-" + parserName);
					
					do {
						try {
							failedInSelect = false;
							parsersInProcess = 0;
							Thread.sleep(10000);
		
							// TDM 5.1- support multi fabric clusters on the same Cassandra
							// if the cluster ID is define, add it to the name of the keyspace.
							String countSql ="";
							//String clusterID = (String)DBSelectValue(DB_FABRIC,"clusterid",null);
							String clusterID = "" + fabric().fetch("clusterid").firstValue();
							if (clusterID == null || clusterID.isEmpty()) {
								countSql ="select count(status) from k2system.k2_jobs where name in (" +
										parserInClause.toString() + ") and status = 'IN_PROCESS' and type='PARSER' allow filtering";
							} else {
								countSql ="select count(status) from k2system_" + clusterID +
										".k2_jobs where name in (" + parserInClause.toString() + ") and status = 'IN_PROCESS' and type='PARSER' allow filtering";
							}
		
				    		//String countResult = DBSelectValue("DB_CASSANDRA",countSql , null) + "";
							String countResult = "" + db("DB_CASSANDRA").fetch(countSql).firstValue();
							parsersInProcess = Integer.parseInt(countResult);
						} catch (Exception e) {
							log.error("fnSplitParser", e);
							failedInSelect = true;
						}
					}
					while(parsersInProcess >= engine || failedInSelect);
					
				}
			}
		yield(null);
	}

	
	
	
	
}
