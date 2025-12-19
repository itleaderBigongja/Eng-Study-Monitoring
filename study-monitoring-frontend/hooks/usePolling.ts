import { useState, useEffect, useCallback, useRef } from 'react';

interface UsePollingOptions<T> {
    fetcher: () => Promise<T>;
    interval?: number;      // 폴링 간격 (ms)
    enabled?: boolean;      // 폴링 활성화 여부
    onSuccess?: (data: T) => void;
    onError?: (error: Error) => void;
}

export function usePolling<T>({
                                  fetcher,
                                  interval = 30000,
                                  enabled = true,
                                  onSuccess,
                                  onError,
                              }: UsePollingOptions<T>) {
    const [data, setData] = useState<T | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<Error | null>(null);
    const intervalRef = useRef<NodeJS.Timeout | null>(null);

    const fetch = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const result = await fetcher();
            setData(result);
            onSuccess?.(result);
        } catch (err) {
            const error = err instanceof Error ? err : new Error('Unknown error');
            setError(error);
            onError?.(error);
        } finally {
            setLoading(false);
        }
    }, [fetcher, onSuccess, onError]);

    useEffect(() => {
        if (!enabled) {
            if (intervalRef.current) {
                clearInterval(intervalRef.current);
                intervalRef.current = null;
            }
            return;
        }

        // 즉시 한 번 실행
        fetch();

        // 폴링 시작
        intervalRef.current = setInterval(fetch, interval);

        return () => {
            if (intervalRef.current) {
                clearInterval(intervalRef.current);
            }
        };
    }, [fetch, interval, enabled]);

    return { data, loading, error, refetch: fetch };
}