package com.study.monitoring.studymonitoring.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 메트릭 데이터 유틸리티 클래스
 **/
public class MetricUtil {

    /**
     * BigDecimal을 Double로 안전하게 변환
     *
     * @param value BigDecimal 값
     * @return Double(null이면 0.0)
     **/
    public static Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    /**
     * 퍼센트 값 계산 및 반올림
     *
     * @param value 현재 값
     * @param max 최대 값
     * @param scale 소수점 자리수
     * @return 백분율(%)
     **/
    public static Double calculatePercentage(double value, double max, int scale) {
        if(max == 0) {
            return 0.0;
        }

        BigDecimal result = BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(max), scale + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(scale, RoundingMode.HALF_UP);

        return result.doubleValue();
    }

    /**
     * 메트릭 값 반올림
     * @param value 값
     * @param scale 소수점 자리수
     * @return 반올림된 값
     **/
    public static Double round(Double value, int scale) {
        if (value == null) {
            return 0.0;
        }

        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 바이트를 메가바이트로 변환
     *
     * @param bytes 바이트
     * @return 메가바이트(수수점 2자리)
     */
    public static Double bytesToMB(long bytes) {
        return round(bytes / (1024.0 * 1024.0), 2);
    }

    /**
     * 메트릭 값의 임계치 초과 여부 확인
     *
     * @param value 현재 값
     * @param threshold 임계치
     * @return 초과 여부
     **/
    public static boolean isAboveThreshold(Double value, Double threshold) {
        if (value == null || threshold == null) {
            return false;
        }
        return value > threshold;
    }

    /**
     * PromQL 쿼리 빌더 헬퍼 - TPS
     *
     * @param application 애플리케이션 이름
     * @return PromQL 쿼리
     */
    public static String buildTPSQuery(String application) {
        return String.format(
                "rate(http_server_requests_seconds_count{application=\"%s\"}[1m])",
                application
        );
    }

    /**
     * PromQL 쿼리 빌더 헬퍼 - Heap Usage
     *
     * @param application 애플리케이션 이름
     * @return PromQL 쿼리
     */
    public static String buildHeapUsageQuery(String application) {
        return String.format(
                "jvm_memory_used_bytes{application=\"%s\",area=\"heap\"} / " +
                        "jvm_memory_max_bytes{application=\"%s\",area=\"heap\"} * 100",
                application, application
        );
    }

    /**
     * PromQL 쿼리 빌더 헬퍼 - Error Rate
     *
     * @param application 애플리케이션 이름
     * @return PromQL 쿼리
     **/
    public static String buildErrorRateQuery(String application) {
        return String.format(
                "rate(http_server_requests_seconds_count{application=\"%s\",status=~\"5..\"}[5m]) / " +
                        "rate(http_server_requests_seconds_count{application=\"%s\"}[5m]) * 100",
                application, application
        );
    }
}
