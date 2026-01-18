package com.study.monitoring.studymonitoring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ============================================================================
 * 스케줄러 설정
 * ============================================================================
 *
 * 역할:
 * - @Scheduled 어노테이션 활성화
 * - AlertScheduler의 1분마다 실행 활성화
 *
 * ⚠️ 주의:
 * - 이 클래스가 없으면 @Scheduled가 동작하지 않습니다!
 * - AlertScheduler.checkAlerts()가 실행되지 않습니다!
 *
 * ============================================================================
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // 별도 설정 불필요
    // @EnableScheduling 어노테이션만으로 충분
}