package com.study.monitoring.studymonitoring.service;

import com.study.monitoring.studymonitoring.model.dto.response.AlertHistoryResponseDTO;

import java.util.List;

/**
 * ============================================================================
 * 알림 히스토리 서비스 인터페이스
 * ============================================================================
 * 역할: 알림 기록 조회 및 관리 비즈니스 로직 명세
 */
public interface AlertHistoryService {

    /**
     * 최근 알림 이력 조회 (페이징)
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 알림 이력 리스트
     */
    List<AlertHistoryResponseDTO> getRecentHistory(int page, int size);

    /**
     * 특정 알림 규칙의 이력 조회
     * @param alertRuleId 알림 규칙 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 알림 이력 리스트
     */
    List<AlertHistoryResponseDTO> getHistoryByRuleId(Long alertRuleId, int page, int size);

    /**
     * 미해결 알림 조회 (모든 규칙 대상)
     * @return 미해결 알림 리스트
     */
    List<AlertHistoryResponseDTO> getUnresolvedAlerts();

    /**
     * 알림 단건 해결 처리
     * @param historyId 히스토리 ID
     * @param message 해결 메시지 (예: "서버 재부팅으로 해결됨")
     * @return 성공 여부
     */
    boolean resolveAlert(Long historyId, String message);

    /**
     * 특정 규칙의 모든 미해결 알림 일괄 해결 처리
     * @param alertRuleId 알림 규칙 ID
     * @param message 해결 메시지
     * @return 해결된 건수
     */
    int resolveAllByRuleId(Long alertRuleId, String message);
}