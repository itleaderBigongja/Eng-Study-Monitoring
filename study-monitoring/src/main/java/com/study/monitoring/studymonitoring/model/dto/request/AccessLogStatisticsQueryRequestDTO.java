package com.study.monitoring.studymonitoring.model.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 접근 로그 통계 조회 요청 DTO
 *  인덱스: access-logs-YYYY-MM
 *  주요 통계: HTTP 메서드별, 상태코드별, 응답시간 분석 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessLogStatisticsQueryRequestDTO {

    private static final DateTimeFormatter FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @NotNull(message = "시작 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @NotNull(message = "종료 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @NotNull(message = "시간 주기는 필수입니다.")
    private String timePeriod;                  // MINUTE, HOUR, DAY, WEEK, MONTH, 등

    private String httpMethod;                  // 선택: GET, POST, PUT, DELETE, 등

    private Integer statusCode;                 // 선택: 200, 404, 500, 등
    private String url;                         // 선택: URL 필터링


    /** String -> LocalDateTime 변환 */
    public LocalDateTime getStartTimeAsLocalDateTime() {
        return LocalDateTime.parse(startTime, FORMATTER);
    }

    /** String -> LocalDateTime 변환 */
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
