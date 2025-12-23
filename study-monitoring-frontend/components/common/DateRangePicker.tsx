'use client';

import { useState } from 'react';
import { Calendar } from 'lucide-react';

interface DateRangePickerProps {
    startDate: string;
    endDate: string;
    onChange: (start: string, end: string) => void;
}

export default function DateRangePicker({
                                            startDate,
                                            endDate,
                                            onChange,
                                        }: DateRangePickerProps) {
    const [start, setStart] = useState(startDate);
    const [end, setEnd] = useState(endDate);

    const handleApply = () => {
        onChange(start, end);
    };

    // 빠른 선택 옵션
    const quickOptions = [
        { label: '최근 1시간', hours: 1 },
        { label: '최근 6시간', hours: 6 },
        { label: '최근 24시간', hours: 24 },
        { label: '최근 7일', hours: 24 * 7 },
        { label: '최근 30일', hours: 24 * 30 },
    ];

    const handleQuickSelect = (hours: number) => {
        const now = new Date();
        const past = new Date(now.getTime() - hours * 60 * 60 * 1000);

        const formatDate = (date: Date) => {
            return date.toISOString().slice(0, 16);
        };

        setStart(formatDate(past));
        setEnd(formatDate(now));
    };

    return (
        <div className="card">
            <div className="flex items-center space-x-2 mb-4">
                <Calendar className="w-5 h-5 text-primary-600" />
                <h3 className="font-semibold text-primary-700">기간 선택</h3>
            </div>

            {/* 빠른 선택 */}
            <div className="flex flex-wrap gap-2 mb-4">
                {quickOptions.map((option) => (
                    <button
                        key={option.label}
                        onClick={() => handleQuickSelect(option.hours)}
                        className="px-3 py-1.5 text-sm bg-primary-50 hover:bg-primary-100 text-primary-700 rounded-lg transition-colors"
                    >
                        {option.label}
                    </button>
                ))}
            </div>

            {/* 날짜 입력 */}
            <div className="space-y-4">
                <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                        시작 시간
                    </label>
                    <input
                        type="datetime-local"
                        value={start}
                        onChange={(e) => setStart(e.target.value)}
                        className="input-field"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                        종료 시간
                    </label>
                    <input
                        type="datetime-local"
                        value={end}
                        onChange={(e) => setEnd(e.target.value)}
                        className="input-field"
                    />
                </div>

                <button onClick={handleApply} className="btn-primary w-full">
                    적용
                </button>
            </div>
        </div>
    );
}