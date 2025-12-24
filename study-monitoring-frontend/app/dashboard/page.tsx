'use client';

import { useEffect, useState, useCallback, useRef } from 'react';
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

// 차트 데이터 포인트 타입
interface ChartPoint {
    timeStr: string; // X축 표시용 (HH:mm:ss)
    tps: number;
}

const MAX_DATA_POINTS = 30; // 차트에 유지할 최대 데이터 개수 (30개)

export default function DashboardPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // 현재 상태 데이터 (카드, 리스트용)
    const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);

    // 실시간 차트 데이터 (누적용)
    const [chartData, setChartData] = useState<ChartPoint[]>([]);

    // 1. 초기 데이터 및 주기적 데이터 로드
    const fetchDashboard = useCallback(async () => {
        try {
            const response = await fetch('/api/dashboard/overview');
            const result = await response.json();

            if (result.success) {
                const newData = result.data;
                setDashboardData(newData);

                // [핵심] 실시간 차트 데이터 구성 (Sliding Window)
                const now = new Date();
                const timeStr = now.toLocaleTimeString('ko-KR', {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                });

                setChartData(prev => {
                    // 기존 데이터에 새 포인트 추가
                    const newPoint = {
                        timeStr: timeStr,
                        tps: newData.metrics.engStudy.tps ?? 0
                    };
                    const newHistory = [...prev, newPoint];

                    // 최대 개수를 넘으면 가장 오래된 데이터 제거 (왼쪽 삭제)
                    return newHistory.slice(-MAX_DATA_POINTS);
                });
            }
        } catch (err: any) {
            console.error('Fetch error:', err);
            // 에러가 나도 기존 데이터는 유지 (화면 깜빡임 방지)
            if (!dashboardData) setError(err.message);
        } finally {
            setLoading(false);
        }
    }, [dashboardData]); // dashboardData 의존성 주의 (여기선 stale closure 방지를 위해 함수형 업데이트 사용했으므로 빈 배열 가능하지만, 안전하게)

    // 2. 주기 설정 (5초마다 갱신 - 실시간 느낌을 위해 주기를 짧게 설정)
    useEffect(() => {
        // 최초 로딩
        fetchDashboard();

        const interval = setInterval(() => {
            fetchDashboard();
        }, 5000); // 5초 단위 갱신

        return () => clearInterval(interval);
    }, []); // 의존성 배열 비움 (fetchDashboard 내부에서 함수형 업데이트 사용)


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

    // 프로세스 데이터 매핑
    const processData = dashboardData.processes.map(p => ({
        name: p.processName,
        status: p.status.toLowerCase() as 'running' | 'stopped' | 'warning',
        uptime: p.uptime,
        cpu: p.cpuUsage,
        memory: p.memoryUsage,
        pid: p.processId
    }));

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

            {/* 헤더: 실시간 표시등 추가 */}
            <div className="flex justify-between items-end mb-8">
                <div>
                    <div className="flex items-center space-x-3 mb-2">
                        <h1 className="text-3xl font-bold text-primary-700">
                            시스템 대시보드
                        </h1>
                        {/* Live Indicator */}
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
                    마지막 갱신: {chartData.length > 0 ? chartData[chartData.length - 1].timeStr : '-'}
                </div>
            </div>

            {/* 메트릭 요약 카드 */}
            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <MetricCard
                    title="Eng-Study TPS"
                    value={(dashboardData.metrics.engStudy.tps ?? 0).toFixed(2)}
                    unit="req/s"
                    icon={<Zap className="w-6 h-6" />}
                    color="blue"
                    // 이전 값과 비교하여 증감 표시 로직 등을 추가할 수 있음
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
                <ProcessCard processes={processData} />
                <ErrorList errors={errorData} maxItems={5} />
            </div>

            {/* 실시간 차트 & 로그 분포 */}
            <div className="grid lg:grid-cols-3 gap-6">
                {/* 실시간 트래픽 차트 */}
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
                                        <XAxis
                                            dataKey="timeStr"
                                            style={{ fontSize: '11px', fill: '#6b7280' }}
                                            tickMargin={10}
                                        />
                                        <YAxis
                                            style={{ fontSize: '11px', fill: '#6b7280' }}
                                            domain={[0, 'auto']} // Y축 자동 스케일링
                                        />
                                        <Tooltip
                                            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                            formatter={(value: number) => [value.toFixed(2), 'TPS']}
                                            labelStyle={{ color: '#6b7280', marginBottom: '0.25rem' }}
                                        />
                                        <Area
                                            type="monotone"
                                            dataKey="tps"
                                            stroke="#3b82f6"
                                            strokeWidth={2}
                                            fillOpacity={1}
                                            fill="url(#colorTps)"
                                            isAnimationActive={true} // 애니메이션 활성화
                                            animationDuration={1000} // 부드러운 연결을 위한 시간
                                        />
                                    </AreaChart>
                                </ResponsiveContainer>
                            ) : (
                                <div className="h-full flex items-center justify-center text-gray-400">
                                    데이터 수집 중...
                                </div>
                            )}
                        </div>
                    </Card>
                </div>

                {/* 로그 레벨 분포 */}
                <Card title="로그 레벨 현황">
                    <div className="space-y-4">
                        {dashboardData.logCounts && Object.keys(dashboardData.logCounts).length > 0 ? (
                            Object.entries(dashboardData.logCounts).map(([level, count]) => (
                                <div key={level} className="flex items-center justify-between p-2 hover:bg-gray-50 rounded transition-colors">
                                    <div className="flex items-center space-x-3">
                                        <div className={`w-3 h-3 rounded-full ${
                                            level === 'ERROR' ? 'bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.6)]' : // 에러는 글로우 효과
                                                level === 'WARN' ? 'bg-yellow-500' :
                                                    level === 'INFO' ? 'bg-blue-500' :
                                                        'bg-gray-500'
                                        }`} />
                                        <span className="text-sm font-medium text-gray-700">{level}</span>
                                    </div>
                                    <span className="text-lg font-bold text-primary-700 font-mono">
                                        {count.toLocaleString()}
                                    </span>
                                </div>
                            ))
                        ) : (
                            <div className="py-8 text-center text-gray-400 text-sm">
                                로그 데이터 없음
                            </div>
                        )}
                    </div>

                    <div className="mt-6 pt-4 border-t border-gray-200">
                        <div className="flex justify-between items-center">
                            <span className="text-sm text-secondary-600">누적 로그</span>
                            <span className="text-xl font-bold text-primary-700">
                                {Object.values(dashboardData.logCounts || {})
                                    .reduce((sum, count) => sum + count, 0)
                                    .toLocaleString()}
                            </span>
                        </div>
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

// Helper Components
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