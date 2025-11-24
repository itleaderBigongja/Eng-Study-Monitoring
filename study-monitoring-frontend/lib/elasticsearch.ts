import apiClient from './api';

export interface LogEntry {
    timestamp: string;
    level: string;
    logger: string;
    message: string;
    thread: string;
}

export interface SearchResponse {
    hits: {
        total: {
            value: number;
        };
        hits: Array<{
            _source: LogEntry;
        }>;
    };
}

export const elasticsearchApi = {
    /**
     * 로그 검색
     */
    searchLogs: async (query: string, size: number = 100): Promise<SearchResponse> => {
        return apiClient.post<SearchResponse>('/elasticsearch/search', {
            query: {
                match: {
                    message: query,
                },
            },
            size,
            sort: [
                {
                    timestamp: {
                        order: 'desc',
                    },
                },
            ],
        });
    },

    /**
     * 최근 로그 가져오기
     */
    getRecentLogs: async (size: number = 100): Promise<SearchResponse> => {
        return apiClient.post<SearchResponse>('/elasticsearch/search', {
            query: {
                match_all: {},
            },
            size,
            sort: [
                {
                    timestamp: {
                        order: 'desc',
                    },
                },
            ],
        });
    },

    /**
     * 에러 로그 가져오기
     */
    getErrorLogs: async (size: number = 100): Promise<SearchResponse> => {
        return apiClient.post<SearchResponse>('/elasticsearch/search', {
            query: {
                match: {
                    level: 'ERROR',
                },
            },
            size,
            sort: [
                {
                    timestamp: {
                        order: 'desc',
                    },
                },
            ],
        });
    },
};