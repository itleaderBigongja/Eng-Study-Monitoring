package com.study.monitoring.studymonitoring.converter;

import com.study.monitoring.studymonitoring.model.dto.request.AlertRuleRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AlertHistoryResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AlertRuleResponseDTO;
import com.study.monitoring.studymonitoring.model.vo.AlertHistoryVO;
import com.study.monitoring.studymonitoring.model.vo.AlertRuleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * 알림 VO ↔ DTO 변환기 (MyBatis 버전)
 * ============================================================================
 *
 * 역할:
 * - AlertRuleVO → AlertRuleResponseDTO 변환
 * - AlertRuleRequestDTO → AlertRuleVO 변환
 * - AlertHistoryVO → AlertHistoryResponseDTO 변환
 *
 * 변환 흐름:
 * [Client] → RequestDTO → VO → [Database]
 * [Database] → VO → ResponseDTO → [Client]
 *
 * ============================================================================
 */
@Slf4j
@Component
public class AlertConverter {

    // ========================================================================
    // 📌 AlertRule 변환
    // ========================================================================

    /**
     * AlertRuleVO → AlertRuleResponseDTO
     *
     * @param vo 알림 규칙 VO
     * @return 알림 규칙 응답 DTO
     */
    public AlertRuleResponseDTO toResponseDTO(AlertRuleVO vo) {
        if (vo == null) {
            return null;
        }

        return AlertRuleResponseDTO.builder()
                .id(vo.getAlertRuleId())
                .name(vo.getAlertName())
                .application(vo.getApplication())
                .alertType(vo.getAlertType())
                .metricType(vo.getMetricType())
                .condition(vo.getConditionOperator())
                .threshold(vo.getThresholdValue())
                .durationMinutes(vo.getDurationMinutes())
                .severity(vo.getSeverity())
                .notificationMethods(vo.getNotificationMethodList())
                .notificationEmail(vo.getNotificationEmail())
                .notificationSlack(vo.getNotificationSlack())
                .active(vo.getIsActive())
                .lastTriggeredAt(vo.getLastTriggeredAt())
                .triggerCount(vo.getTriggerCount())
                .createdAt(vo.getCreatedAt())
                .updatedAt(vo.getUpdatedAt())
                .build();
    }

    /**
     * AlertRuleRequestDTO → AlertRuleVO
     *
     * 용도: 새로운 알림 규칙 생성
     *
     * @param dto 알림 규칙 요청 DTO
     * @return 알림 규칙 VO
     */
    public AlertRuleVO toVO(AlertRuleRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        AlertRuleVO vo = AlertRuleVO.builder()
                .alertName(dto.getName())
                .application(dto.getApplication())
                .alertType("THRESHOLD")  // 기본값: 임계치 기반 알림
                .metricType(dto.getMetricType())
                .conditionOperator(dto.getCondition())
                .thresholdValue(dto.getThreshold())
                .durationMinutes(dto.getDurationMinutes())
                .severity(determineSeverity(dto.getMetricType(), dto.getThreshold().doubleValue()))
                .isActive(dto.getActive())
                .notificationEmail(dto.getNotificationEmail())
                .notificationSlack(dto.getNotificationSlack())
                .triggerCount(0)  // 초기값
                .createdId("system")  // TODO: 실제 사용자 ID로 변경 (Spring Security 연동 시)
                .build();

        // 알림 수신 방법 설정 (List → String 변환)
        vo.setNotificationMethodList(dto.getNotificationMethods());

        return vo;
    }

    /**
     * 기존 AlertRuleVO 업데이트
     *
     * 용도: 알림 규칙 수정
     * 참고: ID는 변경하지 않음
     *
     * @param vo 기존 VO
     * @param dto 수정 요청 DTO
     */
    public void updateVO(AlertRuleVO vo, AlertRuleRequestDTO dto) {
        if (vo == null || dto == null) {
            return;
        }

        vo.setAlertName(dto.getName());
        vo.setApplication(dto.getApplication());
        vo.setMetricType(dto.getMetricType());
        vo.setConditionOperator(dto.getCondition());
        vo.setThresholdValue(dto.getThreshold());
        vo.setDurationMinutes(dto.getDurationMinutes());
        vo.setNotificationMethodList(dto.getNotificationMethods());
        vo.setNotificationEmail(dto.getNotificationEmail());
        vo.setNotificationSlack(dto.getNotificationSlack());
        vo.setIsActive(dto.getActive());
        vo.setSeverity(determineSeverity(dto.getMetricType(), dto.getThreshold().doubleValue()));
        vo.setUpdatedId("system");  // TODO: 실제 사용자 ID로 변경

        // updatedAt은 SQL에서 CURRENT_TIMESTAMP로 자동 갱신됨
    }

    // ========================================================================
    // 📌 AlertHistory 변환
    // ========================================================================

