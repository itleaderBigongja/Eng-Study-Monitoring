package com.study.monitoring.studymonitoring.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 메트릭 조회 응답 DTO
 *
 * 사용처:
 * - GET /api/dashboard/metrics
 * - GET /api/metrics/current
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricsResponseDTO {

    private String application;         // 애플리케이션 이름
    private String metric;              // 메트릭 타입
    private List<DataPoint> data;       // 시계열 데이터
    private Long start;                 // 시작 시간
    private Long end;                   // 종료 시간

    /**
     * 시계열 데이터 포인트
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private Long timestamp;         // Unix timestamp
        private Double value;           // 메트릭 값
    }
}
