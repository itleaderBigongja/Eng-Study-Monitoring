package com.study.monitoring.studymonitoring.mapper;

import com.study.monitoring.studymonitoring.model.vo.RealtimeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 실시간 모니터링 데이터 Mapper
 *
 * 테이블: MONITORING_REALTIME
 */
@Mapper
public interface RealtimeMapper {

    /**
     * 최근 알림 조회
     *
     * @param limit 조회 개수
     * @return 알림 리스트
     */
    List<Map<String, Object>> getRecentAlerts(@Param("limit") int limit);

    /**
     * 프로세스별 실시간 메트릭 조회
     *
     * @param processId 프로세스 ID
     * @param metricType 메트릭 타입
     * @param hours 조회 기간 (시간)
     * @return 메트릭 리스트
     */
    List<RealtimeVO> getMetricsByProcess(
            @Param("processId") Long processId,
            @Param("metricType") String metricType,
            @Param("hours") int hours
    );

    /**
     * 실시간 메트릭 삽입
     *
     * @param realtime RealtimeVO
     * @return 삽입 개수
     */
    int insertMetric(RealtimeVO realtime);
}