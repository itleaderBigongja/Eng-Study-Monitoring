// 경로 : /Monitering/study-monitoring-frontend/lib/types/dashboard.ts

export interface ProcessStatus {
    processId: number;
    processName: string;
    processType: string;
    status: 'RUNNING' | 'STOPPED' | 'ERROR' | 'STARTING' | 'STOPPING';
    cpuUsage: number;
    memoryUsage: number;
    uptime: string;
    lastHealthCheck: string;
}

export interface ApplicationMetrics {
    tps: number | null;
    heapUsage: number | null;
    errorRate: number | null;
    responseTime: number | null;
}

export interface MetricsSummary {
    engStudy: ApplicationMetrics;
    monitoring: ApplicationMetrics;
}

export interface ErrorLog {
    id: string;
    timestamp: string;
    logLevel: string;
    message: string;
    application: string;
}

export interface LogCounts {
    [logLevel: string]: number;
}

export interface SystemStatistics {
    totalRequests: number;
    avgResponseTime: number;
    uptime: string;
}

export interface DashboardOverview {
    processes: ProcessStatus[];
    metrics: MetricsSummary;
    recentErrors: ErrorLog[];
    logCounts: LogCounts;
    statistics: SystemStatistics;
}

// 메트릭 쿼리 요청
export interface MetricsQueryRequest {
    application: string;
    metric: string;
    hours?: number;
}

// 메트릭 응답
export interface MetricsResponse {
    application: string;
    metric: string;
    data: Array<{
        timestamp: number;
        value: number;
    }>;
    start: number;
    end: number;
}

// 프로세스 요약
export interface ProcessSummary {
    total: number;
    running: number;
    stopped: number;
    error: number;
}

// 에러 로그 관련 타입 추가
export interface ErrorLogItem {
    id: string;
    timestamp: string;
    logLevel: string;
    message: string;
    application: string;
}

// 페이지 응답 타입 추가
export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    size: number;
}

// 에러 로그 쿼리 파라미터 타입 추가
export interface ErrorLogQueryParams {
    type: 'APP' | 'SYSTEM';
    page: number;
    size: number;
}