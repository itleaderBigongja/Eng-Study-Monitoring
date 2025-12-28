# Study Monitoring Frontend 프로젝트 구축 가이드
# 목차
1. 프로젝트 초기 설정
2. 프로젝트 구조
3. 환경 설정
4. API 연동 설정
5. 공통 컴포넌트
6. 페이지 구현
7. 실행 및 테스트

## 1. 프로젝트 초기 설정
### 1. 필수 패키지 설치
```bash
# 차트 라이브러리
npm install recharts

# 날짜 처리
npm install date-fns

# 아이콘
npm install lucide-react

# 상태 관리 (선택사항)
npm install zustand

# 개발 도구
npm install -D @types/node
```

### 2. 프로젝트 구조
```text
study-monitoring-frontend/
├── app/                              # Next.js App Router
│   ├── layout.tsx                    # 루트 레이아웃
│   ├── globals.css                   # app 디렉토리 전역 css
│   ├── page.tsx                      # 홈페이지 (/)
│   ├── dashboard/                    # 대시보드 (/dashboard)
│   │   └── page.tsx
│   │
│   ├── logs/                         # 로그 (/logs)
│   │   └── page.tsx
│   │
│   ├── metrics/                      # 메트릭 (/metrics)
│   │   └── page.tsx
│   │
│   ├── health/                       # 헬스체크 (/health)
│   │   └── page.tsx
│   │
│   └── statistics/                   # 통계 (/statistics)
│       ├── page.tsx
│       ├── timeseries/
│       │   └── page.tsx
│       ├── logs/
│       │   └── page.tsx
│       ├── access-logs/
│       │   └── page.tsx
│       ├── error-logs/
│       │   └── page.tsx
│       ├── performance-metrics/
│       │   └── page.tsx
│       ├── database-logs/
│       │   └── page.tsx
│       ├── audit-logs/
│       │   └── page.tsx
│       └── security-logs/
│           └── page.tsx
│   
├── components/                       # 컴포넌트
│   ├── layout/                       # 레이아웃 컴포넌트
│   │   ├── Header.tsx
│   │   ├── Sidebar.tsx
│   │   └── Footer.tsx
│   ├── dashboard/                    # 대시보드 컴포넌트
│   │   ├── ProcessCard.tsx
│   │   ├── MetricsCard.tsx
│   │   ├── ErrorList.tsx
│   │   └── LogChart.tsx
│   ├── charts/                       # 차트 컴포넌트
│   │   ├── LineChart.tsx
│   │   ├── BarChart.tsx
│   │   ├── PieChart.tsx
│   │   └── AreaChart.tsx
│   ├── common/                       # 공통 컴포넌트
│   │   ├── Card.tsx
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Select.tsx
│   │   ├── DateRangePicker.tsx
│   │   ├── Loading.tsx
│   │   └── ErrorMessage.tsx
│   └── statistics/                   # 통계 컴포넌트
│       ├── TimeSeriesChart.tsx
│       ├── LogStatistics.tsx
│       └── MetricsTable.tsx
│
├── lib/                              # 유틸리티 및 설정
│   ├── api/                          # API 연동
│   │   ├── client.ts                 # API 클라이언트 설정
│   │   ├── endpoints.ts              # API 엔드포인트 정의
│   │   ├── dashboard.ts              # 대시보드 API
│   │   ├── logs.ts                   # 로그 API
│   │   ├── metrics.ts                # 메트릭 API
│   │   ├── statistics.ts             # 통계 API
│   │   └── health.ts                 # 헬스체크 API
│   ├── utils/                        # 유틸리티
│   │   ├── dateFormatter.ts
│   │   ├── numberFormatter.ts
│   │   ├── chartHelpers.ts
│   │   └── validators.ts
│   └── types/                        # TypeScript 타입
│       ├── dashboard.ts
│       ├── logs.ts
│       ├── metrics.ts
│       └── statistics.ts
│
├── styles/                           # 스타일
│   └── globals.css
│
├── public/                           # 정적 파일
│   ├── logo.svg
│   └── favicon.ico
│
├── .env.local                        # 환경변수
├── next.config.ts                    # Next.js 설정
├── tailwind.config.js                # Tailwind 설정
├── tsconfig.json                     # TypeScript 설정
└── package.json
```

## 3. 환경변수 설정
### study-monitoring-frontend/.env.local
```
# Backend API URL
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081

# API Timeout (ms)
NEXT_PUBLIC_API_TIMEOUT=30000

# Refresh Interval (ms)
NEXT_PUBLIC_REFRESH_INTERVAL=15000

# 환경
NEXT_PUBLIC_ENV=development
```

### study-monitoring-frontend/.env-production
```bash
# 프로덕션 API URL
NEXT_PUBLIC_API_URL=http://monitoring-backend-service.monitoring.svc.cluster.local:8081

# API Timeout (ms)
NEXT_PUBLIC_API_TIMEOUT=30000

# Refresh Interval (ms)
NEXT_PUBLIC_REFRESH_INTERVAL=15000

# 환경
NEXT_PUBLIC_ENV=production
```

## Next.js 설정
### study-monitoring-frontend/next.config.ts
```typescript
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    reactStrictMode: true,

    // 1. API 프록시 설정 (CORS 문제 해결의 핵심)
    // 브라우저가 '/api/...'로 요청을 보내면 Next.js 서버가 백엔드로 대신 전달합니다.
    async rewrites() {
        // 환경변수에 설정된 백엔드 주소를 가져옵니다. (없으면 로컬호스트 기본값)
        const apiUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8081';

        console.log(`[Next.js] Proxy mapping: /api/* -> ${apiUrl}/api/*`);

        return [
            {
                source: '/api/:path*',      // 프론트엔드에서 호출하는 경로
                destination: `${apiUrl}/api/:path*`, // 실제 백엔드 주소
            },
        ];
    },

    // 2. 이미지 최적화 설정
    // 'domains' 대신 최신 방식인 'remotePatterns'를 사용하여 경고 메시지를 제거했습니다.
    images: {
        remotePatterns: [
            {
                protocol: 'http',
                hostname: 'localhost',
            },
        ],
    },

    // 3. 환경변수 명시적 로드 (선택사항)
    env: {
        NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL,
    },
};

export default nextConfig;
```

## Tailwind 설정
### study-monitoring-frontend/tailwind.config.js
```typescript
/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./app/**/*.{js,ts,jsx,tsx,mdx}",
        "./components/**/*.{js,ts,jsx,tsx,mdx}",
        "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    ],
    theme: {
        extend: {
            colors: {
                primary: {
                    50: '#f0f9ff',
                    100: '#e0f2fe',
                    200: '#bae6fd',
                    300: '#7dd3fc',
                    400: '#38bdf8',
                    500: '#0ea5e9',
                    600: '#0284c7',
                    700: '#0369a1',
                    800: '#075985',
                    900: '#0c4a6e',
                },
                // secondary는 colors 객체를 직접 가져오는 대신 값으로 하드코딩하여 오류 방지
                secondary: {
                    50: '#f8fafc',
                    100: '#f1f5f9',
                    200: '#e2e8f0',
                    300: '#cbd5e1',
                    400: '#94a3b8',
                    500: '#64748b',
                    600: '#475569',
                    700: '#334155',
                    800: '#1e293b',
                    900: '#0f172a',
                },
            },
            backgroundImage: {
                'gradient-sky': 'linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%)',
            },
            boxShadow: {
                'card': '0 4px 6px -1px rgba(14, 165, 233, 0.1), 0 2px 4px -1px rgba(14, 165, 233, 0.06)',
                'sky': '0 4px 14px 0 rgba(14, 165, 233, 0.39)',
            },
        },
    },
    plugins: [],
};
```

## 전역 스타일
### study-monitoring-frontend/app/globals.css
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

/* 추가 스타일 */
@layer utilities {
    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(10px); }
        to { opacity: 1; transform: translateY(0); }
    }
    .fade-in {
        animation: fadeIn 0.5s ease-out forwards;
    }
}

@layer components {
    ::-webkit-scrollbar {
        width: 8px;
        height: 8px;
    }
    ::-webkit-scrollbar-track {
        @apply bg-gray-100;
    }
    ::-webkit-scrollbar-thumb {
        @apply rounded-full;
        background-color: #38bdf8;
    }
    ::-webkit-scrollbar-thumb:hover {
        background-color: #0ea5e9;
    }

    /* 카드 스타일 */
    .card {
        @apply bg-white rounded-xl p-6 transition-all duration-300 border border-primary-100;
        box-shadow: 0 4px 6px -1px rgba(14, 165, 233, 0.1);
    }

    /* 버튼 스타일 */
    .btn-primary {
        @apply w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 transition-colors duration-200;
    }

    /* 입력 필드 스타일 */
    .input-field {
        @apply mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 sm:text-sm border p-2;
    }
}

@layer utilities {
    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(10px); }
        to { opacity: 1; transform: translateY(0); }
    }

    /* ★ 수정됨: 코드에서 사용하는 'fade-in' 클래스 이름으로 매핑 */
    .fade-in {
        animation: fadeIn 0.5s ease-out forwards;
    }

    .text-gradient {
        background-image: linear-gradient(to right, #0ea5e9, #06b6d4);
        @apply bg-clip-text text-transparent;
    }

    .chart-container {
        @apply w-full h-64 md:h-80;
    }
}
```

## layout.tsx
### study-monitoring-frontend/app/layout.tsx
```typescript jsx
import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';

import Header from '@/components/layout/Header'; // components/layout/Header.tsx 컴포넌트를 불러옵니다.
import Footer from '@/components/layout/Footer'; // components/layout/Footer.tsx 컴포넌트를 불러옵니다.

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
    title: 'Study Monitoring System',
    description: '실시간 시스템 모니터링 대시보드',
};

export default function RootLayout({
   children,
}: {
    children: React.ReactNode;
}) {
    return (
        <html lang="ko">
        <body className={`${inter.className} bg-primary-50 text-gray-900`}>
        <div className="min-h-screen flex flex-col">
            <Header />
            <main className="flex-1 container mx-auto px-4 py-8">
                {children}
            </main>
            <Footer />
        </div>
        </body>
        </html>
    );
}
```

# API 연동 설정
## 클라이언트 API 연동 설정
### study-monitoring-frontend/lib/api/client.ts
```typescript
// 환경변수 가져오기
const ENV_API_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8081';
const API_TIMEOUT = parseInt(process.env.NEXT_PUBLIC_API_TIMEOUT || '30000');

// 클라이언트 사이드인지 확인 (브라우저 환경)
const isClient = typeof window !== 'undefined';

// Base URL 결정 로직 수정
// 클라이언트 사이드면 Proxy를 타기 위해 빈 문자열('') 사용 -> /api/dashboard/... 로 요청됨
// 서버 사이드면 절대 경로 사용
const API_BASE_URL = isClient ? '' : ENV_API_URL;

export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data: T;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public status?: number,
    public data?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * API 요청 공통 함수
 */
async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;
  
  const config: RequestInit = {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  };

  // Timeout 설정
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT);
  config.signal = controller.signal;

  try {
    console.log(`[API Request] ${options.method || 'GET'} ${url}`);
    
    const response = await fetch(url, config);
    clearTimeout(timeoutId);

    // JSON 파싱
    const data = await response.json();

    // 에러 응답 처리
    if (!response.ok) {
      throw new ApiError(
        data.message || 'API 요청 실패',
        response.status,
        data
      );
    }

    // 백엔드 ApiResponseDTO 구조 확인
    if (data.success === false) {
      throw new ApiError(data.message || '요청 처리 실패', response.status, data);
    }

    console.log(`[API Response] ${url} - Success`);
    return data.data || data; // data.data 또는 data 자체 반환
    
  } catch (error) {
    clearTimeout(timeoutId);
    
    if (error instanceof ApiError) {
      throw error;
    }
    
    if ((error as Error).name === 'AbortError') {
      throw new ApiError('요청 시간 초과', 408);
    }
    
    console.error(`[API Error] ${url}`, error);
    throw new ApiError('네트워크 오류가 발생했습니다');
  }
}

/**
 * GET 요청
 */
export async function get<T>(
  endpoint: string,
  params?: Record<string, any>
): Promise<T> {
  // 쿼리 파라미터 추가
  const queryString = params
    ? '?' + new URLSearchParams(params as any).toString()
    : '';
  
  return request<T>(`${endpoint}${queryString}`, {
    method: 'GET',
  });
}

/**
 * POST 요청
 */
export async function post<T>(
  endpoint: string,
  body?: any
): Promise<T> {
  return request<T>(endpoint, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

/**
 * PUT 요청
 */
export async function put<T>(
  endpoint: string,
  body?: any
): Promise<T> {
  return request<T>(endpoint, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

/**
 * DELETE 요청
 */
export async function del<T>(
  endpoint: string
): Promise<T> {
  return request<T>(endpoint, {
    method: 'DELETE',
  });
}

/**
 * API 상태 확인
 */
export async function checkApiHealth(): Promise<boolean> {
  try {
    await get('/api/health');
    return true;
  } catch {
    return false;
  }
}
```

## 4.2 API 엔드 포인트 정의
### study-monitoring-frontend/lib/api/endpoints.ts
```typescript
// API 엔드포인트 상수 정의

export const ENDPOINTS = {
  // Health Check
  HEALTH: {
    BASE: '/api/health',
    ELASTICSEARCH: '/api/health/elasticsearch',
    DATABASE: '/api/health/database',
    PROMETHEUS: '/api/health/prometheus',
  },
  
  // Dashboard
  DASHBOARD: {
    OVERVIEW: '/api/dashboard/overview',
    METRICS: '/api/dashboard/metrics',
    PROCESSES: '/api/dashboard/processes',
  },
  
  // Logs
  LOGS: {
    SEARCH: '/api/logs/search',
    ERRORS: '/api/logs/errors',
    STATS: '/api/logs/stats',
  },
  
  // Metrics
  METRICS: {
    QUERY: '/api/metrics/query',
    CURRENT: '/api/metrics/current',
    RANGE: '/api/metrics/range',
  },
  
  // Statistics
  STATISTICS: {
    TIMESERIES: '/api/statistics/timeseries',
    LOGS: '/api/statistics/logs',
    ACCESS_LOGS: '/api/statistics/access-logs',
    ERROR_LOGS: '/api/statistics/error-logs',
    PERFORMANCE_METRICS: '/api/statistics/performance-metrics',
    DATABASE_LOGS: '/api/statistics/database-logs',
    AUDIT_LOGS: '/api/statistics/audit-logs',
    SECURITY_LOGS: '/api/statistics/security-logs',
  },
} as const;
```

## 헬스체크 Health API
### study-monitoring-frontend/lib/api/health.ts
```typescript
import { get } from './client';
import { ENDPOINTS } from './endpoints';

export interface HealthStatus {
  status: 'UP' | 'DOWN';
  [key: string]: any;
}

/**
 * 전체 헬스체크
 */
export async function getHealthStatus(): Promise<HealthStatus> {
  return get<HealthStatus>(ENDPOINTS.HEALTH.BASE);
}

/**
 * Elasticsearch 헬스체크
 */
export async function getElasticsearchHealth(): Promise<HealthStatus> {
  return get<HealthStatus>(ENDPOINTS.HEALTH.ELASTICSEARCH);
}

/**
 * Database 헬스체크
 */
export async function getDatabaseHealth(): Promise<HealthStatus> {
  return get<HealthStatus>(ENDPOINTS.HEALTH.DATABASE);
}

/**
 * Prometheus 헬스체크
 */
export async function getPrometheusHealth(): Promise<HealthStatus> {
  return get<HealthStatus>(ENDPOINTS.HEALTH.PROMETHEUS);
}
```

## 메트릭 시계열 API
### study-monitoring-frontend/lib/api/metrics.ts
```typescript
import { get, post } from './client';
import { ENDPOINTS } from './endpoints';

export interface MetricsQueryRequest {
  query: string;
  start?: number;
  end?: number;
  step?: string;
}

export interface CurrentMetricsParams {
  application?: string;
}

/**
 * PromQL 쿼리 실행
 */
export async function executeMetricsQuery(request: MetricsQueryRequest): Promise<any> {
  return post(ENDPOINTS.METRICS.QUERY, request);
}

/**
 * 현재 메트릭 조회
 */
export async function getCurrentMetrics(params?: CurrentMetricsParams): Promise<any> {
  return get(ENDPOINTS.METRICS.CURRENT, params);
}

/**
 * Range 메트릭 조회
 */
export async function getRangeMetrics(request: MetricsQueryRequest): Promise<any> {
  return post(ENDPOINTS.METRICS.RANGE, request);
}
```

## 통계 Statistics API
### study-monitoring-frontend/lib/api/statistics.ts
```typescript
import { get } from './client';
import { ENDPOINTS } from './endpoints';

export interface StatisticsQueryParams {
    metricType: string;
    startTime: string;
    endTime: string;
    timePeriod: string;
    aggregationType: string;
}

export interface LogStatisticsParams {
    startTime: string;
    endTime: string;
    timePeriod: string;
    logLevel?: string;
    eventAction?: string;
    threatLevel?: string;
}

/**
 * 시계열 통계 조회
 */
export async function getTimeSeriesStatistics(params: StatisticsQueryParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.TIMESERIES, params);
}

/**
 * 로그 통계 조회
 */
export async function getLogStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.LOGS, params);
}

/**
 * 접근 로그 통계 조회
 */
export async function getAccessLogStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.ACCESS_LOGS, params);
}

/**
 * 에러 로그 통계 조회
 */
export async function getErrorLogStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.ERROR_LOGS, params);
}

/**
 * 성능 메트릭 통계 조회
 */
export async function getPerformanceMetricsStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.PERFORMANCE_METRICS, params);
}

/**
 * 데이터베이스 로그 통계 조회
 */
export async function getDatabaseLogStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.DATABASE_LOGS, params);
}

/**
 * 감사 로그 통계 조회
 */
export async function getAuditLogStatistics(params: LogStatisticsParams): Promise<any> {
    return get(ENDPOINTS.STATISTICS.AUDIT_LOGS, params);
}

/**
 * 보안 로그 통계 조회
 */
export async function getSecurityLogStatistics(params: LogStatisticsParams): Promise<any> {
    // URLSearchParams를 사용하여 쿼리 파라미터 생성
    const queryParams = new URLSearchParams({
        startTime: params.startTime,
        endTime: params.endTime,
        timePeriod: params.timePeriod,
    });

    // threatLevel이 있을 때만 파라미터 추가
    if (params.threatLevel) {
        queryParams.append('threatLevel', params.threatLevel);
    }

    return get(`${ENDPOINTS.STATISTICS.SECURITY_LOGS}?${queryParams.toString()}`);
}
```

