package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.StatisticsResponseDTO;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class PrometheusStatisticsConverter {

    /**
     * Prometheus 데이터를 시간 주기별로 집계
     *
     * ✅ [개선] 여러 시계열(multiple series) 데이터를 모두 처리
     * ✅ [수정] step 문자열을 올바르게 처리
     */
    public List<StatisticsResponseDTO.DataPoint> convertData(
            List<Map<String, Object>> promData,
            String step,  // "15m", "1h", "6h" 형태의 Prometheus step
            String aggregationType) {

        if (promData == null || promData.isEmpty()) {
            log.warn("Prometheus data is empty");
            return Collections.emptyList();
        }

        // ✅ [수정] step을 timePeriod로 변환
        String timePeriod = convertStepToPeriod(step);
        log.debug("Converting Prometheus data with step={}, timePeriod={}", step, timePeriod);

        // ✅ [중요] 모든 시계열 데이터를 수집 (첫 번째 것만 사용하지 않음)
        Map<Long, List<Double>> groupedData = new TreeMap<>();

        for (Map<String, Object> result : promData) {
            List<List<Object>> values = (List<List<Object>>) result.get("values");

            if (values == null || values.isEmpty()) {
                log.warn("No values found in Prometheus result");
                continue;
            }

            // 각 시계열의 모든 값 처리
            for (List<Object> value : values) {
                try {
                    Long timestamp = ((Number) value.get(0)).longValue();
                    String valueStr = value.get(1).toString();

                    // ✅ [개선] NaN, Inf 처리
                    if ("NaN".equalsIgnoreCase(valueStr) ||
                            "Inf".equalsIgnoreCase(valueStr) ||
                            "+Inf".equalsIgnoreCase(valueStr) ||
                            "-Inf".equalsIgnoreCase(valueStr)) {
                        log.debug("Skipping invalid value: {}", valueStr);
                        continue;
                    }

                    Double metricValue = Double.parseDouble(valueStr);

                    // ✅ [개선] 음수 또는 비정상적인 값 필터링 (Heap, CPU는 0~100%)
                    if (metricValue < 0 || metricValue > 10000) {
                        log.warn("Suspicious metric value detected: {}. Skipping.", metricValue);
                        continue;
                    }

                    // 시간 주기에 맞게 타임스탬프 정규화
                    Long normalizedTimestamp = normalizeTimestamp(timestamp, timePeriod);

                    groupedData.computeIfAbsent(normalizedTimestamp, k -> new ArrayList<>())
                            .add(metricValue);

                } catch (Exception e) {
                    log.error("Failed to parse Prometheus value: {}", value, e);
                }
            }
        }

        if (groupedData.isEmpty()) {
            log.warn("No data after grouping. Original data size: {}", promData.size());
            return Collections.emptyList();
        }

        // 그룹화된 데이터를 집계
        List<StatisticsResponseDTO.DataPoint> result = groupedData.entrySet().stream()
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

        log.info("Converted {} Prometheus series to {} aggregated points",
                promData.size(), result.size());

        return result;
    }

    /**
     * ✅ [신규] Prometheus step 문자열을 timePeriod로 변환
     *
     * @param step Prometheus step 문자열 (예: "1m", "15m", "1h", "6h", "1d")
     * @return timePeriod 문자열 (예: "MINUTE", "HOUR", "DAY")
     */
    private String convertStepToPeriod(String step) {
        if (step == null || step.isEmpty()) {
            return "HOUR"; // 기본값
        }

        // 마지막 문자(단위)와 숫자 부분 분리
        char unit = step.charAt(step.length() - 1);

        try {
            // 숫자 부분 추출 (선택적)
            // int value = Integer.parseInt(step.substring(0, step.length() - 1));

            return switch (unit) {
                case 's' -> "MINUTE";  // 초 단위는 분으로 정규화
                case 'm' -> "MINUTE";  // 분
                case 'h' -> "HOUR";    // 시간
                case 'd' -> "DAY";     // 일
                case 'w' -> "WEEK";    // 주
                default -> "HOUR";     // 알 수 없는 경우 시간 단위
            };
        } catch (Exception e) {
            log.warn("Failed to parse step: {}. Using HOUR as default.", step);
            return "HOUR";
        }
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

        return switch (aggregationType.toUpperCase()) {
            case "AVG" -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case "SUM" -> values.stream().mapToDouble(Double::doubleValue).sum();
            case "MIN" -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            case "MAX" -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            case "COUNT" -> (double) values.size();
            default -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        };
    }
}