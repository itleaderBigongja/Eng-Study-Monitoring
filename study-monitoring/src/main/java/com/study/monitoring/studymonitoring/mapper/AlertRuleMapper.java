package com.study.monitoring.studymonitoring.mapper;

import com.study.monitoring.studymonitoring.model.vo.AlertRuleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * 알림 규칙 MyBatis Mapper
 * ============================================================================
 *
 * 역할:
 * - MONITORING_ALERT_RULE 테이블 CRUD
 * - 복잡한 쿼리 처리
 *
 * XML 위치: src/main/resources/mapper/AlertRuleMapper.xml
 *
 * ============================================================================
 */
@Mapper
public interface AlertRuleMapper {

    // ========================================================================
    // 조회 쿼리
    // ========================================================================

    /**
     * 모든 알림 규칙 조회
     *
     * @return 알림 규칙 리스트
     */
    List<AlertRuleVO> selectAllAlerts();

    /**
     * 활성화된 알림 규칙만 조회
     *
     * @return 활성화된 알림 규칙 리스트
     */
    List<AlertRuleVO> selectActiveAlerts();

    /**
     * 단건 조회 (ID로)
     *
     * @param alertRuleId 알림 규칙 ID
     * @return 알림 규칙 VO
     */
    AlertRuleVO selectAlertById(@Param("alertRuleId") Long alertRuleId);

    /**
     * 애플리케이션별 조회
     *
     * @param application 애플리케이션 이름
     * @return 알림 규칙 리스트
     */
    List<AlertRuleVO> selectAlertsByApplication(@Param("application") String application);

    /**
     * 심각도별 조회
     *
     * @param severity 심각도
     * @return 알림 규칙 리스트
     */
    List<AlertRuleVO> selectAlertsBySeverity(@Param("severity") String severity);

    /**
     * 이름 중복 체크
     *
     * @param alertName 알림 규칙 이름
     * @return 존재 여부
     */
    boolean existsByName(@Param("alertName") String alertName);

    /**
     * 활성 알림 개수 조회
     *
     * @return 활성 알림 수
     */
    int countActiveAlerts();

    // ========================================================================
    // 생성/수정/삭제 쿼리
    // ========================================================================

    /**
     * 알림 규칙 생성
     *
     * @param alert 알림 규칙 VO
     * @return 생성된 행 수
     */
    int insertAlert(AlertRuleVO alert);

    /**
     * 알림 규칙 수정
     *
     * @param alert 알림 규칙 VO
     * @return 수정된 행 수
     */
    int updateAlert(AlertRuleVO alert);

    /**
     * 활성화/비활성화 토글
     *
     * @param alertRuleId 알림 규칙 ID
     * @return 수정된 행 수
     */
    int toggleAlert(@Param("alertRuleId") Long alertRuleId);

    /**
     * 마지막 발생 시간 업데이트
     *
     * @param alertRuleId 알림 규칙 ID
     * @return 수정된 행 수
     */
    int updateLastTriggered(@Param("alertRuleId") Long alertRuleId);

    /**
     * 알림 규칙 삭제
     *
     * @param alertRuleId 알림 규칙 ID
     * @return 삭제된 행 수
     */
    int deleteAlert(@Param("alertRuleId") Long alertRuleId);

    // ========================================================================
    // 통계 쿼리
    // ========================================================================

    /**
     * 애플리케이션별 알림 개수
     *
     * @return Map 리스트 (application, count)
     */
    List<Map<String, Object>> countByApplication();

    /**
     * 심각도별 알림 개수
     *
     * @return Map 리스트 (severity, count)
     */
    List<Map<String, Object>> countBySeverity();
}