## 대시보드 API 연계
### study-monitoring-frontend/lib/api/dashboard.ts
```typescript
import { get } from './client';
import { ENDPOINTS } from './endpoints';
import {
  DashboardOverview,
  MetricsQueryRequest,
  MetricsResponse,
  ProcessSummary,
} from '../types/dashboard';

/**
 * 대시보드 전체 개요 조회
 */
export async function getDashboardOverview(): Promise<DashboardOverview> {
  return get<DashboardOverview>(ENDPOINTS.DASHBOARD.OVERVIEW);
}

/**
 * 메트릭 조회
 */
export async function getDashboardMetrics(
  params: MetricsQueryRequest
): Promise<MetricsResponse> {
  return get<MetricsResponse>(ENDPOINTS.DASHBOARD.METRICS, params);
}

/**
 * 프로세스 목록 및 요약 조회
 */
export async function getProcesses(): Promise<{
  processes: any[];
  summary: ProcessSummary;
}> {
  return get(ENDPOINTS.DASHBOARD.PROCESSES);
}
```

## 로그 검색 API
### study-monitoring-frontend/lib/api/logs.ts
```typescript
import { get } from './client';
import { ENDPOINTS } from './endpoints';
import {
  LogSearchRequest,
  LogSearchResponse,
  LogStats,
  ErrorLogItem,
} from '../types/logs';

/**
 * 로그 검색
 */
export async function searchLogs(
  params: LogSearchRequest
): Promise<LogSearchResponse> {
  return get<LogSearchResponse>(ENDPOINTS.LOGS.SEARCH, params);
}

/**
 * 최근 에러 로그 조회
 */
export async function getRecentErrors(
  limit: number = 20
): Promise<ErrorLogItem[]> {
  const response = await get<{ errors: ErrorLogItem[] }>(
    ENDPOINTS.LOGS.ERRORS,
    { limit }
  );
  return response.errors;
}

/**
 * 로그 통계 조회
 */
export async function getLogStats(
  index: string = 'application-logs-*'
): Promise<LogStats> {
  return get<LogStats>(ENDPOINTS.LOGS.STATS, { index });
}
```

# 유틸리티 함수
## 날짜/시간 포멧팅
### study-monitoring-frontend/lib/utils/dateFormatter.ts
```typescript
// ============================================
// 날짜/시간 포맷팅 유틸리티
// ============================================

/**
 * ISO 문자열을 로컬 시간으로 변환
 */
export function formatDateTime(isoString: string): string {
  try {
    const date = new Date(isoString);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return isoString;
  }
}

/**
 * ISO 문자열을 날짜만 표시
 */
export function formatDate(isoString: string): string {
  try {
    const date = new Date(isoString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  } catch {
    return isoString;
  }
}

/**
 * ISO 문자열을 시간만 표시
 */
export function formatTime(isoString: string): string {
  try {
    const date = new Date(isoString);
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return isoString;
  }
}

/**
 * Unix timestamp를 로컬 시간으로 변환
 */
export function formatTimestamp(timestamp: number): string {
  try {
    const date = new Date(timestamp * 1000);
    return formatDateTime(date.toISOString());
  } catch {
    return String(timestamp);
  }
}

/**
 * 상대 시간 표시 (예: 5분 전, 2시간 전)
 */
export function formatRelativeTime(isoString: string): string {
  try {
    const date = new Date(isoString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffSec < 60) return `${diffSec}초 전`;
    if (diffMin < 60) return `${diffMin}분 전`;
    if (diffHour < 24) return `${diffHour}시간 전`;
    if (diffDay < 7) return `${diffDay}일 전`;
    return formatDate(isoString);
  } catch {
    return isoString;
  }
}

/**
 * 날짜 범위를 문자열로 표시
 */
export function formatDateRange(start: string, end: string): string {
  return `${formatDateTime(start)} ~ ${formatDateTime(end)}`;
}

/**
 * 현재 시간을 ISO 문자열로 반환
 */
export function getCurrentISOString(): string {
  return new Date().toISOString();
}

/**
 * N시간 전 ISO 문자열 반환
 */
export function getHoursAgoISOString(hours: number): string {
  const date = new Date();
  date.setHours(date.getHours() - hours);
  return date.toISOString();
}

/**
 * datetime-local input용 포맷 (yyyy-MM-ddTHH:mm)
 */
export function formatForDateTimeInput(isoString: string): string {
  try {
    const date = new Date(isoString);
    return date.toISOString().slice(0, 16);
  } catch {
    return '';
  }
}

/**
 * datetime-local input 값을 백엔드 형식으로 변환 (yyyy-MM-dd HH:mm:ss)
 */
export function formatForBackend(dateTimeInput: string): string {
  return dateTimeInput.replace('T', ' ') + ':00';
}
```

## 숫자 포멧팅
### study-monitoring-frontned/lib/utils/numberFormatter.ts
```typescript
// ============================================
// 숫자 포맷팅 유틸리티
// ============================================

/**
 * 천 단위 콤마 추가
 */
export function formatNumber(num: number): string {
  return num.toLocaleString('ko-KR');
}

/**
 * 소수점 포맷팅
 */
export function formatDecimal(num: number, decimals: number = 2): string {
  return num.toFixed(decimals);
}

/**
 * 퍼센트 포맷팅
 */
export function formatPercent(num: number, decimals: number = 1): string {
  return `${num.toFixed(decimals)}%`;
}

/**
 * 바이트를 KB, MB, GB로 변환
 */
export function formatBytes(bytes: number, decimals: number = 2): string {
  if (bytes === 0) return '0 Bytes';

  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(decimals))} ${sizes[i]}`;
}

/**
 * 밀리초를 읽기 쉬운 형식으로 변환
 */
export function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}초`;
  
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  if (minutes < 60) {
    return remainingSeconds > 0 
      ? `${minutes}분 ${remainingSeconds}초` 
      : `${minutes}분`;
  }
  
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return remainingMinutes > 0 
    ? `${hours}시간 ${remainingMinutes}분` 
    : `${hours}시간`;
}

/**
 * 큰 숫자를 K, M, B 단위로 축약
 */
export function formatCompactNumber(num: number): string {
  if (num < 1000) return String(num);
  if (num < 1000000) return `${(num / 1000).toFixed(1)}K`;
  if (num < 1000000000) return `${(num / 1000000).toFixed(1)}M`;
  return `${(num / 1000000000).toFixed(1)}B`;
}

/**
 * 통화 포맷팅 (원화)
 */
export function formatCurrency(num: number): string {
  return `${num.toLocaleString('ko-KR')}원`;
}

/**
 * TPS를 읽기 쉬운 형식으로 변환
 */
export function formatTPS(tps: number): string {
  if (tps < 1) return `${(tps * 1000).toFixed(0)} req/s`;
  return `${tps.toFixed(2)} req/s`;
}

/**
 * 응답시간을 읽기 쉬운 형식으로 변환
 */
export function formatResponseTime(ms: number): string {
  if (ms < 1) return `${(ms * 1000).toFixed(0)}μs`;
  if (ms < 1000) return `${ms.toFixed(0)}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}
```

## 입력 검증
### study-monitoring-frontend/lib/utils/validators.ts
```typescript
// ============================================
// 입력 검증 유틸리티
// ============================================

/**
 * 이메일 유효성 검증
 */
export function isValidEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * URL 유효성 검증
 */
export function isValidURL(url: string): boolean {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * IP 주소 유효성 검증
 */
export function isValidIP(ip: string): boolean {
  const ipRegex = /^(\d{1,3}\.){3}\d{1,3}$/;
  if (!ipRegex.test(ip)) return false;
  
  return ip.split('.').every(part => {
    const num = parseInt(part, 10);
    return num >= 0 && num <= 255;
  });
}

/**
 * 날짜 형식 검증 (yyyy-MM-dd HH:mm:ss)
 */
export function isValidDateFormat(dateStr: string): boolean {
  const dateRegex = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/;
  if (!dateRegex.test(dateStr)) return false;
  
  try {
    const date = new Date(dateStr);
    return !isNaN(date.getTime());
  } catch {
    return false;
  }
}

/**
 * 날짜 범위 검증
 */
export function isValidDateRange(start: string, end: string): boolean {
  try {
    const startDate = new Date(start);
    const endDate = new Date(end);
    return startDate <= endDate;
  } catch {
    return false;
  }
}

/**
 * 숫자 범위 검증
 */
export function isInRange(
  value: number,
  min: number,
  max: number
): boolean {
  return value >= min && value <= max;
}

/**
 * 문자열 길이 검증
 */
export function isValidLength(
  str: string,
  minLength: number,
  maxLength: number
): boolean {
  return str.length >= minLength && str.length <= maxLength;
}

/**
 * 포트 번호 유효성 검증
 */
export function isValidPort(port: number): boolean {
  return isInRange(port, 1, 65535);
}

/**
 * 로그 레벨 유효성 검증
 */
export function isValidLogLevel(level: string): boolean {
  const validLevels = ['DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL'];
  return validLevels.includes(level.toUpperCase());
}

/**
 * 시간 주기 유효성 검증
 */
export function isValidTimePeriod(period: string): boolean {
  const validPeriods = ['MINUTE', 'HOUR', 'DAY', 'WEEK', 'MONTH'];
  return validPeriods.includes(period.toUpperCase());
}

/**
 * 메트릭 타입 유효성 검증
 */
export function isValidMetricType(type: string): boolean {
  const validTypes = ['TPS', 'HEAP_USAGE', 'ERROR_RATE', 'CPU_USAGE'];
  return validTypes.includes(type.toUpperCase());
}

/**
 * 빈 문자열 또는 null/undefined 체크
 */
export function isEmpty(value: any): boolean {
  return (
    value === null ||
    value === undefined ||
    (typeof value === 'string' && value.trim() === '')
  );
}

/**
 * 객체가 비어있는지 체크
 */
export function isEmptyObject(obj: any): boolean {
  return obj && Object.keys(obj).length === 0;
}
```

## 차트 헬퍼
### study-monitoring-frontend/lib/utils/chartHelpers.ts
```typescript
// ============================================
// 차트 관련 헬퍼 함수
// ============================================

/**
 * 차트 색상 팔레트
 */
export const CHART_COLORS = {
  primary: '#0ea5e9',
  success: '#10b981',
  warning: '#f59e0b',
  error: '#ef4444',
  info: '#3b82f6',
  purple: '#8b5cf6',
  pink: '#ec4899',
  indigo: '#6366f1',
};

/**
 * 로그 레벨별 색상
 */
export const LOG_LEVEL_COLORS = {
  DEBUG: '#8b5cf6',
  INFO: '#0ea5e9',
  WARN: '#f59e0b',
  ERROR: '#ef4444',
  FATAL: '#dc2626',
};

/**
 * 차트 툴팁 포맷터
 */
export function formatChartTooltip(
  value: number,
  unit: string = ''
): string {
  return `${value.toFixed(2)} ${unit}`;
}

/**
 * 차트 데이터 정규화
 */
export function normalizeChartData<T>(
  data: T[],
  keyField: keyof T,
  valueField: keyof T
): Array<{ name: string; value: number }> {
  return data.map(item => ({
    name: String(item[keyField]),
    value: Number(item[valueField]),
  }));
}

/**
 * Y축 도메인 계산
 */
export function calculateYDomain(
  data: number[],
  padding: number = 0.1
): [number, number] {
  if (data.length === 0) return [0, 100];
  
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min;
  
  return [
    Math.max(0, min - range * padding),
    max + range * padding,
  ];
}
```

# 타입 정의
## 대시보드 관련 타입
### study-monitoring-frontend/lib/types/dashboard.ts
```typescript
// ============================================
// Dashboard 관련 타입 정의
// ============================================

export interface ProcessStatus {
  processId: number;
  processName: string;
  processType: string;
  status: 'RUNNING' | 'STOPPED' | 'ERROR' | 'STARTING' | 'STOPPING';
  cpuUsage: number;
  memoryUsage: number;
  uptime: string;
  lastHealthCheck: string;
}

export interface ApplicationMetrics {
  tps: number | null;
  heapUsage: number | null;
  errorRate: number | null;
  responseTime: number | null;
}

export interface MetricsSummary {
  engStudy: ApplicationMetrics;
  monitoring: ApplicationMetrics;
}

export interface ErrorLog {
  id: string;
  timestamp: string;
  logLevel: string;
  message: string;
  application: string;
}

export interface LogCounts {
  [logLevel: string]: number;
}

export interface SystemStatistics {
  totalRequests: number;
  avgResponseTime: number;
  uptime: string;
}

export interface DashboardOverview {
  processes: ProcessStatus[];
  metrics: MetricsSummary;
  recentErrors: ErrorLog[];
  logCounts: LogCounts;
  statistics: SystemStatistics;
}

// 메트릭 쿼리 요청
export interface MetricsQueryRequest {
  application: string;
  metric: string;
  hours?: number;
}

// 메트릭 응답
export interface MetricsResponse {
  application: string;
  metric: string;
  data: Array<{
    timestamp: number;
    value: number;
  }>;
  start: number;
  end: number;
}

// 프로세스 요약
export interface ProcessSummary {
  total: number;
  running: number;
  stopped: number;
  error: number;
}
```

## 로그 관련 타입
### study-monitoring-frontend/lib/types/logs.ts
```typescript
// ============================================
// Logs 관련 타입 정의
// ============================================

export interface LogSearchRequest {
  index: string;
  keyword?: string;
  logLevel?: string;
  from?: number;
  size?: number;
}

export interface LogEntry {
  id: string;
  index: string;
  timestamp: string;
  logLevel: string;
  loggerName: string;
  message: string;
  application: string;
  stackTrace?: string;
}

export interface LogSearchResponse {
  total: number;
  logs: LogEntry[];
  from: number;
  size: number;
}

export interface LogStats {
  index: string;
  stats: {
    [logLevel: string]: number;
  };
}

export interface ErrorLogItem {
  id: string;
  timestamp: string;
  logLevel: string;
  message: string;
  application: string;
}
```

## 메트릭 관련 타입
### study-monitoring-frontend/lib/types/metrics.ts
```typescript
// ============================================
// Metrics 관련 타입 정의
// ============================================

export interface PrometheusQueryRequest {
  query: string;
  start?: number;
  end?: number;
  step?: string;
}

export interface PrometheusQueryResponse {
  status: string;
  data: {
    resultType: string;
    result: Array<{
      metric: Record<string, string>;
      value?: [number, string];
      values?: Array<[number, string]>;
    }>;
  };
}

export interface CurrentMetricsResponse {
  application: string;
  metrics: {
    tps: number;
    heapUsage: number;
    errorRate: number;
    cpuUsage: number;
    timestamp: number;
  };
}

export interface MetricDataPoint {
  timestamp: number;
  value: number;
}

export interface RangeMetricsResponse {
  query: string;
  start: number;
  end: number;
  step: string;
  data: MetricDataPoint[];
}
```

## 통계 관련 타입(모든 통계 페이지)
### study-monitoring-frontend/lib/types/statistics.ts
```typescript
// ============================================
// Statistics 관련 타입 정의
// ============================================

// 공통 쿼리 파라미터
export interface BaseStatisticsQuery {
  startTime: string;
  endTime: string;
  timePeriod: 'MINUTE' | 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';
}

// 시계열 통계
export interface TimeSeriesStatisticsQuery extends BaseStatisticsQuery {
  metricType: 'TPS' | 'HEAP_USAGE' | 'ERROR_RATE' | 'CPU_USAGE';
  aggregationType: 'AVG' | 'SUM' | 'MIN' | 'MAX' | 'COUNT';
}

export interface TimeSeriesDataPoint {
  timestamp: string;
  value: number;
  minValue?: number;
  maxValue?: number;
  sampleCount?: number;
}

export interface TimeSeriesStatisticsResponse {
  metricType: string;
  timePeriod: string;
  aggregationType: string;
  startTime: string;
  endTime: string;
  dataSource: 'PROMETHEUS' | 'POSTGRESQL' | 'MIXED';
  data: TimeSeriesDataPoint[];
}

// 로그 통계
export interface LogStatisticsQuery extends BaseStatisticsQuery {
  logLevel?: string;
}

export interface LogDistribution {
  timestamp: string;
  count: number;
}

export interface LogStatisticsResponse {
  startTime: string;
  endTime: string;
  timePeriod: string;
  logCounts: {
    [logLevel: string]: number;
  };
  distributions: LogDistribution[];
}

// 접근 로그 통계
export interface AccessLogStatisticsQuery extends BaseStatisticsQuery {
  httpMethod?: string;
  statusCode?: number;
  url?: string;
}

export interface AccessDistribution {
  timestamp: string;
  requestCount: number;
  avgResponseTime: number;
  errorCount: number;
}

export interface AccessLogStatisticsResponse {
  startTime: string;
  endTime: string;
  timePeriod: string;
  methodCounts: {
    [method: string]: number;
  };
  statusCodeCounts: {
    [code: string]: number;
  };
  avgResponseTime: number;
  distributions: AccessDistribution[];
}

// 에러 로그 통계
export interface ErrorLogStatisticsQuery extends BaseStatisticsQuery {
  errorType?: string;
  severity?: string;
}

export interface ErrorDistribution {
  timestamp: string;
  errorCount: number;
  errorTypeBreakdown: {
    [errorType: string]: number;
  };
}

export interface ErrorLogStatisticsResponse {
  startTime: string;
  endTime: string;
  timePeriod: string;
  errorTypeCounts: {
    [errorType: string]: number;
  };
  severityCounts: {
    [severity: string]: number;
  };
  distributions: ErrorDistribution[];
}

// 성능 메트릭 통계
export interface PerformanceMetricsQuery extends BaseStatisticsQuery {
  metricName?: string;
  aggregationType?: string;
}

export interface SystemMetrics {
  avgCpuUsage: number;
  avgMemoryUsage: number;
  avgDiskUsage: number;
  maxCpuUsage: number;
  maxMemoryUsage: number;
}

export interface JvmMetrics {
  avgHeapUsed: number;
  maxHeapUsed: number;
  totalGcCount: number;
  totalGcTime: number;
  avgThreadCount: number;
}

export interface MetricDistribution {
  timestamp: string;
  cpuUsage: number;
  memoryUsage: number;
  heapUsage: number;
}

export interface PerformanceMetricsResponse {
  startTime: string;
  endTime: string;
  timePeriod: string;
  systemMetrics: SystemMetrics;
  jvmMetrics: JvmMetrics;
  distributions: MetricDistribution[];
}

// 데이터베이스 로그 통계
export interface DatabaseLogStatisticsQuery extends BaseStatisticsQuery {
  operation?: string;
  tableName?: string;
}

export interface QueryPerformance {
  avgDuration: number;
  maxDuration: number;
  slowQueryCount: number;
  totalQueryCount: number;
}

export interface DatabaseDistribution {
  timestamp: string;
  queryCount: number;
  avgDuration: number;
  slowQueryCount: number;
}

export interface DatabaseLogStatisticsResponse {
  startTime: string;
  endTime: string;
  timePeriod: string;
  operationCounts: {
    [operation: string]: number;
  };
  tableCounts: {
    [table: string]: number;
  };
  queryPerformance: QueryPerformance;
  distributions: DatabaseDistribution[];
}

// 감사 로그 통계
export interface AuditLogStatisticsQuery extends BaseStatisticsQuery {
  eventAction?: string;
  eventCategory?: string;
  eventResult?: string;
}

export interface ResultStats {
  successCount: number;
  failureCount: number;
  successRate: number;
}

export interface AuditDistribution {
  timestamp: string;
  totalEvents: number;
  successEvents: number;
  failureEvents: number;
}

export interface AuditLogStatisticsResponse {
  startTime: string;
  endTime: string;
  timePeriod: string;
  eventActionCounts: {
    [action: string]: number;
  };
  categoryCounts: {
    [category: string]: number;
  };
  resultStats: ResultStats;
  distributions: AuditDistribution[];
}

// 보안 로그 통계
export interface SecurityLogStatisticsQuery extends BaseStatisticsQuery {
  threatLevel?: string;
  attackType?: string;
  blocked?: boolean;
}

export interface BlockStats {
  totalAttacks: number;
  blockedAttacks: number;
  allowedAttacks: number;
  blockRate: number;
}

export interface SecurityDistribution {
  timestamp: string;
  attackCount: number;
  blockedCount: number;
  threatLevelBreakdown: {
    [level: string]: number;
  };
}

export interface SecurityLogStatisticsResponse {
  startTime: string;
  endTime: string;
  timePeriod: string;
  threatLevelCounts: {
    [level: string]: number;
  };
  attackTypeCounts: {
    [type: string]: number;
  };
  blockStats: BlockStats;
  distributions: SecurityDistribution[];
}
```

## 공통 타입
### study-monitoring-frontend/lib/types/common.ts
```typescript
// ============================================
// 공통 타입 정의
// ============================================

// API 응답 공통 구조
export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data: T;
}

