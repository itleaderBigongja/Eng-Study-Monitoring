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

    // ✅ 한국 시간(KST) 기준으로 datetime-local 형식 반환
    const getKSTDateTime = (date: Date): string => {
        // 1. 한국 시간대(Asia/Seoul)로 변환
        const kstDate = new Date(date.toLocaleString('en-US', { timeZone: 'Asia/Seoul' }));

        // 2. YYYY-MM-DDTHH:mm 형식으로 변환
        const year = kstDate.getFullYear();
        const month = String(kstDate.getMonth() + 1).padStart(2, '0');
        const day = String(kstDate.getDate()).padStart(2, '0');
        const hours = String(kstDate.getHours()).padStart(2, '0');
        const minutes = String(kstDate.getMinutes()).padStart(2, '0');

        return `${year}-${month}-${day}T${hours}:${minutes}`;
    };

    // 빠른 선택 옵션
    const quickOptions = [
        { label: '최근 1시간', hours: 1 },
        { label: '최근 6시간', hours: 6 },
        { label: '최근 24시간', hours: 24 },
        { label: '최근 7일', hours: 24 * 7 },
        { label: '최근 30일', hours: 24 * 30 },
    ];

    // ✅ 한국 시간 기준으로 빠른 선택 처리
    const handleQuickSelect = (hours: number) => {
        const now = new Date();
        const past = new Date(now);
        past.setHours(now.getHours() - hours);

        const startKST = getKSTDateTime(past);
        const endKST = getKSTDateTime(now);

        setStart(startKST);
        setEnd(endKST);

        // 자동으로 적용
        onChange(startKST, endKST);
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
                        className="px-3 py-1.5 text-sm bg-primary-50 hover:bg-primary-100 text-primary-700 rounded-lg transition-colors font-medium"
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
                        className="input-field w-full"
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
                        className="input-field w-full"
                    />
                </div>

                <button onClick={handleApply} className="btn-primary w-full">
                    적용
                </button>
            </div>
        </div>
    );
}