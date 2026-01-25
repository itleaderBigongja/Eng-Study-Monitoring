'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getTimeSeriesStatistics } from '@/lib/api/statistics';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Search, Download, Server } from 'lucide-react';

const APP_METRICS: Record<string, { value: string; label: string }[]> = {
    'eng-study': [
        { value: 'TPS', label: 'TPS (Transactions Per Sec)' },
        { value: 'HEAP_USAGE', label: 'Heap Memory (%)' },
        { value: 'ERROR_RATE', label: 'Error Rate (%)' },
        { value: 'CPU_USAGE', label: 'CPU Usage (%)' },
    ],
    'monitoring': [
        { value: 'TPS', label: 'TPS (Transactions Per Sec)' },
        { value: 'HEAP_USAGE', label: 'Heap Memory (%)' },
        { value: 'ERROR_RATE', label: 'Error Rate (%)' },
        { value: 'CPU_USAGE', label: 'CPU Usage (%)' },
    ],
    'postgres': [
        { value: 'DB_CONNECTIONS', label: 'Active Connections (개)' },
        { value: 'DB_SIZE', label: 'Database Size (Bytes)' },
        { value: 'DB_TRANSACTIONS', label: 'Transactions / Sec' },
    ],
    'elasticsearch': [
        { value: 'ES_JVM_HEAP', label: 'ES JVM Heap (%)' },
        { value: 'ES_DATA_SIZE', label: 'Data/Index Size (Bytes)' },
        { value: 'ES_CPU', label: 'Node CPU Usage (%)' },
    ]
};

