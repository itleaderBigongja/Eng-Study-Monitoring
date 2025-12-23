'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getAuditLogStatistics } from '@/lib/api/statistics';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
    ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell
} from 'recharts';
import { Search, Shield } from 'lucide-react';

const COLORS = ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

// 로컬 시간 변환 헬퍼 함수
const getLocalISOString = (date: Date) => {
    const offset = date.getTimezoneOffset() * 60000;
    const localDate = new Date(date.getTime() - offset);
    return localDate.toISOString().slice(0, 16);
};

export default function AuditLogStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [eventAction, setEventAction] = useState('');

    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return getLocalISOString(date);
    });

    const [endTime, setEndTime] = useState(() => {
        const date = new Date();
        return getLocalISOString(date);
    });

    const handleSearch = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await getAuditLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
                eventAction: eventAction || undefined,
            });

            console.log('API Response:', result); // 데이터 확인용
            setData(result);
        } catch (err: any) {
            setError(err.message || '감사 로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    // [수정 1] 키 이름 변경: actionCounts -> eventActionCounts
    const actionChartData = data?.eventActionCounts
        ? Object.entries(data.eventActionCounts).map(([action, count]) => ({
            action,
            count,
        }))
        : [];

    const categoryChartData = data?.categoryCounts
        ? Object.entries(data.categoryCounts).map(([category, count]) => ({
            category,
            count,
        }))
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    감사 로그 통계
                </h1>
                <p className="text-secondary-600">
                    사용자 액션, 이벤트 카테고리, 성공/실패율 분석
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    이벤트 액션 (선택)
                                </label>
                                <input
                                    type="text"
                                    value={eventAction}
                                    onChange={(e) => setEventAction(e.target.value)}
                                    placeholder="예: user.login"
                                    className="input-field"
                                />
                            </div>

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
                            <div className="grid md:grid-cols-4 gap-4 mb-6">
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">총 이벤트</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {/* [수정 2] data.eventActionCounts 사용 */}
                                        {(Object.values(data.eventActionCounts || {}).reduce((a: any, b: any) => a + b, 0) as number)}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">성공 이벤트</p>
                                    <p className="text-3xl font-bold text-success">
                                        {/* [수정 3] data.resultStats 구조 반영 */}
                                        {data.resultStats?.successCount || 0}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">실패 이벤트</p>
                                    <p className="text-3xl font-bold text-error">
                                        {/* [수정 3] data.resultStats 구조 반영 */}
                                        {data.resultStats?.failureCount || 0}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">성공률</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {data.resultStats?.successRate?.toFixed(1) || '0.0'}
                                        <span className="text-lg text-secondary-500 ml-1">%</span>
                                    </p>
                                </Card>
                            </div>

                            {/* 액션별 & 카테고리별 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="액션별 이벤트 수">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <BarChart data={actionChartData} layout="vertical">
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis type="number" />
                                            <YAxis
                                                dataKey="action"
                                                type="category"
                                                width={120}
                                                style={{ fontSize: '11px' }}
                                            />
                                            <Tooltip />
                                            <Bar dataKey="count" fill="#0ea5e9" />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Card>

                                <Card title="카테고리별 분포">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <PieChart>
                                            <Pie
                                                data={categoryChartData}
                                                dataKey="count"
                                                nameKey="category"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={100}
                                                label
                                            >
                                                {categoryChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip />
                                            <Legend />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>
                            </div>

                            {/* 시간대별 추이 */}
                            <Card title="시간대별 감사 로그 추이">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '12px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: '#fff',
                                                border: '1px solid #e2e8f0',
                                                borderRadius: '8px',
                                            }}
                                        />
                                        <Legend />
                                        {/* [수정 4] 서버 응답 키와 정확히 일치시킴 (오타 포함) */}
                                        <Line
                                            type="monotone"
                                            dataKey="totalEvents"
                                            stroke="#0ea5e9"
                                            strokeWidth={2}
                                            name="전체 이벤트"
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="successEvents"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            name="성공"
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="failureEvents"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            name="실패"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            {/* 사용자별 통계 */}
                            {data.userCounts && Object.keys(data.userCounts).length > 0 && (
                                <Card title="사용자별 활동 통계" className="mt-6">
                                    <div className="overflow-x-auto">
                                        <table className="min-w-full divide-y divide-gray-200">
                                            <thead className="bg-gray-50">
                                            <tr>
                                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                    사용자
                                                </th>
                                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                    이벤트 수
                                                </th>
                                            </tr>
                                            </thead>
                                            <tbody className="bg-white divide-y divide-gray-200">
                                            {Object.entries(data.userCounts)
                                                .sort((a: any, b: any) => b[1] - a[1])
                                                .slice(0, 10)
                                                .map(([user, count]: any, index: number) => (
                                                    <tr key={index}>
                                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                                            {user}
                                                        </td>
                                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-primary-700 font-semibold">
                                                            {count}
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </Card>
                            )}
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <Shield className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
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