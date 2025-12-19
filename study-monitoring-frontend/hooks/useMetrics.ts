import { useState, useEffect } from 'react';
import { getMetrics, MetricsQueryParams, MetricsResponse } from '@/lib/api';

export function useMetrics(params: MetricsQueryParams, autoRefresh: boolean = false) {
    const [data, setData] = useState<MetricsResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchMetrics = async () => {
        try {
            setLoading(true);
            setError(null);
            const result = await getMetrics(params);
            setData(result);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch metrics');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchMetrics();

        // 자동 새로고침
        if (autoRefresh) {
            const interval = setInterval(fetchMetrics, 15000); // 15초마다
            return () => clearInterval(interval);
        }
    }, [params.application, params.metric, params.hours, autoRefresh]);

    return { data, loading, error, refetch: fetchMetrics };
}