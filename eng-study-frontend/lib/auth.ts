/**
 * 인증 관련 헬퍼 함수
 */

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export interface RegisterData {
    loginId: string;
    password: string;
    email: string;
    fullName: string;
}

export interface LoginData {
    loginId: string;
    password: string;
}

export interface ApiResponse<T = any> {
    success: boolean;
    message: string;
    data?: T;
}

export interface UserInfo {
    usersId: number;
    loginId: string;
    fullName: string;
    email: string;
    role?: string;
    createdAt?: string;
}

/**
 * 회원가입
 */
export async function register(data: RegisterData): Promise<ApiResponse<{ user: UserInfo }>> {
    const response = await fetch(`${API_URL}/auth/register`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(data),
    });

    return response.json();
}

/**
 * 로그인
 */
export async function login(data: LoginData): Promise<ApiResponse<{ user: UserInfo }>> {
    const response = await fetch(`${API_URL}/auth/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(data),
    });

    return response.json();
}

/**
 * 로그아웃
 */
export async function logout(): Promise<ApiResponse> {
    const response = await fetch(`${API_URL}/auth/logout`, {
        method: 'POST',
        credentials: 'include',
    });

    return response.json();
}

/**
 * 토큰 갱신
 */
export async function refreshToken(): Promise<ApiResponse<{ user: UserInfo }>> {
    const response = await fetch(`${API_URL}/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
    });

    return response.json();
}

/**
 * 내 정보 조회
 */
export async function getMyInfo(): Promise<ApiResponse<UserInfo>> {
    const response = await fetch(`${API_URL}/auth/me`, {
        method: 'GET',
        credentials: 'include',
    });

    return response.json();
}

/**
 * 로그인 ID 중복 확인
 */
export async function checkLoginIdAvailability(loginId: string): Promise<ApiResponse<{ available: boolean }>> {
    const response = await fetch(`${API_URL}/auth/check-loginId?loginId=${encodeURIComponent(loginId)}`, {
        method: 'GET',
        credentials: 'include',
    });

    return response.json();
}

/**
 * 이메일 중복 확인
 */
export async function checkEmailAvailability(email: string): Promise<ApiResponse<{ available: boolean }>> {
    const response = await fetch(`${API_URL}/auth/check-email`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(email),
    });

    return response.json();
}

/**
 * 인증 상태 확인
 */
export async function checkAuthStatus(): Promise<boolean> {
    try {
        const result = await getMyInfo();
        return result.success;
    } catch {
        return false;
    }
}