package com.study.monitoring.studymonitoring.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 실시간 감시 현황 VO
 *
 * 테이블: MONITORING_REALTIME*/
@Data
public class RealtimeVO {

    private Long realtimeId;            // 실시간 감시 ID
    private Long processId;             // 프로세스 ID(FK)
    private String metricType;          // 메트릭 유형
    private String metricUnit;          // 메트릭 단위
    private BigDecimal metricMin;       // 임계치 최소
    private BigDecimal metricMax;       // 임계치 최대
    private Boolean isAlert;            // 알람 발생 여부
    private String alertMessage;        // 알람 메시지
    private LocalDateTime collectedAt;  // 수집 시간
    private LocalDateTime createdAt;    // 생성일시
}
