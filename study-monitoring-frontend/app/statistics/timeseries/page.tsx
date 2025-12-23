'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getTimeSeriesStatistics } from '@/lib/api/statistics';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Search, Download } from 'lucide-react';

export default function TimeSeriesStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    // 쿼리 파라미터
    const [metricType, setMetricType] = useState('TPS');
    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [aggregationType, setAggregationType] = useState('AVG');
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
            const result = await getTimeSeriesStatistics({
                metricType,
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
                aggregationType,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || '통계 데이터를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    // CSV 다운로드
    const handleDownloadCSV = () => {
        if (!data?.data) return;

        const csvContent = [
            ['Timestamp', 'Value', 'Min', 'Max', 'Sample Count'].join(','),
            ...data.data.map((item: any) =>
                [item.timestamp, item.value, item.minValue, item.maxValue, item.sampleCount].join(',')
            ),
        ].join('\n');

        const blob = new Blob([csvContent], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `timeseries_${metricType}_${Date.now()}.csv`;
        a.click();
    };

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    시계열 통계
                </h1>
                <p className="text-secondary-600">
                    Prometheus + PostgreSQL 기반 시계열 데이터 분석
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                {/* 검색 조건 */}
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            {/* 메트릭 타입 */}
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    메트릭 타입
                                </label>
                                <select
                                    value={metricType}
                                    onChange={(e) => setMetricType(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="TPS">TPS</option>
                                    <option value="HEAP_USAGE">Heap 사용률</option>
                                    <option value="ERROR_RATE">에러율</option>
                                    <option value="CPU_USAGE">CPU 사용률</option>
                                </select>
                            </div>

                            {/* 시간 주기 */}
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
                                    <option value="WEEK">주</option>
                                    <option value="MONTH">월</option>
                                </select>
                            </div>

                            {/* 집계 방식 */}
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    집계 방식
                                </label>
                                <select
                                    value={aggregationType}
                                    onChange={(e) => setAggregationType(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="AVG">평균</option>
                                    <option value="SUM">합계</option>
                                    <option value="MIN">최소</option>
                                    <option value="MAX">최대</option>
                                    <option value="COUNT">카운트</option>
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

                    {/* 날짜 범위 선택 */}
                    <div className="mt-6">
                        <DateRangePicker
                            startDate={startTime}
                            endDate={endTime}
                            onChange={handleDateRangeChange}
                        />
                    </div>
                </div>

                {/* 결과 표시 */}
                <div className="lg:col-span-2">
                    {loading && <Loading text="데이터를 불러오는 중..." />}

                    {error && <ErrorMessage message={error} onRetry={handleSearch} />}

                    {!loading && !error && data && (
                        <>
                            {/* 요약 정보 */}
                            <Card
                                title="요약 정보"
                                headerAction={
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        icon={<Download className="w-4 h-4" />}
                                        onClick={handleDownloadCSV}
                                    >
                                        CSV 다운로드
                                    </Button>
                                }
                            >
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                    <div>
                                        <p className="text-sm text-secondary-600">메트릭</p>
                                        <p className="text-lg font-semibold text-primary-700">{data.metricType}</p>
                                    </div>
                                    <div>
                                        <p className="text-sm text-secondary-600">주기</p>
                                        <p className="text-lg font-semibold text-primary-700">{data.timePeriod}</p>
                                    </div>
                                    <div>
                                        <p className="text-sm text-secondary-600">집계</p>
                                        <p className="text-lg font-semibold text-primary-700">{data.aggregationType}</p>
                                    </div>
                                    <div>
                                        <p className="text-sm text-secondary-600">데이터 소스</p>
                                        <p className="text-lg font-semibold text-primary-700">{data.dataSource}</p>
                                    </div>
                                </div>
                            </Card>

                            {/* 차트 */}
                            <Card title="시계열 차트" className="mt-6">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={data.data}>
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
                                            dataKey="value"
                                            stroke="#0ea5e9"
                                            strokeWidth={2}
                                            dot={{ fill: '#0ea5e9', r: 4 }}
                                            activeDot={{ r: 6 }}
                                            name="값"
                                        />
                                        {data.data[0]?.minValue !== null && (
                                            <Line
                                                type="monotone"
                                                dataKey="minValue"
                                                stroke="#10b981"
                                                strokeWidth={1}
                                                strokeDasharray="5 5"
                                                dot={false}
                                                name="최소"
                                            />
                                        )}
                                        {data.data[0]?.maxValue !== null && (
                                            <Line
                                                type="monotone"
                                                dataKey="maxValue"
                                                stroke="#ef4444"
                                                strokeWidth={1}
                                                strokeDasharray="5 5"
                                                dot={false}
                                                name="최대"
                                            />
                                        )}
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            {/* 데이터 테이블 */}
                            <Card title="상세 데이터" className="mt-6">
                                <div className="overflow-x-auto">
                                    <table className="min-w-full divide-y divide-gray-200">
                                        <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                시간
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                값
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                최소
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                최대
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                샘플 수
                                            </th>
                                        </tr>
                                        </thead>
                                        <tbody className="bg-white divide-y divide-gray-200">
                                        {data.data.slice(0, 10).map((item: any, index: number) => (
                                            <tr key={index}>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                                    {item.timestamp}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-primary-700">
                                                    {item.value.toFixed(2)}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                    {item.minValue?.toFixed(2) || '-'}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                    {item.maxValue?.toFixed(2) || '-'}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                    {item.sampleCount || '-'}
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                    {data.data.length > 10 && (
                                        <p className="text-sm text-secondary-600 text-center py-4">
                                            총 {data.data.length}개 중 10개 표시 (CSV 다운로드로 전체 데이터 확인)
                                        </p>
                                    )}
                                </div>
                            </Card>
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
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