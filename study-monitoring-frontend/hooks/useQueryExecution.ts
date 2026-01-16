// hooks/useQueryExecution.ts
'use client';

import { useState, useCallback, useEffect } from 'react';
import { executeMetricsQuery } from '@/lib/api/metrics';
import type { QueryHistoryItem } from '@/components/query/QueryHistory';

const HISTORY_KEY = 'promql_query_history';
const MAX_HISTORY = 20;

interface UseQueryExecutionReturn {
    result: any;
    loading: boolean;
    error: string | null;
    history: QueryHistoryItem[];
    execute: (query: string) => Promise<void>;
    clearHistory: () => void;
}

/**
 * 쿼리 실행 및 히스토리 관리 훅
 *
 * 기능:
 * - PromQL 쿼리 실행
 * - 실행 히스토리 LocalStorage 저장
 * - 성공/실패 상태 관리
 * - 실행 시간 측정
 *
 * @returns 결과, 로딩 상태, 에러, 히스토리, 실행 함수
 */
export function useQueryExecution(): UseQueryExecutionReturn {
    const [result, setResult] = useState<any>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [history, setHistory] = useState<QueryHistoryItem[]>([]);

    // LocalStorage에서 히스토리 로드
    useEffect(() => {
        try {
            const saved = localStorage.getItem(HISTORY_KEY);
            if (saved) {
                setHistory(JSON.parse(saved));
            }
        } catch (err) {
            console.error('히스토리 로드 실패:', err);
        }
    }, []);

    // 히스토리를 LocalStorage에 저장
    const saveHistory = useCallback((newHistory: QueryHistoryItem[]) => {
        try {
            localStorage.setItem(HISTORY_KEY, JSON.stringify(newHistory));
            setHistory(newHistory);
        } catch (err) {
            console.error('히스토리 저장 실패:', err);
        }
    }, []);

    // 쿼리 실행
    const execute = useCallback(async (query: string) => {
        if (!query.trim()) {
            setError('쿼리를 입력해주세요');
            return;
        }

        setLoading(true);
        setError(null);
        const startTime = performance.now();

        try {
            const response = await executeMetricsQuery({ query });
            const executionTime = performance.now() - startTime;

            setResult(response);

            // 히스토리에 추가
            const historyItem: QueryHistoryItem = {
                id: Date.now().toString(),
                query,
                timestamp: Date.now(),
                success: true,
                executionTime
            };

            const newHistory = [historyItem, ...history].slice(0, MAX_HISTORY);
            saveHistory(newHistory);

        } catch (err: any) {
            const executionTime = performance.now() - startTime;
            const errorMessage = err.message || '쿼리 실행 중 오류가 발생했습니다';

            setError(errorMessage);
            setResult(null);

            // 실패한 쿼리도 히스토리에 추가
            const historyItem: QueryHistoryItem = {
                id: Date.now().toString(),
                query,
                timestamp: Date.now(),
                success: false,
                executionTime
            };

            const newHistory = [historyItem, ...history].slice(0, MAX_HISTORY);
            saveHistory(newHistory);

        } finally {
            setLoading(false);
        }
    }, [history, saveHistory]);

    // 히스토리 삭제
    const clearHistory = useCallback(() => {
        try {
            localStorage.removeItem(HISTORY_KEY);
            setHistory([]);
        } catch (err) {
            console.error('히스토리 삭제 실패:', err);
        }
    }, []);

    return {
        result,
        loading,
        error,
        history,
        execute,
        clearHistory
    };
}