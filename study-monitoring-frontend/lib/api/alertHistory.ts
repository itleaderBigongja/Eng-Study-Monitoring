import { ENDPOINTS } from './endpoints';

// ============================================================================
// 타입 정의 (Backend DTO와 일치)
// ============================================================================

export interface AlertHistory {
    id: number;
    alertRuleId: number;
    alertRuleName: string;
    application: string;
    metricType: string;
    triggeredAt: string; // ISO DateTime string
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

export interface ApiResponse<T> {
    success: boolean;
    message: string;
    data: T;
    timestamp: string;
    errorCode?: string;
    errorDetails?: string;
}

export interface ResolveRequest {
    message: string;
}

// ============================================================================
// API 호출 함수들
// ============================================================================

const BASE_URL = 'http://localhost:8081';

export const alertHistoryApi = {
    /**
     * 최근 알림 히스토리 조회
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @returns 알림 히스토리 배열
     */
    getRecentHistory: async (page = 0, size = 20): Promise<AlertHistory[]> => {
        try {
            const response = await fetch(
                `${BASE_URL}/api/alerts/history?page=${page}&size=${size}`
            );

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result: ApiResponse<AlertHistory[]> = await response.json();

            if (!result.success) {
                throw new Error(result.message || '알림 히스토리 조회 실패');
            }

            return result.data;
        } catch (error) {
            console.error('Failed to fetch alert history:', error);
            throw error;
        }
    },

    /**
     * 미해결 알림 조회
     *
     * @returns 미해결 알림 배열
     */
    getUnresolved: async (): Promise<AlertHistory[]> => {
        try {
            const response = await fetch(
                `${BASE_URL}/api/alerts/history/unresolved`
            );

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result: ApiResponse<AlertHistory[]> = await response.json();

            if (!result.success) {
                throw new Error(result.message || '미해결 알림 조회 실패');
            }

            return result.data;
        } catch (error) {
            console.error('Failed to fetch unresolved alerts:', error);
            throw error;
        }
    },

    /**
     * 특정 알림 규칙의 히스토리 조회
     *
     * @param ruleId 알림 규칙 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @returns 알림 히스토리 배열
     */
    getHistoryByRule: async (
        ruleId: number,
        page = 0,
        size = 20
    ): Promise<AlertHistory[]> => {
        try {
            const response = await fetch(
                `${BASE_URL}/api/alerts/${ruleId}/history?page=${page}&size=${size}`
            );

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result: ApiResponse<AlertHistory[]> = await response.json();

            if (!result.success) {
                throw new Error(result.message || '알림 히스토리 조회 실패');
            }

            return result.data;
        } catch (error) {
            console.error('Failed to fetch history by rule:', error);
            throw error;
        }
    },

    /**
     * 알림 해결 처리
     *
     * @param historyId 히스토리 ID
     * @param message 해결 메시지
     * @returns 성공 메시지
     */
    resolveAlert: async (historyId: number, message: string): Promise<string> => {
        try {
            const body: ResolveRequest = { message };

            const response = await fetch(
                `${BASE_URL}/api/alerts/history/${historyId}/resolve`,
                {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(body),
                }
            );

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result: ApiResponse<null> = await response.json();

            if (!result.success) {
                throw new Error(result.message || '알림 해결 처리 실패');
            }

            return result.message;
        } catch (error) {
            console.error('Failed to resolve alert:', error);
            throw error;
        }
    }
};

// ============================================================================
// 유틸리티 함수들
// ============================================================================

/**
 * 날짜 포맷팅
 */
export const formatDateTime = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
    });
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
    const names: { [key: string]: string } = {
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
    const units: { [key: string]: string } = {
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
 * 심각도별 색상 클래스
 */
export const getSeverityColor = (severity: string): string => {
    const colors: { [key: string]: string } = {
        CRITICAL: 'red',
        ERROR: 'orange',
        WARNING: 'yellow',
        INFO: 'blue',
    };
    return colors[severity] || 'gray';
};

/**
 * 심각도별 배지 스타일
 */
export const getSeverityBadgeClass = (severity: string): string => {
    const classes: { [key: string]: string } = {
        CRITICAL: 'bg-red-100 text-red-800',
        ERROR: 'bg-orange-100 text-orange-800',
        WARNING: 'bg-yellow-100 text-yellow-800',
        INFO: 'bg-blue-100 text-blue-800',
    };
    return classes[severity] || 'bg-gray-100 text-gray-800';
};