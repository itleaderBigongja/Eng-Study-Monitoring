export interface MetricDataPoint {
    timestamp: number;
    value: number;
}

export interface ApplicationMetrics {
    tps: number;
    heapUsage: number;
    errorRate: number;
    responseTime: number;
}

export interface ProcessStatus {
    processId: number;
    processName: string;
    processType: string;
    status: 'RUNNING' | 'STOPPED' | 'ERROR';
    cpuUsage: number;
    memoryUsage: number;
    uptime: string;
    lastHealthCheck: string;
}

export interface DashboardData {
    processes: ProcessStatus[];
    metrics: {
        engStudy: ApplicationMetrics;
        monitoring: ApplicationMetrics;
    };
    recentErrors: ErrorLog[];
    logCounts: Record<string, number>;
    statistics: SystemStatistics;
}

export interface SystemStatistics {
    totalRequests: number;
    avgResponseTime: number;
    uptime: string;
}

export interface ErrorLog {
    id: string;
    timestamp: string;
    logLevel: string;
    message: string;
    application: string;
}