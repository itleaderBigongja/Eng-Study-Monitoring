// lib/api/alerts.ts
import { get, post, put, del, patch } from './client';
import { ENDPOINTS } from './endpoints';

// ============================================================================
// 타입 정의 (Backend DTO와 일치)
// ============================================================================

/**
 * 알림 규칙 응답 DTO
 */
export interface AlertRuleResponse {
    id: number;
    name: string;
    application: string;
    alertType?: string;
    metricType: string;
    condition: string;
    threshold: number;
    durationMinutes: number;
    severity?: string;
    notificationMethods: string[];
    notificationEmail?: string;
    notificationSlack?: string;
    active: boolean;
    lastTriggeredAt?: string;
    triggerCount?: number;
    createdAt: string;
    updatedAt: string;
    conditionDescription?: string;
}

/**
 * 알림 규칙 생성 요청 DTO
 */
export interface AlertRuleRequest {
    name: string;
    application: string;
    metricType: string;
    condition: string;
    threshold: number;
    durationMinutes: number;
    notificationMethods: string[];
    notificationEmail?: string;
    notificationSlack?: string;
    active: boolean;
}

/**
 * 알림 히스토리 응답 DTO
 */
export interface AlertHistoryResponse {
    id: number;
    alertRuleId: number;
    alertRuleName: string;
    application: string;
    metricType: string;
    triggeredAt: string;
    currentValue: number;
    thresholdValue: number;
    message: string;
    severity: 'CRITICAL' | 'ERROR' | 'WARNING' | 'INFO';
    resolved: boolean;
    resolvedAt?: string;
    resolvedMessage?: string;
    durationMinutes?: number;
    notificationSent: boolean;
    notificationMethods?: string;
    notificationResult?: string;
    notificationError?: string;
}

// ============================================================================
// 알림 규칙 API
// ============================================================================

/**
 * 전체 알림 규칙 조회
 */
export const getAlertRules = async (): Promise<AlertRuleResponse[]> => {
    return get<AlertRuleResponse[]>(ENDPOINTS.ALERTS.BASE);
};

/**
 * 활성화된 알림 규칙만 조회
 */
export const getActiveAlertRules = async (): Promise<AlertRuleResponse[]> => {
    return get<AlertRuleResponse[]>(ENDPOINTS.ALERTS.ACTIVE_ONLY);
};

/**
 * 특정 알림 규칙 조회
 */
export const getAlertRule = async (id: number): Promise<AlertRuleResponse> => {
    return get<AlertRuleResponse>(ENDPOINTS.ALERTS.DETAIL(id));
};

/**
 * 알림 규칙 생성
 */
export const createAlertRule = async (
    data: AlertRuleRequest
): Promise<AlertRuleResponse> => {
    return post<AlertRuleResponse>(ENDPOINTS.ALERTS.BASE, data);
};

/**
 * 알림 규칙 수정
 */
export const updateAlertRule = async (
    id: number,
    data: AlertRuleRequest
): Promise<AlertRuleResponse> => {
    return put<AlertRuleResponse>(ENDPOINTS.ALERTS.DETAIL(id), data);
};

/**
 * 알림 규칙 삭제
 */
export const deleteAlertRule = async (id: number): Promise<void> => {
    return del<void>(ENDPOINTS.ALERTS.DETAIL(id));
};

/**
 * 알림 규칙 활성화/비활성화 토글
 */
export const toggleAlertRule = async (id: number): Promise<AlertRuleResponse> => {
    return patch<AlertRuleResponse>(ENDPOINTS.ALERTS.TOGGLE(id));
};

// ============================================================================
// 알림 히스토리 API
// ============================================================================

/**
 * 알림 히스토리 조회 (페이징)
 */
export const getAlertHistory = async (
    page = 1,
    size = 20
): Promise<AlertHistoryResponse[]> => {
    return get<AlertHistoryResponse[]>(
        `${ENDPOINTS.ALERT_HISTORY.BASE}?page=${page}&size=${size}`
    );
};

/**
 * 미해결 알림만 조회
 */
export const getUnresolvedAlerts = async (): Promise<AlertHistoryResponse[]> => {
    return get<AlertHistoryResponse[]>(ENDPOINTS.ALERT_HISTORY.UNRESOLVED);
};

/**
 * 특정 규칙의 이력 조회
 */
export const getHistoryByRule = async (
    ruleId: number,
    page = 1,
    size = 20
): Promise<AlertHistoryResponse[]> => {
    return get<AlertHistoryResponse[]>(
        `${ENDPOINTS.ALERT_HISTORY.BY_RULE(ruleId)}?page=${page}&size=${size}`
    );
};

/**
 * 알림 해결 처리
 */
export const resolveAlert = async (
    historyId: number,
    message: string
): Promise<string> => {
    return put<string>(ENDPOINTS.ALERT_HISTORY.RESOLVE(historyId), { message });
};

/**
 * 특정 규칙의 모든 미해결 건 일괄 해결
 */
export const resolveAllByRule = async (
    ruleId: number,
    message: string
): Promise<string> => {
    return put<string>(ENDPOINTS.ALERT_HISTORY.RESOLVE_ALL(ruleId), { message });
};

// ============================================================================
// 유틸리티 함수들
// ============================================================================

/**
 * 날짜 포맷팅
 */
export const formatDateTime = (dateString: string): string => {
    try {
        const date = new Date(dateString);
        return date.toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false,
        });
    } catch {
        return dateString;
    }
};

/**
 * 지속 시간 포맷팅
 */
export const formatDuration = (minutes?: number): string => {
    if (!minutes) return '-';

    if (minutes < 60) {
        return `${minutes}분`;
    }

    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;

    if (mins === 0) {
        return `${hours}시간`;
    }

    return `${hours}시간 ${mins}분`;
};

/**
 * 메트릭 타입 → 표시 이름
 */
export const getMetricDisplayName = (metricType: string): string => {
    const names: Record<string, string> = {
        CPU_USAGE: 'CPU 사용률',
        HEAP_USAGE: 'Heap 메모리',
        TPS: 'TPS',
        ERROR_RATE: '에러율',
        DB_CONNECTIONS: 'DB 연결 수',
        DB_SIZE: 'DB 크기',
    };
    return names[metricType] || metricType;
};

/**
 * 메트릭 단위
 */
export const getMetricUnit = (metricType: string): string => {
    const units: Record<string, string> = {
        CPU_USAGE: '%',
        HEAP_USAGE: '%',
        ERROR_RATE: '%',
        TPS: ' req/s',
        DB_CONNECTIONS: '개',
        DB_SIZE: ' MB',
    };
    return units[metricType] || '';
};

/**
 * 심각도별 배지 스타일
 */
export const getSeverityBadgeClass = (severity: string): string => {
    const classes: Record<string, string> = {
        CRITICAL: 'bg-red-100 text-red-800',
        ERROR: 'bg-orange-100 text-orange-800',
        WARNING: 'bg-yellow-100 text-yellow-800',
        INFO: 'bg-blue-100 text-blue-800',
    };
    return classes[severity] || 'bg-gray-100 text-gray-800';
};

/**
 * 활성 상태별 배지 스타일
 */
export const getActiveBadgeClass = (active: boolean): string => {
    return active
        ? 'bg-green-100 text-green-800'
        : 'bg-gray-100 text-gray-800';
};