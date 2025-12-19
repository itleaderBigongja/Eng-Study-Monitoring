<h1>Study Monitoring Frontend</h1>


<h1>목차</h1>
1. 주요 기능
2. 주요 특징
3. 시작하기
4. 배포
5. 프로젝트 구조
6. API 엔드 포인트
7. Elasticsearch 역색인 검색 설명
8. 프로젝트 소스코드
9. TODO


# 주요 기능
## 1. 대시보드(Dashboard)

| 대시보드       | 설명                                         |
|------------|--------------------------------------------|
| 실시간 메트릭 카드 | TPS, Heap Usage, Error Rate, Total Requests |
| 프로세스 상태    | 각 서비스별 CPU/Memory 사용률, Uptime 모니터링         |
| 로그 레벨 차트   | INFO, WARN, ERROR, DEBUG 분포 시각화            |
| 최근 에러 요약   | 최신 에러 로그 5건 표시                             |

## 2. 로그 검색(Logs)
### Elasticsearch 역색인 검색 : 토큰화 기반 전문 검색
<p>-> 예: "database error" 검색 시 "database", "error" 토큰으로 자동 분리</p>
<p>-> AND 조건으로 두 단어가 모두 포함된 로그만 검색</p>

### 실시간 필터링
<p>-> 로그 레벨, 인덱스 패턴, 키워드 조합 검색</p>

### 페이지네이션
<p>-> 50개씩 페이징 처리</p>

### Export 기능
<p>-> 검색 결과 다운로드(추후 구현)</p>

## 메트릭(Metric)
### Prometheus 연동 
<p>-> 실시간 시계열 데이터</p>

### 4가지 메트릭 타입

| 메트릭 타입                      | 설명 |
|-----------------------------|----|
| TPS(Transactions Per Second |    |
| Heap Memory Usage(%)        |    |
| Error Rate(%)               |    |
| Response Time P95(seconds)  |    |

### 시간 범위 선택: 1시간 ~ 24시간
### 통계 요약: Current / Average / Min /Max

## 4. 인프라 현황(Infrastructure)

| 인프라              | 현황                                    |
|------------------|---------------------------------------|
| Kubernetes Pod 상태 | RUNNING / STOPPED / ERROR             |
| 리소스 모니터링         | CPU 사용률(프로그레스바)<br/>Memory 사용률(프로그레스바) |
| 헬스 체크            | 마지막 체크 시간, Uptime 표시                  |
| 시각적 경고           | 80%이상 사용 시 빨간색 표시                     |

# 주요 특징
## 1. 디자인 시스템
### 색상 팔레트
<p>Primary Blue: #3b82f6 (메인 액션, 링크)</p>
<p>Success Green: #10b981 (정상 상태)</p>
<p>Warning Yellow: #f59e0b (주의)</p>
<p>Error Red: #ef4444 (에러, 위험)</p>
<p>Secondary Purple: #8b5cf6 (보조 정보)</p>

### 컴포넌트 스타일
<p>카드: 흰색 배경 + 그림자 + 호버 효과</p>
<p>버튼: 둥근 모서리 + 부드러운 전환</p>
<p>차트: Recharts 라이브러리 + 파스텔 톤 색상</p>

### 세련된 디자인
<p>흰색 + 파란색 테마</p>
<p>부드러운 애니메이션</p>
<p>반응형 레이아웃</p>


## 2. Elasticsearch 역색인 검색
<p>-> 토큰화 자동 처리</p>
<p>-> AND 조건 검색</p>
<p>-> 실시간 필터링</p>

## 3. 실시간 모니터링
<p>-> 15초 마다 자동 갱신</p>
<p>-> Prometheus 메트릭 연동</p>
<p>-> Recharts 시각화</p>

## 4. TypeScript 타입 안전성
<p>-> 모든 API 응답 타입 정의</p>
<p>-> 컴파일 타임 에러 체크</p>

# 시작하기
## 사전 요구사항
<p>Node.js 22+</p>
<p>npm 또는 yarn</p>
<p>study-monitoring 백엔드 실행 중(http:localhost:8081)</p>

### 1. 프로젝트 클론
```terminaloutput
cd Monitoring/study-monitoring-frontend
```

### 2. 의존성 설치
```terminaloutput
npm install
```

### 3. 환경 변수 설정
<p>개발 환경</p>

```terminaloutput
# /Monitoring/study-monitoring-frontend/.env.local 파일 생성
# ==========================================
# .env.local (로컬 개발 환경)
# ==========================================

# Monitoring Backend API
NEXT_PUBLIC_MONITORING_API=http://localhost:8081/api

# Prometheus (포트 포워딩 필요)
# kubectl port-forward -n monitoring service/prometheus-service 9090:9090
NEXT_PUBLIC_PROMETHEUS_URL=http://localhost:9090

# Elasticsearch (포트 포워딩 필요)
# kubectl port-forward -n monitoring service/elasticsearch-service 9200:9200
NEXT_PUBLIC_ELASTICSEARCH_URL=http://localhost:9200
```

<p>프로덕션 환경</p>

````terminaloutput
# ==========================================
# .env.production (Kubernetes 배포)
# ==========================================

# Monitoring Backend API (FQDN)
NEXT_PUBLIC_MONITORING_API=http://monitoring-backend-service.monitoring.svc.cluster.local:8081/api

# Prometheus (FQDN)
NEXT_PUBLIC_PROMETHEUS_URL=http://prometheus-service.monitoring.svc.cluster.local:9090

# Elasticsearch (FQDN)
NEXT_PUBLIC_ELASTICSEARCH_URL=http://elasticsearch-service.monitoring.svc.cluster.local:9200
````
### 4. 개발 서버 실행
```terminaloutput
/Monitoring/study-monitoring-frontend/
npm run dev
```
<p>접속 : localhost:3001</p>

# 배포
## 1. Kubernetes 배포
````terminaloutput
# /Monitoring/
./build-local.sh    # 빌드
./deploy-local.sh   # 배포
````

## 2. 접속 확인

| 접속 환경      | URL                               |
|------------|-----------------------------------|
| 로컬         | localhost:3001                    |
| Kubernetes | http://localhost:30081(Node Port) |
| Nginx 경로   | http://localhost:30080/monitoring |


# 프로젝트 구조
```text
study-monitoring-frontend/
├── app/                          # Next.js App Router
│   ├── page.tsx                 # 대시보드 메인
│   ├── layout.tsx               # 루트 레이아웃
│   ├── globals.css              # 전역 스타일
│   ├── logs/
│   │   └── page.tsx            # 로그 검색
│   ├── metrics/
│   │   └── page.tsx            # 메트릭 차트
│   └── infrastructure/
│       └── page.tsx            # 인프라 현황
│
├── components/
│   ├── dashboard/
│   │   ├── MetricCard.tsx
│   │   ├── ProcessStatus.tsx
│   │   ├── LogLevelChart.tsx
│   │   └── ErrorsSummary.tsx
│   └── common/
│       └── Navigation.tsx
│
├── hooks/                       # Custom Hooks
│   └── (추가 훅 파일들)
│
├── lib/                         # 유틸리티
│   └── api.ts
│
├── types/                       # TypeScript 타입
│   └── index.ts
│
├── .env.local                   # 로컬 환경 변수
├── .env.production              # 프로덕션 환경 변수
├── Dockerfile
├── next.config.ts
├── tailwind.config.ts
├── package.json
└── README.md
```

# API 엔드 포인트
## Dashboard API
```typescript
GET /api/dashboard/overview      // 전체 현황
GET /api/dashboard/metrics       // 실시간 메트릭
GET /api/dashboard/processes     // 프로세스 현황
```

## Logs API
```typescript
GET /api/logs/search            // 로그 검색
  ? index=application-logs-*
  & keyword=database
  & logLevel=ERROR
  & from=0
  & size=50

GET /api/logs/errors            // 최근 에러
GET /api/logs/stats             // 로그 통계
```

## Metrics API
```typescript
POST /api/metrics/query         // PromQL 쿼리 실행
GET  /api/metrics/current       // 현재 메트릭
POST /api/metrics/range         // 시간 범위 메트릭
```

# Elasticsearch 역색인 검색 설명
## 역색인(Inverted Index) 동작 원리
### 1. 로그 저장 시 토큰화
```json
원본: "Database connection failed"
↓
토큰: ["database", "connection", "failed"]
↓
역색인:
{
  "database": [log_1, log_5],
  "connection": [log_1, log_3],
  "failed": [log_1, log_7]
}
```

### 2. 검색 시
```json
입력: "database failed"
↓
토큰: ["database", "failed"]
↓
역색인 조회:
  database → [log_1, log_5]
  failed   → [log_1, log_7]
↓
교집합 (AND): log_1
```

### 3. 프론트엔드 검색 UI
```typescript
// 사용자 입력
keyword: "database connection"

// API 요청
GET /api/logs/search?keyword=database%20connection

// Elasticsearch Match Query (백엔드에서 처리)
{
  "query": {
    "match": {
      "message": {
        "query": "database connection",
        "operator": "and"  // 두 토큰 모두 포함
      }
    }
  }
}
```

## 차트 커스터 마이징
### Recharts 사용 예시
```typescript
<LineChart data={chartData}>
  <CartesianGrid strokeDasharray="3 3" />
  <XAxis dataKey="timestamp" />
  <YAxis />
  <Tooltip />
  <Line 
    type="monotone" 
    dataKey="value" 
    stroke="#3b82f6"
    strokeWidth={2}
  />
</LineChart>
```

# 프로젝트 소스코드
## 1. 프로젝트 초기 설정
### package.json
#### 경로 : study-monitoring-frontend/package.json
```json
{
  "name": "study-monitoring-frontend",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "next dev --port 3001",
    "build": "next build",
    "start": "next start --port 3001",
    "lint": "next lint"
  },
  "dependencies": {
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "next": "^15.1.3",
    "recharts": "^2.15.0",
    "lucide-react": "^0.263.1",
    "date-fns": "^3.0.0"
  },
  "devDependencies": {
    "typescript": "^5",
    "@types/node": "^20",
    "@types/react": "^19",
    "@types/react-dom": "^19",
    "postcss": "^8",
    "tailwindcss": "^3.4.1",
    "eslint": "^8",
    "eslint-config-next": "15.1.3"
  }
}
```

### 2. next.config.ts
#### 경로 : study-monitoring-frontend/next.config.ts
````ts
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  output: 'standalone',
  reactStrictMode: true,
  
  env: {
    NEXT_PUBLIC_MONITORING_API: process.env.NEXT_PUBLIC_MONITORING_API || 'http://localhost:8081/api',
  },
  
  images: {
    remotePatterns: [
      {
        protocol: 'http',
        hostname: 'localhost',
      },
      {
        protocol: 'http',
        hostname: 'monitoring-backend-service',
      },
    ],
  },
};

