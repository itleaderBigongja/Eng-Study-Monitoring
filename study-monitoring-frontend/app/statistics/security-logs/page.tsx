'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getSecurityLogStatistics } from '@/lib/api/statistics';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
    ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell
} from 'recharts';
import { Search, Lock, AlertTriangle } from 'lucide-react';

const COLORS = ['#ef4444', '#f59e0b', '#10b981', '#0ea5e9', '#8b5cf6'];

export default function SecurityLogStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [threatLevel, setThreatLevel] = useState('');
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
            const result = await getSecurityLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
                threatLevel: threatLevel || undefined,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || '보안 로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    const threatLevelChartData = data?.threatLevelCounts
        ? Object.entries(data.threatLevelCounts).map(([level, count]) => ({
            level,
            count,
        }))
        : [];

    const attackTypeChartData = data?.attackTypeCounts
        ? Object.entries(data.attackTypeCounts).map(([type, count]) => ({
            type,
            count,
        }))
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    보안 로그 통계
                </h1>
                <p className="text-secondary-600">
                    위협 레벨, 공격 타입, 차단 통계 분석
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    위협 레벨 (선택)
                                </label>
                                <select
                                    value={threatLevel}
                                    onChange={(e) => setThreatLevel(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="">전체</option>
                                    <option value="low">Low</option>
                                    <option value="medium">Medium</option>
                                    <option value="high">High</option>
                                    <option value="critical">Critical</option>
                                </select>
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
                                    <p className="text-sm text-secondary-600 mb-1">총 보안 이벤트</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {(Object.values(data.threatLevelCounts || {}).reduce((a: any, b: any) => a + b, 0) as number)}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">차단된 시도</p>
                                    <p className="text-3xl font-bold text-error">
                                        {data.blockStats?.blockedAttacks || 0}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">Critical 위협</p>
                                    <p className="text-3xl font-bold text-error">
                                        {data.blockStats?.blockedAttacks || 0}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">차단율</p>
                                    <p className="text-3xl font-bold text-success">
                                        {(() => {
                                            const total = Object.values(data.threatLevelCounts || {})
                                                .reduce((a: any, b: any) => a + b, 0) as number;

                                            return total > 0
                                                ? (((data.blockedCount || 0) / total) * 100).toFixed(1)
                                                : '0.0';
                                        })()}
                                        <span className="text-lg text-secondary-500 ml-1">%</span>
                                    </p>
                                </Card>
                            </div>

                            {/* 위협 레벨 & 공격 타입 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="위협 레벨별 분포">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <PieChart>
                                            <Pie
                                                data={threatLevelChartData}
                                                dataKey="count"
                                                nameKey="level"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={100}
                                                label={(entry) => `${entry.level}: ${entry.count}`}
                                            >
                                                {threatLevelChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>

                                <Card title="공격 타입별 분포">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <BarChart data={attackTypeChartData} layout="vertical">
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis type="number" />
                                            <YAxis
                                                dataKey="type"
                                                type="category"
                                                width={120}
                                                style={{ fontSize: '11px' }}
                                            />
                                            <Tooltip />
                                            <Bar dataKey="count" fill="#ef4444" />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Card>
                            </div>

                            {/* 시간대별 추이 */}
                            <Card title="시간대별 보안 이벤트 추이">
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
                                        <Line
                                            type="monotone"
                                            dataKey="threatCount"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            name="위협 탐지"
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="blockedCount"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            name="차단된 시도"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            {/* 공격 소스 IP 통계 */}
                            {data.sourceIpCounts && Object.keys(data.sourceIpCounts).length > 0 && (
                                <Card title="공격 소스 IP Top 10" className="mt-6">
                                    <div className="overflow-x-auto">
                                        <table className="min-w-full divide-y divide-gray-200">
                                            <thead className="bg-gray-50">
                                            <tr>
                                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                    IP 주소
                                                </th>
                                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                    공격 시도
                                                </th>
                                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                    위험도
                                                </th>
                                            </tr>
                                            </thead>
                                            <tbody className="bg-white divide-y divide-gray-200">
                                            {Object.entries(data.sourceIpCounts)
                                                .sort((a: any, b: any) => b[1] - a[1])
                                                .slice(0, 10)
                                                .map(([ip, count]: any, index: number) => {
                                                    const riskLevel = count > 100 ? 'Critical' :
                                                        count > 50 ? 'High' :
                                                            count > 20 ? 'Medium' : 'Low';
                                                    const riskColor = count > 100 ? 'text-error' :
                                                        count > 50 ? 'text-warning' :
                                                            count > 20 ? 'text-yellow-600' : 'text-success';
                                                    return (
                                                        <tr key={index}>
                                                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                                                {ip}
                                                            </td>
                                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-error font-semibold">
                                                                {count}
                                                            </td>
                                                            <td className="px-6 py-4 whitespace-nowrap">
                                                                    <span className={`px-3 py-1 text-xs font-semibold rounded-full ${riskColor} bg-opacity-10`}>
                                                                        {riskLevel}
                                                                    </span>
                                                            </td>
                                                        </tr>
                                                    );
                                                })}
                                            </tbody>
                                        </table>
                                    </div>
                                </Card>
                            )}

                            {/* 위협 레벨별 상세 */}
                            <Card title="위협 레벨별 상세 정보" className="mt-6">
                                <div className="space-y-4">
                                    {threatLevelChartData.map((item: any, index: number) => {
                                        const total = threatLevelChartData.reduce((sum, i: any) => sum + i.count, 0);
                                        const percentage = ((item.count / total) * 100).toFixed(1);
                                        const levelColor =
                                            item.level === 'critical' ? 'bg-red-100 border-red-300' :
                                                item.level === 'high' ? 'bg-orange-100 border-orange-300' :
                                                    item.level === 'medium' ? 'bg-yellow-100 border-yellow-300' :
                                                        'bg-green-100 border-green-300';

                                        return (
                                            <div key={index} className={`p-4 rounded-lg border ${levelColor}`}>
                                                <div className="flex justify-between items-center">
                                                    <div className="flex items-center space-x-3">
                                                        <AlertTriangle className="w-5 h-5" />
                                                        <span className="font-semibold text-gray-900 uppercase">
                                                            {item.level}
                                                        </span>
                                                    </div>
                                                    <div className="text-right">
                                                        <p className="text-2xl font-bold text-gray-900">{item.count}</p>
                                                        <p className="text-sm text-gray-600">{percentage}%</p>
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </Card>
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <Lock className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
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