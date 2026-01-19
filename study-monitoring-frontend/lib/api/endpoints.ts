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

    // ============================================================================
    // 🔔 Alerts Endpoints
    // ============================================================================
    ALERTS: {
        // 기본 CRUD
        BASE: '/api/alerts',                              // GET: 목록, POST: 생성
        DETAIL: (id: number) => `/api/alerts/${id}`,      // GET: 단건, PUT: 수정, DELETE: 삭제
        TOGGLE: (id: number) => `/api/alerts/${id}/toggle`, // PATCH: 활성화/비활성화
        ACTIVE_ONLY: '/api/alerts?active=true',          // 활성화된 것만
    },

    // ============================================================================
    // 📊 Alert History Endpoints (새로운 구조)
    // ============================================================================
    ALERT_HISTORY: {
        // 기본 조회
        BASE: '/api/v1/alert-history',                    // GET: 전체 히스토리 (페이징)
        UNRESOLVED: '/api/v1/alert-history/unresolved',   // GET: 미해결 알림만
        BY_RULE: (ruleId: number) => `/api/v1/alert-history/rule/${ruleId}`, // GET: 특정 규칙의 이력

        // 해결 처리
        RESOLVE: (historyId: number) => `/api/v1/alert-history/${historyId}/resolve`, // PUT: 단건 해결
        RESOLVE_ALL: (ruleId: number) => `/api/v1/alert-history/rule/${ruleId}/resolve-all`, // PUT: 일괄 해결
    },

} as const;