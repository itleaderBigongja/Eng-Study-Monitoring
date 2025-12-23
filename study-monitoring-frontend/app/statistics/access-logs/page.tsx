'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getAccessLogStatistics } from '@/lib/api/statistics';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell } from 'recharts';
import { Search, Activity } from 'lucide-react';

const COLORS = ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

// [수정 1] 로컬 시간 변환 헬퍼 함수 추가
const getLocalISOString = (date: Date) => {
    const offset = date.getTimezoneOffset() * 60000;
    const localDate = new Date(date.getTime() - offset);
    return localDate.toISOString().slice(0, 16);
};

export default function AccessLogStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');

    // [수정 2] 초기값을 로컬 시간 함수로 변경
    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return getLocalISOString(date);
    });

    // [수정 3] 초기값을 로컬 시간 함수로 변경
    const [endTime, setEndTime] = useState(() => {
        const date = new Date();
        return getLocalISOString(date);
    });

    const handleSearch = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await getAccessLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || '접근 로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    const methodChartData = data?.methodCounts
        ? Object.entries(data.methodCounts).map(([method, count]) => ({
            method,
            count,
        }))
        : [];

    const statusCodeChartData = data?.statusCodeCounts
        ? Object.entries(data.statusCodeCounts).map(([code, count]) => ({
            code,
            count,
        }))
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    접근 로그 통계
                </h1>
                <p className="text-secondary-600">
                    HTTP 메서드, 상태코드, 응답시간 분석
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
                                icon={<Search className="w-4 h-4"/>}
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
                    {loading && <Loading text="데이터를 불러오는 중..."/>}

                    {error && <ErrorMessage message={error} onRetry={handleSearch}/>}

                    {!loading && !error && data && (
                        <>
                            {/* 요약 카드 */}
                            <div className="grid md:grid-cols-3 gap-4 mb-6">
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">평균 응답시간</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {data.avgResponseTime?.toFixed(0) || 0}
                                        <span className="text-lg text-secondary-500 ml-1">ms</span>
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">총 요청 수</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {(Object.values(data.methodCounts || {}).reduce((a: any, b: any) => a + b, 0) as number)}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">에러율</p>
                                    <p className="text-3xl font-bold text-error">
                                        {(() => {
                                            const total = Object.values(data.statusCodeCounts || {}).reduce((a: any, b: any) => a + b, 0) as number;
                                            const errors = Object.entries(data.statusCodeCounts || {})
                                                .filter(([code]) => code.startsWith('5'))
                                                .reduce((sum, [, count]) => sum + (count as number), 0);
                                            return total > 0 ? ((errors / total) * 100).toFixed(2) : '0.00';
                                        })()}
                                        <span className="text-lg text-secondary-500 ml-1">%</span>
                                    </p>
                                </Card>
                            </div>

                            {/* HTTP 메서드 & 상태코드 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="HTTP 메서드별 요청 수">
                                    <ResponsiveContainer width="100%" height={250}>
                                        <PieChart>
                                            <Pie
                                                data={methodChartData}
                                                dataKey="count"
                                                nameKey="method"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={80}
                                                label
                                            >
                                                {methodChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]}/>
                                                ))}
                                            </Pie>
                                            <Tooltip/>
                                            <Legend/>
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>

                                <Card title="상태코드별 분포">
                                    <ResponsiveContainer width="100%" height={250}>
                                        <BarChart data={statusCodeChartData}>
                                            <CartesianGrid strokeDasharray="3 3"/>
                                            <XAxis dataKey="code"/>
                                            <YAxis/>
                                            <Tooltip/>
                                            <Bar dataKey="count" fill="#0ea5e9"/>
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Card>
                            </div>

                            {/* 시간대별 분포 */}
                            <Card title="시간대별 접근 로그 분포">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0"/>
                                        <XAxis dataKey="timestamp" stroke="#64748b" style={{fontSize: '12px'}}/>
                                        <YAxis yAxisId="left" stroke="#64748b" style={{fontSize: '12px'}}/>
                                        <YAxis yAxisId="right" orientation="right" stroke="#64748b"
                                               style={{fontSize: '12px'}}/>
                                        <Tooltip/>
                                        <Legend/>
                                        <Line
                                            yAxisId="left"
                                            type="monotone"
                                            dataKey="requestCount"
                                            stroke="#0ea5e9"
                                            strokeWidth={2}
                                            name="요청 수"
                                        />
                                        <Line
                                            yAxisId="right"
                                            type="monotone"
                                            dataKey="avgResponseTime"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            name="평균 응답시간 (ms)"
                                        />
                                        <Line
                                            yAxisId="left"
                                            type="monotone"
                                            dataKey="errorCount"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            name="에러 수"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>
                        </>
                    )}
                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <Activity className="w-16 h-16 text-secondary-300 mx-auto mb-4"/>
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