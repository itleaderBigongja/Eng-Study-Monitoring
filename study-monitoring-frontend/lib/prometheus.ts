import apiClient from './api';

export interface PrometheusMetric {
    metric: Record<string, string>;
    value: [number, string];
}

export interface PrometheusResponse {
    status: string;
    data: {
        resultType: string;
        result: PrometheusMetric[];
    };
}

export const prometheusApi = {
    /**
     * 메트릭 쿼리
     */
    query: async (query: string): Promise<PrometheusResponse> => {
        return apiClient.get<PrometheusResponse>(`/prometheus/query?query=${encodeURIComponent(query)}`);
    },

    /**
     * CPU 사용률 가져오기
     */
    getCpuUsage: async (): Promise<PrometheusResponse> => {
        return prometheusApi.query('rate(process_cpu_seconds_total[5m])');
    },

    /**
     * 메모리 사용률 가져오기
     */
    getMemoryUsage: async (): Promise<PrometheusResponse> => {
        return prometheusApi.query('jvm_memory_used_bytes');
    },

    /**
     * HTTP 요청 수 가져오기
     */
    getHttpRequests: async (): Promise<PrometheusResponse> => {
        return prometheusApi.query('rate(http_server_requests_seconds_count[5m])');
    },
};