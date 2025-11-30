'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

interface User {
    usersId: number;
    loginId: string;
    fullName: string;
    email: string;
}

export default function HomePage() {
    const router = useRouter();
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        checkAuth();
    }, []);

    const checkAuth = async () => {
        try {
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/auth/me`, {
                credentials: 'include',
            });

            const data = await response.json();

            if (data.success && data.data) {
                setUser(data.data);
            }
        } catch (error) {
            console.error('Auth check failed:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = async () => {
        try {
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/auth/logout`, {
                method: 'POST',
                credentials: 'include',
            });

            const data = await response.json();

            if (data.success) {
                setUser(null);
                alert('로그아웃되었습니다.');
                router.push('/login');
            }
        } catch (error) {
            console.error('Logout failed:', error);
            alert('로그아웃 중 오류가 발생했습니다.');
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gradient-to-br from-cyan-50 via-white to-blue-50 flex items-center justify-center">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-16 w-16 border-t-4 border-b-4 border-cyan-500 mx-auto"></div>
                    <p className="mt-4 text-gray-600">로딩 중...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-cyan-50 via-white to-blue-50">
            {/* Header */}
            <header className="bg-white/80 backdrop-blur-sm border-b border-cyan-100 sticky top-0 z-50 shadow-sm">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center h-16">
                        {/* Logo */}
                        <div className="flex items-center space-x-3">
                            <div className="p-2 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-xl shadow-lg">
                                <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                </svg>
                            </div>
                            <span className="text-xl font-bold bg-gradient-to-r from-cyan-600 to-blue-600 bg-clip-text text-transparent">
                English Learning
              </span>
                        </div>

                        {/* User Menu */}
                        <div className="flex items-center space-x-4">
                            {user ? (
                                <>
                  <span className="text-gray-700 font-medium">
                    안녕하세요, <span className="text-cyan-600">{user.fullName}</span>님
                  </span>
                                    <button
                                        onClick={handleLogout}
                                        className="px-4 py-2 bg-gradient-to-r from-cyan-400 to-blue-500 text-white rounded-lg hover:from-cyan-500 hover:to-blue-600 transition-all shadow-md hover:shadow-lg"
                                    >
                                        로그아웃
                                    </button>
                                </>
                            ) : (
                                <Link
                                    href="/login"
                                    className="px-4 py-2 bg-gradient-to-r from-cyan-400 to-blue-500 text-white rounded-lg hover:from-cyan-500 hover:to-blue-600 transition-all shadow-md hover:shadow-lg"
                                >
                                    로그인
                                </Link>
                            )}
                        </div>
                    </div>
                </div>
            </header>

            {/* Hero Section */}
            <section className="py-20 px-4">
                <div className="max-w-7xl mx-auto text-center">
                    <h1 className="text-5xl md:text-6xl font-bold text-gray-800 mb-6">
                        즐겁게 배우는 <br />
                        <span className="bg-gradient-to-r from-cyan-500 to-blue-600 bg-clip-text text-transparent">
              영어 학습 플랫폼
            </span>
                    </h1>
                    <p className="text-xl text-gray-600 mb-12 max-w-2xl mx-auto">
                        체계적인 레슨, 실전 단어장, 문법 학습까지
                        <br />
                        당신의 영어 실력 향상을 위한 모든 것이 여기에
                    </p>

                    {!user && (
                        <div className="flex justify-center space-x-4">
                            <Link
                                href="/registor"
                                className="px-8 py-4 bg-gradient-to-r from-cyan-400 to-blue-500 text-white font-semibold rounded-xl hover:from-cyan-500 hover:to-blue-600 transition-all shadow-lg hover:shadow-xl"
                            >
                                시작하기
                            </Link>
                            <Link
                                href="/login"
                                className="px-8 py-4 bg-white text-cyan-600 font-semibold rounded-xl hover:bg-gray-50 transition-all shadow-lg hover:shadow-xl border-2 border-cyan-200"
                            >
                                로그인
                            </Link>
                        </div>
                    )}
                </div>
            </section>

            {/* Features Section */}
            <section className="py-16 px-4">
                <div className="max-w-7xl mx-auto">
                    <h2 className="text-3xl font-bold text-center text-gray-800 mb-12">
                        주요 학습 기능
                    </h2>

                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                        {/* Lessons */}
                        <Link href={user ? "/lessons" : "/login"}>
                            <div className="bg-white rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 border border-cyan-100 hover:border-cyan-300 cursor-pointer group">
                                <div className="w-14 h-14 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform shadow-lg">
                                    <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                    </svg>
                                </div>
                                <h3 className="text-xl font-bold text-gray-800 mb-2">레슨 학습</h3>
                                <p className="text-gray-600 text-sm">
                                    초급부터 고급까지 단계별 영어 학습 과정
                                </p>
                            </div>
                        </Link>

                        {/* Vocabulary */}
                        <Link href={user ? "/vocabulary" : "/login"}>
                            <div className="bg-white rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 border border-cyan-100 hover:border-cyan-300 cursor-pointer group">
                                <div className="w-14 h-14 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform shadow-lg">
                                    <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                    </svg>
                                </div>
                                <h3 className="text-xl font-bold text-gray-800 mb-2">단어장</h3>
                                <p className="text-gray-600 text-sm">
                                    플래시카드로 효과적인 단어 암기
                                </p>
                            </div>
                        </Link>

                        {/* Grammar */}
                        <Link href={user ? "/grammar" : "/login"}>
                            <div className="bg-white rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 border border-cyan-100 hover:border-cyan-300 cursor-pointer group">
                                <div className="w-14 h-14 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform shadow-lg">
                                    <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                    </svg>
                                </div>
                                <h3 className="text-xl font-bold text-gray-800 mb-2">문법 학습</h3>
                                <p className="text-gray-600 text-sm">
                                    체계적인 영문법 설명과 예문
                                </p>
                            </div>
                        </Link>

                        {/* Practice */}
                        <Link href={user ? "/practice" : "/login"}>
                            <div className="bg-white rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 border border-cyan-100 hover:border-cyan-300 cursor-pointer group">
                                <div className="w-14 h-14 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform shadow-lg">
                                    <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
                                    </svg>
                                </div>
                                <h3 className="text-xl font-bold text-gray-800 mb-2">퀴즈 & 테스트</h3>
                                <p className="text-gray-600 text-sm">
                                    실력을 확인하는 다양한 테스트
                                </p>
                            </div>
                        </Link>
                    </div>
                </div>
            </section>

            {/* Stats Section */}
            {user && (
                <section className="py-16 px-4 bg-white/50">
                    <div className="max-w-7xl mx-auto">
                        <h2 className="text-3xl font-bold text-center text-gray-800 mb-12">
                            나의 학습 현황
                        </h2>

                        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                            <div className="bg-gradient-to-br from-cyan-400 to-blue-500 rounded-2xl p-6 text-white shadow-xl">
                                <div className="text-4xl font-bold mb-2">12</div>
                                <div className="text-cyan-50">완료한 레슨</div>
                            </div>

                            <div className="bg-gradient-to-br from-blue-400 to-indigo-500 rounded-2xl p-6 text-white shadow-xl">
                                <div className="text-4xl font-bold mb-2">248</div>
                                <div className="text-blue-50">학습한 단어</div>
                            </div>

                            <div className="bg-gradient-to-br from-cyan-500 to-teal-500 rounded-2xl p-6 text-white shadow-xl">
                                <div className="text-4xl font-bold mb-2">85%</div>
                                <div className="text-cyan-50">평균 점수</div>
                            </div>

                            <div className="bg-gradient-to-br from-blue-500 to-cyan-500 rounded-2xl p-6 text-white shadow-xl">
                                <div className="text-4xl font-bold mb-2">15</div>
                                <div className="text-blue-50">연속 학습일</div>
                            </div>
                        </div>
                    </div>
                </section>
            )}

            {/* CTA Section */}
            {!user && (
                <section className="py-20 px-4">
                    <div className="max-w-4xl mx-auto bg-gradient-to-r from-cyan-400 to-blue-500 rounded-3xl p-12 text-center shadow-2xl">
                        <h2 className="text-4xl font-bold text-white mb-6">
                            지금 바로 시작하세요
                        </h2>
                        <p className="text-xl text-cyan-50 mb-8">
                            무료로 가입하고 영어 학습의 새로운 경험을 만나보세요
                        </p>
                        <Link
                            href="/registor"
                            className="inline-block px-8 py-4 bg-white text-cyan-600 font-semibold rounded-xl hover:bg-gray-50 transition-all shadow-lg hover:shadow-xl"
                        >
                            무료로 시작하기
                        </Link>
                    </div>
                </section>
            )}

            {/* Footer */}
            <footer className="bg-white border-t border-cyan-100 py-8">
                <div className="max-w-7xl mx-auto px-4 text-center text-gray-600">
                    <p>© 2024 English Learning Platform. All rights reserved.</p>
                    <div className="mt-4 flex justify-center space-x-6">
                        <a href="#" className="text-cyan-600 hover:text-cyan-700 transition-colors">이용약관</a>
                        <a href="#" className="text-cyan-600 hover:text-cyan-700 transition-colors">개인정보처리방침</a>
                        <a href="#" className="text-cyan-600 hover:text-cyan-700 transition-colors">고객센터</a>
                    </div>
                </div>
            </footer>
        </div>
    );
}