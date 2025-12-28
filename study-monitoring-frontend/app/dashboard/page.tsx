'use client';

import { useEffect, useState, useCallback } from 'react';
import { Activity, TrendingUp, AlertTriangle, Zap, Server, Clock } from 'lucide-react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import ProcessCard from '@/components/dashboard/ProcessCard';
import ErrorList from '@/components/dashboard/ErrorList';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

// --- [Type Definitions] ---
interface DashboardData {
    processes: Array<{
        processId: number;
        processName: string;
        processType: string;
        status: string;
        cpuUsage: number;
        memoryUsage: number;
        uptime: string;
        lastHealthCheck: string;
    }>;
    metrics: {
        engStudy: {
            tps: number | null;
            heapUsage: number | null;
            errorRate: number | null;
            responseTime: number | null;
        };
        monitoring: {
            tps: number | null;
            heapUsage: number | null;
            errorRate: number | null;
            responseTime: number | null;
        };
    };
    recentErrors: Array<{
        id: string;
        timestamp: string;
        logLevel: string;
        message: string;
        application: string;
    }>;
    logCounts: { [key: string]: number };
    statistics: {
        totalRequest: number;
        avgResponseTime: number;
        uptime: string;
    };
}

interface ChartPoint {
    timeStr: string;
    tps: number;
}

const MAX_DATA_POINTS = 30;

// [✨ 핵심 변경 1] 프로세스별 단위/라벨 결정 헬퍼 함수
const getProcessUnitInfo = (processName: string) => {
    const name = processName.toLowerCase();

    if (name.includes('postgres')) {
        return {
            cpuLabel: 'Active Conn', // CPU 대신 활성 연결 수
            cpuUnit: '개',
            memLabel: 'Disk Usage',  // 메모리 대신 디스크 용량
            memUnit: 'MB'
        };
    } else if (name.includes('elasticsearch')) {
        return {
            cpuLabel: 'Active Ops',  // 작업량
            cpuUnit: '개',
            memLabel: 'Data Size',   // 데이터 크기
            memUnit: 'MB'
        };
    } else {
        // 기본 Spring Boot 앱
        return {
            cpuLabel: 'CPU',
            cpuUnit: '%',
            memLabel: 'Memory',
            memUnit: '%'
        };
    }
};

