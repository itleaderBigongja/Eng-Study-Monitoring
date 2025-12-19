package com.study.monitoring.studymonitoring.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 프로세스 현황 VO
 *
 * 테이블: MONITORING_PROCESS
 */
@Data
public class ProcessVO {
    private Long processId;                 // 프로세스 고유 ID
    private String processName;             // 프로세스명
    private String processType;             // 프로세스 유형(BACKEND, FRONTEND, DATABASE)
    private String hostName;                // 호스트명
    private Integer ipAddress;              // IP 주소
    private Integer port;                   // 포트번호
    private Integer pid;                    // 프로세스 ID
    private String status;                  // 상태(RUNNING, STOPPED, ERROR, STARTING, STOPPING)
    private BigDecimal cpuUsage;            // CPU 사용률(%)
    private BigDecimal memoryUsage;         // 메모리 사용률(%)
    private Long uptimeSeconds;             // 가동 시간 (초)
    private LocalDateTime lastHealthCheck;  // 마지막 헬스체크 시간
    private String errorMessage;            // 에러 메시지
    private LocalDateTime createdAt;        // 생성일시
    private LocalDateTime updatedAt;        // 수정일시
}
