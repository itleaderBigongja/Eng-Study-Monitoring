package com.study.monitoring.studymonitoring.model.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 에러 로그 통계 조회 요청 DTO
 * 인덱스: error-logs-YYYY-MM
 * . 주요 통계: 에러 타입별, 심각도별, 발생 빈도
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogStatisticsQueryRequestDTO {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @NotNull(message = "시작 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @NotNull(message = "종료 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @NotNull(message = "시간 주기는 필수입니다")
    private String timePeriod;

    private String errorType;
    private String errorMessage;            // 선택: NullPointException, SQLException
    private String severity;                // 선택: ERROR, CRITICAL, FATAL

    /** 날짜 형식 검증 */
    public boolean isVaildDateFormat() {
        try {
            LocalDateTime.parse(startTime, FORMATTER);
            LocalDateTime.parse(endTime, FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** String -> LocalDateTime 변환 */
    public LocalDateTime getStartTimeAsLocalDateTime() {
        return LocalDateTime.parse(startTime, FORMATTER);
    }

    /** String -> LocalDateTime 변환 */
    public LocalDateTime getEndTimeAsLocalDateTime() {
        return LocalDateTime.parse(endTime, FORMATTER);
    }
}
