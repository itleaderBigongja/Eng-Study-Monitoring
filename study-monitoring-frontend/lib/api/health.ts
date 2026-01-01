import { get } from './client';
import { ENDPOINTS } from './endpoints';

export interface HealthStatus {
    status: 'UP' | 'DOWN';
    [key: string]: any;
}

/**
 * 전체 헬스체크
 */
export async function getHealthStatus(): Promise<HealthStatus> {
    // ENDPOINT.HEALTH.BASE = '/api/health'
    // 실제 전송은 client.ts파일의 get 함수에게 위임
    return get<HealthStatus>(ENDPOINTS.HEALTH.BASE);
}

/**
 * Elasticsearch 헬스체크
 */
export async function getElasticsearchHealth(): Promise<HealthStatus> {
    return get<HealthStatus>(ENDPOINTS.HEALTH.ELASTICSEARCH);
}

/**
 * Database 헬스체크
 */
export async function getDatabaseHealth(): Promise<HealthStatus> {
    return get<HealthStatus>(ENDPOINTS.HEALTH.DATABASE);
}

/**
 * Prometheus 헬스체크
 */
export async function getPrometheusHealth(): Promise<HealthStatus> {
    return get<HealthStatus>(ENDPOINTS.HEALTH.PROMETHEUS);
}