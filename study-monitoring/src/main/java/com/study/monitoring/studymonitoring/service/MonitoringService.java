package com.study.monitoring.studymonitoring.service;

import com.study.monitoring.studymonitoring.model.vo.ProcessVO;

import java.util.List;
import java.util.Map;

/**
 * 시스템 모니터링 관련 비즈니스 로직을 정의하는 서비스 인터페이스.
 * 프로세스 관리, 요약 정보, 시스템 통계 및 실시간 알림 조회 기능을 제공합니다.
 */
public interface MonitoringService {

    /**
     * 시스템에 등록된 모든 프로세스 목록을 조회합니다.
     *
     * @return 모든 프로세스 정보(List<ProcessVO>)
     */
    List<ProcessVO> getAllProcesses();

    /**
     * 특정 ID를 가진 프로세스의 상세 정보를 조회합니다.
     *
     * @param processId 조회할 프로세스의 고유 ID
     * @return 해당 ID의 프로세스 정보(ProcessVO) 또는 없을 경우 null
     */
    ProcessVO getProcessById(Long processId);

    /**
     * 현재 등록된 프로세스들의 상태별(전체, 실행 중, 중지됨, 오류) 요약 정보를 조회합니다.
     *
     * @return 상태별 프로세스 개수를 담은 맵 (키: total, running, stopped, error)
     */
    Map<String, Long> getProcessSummary();

    /**
     * 시스템의 전반적인 통계 정보(예: 총 요청 수, 평균 응답 시간, 시스템 가동 시간 등)를 조회합니다.
     *
     * @return 시스템 통계 데이터를 담은 맵
     */
    Map<String, Object> getSystemStatistics();

    /**
     * 최근 발생한 알림 목록을 지정된 개수만큼 조회합니다.
     *
     * @param limit 조회할 알림의 최대 개수
     * @return 최근 알림 목록 (각 알림 정보는 Map<String, Object> 형태)
     */
    List<Map<String, Object>> getRecentAlerts(int limit);

    /**
     * 특정 프로세스의 상태를 업데이트합니다.
     *
     * @param process 업데이트할 정보를 담고 있는 ProcessVO 객체 (ID와 새로운 상태 포함)
     * @return 업데이트 성공 시 true, 실패 시 false
     */
    boolean updateProcessStatus(ProcessVO process);
}