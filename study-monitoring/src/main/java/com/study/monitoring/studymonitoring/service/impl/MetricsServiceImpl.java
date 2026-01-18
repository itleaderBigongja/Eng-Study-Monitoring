package com.study.monitoring.studymonitoring.service.impl;

import com.study.monitoring.studymonitoring.service.MetricsService;
import com.study.monitoring.studymonitoring.service.PrometheusService;
import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;

/**
 * ============================================================================
 * 메트릭 비즈니스 로직 서비스 구현
 * ============================================================================
 *
 * 책임:
 * - 현재 메트릭 조회 비즈니스 로직 처리
 * - 응답 데이터 구조 생성
 * - PrometheusService에 실제 데이터 조회 위임
 *
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements MetricsService {

    private final PrometheusService prometheusService;
    private final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    @Override
    public Map<String, Object> getCurrentMetrics(String application) {
        try {
            log.debug("🔍 [MetricsService] 메트릭 조회 시작 - application: {}", application);

            // ✅ PrometheusService에서 실제 데이터 조회
            // 각 메서드는 애플리케이션 타입(Spring Boot, DB, ES)에 따라 적절한 쿼리 실행
            Double tps = prometheusService.getTps(application);
            Double heapUsage = prometheusService.getHeapMemoryUsage(application);
            Double errorRate = prometheusService.getErrorRate(application);
            Double cpuUsage = prometheusService.getCpuUsage(application);

            // ✅ 2. [핵심] CPU 값이 없으면(0.0) 내 컴퓨터 실제 CPU 사용 (하이브리드)
            if (cpuUsage == null || cpuUsage == 0.0) {
                double systemCpu = osBean.getCpuLoad(); // 0.0 ~ 1.0
                if (systemCpu >= 0) {
                    cpuUsage = systemCpu * 100.0; // 퍼센트 변환
                }
            }

            // ✅ 메트릭 데이터 구조화
            Map<String, Object> metrics = Map.of(
                    "tps", safeValue(tps),
                    "heapUsage", safeValue(heapUsage),
                    "errorRate", safeValue(errorRate),
                    "cpuUsage", safeValue(cpuUsage),
                    "timestamp", Instant.now().toEpochMilli()
            );

            // ✅ 최종 응답 구조 생성
            Map<String, Object> response = Map.of(
                    "application", application,
                    "metrics", metrics
            );

            log.debug("✅ [MetricsService] 메트릭 조회 완료 - TPS: {}, Heap: {}%",
                    metrics.get("tps"), metrics.get("heapUsage"));

            return response;

        } catch (Exception e) {
            log.error("❌ [MetricsService] 메트릭 조회 실패 - application: {}", application, e);

            // ✅ 에러 발생 시에도 기본 구조 반환 (프론트엔드 안정성)
            return Map.of(
                    "application", application,
                    "metrics", Map.of(
                            "tps", 0.0,
                            "heapUsage", 0.0,
                            "errorRate", 0.0,
                            "cpuUsage", 0.0,
                            "timestamp", Instant.now().toEpochMilli()
                    )
            );
        }
    }

    /**
     * Null 안전 값 변환
     *
     * @param value Prometheus 조회 값
     * @return null이면 0.0, 아니면 원래 값
     */
    private Double safeValue(Double value) {
        return value != null ? value : 0.0;
    }
}