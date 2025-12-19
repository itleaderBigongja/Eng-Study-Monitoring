package com.study.monitoring.studymonitoring.model.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 로그 통계 조회 전용 요청 DTO
 * 특징:
 * - metricType, aggregationType 불필요( 로그는 카운트만 )
 * */
@Data
public class LogStatisticsQueryRequestDTO {

    private static final DateTimeFormatter FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @NotNull(message = "시작 시간은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @NotNull(message = "종료 시간은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String endTime;                     // MINUTE, HOUR, DAY, WEEK, MONTH

    @NotNull(message = "시간 주기는 필수입니다.")
    private String timePeriod;                  // MINUTE, HOUR, DAY, WEEK, MONTH

    // 로그 레벨은 선택
    private String logLevel;                    // DEBUG, INFO, WARN, ERROR(옵션)

    /** startTime을 LocalDateTime으로 볂롼 */
    public LocalDateTime getStartTimeAsLocalDateTime() {
        return LocalDateTime.parse(startTime, FORMATTER);
    }

    /** endTime을 LocalDateTime으로 변환*/
    public LocalDateTime getEndTimeAsLocalDateTime() {
        return LocalDateTime.parse(endTime, FORMATTER);
    }

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
}