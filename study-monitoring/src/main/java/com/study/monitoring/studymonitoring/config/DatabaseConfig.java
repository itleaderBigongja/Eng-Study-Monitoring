package com.study.monitoring.studymonitoring.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis 및 데이터베이스 설정
 *
 * 역할:
 * - MyBatis SqlSessionFactory 구성
 * - Mapper 인터페이스 스캔 설정
 * - PostgreSQL 연결 관리
 *
 * 참고:
 * - eng-study 프로젝트의 DatabaseConfig.java와 동일한 구조
 * - 모니터링 데이터는 동일한 PostgreSQL DB 사용
 */
@Configuration
@MapperScan("com.study.monitoring.studymonitoring.mapper")  // Mapper 인터페이스 위치
public class DatabaseConfig {

    /**
     * MyBatis SqlSessionFactory Bean 생성
     *
     * @param dataSource Spring Boot가 자동 생성한 DataSource
     * @return SqlSessionFactory 인스턴스
     * @throws Exception 설정 오류 시
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();

        // 1. DataSource 설정 (application.yml의 spring.datasource 사용)
        sessionFactory.setDataSource(dataSource);

        // 2. MyBatis XML 매퍼 파일 위치 설정
        sessionFactory.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/**/*.xml")
        );

        // 3. VO 클래스 패키지 설정 (별칭 자동 생성)
        sessionFactory.setTypeAliasesPackage("com.study.monitoring.model.vo");

        return sessionFactory.getObject();
    }
}
