package com.study.monitoring.studymonitoring.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 보안 로그 통계 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityLogStatisticsResponseDTO {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long endTime;
    private String timePeriod;

    // 위험 레벨 별 카운트
    private Map<String, Long> threatLevelCounts;
    // 공격 타입 별 카운트
    private Map<String, Long> attackTypeCounts;
    // 차단 통계
    private BlockStats blockStats;
    // 시간대 별 분포
    private List<SecurityDistribution> distributions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockStats {
        private Long totalAttacks;          // 전체 공격 시도
        private Long blockedAttacks;        // 차단된 공격
        private Long allowedAttacks;        // 차단 안된 공격
        private Double blockRate;           // 차단율 (%)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityDistribution {
        private String timestamp;
        private Long attackCount;
        private Long blockedCount;
        private Map<String, Long> threatLevelBreakdown;
    }
}
