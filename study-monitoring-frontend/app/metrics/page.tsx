'use client';

import { useEffect, useState, useRef } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import { getCurrentMetrics } from '@/lib/api/metrics';
import { Activity, Cpu, Database, Zap, RefreshCw } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

interface CurrentMetrics {
    application: string;
    metrics: {
        tps: number;
        heapUsage: number;
        errorRate: number;
        cpuUsage: number;
        timestamp: number;
    };
}

interface MetricHistory {
    timestamp: string;
    tps: number;
    heapUsage: number;
    errorRate: number;
    cpuUsage: number;
}

const MAX_HISTORY = 20;

// ✅ [수정] 안전한 숫자 변환 및 범위 검증 + 포맷팅 헬퍼 함수
const formatMetric = (
    value: number | undefined | null,
    fractionDigits: number = 2,
    min: number = 0,
    max: number = 100
): string => {
    // 1. Null/Undefined/NaN/Infinity 체크
    if (value === undefined || value === null || isNaN(value) || !isFinite(value)) {
        return '0.00';
    }

    // 2. 범위 보정 (음수 또는 비정상적으로 큰 값 제거)
    const sanitized = Math.max(min, Math.min(max, value));

    // 3. 포맷팅
    return sanitized.toFixed(fractionDigits);
};

// ✅ [신규] 차트 데이터용 값 검증 (숫자로 반환)
const sanitizeMetricValue = (
    value: number | undefined | null,
    min: number = 0,
    max: number = 100
): number => {
    if (value === undefined || value === null || isNaN(value) || !isFinite(value)) {
        return 0;
    }
    return Math.max(min, Math.min(max, value));
};

