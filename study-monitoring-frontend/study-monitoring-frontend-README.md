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
│   ├── page.tsx                      # 홈페이지 (/)
│   ├── dashboard/                    # 대시보드 (/dashboard)
│   │   └── page.tsx
│   ├── logs/                         # 로그 (/logs)
│   │   └── page.tsx
│   ├── metrics/                      # 메트릭 (/metrics)
│   │   └── page.tsx
│   ├── statistics/                   # 통계 (/statistics)
│   │   ├── page.tsx
│   │   ├── timeseries/
│   │   │   └── page.tsx
│   │   ├── logs/
│   │   │   └── page.tsx
│   │   ├── access-logs/
│   │   │   └── page.tsx
│   │   ├── error-logs/
│   │   │   └── page.tsx
│   │   ├── performance-metrics/
│   │   │   └── page.tsx
│   │   ├── database-logs/
│   │   │   └── page.tsx
│   │   ├── audit-logs/
│   │   │   └── page.tsx
│   │   └── security-logs/
│   │       └── page.tsx
│   └── health/                       # 헬스체크 (/health)
│       └── page.tsx
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

## 4. API 연동 설정
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

## Health API
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

## Metics API
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

## Statistics API
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

## 공통 UI 컴포넌트
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
                    {Object.values(data.errorTypeCounts || {}).reduce((a: any, b: any) => a + b, 0)}
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
                      {(data.jvmMetrics?.avgHeapUsed / 1024 / 1024)?.toFixed(0) || 0} MB
                    </p>
                  </div>
                  <div className="p-4 bg-primary-50 rounded-lg">
                    <p className="text-sm text-secondary-600 mb-1">최대 Heap</p>
                    <p className="text-xl font-bold text-primary-700">
                      {(data.jvmMetrics?.maxHeapUsed / 1024 / 1024)?.toFixed(0) || 0} MB
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

export default function AuditLogStatisticsPage() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<any>(null);

    const [timePeriod, setTimePeriod] = useState('HOUR');
    const [eventAction, setEventAction] = useState('');
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
            const result = await getAuditLogStatistics({
                startTime: startTime.replace('T', ' ') + ':00',
                endTime: endTime.replace('T', ' ') + ':00',
                timePeriod,
                eventAction: eventAction || undefined,
            });

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

    const actionChartData = data?.actionCounts
        ? Object.entries(data.actionCounts).map(([action, count]) => ({
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
                                        {(Object.values(data.actionCounts || {}).reduce((a: any, b: any) => a + b, 0) as number)}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">성공 이벤트</p>
                                    <p className="text-3xl font-bold text-success">
                                        {data.successCount || 0}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">실패 이벤트</p>
                                    <p className="text-3xl font-bold text-error">
                                        {data.failureCount || 0}
                                    </p>
                                </Card>
                                <Card>
                                    <p className="text-sm text-secondary-600 mb-1">성공률</p>
                                    <p className="text-3xl font-bold text-primary-700">
                                        {(() => {
                                            const total = (data.successCount || 0) + (data.failureCount || 0);
                                            return total > 0
                                                ? ((data.successCount / total) * 100).toFixed(1)
                                                : '0.0';
                                        })()}
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
                                        <Line
                                            type="monotone"
                                            dataKey="eventCount"
                                            stroke="#0ea5e9"
                                            strokeWidth={2}
                                            name="전체 이벤트"
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="successCount"
                                            stroke="#10b981"
                                            strokeWidth={2}
                                            name="성공"
                                        />
                                        <Line
                                            type="monotone"
                                            dataKey="failureCount"
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