export default nextConfig;
````

### 3. tailwind.config.ts
#### 경로 : study-monitoring-frontend/tailwind.config.ts
```ts
import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
        },
        secondary: {
          50: '#f0fdfa',
          100: '#ccfbf1',
          200: '#99f6e4',
          300: '#5eead4',
          400: '#2dd4bf',
          500: '#14b8a6',
          600: '#0d9488',
          700: '#0f766e',
          800: '#115e59',
          900: '#134e4a',
        },
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'gradient-blue': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'gradient-ocean': 'linear-gradient(135deg, #2E3192 0%, #1BFFFF 100%)',
      },
    },
  },
  plugins: [],
};

export default config;
```

## 2. TypeScript 타입 정의
### index.ts
#### 경로 : study-monitoring-frontend/types
```typescript
// ===================================
// 공통 API 응답 타입
// ===================================
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

// ===================================
// 대시보드 관련 타입
// ===================================
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
  tps: number;
  heapUsage: number;
  errorRate: number;
  responseTime: number;
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

export interface SystemStatistics {
  totalRequests: number;
  avgResponseTime: number;
  uptime: string;
}

export interface DashboardData {
  processes: ProcessStatus[];
  metrics: MetricsSummary;
  recentErrors: ErrorLog[];
  logCounts: Record<string, number>;
  statistics: SystemStatistics;
}

// ===================================
// 메트릭 관련 타입
// ===================================
export interface DataPoint {
  timestamp: number;
  value: number;
}

export interface MetricsData {
  application: string;
  metric: string;
  data: DataPoint[];
  start: number;
  end: number;
}

export interface MetricsQueryRequest {
  application: 'eng-study' | 'monitoring';
  metric: 'tps' | 'heap' | 'error_rate' | 'response_time';
  hours: number;
}

// ===================================
// 로그 관련 타입
// ===================================
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

export interface LogSearchRequest {
  index: string;
  keyword?: string;
  logLevel?: string;
  from: number;
  size: number;
}

export interface LogSearchResponse {
  total: number;
  logs: LogEntry[];
  from: number;
  size: number;
}

// ===================================
// 유틸리티 타입
// ===================================
export type MetricType = 'tps' | 'heap' | 'error_rate' | 'response_time';
export type ApplicationType = 'eng-study' | 'monitoring';
export type LogLevel = 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
export type ProcessStatusType = 'RUNNING' | 'STOPPED' | 'ERROR' | 'STARTING' | 'STOPPING';
```

