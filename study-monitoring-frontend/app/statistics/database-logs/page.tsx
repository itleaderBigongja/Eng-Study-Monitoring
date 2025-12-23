'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getDatabaseLogStatistics } from '@/lib/api/statistics';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
    ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell
} from 'recharts';
import { Search, Database } from 'lucide-react';

const COLORS = ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

export default function DatabaseLogStatisticsPage() {
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
            const result = await getDatabaseLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || 'DB 로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    const operationChartData = data?.operationCounts
        ? Object.entries(data.operationCounts).map(([operation, count]) => ({
            operation,
            count,
        }))
        : [];

    const tableChartData = data?.tableCounts
        ? Object.entries(data.tableCounts)
            .map(([table, count]) => ({ table, count }))
            .sort((a: any, b: any) => b.count - a.count)
            .slice(0, 10)
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    데이터베이스 로그 통계
                </h1>
                <p className="text-secondary-600">
                    쿼리 실행시간, Operation별, 테이블별 통계
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
                            {/* 쿼리 성능 요약 */}
                            <Card title="쿼리 성능 요약" className="mb-6">
                                <div className="grid md:grid-cols-4 gap-4">
                                    <div className="p-4 bg-primary-50 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">평균 실행시간</p>
                                        <p className="text-2xl font-bold text-primary-700">
                                            {data.queryPerformance?.avgDuration?.toFixed(0) || 0}
                                            <span className="text-sm text-secondary-500 ml-1">ms</span>
                                        </p>
                                    </div>
                                    <div className="p-4 bg-warning/10 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">최대 실행시간</p>
                                        <p className="text-2xl font-bold text-warning">
                                            {data.queryPerformance?.maxDuration?.toFixed(0) || 0}
                                            <span className="text-sm text-secondary-500 ml-1">ms</span>
                                        </p>
                                    </div>
                                    <div className="p-4 bg-error/10 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">느린 쿼리</p>
                                        <p className="text-2xl font-bold text-error">
                                            {data.queryPerformance?.slowQueryCount || 0}
                                        </p>
                                    </div>
                                    <div className="p-4 bg-success/10 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">전체 쿼리</p>
                                        <p className="text-2xl font-bold text-success">
                                            {data.queryPerformance?.totalQueryCount || 0}
                                        </p>
                                    </div>
                                </div>
                            </Card>

                            {/* Operation별 & 테이블별 통계 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="Operation별 쿼리 수">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <PieChart>
                                            <Pie
                                                data={operationChartData}
                                                dataKey="count"
                                                nameKey="operation"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={100}
                                                label
                                            >
                                                {/* ▼▼▼ 여기가 수정된 부분입니다 (백틱 ` 적용) ▼▼▼ */}
                                                {operationChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip />
                                            <Legend />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>
                                <Card title="테이블별 쿼리 수 (Top 10)">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <BarChart data={tableChartData} layout="vertical">
                                            {/* layout="vertical"일 때는 X/Y축 설정을 맞춰주어야 바가 보입니다 */}
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis type="number" />
                                            <YAxis dataKey="table" type="category" width={100} style={{ fontSize: '11px' }} />
                                            <Tooltip />
                                            <Bar dataKey="count" fill="#0ea5e9" />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Card>
                            </div>

                            {/* 시간대별 DB 로그 분포 */}
                            <Card title="시간대별 쿼리 실행 추이">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis dataKey="timestamp" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <YAxis yAxisId="left" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <YAxis yAxisId="right" orientation="right" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Legend />
                                        <Line
                                            yAxisId="left"
                                            type="monotone"
                                            dataKey="queryCount"
                                            stroke="#0ea5e9"
                                            strokeWidth={2}
                                            name="쿼리 수"
                                        />
                                        <Line
                                            yAxisId="right"
                                            type="monotone"
                                            dataKey="avgDuration"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            name="평균 실행시간 (ms)"
                                        />
                                        <Line
                                            yAxisId="left"
                                            type="monotone"
                                            dataKey="slowQueryCount"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            name="느린 쿼리 수"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <Database className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
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