export default function TimeSeriesStatisticsPage() {
    const getKSTDateTime = (date: Date): string => {
        const kstDate = new Date(date.toLocaleString('en-US', { timeZone: 'Asia/Seoul' }));
        const year = kstDate.getFullYear();
        const month = String(kstDate.getMonth() + 1).padStart(2, '0');
        const day = String(kstDate.getDate()).padStart(2, '0');
        const hours = String(kstDate.getHours()).padStart(2, '0');
        const minutes = String(kstDate.getMinutes()).padStart(2, '0');
        return `${year}-${month}-${day}T${hours}:${minutes}`;
    };

    const convertToBackendFormat = (datetimeLocal: string): string => {
        return datetimeLocal.replace('T', ' ') + ':00';
    };

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any[]>([]); // ✅ 초기값을 빈 배열로 확실하게 설정

    const [application, setApplication] = useState('eng-study');
    const [metricType, setMetricType] = useState('TPS');
    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [aggregationType, setAggregationType] = useState('AVG');

    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return getKSTDateTime(date);
    });

    const [endTime, setEndTime] = useState(() => {
        return getKSTDateTime(new Date());
    });

    const handleAppChange = (newApp: string) => {
        setApplication(newApp);
        if (APP_METRICS[newApp] && APP_METRICS[newApp].length > 0) {
            setMetricType(APP_METRICS[newApp][0].value);
        }
    };

    // ✅ [수정됨] 데이터 조회 및 파싱 함수
    const handleSearch = async () => {
        setLoading(true);
        setError(null);
        setData([]); // 기존 데이터 초기화

        try {
            // client.ts에서 이미 .data(알맹이)를 꺼내서 줍니다.
            // 즉, result 변수에는 { success: true, data: [...] } 가 아니라
            // 바로 [...] (배열) 이거나 { content: [...] } (페이지 객체)가 들어옵니다.
            const result: any = await getTimeSeriesStatistics({
                application,
                metricType,
                startTime: convertToBackendFormat(startTime),
                endTime: convertToBackendFormat(endTime),
                timePeriod,
                aggregationType,
            });

            console.log("API Result (Unwrapped):", result); // 로그 확인

            // 1. result 자체가 배열인 경우 (List 반환 시)
            if (Array.isArray(result)) {
                setData(result);
            }
            // 2. result.content가 배열인 경우 (Page 객체 반환 시)
            else if (result && Array.isArray(result.content)) {
                setData(result.content);
            }
            // 3. 혹시 모를 경우 (result.data가 또 있는 경우 - client.ts 수정 전 대비)
            else if (result && Array.isArray(result.data)) {
                setData(result.data);
            }
            else {
                console.warn("데이터 형식을 알 수 없습니다.", result);
                setData([]);
            }

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

    const handleDownloadCSV = () => {
        const safeData = getSafeData();
        if (safeData.length === 0) {
            alert("다운로드할 데이터가 없습니다.");
            return;
        }

        const csvContent = [
            ['Timestamp', 'Value', 'Min', 'Max', 'Sample Count'].join(','),
            ...safeData.map((item: any) =>
                [item.timestamp, item.value ?? 0, item.minValue ?? 0, item.maxValue ?? 0, item.sampleCount ?? 0].join(',')
            ),
        ].join('\n');

        const blob = new Blob([csvContent], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `timeseries_${application}_${metricType}_${Date.now()}.csv`;
        a.click();
    };

    const formatBytes = (bytes: number) => {
        if (bytes === 0) return '0 B';
        if (!bytes) return '-';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    const formatMetricValue = (value: number) => {
        if (value === null || value === undefined) return '-';
        if (metricType === 'DB_SIZE' || metricType === 'ES_DATA_SIZE') {
            return formatBytes(value);
        }
        if (metricType.includes('USAGE') || metricType.includes('RATE') || metricType.includes('HEAP') || metricType.includes('CPU')) {
            return `${value.toFixed(2)}%`;
        }
        return value.toLocaleString(undefined, { maximumFractionDigits: 2 });
    };

    // ✅ 안전하게 배열 데이터를 가져오는 헬퍼 함수
    const getSafeData = () => {
        return Array.isArray(data) ? data : [];
    };

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <div className="flex items-center gap-3 mb-2">
                    <h1 className="text-3xl font-bold text-primary-700">시계열 통계</h1>
                    <span className="px-3 py-1 bg-blue-100 text-blue-800 text-xs font-semibold rounded-full border border-blue-200">
                        {application}
                    </span>
                </div>
                <p className="text-secondary-600">Prometheus + PostgreSQL 기반 시계열 데이터 분석</p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2 flex items-center gap-2">
                                    <Server className="w-4 h-4 text-gray-500" />
                                    애플리케이션
                                </label>
                                <select
                                    value={application}
                                    onChange={(e) => handleAppChange(e.target.value)}
                                    className="input-field w-full p-2 border border-gray-300 rounded-md"
                                >
                                    <option value="eng-study">Eng-Study (Main App)</option>
                                    <option value="monitoring">Monitoring System</option>
                                    <option value="elasticsearch">Elasticsearch</option>
                                    <option value="postgres">PostgreSQL</option>
                                </select>
                            </div>
                            <hr className="border-gray-100 my-2"/>
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">메트릭 타입</label>
                                <select value={metricType} onChange={(e) => setMetricType(e.target.value)} className="input-field w-full p-2 border border-gray-300 rounded-md">
                                    {APP_METRICS[application]?.map((opt) => (
                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">시간 주기</label>
                                <select value={timePeriod} onChange={(e) => setTimePeriod(e.target.value)} className="input-field w-full p-2 border border-gray-300 rounded-md">
                                    <option value="MINUTE">분 (Minute)</option>
                                    <option value="HOUR">시간 (Hour)</option>
                                    <option value="DAY">일 (Day)</option>
                                    <option value="WEEK">주 (Week)</option>
                                    <option value="MONTH">월 (Month)</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">집계 방식</label>
                                <select value={aggregationType} onChange={(e) => setAggregationType(e.target.value)} className="input-field w-full p-2 border border-gray-300 rounded-md">
                                    <option value="AVG">평균 (Average)</option>
                                    <option value="SUM">합계 (Sum)</option>
                                    <option value="MIN">최소 (Min)</option>
                                    <option value="MAX">최대 (Max)</option>
                                    <option value="COUNT">샘플 수 (Count)</option>
                                </select>
                            </div>
                            <Button variant="primary" icon={<Search className="w-4 h-4" />} onClick={handleSearch} loading={loading} className="w-full mt-4">
                                조회하기
                            </Button>
                        </div>
                    </Card>

                    <div className="mt-6">
                        <label className="block text-sm font-bold text-gray-700 mb-2">조회 기간 설정</label>
                        <DateRangePicker startDate={startTime} endDate={endTime} onChange={handleDateRangeChange} />
                    </div>
                </div>

                <div className="lg:col-span-2">
                    {loading && <Loading text={`${application} 데이터 분석 중...`} />}
                    {error && <ErrorMessage message={error} onRetry={handleSearch} />}

                    {!loading && !error && getSafeData().length > 0 && (
                        <>
                            <Card title="분석 결과 요약" headerAction={<Button variant="outline" size="sm" icon={<Download className="w-4 h-4" />} onClick={handleDownloadCSV}>CSV</Button>}>
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                    <div className="bg-gray-50 p-3 rounded-lg">
                                        <p className="text-xs text-gray-500 mb-1">Target App</p>
                                        <p className="text-sm font-bold text-blue-700 truncate">{application}</p>
                                    </div>
                                    <div className="bg-gray-50 p-3 rounded-lg">
                                        <p className="text-xs text-gray-500 mb-1">Metric</p>
                                        <p className="text-sm font-bold text-gray-800">{metricType}</p>
                                    </div>
                                    <div className="bg-gray-50 p-3 rounded-lg">
                                        <p className="text-xs text-gray-500 mb-1">Period</p>
                                        <p className="text-sm font-bold text-gray-800">{timePeriod}</p>
                                    </div>
                                    <div className="bg-gray-50 p-3 rounded-lg">
                                        <p className="text-xs text-gray-500 mb-1">Aggregation</p>
                                        <p className="text-sm font-bold text-gray-800">{aggregationType}</p>
                                    </div>
                                </div>
                            </Card>

                            <Card title={`${application} - ${metricType} Trend`} className="mt-6">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={getSafeData()} margin={{ top: 10, right: 30, left: 20, bottom: 0 }}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" vertical={false} />
                                        {/* ✅ [수정됨] X축 시간 포맷팅 강화 */}
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#9ca3af"
                                            style={{ fontSize: '11px' }}
                                            tickMargin={10}
                                            tickFormatter={(value) => {
                                                if (!value) return '';
                                                return value.length > 16 ? value.substring(5, 16) : value;
                                            }}
                                        />
                                        <YAxis
                                            stroke="#9ca3af"
                                            style={{ fontSize: '11px' }}
                                            width={60}
                                            tickFormatter={formatMetricValue}
                                        />
                                        <Tooltip
                                            contentStyle={{ backgroundColor: 'rgba(255, 255, 255, 0.95)', border: 'none', borderRadius: '8px', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' }}
                                            labelStyle={{ color: '#6b7280', marginBottom: '0.5rem' }}
                                            formatter={(value: number) => [formatMetricValue(value), aggregationType]}
                                        />
                                        <Legend wrapperStyle={{ paddingTop: '20px' }} />
                                        <Line type="monotone" dataKey="value" stroke="#3b82f6" strokeWidth={3} dot={{ fill: '#3b82f6', r: 3, strokeWidth: 0 }} activeDot={{ r: 6, stroke: '#fff', strokeWidth: 2 }} name={`${aggregationType} 값`} animationDuration={1000} />
                                        <Line type="monotone" dataKey="minValue" stroke="#10b981" strokeWidth={1} strokeDasharray="4 4" dot={false} name="Min" opacity={0.5} />
                                        <Line type="monotone" dataKey="maxValue" stroke="#ef4444" strokeWidth={1} strokeDasharray="4 4" dot={false} name="Max" opacity={0.5} />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            <Card title="Raw Data View" className="mt-6">
                                <div className="overflow-x-auto max-h-[400px] overflow-y-auto custom-scrollbar">
                                    <table className="min-w-full divide-y divide-gray-200">
                                        <thead className="bg-gray-50 sticky top-0 z-10">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Timestamp</th>
                                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Value</th>
                                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Min / Max</th>
                                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Samples</th>
                                        </tr>
                                        </thead>
                                        {/* ✅ [수정됨] 안전한 데이터 렌더링 */}
                                        <tbody className="bg-white divide-y divide-gray-200">
                                        {getSafeData().map((item: any, index: number) => (
                                            <tr key={index} className="hover:bg-gray-50 transition-colors">
                                                <td className="px-6 py-3 whitespace-nowrap text-xs text-gray-600 font-mono">
                                                    {item.timestamp}
                                                </td>
                                                <td className="px-6 py-3 whitespace-nowrap text-sm font-bold text-primary-700 text-right font-mono">
                                                    {formatMetricValue(item.value)}
                                                </td>
                                                <td className="px-6 py-3 whitespace-nowrap text-xs text-gray-500 text-right font-mono">
                                                    <span className="text-green-600 mr-2">
                                                        ↓{formatMetricValue(item.minValue)}
                                                    </span>
                                                    <span className="text-red-600">
                                                        ↑{formatMetricValue(item.maxValue)}
                                                    </span>
                                                </td>
                                                <td className="px-6 py-3 whitespace-nowrap text-xs text-gray-500 text-right">
                                                    {item.sampleCount || 1}
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                                <div className="bg-gray-50 px-4 py-2 text-right border-t border-gray-100">
                                    <span className="text-xs text-gray-500">Total Records: {getSafeData().length}</span>
                                </div>
                            </Card>
                        </>
                    )}

                    {!loading && !error && getSafeData().length === 0 && (
                        <Card className="h-full flex flex-col justify-center min-h-[400px]">
                            <div className="text-center py-12">
                                <div className="bg-blue-50 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <Search className="w-8 h-8 text-blue-500" />
                                </div>
                                <h3 className="text-lg font-medium text-gray-900 mb-2">데이터가 없습니다</h3>
                                <p className="text-secondary-600 max-w-sm mx-auto">
                                    해당 기간 또는 조건에 맞는 데이터가 존재하지 않습니다.<br/>
                                    좌측의 검색 조건을 변경하고 <strong>조회하기</strong> 버튼을 눌러보세요.
                                </p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
}