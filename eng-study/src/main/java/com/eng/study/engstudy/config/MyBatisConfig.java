//package com.eng.study.engstudy.config;
//
//import com.eng.study.engstudy.interceptor.DatabaseLogInterceptor;
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.mybatis.spring.SqlSessionFactoryBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import javax.sql.DataSource;
//
//@Configuration
//public class MyBatisConfig {
//    @Bean
//    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
//        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
//        sessionFactory.setDataSource(dataSource);
//
//        SqlSessionFactory factory = sessionFactory.getObject();
//
//        // ✅ 새 인터셉터 추가 (기존 SqlPrintingInterceptor는 @Component로 자동 등록됨)
//        factory.getConfiguration().addInterceptor(new DatabaseLogInterceptor());
//
//        return factory;
//    }
//}