## 3. API 클라이언트
### api.ts
#### 경로 : study-monitoring-frontend/lib/api.ts
```ts
import type {
  ApiResponse,
  DashboardData,
  MetricsData,
  MetricsQueryRequest,
  LogSearchRequest,
  LogSearchResponse,
} from '../types';

const API_BASE_URL = process.env.NEXT_PUBLIC_MONITORING_API || 'http://localhost:8081/api';

/**
 * 통합 API 클라이언트
 */
class ApiClient {
  private baseURL: string;

  constructor(baseURL: string) {
    this.baseURL = baseURL;
  }

  /**
   * GET 요청
   */
  async get<T>(endpoint: string, params?: Record<string, any>): Promise<ApiResponse<T>> {
    const url = new URL(`${this.baseURL}${endpoint}`);
    
    if (params) {
      Object.keys(params).forEach(key => {
        if (params[key] !== undefined && params[key] !== null) {
          url.searchParams.append(key, String(params[key]));
        }
      });
    }

    const response = await fetch(url.toString(), {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * POST 요청
   */
  async post<T>(endpoint: string, body?: any): Promise<ApiResponse<T>> {
    const response = await fetch(`${this.baseURL}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: body ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }
}

const apiClient = new ApiClient(API_BASE_URL);

// ===================================
// Dashboard API
// ===================================

/**
 * 대시보드 전체 현황 조회
 */
export async function getDashboardOverview(): Promise<ApiResponse<DashboardData>> {
  return apiClient.get<DashboardData>('/dashboard/overview');
}

/**
 * 실시간 메트릭 조회
 */
export async function getMetrics(request: MetricsQueryRequest): Promise<ApiResponse<MetricsData>> {
  return apiClient.get<MetricsData>('/dashboard/metrics', request);
}

/**
 * 프로세스 현황 조회
 */
export async function getProcesses(): Promise<ApiResponse<any>> {
  return apiClient.get('/dashboard/processes');
}

// ===================================
// Logs API
// ===================================

/**
 * 로그 검색
 */
export async function searchLogs(request: LogSearchRequest): Promise<ApiResponse<LogSearchResponse>> {
  return apiClient.get<LogSearchResponse>('/logs/search', request);
}

/**
 * 최근 에러 로그 조회
 */
export async function getRecentErrors(limit: number = 20): Promise<ApiResponse<any>> {
  return apiClient.get('/logs/errors', { limit });
}

/**
 * 로그 통계 조회
 */
export async function getLogStats(index: string = 'application-logs-*'): Promise<ApiResponse<any>> {
  return apiClient.get('/logs/stats', { index });
}

// ===================================
// Metrics API
// ===================================

/**
 * PromQL 쿼리 실행
 */
export async function executeQuery(query: string): Promise<ApiResponse<any>> {
  return apiClient.post('/metrics/query', { query });
}

/**
 * 현재 메트릭 조회
 */
export async function getCurrentMetrics(application: string = 'eng-study'): Promise<ApiResponse<any>> {
  return apiClient.get('/metrics/current', { application });
}

/**
 * 시간 범위 메트릭 조회
 */
export async function executeRangeQuery(
  query: string,
  start?: number,
  end?: number,
  step: string = '15s'
): Promise<ApiResponse<any>> {
  return apiClient.post('/metrics/range', { query, start, end, step });
}

export default apiClient;
```

## 4. 글로벌 스타일
### globals.css
#### 경로 : study-monitoring-frontend/app/global.css
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

:root {
  --background: #ffffff;
  --foreground: #171717;
}

@media (prefers-color-scheme: dark) {
  :root {
    --background: #0a0a0a;
    --foreground: #ededed;
  }
}

body {
  color: var(--foreground);
  background: var(--background);
  font-family: Arial, Helvetica, sans-serif;
}

@layer utilities {
  .text-balance {
    text-wrap: balance;
  }
}

/* 커스텀 스크롤바 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #94a3b8;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #64748b;
}

/* 애니메이션 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fadeIn 0.3s ease-in-out;
}

/* 그라디언트 텍스트 */
.gradient-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 카드 호버 효과 */
.card-hover {
  transition: all 0.3s ease;
}

.card-hover:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
}

/* 상태 배지 */
.badge {
  @apply inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium;
}

.badge-success {
  @apply bg-green-100 text-green-800;
}

.badge-warning {
  @apply bg-yellow-100 text-yellow-800;
}

.badge-error {
  @apply bg-red-100 text-red-800;
}

.badge-info {
  @apply bg-blue-100 text-blue-800;
}
```

## 5. 루트 레이아웃
### layout.tsx
#### 경로 : study-monitoring-frontend/app/layout.tsx
```tsx
import type { Metadata } from 'next';
import './globals.css';
import Navigation from '../components/common/Navigation';

export const metadata: Metadata = {
  title: 'Study Monitoring Dashboard',
  description: 'Real-time monitoring dashboard for English Study Platform',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body className="bg-gray-50">
        <div className="min-h-screen flex flex-col">
          {/* 네비게이션 */}
          <Navigation />
          
          {/* 메인 콘텐츠 */}
          <main className="flex-1">
            {children}
          </main>
          
          {/* 푸터 */}
          <footer className="bg-white border-t border-gray-200 py-6 mt-auto">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
              <p className="text-center text-sm text-gray-500">
                © 2024 Study Monitoring System. Built with Next.js + Spring Boot + Kubernetes
              </p>
            </div>
          </footer>
        </div>
      </body>
    </html>
  );
}
```

