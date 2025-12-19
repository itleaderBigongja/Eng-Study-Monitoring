package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.StatisticsResponseDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Prometheus 시계열 메트릭 통계 변환기
 *
 * 책임:
 *  - Prometheus range query 결과를
 *    StatisticsResponseDTO.DataPoint 형식으로 변환
 *
 * 역할:
 *  - timestamp 정규화 (시간 주기 기준)
 *  - 데이터 집계 (avg, sum, min, max, count)
 *
 * 주의:
 *  - Prometheus 조회, 기간 분기 로직은 담당하지 않는다.
 */
@Component
public class PrometheusStatisticsConverter {

    /**
     * Prometheus 데이터를 시간 주기별로 집계
     */
    public List<StatisticsResponseDTO.DataPoint> convertData(
            List<Map<String, Object>> promData,
            String timePeriod,
            String aggregationType) {

        if (promData == null || promData.isEmpty()) {
            return Collections.emptyList();
        }

        // 첫 번째 결과 사용
        Map<String, Object> firstResult = promData.get(0);
        List<List<Object>> values = (List<List<Object>>) firstResult.get("values");

        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        // 시간 주기에 따라 데이터 그룹화
        Map<Long, List<Double>> groupedData = new TreeMap<>();

        for (List<Object> value : values) {
            Long timestamp = ((Number) value.get(0)).longValue();
            Double metricValue = Double.parseDouble(value.get(1).toString());

            // 시간 주기에 맞게 타임스탬프 정규화
            Long normalizedTimestamp = normalizeTimestamp(timestamp, timePeriod);

            groupedData.computeIfAbsent(normalizedTimestamp, k -> new ArrayList<>())
                    .add(metricValue);
        }

        // 그룹화된 데이터를 집계
        return groupedData.entrySet().stream()
                .map(entry -> {
                    Long timestamp = entry.getKey();
                    List<Double> dataPoints = entry.getValue();

                    Double aggregatedValue = aggregateValues(dataPoints, aggregationType);
                    Double minValue = dataPoints.stream().min(Double::compare).orElse(null);
                    Double maxValue = dataPoints.stream().max(Double::compare).orElse(null);

                    return new StatisticsResponseDTO.DataPoint(
                            timestamp,
                            aggregatedValue,
                            minValue,
                            maxValue,
                            dataPoints.size()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 타임스탬프 정규화 (시간 주기에 맞게)
     */
    private Long normalizeTimestamp(Long timestamp, String timePeriod) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(
                timestamp,
                0,
                ZoneId.systemDefault().getRules().getOffset(LocalDateTime.now())
        );

        LocalDateTime normalized = switch (timePeriod.toUpperCase()) {
            case "MINUTE" -> dateTime.truncatedTo(ChronoUnit.MINUTES);
            case "HOUR" -> dateTime.truncatedTo(ChronoUnit.HOURS);
            case "DAY" -> dateTime.truncatedTo(ChronoUnit.DAYS);
            case "WEEK" -> dateTime.truncatedTo(ChronoUnit.DAYS)
                    .minusDays(dateTime.getDayOfWeek().getValue() - 1);
            case "MONTH" -> dateTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            default -> dateTime;
        };

        return normalized.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * 데이터 집계
     */
    private Double aggregateValues(List<Double> values, String aggregationType) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        return switch (aggregationType.toLowerCase()) {
            case "avg" -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case "sum" -> values.stream().mapToDouble(Double::doubleValue).sum();
            case "min" -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            case "max" -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            case "count" -> (double) values.size();
            default -> 0.0;
        };
    }
}
