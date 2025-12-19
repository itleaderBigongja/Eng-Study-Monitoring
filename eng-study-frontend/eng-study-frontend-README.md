Next.js 16 + TypeScript 기반의 영어 학습 웹 애플리케이션
📋 목차

프로젝트 개요
주요 기능
기술 스택
프로젝트 구조
시작하기
환경 설정
주요 컴포넌트
API 연동
배포

<h1>주요 특징</h1>
<p>현대적인 UI/UX: Tailwind CSS 기반의 반응형 디자인</p>
<p>보안 강화: HttpOnly Cookie 기반 인증</p>
<p>타입 안정성: TypeScript를 통한 타입 안전성 보장</p>
<p>컴포넌트 재사용성: 모듈화된 컴포넌트 구조</p>
<p>실시간 주소 검색: Daum 우편번호 API 연동</p>

<h1>주요 기능</h1>
## 1. 사용자 인증

| 기능        | 설명                            |
|-----------|-------------------------------|
| 회원가입      | 이메일/ID 중복 확인, 주소 검색 API 연동    |
| 로그인       | JWT 기반 인증, HttpOnly Cookie 저장 |
| 로그아웃      | 안전한 세션 종료                     |
| 자동 로그인 유지 | Refresh Token 기반 세션 유지        |
| 내 정보 조회   | 인증된 사용자 정보 표시                 |

## 2. 학습 기능
| 기능       | 설명              |
|----------|-----------------|
| 레슨 학습    | 초급/중급/고급 단계별 학습 |
| 단어장      | 플래시카드 기반 단어 암기  |
| 문법 학습    | 체계적인 문법 설명과 예문  |
| 퀴즈 & 테스트 | 실력 확인 테스트       |
| 학습 현황    | 개인별 통계 대시보드     |

## 3. UI/UX 특징
<p>반응형 디자인: 모바일/태블릿/데스크톱 대응</p>
<p>그라디언트 효과: 현대적인 색상 그라디언트</p>
<p>애니메이션: 부드러운 페이지 전환 효과</p>
<p>로딩 상태: 사용자 피드백을 위한 로딩 UI</p>
<p>에러 처리: 친절한 에러 메시지 표시</p>

<h1>기술 스택</h1>
## 핵심 환경
<p>Node.js: 22</p>
<p>Next.js: 16 (App Router)</p>
<p>TypeScript: 최신 버전</p>
<p>React: 18+</p>

## UI 라이브러리
<p>Tailwind CSS: 유틸리티 기반 CSS 프레임워크</p>
<p>React Hooks: useState, useEffect, useCallback 등</p>

## 상태 관리
<p>Custom Hooks: useAuth, useLessons, useVocabulary</p>
<p>React Context: 전역 상태 관리 (필요시)</p>

## API 통신
<p>Fetch API: 네이티브 HTTP 클라이언트</p>
<p>Custom API Client: 중앙화된 API 요청 관리</p>
<p>Credentials: HttpOnly Cookie 기반 인증</p>

## 외부 API
<p>Daum 우편번호 서비스: 주소 검색 기능</p>


<h1>프로젝트 구조</h1>

```json
eng-study-frontend/
├── app/                          # Next.js App Router
│   ├── favicon.ico
│   ├── globals.css              # 전역 스타일
│   ├── layout.tsx               # 루트 레이아웃
│   ├── page.tsx                 # 메인 페이지 ✅
│   │
│   ├── login/                   # 로그인
│   │   └── page.tsx             # 로그인 페이지 ✅
│   │
│   ├── registor/                # 회원가입
│   │   └── page.tsx             # 회원가입 페이지 ✅
│   │
│   ├── lessons/                 # 레슨 학습
│   ├── vocabulary/              # 단어장
│   ├── grammar/                 # 문법 학습
│   ├── practice/                # 퀴즈 & 테스트
│   └── profile/                 # 마이페이지
│
├── components/                   # React 컴포넌트
│   ├── common/                  # 공통 컴포넌트
│   │   ├── AddressInput.tsx    # 주소 검색 컴포넌트 ✅
│   │   ├── Button.tsx          # 버튼 컴포넌트
│   │   ├── Footer.tsx          # 푸터
│   │   ├── Header.tsx          # 헤더
│   │   └── Navigation.tsx      # 네비게이션
│   │
│   ├── lesson/                  # 레슨 관련
│   │   ├── LessonCard.tsx
│   │   ├── LessonPlayer.tsx
│   │   └── QuizComponent.tsx
│   │
│   └── vocabulary/              # 단어장 관련
│       ├── FlashCard.tsx
│       └── VocabCard.tsx
│
├── hooks/                        # Custom Hooks
│   ├── useAuth.ts               # 인증 훅 ✅
│   ├── useLessons.ts            # 레슨 훅
│   └── useVocabulary.ts         # 단어장 훅
│
├── lib/                          # 유틸리티 & API
│   ├── api.ts                   # API 클라이언트 ✅
│   ├── auth.ts                  # 인증 API ✅
│   └── utils.ts                 # 헬퍼 함수
│
├── types/                        # TypeScript 타입 정의
│   ├── auth.ts                  # 인증 타입 ✅
│   └── daum.ts                  # Daum API 타입 ✅
│
├── public/                       # 정적 파일
│
├── .env.local                    # 로컬 환경변수 ✅
├── .env.production               # 프로덕션 환경변수 ✅
├── next.config.ts                # Next.js 설정 ✅
├── Dockerfile                    # Docker 이미지 빌드
├── package.json                  # 의존성 관리
└── README.md                     # 이 문서
```

