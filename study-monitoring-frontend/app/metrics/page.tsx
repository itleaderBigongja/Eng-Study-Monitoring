// app/metrics/page.tsx
'use client';

import { useState } from 'react';
import { Activity, Cpu, Database, Zap, RefreshCw, Bell, Download } from 'lucide-react';
import Button from '@/components/common/Button';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import MetricCard from '@/components/metrics/MetricCard';
import MetricChart from '@/components/metrics/MetricChartProps';
import TimeRangeSelector, { TimeRange } from '@/components/metrics/TimeRangeSelector';
import AppTabs, { DEFAULT_APPS } from '@/components/metrics/AppTabs';
import { useMetricsRange } from '@/hooks/useMetricsRange';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import Card from '@/components/common/Card';

// ✅ 안전한 숫자 변환 및 범위 검증 + 포맷팅 헬퍼 함수
const formatMetric = (
    value: number | undefined | null,
    fractionDigits: number = 2,
    min: number = 0,
    max: number = 100
): string => {
    if (value === undefined || value === null || isNaN(value) || !isFinite(value)) {
        return '0.00';
    }
    const sanitized = Math.max(min, Math.min(max, value));
    return sanitized.toFixed(fractionDigits);
};

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
    const [selectedApp, setSelectedApp] = useState('eng-study');
    const [timeRange, setTimeRange] = useState<TimeRange>('5m');
    const [autoRefresh, setAutoRefresh] = useState(true);

    // ✅ [신규] useMetricsRange 훅 사용
    const {
        current: metrics,
        history,
        loading,
        error,
        refetch
    } = useMetricsRange({
        application: selectedApp,
        timeRange,
        refreshInterval: autoRefresh ? 5000 : 0
    });

    // ✅ DB/ES는 메트릭 의미가 다름 (표시 레이블 변경)
    const getMetricLabel = (key: string) => {
        if (selectedApp === 'postgres' || selectedApp === 'elasticsearch') {
            switch (key) {
                case 'heapUsage':
                    return selectedApp === 'postgres' ? '디스크 사용량' : '인덱스 크기';
                case 'cpuUsage':
                    return '활성 작업 수';
                case 'tps':
                    return selectedApp === 'postgres' ? '트랜잭션/초' : '인덱싱/초';
                default:
                    return key;
            }
        }
        return key;
    };

    const getMetricUnit = (key: string) => {
        if (selectedApp === 'postgres' || selectedApp === 'elasticsearch') {
            switch (key) {
                case 'heapUsage':
                    return 'MB';
                case 'cpuUsage':
                    return 'count';
                case 'tps':
                    return '/s';
                default:
                    return '%';
            }
        }

        switch (key) {
            case 'tps':
                return 'req/s';
            case 'heapUsage':
            case 'cpuUsage':
            case 'errorRate':
                return '%';
            default:
                return '';
        }
    };

    // 로딩 상태
    if (loading && !metrics) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <Loading text="시스템 메트릭을 초기화하는 중..." />
            </div>
        );
    }

    // 에러 상태
    if (error && !metrics) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <ErrorMessage message={error} onRetry={refetch} />
            </div>
        );
    }

    const currentMetrics = metrics?.metrics;

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* 헤더 */}
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-3xl font-bold text-primary-700 mb-2">실시간 메트릭</h1>
                    <p className="text-secondary-600">애플리케이션 상태 모니터링</p>
                </div>

                <div className="flex gap-3">
                    {/* 시간 범위 선택 */}
                    <TimeRangeSelector value={timeRange} onChange={setTimeRange} />

                    {/* 자동 새로고침 토글 */}
                    <Button
                        variant={autoRefresh ? 'primary' : 'outline'}
                        icon={<RefreshCw className={`w-4 h-4 ${autoRefresh ? 'animate-spin' : ''}`} />}
                        onClick={() => setAutoRefresh(!autoRefresh)}
                    >
                        {autoRefresh ? '자동 새로고침 ON' : '자동 새로고침 OFF'}
                    </Button>

                    {/* 새로고침 버튼 */}
                    <Button
                        variant="outline"
                        icon={<RefreshCw className="w-4 h-4" />}
                        onClick={refetch}
                    >
                        새로고침
                    </Button>
                </div>
            </div>

            {/* 애플리케이션 탭 */}
            <AppTabs
                apps={DEFAULT_APPS}
                selectedApp={selectedApp}
                onChange={setSelectedApp}
                className="mb-6"
            />

            {/* 현재 메트릭 카드 */}
            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <MetricCard
                    icon={<Zap className="w-8 h-8" />}
                    title={getMetricLabel('tps')}
                    value={formatMetric(currentMetrics?.tps, 2, 0, 1000)}
                    unit={getMetricUnit('tps')}
                    color="blue"
                />
                <MetricCard
                    icon={<Database className="w-8 h-8" />}
                    title={getMetricLabel('heapUsage')}
                    value={formatMetric(currentMetrics?.heapUsage, 1, 0, selectedApp === 'postgres' || selectedApp === 'elasticsearch' ? 10000 : 100)}
                    unit={getMetricUnit('heapUsage')}
                    color="green"
                    warning={selectedApp !== 'postgres' && selectedApp !== 'elasticsearch' && sanitizeMetricValue(currentMetrics?.heapUsage, 0, 100) > 80}
                />
                <MetricCard
                    icon={<Activity className="w-8 h-8" />}
                    title={getMetricLabel('errorRate')}
                    value={formatMetric(currentMetrics?.errorRate, 2, 0, 100)}
                    unit={getMetricUnit('errorRate')}
                    color="red"
                    warning={sanitizeMetricValue(currentMetrics?.errorRate, 0, 100) > 1}
                />
                <MetricCard
                    icon={<Cpu className="w-8 h-8" />}
                    title={getMetricLabel('cpuUsage')}
                    value={formatMetric(currentMetrics?.cpuUsage, 1, 0, selectedApp === 'postgres' || selectedApp === 'elasticsearch' ? 1000 : 100)}
                    unit={getMetricUnit('cpuUsage')}
                    color="purple"
                    warning={selectedApp !== 'postgres' && selectedApp !== 'elasticsearch' && sanitizeMetricValue(currentMetrics?.cpuUsage, 0, 100) > 80}
                />
            </div>

            {/* 전체 메트릭 통합 차트 (모든 메트릭을 한눈에) */}
            <Card title="전체 메트릭 추이" className="mb-8">
                <div className="text-sm text-gray-500 mb-4 text-right">
                    마지막 업데이트: {new Date().toLocaleTimeString('ko-KR')}
                </div>
                <ResponsiveContainer width="100%" height={350}>
                    <LineChart data={history}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                        <XAxis dataKey="timestamp" stroke="#64748b" style={{ fontSize: '11px' }} />
                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                        <Tooltip
                            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                        />
                        <Legend />
                        <Line type="monotone" dataKey="tps" stroke="#3b82f6" strokeWidth={2} name={getMetricLabel('tps')} dot={false} />
                        <Line type="monotone" dataKey="heapUsage" stroke="#10b981" strokeWidth={2} name={getMetricLabel('heapUsage')} dot={false} />
                        <Line type="monotone" dataKey="cpuUsage" stroke="#8b5cf6" strokeWidth={2} name={getMetricLabel('cpuUsage')} dot={false} />
                    </LineChart>
                </ResponsiveContainer>
            </Card>

            {/* ✅ [신규] 개별 차트 - 시계열 통계 스타일 (3선 표현) */}
            {history.length > 0 && (
                <div className="grid md:grid-cols-2 gap-6">
                    <MetricChart
                        title={`${getMetricLabel('tps')} 추이`}
                        data={history}
                        dataKey="tps"
                        color="#3b82f6"
                        name={getMetricLabel('tps')}
                    />
                    <MetricChart
                        title={`${getMetricLabel('heapUsage')} 추이`}
                        data={history}
                        dataKey="heapUsage"
                        color="#10b981"
                        name={getMetricLabel('heapUsage')}
                    />
                    <MetricChart
                        title={`${getMetricLabel('cpuUsage')} 추이`}
                        data={history}
                        dataKey="cpuUsage"
                        color="#8b5cf6"
                        name={getMetricLabel('cpuUsage')}
                    />
                    <MetricChart
                        title={`${getMetricLabel('errorRate')} 추이`}
                        data={history}
                        dataKey="errorRate"
                        color="#ef4444"
                        name={getMetricLabel('errorRate')}
                    />
                </div>
            )}

            {/* 안내 메시지 */}
            <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div className="flex">
                    <div className="text-blue-600 mr-3">💡</div>
                    <div>
                        <h4 className="text-sm font-semibold text-blue-900 mb-1">차트 설명</h4>
                        <p className="text-sm text-blue-800">
                            각 차트는 시계열 통계 스타일로 <strong>최소값 (점선)</strong>, <strong>현재값 (굵은 선)</strong>, <strong>최대값 (점선)</strong>을 표시합니다.
                            {(selectedApp === 'postgres' || selectedApp === 'elasticsearch') && (
                                <> DB/ES의 경우 메트릭 의미가 다릅니다: Heap → {getMetricLabel('heapUsage')}, CPU → {getMetricLabel('cpuUsage')}</>
                            )}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}