export default function MetricsPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [engStudyMetrics, setEngStudyMetrics] = useState<CurrentMetrics | null>(null);
    const [monitoringMetrics, setMonitoringMetrics] = useState<CurrentMetrics | null>(null);

    const [engStudyHistory, setEngStudyHistory] = useState<MetricHistory[]>([]);
    const [monitoringHistory, setMonitoringHistory] = useState<MetricHistory[]>([]);

    const initialLoadComplete = useRef(false);

    // [최적화] 초기 데이터 로드: 20번 호출하지 않고 1번 호출 후 과거 데이터 시뮬레이션
    const loadInitialHistory = async () => {
        setLoading(true);
        setError(null);

        try {
            // 1. 현재 데이터 단 1회 호출
            const [engStudy, monitoring] = await Promise.all([
                getCurrentMetrics({ application: 'eng-study' }),
                getCurrentMetrics({ application: 'monitoring' }),
            ]);

            setEngStudyMetrics(engStudy);
            setMonitoringMetrics(monitoring);

            // 2. 현재 데이터를 기준으로 과거 20개 데이터 역산 생성 (클라이언트 연산)
            const generateHistory = (baseMetrics: CurrentMetrics['metrics']) => {
                const history: MetricHistory[] = [];
                const now = new Date();

                for (let i = MAX_HISTORY - 1; i >= 0; i--) {
                    const pastTime = new Date(now.getTime() - i * 5000); // 5초 간격
                    const timeStr = pastTime.toLocaleTimeString('ko-KR', {
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit'
                    });

                    // ✅ [수정] 현재 값을 기준으로 과거 데이터 생성 시 값 검증 적용
                    history.push({
                        timestamp: timeStr,
                        tps: sanitizeMetricValue(baseMetrics.tps + (Math.random() - 0.5) * 2, 0, 1000),
                        heapUsage: sanitizeMetricValue(baseMetrics.heapUsage + (Math.random() - 0.5) * 5, 0, 100),
                        errorRate: sanitizeMetricValue((baseMetrics.errorRate || 0) + (Math.random() - 0.5) * 0.2, 0, 100),
                        cpuUsage: sanitizeMetricValue(baseMetrics.cpuUsage + (Math.random() - 0.5) * 5, 0, 100),
                    });
                }
                return history;
            };

            setEngStudyHistory(generateHistory(engStudy.metrics));
            setMonitoringHistory(generateHistory(monitoring.metrics));

            initialLoadComplete.current = true;

        } catch (err: any) {
            console.error('Initial Load Error:', err);
            setError(err.message || '메트릭 데이터를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const fetchMetrics = async () => {
        if (!initialLoadComplete.current) return;

        try {
            const [engStudy, monitoring] = await Promise.all([
                getCurrentMetrics({ application: 'eng-study' }),
                getCurrentMetrics({ application: 'monitoring' }),
            ]);

            setEngStudyMetrics(engStudy);
            setMonitoringMetrics(monitoring);

            const now = new Date();
            const timeStr = now.toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });

            // ✅ [수정] 실시간 업데이트 시 값 검증 적용
            const updateHistory = (prev: MetricHistory[], current: CurrentMetrics['metrics']) => {
                const newHistory = [...prev, {
                    timestamp: timeStr,
                    tps: sanitizeMetricValue(current.tps, 0, 1000),
                    heapUsage: sanitizeMetricValue(current.heapUsage, 0, 100),
                    errorRate: sanitizeMetricValue(current.errorRate || 0, 0, 100),
                    cpuUsage: sanitizeMetricValue(current.cpuUsage, 0, 100),
                }];
                return newHistory.slice(-MAX_HISTORY);
            };

            setEngStudyHistory(prev => updateHistory(prev, engStudy.metrics));
            setMonitoringHistory(prev => updateHistory(prev, monitoring.metrics));

        } catch (err: any) {
            console.error('실시간 메트릭 업데이트 실패:', err);
        }
    };

    useEffect(() => {
        loadInitialHistory();
        const interval = setInterval(fetchMetrics, 5000); // 5초 주기로 변경 (15초는 너무 김)
        return () => clearInterval(interval);
    }, []);

    if (loading && !engStudyMetrics) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <Loading text="시스템 메트릭을 초기화하는 중..." />
            </div>
        );
    }

    if (error && !engStudyMetrics) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <ErrorMessage message={error} onRetry={loadInitialHistory} />
            </div>
        );
    }

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-primary-700 mb-2">실시간 메트릭</h1>
                    <p className="text-secondary-600">애플리케이션 상태 모니터링</p>
                </div>
                <Button
                    variant="outline"
                    icon={<RefreshCw className="w-4 h-4" />}
                    onClick={fetchMetrics}
                    disabled={!initialLoadComplete.current}
                >
                    새로고침
                </Button>
            </div>

            {/* Eng-Study Section */}
            {engStudyMetrics && (
                <div className="mb-12">
                    <h2 className="text-xl font-semibold text-primary-700 mb-4 border-l-4 border-blue-500 pl-3">
                        Eng-Study Application
                    </h2>
                    <MetricDashboard
                        current={engStudyMetrics}
                        history={engStudyHistory}
                    />
                </div>
            )}

            {/* Monitoring Section */}
            {monitoringMetrics && (
                <div className="mb-8">
                    <h2 className="text-xl font-semibold text-primary-700 mb-4 border-l-4 border-purple-500 pl-3">
                        Monitoring Application
                    </h2>
                    <MetricDashboard
                        current={monitoringMetrics}
                        history={monitoringHistory}
                    />
                </div>
            )}
        </div>
    );
}

