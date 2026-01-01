// 경로 : /Monitering/study-monitoring-frontend/lib/api/dashboard.ts
import { get } from './client';
import { ENDPOINTS } from './endpoints';
import {
    DashboardOverview,
    MetricsQueryRequest,
    MetricsResponse,
    ProcessSummary,
    ErrorLogQueryParams,
    ErrorLogItem,
    PageResponse,
} from '../types/dashboard';

/**
 * 대시보드 전체 개요 조회
 */
export async function getDashboardOverview(): Promise<DashboardOverview> {
    return get<DashboardOverview>(ENDPOINTS.DASHBOARD.OVERVIEW);
}

/**
 * 메트릭 조회
 */
export async function getDashboardMetrics(
    params: MetricsQueryRequest
): Promise<MetricsResponse> {
    return get<MetricsResponse>(ENDPOINTS.DASHBOARD.METRICS, params);
}

/**
 * 프로세스 목록 및 요약 조회
 */
export async function getProcesses(): Promise<{
    processes: any[];
    summary: ProcessSummary;
}> {
    return get(ENDPOINTS.DASHBOARD.PROCESSES);
}

/**
 * 에러 로그 조회 (페이징)
 */
export async function getErrorLogs(
    params: ErrorLogQueryParams
): Promise<PageResponse<ErrorLogItem>> {
    return get<PageResponse<ErrorLogItem>>(ENDPOINTS.DASHBOARD.ERRORS, params);
}