package com.study.monitoring.studymonitoring.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 시계열 통계 응답 DTO
 *
 * 변경사항:
 * - timestamp를 Long -> String으로 변경
 * - 날짜 형식: "yyyy-MM-dd HH:mm:ss"
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponseDTO {

    private static final DateTimeFormatter FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long startTime;             // 시작 시간

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long endTime;               // 종료 시간

    private String metricType;          // 메트릭 타입
    private String timePeriod;          // 시간 주기
    private String aggregationType;     // 집계 방식
    private String dataSource;          // PROMETHEUS, POSTGRESQL, MIXED
    private List<DataPoint> data;       // 시계열 데이터

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private String timestamp;         // Unix timestamp

        private Double value;           // 집계된 값
        private Double minValue;        // 최소값(옵션)
        private Double maxValue;        // 최대값(옵션)
        private Integer sampleCount;    // 샘플수

        /** Unix timestamp를 받아서 String으로 변환하는 생성자*/
        public DataPoint(Long timestamp, Double value, Double minValue, Double maxValue, Integer sampleCount) {
            this.timestamp = formatTimestamp(timestamp);
            this.value = value;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.sampleCount = sampleCount;
        }

        /** Unix timestamp -> "yyyy-MM-dd HH:mm:ss" 변환 */
        private static String formatTimestamp(Long timestamp) {
            if (timestamp == null) {
                return null;
            }
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp),
                    ZoneId.systemDefault()
            );
            return dateTime.format(FORMATTER);
        }
    }
}
