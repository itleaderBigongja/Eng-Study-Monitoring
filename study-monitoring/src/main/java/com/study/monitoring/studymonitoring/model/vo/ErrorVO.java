package com.study.monitoring.studymonitoring.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Error Count 실시간 감시 VO
 *
 * 테이블: MONITORING_ERROR
 *
 * 설명:
 * - 에러 유형별 발생 횟수 추적
 * - HTTP 상태 코드, 요청 URL, 사용자 정보
 * - 스택 트레이스 및 심각도 분류
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorVO {

    private Long errorId;                  // Error ID (PK)
    private Long processId;                // 프로세스 ID (FK)
    private String errorType;              // 에러 유형 (NullPointerException, IOException 등)
    private Integer errorCount;            // 에러 발생 횟수
    private String errorMessage;           // 에러 메시지
    private String errorCode;              // 에러 코드
    private Integer httpStatus;            // HTTP 상태 코드
    private String requestUrl;             // 요청 URL
    private String requestMethod;          // 요청 메소드 (GET, POST 등)
    private Long userId;                   // 사용자 ID
    private String ipAddress;              // IP 주소
    private String userAgent;              // User Agent
    private String stackTrace;             // 스택 트레이스
    private Boolean isCritical;            // 심각 여부
    private LocalDateTime firstOccurredAt; // 최초 발생 시간
    private LocalDateTime lastOccurredAt;  // 마지막 발생 시간
    private LocalDateTime createdAt;       // 생성일시

    /**
     * 에러 발생 횟수 증가
     */
    public void incrementErrorCount() {
        if (this.errorCount == null) {
            this.errorCount = 1;
        } else {
            this.errorCount++;
        }
        this.lastOccurredAt = LocalDateTime.now();
    }

    /**
     * Critical 에러인지 확인
     *
     * @return Critical 여부
     */
    public boolean isCriticalError() {
        return this.isCritical != null && this.isCritical;
    }

    /**
     * 5xx HTTP 에러인지 확인
     *
     * @return 서버 에러 여부
     */
    public boolean isServerError() {
        return this.httpStatus != null && this.httpStatus >= 500;
    }

    /**
     * 4xx HTTP 에러인지 확인
     *
     * @return 클라이언트 에러 여부
     */
    public boolean isClientError() {
        return this.httpStatus != null && this.httpStatus >= 400 && this.httpStatus < 500;
    }

    /**
     * 에러 발생 빈도가 높은지 확인
     *
     * @param threshold 임계치
     * @return 고빈도 에러 여부
     */
    public boolean isHighFrequencyError(int threshold) {
        return this.errorCount != null && this.errorCount >= threshold;
    }

    /**
     * 스택 트레이스 요약 (최대 500자)
     *
     * @param fullStackTrace 전체 스택 트레이스
     * @return 요약된 스택 트레이스
     */
    public static String summarizeStackTrace(String fullStackTrace) {
        if (fullStackTrace == null) {
            return null;
        }

        if (fullStackTrace.length() <= 500) {
            return fullStackTrace;
        }

        return fullStackTrace.substring(0, 500) + "... (truncated)";
    }
}