    /**
     * AlertHistoryVO → AlertHistoryResponseDTO
     *
     * @param vo 알림 히스토리 VO
     * @return 알림 히스토리 응답 DTO
     */
    public AlertHistoryResponseDTO toHistoryResponseDTO(AlertHistoryVO vo) {
        if (vo == null) {
            return null;
        }

        return AlertHistoryResponseDTO.builder()
                .id(vo.getHistoryId())
                .alertRuleId(vo.getAlertRuleId())
                .alertRuleName(vo.getAlertName())  // 조인 결과
                .application(vo.getApplication())
                .metricType(vo.getMetricType())
                .triggeredAt(vo.getTriggeredAt())
                .currentValue(vo.getCurrentValue())
                .thresholdValue(vo.getThresholdValue())
                .message(vo.getAlertMessage())
                .severity(vo.getSeverity())
                .resolved(vo.getIsResolved())
                .resolvedAt(vo.getResolvedAt())
                .resolvedMessage(vo.getResolvedMessage())
                .durationMinutes(vo.getDurationMinutes() != null ? vo.getDurationMinutes().longValue() : null)
                .notificationSent(vo.getNotificationSent())
                .notificationMethods(vo.getNotificationMethods())
                .notificationResult(vo.getNotificationResult())
                .notificationError(vo.getNotificationError())
                .build();
    }

    /**
     * AlertHistoryVO 생성
     *
     * 용도: 알림 발생 시 새로운 히스토리 기록
     *
     * @param alertRule 알림 규칙 VO
     * @param currentValue 현재 메트릭 값
     * @param message 알림 메시지
     * @return 알림 히스토리 VO
     */
    public AlertHistoryVO createHistoryVO(AlertRuleVO alertRule, Double currentValue, String message) {
        if (alertRule == null) {
            return null;
        }

        return AlertHistoryVO.builder()
                .alertRuleId(alertRule.getAlertRuleId())
                .currentValue(java.math.BigDecimal.valueOf(currentValue))
                .thresholdValue(alertRule.getThresholdValue())
                .alertMessage(message)
                .severity(alertRule.getSeverity())
                .isResolved(false)
                .notificationSent(false)
                .application(alertRule.getApplication())
                .metricType(alertRule.getMetricType())
                .build();
    }

    /**
     * AlertRuleResponseDTO → AlertHistoryVO 생성 (오버로딩)
     * * 용도: 스케줄러에서 DTO 데이터를 기반으로 히스토리 VO 생성
     * (기존 메서드는 VO를 받지만, 이 메서드는 DTO를 받습니다.)
     */
    public AlertHistoryVO createHistoryVO(AlertRuleResponseDTO ruleDto, Double currentValue, String message) {
        if (ruleDto == null) {
            return null;
        }

        return AlertHistoryVO.builder()
                .alertRuleId(ruleDto.getId())          // DTO는 getId()
                .alertName(ruleDto.getName())          // DTO는 getName()
                .application(ruleDto.getApplication())
                .metricType(ruleDto.getMetricType())
                .currentValue(java.math.BigDecimal.valueOf(currentValue))
                .thresholdValue(ruleDto.getThreshold()) // DTO는 getThreshold()
                .alertMessage(message)
                .severity(ruleDto.getSeverity())
                .isResolved(false)
                .notificationSent(false)
                .application(ruleDto.getApplication())
                .metricType(ruleDto.getMetricType())
                .build();
    }

    // ========================================================================
    // 📌 헬퍼 메서드
    // ========================================================================

    /**
     * 심각도 자동 결정
     *
     * 메트릭 타입과 임계값에 따라 심각도를 자동으로 결정합니다.
     *
     * @param metricType 메트릭 타입
     * @param thresholdValue 임계값
     * @return 심각도 (CRITICAL, ERROR, WARNING, INFO)
     */
    private String determineSeverity(String metricType, double thresholdValue) {
        return switch (metricType) {
            case "CPU_USAGE", "HEAP_USAGE" -> {
                if (thresholdValue >= 90) yield "CRITICAL";
                if (thresholdValue >= 80) yield "ERROR";
                if (thresholdValue >= 70) yield "WARNING";
                yield "INFO";
            }
            case "ERROR_RATE" -> {
                if (thresholdValue >= 10) yield "CRITICAL";
                if (thresholdValue >= 5) yield "ERROR";
                if (thresholdValue >= 1) yield "WARNING";
                yield "INFO";
            }
            case "TPS" -> {
                // TPS는 일반적으로 경고 수준
                // 높은 값이 문제가 아니라 낮은 값이 문제일 수 있음
                yield "WARNING";
            }
            case "DB_CONNECTIONS" -> {
                if (thresholdValue >= 100) yield "CRITICAL";
                if (thresholdValue >= 80) yield "ERROR";
                if (thresholdValue >= 50) yield "WARNING";
                yield "INFO";
            }
            case "DB_SIZE" -> {
                if (thresholdValue >= 10000) yield "CRITICAL";  // 10GB 이상
                if (thresholdValue >= 5000) yield "ERROR";      // 5GB 이상
                if (thresholdValue >= 1000) yield "WARNING";    // 1GB 이상
                yield "INFO";
            }
            default -> "INFO";
        };
    }
}