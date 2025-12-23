import Link from 'next/link';
import { Activity, BarChart3, FileText, Heart } from 'lucide-react';

export default function HomePage() {
    return (
        <div className="min-h-[calc(100vh-8rem)] bg-gradient-sky">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
                {/* Hero Section */}
                <div className="text-center mb-16 fade-in">
                    <h1 className="text-5xl font-bold text-primary-700 mb-4">
                        Study Monitoring System
                    </h1>
                    <p className="text-xl text-secondary-600 mb-8">
                        실시간으로 시스템 상태를 모니터링하고 분석합니다
                    </p>
                    <Link
                        href="/dashboard"
                        className="inline-flex items-center px-8 py-4 bg-primary-500 hover:bg-primary-600 text-white text-lg font-semibold rounded-lg shadow-sky transition-colors"
                    >
                        <Activity className="w-6 h-6 mr-2" />
                        대시보드 시작하기
                    </Link>
                </div>

                {/* Features Grid */}
                <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <FeatureCard
                        icon={<Activity className="w-8 h-8" />}
                        title="실시간 메트릭"
                        description="TPS, Heap 사용률, CPU 등 핵심 메트릭을 실시간으로 모니터링"
                        href="/metrics"
                    />

                    <FeatureCard
                        icon={<BarChart3 className="w-8 h-8" />}
                        title="통계 분석"
                        description="시계열 데이터 분석 및 7가지 로그 통계 제공"
                        href="/statistics"
                    />

                    <FeatureCard
                        icon={<FileText className="w-8 h-8" />}
                        title="로그 검색"
                        description="Elasticsearch 기반 강력한 로그 검색 기능"
                        href="/logs"
                    />

                    <FeatureCard
                        icon={<Heart className="w-8 h-8" />}
                        title="헬스체크"
                        description="시스템 구성 요소의 상태를 확인"
                        href="/health"
                    />
                </div>

                {/* System Status */}
                <div className="mt-16 card text-center">
                    <h2 className="text-2xl font-bold text-primary-700 mb-4">
                        시스템 구성
                    </h2>
                    <div className="flex flex-wrap justify-center gap-4 text-secondary-600">
                        <div className="px-4 py-2 bg-primary-50 rounded-lg">
                            <span className="font-semibold">Backend:</span> Spring Boot 3.5.7
                        </div>
                        <div className="px-4 py-2 bg-primary-50 rounded-lg">
                            <span className="font-semibold">Frontend:</span> Next.js 15
                        </div>
                        <div className="px-4 py-2 bg-primary-50 rounded-lg">
                            <span className="font-semibold">Database:</span> PostgreSQL
                        </div>
                        <div className="px-4 py-2 bg-primary-50 rounded-lg">
                            <span className="font-semibold">Search:</span> Elasticsearch
                        </div>
                        <div className="px-4 py-2 bg-primary-50 rounded-lg">
                            <span className="font-semibold">Metrics:</span> Prometheus
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

function FeatureCard({
                         icon,
                         title,
                         description,
                         href,
                     }: {
    icon: React.ReactNode;
    title: string;
    description: string;
    href: string;
}) {
    return (
        <Link
            href={href}
            className="card hover:shadow-xl hover:scale-105 transition-all duration-300 cursor-pointer group"
        >
            <div className="text-primary-500 group-hover:text-primary-600 mb-4 transition-colors">
                {icon}
            </div>
            <h3 className="text-lg font-semibold text-primary-700 mb-2">{title}</h3>
            <p className="text-sm text-secondary-600">{description}</p>
        </Link>
    );
}