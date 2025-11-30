/**
 * 인증 관련 TypeScript 타입 정의
 */

// 사용자 역할
export enum UserRole {
    USER = 'USER',
    ADMIN = 'ADMIN',
}

// 사용자 정보
export interface User {
    usersId: number;
    loginId: string;
    fullName: string;
    email: string;
    role: UserRole;
    createdAt?: string;
    updatedAt?: string;
}

// 회원가입 요청
export interface RegisterRequest {
    loginId: string;
    password: string;
    email: string;
    fullName: string;
}

// 로그인 요청
export interface LoginRequest {
    loginId: string;
    password: string;
}

// 인증 응답
export interface AuthResponse {
    success: boolean;
    message: string;
    data?: {
        user: User;
        accessToken?: string;  // 실제로는 Cookie로 전달됨
        refreshToken?: string; // 실제로는 Cookie로 전달됨
    };
}

// API 응답 기본 타입
export interface ApiResponse<T = any> {
    success: boolean;
    message: string;
    data?: T;
}

// 로그인 ID 중복 확인 응답
export interface CheckLoginIdResponse {
    success: boolean;
    available: boolean;
    message: string;
}

// 이메일 중복 확인 응답
export interface CheckEmailResponse {
    success: boolean;
    available: boolean;
    message: string;
}

// 폼 유효성 검사 오류
export interface FormErrors {
    [key: string]: string;
}

// 인증 상태
export interface AuthState {
    user: User | null;
    loading: boolean;
    error: string | null;
    isAuthenticated: boolean;
}