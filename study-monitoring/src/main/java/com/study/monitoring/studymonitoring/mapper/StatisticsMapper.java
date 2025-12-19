package com.study.monitoring.studymonitoring.mapper;

import com.study.monitoring.studymonitoring.model.vo.StatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 통계 데이터 Mapper
 *
 * 테이블: MONITORING_STATISTICS
 */
@Mapper
public interface StatisticsMapper {

    /**
     * 집계 데이터 저장
     *
     * @param statistics StatisticsVO
     * @return 삽입 개수
     */
    int insertStatistics(StatisticsVO statistics);

    /**
     * 시스템 전체 통계 조회
     *
     * @return 통계 Map (totalRequests, avgResponseTime, uptime)
     */
    Map<String, Object> getSystemStats();

    /**
     * 기간별 통계 조회 (PostgreSQL)
     *
     * @param metricType 메트릭 타입
     * @param timePeriod 시간 주기
     * @param aggregationType 집계 방식
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return StatisticsVO 리스트
     */
    List<StatisticsVO> getStatisticsByPeriod(
            @Param("metricType") String metricType,
            @Param("timePeriod") String timePeriod,
            @Param("aggregationType") String aggregationType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 애플리케이션별 통계 조회
     *
     * @param application 애플리케이션 이름
     * @return 통계 Map
     */
    Map<String, Object> getStatsByApplication(@Param("application") String application);

    /**
     * 기간별 집계 통계 조회
     *
     * @param metricType 메트릭 타입
     * @param timePeriod 시간 주기 (HOUR, DAY, WEEK)
     * @param start 시작 시간
     * @param end 종료 시간
     * @return 통계 리스트
     */
    Map<String, Object> getAggregatedStats(
            @Param("metricType") String metricType,
            @Param("timePeriod") String timePeriod,
            @Param("start") String start,
            @Param("end") String end
    );

    /**
     * 오래된 통계 데이터 삭제
     *
     * @param days 보관 일수
     * @return 삭제 개수
     */
    int deleteOldStatistics(@Param("days") int days);
}