## 6. 네비게이션 컴포넌트
### Navigation.tsx
#### 경로 : study-monitoring-frontend/components/common/Navigation.tsx
```tsx
'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Activity, FileText, BarChart3, Server } from 'lucide-react';

const navItems = [
    { href: '/', label: 'Dashboard', icon: Activity },
    { href: '/logs', label: 'Logs', icon: FileText },
    { href: '/metrics', label: 'Metrics', icon: BarChart3 },
    { href: '/infrastructure', label: 'Infrastructure', icon: Server },
];

export default function Navigation() {
    const pathname = usePathname();

    return (
        <nav className="bg-white shadow-sm border-b border-gray-200">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between h-16">
                    {/* 로고 */}
                    <div className="flex items-center">
                        <Link href="/" className="flex items-center space-x-3">
                            <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
                                <Activity className="w-6 h-6 text-white" />
                            </div>
                            <div>
                                <h1 className="text-xl font-bold text-gray-900">
                                    Study Monitoring
                                </h1>
                                <p className="text-xs text-gray-500">Real-time Dashboard</p>
                            </div>
                        </Link>
                    </div>

                    {/* 네비게이션 메뉴 */}
                    <div className="flex items-center space-x-1">
                        {navItems.map((item) => {
                            const Icon = item.icon;
                            const isActive = pathname === item.href;

                            return (
                                <Link
                                    key={item.href}
                                    href={item.href}
                                    className={`
                    flex items-center space-x-2 px-4 py-2 rounded-lg
                    transition-all duration-200
                    ${
                                        isActive
                                            ? 'bg-blue-50 text-blue-600 font-medium'
                                            : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                                    }
                  `}
                                >
                                    <Icon className="w-5 h-5" />
                                    <span className="text-sm">{item.label}</span>
                                </Link>
                            );
                        })}
                    </div>

                    {/* 상태 표시 */}
                    <div className="flex items-center">
                        <div className="flex items-center space-x-2 px-4 py-2 bg-green-50 rounded-lg">
                            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                            <span className="text-sm text-green-700 font-medium">Online</span>
                        </div>
                    </div>
                </div>
            </div>
        </nav>
    );
}
```

## 7. 대시보드 메인 페이지
### page.tsx
#### 경로 : study-monitoring-frontend/app/page.tsx
```tsx
'use client';

import { useEffect, useState } from 'react';
import { getDashboardOverview } from '../lib/api';
import type { DashboardData } from '../types';
import MetricCard from '../components/dashboard/MetricCard';
import ProcessStatus from '../components/dashboard/ProcessStatus';
import ErrorsSummary from '../components/dashboard/ErrorsSummary';
import LogLevelChart from '../components/dashboard/LogLevelChart';
import { Activity, TrendingUp, AlertTriangle, Clock } from 'lucide-react';

export default function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboardData();
    
    // 30초마다 자동 갱신
    const interval = setInterval(fetchDashboardData, 30000);
    return () => clearInterval(interval);
  }, []);

  async function fetchDashboardData() {
    try {
      const response = await getDashboardOverview();
      if (response.success) {
        setData(response.data);
        setError(null);
      } else {
        setError(response.message);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch dashboard data');
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-200px)]">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <AlertTriangle className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-red-900 mb-2">Failed to Load Dashboard</h3>
          <p className="text-red-700">{error}</p>
          <button
            onClick={fetchDashboardData}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const { metrics, processes, recentErrors, logCounts, statistics } = data;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* 헤더 */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Monitoring Dashboard
        </h1>
        <p className="text-gray-600">
          Real-time system metrics and logs monitoring
        </p>
      </div>

      {/* 메트릭 카드 그리드 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {/* Eng-Study TPS */}
        <MetricCard
          title="Eng-Study TPS"
          value={metrics.engStudy.tps.toFixed(2)}
          unit="req/s"
          icon={Activity}
          trend={metrics.engStudy.tps > 10 ? 'up' : 'down'}
          color="blue"
        />

        {/* Eng-Study Heap */}
        <MetricCard
          title="Eng-Study Heap"
          value={metrics.engStudy.heapUsage.toFixed(1)}
          unit="%"
          icon={TrendingUp}
          trend={metrics.engStudy.heapUsage > 80 ? 'up' : 'down'}
          color={metrics.engStudy.heapUsage > 80 ? 'red' : 'green'}
        />

        {/* Monitoring TPS */}
        <MetricCard
          title="Monitoring TPS"
          value={metrics.monitoring.tps.toFixed(2)}
          unit="req/s"
          icon={Activity}
          trend={metrics.monitoring.tps > 5 ? 'up' : 'down'}
          color="purple"
        />

        {/* Total Requests */}
        <MetricCard
          title="Total Requests"
          value={statistics.totalRequests.toLocaleString()}
          unit="requests"
          icon={Clock}
          color="indigo"
        />
      </div>

      {/* 2열 그리드 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* 프로세스 상태 */}
        <ProcessStatus processes={processes} />

        {/* 로그 레벨 차트 */}
        <LogLevelChart logCounts={logCounts} />
      </div>

      {/* 최근 에러 */}
      <ErrorsSummary errors={recentErrors} />
    </div>
  );
}
```

## 8. 대시보드 컴포넌트들
### MetricCard
#### 경로 : study-monitoring-frontend/components/dashboard/MetricCard.tsx
```tsx
import type { LucideIcon } from 'lucide-react';
import { TrendingUp, TrendingDown } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string | number;
  unit?: string;
  icon: LucideIcon;
  trend?: 'up' | 'down';
  color?: 'blue' | 'green' | 'red' | 'purple' | 'indigo' | 'yellow';
}

const colorClasses = {
  blue: 'bg-blue-50 text-blue-600',
  green: 'bg-green-50 text-green-600',
  red: 'bg-red-50 text-red-600',
  purple: 'bg-purple-50 text-purple-600',
  indigo: 'bg-indigo-50 text-indigo-600',
  yellow: 'bg-yellow-50 text-yellow-600',
};

export default function MetricCard({
  title,
  value,
  unit,
  icon: Icon,
  trend,
  color = 'blue',
}: MetricCardProps) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 card-hover">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-600 mb-1">{title}</p>
          <div className="flex items-baseline space-x-2">
            <h3 className="text-3xl font-bold text-gray-900">{value}</h3>
            {unit && <span className="text-lg text-gray-500">{unit}</span>}
          </div>
          
          {/* 트렌드 표시 */}
          {trend && (
            <div className="flex items-center mt-2 space-x-1">
              {trend === 'up' ? (
                <TrendingUp className="w-4 h-4 text-green-500" />
              ) : (
                <TrendingDown className="w-4 h-4 text-red-500" />
              )}
              <span className={`text-xs font-medium ${
                trend === 'up' ? 'text-green-600' : 'text-red-600'
              }`}>
                {trend === 'up' ? 'Increasing' : 'Decreasing'}
              </span>
            </div>
          )}
        </div>
        
        {/* 아이콘 */}
        <div className={`w-12 h-12 rounded-lg flex items-center justify-center ${colorClasses[color]}`}>
          <Icon className="w-6 h-6" />
        </div>
      </div>
    </div>
  );
}
```

