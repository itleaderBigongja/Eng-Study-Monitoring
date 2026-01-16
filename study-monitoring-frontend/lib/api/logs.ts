import { get } from './client';
import { ENDPOINTS } from './endpoints';

// 검색 파라미터 타입 정의
export interface LogSearchParams {
    index: string;
    from: number;
    size: number;
    keyword?: string;
    logLevel?: string;
    startDate?: string;
    endDate?: string;
}

/**
 * 통합 로그 검색 API
 * @param params 검색 필터 및 페이징 정보
 */
export async function searchLogs(params: LogSearchParams): Promise<any> {
    // client.ts의 get 함수가 params 객체를 쿼리 스트링으로 변환하여 요청한다고 가정합니다.
    return get(ENDPOINTS.LOGS.SEARCH, params);
}

/**
 * 에러 로그 전용 조회 (필요 시 추가 확장 가능)
 */
export async function getErrorLogs(params: LogSearchParams): Promise<any> {
    return get(ENDPOINTS.LOGS.ERRORS, params);
}

/**
 * 로그 통계 데이터 조회 (필요 시 추가 확장 가능)
 */
export async function getLogStats(params: any): Promise<any> {
    return get(ENDPOINTS.LOGS.STATS, params);
}