package com.eng.study.engstudy.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class DatabaseLogInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger("DatabaseLogger");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];

        String sqlId = ms.getId();
        String operation = ms.getSqlCommandType().name();

        try {
            MDC.put("operation", operation);
            MDC.put("sql_id", sqlId);

            String tableName = extractTableName(ms, invocation.getArgs());
            if (tableName != null) {
                MDC.put("table", tableName);
            }

            Object result = invocation.proceed();

            long duration = System.currentTimeMillis() - startTime;
            MDC.put("duration_ms", String.valueOf(duration));

            if (duration > 1000) {
                log.warn("Slow query detected: {} ({}ms)", sqlId, duration);
            } else {
                log.info("Query executed: {} ({}ms)", sqlId, duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("duration_ms", String.valueOf(duration));
            MDC.put("error", e.getMessage());

            log.error("Query execution failed: {} ({}ms) - {}", sqlId, duration, e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private String extractTableName(MappedStatement ms, Object[] args) {
        try {
            BoundSql boundSql = ms.getBoundSql(args[1]);
            String sql = boundSql.getSql().toUpperCase();

            if (sql.contains("FROM")) {
                String[] parts = sql.split("FROM");
                if (parts.length > 1) {
                    String tablePart = parts[1].trim().split("\\s+")[0];
                    return tablePart.replaceAll("[^A-Z_]", "");
                }
            } else if (sql.contains("INTO")) {
                String[] parts = sql.split("INTO");
                if (parts.length > 1) {
                    String tablePart = parts[1].trim().split("\\s+")[0];
                    return tablePart.replaceAll("[^A-Z_]", "");
                }
            } else if (sql.contains("UPDATE")) {
                String[] parts = sql.split("UPDATE");
                if (parts.length > 1) {
                    String tablePart = parts[1].trim().split("\\s+")[0];
                    return tablePart.replaceAll("[^A-Z_]", "");
                }
            }
        } catch (Exception e) {
            // 무시
        }
        return null;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
