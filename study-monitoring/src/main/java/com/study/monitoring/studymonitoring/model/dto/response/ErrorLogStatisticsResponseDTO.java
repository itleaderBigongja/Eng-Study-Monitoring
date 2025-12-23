package com.study.monitoring.studymonitoring.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** 에러 로그 통계 응답 DTO */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogStatisticsResponseDTO {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long endTime;
    private String timePeriod;

    // 에러 타입별 카운트
    private Map<String, Long> errorTypeCounts;

    // 심각도별 카운트
    private Map<String, Long> severityCounts;

    // 전체 에러 수
    private List<ErrorDistribution> distributions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDistribution {
        private String timestamp;
        private Long errorCount;
        private Map<String, Long> errorTypeBreakdown;  // 해당 시간대 에러 타입 분포
    }
}
