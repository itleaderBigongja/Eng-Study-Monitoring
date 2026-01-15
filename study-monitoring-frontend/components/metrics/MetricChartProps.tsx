// components/metrics/MetricChart.tsx
import React from 'react';
import Card from '@/components/common/Card';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';

interface MetricChartProps {
    title: string;
    data: any[];
    dataKey: string;
    color: string;
    name: string;
}

/**
 * 메트릭 차트 컴포넌트 (시계열 통계 스타일 - 3선 표현)
 *
 * 용도: TPS, Heap, CPU, Error Rate의 시간별 추이를 3개 선으로 표시
 * - 최소값 (Min) - 점선
 * - 현재값 (Current) - 굵은 실선
 * - 최대값 (Max) - 점선
 *
 * @param title - 차트 제목
 * @param data - 시계열 데이터 배열
 * @param dataKey - 데이터 키 (tps, heapUsage, cpuUsage, errorRate)
 * @param color - 선 색상
 * @param name - 범례 이름
 */
export default function MetricChart({
                                        title,
                                        data,
                                        dataKey,
                                        color,
                                        name
                                    }: MetricChartProps) {
    // ✅ [신규] 최소, 현재, 최대 값 계산
    const enrichedData = React.useMemo(() => {
        if (!data || data.length === 0) return [];

        // 전체 데이터에서 최소/최대 계산
        const values = data.map(d => d[dataKey]);
        const min = Math.min(...values);
        const max = Math.max(...values);

        // 각 데이터 포인트에 min, max 추가
        return data.map(d => ({
            ...d,
            [`${dataKey}_min`]: min,
            [`${dataKey}_max`]: max,
            [`${dataKey}_current`]: d[dataKey]
        }));
    }, [data, dataKey]);

    // 색상 밝기 조정 (최소/최대는 연하게)
    const lightColor = adjustColorOpacity(color, 0.3);

    return (
        <Card title={title}>
            <ResponsiveContainer width="100%" height={250}>
                <LineChart data={enrichedData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis
                        dataKey="timestamp"
                        stroke="#64748b"
                        style={{ fontSize: '11px' }}
                    />
                    <YAxis
                        stroke="#64748b"
                        style={{ fontSize: '12px' }}
                        domain={[0, 'auto']}
                    />
                    <Tooltip
                        contentStyle={{
                            borderRadius: '8px',
                            border: 'none',
                            boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)'
                        }}
                        formatter={(value: any) => typeof value === 'number' ? value.toFixed(2) : value}
                    />
                    <Legend />

                    {/* ✅ [신규] 최소값 선 (점선, 연한 색상) */}
                    <Line
                        type="monotone"
                        dataKey={`${dataKey}_min`}
                        stroke={lightColor}
                        strokeWidth={1}
                        strokeDasharray="5 5"
                        dot={false}
                        name={`${name} (최소)`}
                        isAnimationActive={false}
                    />

                    {/* ✅ [신규] 현재값 선 (실선, 굵게) */}
                    <Line
                        type="monotone"
                        dataKey={`${dataKey}_current`}
                        stroke={color}
                        strokeWidth={2.5}
                        dot={{ fill: color, r: 3 }}
                        activeDot={{ r: 6 }}
                        name={`${name} (현재)`}
                        isAnimationActive={false}
                    />

                    {/* ✅ [신규] 최대값 선 (점선, 연한 색상) */}
                    <Line
                        type="monotone"
                        dataKey={`${dataKey}_max`}
                        stroke={lightColor}
                        strokeWidth={1}
                        strokeDasharray="5 5"
                        dot={false}
                        name={`${name} (최대)`}
                        isAnimationActive={false}
                    />
                </LineChart>
            </ResponsiveContainer>
        </Card>
    );
}

/**
 * 색상 투명도 조정 헬퍼 함수
 *
 * @param hexColor - HEX 색상 코드 (#3b82f6)
 * @param opacity - 투명도 (0.0 ~ 1.0)
 * @returns rgba 색상 문자열
 */
function adjustColorOpacity(hexColor: string, opacity: number): string {
    // HEX를 RGB로 변환
    const hex = hexColor.replace('#', '');
    const r = parseInt(hex.substring(0, 2), 16);
    const g = parseInt(hex.substring(2, 4), 16);
    const b = parseInt(hex.substring(4, 6), 16);

    return `rgba(${r}, ${g}, ${b}, ${opacity})`;
}