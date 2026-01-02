package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.response.MetricsResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.MetricsResponseDTO.DataPoint;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Prometheus 응답 → MetricsResponseDTO 변환기
 *
 * 역할:
 * - Prometheus API 응답을 DTO로 변환
 * - 시계열 데이터 파싱
 */
@Slf4j
@Component
public class MetricsConverter {

    /**
     * Prometheus Range Query 응답 → MetricsResponseDTO 변환
     *
     * Prometheus 응답 구조:
     * [
     *   {
     *     "metric": {...},
     *     "values": [
     *       [timestamp, "value"],
     *       [timestamp, "value"]
     *     ]
     *   }
     * ]
     *
     * @param prometheusData Prometheus 응답 데이터
     * @param application 애플리케이션 이름
     * @param metric 메트릭 타입
     * @param start 시작 시간
     * @param end 종료 시간
     * @return MetricsResponseDTO
     */
    public MetricsResponseDTO toDTO(
            List<Map<String, Object>> prometheusData,
            String application,
            String metric,
            Long start,
            Long end) {

        MetricsResponseDTO dto = new MetricsResponseDTO();
        dto.setApplication(application);
        dto.setMetric(metric);
        dto.setStart(start);
        dto.setEnd(end);

        // 시계열 데이터 변환
        List<DataPoint> dataPoints = new ArrayList<>();

        if (prometheusData != null && !prometheusData.isEmpty()) {
            // 첫 번째 결과 사용 (일반적으로 하나만 존재)
            Map<String, Object> firstResult = prometheusData.get(0);
            List<List<Object>> values = (List<List<Object>>) firstResult.get("values");

            if (values != null) {
                for (List<Object> value : values) {
                    Long timestamp = ((Number) value.get(0)).longValue();
                    Double metricValue = Double.parseDouble(value.get(1).toString());

                    dataPoints.add(new DataPoint(timestamp, metricValue));
                }
            }
        }

        dto.setData(dataPoints);
        return dto;
    }

    /**
     * Prometheus Instant Query 응답(Map)에서 핵심 값(Double) 하나만 추출
     * 구조:
     * {
     *     "status": "success",
     *     "data": {
     *         result: [
     *              {
     *                  "value": [170000000. "123.45"]
     *              }
     *         ]
     *     }
     * }
     *
     * @param prometheusResponse Prometheus 응답 데이터
     * @return 메트릭 값 (Double)
     */
    public Double extractValue(Map<String, Object> prometheusResponse) {
        try {
            // 1. 응답 자체가 비어있는지 확인
            if (prometheusResponse == null || prometheusResponse.isEmpty()) {
                log.warn("Prometheus response is empty/null");
                return 0.0;
            }

            // 2. "data" 필드 꺼내기
            Map<String, Object> data = (Map<String, Object>) prometheusResponse.get("data");
            if (data == null) {
                // 혹시 이미 data 안쪽 맵이 넘어왔을 경우를 대비해 result 체크
                if (prometheusResponse.containsKey("result")) {
                    data = prometheusResponse;
                } else {
                    return 0.0;
                }
            }

            // 3. "result" 리스트 꺼내기
            List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");

            // 데이터가 없으면 0.0 반환
            if (result == null || result.isEmpty()) {
                return 0.0;
            }

            // 4. 값 추출( 첫 번째 결과의 value )
            List<Object> valueTuple = (List<Object>) result.get(0).get("value");
            if (valueTuple != null && valueTuple.size() > 1) {
                // 값은 문자열로 오므로 Double로 변환("123.45" -> 123.45)
                return Double.parseDouble(valueTuple.get(1).toString());
            }
        } catch (Exception e) {
            log.error("Failed to parse metric value", e);
        }

        return 0.0;
    }
}