<h1>시작하기</h1>

## 사전 요구사항
<p>Node.js: 22 이상</p>
<p>npm 또는 yarn</p>
<p>백엔드 서버: eng-study (Spring Boot) 실행 중</p>

### 1. 프로젝트 클론
<p>git clone [repository-url]</p>
<p>    cd Monitoring/eng-study-frontend</p>

### 2. 의존성 설치
<p>npm install # 또는 yarn install</p>

### 3. 환경 변수 설정
<p>로컬 개발 환경 (.env.local)</p>
<p>envNEXT_PUBLIC_API_URL=http://localhost:8080/api</p>
<p>프로덕션 환경 (.env.production)</p>
<p>envNEXT_PUBLIC_API_URL=http://nginx-service/api</p>

### 4. 개발 서버 실행
<p>npm run dev # 또는 yarn dev</p>
<p>접속: http://localhost:3000</p>

### 5. 프로덕션 빌드
<p>npm run build</p>

### 6. 프로덕션 서버 실행
npm run start

<h1>환경 설정</h1>

## next.config.ts
### 주요 설정
<p>output: 'standalone': Docker 배포를 위한 독립형 빌드</p>
<p>reactStrictMode: 개발 모드에서 잠재적 문제 감지</p>
<p>remotePatterns: Next.js Image 최적화 허용 도메인</p>

```typescript
const nextConfig = {
  output: 'standalone',
  reactStrictMode: true,
  
  // 환경 변수
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
  },
  
  // 이미지 최적화
  images: {
    remotePatterns: [
      {
        protocol: 'http',
        hostname: 'localhost',
      },
      {
        protocol: 'http',
        hostname: 'nginx-service',
      },
    ],
  },
}
```

<h1>주요 컴포넌트</h1>

### 1.useAuth Hook(hooks/useAuth.ts)
<p>-> 사용자 인증 상태를 관리하는 커스텀 훅 입니다.</p>

#### 주요 기능
<p>자동 사용자 정보 로드</p>
<p>로그인/로그아웃 처리</p>
<p>토큰 자동 갱신</p>
<p>에러 상태 관리</p>

```typescript
const {
  user,              // 현재 사용자 정보
  loading,           // 로딩 상태
  error,             // 에러 메시지
  isAuthenticated,   // 인증 여부
  isAdmin,           // 관리자 여부
  login,             // 로그인 함수
  logout,            // 로그아웃 함수
  register,          // 회원가입 함수
  refresh,           // 토큰 갱신 함수
  fetchUser,         // 사용자 정보 재조회
} = useAuth();
```

### 2. AddressInput Component (components/common/AddressInput.tsx)
<p>-> Daum 우편번호 API를 사용한 주소 검색 컴포넌트</p>

#### 주요 기능
<p>우편번호 검색 팝업</p>
<p>도로명/지번 주소 선택</p>
<p>시/도, 시/군/구, 법정동 정보 자동 추출</p>
<p>상세 주소 입력</p>

```typescript
<AddressInput
  value={{
    postalCode: '',
    address: '',
    addressDetail: '',
    addressType: '',
    sido: '',
    sigungu: '',
    bname: '',
  }}
  onChange={(address) => setAddress(address)}
  error={errors.address}
/>
```

### API Client(lib/api.ts)
<p>-> 중앙화된 API 요청 클라이언트</p>

#### 주요 기능
<p>자동 인증 헤더 추가</p>
<p>401 에러 자동 처리 (로그아웃)</p>
<p>중앙화된 에러 핸들링</p>
<p>TypeScript 타입 지원</p>

```typescript
import apiClient from '@/lib/api';

// GET 요청
const data = await apiClient.get('/lessons');

// POST 요청
const result = await apiClient.post('/auth/login', {
  loginId: 'user123',
  password: 'pass1234',
});

// PUT 요청
await apiClient.put('/user/profile', userData);

// DELETE 요청
await apiClient.delete('/lessons/1');
```

<h1>API 연동</h1>

## 인증 API(lib/auth.ts)
### 1. 회원가입
```typescript
const result = await register({
  loginId: 'john_doe',
  password: 'password123',
  email: 'john@example.com',
  fullName: 'John Doe',
});

// 응답: { success: true, message: '회원가입 성공', data: { user } }
```

