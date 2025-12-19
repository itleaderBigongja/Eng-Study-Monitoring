export interface LogEntry {
    id: string;
    index: string;
    timestamp: string;
    logLevel: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
    loggerName: string;
    message: string;
    application: string;
    stackTrace?: string;
}

export interface LogSearchParams {
    index: string;
    keyword?: string;
    logLevel?: string;
    from: number;
    size: number;
}

export interface LogSearchResult {
    total: number;
    logs: LogEntry[];
    from: number;
    size: number;
}