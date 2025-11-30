'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

export default function LoginPage() {
    const router = useRouter();
    const [formData, setFormData] = useState({
        loginId: '',
        password: ''
    });
    const [errors, setErrors] = useState<Record<string, string>>({});
    const [isLoading, setIsLoading] = useState(false);
    const [rememberMe, setRememberMe] = useState(false);

    const validateForm = () => {
        const newErrors: Record<string, string> = {};

        if (!formData.loginId) {
            newErrors.loginId = '로그인 ID를 입력해주세요';
        }

        if (!formData.password) {
            newErrors.password = '비밀번호를 입력해주세요';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!validateForm()) return;

        setIsLoading(true);

        try {
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    loginId: formData.loginId,
                    password: formData.password
                }),
            });

            const data = await response.json();

            if (data.success) {
                // 로그인 성공
                alert('로그인에 성공했습니다!');
                router.push('/'); // 메인 페이지로 이동
            } else {
                alert(data.message || '로그인에 실패했습니다.');
            }
        } catch (error) {
            console.error('Login error:', error);
            alert('서버와의 연결에 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));

        // Clear error when user starts typing
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: '' }));
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-cyan-50 via-white to-blue-50 flex items-center justify-center px-4 py-12">
            <div className="max-w-md w-full">
                {/* Logo & Title */}
                <div className="text-center mb-8">
                    <div className="inline-block p-3 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-2xl mb-4 shadow-lg">
                        <svg className="w-12 h-12 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                        </svg>
                    </div>
                    <h1 className="text-3xl font-bold text-gray-800 mb-2">
                        로그인
                    </h1>
                    <p className="text-gray-600">
                        영어 학습을 시작하세요
                    </p>
                </div>

                {/* Login Form */}
                <div className="bg-white rounded-2xl shadow-xl p-8 border border-cyan-100">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {/* Login ID */}
                        <div>
                            <label htmlFor="loginId" className="block text-sm font-semibold text-gray-700 mb-2">
                                로그인 ID
                            </label>
                            <input
                                type="text"
                                id="loginId"
                                name="loginId"
                                value={formData.loginId}
                                onChange={handleChange}
                                className={`w-full px-4 py-3 rounded-xl border-2 ${
                                    errors.loginId
                                        ? 'border-red-300 focus:border-red-500'
                                        : 'border-gray-200 focus:border-cyan-400'
                                } focus:outline-none transition-colors duration-200`}
                                placeholder="아이디를 입력하세요"
                                autoComplete="username"
                            />
                            {errors.loginId && (
                                <p className="mt-1 text-sm text-red-500">{errors.loginId}</p>
                            )}
                        </div>

                        {/* Password */}
                        <div>
                            <label htmlFor="password" className="block text-sm font-semibold text-gray-700 mb-2">
                                비밀번호
                            </label>
                            <input
                                type="password"
                                id="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                className={`w-full px-4 py-3 rounded-xl border-2 ${
                                    errors.password
                                        ? 'border-red-300 focus:border-red-500'
                                        : 'border-gray-200 focus:border-cyan-400'
                                } focus:outline-none transition-colors duration-200`}
                                placeholder="비밀번호를 입력하세요"
                                autoComplete="current-password"
                            />
                            {errors.password && (
                                <p className="mt-1 text-sm text-red-500">{errors.password}</p>
                            )}
                        </div>

                        {/* Remember Me & Forgot Password */}
                        <div className="flex items-center justify-between">
                            <label className="flex items-center cursor-pointer">
                                <input
                                    type="checkbox"
                                    checked={rememberMe}
                                    onChange={(e) => setRememberMe(e.target.checked)}
                                    className="w-4 h-4 text-cyan-500 border-gray-300 rounded focus:ring-cyan-400 focus:ring-2"
                                />
                                <span className="ml-2 text-sm text-gray-600">로그인 상태 유지</span>
                            </label>
                            <Link
                                href="/forgot-password"
                                className="text-sm text-cyan-500 hover:text-cyan-600 font-medium transition-colors"
                            >
                                비밀번호 찾기
                            </Link>
                        </div>

                        {/* Submit Button */}
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full bg-gradient-to-r from-cyan-400 to-blue-500 text-white font-semibold py-3 px-6 rounded-xl hover:from-cyan-500 hover:to-blue-600 focus:outline-none focus:ring-4 focus:ring-cyan-300 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl"
                        >
                            {isLoading ? (
                                <span className="flex items-center justify-center">
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  로그인 중...
                </span>
                            ) : '로그인'}
                        </button>
                    </form>

                    {/* Divider */}
                    <div className="relative my-6">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-gray-200"></div>
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="px-4 bg-white text-gray-500">또는</span>
                        </div>
                    </div>

                    {/* Register Link */}
                    <div className="text-center">
                        <p className="text-gray-600">
                            아직 계정이 없으신가요?{' '}
                            <Link
                                href="/registor"
                                className="text-cyan-500 hover:text-cyan-600 font-semibold transition-colors"
                            >
                                회원가입
                            </Link>
                        </p>
                    </div>
                </div>

                {/* Features */}
                <div className="mt-8 grid grid-cols-3 gap-4 text-center">
                    <div className="bg-white rounded-xl p-4 shadow-md border border-cyan-50">
                        <div className="text-cyan-500 mb-2">
                            <svg className="w-8 h-8 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>
                        <p className="text-xs text-gray-600 font-medium">안전한 인증</p>
                    </div>
                    <div className="bg-white rounded-xl p-4 shadow-md border border-cyan-50">
                        <div className="text-cyan-500 mb-2">
                            <svg className="w-8 h-8 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                            </svg>
                        </div>
                        <p className="text-xs text-gray-600 font-medium">빠른 학습</p>
                    </div>
                    <div className="bg-white rounded-xl p-4 shadow-md border border-cyan-50">
                        <div className="text-cyan-500 mb-2">
                            <svg className="w-8 h-8 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                            </svg>
                        </div>
                        <p className="text-xs text-gray-600 font-medium">진도 추적</p>
                    </div>
                </div>
            </div>
        </div>
    );
}