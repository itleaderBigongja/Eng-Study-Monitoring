package com.study.monitoring.studymonitoring.model.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 성능 메트릭 통계 조회 요청 DTO
 *  인덱스: CPU, Memory, JVM, System 메트릭 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetricsStatisticsQueryRequestDTO {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @NotNull(message = "시작 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String startTime;

    @NotNull(message = "종료 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String endTime;

    @NotNull(message = "시간 주기는 필수입니다")
    private String timePeriod;

    private String metricName;      // 선택: cpu_usage, memory_usage, heap_used
    private String aggregationType; // 선택: AVG, MAX, MIN

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
