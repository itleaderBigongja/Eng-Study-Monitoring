import apiClient from './api';

// 타입 정의
export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
    fullName?: string;
}

export interface AuthResponse {
    token: string;
    type: string;
    userId: number;
    username: string;
    email: string;
    fullName: string;
}

export interface User {
    userId: number;
    username: string;
    email: string;
    fullName: string;
}

// Auth API
export const authApi = {
    /**
     * 로그인
     */
    login: async (data: LoginRequest): Promise<AuthResponse> => {
        return apiClient.post<AuthResponse>('/auth/login', data);
    },

    /**
     * 회원가입
     */
    register: async (data: RegisterRequest): Promise<AuthResponse> => {
        return apiClient.post<AuthResponse>('/auth/register', data);
    },

    /**
     * 로그아웃
     */
    logout: () => {
        if (typeof window !== 'undefined') {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        }
    },

    /**
     * 토큰 가져오기
     */
    getToken: (): string | null => {
        if (typeof window !== 'undefined') {
            return localStorage.getItem('token');
        }
        return null;
    },

    /**
     * 사용자 정보 가져오기
     */
    getUser: (): User | null => {
        if (typeof window !== 'undefined') {
            const userStr = localStorage.getItem('user');
            return userStr ? JSON.parse(userStr) : null;
        }
        return null;
    },

    /**
     * 인증 여부 확인
     */
    isAuthenticated: (): boolean => {
        if (typeof window !== 'undefined') {
            return !!localStorage.getItem('token');
        }
        return false;
    },

    /**
     * 사용자 정보 저장
     */
    setUser: (user: User) => {
        if (typeof window !== 'undefined') {
            localStorage.setItem('user', JSON.stringify(user));
        }
    },

    /**
     * 토큰 저장
     */
    setToken: (token: string) => {
        if (typeof window !== 'undefined') {
            localStorage.setItem('token', token);
        }
    },
};