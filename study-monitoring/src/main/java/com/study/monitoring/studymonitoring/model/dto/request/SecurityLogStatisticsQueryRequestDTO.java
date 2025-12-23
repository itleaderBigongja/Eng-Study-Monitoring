package com.study.monitoring.studymonitoring.model.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 보안 로그 통계 조회 요청 DTO
 *  인덱스: security-logs-YYYY-MM
 *  주요 통계: 위협, 레벨별, 공격 타입별, 차단 여부 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityLogStatisticsQueryRequestDTO {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @NotNull(message = "시작 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @NotNull(message = "종료 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @NotNull(message = "시간 주기는 필수입니다")
    private String timePeriod;

    private String threatLevel;         // 선택: low, medium, high, critical 등
    private String attackType;          // 선택: brute_force, sql_injection, xss 등
    private Boolean bolcked;            // 선택: true/false

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