### ProcessStatus.tsx
#### 경로 : study-monitoring-frontend/components/dashboard/ProcessStatus.tsx
```tsx
import type { ProcessStatus as ProcessStatusType } from '../../types';
import { Server, Cpu, MemoryStick, Clock } from 'lucide-react';

interface ProcessStatusProps {
  processes: ProcessStatusType[];
}

const statusColors = {
  RUNNING: 'bg-green-100 text-green-800',
  STOPPED: 'bg-gray-100 text-gray-800',
  ERROR: 'bg-red-100 text-red-800',
  STARTING: 'bg-yellow-100 text-yellow-800',
  STOPPING: 'bg-orange-100 text-orange-800',
};

export default function ProcessStatus({ processes }: ProcessStatusProps) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
      <div className="flex items-center space-x-2 mb-6">
        <Server className="w-5 h-5 text-blue-600" />
        <h3 className="text-lg font-semibold text-gray-900">Process Status</h3>
        <span className="ml-auto text-sm text-gray-500">
          {processes.length} processes
        </span>
      </div>

      <div className="space-y-4">
        {processes.map((process) => (
          <div
            key={process.processId}
            className="p-4 bg-gray-50 rounded-lg border border-gray-200 hover:border-blue-300 transition-colors"
          >
            {/* 프로세스 헤더 */}
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                  <Server className="w-5 h-5 text-blue-600" />
                </div>
                <div>
                  <h4 className="font-medium text-gray-900">{process.processName}</h4>
                  <p className="text-xs text-gray-500">{process.processType}</p>
                </div>
              </div>
              
              <span className={`badge ${statusColors[process.status]}`}>
                {process.status}
              </span>
            </div>

            {/* 메트릭 그리드 */}
            <div className="grid grid-cols-3 gap-4">
              {/* CPU 사용률 */}
              <div className="flex items-center space-x-2">
                <Cpu className="w-4 h-4 text-gray-400" />
                <div>
                  <p className="text-xs text-gray-500">CPU</p>
                  <p className="text-sm font-medium text-gray-900">
                    {process.cpuUsage.toFixed(1)}%
                  </p>
                </div>
              </div>

              {/* 메모리 사용률 */}
              <div className="flex items-center space-x-2">
                <MemoryStick className="w-4 h-4 text-gray-400" />
                <div>
                  <p className="text-xs text-gray-500">Memory</p>
                  <p className="text-sm font-medium text-gray-900">
                    {process.memoryUsage.toFixed(1)}%
                  </p>
                </div>
              </div>

              {/* 가동 시간 */}
              <div className="flex items-center space-x-2">
                <Clock className="w-4 h-4 text-gray-400" />
                <div>
                  <p className="text-xs text-gray-500">Uptime</p>
                  <p className="text-sm font-medium text-gray-900">{process.uptime}</p>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
```

### LogLevelChart.tsx
#### 경로 : study-monitoring-frontend/components/dashboard/LogLevelChart.tsx
```tsx
'use client';

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { FileText } from 'lucide-react';

interface LogLevelChartProps {
  logCounts: Record<string, number>;
}

const levelColors: Record<string, string> = {
  INFO: '#3b82f6',
  WARN: '#f59e0b',
  ERROR: '#ef4444',
  DEBUG: '#8b5cf6',
};

export default function LogLevelChart({ logCounts }: LogLevelChartProps) {
  const data = Object.entries(logCounts).map(([level, count]) => ({
    level,
    count,
    color: levelColors[level] || '#6b7280',
  }));

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
      <div className="flex items-center space-x-2 mb-6">
        <FileText className="w-5 h-5 text-blue-600" />
        <h3 className="text-lg font-semibold text-gray-900">Log Levels</h3>
      </div>

      <ResponsiveContainer width="100%" height={280}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis 
            dataKey="level" 
            tick={{ fill: '#6b7280', fontSize: 12 }}
          />
          <YAxis 
            tick={{ fill: '#6b7280', fontSize: 12 }}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: 'white',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              padding: '8px 12px',
            }}
            cursor={{ fill: 'rgba(59, 130, 246, 0.1)' }}
          />
          <Bar dataKey="count" radius={[8, 8, 0, 0]}>
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.color} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>

      {/* 통계 요약 */}
      <div className="mt-4 pt-4 border-t border-gray-200">
        <div className="grid grid-cols-4 gap-4">
          {data.map((item) => (
            <div key={item.level} className="text-center">
              <div 
                className="w-3 h-3 rounded-full mx-auto mb-1"
                style={{ backgroundColor: item.color }}
              />
              <p className="text-xs font-medium text-gray-900">{item.count.toLocaleString()}</p>
              <p className="text-xs text-gray-500">{item.level}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
```

