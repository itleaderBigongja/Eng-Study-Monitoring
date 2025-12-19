// lib/api.ts - 모니터링 API 클라이언트

const API_BASE_URL = process.env.NEXT_PUBLIC_MONITORING_API || 'http://localhost:8081/api';

interface ApiResponse<T> {
    success: boolean;
    message?: string;
    data?: T;
}

class MonitoringApiClient {
    private baseUrl: string;

    constructor(baseUrl: string) {
        this.baseUrl = baseUrl;
    }

    /**
     * GET 요청
     */
    async get<T>(endpoint: string, params?: Record<string, any>): Promise<T> {
        const url = new URL(`${this.baseUrl}${endpoint}`);

        if (params) {
            Object.entries(params).forEach(([key, value]) => {
                if (value !== undefined && value !== null) {
                    url.searchParams.append(key, String(value));
                }
            });
        }

        const response = await fetch(url.toString(), {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            throw new Error(`API Error: ${response.status} ${response.statusText}`);
        }

        const result: ApiResponse<T> = await response.json();

        if (!result.success) {
            throw new Error(result.message || 'API request failed');
        }

        return result.data as T;
    }

    /**
     * POST 요청
     */
    async post<T>(endpoint: string, body?: any): Promise<T> {
        const response = await fetch(`${this.baseUrl}${endpoint}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(body),
        });

        if (!response.ok) {
            throw new Error(`API Error: ${response.status} ${response.statusText}`);
        }

        const result: ApiResponse<T> = await response.json();

        if (!result.success) {
            throw new Error(result.message || 'API request failed');
        }

        return result.data as T;
    }
}

// API 클라이언트 인스턴스
const apiClient = new MonitoringApiClient(API_BASE_URL);

// ===================================
// Dashboard API
// ===================================

export interface ProcessStatus {
    processId: number;
    processName: string;
    processType: string;
    status: string;
    cpuUsage: number;
    memoryUsage: number;
    uptime: string;
    lastHealthCheck?: string;
}

export interface ApplicationMetrics {
    tps: number;
    heapUsage: number;
    errorRate: number;
    responseTime: number;
}

export interface DashboardOverview {
    processes: ProcessStatus[];
    metrics: {
        engStudy: ApplicationMetrics;
        monitoring: ApplicationMetrics;
    };
    recentErrors: Array<{
        id: string;
        timestamp: string;
        logLevel: string;
        message: string;
        application: string;
    }>;
    logCounts: {
        [key: string]: number;
    };
    statistics: {
        totalRequests: number;
        avgResponseTime: number;
        uptime: string;
    };
}

/**
 * 대시보드 전체 현황 조회
 */
export async function getDashboardOverview(): Promise<DashboardOverview> {
    return apiClient.get<DashboardOverview>('/dashboard/overview');
}

/**
 * 실시간 메트릭 조회
 */
export interface MetricsQueryParams {
    application: string;  // 'eng-study' | 'monitoring'
    metric: string;       // 'tps' | 'heap' | 'error_rate' | 'response_time'
    hours?: number;       // 1~168 (기본: 1)
}

export interface DataPoint {
    timestamp: number;
    value: number;
}

export interface MetricsResponse {
    application: string;
    metric: string;
    data: DataPoint[];
    start: number;
    end: number;
}

export async function getMetrics(params: MetricsQueryParams): Promise<MetricsResponse> {
    return apiClient.get<MetricsResponse>('/dashboard/metrics', params);
}

/**
 * 프로세스 현황 조회
 */
export async function getProcesses(): Promise<{ processes: ProcessStatus[]; summary: any }> {
    return apiClient.get('/dashboard/processes');
}

// ===================================
// Logs API
// ===================================

export interface LogSearchParams {
    index?: string;       // 인덱스 패턴 (기본: 'application-logs-*')
    keyword?: string;     // 검색 키워드
    logLevel?: string;    // 'INFO' | 'WARN' | 'ERROR'
    from?: number;        // 페이지 시작 (기본: 0)
    size?: number;        // 페이지 크기 (기본: 50)
}

export interface LogEntry {
    id: string;
    index: string;
    timestamp: string;
    logLevel: string;
    loggerName: string;
    message: string;
    application: string;
    stackTrace?: string;
}

export interface LogSearchResponse {
    total: number;
    logs: LogEntry[];
    from: number;
    size: number;
}

/**
 * 로그 검색
 */
export async function searchLogs(params: LogSearchParams): Promise<LogSearchResponse> {
    return apiClient.get<LogSearchResponse>('/logs/search', params);
}

/**
 * 최근 에러 로그 조회
 */
export async function getRecentErrors(limit: number = 20): Promise<{ total: number; errors: LogEntry[] }> {
    return apiClient.get('/logs/errors', { limit });
}

/**
 * 로그 통계 조회
 */
export async function getLogStats(index: string = 'application-logs-*'): Promise<{ index: string; stats: Record<string, number> }> {
    return apiClient.get('/logs/stats', { index });
}

// ===================================
// Metrics API (상세)
// ===================================

export interface PrometheusQueryParams {
    query: string;
    start?: number;
    end?: number;
    step?: string;
}

/**
 * PromQL 쿼리 실행
 */
export async function executePrometheusQuery(params: PrometheusQueryParams): Promise<any> {
    return apiClient.post('/metrics/query', params);
}

/**
 * 현재 메트릭 조회
 */
export async function getCurrentMetrics(application: string = 'eng-study'): Promise<{
    application: string;
    metrics: {
        tps: number;
        heapUsage: number;
        errorRate: number;
        responseTime: number;
        timestamp: string;
    };
}> {
    return apiClient.get('/metrics/current', { application });
}

/**
 * 시간 범위 메트릭 조회
 */
export async function getRangeMetrics(params: PrometheusQueryParams): Promise<{
    query: string;
    start: number;
    end: number;
    step: string;
    data: any[];
}> {
    return apiClient.post('/metrics/range', params);
}

// ===================================
// Utility Functions
// ===================================

/**
 * 에러 핸들링 래퍼
 */
export async function withErrorHandling<T>(
    apiCall: () => Promise<T>,
    fallbackValue: T
): Promise<T> {
    try {
        return await apiCall();
    } catch (error) {
        console.error('API Error:', error);
        return fallbackValue;
    }
}

/**
 * 재시도 로직
 */
export async function withRetry<T>(
    apiCall: () => Promise<T>,
    maxRetries: number = 3,
    delay: number = 1000
): Promise<T> {
    for (let i = 0; i < maxRetries; i++) {
        try {
            return await apiCall();
        } catch (error) {
            if (i === maxRetries - 1) throw error;
            await new Promise(resolve => setTimeout(resolve, delay * (i + 1)));
        }
    }
    throw new Error('Max retries exceeded');
}

export default apiClient;