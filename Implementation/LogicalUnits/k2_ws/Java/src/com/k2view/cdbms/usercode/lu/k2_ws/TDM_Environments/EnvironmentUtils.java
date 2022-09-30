package com.k2view.cdbms.usercode.lu.k2_ws.TDM_Environments;

import com.k2view.cdbms.FabricEncryption.FabricEncryption;
import com.k2view.cdbms.shared.Db;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.fabric.common.Log;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.k2view.cdbms.shared.user.UserCode.*;
import static com.k2view.cdbms.usercode.common.TDM.TdmSharedUtils.*;
import static com.k2view.cdbms.usercode.lu.k2_ws.TDM_Permissions.Logic.wsGetFabricRolesByUser;

@SuppressWarnings({"unused", "DefaultAnnotationParam", "unchecked"})
public class EnvironmentUtils {
    static final Log log = Log.a(UserCode.class);
    static final String schema = "public";

    static String fnGetInterval(String interval) {
        if ("Day".equals(interval)) {
            return "1 day";
        } else if ("Week".equals(interval)) {
            return "1 week";
        } else if ("Month".equals(interval)) {
            return "1 month";
        } else if ("3Month".equals(interval)) {
            return "3 month";
        } else if ("Year".equals("interval")) {
            return "1 year";
        } else {
            return "1 month";
        }
    }

    static Object fnExtractExecutionStatus(Map<String, List<Map<String, Object>>> executionsStatusGroup) {
        Map<String, Integer> executionsStatus = new HashMap<>();
        executionsStatus.put("failed", 0);
        executionsStatus.put("pending", 0);
        executionsStatus.put("paused", 0);
        executionsStatus.put("stopped", 0);
        executionsStatus.put("running", 0);
        executionsStatus.put("completed", 0);

        out:
        for (Map.Entry<String, List<Map<String, Object>>> entry : executionsStatusGroup.entrySet()) {
            List<Map<String, Object>> group = entry.getValue();
            for (Map<String, Object> execution : group) {
                if ("FAILED".equals(execution.get("execution_status").toString().toUpperCase())) {
                    executionsStatus.put("failed", executionsStatus.get("failed") + 1);
                    continue out;
                }
            }
            for (Map<String, Object> execution : group) {
                if ("PENDING".equals(execution.get("execution_status").toString().toUpperCase())) {
                    executionsStatus.put("pending", executionsStatus.get("pending") + 1);
                    continue out;
                }
            }
            for (Map<String, Object> execution : group) {
                if ("PAUSED".equals(execution.get("execution_status").toString().toUpperCase())) {
                    executionsStatus.put("paused", executionsStatus.get("paused") + 1);
                    continue out;
                }
            }
            for (Map<String, Object> execution : group) {
                if ("stopped".equals(execution.get("execution_status").toString().toUpperCase())) {
                    executionsStatus.put("stopped", executionsStatus.get("stopped") + 1);
                    continue out;
                }
            }

            Boolean runningFound = false;
            for (Map<String, Object> execution : group) {
                if (execution.get("execution_status") == null) continue;
                if ("RUNNING".equals(execution.get("execution_status").toString().toUpperCase()) || "EXECUTING".equals(execution.get("execution_status").toString().toUpperCase()) ||
                        "STARTED".equals(execution.get("execution_status").toString().toUpperCase()) || "STARTEXECUTIONREQUESTED".equals(execution.get("execution_status").toString())) {
                    executionsStatus.put("running", executionsStatus.get("running") + 1);
                    runningFound = true;
                    break;
                }
            }
            if (runningFound == true) {
                continue out;
            }

            executionsStatus.put("completed", executionsStatus.get("completed") + 1);
        }

        return executionsStatus;
    }

