// lib/api/endpoints.ts
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
        ERRORS: '/api/dashboard/errors',
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

    // Alerts Endpoints
    ALERTS: {
        BASE: '/api/alerts',                   // 목록 조회, 생성
        HISTORY: '/api/alerts/history',        // 히스토리 조회
        UNRESOLVED: '/api/alerts/history/unresolved', // 미해결 알림
        BY_APP: '/api/alerts/application',     // 앱별 조회
    },
} as const;