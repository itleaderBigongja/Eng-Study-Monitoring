package com.study.monitoring.studymonitoring.model.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 데이터베이스 로그 통계 조회 요청 DTO
 *  인덱스: database-logs-YYYY-MM
 *  주요 통계: 쿼리 실행 시간, Operation 별, 테이블 별 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseLogStatisticsQueryRequestDTO {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @NotNull(message = "시작 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String startTime;

    @NotNull(message = "종료 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String endTime;

    @NotNull(message = "시간 주기는 필수입니다")
    private String timePeriod;

    private String operation;       // 선택: SELECT, INSERT, UPDATE, DELETE 등
    private String tableName;       // 선택: 특정 테이블

    /** 날짜 형식 검증 */
    public boolean isValidDateFormat() {
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