package com.study.monitoring.studymonitoring.mapper;
import com.study.monitoring.studymonitoring.model.vo.ProcessVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 프로세스 현황 Mapper
 *
 * 테이블: MONITORING_PROCESS
 */
@Mapper
public interface ProcessMapper {

    /**
     * 모든 프로세스 현황 조회
     *
     * @return ProcessVO 리스트
     */
    List<ProcessVO> getAllProcesses();

    /**
     * 특정 프로세스 조회
     *
     * @param processId 프로세스 ID
     * @return ProcessVO
     */
    ProcessVO getProcessById(Long processId);

    /**
     * 프로세스 요약 정보
     *
     * @return Map (total, running, stopped, error 카운트)
     */
    Map<String, Long> getProcessSummary();

    /**
     * 시스템 통계 정보
     *
     * @return Map (totalRequests, avgResponseTime, uptime)
     */
    Map<String, Object> getSystemStatistics();

    /**
     * 실시간 알림 목록
     *
     * @param limit 조회 개수
     * @return 알림 리스트
     */
    List<Map<String, Object>> getRecentAlerts(int limit);

    /**
     * 프로세스 상태 업데이트
     *
     * @param process ProcessVO
     * @return 업데이트 성공 여부
     */
    int updateProcessStatus(ProcessVO process);

    /**
     * 프로세스 정보 삽입
     **/

    /**
     * 상태별 프로세스 수
     * @param status
     **/
    Long countByStatus(String status);

    /**
     * 전체 프로세스 수
     **/
    Long countAllProcesses();
}
