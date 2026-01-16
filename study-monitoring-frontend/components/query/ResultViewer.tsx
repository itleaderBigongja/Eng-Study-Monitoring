// components/query/ResultViewer.tsx
'use client';

import React, { useState } from 'react';
import { Table, FileJson, BarChart3 } from 'lucide-react';
import Card from '@/components/common/Card';

interface ResultViewerProps {
    data: any;
    executionTime?: number;
}

type ViewMode = 'json' | 'table' | 'chart';

/**
 * 쿼리 결과 뷰어 컴포넌트
 *
 * 기능:
 * - JSON 뷰 (전체 응답)
 * - 테이블 뷰 (메트릭 값 정리)
 * - 차트 뷰 (시각화)
 *
 * @param data - Prometheus 응답 데이터
 * @param executionTime - 실행 시간 (ms)
 */
export default function ResultViewer({ data, executionTime }: ResultViewerProps) {
    const [viewMode, setViewMode] = useState<ViewMode>('table');

    if (!data) {
        return (
            <Card title="결과">
                <div className="text-center py-8 text-gray-500">
                    <FileJson className="w-12 h-12 mx-auto mb-3 opacity-50" />
                    <p className="text-sm">쿼리를 실행하면 결과가 표시됩니다</p>
                </div>
            </Card>
        );
    }

    const resultData = data.data?.result || [];
    const resultType = data.data?.resultType || 'unknown';

    return (
        <Card
            title="실행 결과"
            extra={
                <div className="flex items-center gap-2">
                    {executionTime && (
                        <span className="text-sm text-gray-500">
                            실행 시간: {executionTime.toFixed(0)}ms
                        </span>
                    )}
                    <div className="flex border border-gray-300 rounded-lg overflow-hidden">
                        <button
                            onClick={() => setViewMode('table')}
                            className={`px-3 py-1.5 text-sm flex items-center gap-1.5 ${
                                viewMode === 'table'
                                    ? 'bg-blue-500 text-white'
                                    : 'bg-white text-gray-700 hover:bg-gray-50'
                            }`}
                        >
                            <Table className="w-4 h-4" />
                            테이블
                        </button>
                        <button
                            onClick={() => setViewMode('json')}
                            className={`px-3 py-1.5 text-sm flex items-center gap-1.5 border-l border-gray-300 ${
                                viewMode === 'json'
                                    ? 'bg-blue-500 text-white'
                                    : 'bg-white text-gray-700 hover:bg-gray-50'
                            }`}
                        >
                            <FileJson className="w-4 h-4" />
                            JSON
                        </button>
                    </div>
                </div>
            }
        >
            <div className="space-y-3">
                {/* 결과 요약 */}
                <div className="flex gap-4 text-sm">
                    <div>
                        <span className="text-gray-600">Status: </span>
                        <span className={`font-medium ${
                            data.status === 'success' ? 'text-green-600' : 'text-red-600'
                        }`}>
                            {data.status}
                        </span>
                    </div>
                    <div>
                        <span className="text-gray-600">Type: </span>
                        <span className="font-medium text-gray-800">{resultType}</span>
                    </div>
                    <div>
                        <span className="text-gray-600">Results: </span>
                        <span className="font-medium text-gray-800">{resultData.length}</span>
                    </div>
                </div>

                {/* 뷰 모드별 렌더링 */}
                {viewMode === 'table' && <TableView data={resultData} />}
                {viewMode === 'json' && <JSONView data={data} />}
            </div>
        </Card>
    );
}

/**
 * 테이블 뷰 컴포넌트
 */
function TableView({ data }: { data: any[] }) {
    if (data.length === 0) {
        return (
            <div className="text-center py-4 text-gray-500 text-sm">
                결과가 없습니다
            </div>
        );
    }

    return (
        <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                        Metric
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                        Labels
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                        Value
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                        Timestamp
                    </th>
                </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                {data.map((item, index) => {
                    const metric = item.metric || {};
                    const value = item.value || [];
                    const metricName = metric.__name__ || '-';
                    const labels = Object.entries(metric)
                        .filter(([key]) => key !== '__name__')
                        .map(([key, val]) => `${key}="${val}"`)
                        .join(', ');

                    const timestamp = value[0] ? new Date(value[0] * 1000).toLocaleString('ko-KR') : '-';
                    const metricValue = value[1] ? parseFloat(value[1]).toFixed(4) : '-';

                    return (
                        <tr key={index} className="hover:bg-gray-50">
                            <td className="px-4 py-3 text-sm font-mono text-gray-900">
                                {metricName}
                            </td>
                            <td className="px-4 py-3 text-xs font-mono text-gray-600 max-w-md truncate">
                                {labels || '-'}
                            </td>
                            <td className="px-4 py-3 text-sm font-mono text-right text-blue-600 font-medium">
                                {metricValue}
                            </td>
                            <td className="px-4 py-3 text-xs text-gray-500">
                                {timestamp}
                            </td>
                        </tr>
                    );
                })}
                </tbody>
            </table>
        </div>
    );
}

/**
 * JSON 뷰 컴포넌트
 */
function JSONView({ data }: { data: any }) {
    return (
        <pre className="bg-gray-900 text-gray-100 p-4 rounded-lg overflow-x-auto text-xs">
            {JSON.stringify(data, null, 2)}
        </pre>
    );
}