package com.study.monitoring.studymonitoring.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** 데이터베이스 로그 통계 응답 DTO */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseLogStatisticsResponseDTO {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long endTime;
    private String timePeriod;

    // Operation 별 카운트( SELECT : 5000, INSERT 200 )
    private Map<String, Long> operationCounts;

    // 테이블 별 쿼리 수
    private Map<String, Long> tableCounts;

    // 쿼리 성능 지표
    private QueryPerformance queryPerformance;

    // 시간대 별 분포
    private List<DatabaseDistribution> distributions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryPerformance {
        private Double avgDuration;             // 평균 실행시간
        private Double maxDuration;             // 최대 실행시간
        private Long slowQueryCount;            // 느린 쿼리 수
        private Long totalQueryCount;           // 전체 쿼리 수
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseDistribution {
        private String timestamp;
        private Long queryCount;
        private Double avgDuration;
        private Long slowQueryCount;
    }
}
