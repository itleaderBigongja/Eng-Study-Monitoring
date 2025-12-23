// API 엔드포인트 상수 정의

export const ENDPOINTS = {
    // Health Check
    HEALTH: {
        BASE: '/api/health',
        ELASTICSEARCH: '/api/health/elasticsearch',
        DATABASE: '/api/health/database',
        PROMETHEUS: '/api/health/prometheus',
    },

    // Dashboard
    DASHBOARD: {
        OVERVIEW: '/api/dashboard/overview',
        METRICS: '/api/dashboard/metrics',
        PROCESSES: '/api/dashboard/processes',
    },

    // Logs
    LOGS: {
        SEARCH: '/api/logs/search',
        ERRORS: '/api/logs/errors',
        STATS: '/api/logs/stats',
    },

    // Metrics
    METRICS: {
        QUERY: '/api/metrics/query',
        CURRENT: '/api/metrics/current',
        RANGE: '/api/metrics/range',
    },

    // Statistics
    STATISTICS: {
        TIMESERIES: '/api/statistics/timeseries',
        LOGS: '/api/statistics/logs',
        ACCESS_LOGS: '/api/statistics/access-logs',
        ERROR_LOGS: '/api/statistics/error-logs',
        PERFORMANCE_METRICS: '/api/statistics/performance-metrics',
        DATABASE_LOGS: '/api/statistics/database-logs',
        AUDIT_LOGS: '/api/statistics/audit-logs',
        SECURITY_LOGS: '/api/statistics/security-logs',
    },
} as const;