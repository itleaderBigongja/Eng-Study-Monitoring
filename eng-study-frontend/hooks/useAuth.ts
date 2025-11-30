import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import * as authApi from '@/lib/auth';

export interface User {
    usersId: number;
    loginId: string;
    fullName: string;
    email: string;
    role?: string;
}

export function useAuth() {
    const router = useRouter();
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    /**
     * 사용자 정보 가져오기
     */
    const fetchUser = useCallback(async () => {
        try {
            setLoading(true);
            const result = await authApi.getMyInfo();

            if (result.success && result.data) {
                setUser(result.data);
                setError(null);
            } else {
                setUser(null);
            }
        } catch (err) {
            console.error('Failed to fetch user:', err);
            setUser(null);
            setError('사용자 정보를 가져오는데 실패했습니다.');
        } finally {
            setLoading(false);
        }
    }, []);

    /**
     * 로그인
     */
    const login = useCallback(async (loginId: string, password: string) => {
        try {
            setLoading(true);
            setError(null);

            const result = await authApi.login({ loginId, password });

            if (result.success && result.data?.user) {
                setUser(result.data.user);
                return { success: true, message: result.message };
            } else {
                setError(result.message);
                return { success: false, message: result.message };
            }
        } catch (err) {
            const message = '로그인 중 오류가 발생했습니다.';
            setError(message);
            return { success: false, message };
        } finally {
            setLoading(false);
        }
    }, []);

    /**
     * 로그아웃
     */
    const logout = useCallback(async () => {
        try {
            setLoading(true);
            await authApi.logout();
            setUser(null);
            router.push('/login');
        } catch (err) {
            console.error('Logout failed:', err);
            setError('로그아웃 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    }, [router]);

    /**
     * 회원가입
     */
    const register = useCallback(async (data: authApi.RegisterData) => {
        try {
            setLoading(true);
            setError(null);

            const result = await authApi.register(data);

            if (result.success) {
                return { success: true, message: result.message };
            } else {
                setError(result.message);
                return { success: false, message: result.message };
            }
        } catch (err) {
            const message = '회원가입 중 오류가 발생했습니다.';
            setError(message);
            return { success: false, message };
        } finally {
            setLoading(false);
        }
    }, []);

    /**
     * 토큰 갱신
     */
    const refresh = useCallback(async () => {
        try {
            const result = await authApi.refreshToken();

            if (result.success && result.data?.user) {
                setUser(result.data.user);
                return true;
            }
            return false;
        } catch (err) {
            console.error('Token refresh failed:', err);
            return false;
        }
    }, []);

    /**
     * 초기 사용자 정보 로드
     */
    useEffect(() => {
        fetchUser();
    }, [fetchUser]);

    /**
     * 인증 여부 확인
     */
    const isAuthenticated = !!user;

    /**
     * 관리자 여부 확인
     */
    const isAdmin = user?.role === 'ADMIN';

    return {
        user,
        loading,
        error,
        isAuthenticated,
        isAdmin,
        login,
        logout,
        register,
        refresh,
        fetchUser,
    };
}