// API 에러
export interface ApiError {
  message: string;
  status?: number;
  data?: any;
}

// 페이지네이션
export interface Pagination {
  page: number;
  size: number;
  total: number;
}

// 정렬
export interface Sort {
  field: string;
  order: 'asc' | 'desc';
}

// 날짜 범위
export interface DateRange {
  startTime: string;
  endTime: string;
}

// 시간 주기
export type TimePeriod = 'MINUTE' | 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';

// 집계 방식
export type AggregationType = 'AVG' | 'SUM' | 'MIN' | 'MAX' | 'COUNT';

// 로그 레벨
export type LogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR' | 'FATAL';

// 프로세스 상태
export type ProcessStatus = 'RUNNING' | 'STOPPED' | 'ERROR' | 'STARTING' | 'STOPPING';

// 데이터 소스
export type DataSource = 'PROMETHEUS' | 'POSTGRESQL' | 'ELASTICSEARCH' | 'MIXED';
```

# 공통 컴포넌트
## Header 컴포넌트
### study-monitoring-frontend/components/layout/Header.tsx
```typescript jsx
import Link from 'next/link';
import { Activity, BarChart3, FileText, Heart } from 'lucide-react';

export default function HomePage() {
    return (
        <div className="min-h-[calc(100vh-8rem)] bg-gradient-sky">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
                {/* Hero Section */}
                <div className="text-center mb-16 fade-in">
                    <h1 className="text-5xl font-bold text-primary-700 mb-4">
                        Study Monitoring System
                    </h1>
                    <p className="text-xl text-secondary-600 mb-8">
                        실시간으로 시스템 상태를 모니터링하고 분석합니다
                    </p>
                    <Link
                        href="/dashboard"
                        className="inline-flex items-center px-8 py-4 bg-primary-500 hover:bg-primary-600 text-white text-lg font-semibold rounded-lg shadow-sky transition-colors"
                    >
                        <Activity className="w-6 h-6 mr-2" />
                        대시보드 시작하기
                    </Link>
                </div>

                {/* Features Grid */}
                <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <FeatureCard
                        icon={<Activity className="w-8 h-8" />}
                        title="실시간 메트릭"
                        description="TPS, Heap 사용률, CPU 등 핵심 메트릭을 실시간으로 모니터링"
                        href="/metrics"
                    />

                    <FeatureCard
                        icon={<BarChart3 className="w-8 h-8" />}
                        title="통계 분석"
                        description="시계열 데이터 분석 및 7가지 로그 통계 제공"
                        href="/statistics"
                    />

                    <FeatureCard
                        icon={<FileText className="w-8 h-8" />}
                        title="로그 검색"
                        description="Elasticsearch 기반 강력한 로그 검색 기능"
                        href="/logs"
                    />

                    <FeatureCard
                        icon={<Heart className="w-8 h-8" />}
                        title="헬스체크"
                        description="시스템 구성 요소의 상태를 확인"
                        href="/health"
                    />
                </div>

                {/* System Status */}
                <div className="mt-16 card text-center">
                    <h2 className="text-2xl font-bold text-primary-700 mb-4">
                        시스템 구성
                    </h2>
                    <div className="flex flex-wrap justify-center gap-4 text-secondary-600">
                        <div className="px-4 py-2 bg-primary-50 rounded-lg">
                            <span className="font-semibold">Backend:</span> Spring Boot 3.5.7
                        </div>
                        <div className="px-4 py-2 bg-primary-50 rounded-lg">
                            <span className="font-semibold">Frontend:</span> Next.js 15
                        </div>
                        <div className="px-4 py-2 bg-primary-50 rounded-lg">
                            <span className="font-semibold">Database:</span> PostgreSQL
                        </div>
                        <div className="px-4 py-2 bg-primary-50 rounded-lg">
                            <span className="font-semibold">Search:</span> Elasticsearch
                        </div>
                        <div className="px-4 py-2 bg-primary-50 rounded-lg">
                            <span className="font-semibold">Metrics:</span> Prometheus
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

function FeatureCard({
                         icon,
                         title,
                         description,
                         href,
                     }: {
    icon: React.ReactNode;
    title: string;
    description: string;
    href: string;
}) {
    return (
        <Link
            href={href}
            className="card hover:shadow-xl hover:scale-105 transition-all duration-300 cursor-pointer group"
        >
            <div className="text-primary-500 group-hover:text-primary-600 mb-4 transition-colors">
                {icon}
            </div>
            <h3 className="text-lg font-semibold text-primary-700 mb-2">{title}</h3>
            <p className="text-sm text-secondary-600">{description}</p>
        </Link>
    );
}
```

## Sidebar 컴포넌트
### study-monitoring-frontend/components/layout/Sidebar.tsx
```typescript jsx
'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
    LayoutDashboard,
    Activity,
    BarChart3,
    FileText,
    Heart,
    ChevronRight,
} from 'lucide-react';

const menuItems = [
    {
        title: '대시보드',
        href: '/dashboard',
        icon: LayoutDashboard,
    },
    {
        title: '메트릭',
        href: '/metrics',
        icon: Activity,
    },
    {
        title: '통계',
        href: '/statistics',
        icon: BarChart3,
        subItems: [
            { title: '시계열', href: '/statistics/timeseries' },
            { title: '로그 통계', href: '/statistics/logs' },
            { title: '접근 로그', href: '/statistics/access-logs' },
            { title: '에러 로그', href: '/statistics/error-logs' },
            { title: '성능 메트릭', href: '/statistics/performance-metrics' },
            { title: 'DB 로그', href: '/statistics/database-logs' },
            { title: '감사 로그', href: '/statistics/audit-logs' },
            { title: '보안 로그', href: '/statistics/security-logs' },
        ],
    },
    {
        title: '로그',
        href: '/logs',
        icon: FileText,
    },
    {
        title: '헬스체크',
        href: '/health',
        icon: Heart,
    },
];

export default function Sidebar() {
    const pathname = usePathname();

    return (
        <aside className="w-64 bg-white shadow-sky border-r border-primary-100 h-screen sticky top-16 overflow-y-auto">
            <nav className="p-4 space-y-2">
                {menuItems.map((item) => (
                    <div key={item.href}>
                        <SidebarItem
                            item={item}
                            isActive={pathname === item.href || pathname.startsWith(item.href + '/')}
                        />

                        {/* 서브 메뉴 */}
                        {item.subItems && pathname.startsWith(item.href) && (
                            <div className="ml-6 mt-2 space-y-1 fade-in">
                                {item.subItems.map((subItem) => (
                                    <Link
                                        key={subItem.href}
                                        href={subItem.href}
                                        className={`block px-3 py-2 rounded-lg text-sm transition-colors ${
                                            pathname === subItem.href
                                                ? 'bg-primary-100 text-primary-700 font-medium'
                                                : 'text-secondary-600 hover:bg-primary-50 hover:text-primary-600'
                                        }`}
                                    >
                                        {subItem.title}
                                    </Link>
                                ))}
                            </div>
                        )}
                    </div>
                ))}
            </nav>
        </aside>
    );
}

function SidebarItem({
                         item,
                         isActive,
                     }: {
    item: typeof menuItems[0];
    isActive: boolean;
}) {
    const Icon = item.icon;

    return (
        <Link
            href={item.href}
            className={`flex items-center justify-between px-4 py-3 rounded-lg transition-colors ${
                isActive
                    ? 'bg-primary-500 text-white shadow-lg'
                    : 'text-secondary-700 hover:bg-primary-50 hover:text-primary-700'
            }`}
        >
            <div className="flex items-center space-x-3">
                <Icon className="w-5 h-5" />
                <span className="font-medium">{item.title}</span>
            </div>
            {item.subItems && (
                <ChevronRight
                    className={`w-4 h-4 transition-transform ${
                        isActive ? 'rotate-90' : ''
                    }`}
                />
            )}
        </Link>
    );
}
```

## Footer 컴포넌트
### study-monitoring-frontend/components/layout/Footer.tsx
```typescript jsx
export default function Footer() {
    return (
        <footer className="bg-white border-t border-primary-100 mt-auto">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
                <div className="flex flex-col sm:flex-row justify-between items-center space-y-2 sm:space-y-0">
                    <p className="text-sm text-secondary-600">
                        © 2025 Study Monitoring System. All rights reserved.
                    </p>
                    <div className="flex items-center space-x-4 text-sm text-secondary-600">
                        <span>Backend: Spring Boot 3.5.7</span>
                        <span>•</span>
                        <span>Frontend: Next.js 15</span>
                    </div>
                </div>
            </div>
        </footer>
    );
}
```

# 대시보드 UI 컴포넌트
## Process Card 컴포넌트
### study-monitoring-frontend/components/dashboard/ProcessCard.tsx
```typescript jsx
import { Activity, AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import Card from '@/components/common/Card';

interface ProcessInfo {
    name: string;
    status: 'running' | 'stopped' | 'warning';
    uptime?: string;
    cpu?: number;
    memory?: number;
    pid?: number;
}

interface ProcessCardProps {
    processes: ProcessInfo[];
    title?: string;
}

export default function ProcessCard({ processes, title = '프로세스 상태' }: ProcessCardProps) {
    const getStatusIcon = (status: ProcessInfo['status']) => {
        switch (status) {
            case 'running':
                return <CheckCircle className="w-5 h-5 text-success" />;
            case 'warning':
                return <AlertCircle className="w-5 h-5 text-warning" />;
            case 'stopped':
                return <XCircle className="w-5 h-5 text-error" />;
            default:
                return <Activity className="w-5 h-5 text-secondary-400" />;
        }
    };

    const getStatusColor = (status: ProcessInfo['status']) => {
        switch (status) {
            case 'running':
                return 'bg-success/10 text-success border-success/20';
            case 'warning':
                return 'bg-warning/10 text-warning border-warning/20';
            case 'stopped':
                return 'bg-error/10 text-error border-error/20';
            default:
                return 'bg-secondary-100 text-secondary-600 border-secondary-200';
        }
    };

    return (
        <Card title={title}>
            <div className="space-y-3">
                {processes.length === 0 ? (
                    <div className="text-center py-8 text-secondary-500">
                        <Activity className="w-12 h-12 mx-auto mb-2 opacity-50" />
                        <p>프로세스 정보가 없습니다</p>
                    </div>
                ) : (
                    processes.map((process, index) => (
                        <div
                            key={index}
                            className={`p-4 rounded-lg border ${getStatusColor(process.status)} transition-all duration-200`}
                        >
                            <div className="flex items-start justify-between mb-2">
                                <div className="flex items-center space-x-3">
                                    {getStatusIcon(process.status)}
                                    <div>
                                        <h4 className="font-semibold text-gray-900">
                                            {process.name}
                                        </h4>
                                        {process.pid && (
                                            <p className="text-xs text-secondary-500">
                                                PID: {process.pid}
                                            </p>
                                        )}
                                    </div>
                                </div>
                                <span className="text-xs font-medium uppercase">
                                    {process.status}
                                </span>
                            </div>

                            {/* 프로세스 상세 정보 */}
                            {(process.uptime || process.cpu !== undefined || process.memory !== undefined) && (
                                <div className="grid grid-cols-3 gap-2 mt-3 pt-3 border-t border-current border-opacity-20">
                                    {process.uptime && (
                                        <div>
                                            <p className="text-xs text-secondary-600">Uptime</p>
                                            <p className="text-sm font-medium">{process.uptime}</p>
                                        </div>
                                    )}
                                    {process.cpu !== undefined && (
                                        <div>
                                            <p className="text-xs text-secondary-600">CPU</p>
                                            <p className="text-sm font-medium">{process.cpu.toFixed(1)}%</p>
                                        </div>
                                    )}
                                    {process.memory !== undefined && (
                                        <div>
                                            <p className="text-xs text-secondary-600">Memory</p>
                                            <p className="text-sm font-medium">{process.memory.toFixed(1)}%</p>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    ))
                )}
            </div>
        </Card>
    );
}
```

## 대시보드 에러 목록 UI 컴포넌트
### study-monitoring-frontend/components/dashboard/ErrorList.tsx
```typescript jsx
import { AlertTriangle, XCircle, AlertCircle, Info } from 'lucide-react';
import Card from '@/components/common/Card';

interface ErrorItem {
    id: string;
    timestamp: string;
    level: 'critical' | 'error' | 'warning' | 'info';
    message: string;
    source?: string;
    count?: number;
}

interface ErrorListProps {
    errors: ErrorItem[];
    title?: string;
    maxItems?: number;
}

export default function ErrorList({
                                      errors,
                                      title = '최근 에러 로그',
                                      maxItems = 10
                                  }: ErrorListProps) {
    const getLevelIcon = (level: ErrorItem['level']) => {
        switch (level) {
            case 'critical':
                return <XCircle className="w-5 h-5 text-error" />;
            case 'error':
                return <AlertCircle className="w-5 h-5 text-error" />;
            case 'warning':
                return <AlertTriangle className="w-5 h-5 text-warning" />;
            case 'info':
                return <Info className="w-5 h-5 text-primary-500" />;
            default:
                return <AlertCircle className="w-5 h-5 text-secondary-400" />;
        }
    };

    const getLevelColor = (level: ErrorItem['level']) => {
        switch (level) {
            case 'critical':
                return 'bg-error/10 border-error/20';
            case 'error':
                return 'bg-error/5 border-error/10';
            case 'warning':
                return 'bg-warning/10 border-warning/20';
            case 'info':
                return 'bg-primary-50 border-primary-100';
            default:
                return 'bg-secondary-50 border-secondary-100';
        }
    };

    const getLevelBadge = (level: ErrorItem['level']) => {
        const colors = {
            critical: 'bg-error text-white',
            error: 'bg-error/80 text-white',
            warning: 'bg-warning text-white',
            info: 'bg-primary-500 text-white',
        };

        return (
            <span className={`px-2 py-1 rounded text-xs font-semibold uppercase ${colors[level]}`}>
                {level}
            </span>
        );
    };

    const formatTimestamp = (timestamp: string) => {
        try {
            const date = new Date(timestamp);
            return date.toLocaleString('ko-KR', {
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
            });
        } catch {
            return timestamp;
        }
    };

    const displayErrors = errors.slice(0, maxItems);

    return (
        <Card
            title={title}
            subtitle={errors.length > 0 ? `총 ${errors.length}개 (최근 ${displayErrors.length}개 표시)` : undefined}
        >
            {errors.length === 0 ? (
                <div className="text-center py-12">
                    <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-success/10 flex items-center justify-center">
                        <AlertCircle className="w-8 h-8 text-success" />
                    </div>
                    <p className="text-secondary-600 font-medium">에러가 없습니다</p>
                    <p className="text-sm text-secondary-500 mt-1">시스템이 정상적으로 작동 중입니다</p>
                </div>
            ) : (
                <div className="space-y-3">
                    {displayErrors.map((error) => (
                        <div
                            key={error.id}
                            className={`p-4 rounded-lg border transition-all duration-200 hover:shadow-md ${getLevelColor(error.level)}`}
                        >
                            <div className="flex items-start space-x-3">
                                <div className="flex-shrink-0 mt-0.5">
                                    {getLevelIcon(error.level)}
                                </div>

                                <div className="flex-1 min-w-0">
                                    <div className="flex items-start justify-between mb-2">
                                        <div className="flex items-center space-x-2">
                                            {getLevelBadge(error.level)}
                                            {error.source && (
                                                <span className="text-xs text-secondary-600 font-medium">
                                                    {error.source}
                                                </span>
                                            )}
                                        </div>
                                        <span className="text-xs text-secondary-500 whitespace-nowrap ml-2">
                                            {formatTimestamp(error.timestamp)}
                                        </span>
                                    </div>

                                    <p className="text-sm text-gray-900 break-words">
                                        {error.message}
                                    </p>

                                    {error.count && error.count > 1 && (
                                        <div className="mt-2 inline-flex items-center px-2 py-1 bg-white/50 rounded text-xs font-medium text-secondary-700">
                                            <span className="mr-1">🔁</span>
                                            {error.count}회 발생
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))}

                    {errors.length > maxItems && (
                        <div className="text-center pt-2">
                            <p className="text-sm text-secondary-500">
                                {errors.length - maxItems}개의 에러가 더 있습니다
                            </p>
                        </div>
                    )}
                </div>
            )}
        </Card>
    );
}
```

## 대시보드 로그 차트 UI 컴포넌트
### study-monitoring-frontend/components/dashboard/LogChart.tsx
```typescript jsx
import { LineChart, Line, AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import Card from '@/components/common/Card';

interface LogChartData {
    timestamp: string;
    info?: number;
    warn?: number;
    error?: number;
    debug?: number;
    total?: number;
}

interface LogChartProps {
    data: LogChartData[];
    title?: string;
    chartType?: 'line' | 'area' | 'bar';
    height?: number;
}

export default function LogChart({
                                     data,
                                     title = '로그 발생 추이',
                                     chartType = 'area',
                                     height = 300
                                 }: LogChartProps) {
    // 차트 색상 정의
    const colors = {
        info: '#0ea5e9',
        warn: '#f59e0b',
        error: '#ef4444',
        debug: '#8b5cf6',
        total: '#10b981',
    };

    // 커스텀 툴팁
    const CustomTooltip = ({ active, payload, label }: any) => {
        if (active && payload && payload.length) {
            return (
                <div className="bg-white p-3 rounded-lg shadow-lg border border-primary-100">
                    <p className="text-sm font-semibold text-gray-900 mb-2">{label}</p>
                    {payload.map((entry: any, index: number) => (
                        <div key={index} className="flex items-center justify-between space-x-4">
                            <div className="flex items-center space-x-2">
                                <div
                                    className="w-3 h-3 rounded-full"
                                    style={{ backgroundColor: entry.color }}
                                />
                                <span className="text-xs font-medium text-gray-700 uppercase">
                                    {entry.name}
                                </span>
                            </div>
                            <span className="text-sm font-bold text-gray-900">
                                {entry.value}
                            </span>
                        </div>
                    ))}
                </div>
            );
        }
        return null;
    };

    // 라인 차트 렌더링
    const renderLineChart = () => (
        <ResponsiveContainer width="100%" height={height}>
            <LineChart data={data}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis
                    dataKey="timestamp"
                    stroke="#64748b"
                    style={{ fontSize: '12px' }}
                    angle={-45}
                    textAnchor="end"
                    height={60}
                />
                <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                <Tooltip content={<CustomTooltip />} />
                <Legend
                    wrapperStyle={{ paddingTop: '10px' }}
                    iconType="circle"
                />

                {data[0]?.info !== undefined && (
                    <Line
                        type="monotone"
                        dataKey="info"
                        stroke={colors.info}
                        strokeWidth={2}
                        dot={{ fill: colors.info, r: 3 }}
                        name="INFO"
                    />
                )}
                {data[0]?.warn !== undefined && (
                    <Line
                        type="monotone"
                        dataKey="warn"
                        stroke={colors.warn}
                        strokeWidth={2}
                        dot={{ fill: colors.warn, r: 3 }}
                        name="WARN"
                    />
                )}
                {data[0]?.error !== undefined && (
                    <Line
                        type="monotone"
                        dataKey="error"
                        stroke={colors.error}
                        strokeWidth={2}
                        dot={{ fill: colors.error, r: 3 }}
                        name="ERROR"
                    />
                )}
                {data[0]?.debug !== undefined && (
                    <Line
                        type="monotone"
                        dataKey="debug"
                        stroke={colors.debug}
                        strokeWidth={2}
                        dot={{ fill: colors.debug, r: 3 }}
                        name="DEBUG"
                    />
                )}
                {data[0]?.total !== undefined && (
                    <Line
                        type="monotone"
                        dataKey="total"
                        stroke={colors.total}
                        strokeWidth={3}
                        dot={{ fill: colors.total, r: 4 }}
                        name="TOTAL"
                    />
                )}
            </LineChart>
        </ResponsiveContainer>
    );

    // 영역 차트 렌더링
    const renderAreaChart = () => (
        <ResponsiveContainer width="100%" height={height}>
            <AreaChart data={data}>
                <defs>
                    <linearGradient id="colorInfo" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={colors.info} stopOpacity={0.8}/>
                        <stop offset="95%" stopColor={colors.info} stopOpacity={0.1}/>
                    </linearGradient>
                    <linearGradient id="colorWarn" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={colors.warn} stopOpacity={0.8}/>
                        <stop offset="95%" stopColor={colors.warn} stopOpacity={0.1}/>
                    </linearGradient>
                    <linearGradient id="colorError" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={colors.error} stopOpacity={0.8}/>
                        <stop offset="95%" stopColor={colors.error} stopOpacity={0.1}/>
                    </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis
                    dataKey="timestamp"
                    stroke="#64748b"
                    style={{ fontSize: '12px' }}
                    angle={-45}
                    textAnchor="end"
                    height={60}
                />
                <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                <Tooltip content={<CustomTooltip />} />
                <Legend
                    wrapperStyle={{ paddingTop: '10px' }}
                    iconType="circle"
                />

                {data[0]?.info !== undefined && (
                    <Area
                        type="monotone"
                        dataKey="info"
                        stroke={colors.info}
                        fill="url(#colorInfo)"
                        name="INFO"
                    />
                )}
                {data[0]?.warn !== undefined && (
                    <Area
                        type="monotone"
                        dataKey="warn"
                        stroke={colors.warn}
                        fill="url(#colorWarn)"
                        name="WARN"
                    />
                )}
                {data[0]?.error !== undefined && (
                    <Area
                        type="monotone"
                        dataKey="error"
                        stroke={colors.error}
                        fill="url(#colorError)"
                        name="ERROR"
                    />
                )}
            </AreaChart>
        </ResponsiveContainer>
    );

    // 바 차트 렌더링
    const renderBarChart = () => (
        <ResponsiveContainer width="100%" height={height}>
            <BarChart data={data}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis
                    dataKey="timestamp"
                    stroke="#64748b"
                    style={{ fontSize: '12px' }}
                    angle={-45}
                    textAnchor="end"
                    height={60}
                />
                <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                <Tooltip content={<CustomTooltip />} />
                <Legend
                    wrapperStyle={{ paddingTop: '10px' }}
                    iconType="circle"
                />

                {data[0]?.info !== undefined && (
                    <Bar dataKey="info" fill={colors.info} name="INFO" />
                )}
                {data[0]?.warn !== undefined && (
                    <Bar dataKey="warn" fill={colors.warn} name="WARN" />
                )}
                {data[0]?.error !== undefined && (
                    <Bar dataKey="error" fill={colors.error} name="ERROR" />
                )}
                {data[0]?.debug !== undefined && (
                    <Bar dataKey="debug" fill={colors.debug} name="DEBUG" />
                )}
            </BarChart>
        </ResponsiveContainer>
    );

    // 차트 타입에 따라 렌더링
    const renderChart = () => {
        switch (chartType) {
            case 'line':
                return renderLineChart();
            case 'area':
                return renderAreaChart();
            case 'bar':
                return renderBarChart();
            default:
                return renderAreaChart();
        }
    };

    return (
        <Card title={title}>
            {data.length === 0 ? (
                <div className="flex items-center justify-center" style={{ height }}>
                    <p className="text-secondary-500">표시할 데이터가 없습니다</p>
                </div>
            ) : (
                renderChart()
            )}
        </Card>
    );
}
```

# 공통 UI 컴포넌트
## 공통 카드 UI 컴포넌트
### study-monitoring-frontend/components/common/Card.tsx
```typescript tsx
import { ReactNode } from 'react';

interface CardProps {
    title?: string;
    subtitle?: string;
    children: ReactNode;
    className?: string;
    headerAction?: ReactNode;
}

export default function Card({
                                 title,
                                 subtitle,
                                 children,
                                 className = '',
                                 headerAction,
                             }: CardProps) {
    return (
        <div className={`card fade-in ${className}`}>
    {(title || headerAction) && (
        <div className="flex justify-between items-start mb-4">
        <div>
            {title && <h3 className="text-lg font-semibold text-primary-700">{title}</h3>}
        {subtitle && <p className="text-sm text-secondary-500 mt-1">{subtitle}</p>}
            </div>
            {headerAction && <div>{headerAction}</div>}
            </div>
            )}
            {children}
            </div>
        );
        }
```

### study-monitoring-frontend/components/common/Loading.tsx
```typescript tsx
import { Loader2 } from 'lucide-react';

interface LoadingProps {
    text?: string;
    size?: 'sm' | 'md' | 'lg';
    fullScreen?: boolean;
}

export default function Loading({
                                    text = '로딩 중...',
                                    size = 'md',
                                    fullScreen = false
                                }: LoadingProps) {
    const sizeClasses = {
        sm: 'w-6 h-6',
        md: 'w-10 h-10',
        lg: 'w-16 h-16',
    };

    const content = (
        <div className="flex flex-col items-center justify-center space-y-4">
        <Loader2 className={`${sizeClasses[size]} text-primary-500 animate-spin`} />
    <p className="text-secondary-600">{text}</p>
        </div>
);

    if (fullScreen) {
        return (
            <div className="fixed inset-0 bg-white/80 backdrop-blur-sm flex items-center justify-center z-50">
                {content}
                </div>
        );
    }

    return (
        <div className="flex items-center justify-center py-12">
            {content}
            </div>
    );
}
```

### study-monitoring-frontend/components/common/ErrorMessage.tsx
```typescript jsx
import { AlertCircle, RefreshCw } from 'lucide-react';
import Button from './Button';

interface ErrorMessageProps {
    message: string;
    onRetry?: () => void;
}

export default function ErrorMessage({ message, onRetry }: ErrorMessageProps) {
    return (
        <div className="card border-error/20 bg-error/5">
            <div className="flex items-start space-x-3">
                <AlertCircle className="w-6 h-6 text-error flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                    <h3 className="text-lg font-semibold text-error mb-2">오류 발생</h3>
                    <p className="text-secondary-700 mb-4">{message}</p>
                    {onRetry && (
                        <Button
                            variant="outline"
                            size="sm"
                            icon={<RefreshCw className="w-4 h-4" />}
                            onClick={onRetry}
                        >
                            다시 시도
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
}
```

### study-monitoring-frontend/components/common/DateRangePicker.tsx
```typescript jsx
'use client';

import { useState } from 'react';
import { Calendar } from 'lucide-react';

interface DateRangePickerProps {
    startDate: string;
    endDate: string;
    onChange: (start: string, end: string) => void;
}

export default function DateRangePicker({
                                            startDate,
                                            endDate,
                                            onChange,
                                        }: DateRangePickerProps) {
    const [start, setStart] = useState(startDate);
    const [end, setEnd] = useState(endDate);

    const handleApply = () => {
        onChange(start, end);
    };

    // 빠른 선택 옵션
    const quickOptions = [
        { label: '최근 1시간', hours: 1 },
        { label: '최근 6시간', hours: 6 },
        { label: '최근 24시간', hours: 24 },
        { label: '최근 7일', hours: 24 * 7 },
        { label: '최근 30일', hours: 24 * 30 },
    ];

    const handleQuickSelect = (hours: number) => {
        const now = new Date();
        const past = new Date(now.getTime() - hours * 60 * 60 * 1000);

        const formatDate = (date: Date) => {
            return date.toISOString().slice(0, 16);
        };

        setStart(formatDate(past));
        setEnd(formatDate(now));
    };

    return (
        <div className="card">
            <div className="flex items-center space-x-2 mb-4">
                <Calendar className="w-5 h-5 text-primary-600" />
                <h3 className="font-semibold text-primary-700">기간 선택</h3>
            </div>

            {/* 빠른 선택 */}
            <div className="flex flex-wrap gap-2 mb-4">
                {quickOptions.map((option) => (
                    <button
                        key={option.label}
                        onClick={() => handleQuickSelect(option.hours)}
                        className="px-3 py-1.5 text-sm bg-primary-50 hover:bg-primary-100 text-primary-700 rounded-lg transition-colors"
                    >
                        {option.label}
                    </button>
                ))}
            </div>

            {/* 날짜 입력 */}
            <div className="space-y-4">
                <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                        시작 시간
                    </label>
                    <input
                        type="datetime-local"
                        value={start}
                        onChange={(e) => setStart(e.target.value)}
                        className="input-field"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                        종료 시간
                    </label>
                    <input
                        type="datetime-local"
                        value={end}
                        onChange={(e) => setEnd(e.target.value)}
                        className="input-field"
                    />
                </div>

                <button onClick={handleApply} className="btn-primary w-full">
                    적용
                </button>
            </div>
        </div>
    );
}
```

### study-monitoring-frontend/components/common/Button.tsx
```typescript jsx
import { ButtonHTMLAttributes, ReactNode } from 'react';
import { Loader2 } from 'lucide-react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: 'primary' | 'secondary' | 'outline' | 'ghost';
    size?: 'sm' | 'md' | 'lg';
    loading?: boolean;
    icon?: ReactNode;
    children: ReactNode;
}

export default function Button({
                                   variant = 'primary',
                                   size = 'md',
                                   loading = false,
                                   icon,
                                   children,
                                   className = '',
                                   disabled,
                                   ...props
                               }: ButtonProps) {
    const baseStyles = 'inline-flex items-center justify-center font-medium rounded-lg transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed';

    const variantStyles = {
        primary: 'bg-primary-500 hover:bg-primary-600 text-white shadow-sky',
        secondary: 'bg-secondary-100 hover:bg-secondary-200 text-secondary-700',
        outline: 'border-2 border-primary-500 text-primary-600 hover:bg-primary-50',
        ghost: 'text-primary-600 hover:bg-primary-50',
    };

    const sizeStyles = {
        sm: 'px-3 py-1.5 text-sm',
        md: 'px-4 py-2',
        lg: 'px-6 py-3 text-lg',
    };

    return (
        <button
            className={`${baseStyles} ${variantStyles[variant]} ${sizeStyles[size]} ${className}`}
            disabled={disabled || loading}
            {...props}
        >
            {loading ? (
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
                icon && <span className="mr-2">{icon}</span>
            )}
            {children}
        </button>
    );
}
```

## Loading 컴포넌트
### study-monitoring-frontend/components/common/Loading.tsx
```typescript jsx
import { Loader2 } from 'lucide-react';

interface LoadingProps {
    text?: string;
    size?: 'sm' | 'md' | 'lg';
    fullScreen?: boolean;
}

export default function Loading({
                                    text = '로딩 중...',
                                    size = 'md',
                                    fullScreen = false
                                }: LoadingProps) {
    const sizeClasses = {
        sm: 'w-6 h-6',
        md: 'w-10 h-10',
        lg: 'w-16 h-16',
    };

    const content = (
        <div className="flex flex-col items-center justify-center space-y-4">
            <Loader2 className={`${sizeClasses[size]} text-primary-500 animate-spin`} />
            <p className="text-secondary-600">{text}</p>
        </div>
    );

    if (fullScreen) {
        return (
            <div className="fixed inset-0 bg-white/80 backdrop-blur-sm flex items-center justify-center z-50">
                {content}
            </div>
        );
    }

    return (
        <div className="flex items-center justify-center py-12">
            {content}
        </div>
    );
}
```

## ErrorMessage 컴포넌트
### study-monitoring-frontend/components/common/ErrorMessage.tsx
```typescript tsx
import { AlertCircle, RefreshCw } from 'lucide-react';
import Button from './Button';

interface ErrorMessageProps {
  message: string;
  onRetry?: () => void;
}

export default function ErrorMessage({ message, onRetry }: ErrorMessageProps) {
  return (
    <div className="card border-error/20 bg-error/5">
      <div className="flex items-start space-x-3">
        <AlertCircle className="w-6 h-6 text-error flex-shrink-0 mt-0.5" />
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-error mb-2">오류 발생</h3>
          <p className="text-secondary-700 mb-4">{message}</p>
          {onRetry && (
            <Button
              variant="outline"
              size="sm"
              icon={<RefreshCw className="w-4 h-4" />}
              onClick={onRetry}
            >
              다시 시도
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
```

## DateRangePicker 컴포넌트
### study-monitoring-frontend/components/common/DateRangePicker.tsx
```typescript tsx
'use client';

import { useState } from 'react';
import { Calendar } from 'lucide-react';

interface DateRangePickerProps {
  startDate: string;
  endDate: string;
  onChange: (start: string, end: string) => void;
}

export default function DateRangePicker({
  startDate,
  endDate,
  onChange,
}: DateRangePickerProps) {
  const [start, setStart] = useState(startDate);
  const [end, setEnd] = useState(endDate);

  const handleApply = () => {
    onChange(start, end);
  };

  // 빠른 선택 옵션
  const quickOptions = [
    { label: '최근 1시간', hours: 1 },
    { label: '최근 6시간', hours: 6 },
    { label: '최근 24시간', hours: 24 },
    { label: '최근 7일', hours: 24 * 7 },
    { label: '최근 30일', hours: 24 * 30 },
  ];

  const handleQuickSelect = (hours: number) => {
    const now = new Date();
    const past = new Date(now.getTime() - hours * 60 * 60 * 1000);
    
    const formatDate = (date: Date) => {
      return date.toISOString().slice(0, 16);
    };
    
    setStart(formatDate(past));
    setEnd(formatDate(now));
  };

  return (
    <div className="card">
      <div className="flex items-center space-x-2 mb-4">
        <Calendar className="w-5 h-5 text-primary-600" />
        <h3 className="font-semibold text-primary-700">기간 선택</h3>
      </div>

      {/* 빠른 선택 */}
      <div className="flex flex-wrap gap-2 mb-4">
        {quickOptions.map((option) => (
          <button
            key={option.label}
            onClick={() => handleQuickSelect(option.hours)}
            className="px-3 py-1.5 text-sm bg-primary-50 hover:bg-primary-100 text-primary-700 rounded-lg transition-colors"
          >
            {option.label}
          </button>
        ))}
      </div>

      {/* 날짜 입력 */}
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-secondary-700 mb-2">
            시작 시간
          </label>
          <input
            type="datetime-local"
            value={start}
            onChange={(e) => setStart(e.target.value)}
            className="input-field"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-secondary-700 mb-2">
            종료 시간
          </label>
          <input
            type="datetime-local"
            value={end}
            onChange={(e) => setEnd(e.target.value)}
            className="input-field"
          />
        </div>

        <button onClick={handleApply} className="btn-primary w-full">
          적용
        </button>
      </div>
    </div>
  );
}
```

## 메인 페이지(홈페이지)
### study-monitoring-frontend/app/page.tsx
```typescript jsx
import Link from 'next/link';
import { Activity, BarChart3, FileText, Heart } from 'lucide-react';

export default function HomePage() {
    return (
        <div className="min-h-[calc(100vh-8rem)] bg-gradient-sky">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
            {/* Hero Section */}
            <div className="text-center mb-16 fade-in">
    <h1 className="text-5xl font-bold text-primary-700 mb-4">
        Study Monitoring System
    </h1>
    <p className="text-xl text-secondary-600 mb-8">
        실시간으로 시스템 상태를 모니터링하고 분석합니다
    </p>
    <Link
    href="/dashboard"
    className="inline-flex items-center px-8 py-4 bg-primary-500 hover:bg-primary-600 text-white text-lg font-semibold rounded-lg shadow-sky transition-colors"
    >
    <Activity className="w-6 h-6 mr-2" />
        대시보드 시작하기
    </Link>
    </div>

    {/* Features Grid */}
    <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
    <FeatureCard
        icon={<Activity className="w-8 h-8" />}
    title="실시간 메트릭"
    description="TPS, Heap 사용률, CPU 등 핵심 메트릭을 실시간으로 모니터링"
    href="/metrics"
    />

    <FeatureCard
        icon={<BarChart3 className="w-8 h-8" />}
    title="통계 분석"
    description="시계열 데이터 분석 및 7가지 로그 통계 제공"
    href="/statistics"
    />

    <FeatureCard
        icon={<FileText className="w-8 h-8" />}
    title="로그 검색"
    description="Elasticsearch 기반 강력한 로그 검색 기능"
    href="/logs"
    />

    <FeatureCard
        icon={<Heart className="w-8 h-8" />}
    title="헬스체크"
    description="시스템 구성 요소의 상태를 확인"
    href="/health"
        />
        </div>

    {/* System Status */}
    <div className="mt-16 card text-center">
    <h2 className="text-2xl font-bold text-primary-700 mb-4">
        시스템 구성
    </h2>
        <div className="flex flex-wrap justify-center gap-4 text-secondary-600">
                    <div className="px-4 py-2 bg-primary-50 rounded-lg">
                        <span className="font-semibold">Backend:</span> Spring Boot 3.5.7
                    </div>
                    <div className="px-4 py-2 bg-primary-50 rounded-lg">
                        <span className="font-semibold">Frontend:</span> Next.js 15
                    </div>
                    <div className="px-4 py-2 bg-primary-50 rounded-lg">
                        <span className="font-semibold">Database:</span> PostgreSQL
                    </div>
                    <div className="px-4 py-2 bg-primary-50 rounded-lg">
                        <span className="font-semibold">Search:</span> Elasticsearch
                    </div>
                    <div className="px-4 py-2 bg-primary-50 rounded-lg">
                        <span className="font-semibold">Metrics:</span> Prometheus
                    </div>
                </div>
            </div>
        </div>
    </div>
    );
}

function FeatureCard({
                         icon,
                         title,
                         description,
                         href,
                     }: {
    icon: React.ReactNode;
    title: string;
    description: string;
    href: string;
    }) {
    return (
        <Link href={href}
    className="card hover:shadow-xl hover:scale-105 transition-all duration-300 cursor-pointer group"
    >
    <div className="text-primary-500 group-hover:text-primary-600 mb-4 transition-colors">
        {icon}
        </div>
        <h3 className="text-lg font-semibold text-primary-700 mb-2">{title}</h3>
        <p className="text-sm text-secondary-600">{description}</p>
        </Link>
    );
}
```

## 대쉬보드 페이지
### study-monitoring-frontend/app/dashboard/page.tsx
```typescript jsx
'use client';

import { useEffect, useState, useCallback, useRef } from 'react';
import { Activity, TrendingUp, AlertTriangle, Zap, Server, Clock } from 'lucide-react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import ProcessCard from '@/components/dashboard/ProcessCard';
import ErrorList from '@/components/dashboard/ErrorList';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

// --- [Type Definitions] ---
interface DashboardData {
    processes: Array<{
        processId: number;
        processName: string;
        processType: string;
        status: string;
        cpuUsage: number;
        memoryUsage: number;
        uptime: string;
        lastHealthCheck: string;
    }>;
    metrics: {
        engStudy: {
            tps: number | null;
            heapUsage: number | null;
            errorRate: number | null;
            responseTime: number | null;
        };
        monitoring: {
            tps: number | null;
            heapUsage: number | null;
            errorRate: number | null;
            responseTime: number | null;
        };
    };
    recentErrors: Array<{
        id: string;
        timestamp: string;
        logLevel: string;
        message: string;
        application: string;
    }>;
    logCounts: { [key: string]: number };
    statistics: {
        totalRequest: number;
        avgResponseTime: number;
        uptime: string;
    };
}

// 차트 데이터 포인트 타입
interface ChartPoint {
    timeStr: string; // X축 표시용 (HH:mm:ss)
    tps: number;
}

const MAX_DATA_POINTS = 30; // 차트에 유지할 최대 데이터 개수 (30개)

export default function DashboardPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // 현재 상태 데이터 (카드, 리스트용)
    const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);

    // 실시간 차트 데이터 (누적용)
    const [chartData, setChartData] = useState<ChartPoint[]>([]);

    // 1. 초기 데이터 및 주기적 데이터 로드
    const fetchDashboard = useCallback(async () => {
        try {
            const response = await fetch('/api/dashboard/overview');
            const result = await response.json();

            if (result.success) {
                const newData = result.data;
                setDashboardData(newData);

                // [핵심] 실시간 차트 데이터 구성 (Sliding Window)
                const now = new Date();
                const timeStr = now.toLocaleTimeString('ko-KR', {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                });

                setChartData(prev => {
                    // 기존 데이터에 새 포인트 추가
                    const newPoint = {
                        timeStr: timeStr,
                        tps: newData.metrics.engStudy.tps ?? 0
                    };
                    const newHistory = [...prev, newPoint];

                    // 최대 개수를 넘으면 가장 오래된 데이터 제거 (왼쪽 삭제)
                    return newHistory.slice(-MAX_DATA_POINTS);
                });
            }
        } catch (err: any) {
            console.error('Fetch error:', err);
            // 에러가 나도 기존 데이터는 유지 (화면 깜빡임 방지)
            if (!dashboardData) setError(err.message);
        } finally {
            setLoading(false);
        }
    }, [dashboardData]); // dashboardData 의존성 주의 (여기선 stale closure 방지를 위해 함수형 업데이트 사용했으므로 빈 배열 가능하지만, 안전하게)

    // 2. 주기 설정 (5초마다 갱신 - 실시간 느낌을 위해 주기를 짧게 설정)
    useEffect(() => {
        // 최초 로딩
        fetchDashboard();

        const interval = setInterval(() => {
            fetchDashboard();
        }, 5000); // 5초 단위 갱신

        return () => clearInterval(interval);
    }, []); // 의존성 배열 비움 (fetchDashboard 내부에서 함수형 업데이트 사용)


    if (loading && !dashboardData) {
        return (
            <div className="max-w-7xl mx-auto px-4 py-8">
                <Loading text="실시간 모니터링 연결 중..." />
            </div>
        );
    }

    if (error && !dashboardData) {
        return (
            <div className="max-w-7xl mx-auto px-4 py-8">
                <ErrorMessage message={error} onRetry={() => window.location.reload()} />
            </div>
        );
    }

    if (!dashboardData) return null;

    // 프로세스 데이터 매핑
    const processData = dashboardData.processes.map(p => ({
        name: p.processName,
        status: p.status.toLowerCase() as 'running' | 'stopped' | 'warning',
        uptime: p.uptime,
        cpu: p.cpuUsage,
        memory: p.memoryUsage,
        pid: p.processId
    }));

    // 에러 데이터 매핑
    const errorData = dashboardData.recentErrors.map(e => ({
        id: e.id,
        timestamp: e.timestamp,
        level: e.logLevel.toLowerCase() as 'critical' | 'error' | 'warning' | 'info',
        message: e.message,
        source: e.application
    }));

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

            {/* 헤더: 실시간 표시등 추가 */}
            <div className="flex justify-between items-end mb-8">
                <div>
                    <div className="flex items-center space-x-3 mb-2">
                        <h1 className="text-3xl font-bold text-primary-700">
                            시스템 대시보드
                        </h1>
                        {/* Live Indicator */}
                        <span className="flex items-center space-x-1 px-2 py-1 bg-green-100 text-green-700 text-xs font-bold rounded-full border border-green-200 animate-pulse">
                            <span className="relative flex h-2 w-2">
                              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                              <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                            </span>
                            <span>LIVE</span>
                        </span>
                    </div>
                    <p className="text-secondary-600">
                        실시간 인프라 및 애플리케이션 상태 감시
                    </p>
                </div>
                <div className="text-sm text-gray-500 flex items-center">
                    <Clock className="w-4 h-4 mr-1" />
                    마지막 갱신: {chartData.length > 0 ? chartData[chartData.length - 1].timeStr : '-'}
                </div>
            </div>

            {/* 메트릭 요약 카드 */}
            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <MetricCard
                    title="Eng-Study TPS"
                    value={(dashboardData.metrics.engStudy.tps ?? 0).toFixed(2)}
                    unit="req/s"
                    icon={<Zap className="w-6 h-6" />}
                    color="blue"
                    // 이전 값과 비교하여 증감 표시 로직 등을 추가할 수 있음
                    trend={(dashboardData.metrics.engStudy.tps || 0) > 5 ? 'Active' : 'Idle'}
                />
                <MetricCard
                    title="Monitoring TPS"
                    value={(dashboardData.metrics.monitoring.tps ?? 0).toFixed(2)}
                    unit="req/s"
                    icon={<Activity className="w-6 h-6" />}
                    color="green"
                    trend="Stable"
                />
                <MetricCard
                    title="평균 응답시간"
                    value={(dashboardData.statistics.avgResponseTime ?? 0).toFixed(0)}
                    unit="ms"
                    icon={<TrendingUp className="w-6 h-6" />}
                    color="purple"
                    trend="Avg"
                />
                <MetricCard
                    title="에러율 (Max)"
                    value={Math.max(
                        dashboardData.metrics.engStudy.errorRate ?? 0,
                        dashboardData.metrics.monitoring.errorRate ?? 0
                    ).toFixed(2)}
                    unit="%"
                    icon={<AlertTriangle className="w-6 h-6" />}
                    color="red"
                    trend="Realtime"
                    warning={(dashboardData.metrics.engStudy.errorRate || 0) > 1}
                />
            </div>

            {/* 프로세스 & 에러 */}
            <div className="grid lg:grid-cols-2 gap-6 mb-8">
                <ProcessCard processes={processData} />
                <ErrorList errors={errorData} maxItems={5} />
            </div>

            {/* 실시간 차트 & 로그 분포 */}
            <div className="grid lg:grid-cols-3 gap-6">
                {/* 실시간 트래픽 차트 */}
                <div className="lg:col-span-2">
                    <Card title="실시간 트래픽 모니터링 (Eng-Study)">
                        <div className="h-[300px] w-full">
                            {chartData.length > 0 ? (
                                <ResponsiveContainer width="100%" height="100%">
                                    <AreaChart data={chartData}>
                                        <defs>
                                            <linearGradient id="colorTps" x1="0" y1="0" x2="0" y2="1">
                                                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8}/>
                                                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                                            </linearGradient>
                                        </defs>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />
                                        <XAxis
                                            dataKey="timeStr"
                                            style={{ fontSize: '11px', fill: '#6b7280' }}
                                            tickMargin={10}
                                        />
                                        <YAxis
                                            style={{ fontSize: '11px', fill: '#6b7280' }}
                                            domain={[0, 'auto']} // Y축 자동 스케일링
                                        />
                                        <Tooltip
                                            contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                            formatter={(value: number) => [value.toFixed(2), 'TPS']}
                                            labelStyle={{ color: '#6b7280', marginBottom: '0.25rem' }}
                                        />
                                        <Area
                                            type="monotone"
                                            dataKey="tps"
                                            stroke="#3b82f6"
                                            strokeWidth={2}
                                            fillOpacity={1}
                                            fill="url(#colorTps)"
                                            isAnimationActive={true} // 애니메이션 활성화
                                            animationDuration={1000} // 부드러운 연결을 위한 시간
                                        />
                                    </AreaChart>
                                </ResponsiveContainer>
                            ) : (
                                <div className="h-full flex items-center justify-center text-gray-400">
                                    데이터 수집 중...
                                </div>
                            )}
                        </div>
                    </Card>
                </div>

                {/* 로그 레벨 분포 */}
                <Card title="로그 레벨 현황">
                    <div className="space-y-4">
                        {dashboardData.logCounts && Object.keys(dashboardData.logCounts).length > 0 ? (
                            Object.entries(dashboardData.logCounts).map(([level, count]) => (
                                <div key={level} className="flex items-center justify-between p-2 hover:bg-gray-50 rounded transition-colors">
                                    <div className="flex items-center space-x-3">
                                        <div className={`w-3 h-3 rounded-full ${
                                            level === 'ERROR' ? 'bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.6)]' : // 에러는 글로우 효과
                                                level === 'WARN' ? 'bg-yellow-500' :
                                                    level === 'INFO' ? 'bg-blue-500' :
                                                        'bg-gray-500'
                                        }`} />
                                        <span className="text-sm font-medium text-gray-700">{level}</span>
                                    </div>
                                    <span className="text-lg font-bold text-primary-700 font-mono">
                                        {count.toLocaleString()}
                                    </span>
                                </div>
                            ))
                        ) : (
                            <div className="py-8 text-center text-gray-400 text-sm">
                                로그 데이터 없음
                            </div>
                        )}
                    </div>

                    <div className="mt-6 pt-4 border-t border-gray-200">
                        <div className="flex justify-between items-center">
                            <span className="text-sm text-secondary-600">누적 로그</span>
                            <span className="text-xl font-bold text-primary-700">
                                {Object.values(dashboardData.logCounts || {})
                                    .reduce((sum, count) => sum + count, 0)
                                    .toLocaleString()}
                            </span>
                        </div>
                    </div>
                </Card>
            </div>

            {/* 시스템 통계 */}
            <div className="mt-8">
                <Card title="인프라 상태 요약">
                    <div className="grid md:grid-cols-3 gap-6">
                        <div className="text-center p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm text-secondary-600 mb-2">총 처리 요청</p>
                            <p className="text-3xl font-bold text-primary-700 font-mono">
                                {(dashboardData.statistics.totalRequest ?? 0).toLocaleString()}
                            </p>
                        </div>
                        <div className="text-center p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm text-secondary-600 mb-2">평균 응답속도</p>
                            <p className="text-3xl font-bold text-primary-700 font-mono">
                                {(dashboardData.statistics.avgResponseTime ?? 0).toFixed(0)}
                                <span className="text-lg text-secondary-500 ml-1">ms</span>
                            </p>
                        </div>
                        <div className="text-center p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm text-secondary-600 mb-2">가동 시간</p>
                            <div className="flex items-center justify-center space-x-2">
                                <Server className="w-5 h-5 text-green-500" />
                                <p className="text-2xl font-bold text-primary-700 font-mono">
                                    {dashboardData.statistics.uptime || '0s'}
                                </p>
                            </div>
                        </div>
                    </div>
                </Card>
            </div>
        </div>
    );
}

// Helper Components
function MetricCard({ title, value, unit, icon, color, trend, warning = false }: any) {
    const colorClasses: any = {
        blue: 'from-blue-400 to-blue-600 shadow-blue-200',
        green: 'from-green-400 to-green-600 shadow-green-200',
        red: 'from-red-400 to-red-600 shadow-red-200',
        purple: 'from-purple-400 to-purple-600 shadow-purple-200',
    };

    return (
        <Card className={`transition-all duration-300 hover:shadow-lg ${warning ? 'border-2 border-red-300 bg-red-50 animate-pulse' : ''}`}>
            <div className="flex items-start justify-between mb-4">
                <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${colorClasses[color]} flex items-center justify-center text-white shadow-lg transform transition-transform hover:scale-110`}>
                    {icon}
                </div>
                {trend && (
                    <span className="text-xs font-bold px-2 py-1 bg-white/60 rounded-lg text-secondary-600 border border-gray-100 backdrop-blur-sm">
                        {trend}
                    </span>
                )}
            </div>
            <h3 className="text-sm font-medium text-secondary-600 mb-1">{title}</h3>
            <div className="flex items-baseline space-x-1">
                <span className="text-2xl font-bold text-primary-900 tracking-tight font-mono">{value}</span>
                <span className="text-sm text-secondary-500 font-medium">{unit}</span>
            </div>
        </Card>
    );
}
```

## 메트릭 페이지
### study-monitoring-frontend/app/metrics/page.tsx
```typescript jsx
'use client';

import { useEffect, useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import { getCurrentMetrics } from '@/lib/api/metrics';
import { Activity, Cpu, Database, Zap, RefreshCw } from 'lucide-react';

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

export default function MetricsPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [engStudyMetrics, setEngStudyMetrics] = useState<CurrentMetrics | null>(null);
    const [monitoringMetrics, setMonitoringMetrics] = useState<CurrentMetrics | null>(null);

    const fetchMetrics = async () => {
        setLoading(true);
        setError(null);

        try {
            const [engStudy, monitoring] = await Promise.all([
                getCurrentMetrics({ application: 'eng-study' }),
                getCurrentMetrics({ application: 'monitoring' }),
            ]);

            setEngStudyMetrics(engStudy);
            setMonitoringMetrics(monitoring);
        } catch (err: any) {
            setError(err.message || '메트릭 데이터를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchMetrics();

        // 15초마다 자동 새로고침
        const interval = setInterval(fetchMetrics, 15000);
        return () => clearInterval(interval);
    }, []);

    if (loading && !engStudyMetrics) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <Loading text="메트릭 데이터를 불러오는 중..." />
            </div>
        );
    }

    if (error && !engStudyMetrics) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <ErrorMessage message={error} onRetry={fetchMetrics} />
            </div>
        );
    }

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* 헤더 */}
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-primary-700 mb-2">
                        실시간 메트릭
                    </h1>
                    <p className="text-secondary-600">
                        애플리케이션의 핵심 성능 지표를 실시간으로 모니터링합니다
                    </p>
                </div>
                <Button
                    variant="outline"
                    icon={<RefreshCw className="w-4 h-4" />}
                    onClick={fetchMetrics}
                    loading={loading}
                >
                    새로고침
                </Button>
            </div>

            {/* Eng-Study 메트릭 */}
            {engStudyMetrics && (
                <div className="mb-8">
                    <h2 className="text-xl font-semibold text-primary-700 mb-4">
                        Eng-Study Application
                    </h2>
                    <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
                        <MetricCard
                            icon={<Zap className="w-8 h-8" />}
                            title="TPS"
                            value={engStudyMetrics.metrics.tps.toFixed(2)}
                            unit="req/s"
                            color="blue"
                        />
                        <MetricCard
                            icon={<Database className="w-8 h-8" />}
                            title="Heap 사용률"
                            value={engStudyMetrics.metrics.heapUsage.toFixed(1)}
                            unit="%"
                            color="green"
                            warning={engStudyMetrics.metrics.heapUsage > 80}
                        />
                        <MetricCard
                            icon={<Activity className="w-8 h-8" />}
                            title="에러율"
                            value={engStudyMetrics.metrics.errorRate.toFixed(2)}
                            unit="%"
                            color="red"
                            warning={engStudyMetrics.metrics.errorRate > 1}
                        />
                        <MetricCard
                            icon={<Cpu className="w-8 h-8" />}
                            title="CPU 사용률"
                            value={engStudyMetrics.metrics.cpuUsage.toFixed(1)}
                            unit="%"
                            color="purple"
                            warning={engStudyMetrics.metrics.cpuUsage > 80}
                        />
                    </div>
                </div>
            )}

            {/* Monitoring 메트릭 */}
            {monitoringMetrics && (
                <div>
                    <h2 className="text-xl font-semibold text-primary-700 mb-4">
                        Monitoring Application
                    </h2>
                    <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
                        <MetricCard
                            icon={<Zap className="w-8 h-8" />}
                            title="TPS"
                            value={monitoringMetrics.metrics.tps.toFixed(2)}
                            unit="req/s"
                            color="blue"
                        />
                        <MetricCard
                            icon={<Database className="w-8 h-8" />}
                            title="Heap 사용률"
                            value={monitoringMetrics.metrics.heapUsage.toFixed(1)}
                            unit="%"
                            color="green"
                            warning={monitoringMetrics.metrics.heapUsage > 80}
                        />
                        <MetricCard
                            icon={<Activity className="w-8 h-8" />}
                            title="에러율"
                            value={monitoringMetrics.metrics.errorRate.toFixed(2)}
                            unit="%"
                            color="red"
                            warning={monitoringMetrics.metrics.errorRate > 1}
                        />
                        <MetricCard
                            icon={<Cpu className="w-8 h-8" />}
                            title="CPU 사용률"
                            value={monitoringMetrics.metrics.cpuUsage.toFixed(1)}
                            unit="%"
                            color="purple"
                            warning={monitoringMetrics.metrics.cpuUsage > 80}
                        />
                    </div>
                </div>
            )}
        </div>
    );
}

function MetricCard({
                        icon,
                        title,
                        value,
                        unit,
                        color,
                        warning = false,
                    }: {
    icon: React.ReactNode;
    title: string;
    value: string;
    unit: string;
    color: 'blue' | 'green' | 'red' | 'purple';
    warning?: boolean;
}) {
    const colorClasses = {
        blue: 'from-blue-400 to-blue-600',
        green: 'from-green-400 to-green-600',
        red: 'from-red-400 to-red-600',
        purple: 'from-purple-400 to-purple-600',
    };

    return (
        <Card className={warning ? 'border-2 border-warning animate-pulse' : ''}>
            <div className={`w-12 h-12 rounded-lg bg-gradient-to-br ${colorClasses[color]} flex items-center justify-center text-white mb-4`}>
                {icon}
            </div>
            <h3 className="text-sm font-medium text-secondary-600 mb-2">{title}</h3>
            <div className="flex items-baseline space-x-2">
                <span className="text-3xl font-bold text-primary-700">{value}</span>
                <span className="text-lg text-secondary-500">{unit}</span>
            </div>
            {warning && (
                <div className="mt-3 px-3 py-1 bg-warning/10 text-warning text-sm font-medium rounded">
                    ⚠️ 임계치 초과
                </div>
            )}
        </Card>
    );
}
```

## 통계 메인 페이지
### study-monitoring-frontend-app/statistics/page.tsx
```typescript jsx
import Link from 'next/link';
import { BarChart3, TrendingUp, Database, Shield, AlertCircle, FileText, Activity, Lock } from 'lucide-react';

const statisticsPages = [
    {
        title: '시계열 통계',
        description: 'Prometheus + PostgreSQL 기반 시계열 데이터 분석',
        href: '/statistics/timeseries',
        icon: TrendingUp,
        color: 'from-blue-400 to-blue-600',
    },
    {
        title: '로그 통계',
        description: '애플리케이션 로그 레벨별 통계 및 시간대별 분포',
        href: '/statistics/logs',
        icon: FileText,
        color: 'from-green-400 to-green-600',
    },
    {
        title: '접근 로그 통계',
        description: 'HTTP 메서드, 상태코드, 응답시간 분석',
        href: '/statistics/access-logs',
        icon: Activity,
        color: 'from-purple-400 to-purple-600',
    },
    {
        title: '에러 로그 통계',
        description: '에러 타입, 심각도별 통계 및 발생 빈도 분석',
        href: '/statistics/error-logs',
        icon: AlertCircle,
        color: 'from-red-400 to-red-600',
    },
    {
        title: '성능 메트릭 통계',
        description: 'CPU, 메모리, JVM 성능 지표 분석',
        href: '/statistics/performance-metrics',
        icon: BarChart3,
        color: 'from-yellow-400 to-yellow-600',
    },
    {
        title: '데이터베이스 로그 통계',
        description: '쿼리 실행시간, Operation별, 테이블별 통계',
        href: '/statistics/database-logs',
        icon: Database,
        color: 'from-indigo-400 to-indigo-600',
    },
    {
        title: '감사 로그 통계',
        description: '사용자 액션, 이벤트 카테고리, 성공/실패율 분석',
        href: '/statistics/audit-logs',
        icon: Shield,
        color: 'from-teal-400 to-teal-600',
    },
    {
        title: '보안 로그 통계',
        description: '위협 레벨, 공격 타입, 차단 통계 분석',
        href: '/statistics/security-logs',
        icon: Lock,
        color: 'from-pink-400 to-pink-600',
    },
];

export default function StatisticsPage() {
    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    통계 분석
                </h1>
                <p className="text-secondary-600">
                    다양한 로그 및 메트릭 통계를 확인하고 분석합니다
                </p>
            </div>

            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
                {statisticsPages.map((page) => {
                    const Icon = page.icon;
                    return (
                        <Link
                            key={page.href}
                            href={page.href}
                            className="card hover:shadow-xl hover:scale-105 transition-all duration-300 cursor-pointer group"
                        >
                            <div className={`w-14 h-14 rounded-lg bg-gradient-to-br ${page.color} flex items-center justify-center text-white mb-4 group-hover:scale-110 transition-transform`}>
                                <Icon className="w-7 h-7" />
                            </div>
                            <h3 className="text-lg font-semibold text-primary-700 mb-2">
                                {page.title}
                            </h3>
                            <p className="text-sm text-secondary-600">
                                {page.description}
                            </p>
                        </Link>
                    );
                })}
            </div>
        </div>
    );
}
```

# 통계 상세 페이지
## 시계열 통계 페이지
### study-monitoring-frontend/app/statistics/timeseries/page.tsx
```typescript jsx
'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getTimeSeriesStatistics } from '@/lib/api/statistics';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Search, Download } from 'lucide-react';

export default function TimeSeriesStatisticsPage() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<any>(null);

  // 쿼리 파라미터
  const [metricType, setMetricType] = useState('TPS');
  const [timePeriod, setTimePeriod] = useState('HOUR');
  const [aggregationType, setAggregationType] = useState('AVG');
  const [startTime, setStartTime] = useState(() => {
    const date = new Date();
    date.setHours(date.getHours() - 24);
    return date.toISOString().slice(0, 16);
  });
  const [endTime, setEndTime] = useState(() => {
    return new Date().toISOString().slice(0, 16);
  });

  const handleSearch = async () => {
    setLoading(true);
    setError(null);

    try {
      const result = await getTimeSeriesStatistics({
        metricType,
        startTime: startTime.replace('T', ' ') + ':00',
        endTime: endTime.replace('T', ' ') + ':00',
        timePeriod,
        aggregationType,
      });

      setData(result);
    } catch (err: any) {
      setError(err.message || '통계 데이터를 불러오는데 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  const handleDateRangeChange = (start: string, end: string) => {
    setStartTime(start);
    setEndTime(end);
  };

  // CSV 다운로드
  const handleDownloadCSV = () => {
    if (!data?.data) return;

    const csvContent = [
      ['Timestamp', 'Value', 'Min', 'Max', 'Sample Count'].join(','),
      ...data.data.map((item: any) => 
        [item.timestamp, item.value, item.minValue, item.maxValue, item.sampleCount].join(',')
      ),
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `timeseries_${metricType}_${Date.now()}.csv`;
    a.click();
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-primary-700 mb-2">
          시계열 통계
        </h1>
        <p className="text-secondary-600">
          Prometheus + PostgreSQL 기반 시계열 데이터 분석
        </p>
      </div>

      <div className="grid lg:grid-cols-3 gap-6 mb-6">
        {/* 검색 조건 */}
        <div className="lg:col-span-1">
          <Card title="검색 조건">
            <div className="space-y-4">
              {/* 메트릭 타입 */}
              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  메트릭 타입
                </label>
                <select
                  value={metricType}
                  onChange={(e) => setMetricType(e.target.value)}
                  className="input-field"
                >
                  <option value="TPS">TPS</option>
                  <option value="HEAP_USAGE">Heap 사용률</option>
                  <option value="ERROR_RATE">에러율</option>
                  <option value="CPU_USAGE">CPU 사용률</option>
                </select>
              </div>

              {/* 시간 주기 */}
              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  시간 주기
                </label>
                <select
                  value={timePeriod}
                  onChange={(e) => setTimePeriod(e.target.value)}
                  className="input-field"
                >
                  <option value="MINUTE">분</option>
                  <option value="HOUR">시간</option>
                  <option value="DAY">일</option>
                  <option value="WEEK">주</option>
                  <option value="MONTH">월</option>
                </select>
              </div>

              {/* 집계 방식 */}
              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  집계 방식
                </label>
                <select
                  value={aggregationType}
                  onChange={(e) => setAggregationType(e.target.value)}
                  className="input-field"
                >
                  <option value="AVG">평균</option>
                  <option value="SUM">합계</option>
                  <option value="MIN">최소</option>
                  <option value="MAX">최대</option>
                  <option value="COUNT">카운트</option>
                </select>
              </div>

              <Button
                variant="primary"
                icon={<Search className="w-4 h-4" />}
                onClick={handleSearch}
                loading={loading}
                className="w-full"
              >
                조회
              </Button>
            </div>
          </Card>

          {/* 날짜 범위 선택 */}
          <div className="mt-6">
            <DateRangePicker
              startDate={startTime}
              endDate={endTime}
              onChange={handleDateRangeChange}
            />
          </div>
        </div>

        {/* 결과 표시 */}
        <div className="lg:col-span-2">
          {loading && <Loading text="데이터를 불러오는 중..." />}
          
          {error && <ErrorMessage message={error} onRetry={handleSearch} />}
          
          {!loading && !error && data && (
            <>
              {/* 요약 정보 */}
              <Card
                title="요약 정보"
                headerAction={
                  <Button
                    variant="outline"
                    size="sm"
                    icon={<Download className="w-4 h-4" />}
                    onClick={handleDownloadCSV}
                  >
                    CSV 다운로드
                  </Button>
                }
              >
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div>
                    <p className="text-sm text-secondary-600">메트릭</p>
                    <p className="text-lg font-semibold text-primary-700">{data.metricType}</p>
                  </div>
                  <div>
                    <p className="text-sm text-secondary-600">주기</p>
                    <p className="text-lg font-semibold text-primary-700">{data.timePeriod}</p>
                  </div>
                  <div>
                    <p className="text-sm text-secondary-600">집계</p>
                    <p className="text-lg font-semibold text-primary-700">{data.aggregationType}</p>
                  </div>
                  <div>
                    <p className="text-sm text-secondary-600">데이터 소스</p>
                    <p className="text-lg font-semibold text-primary-700">{data.dataSource}</p>
                  </div>
                </div>
              </Card>

              {/* 차트 */}
              <Card title="시계열 차트" className="mt-6">
                <ResponsiveContainer width="100%" height={400}>
                  <LineChart data={data.data}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis
                      dataKey="timestamp"
                      stroke="#64748b"
                      style={{ fontSize: '12px' }}
                    />
                    <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#fff',
                        border: '1px solid #e2e8f0',
                        borderRadius: '8px',
                      }}
                    />
                    <Legend />
                    <Line
                      type="monotone"
                      dataKey="value"
                      stroke="#0ea5e9"
                      strokeWidth={2}
                      dot={{ fill: '#0ea5e9', r: 4 }}
                      activeDot={{ r: 6 }}
                      name="값"
                    />
                    {data.data[0]?.minValue !== null && (
                      <Line
                        type="monotone"
                        dataKey="minValue"
                        stroke="#10b981"
                        strokeWidth={1}
                        strokeDasharray="5 5"
                        dot={false}
                        name="최소"
                      />
                    )}
                    {data.data[0]?.maxValue !== null && (
                      <Line
                        type="monotone"
                        dataKey="maxValue"
                        stroke="#ef4444"
                        strokeWidth={1}
                        strokeDasharray="5 5"
                        dot={false}
                        name="최대"
                      />
                    )}
                  </LineChart>
                </ResponsiveContainer>
              </Card>

              {/* 데이터 테이블 */}
              <Card title="상세 데이터" className="mt-6">
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                          시간
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                          값
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                          최소
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                          최대
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                          샘플 수
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {data.data.slice(0, 10).map((item: any, index: number) => (
                        <tr key={index}>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                            {item.timestamp}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-primary-700">
                            {item.value.toFixed(2)}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {item.minValue?.toFixed(2) || '-'}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {item.maxValue?.toFixed(2) || '-'}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {item.sampleCount || '-'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {data.data.length > 10 && (
                    <p className="text-sm text-secondary-600 text-center py-4">
                      총 {data.data.length}개 중 10개 표시 (CSV 다운로드로 전체 데이터 확인)
                    </p>
                  )}
                </div>
              </Card>
            </>
          )}

          {!loading && !error && !data && (
            <Card>
              <div className="text-center py-12">
                <p className="text-secondary-600">
                  검색 조건을 설정하고 조회 버튼을 클릭하세요
                </p>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
```

## 로그 통계 페이지
### study-monitoring-frontend/app/statistics/logs/page.tsx
```typescript jsx
'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getLogStatistics } from '@/lib/api/statistics';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { Search, FileText } from 'lucide-react';

const COLORS = ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

export default function LogStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [logLevel, setLogLevel] = useState('');
    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return date.toISOString().slice(0, 16);
    });
    const [endTime, setEndTime] = useState(() => {
        return new Date().toISOString().slice(0, 16);
    });

    const handleSearch = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await getLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
                logLevel: logLevel || undefined,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || '로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    // 로그 레벨별 카운트를 차트 데이터로 변환
    const logCountsChartData = data?.logCounts
        ? Object.entries(data.logCounts).map(([level, count]) => ({
            level,
            count,
        }))
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    로그 통계
                </h1>
                <p className="text-secondary-600">
                    애플리케이션 로그 레벨별 통계 및 시간대별 분포
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                {/* 검색 조건 */}
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    로그 레벨 (선택)
                                </label>
                                <select
                                    value={logLevel}
                                    onChange={(e) => setLogLevel(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="">전체</option>
                                    <option value="DEBUG">DEBUG</option>
                                    <option value="INFO">INFO</option>
                                    <option value="WARN">WARN</option>
                                    <option value="ERROR">ERROR</option>
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    시간 주기
                                </label>
                                <select
                                    value={timePeriod}
                                    onChange={(e) => setTimePeriod(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="MINUTE">분</option>
                                    <option value="HOUR">시간</option>
                                    <option value="DAY">일</option>
                                </select>
                            </div>

                            <Button
                                variant="primary"
                                icon={<Search className="w-4 h-4" />}
                                onClick={handleSearch}
                                loading={loading}
                                className="w-full"
                            >
                                조회
                            </Button>
                        </div>
                    </Card>

                    <div className="mt-6">
                        <DateRangePicker
                            startDate={startTime}
                            endDate={endTime}
                            onChange={handleDateRangeChange}
                        />
                    </div>
                </div>

                {/* 결과 표시 */}
                <div className="lg:col-span-2">
                    {loading && <Loading text="데이터를 불러오는 중..." />}

                    {error && <ErrorMessage message={error} onRetry={handleSearch} />}

                    {!loading && !error && data && (
                        <>
                            {/* 로그 레벨별 카운트 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="로그 레벨별 카운트">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <PieChart>
                                            <Pie
                                                data={logCountsChartData}
                                                dataKey="count"
                                                nameKey="level"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={100}
                                                label={(entry) => `${entry.level}: ${entry.count}`}
                                            >
                                                {logCountsChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>

                                <Card title="로그 레벨 통계">
                                    <div className="space-y-3">
                                        {Object.entries(data.logCounts || {}).map(([level, count]) => (
                                            <div key={level} className="flex justify-between items-center p-3 bg-primary-50 rounded-lg">
                                                <span className="font-medium text-primary-700">{level}</span>
                                                <span className="text-2xl font-bold text-primary-600">{count as number}</span>
                                            </div>
                                        ))}
                                    </div>
                                </Card>
                            </div>

                            {/* 시간대별 분포 */}
                            <Card title="시간대별 로그 분포">
                                <ResponsiveContainer width="100%" height={400}>
                                    <BarChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '12px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: '#fff',
                                                border: '1px solid #e2e8f0',
                                                borderRadius: '8px',
                                            }}
                                        />
                                        <Legend />
                                        <Bar dataKey="count" fill="#0ea5e9" name="로그 수" />
                                    </BarChart>
                                </ResponsiveContainer>
                            </Card>
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <FileText className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
                                <p className="text-secondary-600">
                                    검색 조건을 설정하고 조회 버튼을 클릭하세요
                                </p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
}
```

## 접근 로그 통계 페이지
### study-monitoring-frontend/app/statistics/access-logs/page.tsx
```typescript jsx
'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getAccessLogStatistics } from '@/lib/api/statistics';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell } from 'recharts';
import { Search, Activity } from 'lucide-react';

const COLORS = ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

export default function AccessLogStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return date.toISOString().slice(0, 16);
    });
    const [endTime, setEndTime] = useState(() => {
        return new Date().toISOString().slice(0, 16);
    });

    const handleSearch = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await getAccessLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || '접근 로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    const methodChartData = data?.methodCounts
        ? Object.entries(data.methodCounts).map(([method, count]) => ({
            method,
            count,
        }))
        : [];

    const statusCodeChartData = data?.statusCodeCounts
        ? Object.entries(data.statusCodeCounts).map(([code, count]) => ({
            code,
            count,
        }))
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    접근 로그 통계
                </h1>
                <p className="text-secondary-600">
                    HTTP 메서드, 상태코드, 응답시간 분석
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    시간 주기
                                </label>
                                <select
                                    value={timePeriod}
                                    onChange={(e) => setTimePeriod(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="MINUTE">분</option>
                                    <option value="HOUR">시간</option>
                                    <option value="DAY">일</option>
                                </select>
                            </div>

                            <Button
                                variant="primary"
                                icon={<Search className="w-4 h-4"/>}
                                onClick={handleSearch}
                                loading={loading}
                                className="w-full"
                            >
                                조회
                            </Button>
                        </div>
                    </Card>

                    <div className="mt-6">
                        <DateRangePicker
                            startDate={startTime}
                            endDate={endTime}
                            onChange={handleDateRangeChange}
                        />
                    </div>
                </div>

                <div className="lg:col-span-2">
                    {loading && <Loading text="데이터를 불러오는 중..."/>}

                    {error && <ErrorMessage message={error} onRetry={handleSearch}/>}

                    {!loading && !error && data && (
                        <>
                            {/* 요약 카드 */}
                            <div className="grid md:grid-cols-3 gap-4 mb-6">
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">평균 응답시간</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {data.avgResponseTime?.toFixed(0) || 0}
                                        <span className="text-lg text-secondary-500 ml-1">ms</span>
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">총 요청 수</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {(Object.values(data.methodCounts || {}).reduce((a: any, b: any) => a + b, 0) as number)}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">에러율</p>
                                    <p className="text-3xl font-bold text-error">
                                        {(() => {
                                            const total = Object.values(data.statusCodeCounts || {}).reduce((a: any, b: any) => a + b, 0) as number;
                                            const errors = Object.entries(data.statusCodeCounts || {})
                                                .filter(([code]) => code.startsWith('5'))
                                                .reduce((sum, [, count]) => sum + (count as number), 0);
                                            return total > 0 ? ((errors / total) * 100).toFixed(2) : '0.00';
                                        })()}
                                        <span className="text-lg text-secondary-500 ml-1">%</span>
                                    </p>
                                </Card>
                            </div>

                            {/* HTTP 메서드 & 상태코드 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="HTTP 메서드별 요청 수">
                                    <ResponsiveContainer width="100%" height={250}>
                                        <PieChart>
                                            <Pie
                                                data={methodChartData}
                                                dataKey="count"
                                                nameKey="method"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={80}
                                                label
                                            >
                                                {methodChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]}/>
                                                ))}
                                            </Pie>
                                            <Tooltip/>
                                            <Legend/>
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>

                                <Card title="상태코드별 분포">
                                    <ResponsiveContainer width="100%" height={250}>
                                        <BarChart data={statusCodeChartData}>
                                            <CartesianGrid strokeDasharray="3 3"/>
                                            <XAxis dataKey="code"/>
                                            <YAxis/>
                                            <Tooltip/>
                                            <Bar dataKey="count" fill="#0ea5e9"/>
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Card>
                            </div>

                            {/* 시간대별 분포 */}
                            <Card title="시간대별 접근 로그 분포">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0"/>
                                        <XAxis dataKey="timestamp" stroke="#64748b" style={{fontSize: '12px'}}/>
                                        <YAxis yAxisId="left" stroke="#64748b" style={{fontSize: '12px'}}/>
                                        <YAxis yAxisId="right" orientation="right" stroke="#64748b"
                                               style={{fontSize: '12px'}}/>
                                        <Tooltip/>
                                        <Legend/>
                                        <Line
                                            yAxisId="left"
                                            type="monotone"
                                            dataKey="requestCount"
                                            stroke="#0ea5e9"
                                            strokeWidth={2}
                                            name="요청 수"
                                        />
                                        <Line
                                            yAxisId="right"
                                            type="monotone"
                                            dataKey="avgResponseTime"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            name="평균 응답시간 (ms)"
                                        />
                                        <Line
                                            yAxisId="left"
                                            type="monotone"
                                            dataKey="errorCount"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            name="에러 수"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>
                        </>
                    )}
                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <Activity className="w-16 h-16 text-secondary-300 mx-auto mb-4"/>
                                <p className="text-secondary-600">
                                    검색 조건을 설정하고 조회 버튼을 클릭하세요
                                </p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
}
```

## 에러 로그 통계 페이지
### study-monitoring-frontend/app/statistics/error-logs/page.tsx
```typescript jsx
'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getErrorLogStatistics } from '@/lib/api/statistics';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line } from 'recharts';
import { Search, AlertCircle } from 'lucide-react';

const COLORS = ['#ef4444', '#f59e0b', '#10b981', '#0ea5e9', '#8b5cf6'];

export default function ErrorLogStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return date.toISOString().slice(0, 16);
    });
    const [endTime, setEndTime] = useState(() => {
        return new Date().toISOString().slice(0, 16);
    });

    const handleSearch = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await getErrorLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || '에러 로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    const errorTypeChartData = data?.errorTypeCounts
        ? Object.entries(data.errorTypeCounts).map(([type, count]) => ({
            type,
            count,
        }))
        : [];

    const severityChartData = data?.severityCounts
        ? Object.entries(data.severityCounts).map(([severity, count]) => ({
            severity,
            count,
        }))
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    에러 로그 통계
                </h1>
                <p className="text-secondary-600">
                    에러 타입, 심각도별 통계 및 발생 빈도 분석
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    시간 주기
                                </label>
                                <select
                                    value={timePeriod}
                                    onChange={(e) => setTimePeriod(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="MINUTE">분</option>
                                    <option value="HOUR">시간</option>
                                    <option value="DAY">일</option>
                                </select>
                            </div>

                            <Button
                                variant="primary"
                                icon={<Search className="w-4 h-4" />}
                                onClick={handleSearch}
                                loading={loading}
                                className="w-full"
                            >
                                조회
                            </Button>
                        </div>
                    </Card>

                    <div className="mt-6">
                        <DateRangePicker
                            startDate={startTime}
                            endDate={endTime}
                            onChange={handleDateRangeChange}
                        />
                    </div>
                </div>

                <div className="lg:col-span-2">
                    {loading && <Loading text="데이터를 불러오는 중..." />}

                    {error && <ErrorMessage message={error} onRetry={handleSearch} />}

                    {!loading && !error && data && (
                        <>
                            {/* 요약 카드 */}
                            <div className="grid md:grid-cols-3 gap-4 mb-6">
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">총 에러 수</p>
                                    <p className="text-3xl font-bold text-error">
                                        {(Object.values(data.errorTypeCounts || {}).reduce((a: any, b: any) => a + b, 0) as number)}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">에러 타입 수</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {Object.keys(data.errorTypeCounts || {}).length}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">Critical 에러</p>
                                    <p className="text-3xl font-bold text-error">
                                        {data.severityCounts?.CRITICAL || 0}
                                    </p>
                                </Card>
                            </div>

                            {/* 에러 타입 & 심각도 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="에러 타입별 분포">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <BarChart data={errorTypeChartData} layout="vertical">
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis type="number" />
                                            <YAxis dataKey="type" type="category" width={150} style={{ fontSize: '11px' }} />
                                            <Tooltip />
                                            <Bar dataKey="count" fill="#ef4444" />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Card>

                                <Card title="심각도별 분포">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <PieChart>
                                            <Pie
                                                data={severityChartData}
                                                dataKey="count"
                                                nameKey="severity"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={100}
                                                label={(entry) => `${entry.severity}: ${entry.count}`}
                                            >
                                                {severityChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>
                            </div>

                            {/* 시간대별 에러 발생 추이 */}
                            <Card title="시간대별 에러 발생 추이">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis dataKey="timestamp" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: '#fff',
                                                border: '1px solid #e2e8f0',
                                                borderRadius: '8px',
                                            }}
                                        />
                                        <Legend />
                                        <Line
                                            type="monotone"
                                            dataKey="errorCount"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            name="에러 수"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            {/* 에러 타입별 상세 테이블 */}
                            <Card title="에러 타입별 상세 정보" className="mt-6">
                                <div className="overflow-x-auto">
                                    <table className="min-w-full divide-y divide-gray-200">
                                        <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                에러 타입
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                발생 횟수
                                            </th>
                                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                비율
                                            </th>
                                        </tr>
                                        </thead>
                                        <tbody className="bg-white divide-y divide-gray-200">
                                        {errorTypeChartData.map((item: any, index: number) => {
                                            const total = errorTypeChartData.reduce((sum, i: any) => sum + i.count, 0);
                                            const percentage = ((item.count / total) * 100).toFixed(1);
                                            return (
                                                <tr key={index}>
                                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                                        {item.type}
                                                    </td>
                                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-error font-semibold">
                                                        {item.count}
                                                    </td>
                                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                        {percentage}%
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                        </tbody>
                                    </table>
                                </div>
                            </Card>
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <AlertCircle className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
                                <p className="text-secondary-600">
                                    검색 조건을 설정하고 조회 버튼을 클릭하세요
                                </p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
}
```

## 성능 메트릭 통계 페이지
### study-monitoring-frontend/app/statistics/performance-metrics/page.tsx
```typescript jsx
'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getPerformanceMetricsStatistics } from '@/lib/api/statistics';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, AreaChart, Area } from 'recharts';
import { Search, Activity, Cpu, Database } from 'lucide-react';

export default function PerformanceMetricsStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return date.toISOString().slice(0, 16);
    });
    const [endTime, setEndTime] = useState(() => {
        return new Date().toISOString().slice(0, 16);
    });

    const handleSearch = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await getPerformanceMetricsStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || '성능 메트릭 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    성능 메트릭 통계
                </h1>
                <p className="text-secondary-600">
                    CPU, 메모리, JVM 성능 지표 분석
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    시간 주기
                                </label>
                                <select
                                    value={timePeriod}
                                    onChange={(e) => setTimePeriod(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="MINUTE">분</option>
                                    <option value="HOUR">시간</option>
                                    <option value="DAY">일</option>
                                </select>
                            </div>

                            <Button
                                variant="primary"
                                icon={<Search className="w-4 h-4" />}
                                onClick={handleSearch}
                                loading={loading}
                                className="w-full"
                            >
                                조회
                            </Button>
                        </div>
                    </Card>

                    <div className="mt-6">
                        <DateRangePicker
                            startDate={startTime}
                            endDate={endTime}
                            onChange={handleDateRangeChange}
                        />
                    </div>
                </div>

                <div className="lg:col-span-2">
                    {loading && <Loading text="데이터를 불러오는 중..." />}

                    {error && <ErrorMessage message={error} onRetry={handleSearch} />}

                    {!loading && !error && data && (
                        <>
                            {/* 시스템 메트릭 요약 */}
                            <Card title="시스템 메트릭 요약" className="mb-6">
                                <div className="grid md:grid-cols-3 gap-6">
                                    <div className="flex items-start space-x-3">
                                        <div className="p-3 bg-blue-100 rounded-lg">
                                            <Cpu className="w-6 h-6 text-blue-600" />
                                        </div>
                                        <div>
                                            <p className="text-sm text-secondary-600">평균 CPU</p>
                                            <p className="text-2xl font-bold text-primary-700">
                                                {data.systemMetrics?.avgCpuUsage?.toFixed(1) || 0}%
                                            </p>
                                            <p className="text-xs text-secondary-500">
                                                최대: {data.systemMetrics?.maxCpuUsage?.toFixed(1) || 0}%
                                            </p>
                                        </div>
                                    </div>

                                    <div className="flex items-start space-x-3">
                                        <div className="p-3 bg-green-100 rounded-lg">
                                            <Database className="w-6 h-6 text-green-600" />
                                        </div>
                                        <div>
                                            <p className="text-sm text-secondary-600">평균 메모리</p>
                                            <p className="text-2xl font-bold text-primary-700">
                                                {data.systemMetrics?.avgMemoryUsage?.toFixed(1) || 0}%
                                            </p>
                                            <p className="text-xs text-secondary-500">
                                                최대: {data.systemMetrics?.maxMemoryUsage?.toFixed(1) || 0}%
                                            </p>
                                        </div>
                                    </div>

                                    <div className="flex items-start space-x-3">
                                        <div className="p-3 bg-purple-100 rounded-lg">
                                            <Activity className="w-6 h-6 text-purple-600" />
                                        </div>
                                        <div>
                                            <p className="text-sm text-secondary-600">평균 디스크</p>
                                            <p className="text-2xl font-bold text-primary-700">
                                                {data.systemMetrics?.avgDiskUsage?.toFixed(1) || 0}%
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </Card>

                            {/* JVM 메트릭 요약 */}
                            <Card title="JVM 메트릭 요약" className="mb-6">
                                <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
                                    <div className="p-4 bg-primary-50 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">평균 Heap</p>
                                        <p className="text-xl font-bold text-primary-700">
                                            {data.jvmMetrics?.avgHeapUsed?.toFixed(0) || 0} <span className="text-sm font-normal">MB</span>
                                        </p>
                                    </div>
                                    <div className="p-4 bg-primary-50 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">최대 Heap</p>
                                        <p className="text-xl font-bold text-primary-700">
                                            {data.jvmMetrics?.maxHeapUsed?.toFixed(0) || 0} <span className="text-sm font-normal">MB</span>
                                        </p>
                                    </div>
                                    <div className="p-4 bg-primary-50 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">GC 횟수</p>
                                        <p className="text-xl font-bold text-primary-700">
                                            {data.jvmMetrics?.totalGcCount || 0}
                                        </p>
                                    </div>
                                    <div className="p-4 bg-primary-50 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">GC 시간</p>
                                        <p className="text-xl font-bold text-primary-700">
                                            {data.jvmMetrics?.totalGcTime || 0} ms
                                        </p>
                                    </div>
                                </div>
                            </Card>

                            {/* 시스템 리소스 추이 */}
                            <Card title="시스템 리소스 사용률 추이" className="mb-6">
                                <ResponsiveContainer width="100%" height={400}>
                                    <AreaChart data={data.distributions}>
                                        <defs>
                                            <linearGradient id="colorCpu" x1="0" y1="0" x2="0" y2="1">
                                                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8}/>
                                                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                                            </linearGradient>
                                            <linearGradient id="colorMemory" x1="0" y1="0" x2="0" y2="1">
                                                <stop offset="5%" stopColor="#10b981" stopOpacity={0.8}/>
                                                <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                                            </linearGradient>
                                        </defs>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis dataKey="timestamp" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Legend />
                                        <Area
                                            type="monotone"
                                            dataKey="cpuUsage"
                                            stroke="#3b82f6"
                                            fillOpacity={1}
                                            fill="url(#colorCpu)"
                                            name="CPU 사용률 (%)"
                                        />
                                        <Area
                                            type="monotone"
                                            dataKey="memoryUsage"
                                            stroke="#10b981"
                                            fillOpacity={1}
                                            fill="url(#colorMemory)"
                                            name="메모리 사용률 (%)"
                                        />
                                    </AreaChart>
                                </ResponsiveContainer>
                            </Card>

                            {/* Heap 사용량 추이 */}
                            <Card title="Heap 메모리 사용량 추이">
                                <ResponsiveContainer width="100%" height={300}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis dataKey="timestamp" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Legend />
                                        <Line
                                            type="monotone"
                                            dataKey="heapUsage"
                                            stroke="#8b5cf6"
                                            strokeWidth={2}
                                            name="Heap 사용률 (%)"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <Activity className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
                                <p className="text-secondary-600">
                                    검색 조건을 설정하고 조회 버튼을 클릭하세요
                                </p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
}
```

## 데이터베이스 로그 통계 페이지
### study-monitoring-frontend/app/statistics/database-logs/page.tsx
```typescript jsx
'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getDatabaseLogStatistics } from '@/lib/api/statistics';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
    ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell
} from 'recharts';
import { Search, Database } from 'lucide-react';

const COLORS = ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

export default function DatabaseLogStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return date.toISOString().slice(0, 16);
    });
    const [endTime, setEndTime] = useState(() => {
        return new Date().toISOString().slice(0, 16);
    });

    const handleSearch = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await getDatabaseLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || 'DB 로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    const operationChartData = data?.operationCounts
        ? Object.entries(data.operationCounts).map(([operation, count]) => ({
            operation,
            count,
        }))
        : [];

    const tableChartData = data?.tableCounts
        ? Object.entries(data.tableCounts)
            .map(([table, count]) => ({ table, count }))
            .sort((a: any, b: any) => b.count - a.count)
            .slice(0, 10)
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    데이터베이스 로그 통계
                </h1>
                <p className="text-secondary-600">
                    쿼리 실행시간, Operation별, 테이블별 통계
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    시간 주기
                                </label>
                                <select
                                    value={timePeriod}
                                    onChange={(e) => setTimePeriod(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="MINUTE">분</option>
                                    <option value="HOUR">시간</option>
                                    <option value="DAY">일</option>
                                </select>
                            </div>

                            <Button
                                variant="primary"
                                icon={<Search className="w-4 h-4" />}
                                onClick={handleSearch}
                                loading={loading}
                                className="w-full"
                            >
                                조회
                            </Button>
                        </div>
                    </Card>

                    <div className="mt-6">
                        <DateRangePicker
                            startDate={startTime}
                            endDate={endTime}
                            onChange={handleDateRangeChange}
                        />
                    </div>
                </div>

                <div className="lg:col-span-2">
                    {loading && <Loading text="데이터를 불러오는 중..." />}

                    {error && <ErrorMessage message={error} onRetry={handleSearch} />}

                    {!loading && !error && data && (
                        <>
                            {/* 쿼리 성능 요약 */}
                            <Card title="쿼리 성능 요약" className="mb-6">
                                <div className="grid md:grid-cols-4 gap-4">
                                    <div className="p-4 bg-primary-50 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">평균 실행시간</p>
                                        <p className="text-2xl font-bold text-primary-700">
                                            {data.queryPerformance?.avgDuration?.toFixed(0) || 0}
                                            <span className="text-sm text-secondary-500 ml-1">ms</span>
                                        </p>
                                    </div>
                                    <div className="p-4 bg-warning/10 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">최대 실행시간</p>
                                        <p className="text-2xl font-bold text-warning">
                                            {data.queryPerformance?.maxDuration?.toFixed(0) || 0}
                                            <span className="text-sm text-secondary-500 ml-1">ms</span>
                                        </p>
                                    </div>
                                    <div className="p-4 bg-error/10 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">느린 쿼리</p>
                                        <p className="text-2xl font-bold text-error">
                                            {data.queryPerformance?.slowQueryCount || 0}
                                        </p>
                                    </div>
                                    <div className="p-4 bg-success/10 rounded-lg">
                                        <p className="text-sm text-secondary-600 mb-1">전체 쿼리</p>
                                        <p className="text-2xl font-bold text-success">
                                            {data.queryPerformance?.totalQueryCount || 0}
                                        </p>
                                    </div>
                                </div>
                            </Card>

                            {/* Operation별 & 테이블별 통계 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="Operation별 쿼리 수">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <PieChart>
                                            <Pie
                                                data={operationChartData}
                                                dataKey="count"
                                                nameKey="operation"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={100}
                                                label
                                            >
                                                {/* ▼▼▼ 여기가 수정된 부분입니다 (백틱 ` 적용) ▼▼▼ */}
                                                {operationChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip />
                                            <Legend />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>
                                <Card title="테이블별 쿼리 수 (Top 10)">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <BarChart data={tableChartData} layout="vertical">
                                            {/* layout="vertical"일 때는 X/Y축 설정을 맞춰주어야 바가 보입니다 */}
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis type="number" />
                                            <YAxis dataKey="table" type="category" width={100} style={{ fontSize: '11px' }} />
                                            <Tooltip />
                                            <Bar dataKey="count" fill="#0ea5e9" />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Card>
                            </div>

                            {/* 시간대별 DB 로그 분포 */}
                            <Card title="시간대별 쿼리 실행 추이">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis dataKey="timestamp" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <YAxis yAxisId="left" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <YAxis yAxisId="right" orientation="right" stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip />
                                        <Legend />
                                        <Line
                                            yAxisId="left"
                                            type="monotone"
                                            dataKey="queryCount"
                                            stroke="#0ea5e9"
                                            strokeWidth={2}
                                            name="쿼리 수"
                                        />
                                        <Line
                                            yAxisId="right"
                                            type="monotone"
                                            dataKey="avgDuration"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            name="평균 실행시간 (ms)"
                                        />
                                        <Line
                                            yAxisId="left"
                                            type="monotone"
                                            dataKey="slowQueryCount"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            name="느린 쿼리 수"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <Database className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
                                <p className="text-secondary-600">
                                    검색 조건을 설정하고 조회 버튼을 클릭하세요
                                </p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
}
```

## 감사 로그 통계 페이지
### study-monitoring-frontend/app/statistics/audit-logs/page.tsx
```typescript jsx
'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getAuditLogStatistics } from '@/lib/api/statistics';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
    ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell
} from 'recharts';
import { Search, Shield } from 'lucide-react';

const COLORS = ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

// 로컬 시간 변환 헬퍼 함수
const getLocalISOString = (date: Date) => {
    const offset = date.getTimezoneOffset() * 60000;
    const localDate = new Date(date.getTime() - offset);
    return localDate.toISOString().slice(0, 16);
};

export default function AuditLogStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [eventAction, setEventAction] = useState('');

    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return getLocalISOString(date);
    });

    const [endTime, setEndTime] = useState(() => {
        const date = new Date();
        return getLocalISOString(date);
    });

    const handleSearch = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await getAuditLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
                eventAction: eventAction || undefined,
            });

            console.log('API Response:', result); // 데이터 확인용
            setData(result);
        } catch (err: any) {
            setError(err.message || '감사 로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    // [수정 1] 키 이름 변경: actionCounts -> eventActionCounts
    const actionChartData = data?.eventActionCounts
        ? Object.entries(data.eventActionCounts).map(([action, count]) => ({
            action,
            count,
        }))
        : [];

    const categoryChartData = data?.categoryCounts
        ? Object.entries(data.categoryCounts).map(([category, count]) => ({
            category,
            count,
        }))
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    감사 로그 통계
                </h1>
                <p className="text-secondary-600">
                    사용자 액션, 이벤트 카테고리, 성공/실패율 분석
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    이벤트 액션 (선택)
                                </label>
                                <input
                                    type="text"
                                    value={eventAction}
                                    onChange={(e) => setEventAction(e.target.value)}
                                    placeholder="예: user.login"
                                    className="input-field"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    시간 주기
                                </label>
                                <select
                                    value={timePeriod}
                                    onChange={(e) => setTimePeriod(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="MINUTE">분</option>
                                    <option value="HOUR">시간</option>
                                    <option value="DAY">일</option>
                                </select>
                            </div>

                            <Button
                                variant="primary"
                                icon={<Search className="w-4 h-4" />}
                                onClick={handleSearch}
                                loading={loading}
                                className="w-full"
                            >
                                조회
                            </Button>
                        </div>
                    </Card>

                    <div className="mt-6">
                        <DateRangePicker
                            startDate={startTime}
                            endDate={endTime}
                            onChange={handleDateRangeChange}
                        />
                    </div>
                </div>

                <div className="lg:col-span-2">
                    {loading && <Loading text="데이터를 불러오는 중..." />}

                    {error && <ErrorMessage message={error} onRetry={handleSearch} />}

                    {!loading && !error && data && (
                        <>
                            {/* 요약 카드 */}
                            <div className="grid md:grid-cols-4 gap-4 mb-6">
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">총 이벤트</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {/* [수정 2] data.eventActionCounts 사용 */}
                                        {(Object.values(data.eventActionCounts || {}).reduce((a: any, b: any) => a + b, 0) as number)}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">성공 이벤트</p>
                                    <p className="text-3xl font-bold text-success">
                                        {/* [수정 3] data.resultStats 구조 반영 */}
                                        {data.resultStats?.successCount || 0}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">실패 이벤트</p>
                                    <p className="text-3xl font-bold text-error">
                                        {/* [수정 3] data.resultStats 구조 반영 */}
                                        {data.resultStats?.failureCount || 0}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">성공률</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {data.resultStats?.successRate?.toFixed(1) || '0.0'}
                                        <span className="text-lg text-secondary-500 ml-1">%</span>
                                    </p>
                                </Card>
                            </div>

                            {/* 액션별 & 카테고리별 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="액션별 이벤트 수">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <BarChart data={actionChartData} layout="vertical">
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis type="number" />
                                            <YAxis
                                                dataKey="action"
                                                type="category"
                                                width={120}
                                                style={{ fontSize: '11px' }}
                                            />
                                            <Tooltip />
                                            <Bar dataKey="count" fill="#0ea5e9" />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Card>

                                <Card title="카테고리별 분포">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <PieChart>
                                            <Pie
                                                data={categoryChartData}
                                                dataKey="count"
                                                nameKey="category"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={100}
                                                label
                                            >
                                                {categoryChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip />
                                            <Legend />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>
                            </div>

                            {/* 시간대별 추이 */}
                            <Card title="시간대별 감사 로그 추이">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '12px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: '#fff',
                                                border: '1px solid #e2e8f0',
                                                borderRadius: '8px',
                                            }}
                                        />
                                        <Legend />
                                        {/* [수정 4] 서버 응답 키와 정확히 일치시킴 (오타 포함) */}
                                        <Line
                                            type="monotone"
                                            dataKey="totalEvents"
                                            stroke="#0ea5e9"
                                            strokeWidth={2}
                                            name="전체 이벤트"
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="successEvents"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            name="성공"
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="failureEvents"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            name="실패"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            {/* 사용자별 통계 */}
                            {data.userCounts && Object.keys(data.userCounts).length > 0 && (
                                <Card title="사용자별 활동 통계" className="mt-6">
                                    <div className="overflow-x-auto">
                                        <table className="min-w-full divide-y divide-gray-200">
                                            <thead className="bg-gray-50">
                                            <tr>
                                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                    사용자
                                                </th>
                                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                    이벤트 수
                                                </th>
                                            </tr>
                                            </thead>
                                            <tbody className="bg-white divide-y divide-gray-200">
                                            {Object.entries(data.userCounts)
                                                .sort((a: any, b: any) => b[1] - a[1])
                                                .slice(0, 10)
                                                .map(([user, count]: any, index: number) => (
                                                    <tr key={index}>
                                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                                            {user}
                                                        </td>
                                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-primary-700 font-semibold">
                                                            {count}
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </Card>
                            )}
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <Shield className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
                                <p className="text-secondary-600">
                                    검색 조건을 설정하고 조회 버튼을 클릭하세요
                                </p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
}
```

## 보안 로그 통계 페이지
### study-monitoring/app/statistics/security-logs/page
```typescript jsx
'use client';

import { useState } from 'react';
import Card from '@/components/common/Card';
import Loading from '@/components/common/Loading';
import ErrorMessage from '@/components/common/ErrorMessage';
import Button from '@/components/common/Button';
import DateRangePicker from '@/components/common/DateRangePicker';
import { getSecurityLogStatistics } from '@/lib/api/statistics';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
    ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell
} from 'recharts';
import { Search, Lock, AlertTriangle } from 'lucide-react';

const COLORS = ['#ef4444', '#f59e0b', '#10b981', '#0ea5e9', '#8b5cf6'];

export default function SecurityLogStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [threatLevel, setThreatLevel] = useState('');
    const [startTime, setStartTime] = useState(() => {
        const date = new Date();
        date.setHours(date.getHours() - 24);
        return date.toISOString().slice(0, 16);
    });
    const [endTime, setEndTime] = useState(() => {
        return new Date().toISOString().slice(0, 16);
    });

    const handleSearch = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await getSecurityLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
                threatLevel: threatLevel || undefined,
            });

            setData(result);
        } catch (err: any) {
            setError(err.message || '보안 로그 통계를 불러오는데 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleDateRangeChange = (start: string, end: string) => {
        setStartTime(start);
        setEndTime(end);
    };

    const threatLevelChartData = data?.threatLevelCounts
        ? Object.entries(data.threatLevelCounts).map(([level, count]) => ({
            level,
            count,
        }))
        : [];

    const attackTypeChartData = data?.attackTypeCounts
        ? Object.entries(data.attackTypeCounts).map(([type, count]) => ({
            type,
            count,
        }))
        : [];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-primary-700 mb-2">
                    보안 로그 통계
                </h1>
                <p className="text-secondary-600">
                    위협 레벨, 공격 타입, 차단 통계 분석
                </p>
            </div>

            <div className="grid lg:grid-cols-3 gap-6 mb-6">
                <div className="lg:col-span-1">
                    <Card title="검색 조건">
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    위협 레벨 (선택)
                                </label>
                                <select
                                    value={threatLevel}
                                    onChange={(e) => setThreatLevel(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="">전체</option>
                                    <option value="low">Low</option>
                                    <option value="medium">Medium</option>
                                    <option value="high">High</option>
                                    <option value="critical">Critical</option>
                                </select>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-secondary-700 mb-2">
                                    시간 주기
                                </label>
                                <select
                                    value={timePeriod}
                                    onChange={(e) => setTimePeriod(e.target.value)}
                                    className="input-field"
                                >
                                    <option value="MINUTE">분</option>
                                    <option value="HOUR">시간</option>
                                    <option value="DAY">일</option>
                                </select>
                            </div>

                            <Button
                                variant="primary"
                                icon={<Search className="w-4 h-4" />}
                                onClick={handleSearch}
                                loading={loading}
                                className="w-full"
                            >
                                조회
                            </Button>
                        </div>
                    </Card>

                    <div className="mt-6">
                        <DateRangePicker
                            startDate={startTime}
                            endDate={endTime}
                            onChange={handleDateRangeChange}
                        />
                    </div>
                </div>

                <div className="lg:col-span-2">
                    {loading && <Loading text="데이터를 불러오는 중..." />}

                    {error && <ErrorMessage message={error} onRetry={handleSearch} />}

                    {!loading && !error && data && (
                        <>
                            {/* 요약 카드 */}
                            <div className="grid md:grid-cols-4 gap-4 mb-6">
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">총 보안 이벤트</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {(Object.values(data.threatLevelCounts || {}).reduce((a: any, b: any) => a + b, 0) as number)}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">차단된 시도</p>
                                    <p className="text-3xl font-bold text-error">
                                        {data.blockedCount || 0}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">Critical 위협</p>
                                    <p className="text-3xl font-bold text-error">
                                        {data.threatLevelCounts?.critical || 0}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">차단율</p>
                                    <p className="text-3xl font-bold text-success">
                                        {(() => {
                                            const total = Object.values(data.threatLevelCounts || {})
                                                .reduce((a: any, b: any) => a + b, 0) as number;

                                            return total > 0
                                                ? (((data.blockedCount || 0) / total) * 100).toFixed(1)
                                                : '0.0';
                                        })()}
                                        <span className="text-lg text-secondary-500 ml-1">%</span>
                                    </p>
                                </Card>
                            </div>

                            {/* 위협 레벨 & 공격 타입 */}
                            <div className="grid md:grid-cols-2 gap-6 mb-6">
                                <Card title="위협 레벨별 분포">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <PieChart>
                                            <Pie
                                                data={threatLevelChartData}
                                                dataKey="count"
                                                nameKey="level"
                                                cx="50%"
                                                cy="50%"
                                                outerRadius={100}
                                                label={(entry) => `${entry.level}: ${entry.count}`}
                                            >
                                                {threatLevelChartData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </Card>

                                <Card title="공격 타입별 분포">
                                    <ResponsiveContainer width="100%" height={300}>
                                        <BarChart data={attackTypeChartData} layout="vertical">
                                            <CartesianGrid strokeDasharray="3 3" />
                                            <XAxis type="number" />
                                            <YAxis
                                                dataKey="type"
                                                type="category"
                                                width={120}
                                                style={{ fontSize: '11px' }}
                                            />
                                            <Tooltip />
                                            <Bar dataKey="count" fill="#ef4444" />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </Card>
                            </div>

                            {/* 시간대별 추이 */}
                            <Card title="시간대별 보안 이벤트 추이">
                                <ResponsiveContainer width="100%" height={400}>
                                    <LineChart data={data.distributions}>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                                        <XAxis
                                            dataKey="timestamp"
                                            stroke="#64748b"
                                            style={{ fontSize: '12px' }}
                                        />
                                        <YAxis stroke="#64748b" style={{ fontSize: '12px' }} />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: '#fff',
                                                border: '1px solid #e2e8f0',
                                                borderRadius: '8px',
                                            }}
                                        />
                                        <Legend />
                                        <Line
                                            type="monotone"
                                            dataKey="threatCount"
                                            stroke="#ef4444"
                                            strokeWidth={2}
                                            name="위협 탐지"
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="blockedCount"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            name="차단된 시도"
                                        />
                                    </LineChart>
                                </ResponsiveContainer>
                            </Card>

                            {/* 공격 소스 IP 통계 */}
                            {data.sourceIpCounts && Object.keys(data.sourceIpCounts).length > 0 && (
                                <Card title="공격 소스 IP Top 10" className="mt-6">
                                    <div className="overflow-x-auto">
                                        <table className="min-w-full divide-y divide-gray-200">
                                            <thead className="bg-gray-50">
                                            <tr>
                                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                    IP 주소
                                                </th>
                                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                    공격 시도
                                                </th>
                                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                                                    위험도
                                                </th>
                                            </tr>
                                            </thead>
                                            <tbody className="bg-white divide-y divide-gray-200">
                                            {Object.entries(data.sourceIpCounts)
                                                .sort((a: any, b: any) => b[1] - a[1])
                                                .slice(0, 10)
                                                .map(([ip, count]: any, index: number) => {
                                                    const riskLevel = count > 100 ? 'Critical' :
                                                        count > 50 ? 'High' :
                                                            count > 20 ? 'Medium' : 'Low';
                                                    const riskColor = count > 100 ? 'text-error' :
                                                        count > 50 ? 'text-warning' :
                                                            count > 20 ? 'text-yellow-600' : 'text-success';
                                                    return (
                                                        <tr key={index}>
                                                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                                                {ip}
                                                            </td>
                                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-error font-semibold">
                                                                {count}
                                                            </td>
                                                            <td className="px-6 py-4 whitespace-nowrap">
                                                                    <span className={`px-3 py-1 text-xs font-semibold rounded-full ${riskColor} bg-opacity-10`}>
                                                                        {riskLevel}
                                                                    </span>
                                                            </td>
                                                        </tr>
                                                    );
                                                })}
                                            </tbody>
                                        </table>
                                    </div>
                                </Card>
                            )}

                            {/* 위협 레벨별 상세 */}
                            <Card title="위협 레벨별 상세 정보" className="mt-6">
                                <div className="space-y-4">
                                    {threatLevelChartData.map((item: any, index: number) => {
                                        const total = threatLevelChartData.reduce((sum, i: any) => sum + i.count, 0);
                                        const percentage = ((item.count / total) * 100).toFixed(1);
                                        const levelColor =
                                            item.level === 'critical' ? 'bg-red-100 border-red-300' :
                                                item.level === 'high' ? 'bg-orange-100 border-orange-300' :
                                                    item.level === 'medium' ? 'bg-yellow-100 border-yellow-300' :
                                                        'bg-green-100 border-green-300';

                                        return (
                                            <div key={index} className={`p-4 rounded-lg border ${levelColor}`}>
                                                <div className="flex justify-between items-center">
                                                    <div className="flex items-center space-x-3">
                                                        <AlertTriangle className="w-5 h-5" />
                                                        <span className="font-semibold text-gray-900 uppercase">
                                                            {item.level}
                                                        </span>
                                                    </div>
                                                    <div className="text-right">
                                                        <p className="text-2xl font-bold text-gray-900">{item.count}</p>
                                                        <p className="text-sm text-gray-600">{percentage}%</p>
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </Card>
                        </>
                    )}

                    {!loading && !error && !data && (
                        <Card>
                            <div className="text-center py-12">
                                <Lock className="w-16 h-16 text-secondary-300 mx-auto mb-4" />
                                <p className="text-secondary-600">
                                    검색 조건을 설정하고 조회 버튼을 클릭하세요
                                </p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
}
```