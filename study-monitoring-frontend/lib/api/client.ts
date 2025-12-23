// API 클라이언트 설정 및 공통 에러 처리

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8081';
const API_TIMEOUT = parseInt(process.env.NEXT_PUBLIC_API_TIMEOUT || '30000');

export interface ApiResponse<T = any> {
    success: boolean;
    message: string;
    data: T;
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

/**
 * API 요청 공통 함수
 */
async function request<T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;

    const config: RequestInit = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
        },
    };

    // Timeout 설정
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT);
    config.signal = controller.signal;

    try {
        console.log(`[API Request] ${options.method || 'GET'} ${url}`);

        const response = await fetch(url, config);
        clearTimeout(timeoutId);

        // JSON 파싱
        const data = await response.json();

        // 에러 응답 처리
        if (!response.ok) {
            throw new ApiError(
                data.message || 'API 요청 실패',
                response.status,
                data
            );
        }

        // 백엔드 ApiResponseDTO 구조 확인
        if (data.success === false) {
            throw new ApiError(data.message || '요청 처리 실패', response.status, data);
        }

        console.log(`[API Response] ${url} - Success`);
        return data.data || data; // data.data 또는 data 자체 반환

    } catch (error) {
        clearTimeout(timeoutId);

        if (error instanceof ApiError) {
            throw error;
        }

        if ((error as Error).name === 'AbortError') {
            throw new ApiError('요청 시간 초과', 408);
        }

        console.error(`[API Error] ${url}`, error);
        throw new ApiError('네트워크 오류가 발생했습니다');
    }
}

/**
 * GET 요청
 */
export async function get<T>(
    endpoint: string,
    params?: Record<string, any>
): Promise<T> {
    // 쿼리 파라미터 추가
    const queryString = params
        ? '?' + new URLSearchParams(params as any).toString()
        : '';

    return request<T>(`${endpoint}${queryString}`, {
        method: 'GET',
    });
}

/**
 * POST 요청
 */
export async function post<T>(
    endpoint: string,
    body?: any
): Promise<T> {
    return request<T>(endpoint, {
        method: 'POST',
        body: JSON.stringify(body),
    });
}

/**
 * PUT 요청
 */
export async function put<T>(
    endpoint: string,
    body?: any
): Promise<T> {
    return request<T>(endpoint, {
        method: 'PUT',
        body: JSON.stringify(body),
    });
}

/**
 * DELETE 요청
 */
export async function del<T>(
    endpoint: string
): Promise<T> {
    return request<T>(endpoint, {
        method: 'DELETE',
    });
}

/**
 * API 상태 확인
 */
export async function checkApiHealth(): Promise<boolean> {
    try {
        await get('/api/health');
        return true;
    } catch {
        return false;
    }
}