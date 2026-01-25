// lib/api/client.ts

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8081';
const API_TIMEOUT = parseInt(process.env.NEXT_PUBLIC_API_TIMEOUT || '300000');

// 백엔드 응답 껍데기(Wrapper) 타입 정의
interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
    timestamp?: string;
    errorCode?: string | null;
}

export class ApiError extends Error {
    constructor(
        message: string,
        public status?: number,
        public data?: any
    ) {
        super(message);
        this.name = 'ApiError';
    }
}

// ✅ [핵심] 내부적으로 사용하는 request 함수
async function request<T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<T> {
    const url = `${BASE_URL}${endpoint}`;

    const config: RequestInit = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
        },
    };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT);
    config.signal = controller.signal;

    try {
        console.log(`[API Request] ${options.method || 'GET'} ${url}`);
        const response = await fetch(url, config);
        clearTimeout(timeoutId);

        // 1. 응답을 JSON으로 파싱
        const responseData = await response.json();

        // 2. HTTP 상태 코드가 실패(400~500번대)인 경우
        if (!response.ok) {
            throw new ApiError(
                responseData.message || 'API 요청 실패',
                response.status,
                responseData
            );
        }

        // ✅ [수정됨] 백엔드 논리적 에러 체크 (success: false인 경우)
        if (responseData.success === false) {
            throw new ApiError(
                responseData.message || '요청 처리 실패',
                response.status,
                responseData
            );
        }

        console.log(`[API Response] ${url} - Success`);

        // ✅ [수정됨 - 가장 중요한 부분]
        // 대시보드와 통계 페이지 모두를 위해 'data' 알맹이만 반환합니다.
        // 만약 data 필드가 없다면(예외 케이스) 전체를 반환합니다.
        if (responseData.data !== undefined) {
            return responseData.data;
        }

        return responseData;

    } catch (error) {
        clearTimeout(timeoutId);
        if (error instanceof ApiError) throw error;
        if ((error as Error).name === 'AbortError') {
            throw new ApiError('요청 시간 초과', 408);
        }
        console.error(`[API Error] ${url}`, error);
        throw new ApiError('네트워크 오류가 발생했습니다');
    }
}

// --- 아래 함수들은 그대로 사용하시면 됩니다 ---

export async function get<T>(endpoint: string, params?: Record<string, any>): Promise<T> {
    let queryString = '';
    if (params) {
        const searchParams = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== '') {
                searchParams.append(key, String(value));
            }
        });
        const qs = searchParams.toString();
        if (qs) queryString = '?' + qs;
    }
    return request<T>(`${endpoint}${queryString}`, { method: 'GET' });
}

export async function post<T>(endpoint: string, body?: any): Promise<T> {
    return request<T>(endpoint, { method: 'POST', body: JSON.stringify(body) });
}

export async function put<T>(endpoint: string, body?: any): Promise<T> {
    return request<T>(endpoint, { method: 'PUT', body: JSON.stringify(body) });
}

export async function del<T>(endpoint: string): Promise<T> {
    return request<T>(endpoint, { method: 'DELETE' });
}

export async function patch<T>(endpoint: string, body?: any): Promise<T> {
    return request<T>(endpoint, { method: 'PATCH', body: JSON.stringify(body) });
}

export async function checkApiHealth(): Promise<boolean> {
    try {
        await get('/api/health');
        return true;
    } catch {
        return false;
    }
}