export default function DashboardPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
    const [chartData, setChartData] = useState<ChartPoint[]>([]);

    // [✨ 핵심 변경 2] 초기 차트 데이터(과거 기록) 로드
    const loadInitialHistory = async () => {
        try {
            // 백엔드에 만들어둔 range 쿼리 API 사용 (지난 1시간 데이터)
            const response = await fetch('/api/dashboard/metrics?application=eng-study&metric=tps&hours=1');
            const result = await response.json();

            if (result.success && result.data.values) {
                const history = result.data.values.map((item: any) => ({
                    timeStr: new Date(item.timestamp).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
                    tps: parseFloat(item.value)
                }));
                // 데이터가 너무 많으면 뒤에서부터 자름
                setChartData(history.slice(-MAX_DATA_POINTS));
            }
        } catch (e) {
            console.error("초기 차트 로딩 실패 (무시 가능)", e);
        }
    };

    const fetchDashboard = useCallback(async () => {
        try {
            const response = await fetch('/api/dashboard/overview');
            const result = await response.json();

            if (result.success) {
                const newData = result.data;
                setDashboardData(newData);

                const now = new Date();
                const timeStr = now.toLocaleTimeString('ko-KR', {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                });

                setChartData(prev => {
                    const newPoint = {
                        timeStr: timeStr,
                        tps: newData.metrics.engStudy.tps ?? 0
                    };
                    // 기존 데이터에 이어 붙이기
                    const newHistory = [...prev, newPoint];
                    return newHistory.slice(-MAX_DATA_POINTS);
                });
            }
        } catch (err: any) {
            console.error('Fetch error:', err);
            if (!dashboardData) setError(err.message);
        } finally {
            setLoading(false);
        }
    }, [dashboardData]);

    useEffect(() => {
        // 1. 초기 차트 데이터 로드 (새로고침 시 그래프 복구)
        loadInitialHistory();

        // 2. 실시간 데이터 폴링 시작
        fetchDashboard();
        const interval = setInterval(fetchDashboard, 5000);
        return () => clearInterval(interval);
    }, []);

    if (loading && !dashboardData) {
        return (
            <div className="max-w-7xl mx-auto px-4 py-8">
                <Loading text="실시간 모니터링 연결 중..." />
            </div>
        );
    }

    if (error && !dashboardData) {
        return (
            <div className="max-w-7xl mx-auto px-4 py-8">
                <ErrorMessage message={error} onRetry={() => window.location.reload()} />
            </div>
        );
    }

    if (!dashboardData) return null;

    // [✨ 핵심 변경 3] 프로세스 데이터 매핑 시 단위 정보 포함
    const processData = dashboardData.processes.map(p => {
        const unitInfo = getProcessUnitInfo(p.processName);
        return {
            name: p.processName,
            status: p.status.toLowerCase() as 'running' | 'stopped' | 'warning',
            uptime: p.uptime,
            pid: p.processId,

            // 값
            cpu: p.cpuUsage,
            memory: p.memoryUsage,

            // 단위 및 라벨 정보 추가 (ProcessCard에서 사용해야 함)
            cpuLabel: unitInfo.cpuLabel,
            cpuUnit: unitInfo.cpuUnit,
            memLabel: unitInfo.memLabel,
            memUnit: unitInfo.memUnit
        };
    });

    // 에러 데이터 매핑
    const errorData = dashboardData.recentErrors.map(e => ({
        id: e.id,
        timestamp: e.timestamp,
        level: e.logLevel.toLowerCase() as 'critical' | 'error' | 'warning' | 'info',
        message: e.message,
        source: e.application
    }));

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

            {/* 헤더 */}
            <div className="flex justify-between items-end mb-8">
                <div>
                    <div className="flex items-center space-x-3 mb-2">
                        <h1 className="text-3xl font-bold text-primary-700">
                            시스템 대시보드
                        </h1>
                        <span className="flex items-center space-x-1 px-2 py-1 bg-green-100 text-green-700 text-xs font-bold rounded-full border border-green-200 animate-pulse">
                            <span className="relative flex h-2 w-2">
                              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                              <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                            </span>
                            <span>LIVE</span>
                        </span>
                    </div>
                    <p className="text-secondary-600">
                        실시간 인프라 및 애플리케이션 상태 감시
                    </p>
                </div>
                <div className="text-sm text-gray-500 flex items-center">
                    <Clock className="w-4 h-4 mr-1" />
                    Last update: {chartData.length > 0 ? chartData[chartData.length - 1].timeStr : '-'}
                </div>
            </div>

            {/* 메트릭 요약 카드 (Spring Boot 앱들이라 기존 단위 유지) */}
            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <MetricCard
                    title="Eng-Study TPS"
                    value={(dashboardData.metrics.engStudy.tps ?? 0).toFixed(2)}
                    unit="req/s"
                    icon={<Zap className="w-6 h-6" />}
                    color="blue"
                    trend={(dashboardData.metrics.engStudy.tps || 0) > 5 ? 'Active' : 'Idle'}
                />
                <MetricCard
                    title="Monitoring TPS"
                    value={(dashboardData.metrics.monitoring.tps ?? 0).toFixed(2)}
                    unit="req/s"
                    icon={<Activity className="w-6 h-6" />}
                    color="green"
                    trend="Stable"
                />
                <MetricCard
                    title="평균 응답시간"
                    value={(dashboardData.statistics.avgResponseTime ?? 0).toFixed(0)}
                    unit="ms"
                    icon={<TrendingUp className="w-6 h-6" />}
                    color="purple"
                    trend="Avg"
                />
                <MetricCard
                    title="에러율 (Max)"
                    value={Math.max(
                        dashboardData.metrics.engStudy.errorRate ?? 0,
                        dashboardData.metrics.monitoring.errorRate ?? 0
                    ).toFixed(2)}
                    unit="%"
                    icon={<AlertTriangle className="w-6 h-6" />}
                    color="red"
                    trend="Realtime"
                    warning={(dashboardData.metrics.engStudy.errorRate || 0) > 1}
                />
            </div>

            {/* 프로세스 & 에러 */}
            <div className="grid lg:grid-cols-2 gap-6 mb-8">
                {/* ⚠️ 중요: ProcessCard 컴포넌트 내부도 수정이 필요할 수 있습니다.
                   ProcessCard가 cpuUnit, memUnit props를 받아서 출력하도록 확인해주세요.
                */}
                <ProcessCard processes={processData} />
                <ErrorList errors={errorData} maxItems={5} />
            </div>

            {/* 차트 & 로그 */}
            <div className="grid lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2">
                    <Card title="실시간 트래픽 모니터링 (Eng-Study)">
                        <div className="h-[300px] w-full">
                            {chartData.length > 0 ? (
                                <ResponsiveContainer width="100%" height="100%">
                                    <AreaChart data={chartData}>
                                        <defs>
                                            <linearGradient id="colorTps" x1="0" y1="0" x2="0" y2="1">
                                                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8}/>
                                                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                                            </linearGradient>
                                        </defs>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />
                                        <XAxis dataKey="timeStr" style={{ fontSize: '11px', fill: '#6b7280' }} tickMargin={10} />
                                        <YAxis style={{ fontSize: '11px', fill: '#6b7280' }} domain={[0, 'auto']} />
                                        <Tooltip
                                            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                            formatter={(value: number) => [value.toFixed(2), 'TPS']}
                                            labelStyle={{ color: '#6b7280' }}
                                        />
                                        <Area type="monotone" dataKey="tps" stroke="#3b82f6" strokeWidth={2} fillOpacity={1} fill="url(#colorTps)" animationDuration={500} />
                                    </AreaChart>
                                </ResponsiveContainer>
                            ) : (
                                <div className="h-full flex items-center justify-center text-gray-400">데이터 로딩 중...</div>
                            )}
                        </div>
                    </Card>
                </div>

                {/* 로그 레벨 현황 */}
                <Card title="로그 레벨 현황">
                    <div className="space-y-4">
                        {dashboardData.logCounts && Object.keys(dashboardData.logCounts).length > 0 ? (
                            Object.entries(dashboardData.logCounts).map(([level, count]) => (
                                <div key={level} className="flex items-center justify-between p-2 hover:bg-gray-50 rounded transition-colors">
                                    <div className="flex items-center space-x-3">
                                        <div className={`w-3 h-3 rounded-full ${
                                            level === 'ERROR' ? 'bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.6)]' :
                                                level === 'WARN' ? 'bg-yellow-500' :
                                                    level === 'INFO' ? 'bg-blue-500' : 'bg-gray-500'
                                        }`} />
                                        <span className="text-sm font-medium text-gray-700">{level}</span>
                                    </div>
                                    <span className="text-lg font-bold text-primary-700 font-mono">{count.toLocaleString()}</span>
                                </div>
                            ))
                        ) : (
                            <div className="py-8 text-center text-gray-400 text-sm">로그 데이터 없음</div>
                        )}
                    </div>
                </Card>
            </div>

            {/* 시스템 통계 */}
            <div className="mt-8">
                <Card title="인프라 상태 요약">
                    <div className="grid md:grid-cols-3 gap-6">
                        <div className="text-center p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm text-secondary-600 mb-2">총 처리 요청</p>
                            <p className="text-3xl font-bold text-primary-700 font-mono">
                                {(dashboardData.statistics.totalRequest ?? 0).toLocaleString()}
                            </p>
                        </div>
                        <div className="text-center p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm text-secondary-600 mb-2">평균 응답속도</p>
                            <p className="text-3xl font-bold text-primary-700 font-mono">
                                {(dashboardData.statistics.avgResponseTime ?? 0).toFixed(0)}
                                <span className="text-lg text-secondary-500 ml-1">ms</span>
                            </p>
                        </div>
                        <div className="text-center p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm text-secondary-600 mb-2">가동 시간</p>
                            <div className="flex items-center justify-center space-x-2">
                                <Server className="w-5 h-5 text-green-500" />
                                <p className="text-2xl font-bold text-primary-700 font-mono">
                                    {dashboardData.statistics.uptime || '0s'}
                                </p>
                            </div>
                        </div>
                    </div>
                </Card>
            </div>
        </div>
    );
}

