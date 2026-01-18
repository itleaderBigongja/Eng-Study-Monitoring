// lib/api/alerts.ts
import { get, post, del, patch } from './client';
import { ENDPOINTS } from './endpoints';

// ============================================================================
// 📌 Types (DTO)
// ============================================================================

export interface AlertRuleRequest {
    name: string;
    application: string;
    metricType: string;
    condition: string; // 'ABOVE', 'BELOW'
    threshold: number;
    durationMinutes: number;
    notificationMethods: string[];
    active: boolean;
}

export interface AlertRuleResponse {
    id: number;
    name: string;
    application: string;
    metricType: string;
    condition: string;
    threshold: number;
    durationMinutes: number;
    notificationMethods: string[];
    active: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface AlertHistoryResponse {
    id: number;
    alertRuleId: number;
    alertRuleName: string;
    application: string;
    triggeredAt: string;
    currentValue: number;
    message: string;
    resolved: boolean;
    resolvedAt?: string;
    durationMinutes?: number;
}

// ============================================================================
// 📌 API Service Functions
// ============================================================================

/** 모든 알림 규칙 조회 */
export const getAlertRules = async () => {
    return get<AlertRuleResponse[]>(ENDPOINTS.ALERTS.BASE);
};

/** 활성화된 알림 규칙만 조회 */
export const getActiveAlertRules = async () => {
    return get<AlertRuleResponse[]>(ENDPOINTS.ALERTS.BASE, { active: true });
};

/** 알림 규칙 생성 */
export const createAlertRule = async (data: AlertRuleRequest) => {
    return post<AlertRuleResponse>(ENDPOINTS.ALERTS.BASE, data);
};

/** 알림 규칙 삭제 */
export const deleteAlertRule = async (id: number) => {
    return del<void>(`${ENDPOINTS.ALERTS.BASE}/${id}`);
};

/** 알림 규칙 활성화/비활성화 토글 */
export const toggleAlertRule = async (id: number) => {
    return patch<AlertRuleResponse>(`${ENDPOINTS.ALERTS.BASE}/${id}/toggle`);
};

/** 알림 히스토리 조회 (페이징) */
export const getAlertHistory = async (page: number = 0, size: number = 20) => {
    return get<AlertHistoryResponse[]>(ENDPOINTS.ALERTS.HISTORY, { page, size });
};

/** 알림 해결 처리 */
export const resolveAlert = async (historyId: number, message: string = '수동 해결') => {
    return patch<void>(`${ENDPOINTS.ALERTS.HISTORY}/${historyId}/resolve`, { message });
};