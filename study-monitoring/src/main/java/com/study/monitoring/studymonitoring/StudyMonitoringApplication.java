package com.study.monitoring.studymonitoring;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.study.monitoring.studymonitoring.mapper")
public class StudyMonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyMonitoringApplication.class, args);
    }

}
