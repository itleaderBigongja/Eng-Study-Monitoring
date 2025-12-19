import { useState, useEffect } from 'react';
import { searchLogs, LogSearchParams, LogSearchResponse } from '@/lib/api';

export function useLogs(params: LogSearchParams) {
    const [data, setData] = useState<LogSearchResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchLogs = async () => {
        try {
            setLoading(true);
            setError(null);
            const result = await searchLogs(params);
            setData(result);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch logs');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLogs();
    }, [params.index, params.keyword, params.logLevel, params.from, params.size]);

    return { data, loading, error, refetch: fetchLogs };
}