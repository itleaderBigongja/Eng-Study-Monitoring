'use client';

import { useEffect, useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import { getCurrentMetrics } from '@/lib/api/metrics';
import { Activity, Cpu, Database, Zap, RefreshCw } from 'lucide-react';

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

export default function MetricsPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [engStudyMetrics, setEngStudyMetrics] = useState<CurrentMetrics | null>(null);
    const [monitoringMetrics, setMonitoringMetrics] = useState<CurrentMetrics | null>(null);

    const fetchMetrics = async () => {
        setLoading(true);
        setError(null);

        try {
            const [engStudy, monitoring] = await Promise.all([
                getCurrentMetrics({ application: 'eng-study' }),
                getCurrentMetrics({ application: 'monitoring' }),
            ]);

            setEngStudyMetrics(engStudy);
            setMonitoringMetrics(monitoring);
        } catch (err: any) {
            setError(err.message || '메트릭 데이터를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchMetrics();

        // 15초마다 자동 새로고침
        const interval = setInterval(fetchMetrics, 15000);
        return () => clearInterval(interval);
    }, []);

    if (loading && !engStudyMetrics) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <Loading text="메트릭 데이터를 불러오는 중..." />
            </div>
        );
    }

    if (error && !engStudyMetrics) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <ErrorMessage message={error} onRetry={fetchMetrics} />
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
                        애플리케이션의 핵심 성능 지표를 실시간으로 모니터링합니다
                    </p>
                </div>
                <Button
                    variant="outline"
                    icon={<RefreshCw className="w-4 h-4" />}
                    onClick={fetchMetrics}
                    loading={loading}
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
                    <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
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
                </div>
            )}

            {/* Monitoring 메트릭 */}
            {monitoringMetrics && (
                <div>
                    <h2 className="text-xl font-semibold text-primary-700 mb-4">
                        Monitoring Application
                    </h2>
                    <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
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