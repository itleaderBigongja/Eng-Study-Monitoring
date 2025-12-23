'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getErrorLogStatistics } from '@/lib/api/statistics';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line } from 'recharts';
import { Search, AlertCircle } from 'lucide-react';

const COLORS = ['#ef4444', '#f59e0b', '#10b981', '#0ea5e9', '#8b5cf6'];

export default function ErrorLogStatisticsPage() {
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
            const result = await getErrorLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || '에러 로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    const errorTypeChartData = data?.errorTypeCounts
        ? Object.entries(data.errorTypeCounts).map(([type, count]) => ({
            type,
            count,
        }))
        : [];

    const severityChartData = data?.severityCounts
        ? Object.entries(data.severityCounts).map(([severity, count]) => ({
            severity,
            count,
        }))
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    에러 로그 통계
                </h1>
                <p className="text-secondary-600">
                    에러 타입, 심각도별 통계 및 발생 빈도 분석
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
                            {/* 요약 카드 */}
                            <div className="grid md:grid-cols-3 gap-4 mb-6">
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">총 에러 수</p>
                                    <p className="text-3xl font-bold text-error">
                                        {(Object.values(data.errorTypeCounts || {}).reduce((a: any, b: any) => a + b, 0) as number)}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">에러 타입 수</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {Object.keys(data.errorTypeCounts || {}).length}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">Critical 에러</p>
                                    <p className="text-3xl font-bold text-error">
                                        {data.severityCounts?.CRITICAL || 0}
                                    </p>
                                </Card>
                            </div>

                            {/* 에러 타입 & 심각도 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="에러 타입별 분포">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <BarChart data={errorTypeChartData} layout="vertical">
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis type="number" />
                                            <YAxis dataKey="type" type="category" width={150} style={{ fontSize: '11px' }} />
                                            <Tooltip />
                                            <Bar dataKey="count" fill="#ef4444" />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Card>

                                <Card title="심각도별 분포">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <PieChart>
                                            <Pie
                                                data={severityChartData}
                                                dataKey="count"
                                                nameKey="severity"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={100}
                                                label={(entry) => `${entry.severity}: ${entry.count}`}
                                            >
                                                {severityChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>
                            </div>

                            {/* 시간대별 에러 발생 추이 */}
                            <Card title="시간대별 에러 발생 추이">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis dataKey="timestamp" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: '#fff',
                                                border: '1px solid #e2e8f0',
                                                borderRadius: '8px',
                                            }}
                                        />
                                        <Legend />
                                        <Line
                                            type="monotone"
                                            dataKey="errorCount"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            name="에러 수"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            {/* 에러 타입별 상세 테이블 */}
                            <Card title="에러 타입별 상세 정보" className="mt-6">
                                <div className="overflow-x-auto">
                                    <table className="min-w-full divide-y divide-gray-200">
                                        <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                에러 타입
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                발생 횟수
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                비율
                                            </th>
                                        </tr>
                                        </thead>
                                        <tbody className="bg-white divide-y divide-gray-200">
                                        {errorTypeChartData.map((item: any, index: number) => {
                                            const total = errorTypeChartData.reduce((sum, i: any) => sum + i.count, 0);
                                            const percentage = ((item.count / total) * 100).toFixed(1);
                                            return (
                                                <tr key={index}>
                                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                                        {item.type}
                                                    </td>
                                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-error font-semibold">
                                                        {item.count}
                                                    </td>
                                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                        {percentage}%
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                        </tbody>
                                    </table>
                                </div>
                            </Card>
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <AlertCircle className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
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