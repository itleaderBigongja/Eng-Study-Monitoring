// lib/api/metrics.ts
import { get, post } from './client';
import { ENDPOINTS } from './endpoints';

// ============================================================================
// ✅ [Phase 1] 타입 정의 명확화
// ============================================================================

/**
 * 현재 메트릭 조회 파라미터
 */
export interface CurrentMetricsParams {
    application?: string; // 기본값: 'eng-study'
}

/**
 * 현재 메트릭 응답 타입 (백엔드 응답 구조와 일치)
 */
export interface CurrentMetricsResponse {
    application: string;
    metrics: {
        tps: number;
        heapUsage: number;
        errorRate: number;
        cpuUsage: number;
        timestamp: number;
    };
}

/**
 * PromQL 쿼리 요청
 */
export interface MetricsQueryRequest {
    query: string;
    start?: number;
    end?: number;
    step?: string;
}

/**
 * Range Query 응답 타입
 */
export interface MetricsRangeResponse {
    query: string;
    start: number;
    end: number;
    step: string;
    data: Array<{
        metric: Record<string, string>;
        values: Array<[number, string]>;
    }>;
}

// ============================================================================
// ✅ [Phase 1] 실제 사용하는 API
// ============================================================================

/**
 * 현재 메트릭 조회 (실시간 모니터링용)
 *
 * 사용처: MetricsPage, useMetricsRange 훅
 * 호출 주기: 5초
 *
 * @param params - application 이름 (기본값: 'eng-study')
 * @returns 현재 시점의 TPS, Heap, Error, CPU 메트릭
 */
export async function getCurrentMetrics(
    params?: CurrentMetricsParams
): Promise<CurrentMetricsResponse> {
    return get<CurrentMetricsResponse>(ENDPOINTS.METRICS.CURRENT, params);
}

/**
 * ✅ [Phase 1 활성화] Range 메트릭 조회 (시간 범위 쿼리)
 *
 * 사용처: 통계 페이지, 실시간 페이지 히스토리 (향후 확장)
 * 용도: 과거 특정 시간대의 메트릭을 조회할 때 사용
 *
 * @param request - PromQL 쿼리, 시작/종료 시간, step
 * @returns 시간별 메트릭 데이터
 */
export async function getRangeMetrics(
    request: MetricsQueryRequest
): Promise<MetricsRangeResponse> {
    return post<MetricsRangeResponse>(ENDPOINTS.METRICS.RANGE, request);
}

/**
 * [Phase 3에서 활성화 예정] PromQL 쿼리 직접 실행
 *
 * 용도: 고급 사용자가 커스텀 쿼리를 실행할 때 사용
 * 현재 상태: 백엔드는 구현되어 있으나 프론트엔드에서 호출 안 함
 *
 * @param request - PromQL 쿼리
 * @returns Prometheus 응답
 */
export async function executeMetricsQuery(
    request: MetricsQueryRequest
): Promise<any> {
    return post(ENDPOINTS.METRICS.QUERY, request);
}

// ============================================================================
// ✅ [Phase 1] 헬퍼 함수
// ============================================================================

/**
 * 시간 범위를 Unix timestamp로 변환
 *
 * @param rangeStr - 시간 범위 문자열 (5m, 1h, 6h, 24h)
 * @returns { start, end } Unix timestamp (초 단위)
 */
export function getTimeRangeTimestamps(rangeStr: string): { start: number; end: number } {
    const now = Math.floor(Date.now() / 1000); // 현재 시간 (초)
    let seconds = 0;

    switch (rangeStr) {
        case '5m':
            seconds = 5 * 60;
            break;
        case '1h':
            seconds = 60 * 60;
            break;
        case '6h':
            seconds = 6 * 60 * 60;
            break;
        case '24h':
            seconds = 24 * 60 * 60;
            break;
        default:
            seconds = 5 * 60;
    }

    return {
        start: now - seconds,
        end: now
    };
}

/**
 * 애플리케이션별 PromQL 쿼리 생성
 *
 * @param application - 애플리케이션 이름
 * @param metricType - 메트릭 타입 (tps, heap, cpu, error)
 * @returns PromQL 쿼리 문자열
 */
export function buildMetricQuery(application: string, metricType: string): string {
    const queries: Record<string, string> = {
        tps: `rate(http_server_requests_seconds_count{application="${application}"}[1m])`,
        heap: `jvm_memory_used_bytes{application="${application}",area="heap"} / jvm_memory_max_bytes{application="${application}",area="heap"} * 100`,
        cpu: `process_cpu_usage{application="${application}"} * 100`,
        error: `rate(http_server_requests_seconds_count{application="${application}",status=~"5.."}[5m]) / rate(http_server_requests_seconds_count{application="${application}"}[5m]) * 100`,
    };

    return queries[metricType] || queries.tps;
}