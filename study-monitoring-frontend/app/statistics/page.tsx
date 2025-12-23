import Link from 'next/link';
import { BarChart3, TrendingUp, Database, Shield, AlertCircle, FileText, Activity, Lock } from 'lucide-react';

const statisticsPages = [
    {
        title: '시계열 통계',
        description: 'Prometheus + PostgreSQL 기반 시계열 데이터 분석',
        href: '/statistics/timeseries',
        icon: TrendingUp,
        color: 'from-blue-400 to-blue-600',
    },
    {
        title: '로그 통계',
        description: '애플리케이션 로그 레벨별 통계 및 시간대별 분포',
        href: '/statistics/logs',
        icon: FileText,
        color: 'from-green-400 to-green-600',
    },
    {
        title: '접근 로그 통계',
        description: 'HTTP 메서드, 상태코드, 응답시간 분석',
        href: '/statistics/access-logs',
        icon: Activity,
        color: 'from-purple-400 to-purple-600',
    },
    {
        title: '에러 로그 통계',
        description: '에러 타입, 심각도별 통계 및 발생 빈도 분석',
        href: '/statistics/error-logs',
        icon: AlertCircle,
        color: 'from-red-400 to-red-600',
    },
    {
        title: '성능 메트릭 통계',
        description: 'CPU, 메모리, JVM 성능 지표 분석',
        href: '/statistics/performance-metrics',
        icon: BarChart3,
        color: 'from-yellow-400 to-yellow-600',
    },
    {
        title: '데이터베이스 로그 통계',
        description: '쿼리 실행시간, Operation별, 테이블별 통계',
        href: '/statistics/database-logs',
        icon: Database,
        color: 'from-indigo-400 to-indigo-600',
    },
    {
        title: '감사 로그 통계',
        description: '사용자 액션, 이벤트 카테고리, 성공/실패율 분석',
        href: '/statistics/audit-logs',
        icon: Shield,
        color: 'from-teal-400 to-teal-600',
    },
    {
        title: '보안 로그 통계',
        description: '위협 레벨, 공격 타입, 차단 통계 분석',
        href: '/statistics/security-logs',
        icon: Lock,
        color: 'from-pink-400 to-pink-600',
    },
];

export default function StatisticsPage() {
    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    통계 분석
                </h1>
                <p className="text-secondary-600">
                    다양한 로그 및 메트릭 통계를 확인하고 분석합니다
                </p>
            </div>

            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
                {statisticsPages.map((page) => {
                    const Icon = page.icon;
                    return (
                        <Link
                            key={page.href}
                            href={page.href}
                            className="card hover:shadow-xl hover:scale-105 transition-all duration-300 cursor-pointer group"
                        >
                            <div className={`w-14 h-14 rounded-lg bg-gradient-to-br ${page.color} flex items-center justify-center text-white mb-4 group-hover:scale-110 transition-transform`}>
                                <Icon className="w-7 h-7" />
                            </div>
                            <h3 className="text-lg font-semibold text-primary-700 mb-2">
                                {page.title}
                            </h3>
                            <p className="text-sm text-secondary-600">
                                {page.description}
                            </p>
                        </Link>
                    );
                })}
            </div>
        </div>
    );
}