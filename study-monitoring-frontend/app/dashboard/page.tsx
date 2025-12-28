'use client';

import { useEffect, useState, useCallback } from 'react';
import {
    Activity, TrendingUp, AlertTriangle, Zap, Server, Clock,
    ChevronLeft, ChevronRight, FileText, Database
} from 'lucide-react';
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
    // recentErrors는 이제 별도 State로 관리하므로 사용하지 않지만 타입 호환성을 위해 남겨둡니다.
    recentErrors: Array<any>;
    logCounts: { [key: string]: number };
    statistics: {
        totalRequest: number;
        avgResponseTime: number;
        uptime: string;
    };
}

// 에러 아이템 타입 정의
interface ErrorItem {
    id: string;
    timestamp: string;
    logLevel: string;
    message: string;
    application: string;
}

// 차트 데이터 타입 정의
interface ChartPoint {
    timeStr: string;
    tps: number;
}

// 상수 정의
const MAX_DATA_POINTS = 30;
const ERROR_PAGE_SIZE = 5;

// 프로세스별 단위/라벨 결정 헬퍼 함수
const getProcessUnitInfo = (processName: string) => {
    const name = processName.toLowerCase();
    if (name.includes('postgres')) {
        return { cpuLabel: 'Active Conn', cpuUnit: '개', memLabel: 'Disk Usage', memUnit: 'MB' };
    } else if (name.includes('elasticsearch')) {
        return { cpuLabel: 'Active Ops', cpuUnit: '개', memLabel: 'Data Size', memUnit: 'MB' };
    } else {
        return { cpuLabel: 'CPU', cpuUnit: '%', memLabel: 'Memory', memUnit: '%' };
    }
};

