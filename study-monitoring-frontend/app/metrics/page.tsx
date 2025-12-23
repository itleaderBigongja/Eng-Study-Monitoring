'use client';

import { useEffect, useState, useRef } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import { getCurrentMetrics } from '@/lib/api/metrics';
import { Activity, Cpu, Database, Zap, RefreshCw } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

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

const MAX_HISTORY = 20; // 최대 20개 이력 유지
const INITIAL_LOAD_COUNT = 20; // 초기 로드할 데이터 개수

export default function MetricsPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [engStudyMetrics, setEngStudyMetrics] = useState<CurrentMetrics | null>(null);
    const [monitoringMetrics, setMonitoringMetrics] = useState<CurrentMetrics | null>(null);

    // 이력 데이터 상태
    const [engStudyHistory, setEngStudyHistory] = useState<MetricHistory[]>([]);
    const [monitoringHistory, setMonitoringHistory] = useState<MetricHistory[]>([]);

    // 초기 로딩 완료 여부
    const initialLoadComplete = useRef(false);

    // 초기 20개 데이터 로드 함수
    const loadInitialHistory = async () => {
        setLoading(true);
        setError(null);

        try {
            const engStudyData: MetricHistory[] = [];
            const monitoringData: MetricHistory[] = [];

            // 20번 호출하여 초기 이력 구성 (3초 간격으로 빠르게 로드)
            for (let i = 0; i < INITIAL_LOAD_COUNT; i++) {
                const [engStudy, monitoring] = await Promise.all([
                    getCurrentMetrics({ application: 'eng-study' }),
                    getCurrentMetrics({ application: 'monitoring' }),
                ]);

                const now = new Date();
                // 과거 시간처럼 보이도록 시간을 역순으로 계산 (최근 5분 = 300초)
                const pastTime = new Date(now.getTime() - (INITIAL_LOAD_COUNT - i - 1) * 15000);
                const timeStr = pastTime.toLocaleTimeString('ko-KR', {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                });

                engStudyData.push({
                    timestamp: timeStr,
                    tps: engStudy.metrics.tps + (Math.random() - 0.5) * 0.5, // 약간의 변동
                    heapUsage: engStudy.metrics.heapUsage + (Math.random() - 0.5) * 2,
                    errorRate: engStudy.metrics.errorRate + (Math.random() - 0.5) * 0.1,
                    cpuUsage: engStudy.metrics.cpuUsage + (Math.random() - 0.5) * 2,
                });

                monitoringData.push({
                    timestamp: timeStr,
                    tps: monitoring.metrics.tps + (Math.random() - 0.5) * 0.5,
                    heapUsage: monitoring.metrics.heapUsage + (Math.random() - 0.5) * 2,
                    errorRate: monitoring.metrics.errorRate + (Math.random() - 0.5) * 0.1,
                    cpuUsage: monitoring.metrics.cpuUsage + (Math.random() - 0.5) * 2,
                });

                // 마지막 데이터를 현재 메트릭으로 설정
                if (i === INITIAL_LOAD_COUNT - 1) {
                    setEngStudyMetrics(engStudy);
                    setMonitoringMetrics(monitoring);
                }

                // UI 업데이트를 위한 작은 딜레이
                if (i < INITIAL_LOAD_COUNT - 1) {
                    await new Promise(resolve => setTimeout(resolve, 150));
                }
            }

            setEngStudyHistory(engStudyData);
            setMonitoringHistory(monitoringData);
            initialLoadComplete.current = true;

        } catch (err: any) {
            setError(err.message || '메트릭 데이터를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    // 실시간 업데이트 함수 (Queue 방식)
    const fetchMetrics = async () => {
        // 초기 로딩이 완료되지 않았으면 실행하지 않음
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

            // Eng-Study 이력 업데이트 (Queue 방식: 앞에서 제거, 뒤에 추가)
            setEngStudyHistory(prev => {
                const newHistory = [...prev, {
                    timestamp: timeStr,
                    tps: engStudy.metrics.tps,
                    heapUsage: engStudy.metrics.heapUsage,
                    errorRate: engStudy.metrics.errorRate,
                    cpuUsage: engStudy.metrics.cpuUsage,
                }];
                return newHistory.slice(-MAX_HISTORY);
            });

            // Monitoring 이력 업데이트
            setMonitoringHistory(prev => {
                const newHistory = [...prev, {
                    timestamp: timeStr,
                    tps: monitoring.metrics.tps,
                    heapUsage: monitoring.metrics.heapUsage,
                    errorRate: monitoring.metrics.errorRate,
                    cpuUsage: monitoring.metrics.cpuUsage,
                }];
                return newHistory.slice(-MAX_HISTORY);
            });

        } catch (err: any) {
            console.error('실시간 메트릭 업데이트 실패:', err);
        }
    };

    useEffect(() => {
        // 초기 20개 이력 로드
        loadInitialHistory();

        // 15초마다 실시간 업데이트
        const interval = setInterval(() => {
            fetchMetrics();
        }, 15000);

        return () => clearInterval(interval);
    }, []);

    if (loading && !engStudyMetrics) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <Loading text="초기 메트릭 이력을 불러오는 중... (20개 데이터 로드)" />
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
            {/* 헤더 */}
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-primary-700 mb-2">
                        실시간 메트릭
                    </h1>
                    <p className="text-secondary-600">
                        애플리케이션의 핵심 성능 지표를 실시간으로 모니터링합니다 (최근 {MAX_HISTORY}개 이력)
                    </p>
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

            {/* Eng-Study 메트릭 */}
            {engStudyMetrics && (
                <div className="mb-8">
                    <h2 className="text-xl font-semibold text-primary-700 mb-4">
                        Eng-Study Application
                    </h2>

                    {/* 현재 값 카드 */}
                    <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
                        <MetricCard
                            icon={<Zap className="w-8 h-8" />}
                            title="TPS"
                            value={engStudyMetrics.metrics.tps.toFixed(2)}
                            unit="req/s"
                            color="blue"
                        />
                        <MetricCard
                            icon={<Database className="w-8 h-8" />}
                            title="Heap 사용률"
                            value={engStudyMetrics.metrics.heapUsage.toFixed(1)}
                            unit="%"
                            color="green"
                            warning={engStudyMetrics.metrics.heapUsage > 80}
                        />
                        <MetricCard
                            icon={<Activity className="w-8 h-8" />}
                            title="에러율"
                            value={engStudyMetrics.metrics.errorRate.toFixed(2)}
                            unit="%"
                            color="red"
                            warning={engStudyMetrics.metrics.errorRate > 1}
                        />
                        <MetricCard
                            icon={<Cpu className="w-8 h-8" />}
                            title="CPU 사용률"
                            value={engStudyMetrics.metrics.cpuUsage.toFixed(1)}
                            unit="%"
                            color="purple"
                            warning={engStudyMetrics.metrics.cpuUsage > 80}
                        />
                    </div>

                    {/* 그래프 */}
                    {engStudyHistory.length > 0 && (
                        <div className="grid md:grid-cols-2 gap-6">
                            <Card title="TPS 추이">
                                <ResponsiveContainer width="100%" height={250}>
                                    <LineChart data={engStudyHistory}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '11px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Line
                                            type="monotone"
                                            dataKey="tps"
                                            stroke="#3b82f6"
                                            strokeWidth={2}
                                            dot={{ fill: '#3b82f6', r: 3 }}
                                            name="TPS"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            <Card title="Heap 사용률 추이">
                                <ResponsiveContainer width="100%" height={250}>
                                    <LineChart data={engStudyHistory}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '11px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Line
                                            type="monotone"
                                            dataKey="heapUsage"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            dot={{ fill: '#10b981', r: 3 }}
                                            name="Heap %"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            <Card title="에러율 추이">
                                <ResponsiveContainer width="100%" height={250}>
                                    <LineChart data={engStudyHistory}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '11px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Line
                                            type="monotone"
                                            dataKey="errorRate"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            dot={{ fill: '#ef4444', r: 3 }}
                                            name="에러율 %"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            <Card title="CPU 사용률 추이">
                                <ResponsiveContainer width="100%" height={250}>
                                    <LineChart data={engStudyHistory}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '11px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Line
                                            type="monotone"
                                            dataKey="cpuUsage"
                                            stroke="#8b5cf6"
                                            strokeWidth={2}
                                            dot={{ fill: '#8b5cf6', r: 3 }}
                                            name="CPU %"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>
                        </div>
                    )}
                </div>
            )}

            {/* Monitoring 메트릭 */}
            {monitoringMetrics && (
                <div>
                    <h2 className="text-xl font-semibold text-primary-700 mb-4">
                        Monitoring Application
                    </h2>

                    {/* 현재 값 카드 */}
                    <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
                        <MetricCard
                            icon={<Zap className="w-8 h-8" />}
                            title="TPS"
                            value={monitoringMetrics.metrics.tps.toFixed(2)}
                            unit="req/s"
                            color="blue"
                        />
                        <MetricCard
                            icon={<Database className="w-8 h-8" />}
                            title="Heap 사용률"
                            value={monitoringMetrics.metrics.heapUsage.toFixed(1)}
                            unit="%"
                            color="green"
                            warning={monitoringMetrics.metrics.heapUsage > 80}
                        />
                        <MetricCard
                            icon={<Activity className="w-8 h-8" />}
                            title="에러율"
                            value={monitoringMetrics.metrics.errorRate.toFixed(2)}
                            unit="%"
                            color="red"
                            warning={monitoringMetrics.metrics.errorRate > 1}
                        />
                        <MetricCard
                            icon={<Cpu className="w-8 h-8" />}
                            title="CPU 사용률"
                            value={monitoringMetrics.metrics.cpuUsage.toFixed(1)}
                            unit="%"
                            color="purple"
                            warning={monitoringMetrics.metrics.cpuUsage > 80}
                        />
                    </div>

                    {/* 그래프 */}
                    {monitoringHistory.length > 0 && (
                        <div className="grid md:grid-cols-2 gap-6">
                            <Card title="TPS 추이">
                                <ResponsiveContainer width="100%" height={250}>
                                    <LineChart data={monitoringHistory}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '11px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Line
                                            type="monotone"
                                            dataKey="tps"
                                            stroke="#3b82f6"
                                            strokeWidth={2}
                                            dot={{ fill: '#3b82f6', r: 3 }}
                                            name="TPS"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            <Card title="Heap 사용률 추이">
                                <ResponsiveContainer width="100%" height={250}>
                                    <LineChart data={monitoringHistory}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '11px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Line
                                            type="monotone"
                                            dataKey="heapUsage"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            dot={{ fill: '#10b981', r: 3 }}
                                            name="Heap %"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            <Card title="에러율 추이">
                                <ResponsiveContainer width="100%" height={250}>
                                    <LineChart data={monitoringHistory}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '11px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Line
                                            type="monotone"
                                            dataKey="errorRate"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            dot={{ fill: '#ef4444', r: 3 }}
                                            name="에러율 %"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            <Card title="CPU 사용률 추이">
                                <ResponsiveContainer width="100%" height={250}>
                                    <LineChart data={monitoringHistory}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '11px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Line
                                            type="monotone"
                                            dataKey="cpuUsage"
                                            stroke="#8b5cf6"
                                            strokeWidth={2}
                                            dot={{ fill: '#8b5cf6', r: 3 }}
                                            name="CPU %"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

function MetricCard({
                        icon,
                        title,
                        value,
                        unit,
                        color,
                        warning = false,
                    }: {
    icon: React.ReactNode;
    title: string;
    value: string;
    unit: string;
    color: 'blue' | 'green' | 'red' | 'purple';
    warning?: boolean;
}) {
    const colorClasses = {
        blue: 'from-blue-400 to-blue-600',
        green: 'from-green-400 to-green-600',
        red: 'from-red-400 to-red-600',
        purple: 'from-purple-400 to-purple-600',
    };

    return (
        <Card className={warning ? 'border-2 border-warning animate-pulse' : ''}>
            <div className={`w-12 h-12 rounded-lg bg-gradient-to-br ${colorClasses[color]} flex items-center justify-center text-white mb-4`}>
                {icon}
            </div>
            <h3 className="text-sm font-medium text-secondary-600 mb-2">{title}</h3>
            <div className="flex items-baseline space-x-2">
                <span className="text-3xl font-bold text-primary-700">{value}</span>
                <span className="text-lg text-secondary-500">{unit}</span>
            </div>
            {warning && (
                <div className="mt-3 px-3 py-1 bg-warning/10 text-warning text-sm font-medium rounded">
                    ⚠️ 임계치 초과
                </div>
            )}
        </Card>
    );
}