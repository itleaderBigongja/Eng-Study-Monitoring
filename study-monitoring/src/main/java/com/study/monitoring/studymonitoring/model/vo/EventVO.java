package com.study.monitoring.studymonitoring.model.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주요 모니터링 이벤트 VO
 *
 * 테이블: MONITORING_EVENT
 *
 * 설명:
 * - Eviction Mode, SUB LOG, Error, DB Log 등 이벤트 추적
 * - 이벤트 유형별 카운팅 및 상세 정보 저장
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventVO {

    private Long eventId;                  // 이벤트 ID (PK)
    private Long processId;                // 프로세스 ID (FK)
    private String eventType;              // 이벤트 유형 (EVICTION_MODE, SUB_LOG, ERROR, DB_LOG, WARNING, INFO)
    private String eventLevel;             // 이벤트 심각도 (INFO, WARNING, ERROR, CRITICAL)
    private String eventMessage;           // 이벤트 메시지
    private JsonNode eventDetail;          // 이벤트 상세 정보 (JSON)
    private String sourceFile;             // 소스 파일명
    private Integer sourceLine;            // 소스 라인 번호
    private String stackTrace;             // 스택 트레이스
    private Boolean isResolved;            // 해결 여부
    private LocalDateTime resolvedAt;      // 해결 시간
    private String resolvedBy;             // 해결자
    private LocalDateTime occurredAt;      // 발생 시간
    private LocalDateTime createdAt;       // 생성일시

    /**
     * 이벤트 타입 Enum
     */
    public enum EventType {
        EVICTION_MODE,  // Eviction Mode
        SUB_LOG,        // SUB LOG
        ERROR,          // 에러
        DB_LOG,         // DB 로그
        WARNING,        // 경고
        INFO            // 정보
    }

    /**
     * 이벤트 레벨 Enum
     */
    public enum EventLevel {
        INFO,       // 정보
        WARNING,    // 경고
        ERROR,      // 에러
        CRITICAL    // 긴급
    }

    /**
     * 이벤트 해결 처리
     *
     * @param resolvedBy 해결자
     */
    public void resolve(String resolvedBy) {
        this.isResolved = true;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedBy;
    }

    /**
     * 미해결 이벤트인지 확인
     *
     * @return 미해결 여부
     */
    public boolean isUnresolved() {
        return this.isResolved == null || !this.isResolved;
    }

    /**
     * Critical 이벤트인지 확인
     *
     * @return Critical 여부
     */
    public boolean isCritical() {
        return EventLevel.CRITICAL.name().equals(this.eventLevel);
    }
}