### ErrorSummary.tsx
#### 경로 : study-monitoring-frontend/components/dashboard/ErrorSummary.tsx
```tsx
import type { ErrorLog } from '../../types';
import { AlertTriangle, ExternalLink } from 'lucide-react';
import { format } from 'date-fns';
import Link from 'next/link';

interface ErrorsSummaryProps {
  errors: ErrorLog[];
}

export default function ErrorsSummary({ errors }: ErrorsSummaryProps) {
  if (errors.length === 0) {
    return (
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <div className="flex items-center space-x-2 mb-4">
          <AlertTriangle className="w-5 h-5 text-green-600" />
          <h3 className="text-lg font-semibold text-gray-900">Recent Errors</h3>
        </div>
        <div className="text-center py-8">
          <div className="w-16 h-16 bg-green-50 rounded-full flex items-center justify-center mx-auto mb-4">
            <AlertTriangle className="w-8 h-8 text-green-600" />
          </div>
          <p className="text-gray-600">No errors found! System is healthy 🎉</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-2">
          <AlertTriangle className="w-5 h-5 text-red-600" />
          <h3 className="text-lg font-semibold text-gray-900">Recent Errors</h3>
          <span className="badge badge-error">{errors.length}</span>
        </div>
        
        <Link 
          href="/logs?logLevel=ERROR"
          className="flex items-center space-x-1 text-sm text-blue-600 hover:text-blue-700"
        >
          <span>View All</span>
          <ExternalLink className="w-4 h-4" />
        </Link>
      </div>

      <div className="space-y-3">
        {errors.slice(0, 5).map((error) => (
          <div
            key={error.id}
            className="p-4 bg-red-50 border border-red-200 rounded-lg"
          >
            <div className="flex items-start justify-between mb-2">
              <div className="flex items-center space-x-2">
                <span className="badge badge-error">{error.logLevel}</span>
                <span className="text-xs text-gray-500">
                  {error.application}
                </span>
              </div>
              <span className="text-xs text-gray-500">
                {format(new Date(error.timestamp), 'yyyy-MM-dd HH:mm:ss')}
              </span>
            </div>
            
            <p className="text-sm text-gray-900 line-clamp-2">
              {error.message}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
```

## 9. 로그 검색 페이지(Elasticsearch 역색인)
### page.tsx
#### 경로 : study-monitoring-frontend/app/logs/page.tsx
```tsx
'use client';

import { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { searchLogs } from '../../lib/api';
import type { LogSearchResponse, LogEntry, LogLevel } from '../../types';
import { Search, Filter, Download, RefreshCw } from 'lucide-react';

export default function LogsPage() {
  const searchParams = useSearchParams();
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  
  // 검색 필터
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [logLevel, setLogLevel] = useState<LogLevel | ''>(
    (searchParams.get('logLevel') as LogLevel) || ''
  );
  const [index, setIndex] = useState('application-logs-*');
  const [page, setPage] = useState(0);
  const [pageSize] = useState(50);

  useEffect(() => {
    handleSearch();
  }, [page, logLevel]);

  async function handleSearch() {
    setLoading(true);
    try {
      const response = await searchLogs({
        index,
        keyword: keyword.trim() || undefined,
        logLevel: logLevel || undefined,
        from: page * pageSize,
        size: pageSize,
      });

      if (response.success) {
        setLogs(response.data.logs);
        setTotal(response.data.total);
      }
    } catch (error) {
      console.error('Failed to search logs:', error);
    } finally {
      setLoading(false);
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      setPage(0);
      handleSearch();
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* 헤더 */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Log Search
        </h1>
        <p className="text-gray-600">
          Search logs using Elasticsearch inverted index (tokenized search)
        </p>
      </div>

      {/* 검색 필터 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* 키워드 검색 */}
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Keyword (Tokenized Search)
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="Search in messages... (e.g., database error)"
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <p className="mt-1 text-xs text-gray-500">
              💡 Elasticsearch will tokenize your input automatically
            </p>
          </div>

          {/* 로그 레벨 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Log Level
            </label>
            <select
              value={logLevel}
              onChange={(e) => setLogLevel(e.target.value as LogLevel | '')}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Levels</option>
              <option value="INFO">INFO</option>
              <option value="WARN">WARN</option>
              <option value="ERROR">ERROR</option>
              <option value="DEBUG">DEBUG</option>
            </select>
          </div>

          {/* 인덱스 선택 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Index Pattern
            </label>
            <select
              value={index}
              onChange={(e) => setIndex(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            >
              <option value="application-logs-*">Application Logs</option>
              <option value="access-logs-*">Access Logs</option>
              <option value="error-logs-*">Error Logs</option>
            </select>
          </div>
        </div>

        {/* 액션 버튼 */}
        <div className="flex items-center justify-between mt-6 pt-6 border-t border-gray-200">
          <div className="text-sm text-gray-600">
            Found <span className="font-semibold text-gray-900">{total.toLocaleString()}</span> logs
          </div>
          
          <div className="flex space-x-2">
            <button
              onClick={handleSearch}
              disabled={loading}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? (
                <RefreshCw className="w-4 h-4 animate-spin" />
              ) : (
                <Search className="w-4 h-4" />
              )}
              <span>Search</span>
            </button>
            
            <button className="flex items-center space-x-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50">
              <Download className="w-4 h-4" />
              <span>Export</span>
            </button>
          </div>
        </div>
      </div>

      {/* 로그 결과 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <div className="text-center">
              <div className="w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
              <p className="text-gray-600">Searching logs...</p>
            </div>
          </div>
        ) : logs.length === 0 ? (
          <div className="text-center py-20">
            <Filter className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-600">No logs found</p>
            <p className="text-sm text-gray-500 mt-2">
              Try adjusting your search filters
            </p>
          </div>
        ) : (
          <>
            {/* 로그 테이블 */}
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Timestamp
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Level
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Application
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Message
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {logs.map((log) => (
                    <tr key={log.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {new Date(log.timestamp).toLocaleString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`badge badge-${
                          log.logLevel === 'ERROR' ? 'error' :
                          log.logLevel === 'WARN' ? 'warning' :
                          log.logLevel === 'INFO' ? 'info' : 'success'
                        }`}>
                          {log.logLevel}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {log.application}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900">
                        <div className="line-clamp-2">{log.message}</div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* 페이지네이션 */}
            <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
              <div className="text-sm text-gray-700">
                Showing <span className="font-medium">{page * pageSize + 1}</span> to{' '}
                <span className="font-medium">
                  {Math.min((page + 1) * pageSize, total)}
                </span>{' '}
                of <span className="font-medium">{total.toLocaleString()}</span> results
              </div>
              
              <div className="flex space-x-2">
                <button
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                  className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage(page + 1)}
                  disabled={(page + 1) * pageSize >= total}
                  className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
```

