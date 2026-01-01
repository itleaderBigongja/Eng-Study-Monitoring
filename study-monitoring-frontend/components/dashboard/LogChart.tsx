// 경로 : /Monitering/study-monitoring-frontend/components/dashboard/LogChart.tsx
import { LineChart, Line, AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import Card from '@/components/common/Card';

interface LogChartData {
    timestamp: string;
    info?: number;
    warn?: number;
    error?: number;
    debug?: number;
    total?: number;
}

interface LogChartProps {
    data: LogChartData[];
    title?: string;
    chartType?: 'line' | 'area' | 'bar';
    height?: number;
}

export default function LogChart({
                                     data,
                                     title = '로그 발생 추이',
                                     chartType = 'area',
                                     height = 300
                                 }: LogChartProps) {
    // 차트 색상 정의
    const colors = {
        info: '#0ea5e9',
        warn: '#f59e0b',
        error: '#ef4444',
        debug: '#8b5cf6',
        total: '#10b981',
    };

    // 커스텀 툴팁
    const CustomTooltip = ({ active, payload, label }: any) => {
        if (active && payload && payload.length) {
            return (
                <div className="bg-white p-3 rounded-lg shadow-lg border border-primary-100">
                    <p className="text-sm font-semibold text-gray-900 mb-2">{label}</p>
                    {payload.map((entry: any, index: number) => (
                        <div key={index} className="flex items-center justify-between space-x-4">
                            <div className="flex items-center space-x-2">
                                <div
                                    className="w-3 h-3 rounded-full"
                                    style={{ backgroundColor: entry.color }}
                                />
                                <span className="text-xs font-medium text-gray-700 uppercase">
                                    {entry.name}
                                </span>
                            </div>
                            <span className="text-sm font-bold text-gray-900">
                                {entry.value}
                            </span>
                        </div>
                    ))}
                </div>
            );
        }
        return null;
    };

    // 라인 차트 렌더링
    const renderLineChart = () => (
        <ResponsiveContainer width="100%" height={height}>
            <LineChart data={data}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis
                    dataKey="timestamp"
                    stroke="#64748b"
                    style={{ fontSize: '12px' }}
                    angle={-45}
                    textAnchor="end"
                    height={60}
                />
                <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                <Tooltip content={<CustomTooltip />} />
                <Legend
                    wrapperStyle={{ paddingTop: '10px' }}
                    iconType="circle"
                />

                {data[0]?.info !== undefined && (
                    <Line
                        type="monotone"
                        dataKey="info"
                        stroke={colors.info}
                        strokeWidth={2}
                        dot={{ fill: colors.info, r: 3 }}
                        name="INFO"
                    />
                )}
                {data[0]?.warn !== undefined && (
                    <Line
                        type="monotone"
                        dataKey="warn"
                        stroke={colors.warn}
                        strokeWidth={2}
                        dot={{ fill: colors.warn, r: 3 }}
                        name="WARN"
                    />
                )}
                {data[0]?.error !== undefined && (
                    <Line
                        type="monotone"
                        dataKey="error"
                        stroke={colors.error}
                        strokeWidth={2}
                        dot={{ fill: colors.error, r: 3 }}
                        name="ERROR"
                    />
                )}
                {data[0]?.debug !== undefined && (
                    <Line
                        type="monotone"
                        dataKey="debug"
                        stroke={colors.debug}
                        strokeWidth={2}
                        dot={{ fill: colors.debug, r: 3 }}
                        name="DEBUG"
                    />
                )}
                {data[0]?.total !== undefined && (
                    <Line
                        type="monotone"
                        dataKey="total"
                        stroke={colors.total}
                        strokeWidth={3}
                        dot={{ fill: colors.total, r: 4 }}
                        name="TOTAL"
                    />
                )}
            </LineChart>
        </ResponsiveContainer>
    );

    // 영역 차트 렌더링
    const renderAreaChart = () => (
        <ResponsiveContainer width="100%" height={height}>
            <AreaChart data={data}>
                <defs>
                    <linearGradient id="colorInfo" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={colors.info} stopOpacity={0.8}/>
                        <stop offset="95%" stopColor={colors.info} stopOpacity={0.1}/>
                    </linearGradient>
                    <linearGradient id="colorWarn" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={colors.warn} stopOpacity={0.8}/>
                        <stop offset="95%" stopColor={colors.warn} stopOpacity={0.1}/>
                    </linearGradient>
                    <linearGradient id="colorError" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={colors.error} stopOpacity={0.8}/>
                        <stop offset="95%" stopColor={colors.error} stopOpacity={0.1}/>
                    </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis
                    dataKey="timestamp"
                    stroke="#64748b"
                    style={{ fontSize: '12px' }}
                    angle={-45}
                    textAnchor="end"
                    height={60}
                />
                <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                <Tooltip content={<CustomTooltip />} />
                <Legend
                    wrapperStyle={{ paddingTop: '10px' }}
                    iconType="circle"
                />

                {data[0]?.info !== undefined && (
                    <Area
                        type="monotone"
                        dataKey="info"
                        stroke={colors.info}
                        fill="url(#colorInfo)"
                        name="INFO"
                    />
                )}
                {data[0]?.warn !== undefined && (
                    <Area
                        type="monotone"
                        dataKey="warn"
                        stroke={colors.warn}
                        fill="url(#colorWarn)"
                        name="WARN"
                    />
                )}
                {data[0]?.error !== undefined && (
                    <Area
                        type="monotone"
                        dataKey="error"
                        stroke={colors.error}
                        fill="url(#colorError)"
                        name="ERROR"
                    />
                )}
            </AreaChart>
        </ResponsiveContainer>
    );

    // 바 차트 렌더링
    const renderBarChart = () => (
        <ResponsiveContainer width="100%" height={height}>
            <BarChart data={data}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis
                    dataKey="timestamp"
                    stroke="#64748b"
                    style={{ fontSize: '12px' }}
                    angle={-45}
                    textAnchor="end"
                    height={60}
                />
                <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                <Tooltip content={<CustomTooltip />} />
                <Legend
                    wrapperStyle={{ paddingTop: '10px' }}
                    iconType="circle"
                />

                {data[0]?.info !== undefined && (
                    <Bar dataKey="info" fill={colors.info} name="INFO" />
                )}
                {data[0]?.warn !== undefined && (
                    <Bar dataKey="warn" fill={colors.warn} name="WARN" />
                )}
                {data[0]?.error !== undefined && (
                    <Bar dataKey="error" fill={colors.error} name="ERROR" />
                )}
                {data[0]?.debug !== undefined && (
                    <Bar dataKey="debug" fill={colors.debug} name="DEBUG" />
                )}
            </BarChart>
        </ResponsiveContainer>
    );

    // 차트 타입에 따라 렌더링
    const renderChart = () => {
        switch (chartType) {
            case 'line':
                return renderLineChart();
            case 'area':
                return renderAreaChart();
            case 'bar':
                return renderBarChart();
            default:
                return renderAreaChart();
        }
    };

    return (
        <Card title={title}>
            {data.length === 0 ? (
                <div className="flex items-center justify-center" style={{ height }}>
                    <p className="text-secondary-500">표시할 데이터가 없습니다</p>
                </div>
            ) : (
                renderChart()
            )}
        </Card>
    );
}