    static void fnUpdateEnvironmentDate(Long envId) {
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        try {
            String sql = "UPDATE \"" + schema + "\".environments SET " +
                    "environment_last_updated_date=(?)," +
                    "environment_last_updated_by=(?) " +
                    "WHERE environment_id = " + envId;
            String username = sessionUser().name();
            db("TDM").execute(sql, now, username);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    static Long fnAddProcutToEnvironment(long envId, Long product_id, String data_center_name, String product_version) throws Exception {
        String sql = "INSERT INTO \"" + schema + "\".environment_products " +
                "(environment_id, product_id, data_center_name, product_version, created_by, creation_date, last_updated_date, last_updated_by, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING environment_product_id";
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        String username = sessionUser().name();
        Db.Row row = db("TDM").fetch(sql,
                envId,
                product_id,
                data_center_name,
                product_version,
                username,
                now,
                now,
                username,
                "Active").firstRow();
        return Long.parseLong(row.get("environment_product_id").toString());
    }

    static void fnAddProductInterfacesEnvironment(Long envId, Long product_id, Long env_product_id, List<Map<String, Object>> interfaces) throws Exception {
        if (interfaces == null) return;

        for (Map<String, Object> _interface : interfaces) {
            //k2viewAPIs.getDataFromAPI("envEncryptDBConnPwd",[{"key" : "password", "value" : interface.db_password"}]
            String decryptPwd = FabricEncryption.decrypt(_interface.get("db_password").toString());
            //replace regular text password with encrypted password
            _interface.put("db_password", decryptPwd);

            String sql = "INSERT INTO \"" + schema + "\".environment_product_interfaces " +
                    "(environment_id, product_id, db_host, db_port, db_user, db_password, db_schema, db_connection_string,env_product_interface_id,interface_name,interface_type,env_product_interface_status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?)";
            db("TDM").execute(sql,
                    envId,
                    product_id,
                    _interface.get("db_host"),
                    _interface.get("db_port"),
                    _interface.get("db_user"),
                    _interface.get("db_password"),
                    _interface.get("db_schema"),
                    _interface.get("db_connection_string"),
                    env_product_id,
                    _interface.get("interface_name"),
                    _interface.get("interface_type"),
                    "Active");
        }
    }

    static void fnUpdateProductToEnvironment(Long environment_product_id, String data_center_name, String product_version) throws Exception {
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        String sql = "UPDATE \"" + schema + "\".environment_products SET " +
                "data_center_name=(?)," +
                "product_version=(?)," +
                "last_updated_date=(?)," +
                "last_updated_by=(?) " +
                "WHERE environment_product_id = " + environment_product_id;
        String username = sessionUser().name();
        db("TDM").execute(sql, data_center_name, product_version, now, username);
    }

    static void fnUpdateProductInterfacesEnvironment(Long envId, long product_id, Long env_product_id, List<Map<String, Object>> interfaces) throws Exception {
        if (interfaces == null) return;
        for (Map<String, Object> _interface : interfaces) {
            if (_interface.get("update").toString() == "false" || _interface.get("passwordDecrypt").toString() != "false") {
                String decryptPwd = FabricEncryption.decrypt(_interface.get("db_password").toString());
                //replace regular text password with encrypted password
                _interface.put("db_password", decryptPwd);
            }
            //check: env_product_interface_status
            if (_interface.get("newInterface").toString() == "true" && _interface.get("env_product_interface_status").toString() == "null") {
                String sql = "INSERT INTO \"" + schema + "\".environment_product_interfaces " +
                        "(environment_id, product_id, db_host, db_port, db_user, db_password, db_schema, db_connection_string,env_product_interface_id,interface_name,interface_type,env_product_interface_status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?)";
                db("TDM").execute(sql, envId, product_id, _interface.get("db_host"),
                        _interface.get("db_port"), _interface.get("db_user"),
                        _interface.get("db_password"), _interface.get("db_schema"),
                        _interface.get("db_connection_string"), env_product_id,
                        _interface.get("interface_name"), _interface.get("interface_type"), "Active");
            } else if (_interface.get("deleted").toString() == "true") {
                String sql = "DELETE FROM \"" + schema + "\".environment_product_interfaces WHERE ( env_product_interface_id = (?) AND interface_name = (?))";
                db("TDM").execute(sql, _interface.get("env_product_interface_id"), _interface.get("interface_name"));

            } else {
                String sql = "UPDATE \"" + schema + "\".environment_product_interfaces SET " +
                        "db_host=(?)," +
                        "db_port=(?)," +
                        "db_user=(?)," +
                        "db_password=(?)," +
                        "db_schema=(?)," +
                        "db_connection_string=(?) " +
                        "WHERE env_product_interface_id = " + _interface.get("env_product_interface_id") + " AND interface_name = " + "\"" + _interface.get("interface_name") + "\"";
                db("TDM").execute(sql,
                        _interface.get("db_host"), _interface.get("db_port"),
                        _interface.get("db_user"), _interface.get("db_password"),
                        _interface.get("db_schema"), _interface.get("db_connection_string"));
            }
        }
    }


    static void fnUpdateEnvironmentRolesPermissions(Long environment_id, String type, boolean value) {
        try {
            String sql = "UPDATE \"" + schema + "\".environment_roles SET " +
                    "" + type + "=(?)" +
                    "WHERE environment_id = " + environment_id;
            db("TDM").execute(sql, value);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    static void fnInsertActivity(String action, String entity, String description) throws Exception {
        String userId = sessionUser().name();
        String username = userId;
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        String sql = "INSERT INTO \"" + schema + "\".activities " +
                "(date, action, entity, user_id, username, description) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        db("TDM").execute(sql, now, action, entity, userId, username, description);
    }

}
