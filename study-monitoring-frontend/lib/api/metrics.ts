import { get, post } from './client';
import { ENDPOINTS } from './endpoints';

export interface MetricsQueryRequest {
    query: string;
    start?: number;
    end?: number;
    step?: string;
}

export interface CurrentMetricsParams {
    application?: string;
}

/**
 * PromQL 쿼리 실행
 */
export async function executeMetricsQuery(request: MetricsQueryRequest): Promise<any> {
    return post(ENDPOINTS.METRICS.QUERY, request);
}

/**
 * 현재 메트릭 조회
 */
export async function getCurrentMetrics(params?: CurrentMetricsParams): Promise<any> {
    return get(ENDPOINTS.METRICS.CURRENT, params);
}

/**
 * Range 메트릭 조회
 */
export async function getRangeMetrics(request: MetricsQueryRequest): Promise<any> {
    return post(ENDPOINTS.METRICS.RANGE, request);
}