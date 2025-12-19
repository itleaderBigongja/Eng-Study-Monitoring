'use client';
import { useState, useEffect } from 'react';
import { Activity, Server, Database, AlertCircle, CheckCircle, XCircle, TrendingUp, Clock } from 'lucide-react';

// ===================================
// 타입 정의
// ===================================

interface ProcessStatus {
    processId: number;
    processName: string;
    processType: string;
    status: string;
    cpuUsage: number;
    memoryUsage: number;
    uptime: string;
    lastHealthCheck: string;
}

interface ApplicationMetrics {
    tps: number;
    heapUsage: number;
    errorRate: number;
    responseTime: number;
}

interface MetricsSummary {
    engStudy: ApplicationMetrics;
    monitoring: ApplicationMetrics;
}

interface ErrorLog {
    id: string;
    timestamp: string;
    logLevel: string;
    message: string;
    application: string;
}

interface SystemStatistics {
    totalRequests: number;
    avgResponseTime: number;
    uptime: string;
}

interface DashboardData {
    processes: ProcessStatus[];
    metrics: MetricsSummary;
    recentErrors: ErrorLog[];
    logCounts: { [key: string]: number };
    statistics: SystemStatistics;
}

// ===================================
// 메인 컴포넌트
// ===================================

export default function DashboardPage() {
    const [data, setData] = useState<DashboardData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [lastUpdate, setLastUpdate] = useState<Date>(new Date());

    // 데이터 가져오기
    const fetchDashboardData = async () => {
        try {
            const response = await fetch('http://localhost:8081/api/dashboard/overview', {
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error('대시보드 데이터 조회 실패');
            }

            const result = await response.json();

            if (result.success) {
                setData(result.data);
                setLastUpdate(new Date());
                setError(null);
            } else {
                throw new Error(result.message);
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : '알 수 없는 오류');
        } finally {
            setLoading(false);
        }
    };

    // 초기 로드 + 30초마다 자동 갱신
    useEffect(() => {
        fetchDashboardData();
        const interval = setInterval(fetchDashboardData, 30000);
        return () => clearInterval(interval);
    }, []);

    // 로딩 상태
    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-gray-600">대시보드 로딩 중...</p>
                </div>
            </div>
        );
    }

    // 에러 상태
    if (error) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="bg-white p-8 rounded-lg shadow-lg max-w-md">
                    <div className="flex items-center text-red-600 mb-4">
                        <AlertCircle className="w-6 h-6 mr-2" />
                        <h2 className="text-xl font-bold">오류 발생</h2>
                    </div>
                    <p className="text-gray-700 mb-4">{error}</p>
                    <button
                        onClick={fetchDashboardData}
                        className="w-full bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
                    >
                        다시 시도
                    </button>
                </div>
            </div>
        );
    }

    if (!data) return null;

    return (
        <div className="min-h-screen bg-gray-50">
            {/* 헤더 */}
            <header className="bg-white shadow-sm border-b border-gray-200">
                <div className="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center">
                            <Activity className="w-8 h-8 text-blue-600 mr-3" />
                            <div>
                                <h1 className="text-2xl font-bold text-gray-900">모니터링 대시보드</h1>
                                <p className="text-sm text-gray-500">실시간 시스템 현황</p>
                            </div>
                        </div>
                        <div className="flex items-center text-sm text-gray-500">
                            <Clock className="w-4 h-4 mr-2" />
                            최종 업데이트: {lastUpdate.toLocaleTimeString('ko-KR')}
                        </div>
                    </div>
                </div>
            </header>

            <main className="max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8">
                {/* 시스템 통계 카드 */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                    <StatCard
                        title="총 요청 수"
                        value={data.statistics.totalRequests.toLocaleString()}
                        icon={<TrendingUp className="w-6 h-6 text-blue-600" />}
                        bgColor="bg-blue-50"
                    />
                    <StatCard
                        title="평균 응답 시간"
                        value={`${data.statistics.avgResponseTime.toFixed(2)}ms`}
                        icon={<Clock className="w-6 h-6 text-green-600" />}
                        bgColor="bg-green-50"
                    />
                    <StatCard
                        title="시스템 가동 시간"
                        value={data.statistics.uptime}
                        icon={<Server className="w-6 h-6 text-purple-600" />}
                        bgColor="bg-purple-50"
                    />
                </div>

                {/* 프로세스 현황 */}
                <div className="bg-white rounded-lg shadow-sm border border-gray-200 mb-6">
                    <div className="px-6 py-4 border-b border-gray-200">
                        <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                            <Server className="w-5 h-5 mr-2 text-blue-600" />
                            프로세스 현황
                        </h2>
                    </div>
                    <div className="p-6">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {data.processes.map((process) => (
                                <ProcessCard key={process.processId} process={process} />
                            ))}
                        </div>
                    </div>
                </div>

                {/* 애플리케이션 메트릭 */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                    <MetricsCard
                        title="eng-study 메트릭"
                        metrics={data.metrics.engStudy}
                    />
                    <MetricsCard
                        title="monitoring 메트릭"
                        metrics={data.metrics.monitoring}
                    />
                </div>

                {/* 최근 에러 로그 */}
                <div className="bg-white rounded-lg shadow-sm border border-gray-200 mb-6">
                    <div className="px-6 py-4 border-b border-gray-200">
                        <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                            <AlertCircle className="w-5 h-5 mr-2 text-red-600" />
                            최근 에러 로그
                        </h2>
                    </div>
                    <div className="p-6">
                        {data.recentErrors.length === 0 ? (
                            <div className="text-center py-8 text-gray-500">
                                <CheckCircle className="w-12 h-12 mx-auto mb-2 text-green-500" />
                                <p>에러가 없습니다</p>
                            </div>
                        ) : (
                            <div className="space-y-3">
                                {data.recentErrors.map((error) => (
                                    <ErrorCard key={error.id} error={error} />
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* 로그 레벨 통계 */}
                <div className="bg-white rounded-lg shadow-sm border border-gray-200">
                    <div className="px-6 py-4 border-b border-gray-200">
                        <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                            <Database className="w-5 h-5 mr-2 text-indigo-600" />
                            로그 레벨별 통계
                        </h2>
                    </div>
                    <div className="p-6">
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                            {Object.entries(data.logCounts).map(([level, count]) => (
                                <LogLevelCard key={level} level={level} count={count} />
                            ))}
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
}

// ===================================
// 서브 컴포넌트
// ===================================

function StatCard({ title, value, icon, bgColor }: {
    title: string;
    value: string;
    icon: React.ReactNode;
    bgColor: string;
}) {
    return (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-sm text-gray-500 mb-1">{title}</p>
                    <p className="text-2xl font-bold text-gray-900">{value}</p>
                </div>
                <div className={`${bgColor} rounded-full p-3`}>
                    {icon}
                </div>
            </div>
        </div>
    );
}

function ProcessCard({ process }: { process: ProcessStatus }) {
    const statusColor = {
        RUNNING: 'bg-green-100 text-green-800',
        STOPPED: 'bg-gray-100 text-gray-800',
        ERROR: 'bg-red-100 text-red-800',
    }[process.status] || 'bg-gray-100 text-gray-800';

    const StatusIcon = {
        RUNNING: CheckCircle,
        STOPPED: XCircle,
        ERROR: AlertCircle,
    }[process.status] || XCircle;

    return (
        <div className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between mb-3">
                <div className="flex items-center">
                    <Server className="w-5 h-5 text-gray-400 mr-2" />
                    <h3 className="font-semibold text-gray-900">{process.processName}</h3>
                </div>
                <span className={`px-2 py-1 text-xs font-medium rounded-full ${statusColor} flex items-center`}>
          <StatusIcon className="w-3 h-3 mr-1" />
                    {process.status}
        </span>
            </div>
            <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                    <span className="text-gray-500">CPU</span>
                    <span className="font-medium text-gray-900">{process.cpuUsage.toFixed(1)}%</span>
                </div>
                <div className="flex justify-between">
                    <span className="text-gray-500">메모리</span>
                    <span className="font-medium text-gray-900">{process.memoryUsage.toFixed(1)}%</span>
                </div>
                <div className="flex justify-between">
                    <span className="text-gray-500">가동 시간</span>
                    <span className="font-medium text-gray-900">{process.uptime}</span>
                </div>
            </div>
        </div>
    );
}

function MetricsCard({ title, metrics }: {
    title: string;
    metrics: ApplicationMetrics;
}) {
    return (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">{title}</h3>
            <div className="space-y-4">
                <MetricRow label="TPS" value={metrics.tps.toFixed(2)} unit="req/s" />
                <MetricRow label="Heap 사용률" value={metrics.heapUsage.toFixed(1)} unit="%" />
                <MetricRow label="에러율" value={metrics.errorRate.toFixed(2)} unit="%" />
                <MetricRow label="응답 시간" value={metrics.responseTime.toFixed(2)} unit="ms" />
            </div>
        </div>
    );
}

function MetricRow({ label, value, unit }: {
    label: string;
    value: string;
    unit: string;
}) {
    return (
        <div className="flex justify-between items-center">
            <span className="text-gray-600">{label}</span>
            <span className="font-semibold text-gray-900">
        {value} <span className="text-sm text-gray-500">{unit}</span>
      </span>
        </div>
    );
}

function ErrorCard({ error }: { error: ErrorLog }) {
    return (
        <div className="border-l-4 border-red-500 bg-red-50 p-4 rounded">
            <div className="flex items-start">
                <AlertCircle className="w-5 h-5 text-red-600 mr-3 mt-0.5 flex-shrink-0" />
                <div className="flex-1">
                    <div className="flex items-center justify-between mb-1">
                        <span className="text-xs font-medium text-red-800">{error.application}</span>
                        <span className="text-xs text-gray-500">{new Date(error.timestamp).toLocaleString('ko-KR')}</span>
                    </div>
                    <p className="text-sm text-gray-800">{error.message}</p>
                </div>
            </div>
        </div>
    );
}

function LogLevelCard({ level, count }: { level: string; count: number }) {
    const colors = {
        INFO: 'bg-blue-50 text-blue-700 border-blue-200',
        WARN: 'bg-yellow-50 text-yellow-700 border-yellow-200',
        ERROR: 'bg-red-50 text-red-700 border-red-200',
        DEBUG: 'bg-gray-50 text-gray-700 border-gray-200',
    }[level] || 'bg-gray-50 text-gray-700 border-gray-200';

    return (
        <div className={`border rounded-lg p-4 ${colors}`}>
            <p className="text-sm font-medium mb-1">{level}</p>
            <p className="text-2xl font-bold">{count.toLocaleString()}</p>
        </div>
    );
}