## 10. 메트릭 페이지
### page.tsx
#### 경로 : study-monitoring-frontend/app/metrics/page.tsx
```tsx
'use client';

import { useState, useEffect } from 'react';
import { getMetrics } from '../../lib/api';
import type { MetricsData, MetricType, ApplicationType } from '../../types';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { TrendingUp, Activity, AlertTriangle, Clock } from 'lucide-react';
import { format } from 'date-fns';

export default function MetricsPage() {
    const [metricsData, setMetricsData] = useState<MetricsData | null>(null);
    const [loading, setLoading] = useState(false);

    // 필터 상태
    const [application, setApplication] = useState<ApplicationType>('eng-study');
    const [metric, setMetric] = useState<MetricType>('tps');
    const [hours, setHours] = useState(1);

    useEffect(() => {
        fetchMetrics();

        // 30초마다 자동 갱신
        const interval = setInterval(fetchMetrics, 30000);
        return () => clearInterval(interval);
    }, [application, metric, hours]);

    async function fetchMetrics() {
        setLoading(true);
        try {
            const response = await getMetrics({ application, metric, hours });
            if (response.success) {
                setMetricsData(response.data);
            }
        } catch (error) {
            console.error('Failed to fetch metrics:', error);
        } finally {
            setLoading(false);
        }
    }

    const chartData = metricsData?.data.map(point => ({
        timestamp: format(new Date(point.timestamp * 1000), 'HH:mm:ss'),
        value: point.value,
    })) || [];

    const metricInfo = {
        tps: { label: 'TPS (Transactions Per Second)', unit: 'req/s', icon: Activity, color: '#3b82f6' },
        heap: { label: 'Heap Memory Usage', unit: '%', icon: TrendingUp, color: '#10b981' },
        error_rate: { label: 'Error Rate', unit: '%', icon: AlertTriangle, color: '#ef4444' },
        response_time: { label: 'Response Time (P95)', unit: 'seconds', icon: Clock, color: '#8b5cf6' },
    };

    const currentMetric = metricInfo[metric];

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* 헤더 */}
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-900 mb-2">
                    Metrics Dashboard
                </h1>
                <p className="text-gray-600">
                    Real-time application metrics from Prometheus
                </p>
            </div>

            {/* 필터 */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {/* 애플리케이션 선택 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Application
                        </label>
                        <select
                            value={application}
                            onChange={(e) => setApplication(e.target.value as ApplicationType)}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        >
                            <option value="eng-study">Eng-Study Backend</option>
                            <option value="monitoring">Monitoring Backend</option>
                        </select>
                    </div>

                    {/* 메트릭 선택 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Metric Type
                        </label>
                        <select
                            value={metric}
                            onChange={(e) => setMetric(e.target.value as MetricType)}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        >
                            <option value="tps">TPS</option>
                            <option value="heap">Heap Memory</option>
                            <option value="error_rate">Error Rate</option>
                            <option value="response_time">Response Time</option>
                        </select>
                    </div>

                    {/* 시간 범위 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            Time Range
                        </label>
                        <select
                            value={hours}
                            onChange={(e) => setHours(Number(e.target.value))}
                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                        >
                            <option value={1}>Last 1 Hour</option>
                            <option value={3}>Last 3 Hours</option>
                            <option value={6}>Last 6 Hours</option>
                            <option value={12}>Last 12 Hours</option>
                            <option value={24}>Last 24 Hours</option>
                        </select>
                    </div>
                </div>
            </div>

            {/* 차트 */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center space-x-3">
                        <div
                            className="w-10 h-10 rounded-lg flex items-center justify-center"
                            style={{ backgroundColor: `${currentMetric.color}20` }}
                        >
                            <currentMetric.icon className="w-5 h-5" style={{ color: currentMetric.color }} />
                        </div>
                        <div>
                            <h3 className="text-lg font-semibold text-gray-900">
                                {currentMetric.label}
                            </h3>
                            <p className="text-sm text-gray-500">
                                {application === 'eng-study' ? 'Eng-Study Backend' : 'Monitoring Backend'}
                            </p>
                        </div>
                    </div>

                    {loading && (
                        <div className="flex items-center space-x-2 text-sm text-gray-500">
                            <div className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
                            <span>Loading...</span>
                        </div>
                    )}
                </div>

                {chartData.length === 0 ? (
                    <div className="flex items-center justify-center h-96 text-gray-500">
                        No data available
                    </div>
                ) : (
                    <ResponsiveContainer width="100%" height={400}>
                        <LineChart data={chartData}>
                            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                            <XAxis
                                dataKey="timestamp"
                                tick={{ fill: '#6b7280', fontSize: 12 }}
                                angle={-45}
                                textAnchor="end"
                                height={80}
                            />
                            <YAxis
                                tick={{ fill: '#6b7280', fontSize: 12 }}
                                label={{
                                    value: currentMetric.unit,
                                    angle: -90,
                                    position: 'insideLeft',
                                    style: { fill: '#6b7280', fontSize: 12 }
                                }}
                            />
                            <Tooltip
                                contentStyle={{
                                    backgroundColor: 'white',
                                    border: '1px solid #e5e7eb',
                                    borderRadius: '8px',
                                    padding: '8px 12px',
                                }}
                                labelStyle={{ color: '#374151', fontWeight: 600 }}
                            />
                            <Legend />
                            <Line
                                type="monotone"
                                dataKey="value"
                                name={currentMetric.label}
                                stroke={currentMetric.color}
                                strokeWidth={2}
                                dot={{ fill: currentMetric.color, r: 3 }}
                                activeDot={{ r: 5 }}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                )}

                {/* 통계 요약 */}
                {chartData.length > 0 && (
                    <div className="mt-6 pt-6 border-t border-gray-200">
                        <div className="grid grid-cols-4 gap-4">
                            <div className="text-center">
                                <p className="text-sm text-gray-500 mb-1">Current</p>
                                <p className="text-lg font-semibold text-gray-900">
                                    {chartData[chartData.length - 1]?.value.toFixed(2)} {currentMetric.unit}
                                </p>
                            </div>
                            <div className="text-center">
                                <p className="text-sm text-gray-500 mb-1">Average</p>
                                <p className="text-lg font-semibold text-gray-900">
                                    {(chartData.reduce((sum, d) => sum + d.value, 0) / chartData.length).toFixed(2)} {currentMetric.unit}
                                </p>
                            </div>
                            <div className="text-center">
                                <p className="text-sm text-gray-500 mb-1">Min</p>
                                <p className="text-lg font-semibold text-gray-900">
                                    {Math.min(...chartData.map(d => d.value)).toFixed(2)} {currentMetric.unit}
                                </p>
                            </div>
                            <div className="text-center">
                                <p className="text-sm text-gray-500 mb-1">Max</p>
                                <p className="text-lg font-semibold text-gray-900">
                                    {Math.max(...chartData.map(d => d.value)).toFixed(2)} {currentMetric.unit}
                                </p>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
```

