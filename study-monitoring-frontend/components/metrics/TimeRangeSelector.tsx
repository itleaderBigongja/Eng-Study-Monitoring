// components/metrics/TimeRangeSelector.tsx
import React from 'react';

export type TimeRange = '5m' | '1h' | '6h' | '24h';

interface TimeRangeSelectorProps {
    value: TimeRange;
    onChange: (range: TimeRange) => void;
    className?: string;
}

/**
 * 시간 범위 선택 컴포넌트
 *
 * 용도: 실시간 메트릭 페이지에서 표시할 시간 범위 선택
 * - 5m: 최근 5분
 * - 1h: 최근 1시간
 * - 6h: 최근 6시간
 * - 24h: 최근 24시간
 *
 * @param value - 현재 선택된 시간 범위
 * @param onChange - 시간 범위 변경 핸들러
 * @param className - 추가 CSS 클래스
 */
export default function TimeRangeSelector({
                                              value,
                                              onChange,
                                              className = ''
                                          }: TimeRangeSelectorProps) {
    const ranges: { value: TimeRange; label: string }[] = [
        { value: '5m', label: '최근 5분' },
        { value: '1h', label: '최근 1시간' },
        { value: '6h', label: '최근 6시간' },
        { value: '24h', label: '최근 24시간' },
    ];

    return (
        <select
            value={value}
            onChange={(e) => onChange(e.target.value as TimeRange)}
            className={`px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white text-gray-700 font-medium cursor-pointer hover:border-gray-400 transition-colors ${className}`}
        >
            {ranges.map((range) => (
                <option key={range.value} value={range.value}>
                    {range.label}
                </option>
            ))}
        </select>
    );
}

/**
 * 시간 범위를 초 단위로 변환
 *
 * @param range - 시간 범위 문자열
 * @returns 초 단위 시간
 */
export function getRangeInSeconds(range: TimeRange): number {
    switch (range) {
        case '5m':
            return 5 * 60;
        case '1h':
            return 60 * 60;
        case '6h':
            return 6 * 60 * 60;
        case '24h':
            return 24 * 60 * 60;
        default:
            return 5 * 60;
    }
}

/**
 * 시간 범위에 따른 데이터 포인트 수 계산
 *
 * @param range - 시간 범위 문자열
 * @returns 데이터 포인트 수
 */
export function getDataPointsCount(range: TimeRange): number {
    switch (range) {
        case '5m':
            return 20; // 15초마다 1개
        case '1h':
            return 60; // 1분마다 1개
        case '6h':
            return 120; // 3분마다 1개
        case '24h':
            return 144; // 10분마다 1개
        default:
            return 20;
    }
}