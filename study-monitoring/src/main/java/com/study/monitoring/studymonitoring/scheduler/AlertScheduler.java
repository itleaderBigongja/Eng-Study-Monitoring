package com.study.monitoring.studymonitoring.scheduler;

import com.study.monitoring.studymonitoring.converter.AlertConverter;
import com.study.monitoring.studymonitoring.mapper.AlertHistoryMapper;
import com.study.monitoring.studymonitoring.model.dto.response.AlertRuleResponseDTO;
import com.study.monitoring.studymonitoring.model.vo.AlertHistoryVO;
import com.study.monitoring.studymonitoring.service.AlertService;
import com.study.monitoring.studymonitoring.service.MetricsService;
import com.study.monitoring.studymonitoring.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final AlertService alertService;
    private final NotificationService notificationService;
    private final MetricsService metricsService;
    private final AlertHistoryMapper alertHistoryMapper;
    private final AlertConverter alertConverter;

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void checkAlertRules() {
        // Service는 DTO 리스트를 반환함
        List<AlertRuleResponseDTO> activeRules = alertService.getActiveAlertRules();

        if (activeRules.isEmpty()) return;

        log.info("⏰ [Scheduler] {}개의 알림 규칙 검사 중...", activeRules.size());

        for (AlertRuleResponseDTO rule : activeRules) {
            try {
                processRule(rule);
            } catch (Exception e) {
                // 에러 발생 시 DTO의 getId() 사용
                log.error("❌ 규칙 처리 중 오류 (ID: {})", rule.getId(), e);
            }
        }
    }

    private void processRule(AlertRuleResponseDTO rule) {
        // 1. 현재 메트릭 조회
        Map<String, Object> result = metricsService.getCurrentMetrics(rule.getApplication());
        Map<String, Object> metrics = (Map<String, Object>) result.get("metrics");

        // DB의 MetricType을 Map Key로 변환
        String key = convertMetricTypeToKey(rule.getMetricType());

        // 메트릭 값이 없으면 검사 중단
        if (metrics == null || !metrics.containsKey(key)) return;

        double rawValue = Double.parseDouble(metrics.get(key).toString());
        BigDecimal currentValue = BigDecimal.valueOf(rawValue);

        // 2. 조건 비교 (DTO의 getThreshold() 사용)
        if (currentValue.compareTo(rule.getThreshold()) > 0) {

            // 3. 메시지 생성 (DTO의 getName(), getThreshold() 사용)
            String message = String.format("🚨 [경고] %s\n- 현재값: %.2f%%\n- 임계값: %.2f%%",
                    rule.getName(), currentValue, rule.getThreshold());

            log.info("🔥 알림 발생! {}", message);

            // 4. [DB 저장 준비] DTO를 지원하는 Converter 메서드 호출
            AlertHistoryVO history = alertConverter.createHistoryVO(rule, rawValue, message);
            history.setTriggeredAt(LocalDateTime.now()); // 발생 시간 설정

            // 5. 슬랙 전송
            boolean sent = notificationService.sendNotification(message);

            // 6. 결과 업데이트
            if (sent) {
                history.markNotificationSent("SLACK", "성공");
            } else {
                history.markNotificationFailed("SLACK", "연동 실패");
            }

            // 7. [최종 저장] Mapper 호출
            alertHistoryMapper.insertHistory(history);

            log.info("💾 DB 저장 완료 (History ID: {})", history.getHistoryId());
        }
    }

    // Enum 타입 문자열을 Map Key로 변환하는 헬퍼 메서드
    private String convertMetricTypeToKey(String metricType) {
        if (metricType == null) return "";
        return switch (metricType) {
            case "CPU_USAGE" -> "cpuUsage";
            case "HEAP_USAGE" -> "heapUsage";
            case "ERROR_RATE" -> "errorRate";
            case "TPS" -> "tps";
            default -> "";
        };
    }
}