## 11 인프라 현황 페이지
### page.tsx
#### 경로 : study-monitoring-frontend/app/infrastructure/page.tsx
```tsx
'use client';

import { useEffect, useState } from 'react';
import { getProcesses } from '../../lib/api';
import type { ProcessStatus } from '../../types';
import { Server, Database, FileText, BarChart3, Cpu, MemoryStick, HardDrive } from 'lucide-react';

export default function InfrastructurePage() {
  const [processes, setProcesses] = useState<ProcessStatus[]>([]);
  const [summary, setSummary] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchInfrastructure();
    
    const interval = setInterval(fetchInfrastructure, 15000);
    return () => clearInterval(interval);
  }, []);

  async function fetchInfrastructure() {
    try {
      const response = await getProcesses();
      if (response.success) {
        setProcesses(response.data.processes);
        setSummary(response.data.summary);
      }
    } catch (error) {
      console.error('Failed to fetch infrastructure:', error);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-200px)]">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Loading infrastructure...</p>
        </div>
      </div>
    );
  }

  const serviceIcons: Record<string, any> = {
    BACKEND: Server,
    FRONTEND: FileText,
    DATABASE: Database,
    MONITORING: BarChart3,
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* 헤더 */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Infrastructure Overview
        </h1>
        <p className="text-gray-600">
          Kubernetes cluster status and resource usage
        </p>
      </div>

      {/* 요약 카드 */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <div className="flex items-center space-x-3">
              <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                <Server className="w-6 h-6 text-green-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Running Processes</p>
                <p className="text-2xl font-bold text-gray-900">
                  {summary.runningProcesses || 0}
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <div className="flex items-center space-x-3">
              <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                <Cpu className="w-6 h-6 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Avg CPU Usage</p>
                <p className="text-2xl font-bold text-gray-900">
                  {summary.avgCpuUsage?.toFixed(1) || 0}%
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <div className="flex items-center space-x-3">
              <div className="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
                <MemoryStick className="w-6 h-6 text-purple-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Avg Memory Usage</p>
                <p className="text-2xl font-bold text-gray-900">
                  {summary.avgMemoryUsage?.toFixed(1) || 0}%
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 프로세스 그리드 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {processes.map((process) => {
          const Icon = serviceIcons[process.processType] || Server;
          const isHealthy = process.status === 'RUNNING';

          return (
            <div
              key={process.processId}
              className={`bg-white rounded-xl shadow-sm border-2 p-6 transition-all ${
                isHealthy
                  ? 'border-green-200 hover:border-green-300'
                  : 'border-red-200 hover:border-red-300'
              }`}
            >
              {/* 헤더 */}
              <div className="flex items-start justify-between mb-6">
                <div className="flex items-center space-x-3">
                  <div
                    className={`w-12 h-12 rounded-lg flex items-center justify-center ${
                      isHealthy ? 'bg-green-100' : 'bg-red-100'
                    }`}
                  >
                    <Icon
                      className={`w-6 h-6 ${
                        isHealthy ? 'text-green-600' : 'text-red-600'
                      }`}
                    />
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900">
                      {process.processName}
                    </h3>
                    <p className="text-sm text-gray-500">{process.processType}</p>
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <div
                    className={`w-3 h-3 rounded-full ${
                      isHealthy ? 'bg-green-500 animate-pulse' : 'bg-red-500'
                    }`}
                  />
                  <span
                    className={`text-sm font-medium ${
                      isHealthy ? 'text-green-600' : 'text-red-600'
                    }`}
                  >
                    {process.status}
                  </span>
                </div>
              </div>

              {/* 리소스 사용률 */}
              <div className="space-y-4">
                {/* CPU */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      <Cpu className="w-4 h-4 text-gray-400" />
                      <span className="text-sm font-medium text-gray-700">CPU</span>
                    </div>
                    <span className="text-sm font-semibold text-gray-900">
                      {process.cpuUsage.toFixed(1)}%
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full transition-all ${
                        process.cpuUsage > 80
                          ? 'bg-red-500'
                          : process.cpuUsage > 60
                          ? 'bg-yellow-500'
                          : 'bg-green-500'
                      }`}
                      style={{ width: `${Math.min(process.cpuUsage, 100)}%` }}
                    />
                  </div>
                </div>

                {/* Memory */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      <MemoryStick className="w-4 h-4 text-gray-400" />
                      <span className="text-sm font-medium text-gray-700">Memory</span>
                    </div>
                    <span className="text-sm font-semibold text-gray-900">
                      {process.memoryUsage.toFixed(1)}%
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full transition-all ${
                        process.memoryUsage > 80
                          ? 'bg-red-500'
                          : process.memoryUsage > 60
                          ? 'bg-yellow-500'
                          : 'bg-blue-500'
                      }`}
                      style={{ width: `${Math.min(process.memoryUsage, 100)}%` }}
                    />
                  </div>
                </div>
              </div>

              {/* 추가 정보 */}
              <div className="mt-6 pt-6 border-t border-gray-200 grid grid-cols-2 gap-4">
                <div>
                  <p className="text-xs text-gray-500 mb-1">Uptime</p>
                  <p className="text-sm font-medium text-gray-900">{process.uptime}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-500 mb-1">Last Check</p>
                  <p className="text-sm font-medium text-gray-900">
                    {new Date(process.lastHealthCheck).toLocaleTimeString()}
                  </p>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
```

## 12. Dockerfile
### Dockerfile
#### 경로 : study-monitoring-frontend/Dockerfile
```dockerfile
# ==========================================
# Stage 1: Dependencies
# ==========================================
FROM node:22-alpine AS deps
RUN apk add --no-cache libc6-compat
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

# ==========================================
# Stage 2: Builder
# ==========================================
FROM node:22-alpine AS builder
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# 환경 변수 설정 (빌드 시)
ENV NEXT_PUBLIC_MONITORING_API=http://monitoring-backend-service:8081/api
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# ==========================================
# Stage 3: Runner
# ==========================================
FROM node:22-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Standalone 파일 복사
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3001

ENV PORT=3001
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
```



# TODO
<p>로그 Export 기능 구현(CSV/JSON)</p>
<p>알림 설정 UI</p>
<p>다크 모드 지원</p>
<p>실시간 WebSocket 연동</p>
<p>커스텀 대시보드 생성 기능</p>
<p>메트릭 알람 임계값 설정</p>


