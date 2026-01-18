package com.study.monitoring.studymonitoring.mapper;

import com.study.monitoring.studymonitoring.model.vo.AlertHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * 알림 히스토리 MyBatis Mapper
 * ============================================================================
 *
 * 역할:
 * - MONITORING_ALERT_HISTORY 테이블 CRUD
 * - 알림 발생 기록 관리
 *
 * XML 위치: src/main/resources/mapper/AlertHistoryMapper.xml
 *
 * ============================================================================
 */
@Mapper
public interface AlertHistoryMapper {

    // ========================================================================
    // 조회 쿼리
    // ========================================================================

    /**
     * 최근 히스토리 조회 (페이징)
     *
     * @param offset 시작 위치
     * @param size 페이지 크기
     * @return 알림 히스토리 리스트
     */
    List<AlertHistoryVO> selectRecentHistory(@Param("offset") int offset, @Param("size") int size);

    /**
     * 특정 알림 규칙의 히스토리 조회
     *
     * @param alertRuleId 알림 규칙 ID
     * @param offset 시작 위치
     * @param size 페이지 크기
     * @return 알림 히스토리 리스트
     */
    List<AlertHistoryVO> selectHistoryByAlertId(
            @Param("alertRuleId") Long alertRuleId,
            @Param("offset") int offset,
            @Param("size") int size);

    /**
     * 미해결 알림 조회
     *
     * @return 미해결 알림 리스트
     */
    List<AlertHistoryVO> selectUnresolvedHistory();

    /**
     * 특정 알림의 미해결 건 조회
     *
     * @param alertRuleId 알림 규칙 ID
     * @return 미해결 알림 리스트
     */
    List<AlertHistoryVO> selectUnresolvedByAlertId(@Param("alertRuleId") Long alertRuleId);

    /**
     * 기간별 히스토리 조회
     *
     * @param startDate 시작 시간
     * @param endDate 종료 시간
     * @return 알림 히스토리 리스트
     */
    List<AlertHistoryVO> selectHistoryByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 단건 조회
     *
     * @param historyId 히스토리 ID
     * @return 알림 히스토리 VO
     */
    AlertHistoryVO selectHistoryById(@Param("historyId") Long historyId);

    /**
     * 전체 개수 조회 (페이징용)
     *
     * @return 전체 히스토리 개수
     */
    long countAll();

    /**
     * 알림 규칙별 개수 조회 (페이징용)
     *
     * @param alertRuleId 알림 규칙 ID
     * @return 해당 규칙의 히스토리 개수
     */
    long countByAlertId(@Param("alertRuleId") Long alertRuleId);

    // ========================================================================
    // 생성/수정 쿼리
    // ========================================================================

    /**
     * 알림 히스토리 생성
     *
     * @param history 알림 히스토리 VO
     * @return 생성된 행 수
     */
    int insertHistory(AlertHistoryVO history);

    /**
     * 알림 해결 처리
     *
     * @param historyId 히스토리 ID
     * @param resolvedMessage 해결 메시지
     * @return 수정된 행 수
     */
    int resolveHistory(@Param("historyId") Long historyId, @Param("resolvedMessage") String resolvedMessage);

    /**
     * 특정 알림의 모든 미해결 건 일괄 해결
     *
     * @param alertRuleId 알림 규칙 ID
     * @param resolvedMessage 해결 메시지
     * @return 수정된 행 수
     */
    int resolveAllByAlertId(@Param("alertRuleId") Long alertRuleId, @Param("resolvedMessage") String resolvedMessage);

    /**
     * 알림 전송 결과 업데이트
     *
     * @param historyId 히스토리 ID
     * @param sent 전송 여부
     * @param result 전송 결과
     * @return 수정된 행 수
     */
    int updateNotificationResult(
            @Param("historyId") Long historyId,
            @Param("sent") boolean sent,
            @Param("result") String result);

    // ========================================================================
    // 통계 쿼리
    // ========================================================================

    /**
     * 기간별 알림 발생 횟수
     *
     * @param startDate 시작 시간
     * @param endDate 종료 시간
     * @return 알림 발생 횟수
     */
    int countByPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 애플리케이션별 알림 발생 통계
     *
     * @return Map 리스트 (application, count, unresolved_count)
     */
    List<Map<String, Object>> countByApplication();

    /**
     * 심각도별 알림 발생 통계
     *
     * @return Map 리스트 (severity, count, unresolved_count)
     */
    List<Map<String, Object>> countBySeverity();

    /**
     * 알림 규칙별 발생 횟수
     *
     * @return Map 리스트 (name, application, count)
     */
    List<Map<String, Object>> countByAlertRule();

    // ========================================================================
    // 유지보수 쿼리
    // ========================================================================

    /**
     * 오래된 히스토리 삭제 (30일 이전)
     *
     * @param days 보관 기간 (일)
     * @return 삭제된 행 수
     */
    int deleteOldHistory(@Param("days") int days);
}