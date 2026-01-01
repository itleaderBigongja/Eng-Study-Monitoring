package com.study.monitoring.studymonitoring.model.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 통계 조회 요청 DTO
 * 변경사항
 * - startTime, endTime을 String으로 받아 LocalDateTime으로 변환
 * - 날짜 형식: "yyyy-MM-dd HH:mm:ss"
 **/
@Data
public class StatisticsQueryRequestDTO {

    private static final DateTimeFormatter FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // === 시계열 데이터 통계 ===
    @NotNull(message = "시작 시간은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @NotNull(message = "종료 시간은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @NotBlank(message = "메트릭 타입은 필수입니다.")
    private String metricType;      // TPS, HEAP_USAGE, ERROR_RATE, CPU_USAGE

    @NotBlank(message = "시간 주기는 필수입니다.")
    private String timePeriod;      // MINUTE, HOUR, DAY, WEEK, MONTH

    @NotBlank(message = "집계 방식은 필수입니다.")
    private String aggregationType; // AVG, SUM, MIN, MAX, COUNT

    // === 로그 통계 (옵션) ===
    private String logLevel;  // DEBUG, INFO, WARN, ERROR (옵션)

    // 조회할 애플리케이션 이름
    private String application; // eng-study, monitoring, postgres, elasticsearch

    /** startTime을 LocalDateTime으로 변환 */
    public LocalDateTime getStartTimeAsLocalDateTime() {
        return LocalDateTime.parse(this.startTime, FORMATTER);
    }

    /** endTime을 LocalDateTime으로 변환 */
    public LocalDateTime getEndTimeAsLocalDateTime() {
        return LocalDateTime.parse(this.endTime, FORMATTER);
    }

    /** 날짜 형식 검증*/
    public boolean isValidDateFormat() {
        try {
            LocalDateTime.parse(this.startTime, FORMATTER);
            LocalDateTime.parse(this.endTime, FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
