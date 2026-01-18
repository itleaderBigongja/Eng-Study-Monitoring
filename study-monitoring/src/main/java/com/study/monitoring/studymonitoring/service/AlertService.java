package com.study.monitoring.studymonitoring.service;

import com.study.monitoring.studymonitoring.model.dto.request.AlertRuleRequestDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AlertHistoryResponseDTO;
import com.study.monitoring.studymonitoring.model.dto.response.AlertRuleResponseDTO;

import java.util.List;

/**
 * ============================================================================
 * 알림 규칙 관리 서비스 인터페이스
 * ============================================================================
 * * 역할:
 * - 알림 규칙 CRUD 명세
 * - 알림 히스토리 조회 명세
 * - 비즈니스 로직 명세
 * * ============================================================================
 */
public interface AlertService {

    // ========================================================================
    // 📌 알림 규칙 CRUD
    // ========================================================================

    /**
     * 알림 규칙 생성
     * * @param requestDTO 알림 규칙 요청 DTO
     * @return 생성된 알림 규칙 응답 DTO
     */
    AlertRuleResponseDTO createAlertRule(AlertRuleRequestDTO requestDTO);

    /**
     * 알림 규칙 수정
     * * @param id 알림 규칙 ID
     * @param requestDTO 수정 요청 DTO
     * @return 수정된 알림 규칙 응답 DTO
     */
    AlertRuleResponseDTO updateAlertRule(Long id, AlertRuleRequestDTO requestDTO);

    /**
     * 알림 규칙 삭제
     * * @param id 알림 규칙 ID
     */
    void deleteAlertRule(Long id);

    /**
     * 알림 규칙 단건 조회
     * * @param id 알림 규칙 ID
     * @return 알림 규칙 응답 DTO
     */
    AlertRuleResponseDTO getAlertRule(Long id);

    /**
     * 모든 알림 규칙 조회
     * * @return 알림 규칙 리스트
     */
    List<AlertRuleResponseDTO> getAllAlertRules();

    /**
     * 활성화된 알림 규칙만 조회
     * * @return 활성화된 알림 규칙 리스트
     */
    List<AlertRuleResponseDTO> getActiveAlertRules();

    /**
     * 애플리케이션별 알림 규칙 조회
     * * @param application 애플리케이션 이름
     * @return 알림 규칙 리스트
     */
    List<AlertRuleResponseDTO> getAlertRulesByApplication(String application);

    // ========================================================================
    // 📌 알림 히스토리 조회
    // ========================================================================

    /**
     * 최근 알림 히스토리 조회 (페이징)
     * * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 알림 히스토리 리스트
     */
    List<AlertHistoryResponseDTO> getRecentHistory(int page, int size);

    /**
     * 특정 알림 규칙의 히스토리 조회
     * * @param alertRuleId 알림 규칙 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 알림 히스토리 리스트
     */
    List<AlertHistoryResponseDTO> getHistoryByRule(Long alertRuleId, int page, int size);

    /**
     * 미해결 알림 조회
     * * @return 미해결 알림 리스트
     */
    List<AlertHistoryResponseDTO> getUnresolvedAlerts();

    // ========================================================================
    // 📌 알림 제어 및 처리
    // ========================================================================

    /**
     * 알림 규칙 활성화/비활성화 토글
     * * @param id 알림 규칙 ID
     * @return 토글 후 상태
     */
    AlertRuleResponseDTO toggleAlertRule(Long id);

    /**
     * 알림 해결 처리
     * * @param historyId 알림 히스토리 ID
     * @param resolveMessage 해결 메시지
     */
    void resolveAlert(Long historyId, String resolveMessage);
}