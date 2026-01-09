package com.eng.study.engstudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EngStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngStudyApplication.class, args);
    }

}
