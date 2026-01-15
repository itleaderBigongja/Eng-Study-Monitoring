// components/metrics/MetricCard.tsx
import React from 'react';
import Card from '@/components/common/Card';

interface MetricCardProps {
    icon: React.ReactNode;
    title: string;
    value: string;
    unit: string;
    color: 'blue' | 'green' | 'red' | 'purple' | 'orange';
    warning?: boolean;
}

/**
 * 메트릭 카드 컴포넌트
 *
 * 용도: TPS, Heap, CPU, Error Rate 등의 현재 값을 카드 형태로 표시
 *
 * @param icon - 아이콘 (lucide-react)
 * @param title - 메트릭 이름
 * @param value - 현재 값 (포맷팅된 문자열)
 * @param unit - 단위 (%, req/s, MB 등)
 * @param color - 카드 색상 테마
 * @param warning - 임계치 초과 여부
 */
export default function MetricCard({
                                       icon,
                                       title,
                                       value,
                                       unit,
                                       color,
                                       warning = false
                                   }: MetricCardProps) {
    const colorClasses = {
        blue: 'from-blue-400 to-blue-600',
        green: 'from-green-400 to-green-600',
        red: 'from-red-400 to-red-600',
        purple: 'from-purple-400 to-purple-600',
        orange: 'from-orange-400 to-orange-600',
    };

    return (
        <Card className={warning ? 'border-2 border-warning animate-pulse' : ''}>
            <div className={`w-12 h-12 rounded-lg bg-gradient-to-br ${colorClasses[color]} flex items-center justify-center text-white mb-4 shadow-md`}>
                {icon}
            </div>
            <h3 className="text-sm font-medium text-secondary-600 mb-2">{title}</h3>
            <div className="flex items-baseline space-x-2">
                <span className="text-3xl font-bold text-primary-700">{value}</span>
                <span className="text-lg text-secondary-500">{unit}</span>
            </div>
            {warning && (
                <div className="mt-3 px-3 py-1 bg-warning/10 text-warning text-sm font-medium rounded inline-block">
                    ⚠️ 임계치 초과
                </div>
            )}
        </Card>
    );
}