// components/query/QueryHistory.tsx
'use client';

import React from 'react';
import { Clock, Trash2, Play } from 'lucide-react';
import Card from '@/components/common/Card';

export interface QueryHistoryItem {
    id: string;
    query: string;
    timestamp: number;
    success: boolean;
    executionTime?: number; // ms
}

interface QueryHistoryProps {
    history: QueryHistoryItem[];
    onSelectQuery: (query: string) => void;
    onClearHistory: () => void;
}

/**
 * 쿼리 실행 히스토리 컴포넌트
 *
 * 기능:
 * - 실행한 쿼리 기록 표시
 * - 성공/실패 상태 표시
 * - 실행 시간 표시
 * - 클릭으로 재실행
 * - 전체 삭제
 *
 * @param history - 쿼리 히스토리 배열
 * @param onSelectQuery - 쿼리 선택 핸들러
 * @param onClearHistory - 히스토리 삭제 핸들러
 */
export default function QueryHistory({
                                         history,
                                         onSelectQuery,
                                         onClearHistory
                                     }: QueryHistoryProps) {
    const formatTime = (timestamp: number) => {
        const date = new Date(timestamp);
        return date.toLocaleString('ko-KR', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    };

    const formatExecutionTime = (ms?: number) => {
        if (!ms) return '-';
        if (ms < 1000) return `${ms.toFixed(0)}ms`;
        return `${(ms / 1000).toFixed(2)}s`;
    };

    if (history.length === 0) {
        return (
            <Card title="쿼리 히스토리">
                <div className="text-center py-8 text-gray-500">
                    <Clock className="w-12 h-12 mx-auto mb-3 opacity-50" />
                    <p className="text-sm">실행한 쿼리가 없습니다</p>
                    <p className="text-xs mt-1">쿼리를 실행하면 여기에 기록됩니다</p>
                </div>
            </Card>
        );
    }

    return (
        <Card
            title={`쿼리 히스토리 (${history.length})`}
            extra={
                <button
                    onClick={onClearHistory}
                    className="text-sm text-red-600 hover:text-red-700 font-medium flex items-center gap-1"
                >
                    <Trash2 className="w-4 h-4" />
                    전체 삭제
                </button>
            }
        >
            <div className="space-y-2 max-h-96 overflow-y-auto">
                {history.slice().reverse().map((item) => (
                    <div
                        key={item.id}
                        className={`border rounded-lg p-3 hover:bg-gray-50 transition-colors cursor-pointer ${
                            item.success
                                ? 'border-gray-200'
                                : 'border-red-200 bg-red-50'
                        }`}
                        onClick={() => onSelectQuery(item.query)}
                    >
                        <div className="flex justify-between items-start mb-2">
                            <div className="flex items-center gap-2">
                                <span className={`w-2 h-2 rounded-full ${
                                    item.success ? 'bg-green-500' : 'bg-red-500'
                                }`} />
                                <span className="text-xs text-gray-500">
                                    {formatTime(item.timestamp)}
                                </span>
                                {item.executionTime && (
                                    <span className="text-xs text-gray-500">
                                        • {formatExecutionTime(item.executionTime)}
                                    </span>
                                )}
                            </div>
                            <button
                                onClick={(e) => {
                                    e.stopPropagation();
                                    onSelectQuery(item.query);
                                }}
                                className="p-1 text-blue-600 hover:text-blue-700 hover:bg-blue-50 rounded transition-colors"
                                title="재실행"
                            >
                                <Play className="w-4 h-4" />
                            </button>
                        </div>
                        <code className="block text-xs bg-white p-2 rounded border border-gray-200 text-gray-700 overflow-x-auto">
                            {item.query}
                        </code>
                    </div>
                ))}
            </div>
        </Card>
    );
}