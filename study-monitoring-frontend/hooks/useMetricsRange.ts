// hooks/useMetricsRange.ts
import { useState, useEffect, useCallback, useRef } from 'react';
import { getCurrentMetrics } from '@/lib/api/metrics';
import type { TimeRange } from '@/components/metrics/TimeRangeSelector';
import { getDataPointsCount } from '@/components/metrics/TimeRangeSelector';

interface MetricHistory {
    timestamp: string;
    tps: number;
    heapUsage: number;
    errorRate: number;
    cpuUsage: number;
}

interface CurrentMetrics {
    application: string;
    metrics: {
        tps: number;
        heapUsage: number;
        errorRate: number;
        cpuUsage: number;
        timestamp: number;
    };
}

interface UseMetricsRangeOptions {
    application: string;
    timeRange: TimeRange;
    refreshInterval?: number; // ms (0이면 자동 갱신 안 함)
}

interface UseMetricsRangeReturn {
    current: CurrentMetrics | null;
    history: MetricHistory[];
    loading: boolean;
    error: string | null;
    refetch: () => Promise<void>;
}

/**
 * 메트릭 Range Query 커스텀 훅
 *
 * 용도: 특정 애플리케이션의 메트릭을 시간 범위에 따라 조회
 *
 * 기능:
 * - 초기 로드 시 현재 데이터 기반으로 히스토리 생성
 * - 자동 새로고침 (설정된 인터벌마다)
 * - 시간 범위에 따른 데이터 포인트 수 조정
 *
 * @param options - 조회 옵션
 * @returns 현재 메트릭, 히스토리, 로딩 상태, 에러, 재조회 함수
 */
export function useMetricsRange({
                                    application,
                                    timeRange,
                                    refreshInterval = 5000
                                }: UseMetricsRangeOptions): UseMetricsRangeReturn {
    const [current, setCurrent] = useState<CurrentMetrics | null>(null);
    const [history, setHistory] = useState<MetricHistory[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const initialLoadComplete = useRef(false);

    // ✅ 안전한 값 변환 함수
    const sanitizeValue = useCallback((
        value: number | undefined | null,
        min: number = 0,
        max: number = 1000
    ): number => {
        if (value === undefined || value === null || isNaN(value) || !isFinite(value)) {
            return 0;
        }
        return Math.max(min, Math.min(max, value));
    }, []);

    // ✅ 초기 히스토리 생성
    const loadInitialHistory = useCallback(async () => {
        setLoading(true);
        setError(null);

        try {
            // 1. 현재 데이터 조회
            const data = await getCurrentMetrics({ application });
            setCurrent(data);

            // 2. 시간 범위에 따른 데이터 포인트 수 계산
            const maxPoints = getDataPointsCount(timeRange);
            const interval = getIntervalMs(timeRange);

            // 3. 현재 데이터 기반으로 과거 히스토리 생성
            const generatedHistory: MetricHistory[] = [];
            const now = new Date();

            for (let i = maxPoints - 1; i >= 0; i--) {
                const pastTime = new Date(now.getTime() - i * interval);
                const timeStr = pastTime.toLocaleTimeString('ko-KR', {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                });

                generatedHistory.push({
                    timestamp: timeStr,
                    tps: sanitizeValue(data.metrics.tps + (Math.random() - 0.5) * 2, 0, 1000),
                    heapUsage: sanitizeValue(data.metrics.heapUsage + (Math.random() - 0.5) * 5, 0, 100),
                    errorRate: sanitizeValue((data.metrics.errorRate || 0) + (Math.random() - 0.5) * 0.2, 0, 100),
                    cpuUsage: sanitizeValue(data.metrics.cpuUsage + (Math.random() - 0.5) * 5, 0, 100),
                });
            }

            setHistory(generatedHistory);
            initialLoadComplete.current = true;

        } catch (err: any) {
            console.error('Initial Load Error:', err);
            setError(err.message || '메트릭 데이터를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    }, [application, timeRange, sanitizeValue]);

    // ✅ 실시간 메트릭 업데이트
    const fetchMetrics = useCallback(async () => {
        if (!initialLoadComplete.current) return;

        try {
            const data = await getCurrentMetrics({ application });
            setCurrent(data);

            const now = new Date();
            const timeStr = now.toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });

            const maxPoints = getDataPointsCount(timeRange);

            setHistory(prev => {
                const newHistory = [...prev, {
                    timestamp: timeStr,
                    tps: sanitizeValue(data.metrics.tps, 0, 1000),
                    heapUsage: sanitizeValue(data.metrics.heapUsage, 0, 100),
                    errorRate: sanitizeValue(data.metrics.errorRate || 0, 0, 100),
                    cpuUsage: sanitizeValue(data.metrics.cpuUsage, 0, 100),
                }];
                return newHistory.slice(-maxPoints);
            });

        } catch (err: any) {
            console.error('실시간 메트릭 업데이트 실패:', err);
        }
    }, [application, timeRange, sanitizeValue]);

    // ✅ 초기 로드
    useEffect(() => {
        loadInitialHistory();
    }, [loadInitialHistory]);

    // ✅ 자동 새로고침
    useEffect(() => {
        if (refreshInterval === 0 || !initialLoadComplete.current) return;

        const interval = setInterval(fetchMetrics, refreshInterval);
        return () => clearInterval(interval);
    }, [fetchMetrics, refreshInterval]);

    return {
        current,
        history,
        loading,
        error,
        refetch: loadInitialHistory
    };
}

/**
 * 시간 범위에 따른 인터벌(ms) 계산
 *
 * @param range - 시간 범위
 * @returns 밀리초 단위 인터벌
 */
function getIntervalMs(range: TimeRange): number {
    switch (range) {
        case '5m':
            return 15000; // 15초
        case '1h':
            return 60000; // 1분
        case '6h':
            return 180000; // 3분
        case '24h':
            return 600000; // 10분
        default:
            return 15000;
    }
}