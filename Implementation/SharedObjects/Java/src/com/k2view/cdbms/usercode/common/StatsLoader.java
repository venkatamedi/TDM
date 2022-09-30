package com.k2view.cdbms.usercode.common;

import com.k2view.broadway.actors.builtin.DbCommand;
import com.k2view.broadway.model.Actor;
import com.k2view.broadway.model.Context;
import com.k2view.broadway.model.Data;
import com.k2view.fabric.common.Util;
import com.k2view.fabric.common.io.IoCommand;
import com.k2view.fabric.common.io.IoSession;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.k2view.fabric.common.Util.safeClose;

@SuppressWarnings({"unchecked"})
public class StatsLoader implements Actor {
    private static final String QUERY_INSERT = "INSERT INTO public.task_exe_stats_detailed (" +
            "task_execution_id, " +
            "lu_name, " +
            "entity_id, " +
            "target_entity_id, " +
            "table_name, " +
            "stage_name, " +
            "flow_name, " +
            "actor_name, " +
            "creation_date, " +
            "source_count, " +
            "target_count, " +
            "diff, " +
            "results" +
            ") VALUES " +
            "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private IoSession fabricSession;

    @Override
    public void action(Data input, Data output, Context context) throws Exception {
        if (fabricSession == null) {
            fabricSession = context.ioProvider().createSession("fabric");
        }

        String executionId = getQueryFirstResult("set execution_id;", "value", "NO_EXECUTION_ID");
        String entityIid = getQueryFirstResult("set IID;", "value", "NO_IID");
        String targetEntityID = getQueryFirstResult("set TARGET_ENTITY_ID;", "value", "NO_TARGET_IID");
        String luName = getQueryFirstResult("set LU_TYPE;", "value", "NO_LU_TYPE");

        // Parse input stats
        Map statsInput = (Map) input.get("stats");
        Map<String, TableStats> stats = new HashMap<>();
        statsInput.forEach((key, value) -> {
            if (key.toString().startsWith(DbCommand.STATS_EXECUTION_ROWS_EFFECTED + "_")) {
                TableStats tableStats = stats.computeIfAbsent(getTableName(key.toString(), DbCommand.STATS_EXECUTION_ROWS_EFFECTED + "_"), o -> new TableStats());
                tableStats.affected = (Long) value;
            } else if (key.toString().startsWith(DbCommand.STATS_EXECUTIONS_COUNT + "_")) {
                TableStats tableStats = stats.computeIfAbsent(getTableName(key.toString(), DbCommand.STATS_EXECUTIONS_COUNT + "_"), o -> new TableStats());
                tableStats.exec = (Long) value;
            } else if (key.toString().startsWith(DbCommand.STATS_EXECUTIONS_ERRORS + "_")) {
                TableStats tableStats = stats.computeIfAbsent(getTableName(key.toString(), DbCommand.STATS_EXECUTIONS_ERRORS + "_"), o -> new TableStats());
                tableStats.errors = (Long) value;
            }
        });

        // Update table with the stats per table
        IoSession session = context.ioProvider().createSession(input.string("interface"));
        IoCommand.Statement statement = session.prepareStatement(QUERY_INSERT);
        stats.forEach((tableName, tableStats) -> {
            try {
                statement.execute(
                        executionId,
                        luName,
                        entityIid,
                        targetEntityID,
                        tableName,
                        null,
                        null,
                        null,
                        new Timestamp(System.currentTimeMillis()),
                        tableStats.exec + tableStats.errors,
                        tableStats.affected,
                        tableStats.exec + tableStats.errors - tableStats.affected,
                        tableStats.errors == 0 ? "OK" : "FAIL"
                );
            } catch (Exception e) {
                throw new RuntimeException("Can't update stats for the table " + tableName + ".", e);
            }
        });

    }

    private String getTableName(String key, String prefix) {
        // remove stats prefix
        String tableNameWithSqlCommand = key.substring(prefix.length());

        // return only table name without LU name
        if (tableNameWithSqlCommand.contains(".")) {
            tableNameWithSqlCommand = tableNameWithSqlCommand.substring(tableNameWithSqlCommand.indexOf('.') + 1);
        } else {
            tableNameWithSqlCommand = tableNameWithSqlCommand.substring(tableNameWithSqlCommand.indexOf('_') + 1);
        }
        return tableNameWithSqlCommand;
    }

    private String getQueryFirstResult(String query, String columnName, String defaultValue) throws Exception {
        String value = defaultValue;
        try (IoCommand.Statement statement = fabricSession.prepareStatement(query)) {
            try (IoCommand.Result result = statement.execute()) {
                Iterator<IoCommand.Row> iterator = result.iterator();
                if (iterator.hasNext()) {
                    IoCommand.Row row = iterator.next();
                    if (iterator.hasNext()) {
                        throw new IllegalArgumentException("Command '" + statement.toString() + "' from 'fabric' session returns multiple response.");
                    }
                    String newValue = (String) row.get(columnName);
                    // Fabric can response with word "empty" in case there is no IID
                    if (!Util.isEmpty(newValue) && !"empty".equals(newValue)) {
                        value = newValue;
                    }
                } else if (defaultValue == null) {
                    throw new IllegalArgumentException("Command '" + statement.toString() + "' from 'fabric' session returns empty response.");
                }
            }
        }
        return value;
    }

    @Override
    public void close() {
        safeClose(fabricSession);
        fabricSession = null;
    }

    private class TableStats {
        long exec;
        long affected;
        long errors;
    }
}
