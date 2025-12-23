package com.study.monitoring.studymonitoring.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogStatisticsResponseDTO {

    /** 로그 통계 응답 DTO */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long startTime;                         // 시작 시간(Unix timestamp)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long endTime;                           // 종료 시간
    private String timePeriod;                      // 시간 주기
    private Map<String, Long> logCounts;            // 로그 레벨별 카운트
    private List<LogDistribution> distributions;    // 시간대 별 로그 분포


    /** 시간대 별 로그 분포 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogDistribution {
        private String timestamp;
        private Long count;
    }
}
