'use client';

import React, { useState } from 'react';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
} from 'recharts';

// =====================
// 타입 정의
// =====================

type MetricType = 'tps' | 'heap' | 'response' | 'error';

type MetricInfo = {
    id: MetricType;
    name: string;
    unit: string;
    color: string;
};

type ChartData = {
    time: string;
    value: number;
};

type StatCardProps = {
    title: string;
    value: number;
    unit: string;
    color: 'blue' | 'green' | 'red' | 'purple';
};

type ComparisonBarProps = {
    label: string;
    value: number;
    max: number;
    color: 'purple' | 'blue' | 'green' | 'yellow' | 'red';
};

// =====================
// 메인 페이지
// =====================

export default function MetricsPage() {
    const [selectedApp, setSelectedApp] = useState<string>('eng-study');
    const [selectedMetric, setSelectedMetric] = useState<MetricType>('tps');
    const [timeRange, setTimeRange] = useState<string>('1h');

    // 더미 데이터 생성
    const generateData = (metric: MetricType): ChartData[] => {
        return Array.from({ length: 60 }, (_, i) => {
            const base = metric === 'tps' ? 120 : metric === 'heap' ? 65 : metric === 'response' ? 85 : 0.1;
            const variance = metric === 'tps' ? 30 : metric === 'heap' ? 15 : metric === 'response' ? 20 : 0.05;

            return {
                time: `10:${String(i).padStart(2, '0')}`,
                value: base + (Math.random() - 0.5) * variance,
            };
        });
    };

    const apps = [
        { id: 'eng-study', name: 'eng-study', icon: '📚' },
        { id: 'monitoring', name: 'monitoring', icon: '📊' },
    ];

    const metrics: MetricInfo[] = [
        { id: 'tps', name: 'TPS (Transactions Per Second)', unit: 'req/s', color: '#a855f7' },
        { id: 'heap', name: 'Heap Memory Usage', unit: '%', color: '#3b82f6' },
        { id: 'response', name: 'Response Time (P95)', unit: 'ms', color: '#10b981' },
        { id: 'error', name: 'Error Rate', unit: '%', color: '#ef4444' },
    ];

    const timeRanges = [
        { id: '1h', label: '1시간' },
        { id: '6h', label: '6시간' },
        { id: '24h', label: '24시간' },
        { id: '7d', label: '7일' },
    ];

    const chartData = generateData(selectedMetric);
    const currentMetric = metrics.find(m => m.id === selectedMetric)!;

    return (
        <div className="min-h-screen bg-gradient-to-br from-purple-50 to-lavender-50 p-6">
            {/* 헤더 */}
            <div className="mb-8">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-600 to-purple-400 bg-clip-text text-transparent mb-2">
                    메트릭 분석
                </h1>
                <p className="text-gray-600">실시간 성능 메트릭 모니터링 및 분석</p>
            </div>

            {/* 필터 섹션 */}
            <div className="card mb-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {/* 애플리케이션 선택 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">애플리케이션</label>
                        <div className="flex gap-2">
                            {apps.map(app => (
                                <button
                                    key={app.id}
                                    onClick={() => setSelectedApp(app.id)}
                                    className={`flex-1 py-2 px-4 rounded-lg font-medium transition-all ${
                                        selectedApp === app.id
                                            ? 'bg-gradient-to-r from-purple-500 to-purple-600 text-white shadow-md'
                                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                                    }`}
                                >
                                    {app.icon} {app.name}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* 메트릭 선택 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">메트릭</label>
                        <select
                            value={selectedMetric}
                            onChange={(e) => setSelectedMetric(e.target.value as MetricType)}
                            className="w-full px-4 py-2 border-2 border-purple-200 rounded-lg focus:outline-none focus:border-purple-500 transition-colors"
                        >
                            {metrics.map(metric => (
                                <option key={metric.id} value={metric.id}>
                                    {metric.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* 시간 범위 선택 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">시간 범위</label>
                        <div className="grid grid-cols-4 gap-2">
                            {timeRanges.map(range => (
                                <button
                                    key={range.id}
                                    onClick={() => setTimeRange(range.id)}
                                    className={`py-2 px-3 rounded-lg font-medium text-sm transition-all ${
                                        timeRange === range.id
                                            ? 'bg-gradient-to-r from-purple-500 to-purple-600 text-white shadow-md'
                                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                                    }`}
                                >
                                    {range.label}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            {/* 메인 차트 */}
            <div className="card mb-6">
                <div className="flex items-center justify-between mb-6">
                    <div>
                        <h2 className="text-2xl font-bold text-gray-800">{currentMetric.name}</h2>
                        <p className="text-sm text-gray-600 mt-1">
                            {selectedApp} • {timeRanges.find(r => r.id === timeRange)?.label}
                        </p>
                    </div>
                </div>

                <ResponsiveContainer width="100%" height={400}>
                    <LineChart data={chartData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#e9d5ff" />
                        <XAxis dataKey="time" stroke="#9ca3af" tick={{ fontSize: 12 }} interval={Math.floor(chartData.length / 10)} />
                        <YAxis stroke="#9ca3af" tick={{ fontSize: 12 }} label={{ value: currentMetric.unit, angle: -90, position: 'insideLeft' }} />
                        <Tooltip
                            contentStyle={{ background: 'white', border: '1px solid #e9d5ff', borderRadius: '8px' }}
                            formatter={(value: number) => [`${value.toFixed(2)} ${currentMetric.unit}`, currentMetric.name]}
                        />
                        <Line type="monotone" dataKey="value" stroke={currentMetric.color} strokeWidth={2} dot={false} />
                    </LineChart>
                </ResponsiveContainer>
            </div>

            {/* 통계 요약 */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
                <StatCard title="평균" value={average(chartData)} unit={currentMetric.unit} color="blue" />
                <StatCard title="최소" value={Math.min(...chartData.map(d => d.value))} unit={currentMetric.unit} color="green" />
                <StatCard title="최대" value={Math.max(...chartData.map(d => d.value))} unit={currentMetric.unit} color="red" />
                <StatCard title="표준편차" value={stdDev(chartData.map(d => d.value))} unit={currentMetric.unit} color="purple" />
            </div>
        </div>
    );
}

// =====================
// 컴포넌트
// =====================

function StatCard({ title, value, unit, color }: StatCardProps) {
    return (
        <div className="card">
            <p className="text-sm text-gray-600 mb-2">{title}</p>
            <div className="flex items-baseline gap-1">
                <span className="text-3xl font-bold">{value.toFixed(2)}</span>
                <span className="text-sm text-gray-500">{unit}</span>
            </div>
        </div>
    );
}

function ComparisonBar({ label, value, max, color }: ComparisonBarProps) {
    const percentage = (value / max) * 100;
    return (
        <div>
            <div className="flex justify-between items-center mb-1">
                <span className="text-sm text-gray-700">{label}</span>
                <span className="text-sm font-medium text-gray-800">{value.toFixed(1)}</span>
            </div>
            <div className="bg-gray-200 rounded-full h-3">
                <div className="bg-blue-500 h-3 rounded-full" style={{ width: `${percentage}%` }} />
            </div>
        </div>
    );
}

// =====================
// 유틸
// =====================

function average(data: ChartData[]): number {
    return data.reduce((sum, d) => sum + d.value, 0) / data.length;
}

function stdDev(values: number[]): number {
    const avg = values.reduce((sum, v) => sum + v, 0) / values.length;
    const variance = values.reduce((sum, v) => sum + Math.pow(v - avg, 2), 0) / values.length;
    return Math.sqrt(variance);
}
