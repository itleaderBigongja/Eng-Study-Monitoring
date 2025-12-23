import { get } from './client';
import { ENDPOINTS } from './endpoints';

export interface StatisticsQueryParams {
    metricType: string;
    startTime: string;
    endTime: string;
    timePeriod: string;
    aggregationType: string;
}

export interface LogStatisticsParams {
    startTime: string;
    endTime: string;
    timePeriod: string;
    logLevel?: string;
    eventAction?: string;
    threatLevel?: string;
}

/**
 * 시계열 통계 조회
 */
export async function getTimeSeriesStatistics(params: StatisticsQueryParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.TIMESERIES, params);
}

/**
 * 로그 통계 조회
 */
export async function getLogStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.LOGS, params);
}

/**
 * 접근 로그 통계 조회
 */
export async function getAccessLogStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.ACCESS_LOGS, params);
}

/**
 * 에러 로그 통계 조회
 */
export async function getErrorLogStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.ERROR_LOGS, params);
}

/**
 * 성능 메트릭 통계 조회
 */
export async function getPerformanceMetricsStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.PERFORMANCE_METRICS, params);
}

/**
 * 데이터베이스 로그 통계 조회
 */
export async function getDatabaseLogStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.DATABASE_LOGS, params);
}

/**
 * 감사 로그 통계 조회
 */
export async function getAuditLogStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.AUDIT_LOGS, params);
}

/**
 * 보안 로그 통계 조회
 */
export async function getSecurityLogStatistics(params: LogStatisticsParams): Promise<any> {
    // URLSearchParams를 사용하여 쿼리 파라미터 생성
    const queryParams = new URLSearchParams({
        startTime: params.startTime,
        endTime: params.endTime,
        timePeriod: params.timePeriod,
    });

    // threatLevel이 있을 때만 파라미터 추가
    if (params.threatLevel) {
        queryParams.append('threatLevel', params.threatLevel);
    }

    // ★ 중요: 쿼리 스트링을 포함하여 요청
    return get(`${ENDPOINTS.STATISTICS.SECURITY_LOGS}?${queryParams.toString()}`);
}