// ... MetricCard 컴포넌트 유지 ...
function MetricCard({ title, value, unit, icon, color, trend, warning = false }: any) {
    const colorClasses: any = {
        blue: 'from-blue-400 to-blue-600 shadow-blue-200',
        green: 'from-green-400 to-green-600 shadow-green-200',
        red: 'from-red-400 to-red-600 shadow-red-200',
        purple: 'from-purple-400 to-purple-600 shadow-purple-200',
    };

    return (
        <Card className={`transition-all duration-300 hover:shadow-lg ${warning ? 'border-2 border-red-300 bg-red-50 animate-pulse' : ''}`}>
            <div className="flex items-start justify-between mb-4">
                <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${colorClasses[color]} flex items-center justify-center text-white shadow-lg transform transition-transform hover:scale-110`}>
                    {icon}
                </div>
                {trend && (
                    <span className="text-xs font-bold px-2 py-1 bg-white/60 rounded-lg text-secondary-600 border border-gray-100 backdrop-blur-sm">
                        {trend}
                    </span>
                )}
            </div>
            <h3 className="text-sm font-medium text-secondary-600 mb-1">{title}</h3>
            <div className="flex items-baseline space-x-1">
                <span className="text-2xl font-bold text-primary-900 tracking-tight font-mono">{value}</span>
                <span className="text-sm text-secondary-500 font-medium">{unit}</span>
            </div>
        </Card>
    );
}