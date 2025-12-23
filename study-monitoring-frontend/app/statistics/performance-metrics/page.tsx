'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getPerformanceMetricsStatistics } from '@/lib/api/statistics';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, AreaChart, Area } from 'recharts';
import { Search, Activity, Cpu, Database } from 'lucide-react';

export default function PerformanceMetricsStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return date.toISOString().slice(0, 16);
    });
    const [endTime, setEndTime] = useState(() => {
        return new Date().toISOString().slice(0, 16);
    });

    const handleSearch = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await getPerformanceMetricsStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || '성능 메트릭 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    성능 메트릭 통계
                </h1>
                <p className="text-secondary-600">
                    CPU, 메모리, JVM 성능 지표 분석
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    시간 주기
                                </label>
                                <select
                                    value={timePeriod}
                                    onChange={(e) => setTimePeriod(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="MINUTE">분</option>
                                    <option value="HOUR">시간</option>
                                    <option value="DAY">일</option>
                                </select>
                            </div>

                            <Button
                                variant="primary"
                                icon={<Search className="w-4 h-4" />}
                                onClick={handleSearch}
                                loading={loading}
                                className="w-full"
                            >
                                조회
                            </Button>
                        </div>
                    </Card>

                    <div className="mt-6">
                        <DateRangePicker
                            startDate={startTime}
                            endDate={endTime}
                            onChange={handleDateRangeChange}
                        />
                    </div>
                </div>

                <div className="lg:col-span-2">
                    {loading && <Loading text="데이터를 불러오는 중..." />}

                    {error && <ErrorMessage message={error} onRetry={handleSearch} />}

                    {!loading && !error && data && (
                        <>
                            {/* 시스템 메트릭 요약 */}
                            <Card title="시스템 메트릭 요약" className="mb-6">
                                <div className="grid md:grid-cols-3 gap-6">
                                    <div className="flex items-start space-x-3">
                                        <div className="p-3 bg-blue-100 rounded-lg">
                                            <Cpu className="w-6 h-6 text-blue-600" />
                                        </div>
                                        <div>
                                            <p className="text-sm text-secondary-600">평균 CPU</p>
                                            <p className="text-2xl font-bold text-primary-700">
                                                {data.systemMetrics?.avgCpuUsage?.toFixed(1) || 0}%
                                            </p>
                                            <p className="text-xs text-secondary-500">
                                                최대: {data.systemMetrics?.maxCpuUsage?.toFixed(1) || 0}%
                                            </p>
                                        </div>
                                    </div>

                                    <div className="flex items-start space-x-3">
                                        <div className="p-3 bg-green-100 rounded-lg">
                                            <Database className="w-6 h-6 text-green-600" />
                                        </div>
                                        <div>
                                            <p className="text-sm text-secondary-600">평균 메모리</p>
                                            <p className="text-2xl font-bold text-primary-700">
                                                {data.systemMetrics?.avgMemoryUsage?.toFixed(1) || 0}%
                                            </p>
                                            <p className="text-xs text-secondary-500">
                                                최대: {data.systemMetrics?.maxMemoryUsage?.toFixed(1) || 0}%
                                            </p>
                                        </div>
                                    </div>

                                    <div className="flex items-start space-x-3">
                                        <div className="p-3 bg-purple-100 rounded-lg">
                                            <Activity className="w-6 h-6 text-purple-600" />
                                        </div>
                                        <div>
                                            <p className="text-sm text-secondary-600">평균 디스크</p>
                                            <p className="text-2xl font-bold text-primary-700">
                                                {data.systemMetrics?.avgDiskUsage?.toFixed(1) || 0}%
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </Card>

                            {/* JVM 메트릭 요약 */}
                            <Card title="JVM 메트릭 요약" className="mb-6">
                                <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
                                    <div className="p-4 bg-primary-50 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">평균 Heap</p>
                                        <p className="text-xl font-bold text-primary-700">
                                            {data.jvmMetrics?.avgHeapUsed?.toFixed(0) || 0} <span className="text-sm font-normal">MB</span>
                                        </p>
                                    </div>
                                    <div className="p-4 bg-primary-50 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">최대 Heap</p>
                                        <p className="text-xl font-bold text-primary-700">
                                            {data.jvmMetrics?.maxHeapUsed?.toFixed(0) || 0} <span className="text-sm font-normal">MB</span>
                                        </p>
                                    </div>
                                    <div className="p-4 bg-primary-50 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">GC 횟수</p>
                                        <p className="text-xl font-bold text-primary-700">
                                            {data.jvmMetrics?.totalGcCount || 0}
                                        </p>
                                    </div>
                                    <div className="p-4 bg-primary-50 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">GC 시간</p>
                                        <p className="text-xl font-bold text-primary-700">
                                            {data.jvmMetrics?.totalGcTime || 0} ms
                                        </p>
                                    </div>
                                </div>
                            </Card>

                            {/* 시스템 리소스 추이 */}
                            <Card title="시스템 리소스 사용률 추이" className="mb-6">
                                <ResponsiveContainer width="100%" height={400}>
                                    <AreaChart data={data.distributions}>
                                        <defs>
                                            <linearGradient id="colorCpu" x1="0" y1="0" x2="0" y2="1">
                                                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8}/>
                                                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                                            </linearGradient>
                                            <linearGradient id="colorMemory" x1="0" y1="0" x2="0" y2="1">
                                                <stop offset="5%" stopColor="#10b981" stopOpacity={0.8}/>
                                                <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                                            </linearGradient>
                                        </defs>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis dataKey="timestamp" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Legend />
                                        <Area
                                            type="monotone"
                                            dataKey="cpuUsage"
                                            stroke="#3b82f6"
                                            fillOpacity={1}
                                            fill="url(#colorCpu)"
                                            name="CPU 사용률 (%)"
                                        />
                                        <Area
                                            type="monotone"
                                            dataKey="memoryUsage"
                                            stroke="#10b981"
                                            fillOpacity={1}
                                            fill="url(#colorMemory)"
                                            name="메모리 사용률 (%)"
                                        />
                                    </AreaChart>
                                </ResponsiveContainer>
                            </Card>

                            {/* Heap 사용량 추이 */}
                            <Card title="Heap 메모리 사용량 추이">
                                <ResponsiveContainer width="100%" height={300}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis dataKey="timestamp" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Legend />
                                        <Line
                                            type="monotone"
                                            dataKey="heapUsage"
                                            stroke="#8b5cf6"
                                            strokeWidth={2}
                                            name="Heap 사용률 (%)"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <Activity className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
                                <p className="text-secondary-600">
                                    검색 조건을 설정하고 조회 버튼을 클릭하세요
                                </p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
}