// 컴포넌트 분리 및 재사용 (코드 중복 제거)
function MetricDashboard({ current, history }: { current: CurrentMetrics, history: MetricHistory[] }) {
    // ?. 문법(Optional Chaining)을 사용하여 안전하게 접근
    const tps = current?.metrics?.tps;
    const heap = current?.metrics?.heapUsage;
    const errRate = current?.metrics?.errorRate;
    const cpu = current?.metrics?.cpuUsage;

    return (
        <>
            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
                <MetricCard
                    icon={<Zap className="w-8 h-8" />}
                    title="TPS"
                    value={formatMetric(tps, 2, 0, 1000)} // ✅ TPS는 0~1000 범위
                    unit="req/s"
                    color="blue"
                />
                <MetricCard
                    icon={<Database className="w-8 h-8" />}
                    title="Heap 사용률"
                    value={formatMetric(heap, 1, 0, 100)} // ✅ Heap은 0~100% 범위
                    unit="%"
                    color="green"
                    warning={sanitizeMetricValue(heap, 0, 100) > 80}
                />
                <MetricCard
                    icon={<Activity className="w-8 h-8" />}
                    title="에러율"
                    value={formatMetric(errRate, 2, 0, 100)} // ✅ 에러율은 0~100% 범위
                    unit="%"
                    color="red"
                    warning={sanitizeMetricValue(errRate, 0, 100) > 1}
                />
                <MetricCard
                    icon={<Cpu className="w-8 h-8" />}
                    title="CPU 사용률"
                    value={formatMetric(cpu, 1, 0, 100)} // ✅ CPU는 0~100% 범위
                    unit="%"
                    color="purple"
                    warning={sanitizeMetricValue(cpu, 0, 100) > 80}
                />
            </div>

            {history.length > 0 && (
                <div className="grid md:grid-cols-2 gap-6">
                    <ChartCard title="TPS 추이" data={history} dataKey="tps" color="#3b82f6" name="TPS" />
                    <ChartCard title="Heap 사용률 추이" data={history} dataKey="heapUsage" color="#10b981" name="Heap %" />
                    <ChartCard title="에러율 추이" data={history} dataKey="errorRate" color="#ef4444" name="에러율 %" />
                    <ChartCard title="CPU 사용률 추이" data={history} dataKey="cpuUsage" color="#8b5cf6" name="CPU %" />
                </div>
            )}
        </>
    );
}

function MetricCard({ icon, title, value, unit, color, warning = false }: any) {
    const colorClasses: any = {
        blue: 'from-blue-400 to-blue-600',
        green: 'from-green-400 to-green-600',
        red: 'from-red-400 to-red-600',
        purple: 'from-purple-400 to-purple-600',
    };

    return (
        <Card className={warning ? 'border-2 border-warning animate-pulse' : ''}>
            <div className={`w-12 h-12 rounded-lg bg-gradient-to-br ${colorClasses[color]} flex items-center justify-center text-white mb-4 shadow-md`}>
                {icon}
            </div>
            <h3 className="text-sm font-medium text-secondary-600 mb-2">{title}</h3>
            <div className="flex items-baseline space-x-2">
                <span className="text-3xl font-bold text-primary-700">{value}</span>
                <span className="text-lg text-secondary-500">{unit}</span>
            </div>
            {warning && (
                <div className="mt-3 px-3 py-1 bg-warning/10 text-warning text-sm font-medium rounded inline-block">
                    ⚠️ 임계치 초과
                </div>
            )}
        </Card>
    );
}

function ChartCard({ title, data, dataKey, color, name }: any) {
    return (
        <Card title={title}>
            <ResponsiveContainer width="100%" height={250}>
                <LineChart data={data}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis dataKey="timestamp" stroke="#64748b" style={{ fontSize: '11px' }} />
                    <YAxis stroke="#64748b" style={{ fontSize: '12px' }} domain={[0, 'auto']} />
                    <Tooltip
                        contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                    />
                    <Line
                        type="monotone"
                        dataKey={dataKey}
                        stroke={color}
                        strokeWidth={2}
                        dot={{ fill: color, r: 2 }}
                        activeDot={{ r: 6 }}
                        name={name}
                        isAnimationActive={false} // 실시간 업데이트 시 깜빡임 방지
                    />
                </LineChart>
            </ResponsiveContainer>
        </Card>
    );
}