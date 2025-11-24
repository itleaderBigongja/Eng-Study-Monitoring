'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { authApi, User } from '@/lib/auth';

export function useAuth(requireAuth: boolean = true) {
    const router = useRouter();
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!authApi.isAuthenticated()) {
            if (requireAuth) {
                router.push('/login');
            }
            setLoading(false);
        } else {
            const userData = authApi.getUser();
            setUser(userData);
            setLoading(false);
        }
    }, [router, requireAuth]);

    return { user, loading, isAuthenticated: authApi.isAuthenticated() };
}