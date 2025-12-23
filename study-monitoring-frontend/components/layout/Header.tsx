'use client';

import Link from 'next/link';
import { useState, useEffect } from 'react';
import { Activity, Menu, X } from 'lucide-react';

export default function Header() {
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [currentTime, setCurrentTime] = useState<Date | null>(null);

    // 현재 시간 업데이트
    useEffect(() => {
        setCurrentTime(new Date());

        const timer = setInterval(() => {
            setCurrentTime(new Date());
        }, 1000);

        return () => clearInterval(timer);
    }, []);

    return (
        <header className="bg-white shadow-sky border-b border-primary-100 sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between items-center h-16">
                    {/* 로고 & 타이틀 */}
                    <div className="flex items-center space-x-3">
                        <Link href="/" className="flex items-center space-x-3 hover:opacity-80 transition">
                            <div className="bg-gradient-to-br from-primary-400 to-primary-600 p-2 rounded-lg">
                                <Activity className="w-6 h-6 text-white" />
                            </div>
                            <div>
                                <h1 className="text-xl font-bold text-primary-700">
                                    Study Monitoring
                                </h1>
                                <p className="text-xs text-secondary-500">실시간 시스템 모니터링</p>
                            </div>
                        </Link>
                    </div>

                    {/* 데스크톱 네비게이션 */}
                    <nav className="hidden md:flex items-center space-x-1">
                        <NavLink href="/dashboard">대시보드</NavLink>
                        <NavLink href="/metrics">메트릭</NavLink>
                        <NavLink href="/statistics">통계</NavLink>
                        <NavLink href="/logs">로그</NavLink>
                        <NavLink href="/health">헬스체크</NavLink>
                    </nav>

                    {/* 현재 시간 & 모바일 메뉴 버튼 */}
                    <div className="flex items-center space-x-4">
                        <div className="hidden sm:block text-sm text-secondary-600">
                            {currentTime ? currentTime.toLocaleString('ko-KR', {
                                year: 'numeric',
                                month: '2-digit',
                                day: '2-digit',
                                hour: '2-digit',
                                minute: '2-digit',
                                second: '2-digit',
                            }) : (
                                // 로딩 중일 때 빈 공간 유지 (Layout Shift 방지용)
                                <span>&nbsp;</span>
                            )}
                        </div>

                        {/* 모바일 메뉴 버튼 */}
                        <button
                            onClick={() => setIsMenuOpen(!isMenuOpen)}
                            className="md:hidden p-2 rounded-lg hover:bg-primary-50 transition"
                        >
                            {isMenuOpen ? (
                                <X className="w-6 h-6 text-primary-600" />
                            ) : (
                                <Menu className="w-6 h-6 text-primary-600" />
                            )}
                        </button>
                    </div>
                </div>

                {/* 모바일 네비게이션 */}
                {isMenuOpen && (
                    <div className="md:hidden pb-4 fade-in">
                        <nav className="flex flex-col space-y-2">
                            <MobileNavLink href="/dashboard" onClick={() => setIsMenuOpen(false)}>
                                대시보드
                            </MobileNavLink>
                            <MobileNavLink href="/metrics" onClick={() => setIsMenuOpen(false)}>
                                메트릭
                            </MobileNavLink>
                            <MobileNavLink href="/statistics" onClick={() => setIsMenuOpen(false)}>
                                통계
                            </MobileNavLink>
                            <MobileNavLink href="/logs" onClick={() => setIsMenuOpen(false)}>
                                로그
                            </MobileNavLink>
                            <MobileNavLink href="/health" onClick={() => setIsMenuOpen(false)}>
                                헬스체크
                            </MobileNavLink>
                        </nav>
                    </div>
                )}
            </div>
        </header>
    );
}

// 데스크톱 네비게이션 링크
function NavLink({ href, children }: { href: string; children: React.ReactNode }) {
    return (
        <Link
            href={href}
            className="px-4 py-2 rounded-lg text-sm font-medium text-secondary-700 hover:bg-primary-50 hover:text-primary-700 transition-colors"
        >
            {children}
        </Link>
    );
}

// 모바일 네비게이션 링크
function MobileNavLink({
   href,
   children,
   onClick,
}: {
    href: string;
    children: React.ReactNode;
    onClick: () => void;
}) {
    return (
        <Link
            href={href}
            onClick={onClick}
            className="px-4 py-3 rounded-lg text-base font-medium text-secondary-700 hover:bg-primary-50 hover:text-primary-700 transition-colors"
        >
            {children}
        </Link>
    );
}