### 2. 로그인
```typescript
const result = await login({
  loginId: 'john_doe',
  password: 'password123',
});

// HttpOnly Cookie에 JWT 저장됨
```

### 3. 로그아웃
```typescript
await logout();
// 쿠키 삭제 및 세션 종료
```

### 4. 내 정보 조회
```typescript
const result = await getMyInfo();
// 응답: { success: true, data: { usersId, loginId, fullName, email } }
```

### 5. 토큰 갱신
```typescript
const result = await refreshToken();
// 자동으로 Access Token 갱신
```

### 6. 중복 확인
```typescript
// 로그인 ID 중복 확인
const result = await checkLoginIdAvailability('john_doe');
// 응답: { success: true, data: { available: true/false } }

// 이메일 중복 확인
const result = await checkEmailAvailability('john@example.com');
```

### API 요청 흐름
```typescript
┌─────────────┐
│ 사용자 요청   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ lib/auth.ts │ ◄─── credentials: 'include' (Cookie 자동 전송)
└──────┬──────┘
       │
       ▼
┌─────────────────────────┐
│ Spring Boot Backend     │
│ localhost:8080/api      │ ◄─── HttpOnly Cookie 검증
└──────┬──────────────────┘
       │
       ▼
┌─────────────┐
│ PostgreSQL  │
└─────────────┘
```

## 페이지 구성
### 1. 메인 페이지 (app/page.tsx)
#### 주요 기능:
<p>로그인/비로그인 사용자 구분 UI</p>
<p>학습 기능 소개 (레슨, 단어장, 문법, 퀴즈)</p>
<p>로그인 사용자: 학습 현황 대시보드</p>
<p>Hero Section + CTA</p>

#### 화면 구성
```typescript
┌────────────────────────────────┐
│ Header (로그인/로그아웃)          │
├────────────────────────────────┤
│ Hero Section                   │
│ "즐겁게 배우는 영어 학습 플랫폼"   │
├────────────────────────────────┤
│ Features Section               │
│ [레슨] [단어장] [문법] [퀴즈]    │
├────────────────────────────────┤
│ Stats Section (로그인 시)       │
│ 완료한 레슨 | 학습한 단어 | 평균점수│
├────────────────────────────────┤
│ CTA Section (비로그인 시)       │
│ "지금 바로 시작하세요"           │
└────────────────────────────────┘
```

### 2. 회원가입 페이지(app/register/page.tsx)
#### 입력필드
<p>-> 로그인 ID (중복 확인)</p>
<p>-> 비밀번호 + 비밀번호 확인</p>
<p>-> 이메일 (중복 확인)</p>
<p>-> 이름</p>
<p>-> 주소 (Daum API 연동)</p>

#### 유효성 검사
<p>-> 필수 입력 확인</p>
<p>-> 이메일 형식 검증</p>
<p>-> 비밀번호 일치 여부</p>
<p>-> 실시간 중복 확인</p>

### 3. 로그인 페이지(app/login/page.tsx)
#### 주요 기능
<p>-> 로그인 ID + 비밀번호 입력</p>
<p>-> 로그인 실패 시 에러 메시지</p>
<p>-> 회원가입 링크</p>
<p>-> 로그인 성공 시 메인 페이지 이동</p>

<h1>배포</h1>

## Docker 빌드
### 1. Docker 이미지 빌드
```terminaloutput
# 프로젝트 루트에서
docker build -t eng-study-frontend:local -f eng-study-frontend/Dockerfile eng-study-frontend/
```

### 2. Kubernetes 배포
```terminaloutput
# 배포 스크립트 실행
./deploy-local.sh
```

### 3. Kubernetes 접속 정보

| 서비스     | URL                        | 설명            |
|---------|----------------------------|---------------|
| 프론트엔드   | http://localhost:30080     | Nginx를 통한 접속  |
| 백엔드 API | http://localhost:30080/api | Nginx 리버스 프록시 |

### 4. Docker 이미지 정보
<p>이미지명: eng-study-frontend:local</p>
<p>포트: 3000 (컨테이너 내부)</p>
<p>빌드 타입: standalone</p>

<h1>보안</h1>

## 1. HttpOnly Cookie
### 장점
<p>-> JavaScript로 접근 불가 (XSS 방지)</p>
<p>-> 자동으로 요청에 포함됨</p>
<p>-> SameSite 속성으로 CSRF 방지</p>

### 2. 환경 변수 관리
```typescript
// ❌ 잘못된 방법: 하드코딩
const API_URL = 'http://localhost:8080/api';

// ✅ 올바른 방법: 환경 변수 사용
const API_URL = process.env.NEXT_PUBLIC_API_URL;
```

### 3. 에러 처리
```typescript
try {
  const result = await login(data);
  if (result.success) {
    // 성공 처리
  } else {
    // 에러 메시지 표시
    setError(result.message);
  }
} catch (error) {
  // 네트워크 에러 등
  console.error('Login failed:', error);
}
```