export default function DashboardPage() {
    // --- [State 관리] ---
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
    const [chartData, setChartData] = useState<ChartPoint[]>([]);

    // 에러 로그 & 탭 관련 State
    const [activeTab, setActiveTab] = useState<'APP' | 'SYSTEM'>('APP'); // 현재 탭
    const [errorList, setErrorList] = useState<ErrorItem[]>([]);         // 에러 목록 데이터
    const [errorPage, setErrorPage] = useState(1);                       // 현재 페이지
    const [totalErrorPages, setTotalErrorPages] = useState(1);           // 전체 페이지 수
    const [isErrorLoading, setIsErrorLoading] = useState(false);         // 에러 로딩 상태

    // --- [API 호출 함수들] ---

    // 1. 초기 차트 데이터(과거 기록) 로드
    const loadInitialHistory = async () => {
        try {
            const response = await fetch('/api/dashboard/metrics?application=eng-study&metric=tps&hours=1');
            const result = await response.json();

            if (result.success && result.data.values) {
                const history = result.data.values.map((item: any) => ({
                    timeStr: new Date(item.timestamp).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
                    tps: parseFloat(item.value)
                }));
                setChartData(history.slice(-MAX_DATA_POINTS));
            }
        } catch (e) {
            console.error("초기 차트 로딩 실패 (무시 가능)", e);
        }
    };

    // 2. 에러 로그 조회 (페이징 & 탭 지원)
    // 인자로 page와 type을 받아야 상태 꼬임 없이 정확히 조회 가능
    const fetchErrorLogs = async (page: number, type: string) => {
        try {
            setIsErrorLoading(true);
            const response = await fetch(`/api/dashboard/errors?type=${type}&page=${page}&size=${ERROR_PAGE_SIZE}`);
            const result = await response.json();

            if (result.success) {
                setErrorList(result.data.content);
                setTotalErrorPages(result.data.totalPages);
                setErrorPage(result.data.currentPage);
            }
        } catch (e) {
            console.error("에러 로그 조회 실패", e);
        } finally {
            setIsErrorLoading(false);
        }
    };

    // 3. 메인 대시보드 데이터 조회 (메트릭, 프로세스 등)
    const fetchDashboard = useCallback(async () => {
        try {
            const response = await fetch('/api/dashboard/overview');
            const result = await response.json();

            if (result.success) {
                const newData = result.data;
                setDashboardData(newData);

                // 차트 실시간 업데이트 로직
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

    // --- [Effect Hooks] ---

    useEffect(() => {
        // 1. 초기 로드
        loadInitialHistory();
        fetchDashboard();
        fetchErrorLogs(1, 'APP'); // 초기에는 APP 탭, 1페이지 로드

        // 2. 5초마다 대시보드 메트릭 갱신 (에러 로그는 자동 갱신 안 함)
        const interval = setInterval(fetchDashboard, 5000);
        return () => clearInterval(interval);
    }, []);

    // --- [Event Handlers] ---

    // 탭 변경 핸들러
    const handleTabChange = (tab: 'APP' | 'SYSTEM') => {
        if (tab === activeTab) return; // 이미 선택된 탭이면 무시

        setActiveTab(tab);
        setErrorPage(1);        // 페이지 1로 초기화
        fetchErrorLogs(1, tab); // 해당 탭 데이터 로드
    };

    // 페이지 변경 핸들러
    const handlePageChange = (newPage: number) => {
        if (newPage >= 1 && newPage <= totalErrorPages) {
            fetchErrorLogs(newPage, activeTab); // 현재 탭 유지하면서 페이지 이동
        }
    };

    // --- [Rendering Helpers] ---

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

    // 프로세스 데이터 가공
    const processData = dashboardData.processes.map(p => {
        const unitInfo = getProcessUnitInfo(p.processName);
        return {
            name: p.processName,
            status: p.status.toLowerCase() as 'running' | 'stopped' | 'warning',
            uptime: p.uptime,
            pid: p.processId,
            cpu: p.cpuUsage,
            memory: p.memoryUsage,
            cpuLabel: unitInfo.cpuLabel,
            cpuUnit: unitInfo.cpuUnit,
            memLabel: unitInfo.memLabel,
            memUnit: unitInfo.memUnit
        };
    });

    // 에러 데이터 가공 (API에서 받아온 errorList 사용)
    const displayErrorData = errorList.map(e => ({
        id: e.id,
        timestamp: e.timestamp,
        level: e.logLevel ? e.logLevel.toLowerCase() as 'critical' | 'error' | 'warning' | 'info' : 'error',
        message: e.message,
        source: e.application
    }));

    // --- [Main JSX] ---
    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

            {/* 헤더 */}
            <div className="flex justify-between items-end mb-8">
                <div>
                    <div className="flex items-center space-x-3 mb-2">
                        <h1 className="text-3xl font-bold text-primary-700">시스템 대시보드</h1>
                        <span className="flex items-center space-x-1 px-2 py-1 bg-green-100 text-green-700 text-xs font-bold rounded-full border border-green-200 animate-pulse">
                            <span className="relative flex h-2 w-2">
                              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                              <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                            </span>
                            <span>LIVE</span>
                        </span>
                    </div>
                    <p className="text-secondary-600">실시간 인프라 및 애플리케이션 상태 감시</p>
                </div>
                <div className="text-sm text-gray-500 flex items-center">
                    <Clock className="w-4 h-4 mr-1" />
                    Last update: {chartData.length > 0 ? chartData[chartData.length - 1].timeStr : '-'}
                </div>
            </div>

            {/* 메트릭 카드 */}
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

            {/* 중간 섹션: 프로세스 목록 & 에러 로그(탭+페이징) */}
            <div className="grid lg:grid-cols-2 gap-6 mb-8">

                {/* 왼쪽: 프로세스 카드 */}
                <ProcessCard processes={processData} />

                {/* 오른쪽: 에러 리스트 (탭 & 페이징 적용) */}
                <div className="flex flex-col h-full bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">

                    {/* 1. 탭 버튼 영역 */}
                    <div className="flex border-b border-gray-100">
                        <button
                            onClick={() => handleTabChange('APP')}
                            className={`flex-1 py-3 text-sm font-medium transition-colors relative flex items-center justify-center space-x-2 ${
                                activeTab === 'APP'
                                    ? 'text-primary-700 bg-blue-50/50'
                                    : 'text-gray-500 hover:bg-gray-50 hover:text-gray-700'
                            }`}
                        >
                            <FileText className="w-4 h-4" />
                            <span>Application Error</span>
                            {activeTab === 'APP' && (
                                <div className="absolute bottom-0 left-0 w-full h-0.5 bg-primary-600"></div>
                            )}
                        </button>
                        <div className="w-[1px] bg-gray-100"></div>
                        <button
                            onClick={() => handleTabChange('SYSTEM')}
                            className={`flex-1 py-3 text-sm font-medium transition-colors relative flex items-center justify-center space-x-2 ${
                                activeTab === 'SYSTEM'
                                    ? 'text-red-700 bg-red-50/50'
                                    : 'text-gray-500 hover:bg-gray-50 hover:text-gray-700'
                            }`}
                        >
                            <Database className="w-4 h-4" />
                            <span>System/DB Error</span>
                            {activeTab === 'SYSTEM' && (
                                <div className="absolute bottom-0 left-0 w-full h-0.5 bg-red-600"></div>
                            )}
                        </button>
                    </div>

                    {/* 2. 리스트 본문 영역 */}
                    <div className="flex-grow p-4 min-h-[350px]">
                        <div className={`h-full transition-opacity duration-200 ${isErrorLoading ? "opacity-50 pointer-events-none" : "opacity-100"}`}>
                            {displayErrorData.length === 0 ? (
                                <div className="h-full flex flex-col items-center justify-center text-gray-400">
                                    <AlertTriangle className="w-10 h-10 mb-2 opacity-20" />
                                    <p className="text-sm font-medium">발견된 에러 로그가 없습니다.</p>
                                    <p className="text-xs mt-1">시스템이 안정적입니다.</p>
                                </div>
                            ) : (
                                <ErrorList errors={displayErrorData} />
                            )}
                        </div>
                    </div>

                    {/* 3. 페이징 컨트롤러 영역 */}
                    <div className="bg-gray-50 border-t border-gray-100 p-2 flex items-center justify-between">
                        <button
                            onClick={() => handlePageChange(errorPage - 1)}
                            disabled={errorPage === 1 || isErrorLoading}
                            className="p-1.5 rounded-md hover:bg-white hover:shadow-sm disabled:opacity-30 disabled:cursor-not-allowed text-gray-600 transition-all"
                        >
                            <ChevronLeft className="w-5 h-5" />
                        </button>

                        <span className="text-xs text-gray-500 font-medium font-mono">
                            Page <span className="text-gray-900 font-bold">{errorPage}</span> / {totalErrorPages}
                        </span>

                        <button
                            onClick={() => handlePageChange(errorPage + 1)}
                            disabled={errorPage === totalErrorPages || isErrorLoading}
                            className="p-1.5 rounded-md hover:bg-white hover:shadow-sm disabled:opacity-30 disabled:cursor-not-allowed text-gray-600 transition-all"
                        >
                            <ChevronRight className="w-5 h-5" />
                        </button>
                    </div>
                </div>
            </div>

            {/* 하단: 차트 & 로그 통계 */}
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

            {/* 최하단: 인프라 상태 요약 */}
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

// --- [Sub Components] ---
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