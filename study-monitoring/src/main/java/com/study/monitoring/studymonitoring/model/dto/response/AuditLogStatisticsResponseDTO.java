package com.study.monitoring.studymonitoring.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** 감사 로그 통계 응답 DTO */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogStatisticsResponseDTO {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long endTime;
    private String timePeriod;

    // 이벤트 액션별 카운트
    private Map<String, Long> eventActionCounts;

    // 카테고리별 카운트
    private Map<String, Long> categoryCounts;

    // 성공/실패 비율
    private ResultStats resultStats;

    // 시간대 별 분포
    private List<AuditDistribution> distributions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultStats {
        private Long successCount;
        private Long failureCount;
        private Double successRate;         // 성공률(%)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditDistribution {
        private String timestamp;
        private Long totalEvents;
        private Long successEvents;
        private Long failureEvents;
    }
}
