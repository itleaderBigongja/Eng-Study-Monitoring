package com.eng.study.engstudy.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SqlPrintingInterceptor implements Interceptor {

    // 🔴 [수정 포인트] 로거 이름을 XML 설정과 화면에 보이는 "DatabaseLog"와 똑같이 맞춥니다.
    private static final Logger log = LoggerFactory.getLogger("DatabaseLog");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object result = invocation.proceed();

        // 🔴 [수정 포인트] 운영 환경에서도 보이도록 debug -> info 로 변경
        if (log.isInfoEnabled()) {
            try {
                Object[] args = invocation.getArgs();
                MappedStatement ms = (MappedStatement) args[0];
                Object parameter = args[1];

                BoundSql boundSql;
                // args 길이에 따른 분기 처리 유지
                if (args.length == 6) {
                    boundSql = (BoundSql) args[5];
                } else {
                    boundSql = ms.getBoundSql(parameter);
                }

                // SQL 정제
                String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
                String paramString = (parameter == null) ? "[]" : parameter.toString();

                // 🔴 [수정 포인트] INFO 레벨로 기록 (Case 2 포맷)
                log.info("SQL: [{}] | Params: [{}]", sql, paramString);

            } catch (Exception e) {
                log.warn("SQL Logging failed", e);
            }
        }
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {}
}