영어 학습 플랫폼 & 모니터링 시스템
Spring Boot + Next.js + Kubernetes 기반의 마이크로 서비스 아키텍처

[ 목차 ]
- 프로젝트 개요
- 시스템 아키텍처
- 기술 스택
- 프로젝트 구조
- 시작하기
- 개발 가이드
- 배포 가이드
- API 문서
- 보안
- 트러블 슈팅


<h3>[ 주요 기능 ]</h3>
<h3>영어 학습 사이트</h3>
- 사용자 인증(회원가입/로그인) : HttpOnly Cookie 기반
- 레슨 관리(초급/중급/고급)
- 단어장 학습
- 문법 학습
- 퀴즈 및 테스트
- 학습 진도 추적

<h3>모니터링 시스템</h3>
- Prometheus 메트릭 수집 및 시각화
- Elasticsearch 로그 수집 및 검색
- Kubernetes Pod/Node 상태 모니터링
- 실시간 대시보드
- 알림 시스템

<h3>🏗️ 시스템 아키텍처</h3>
![시스템 아키텍처 전체 구조.png](%E1%84%89%E1%85%B5%E1%84%89%E1%85%B3%E1%84%90%E1%85%A6%E1%86%B7%20%E1%84%8B%E1%85%A1%E1%84%8F%E1%85%B5%E1%84%90%E1%85%A6%E1%86%A8%E1%84%8E%E1%85%A5%20%E1%84%8C%E1%85%A5%E1%86%AB%E1%84%8E%E1%85%A6%20%E1%84%80%E1%85%AE%E1%84%8C%E1%85%A9.png)

| 구분        | 로컬 개발                                                            | Kubernetes 배포            |
|-----------|------------------------------------------------------------------|--------------------------|
| 실행 방식     | npm run dev / ./mvnw spring-boot:run                             | Docker Container(Pod)    |
| 접속 주소     | localhost:3000, localhost:8080                                   | localhost:30080(NodePort) |
| 수정 반영     | Hot Reload(자동)                                                   | 재빌드 + 재배포 필요             |
| DB접속      | localhost:5432(Docker 직접)                                        | Port Forward 필요          |
| DB 접속 명령어 | kubectl port-forward -n eng-study service/postgres-service 5432:5432 |                          |
| 용도        | 빠른 개발 및 테스트                                                      | 프로덕션 환경 시뮬레이션            |

<h3>⚙️ 백엔드 기술 스택 목록</h3>
1. 핵심 환경 및 프레임워크 
- Java Version: 21 (OpenJDK)
- Spring Boot: 3.5.7

2. 주요 의존성 (Dependencies)
- 웹 개발: spring-boot-starter-web (RESTful API)
- 데이터베이스: spring-boot-starter-jdbc
- DB 드라이버: postgresql
- 보안: spring-boot-starter-security
- 인증 토큰: jjwt-api, jjwt-impl, jjwt-jackson (JWT 구현)

3. 모니터링 및 유틸리티
- 모니터링: spring-boot-starter-actuator
- 메트릭 수집: micrometer-registry-prometheus
- 데이터 검증: spring-boot-starter-validation
- 코드 간소화: lombok
- 설정 암호화: jasypt-spring-boot-starter

<h3>⚙️ 프론트엔드 기술 스택 목록</h3>
- Node.js 22
- Next.js 16 (App Router)
- TypeScript
- Tailwind CSS
- React Context API (상태 관리)

<h3>⚙️ 인프라 기술 스택 목록</h3>
- Docker Desktop (Kubernetes 내장)
- Kubernetes 1.34
- Nginx (Reverse Proxy)
- Elasticsearch 8.11
- Prometheus (최신)

<h3>⚙️ 모니터링 기술 스택 목록</h3>
- Prometheus (메트릭 수집)
- Elasticsearch (로그 수집)
- Spring Boot Actuator
- Micrometer (메트릭 라이브러리)

<h3>프로젝트 구조</h3>
Monitoring/
├── eng-study/                          # 영어 학습 백엔드
│   ├── src/
│   │   └── main/
│   │       ├── java/.../engstudy/
│   │       │   ├── config/            # 설정 클래스
│   │       │   ├── controller/        # REST API
│   │       │   ├── domain/
│   │       │   │   ├── dto/           # 요청/응답 DTO
│   │       │   │   └── vo/            # 테이블 매핑 VO
│   │       │   ├── mapper/            # MyBatis Mapper
│   │       │   ├── service/           # 비즈니스 로직
│   │       │   │   └── impl/
│   │       │   └── util/              # 유틸리티
│   │       └── resources/
│   │           ├── mapper/            # MyBatis XML
│   │           ├── application.yml
│   │           └── application-prod.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── eng-study-frontend/                 # 영어 학습 프론트엔드
│   ├── src/
│   │   ├── app/                       # Next.js App Router
│   │   │   ├── login/
│   │   │   ├── register/
│   │   │   └── page.tsx
│   │   ├── components/                # React 컴포넌트
│   │   ├── contexts/                  # React Context
│   │   │   └── AuthContext.tsx
│   │   ├── lib/                       # API 클라이언트
│   │   │   ├── api.ts
│   │   │   └── auth.ts
│   │   └── hooks/                     # 커스텀 훅
│   ├── Dockerfile
│   ├── next.config.js
│   └── package.json
│
├── study-monitoring/                   # 모니터링 백엔드
│   ├── src/
│   │   └── main/
│   │       ├── java/.../monitoring/
│   │       └── resources/
│   │           └── application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── study-monitoring-frontend/          # 모니터링 프론트엔드
│   ├── src/
│   │   ├── app/
│   │   │   ├── metrics/
│   │   │   ├── logs/
│   │   │   └── infrastructure/
│   │   └── lib/
│   │       ├── prometheus.ts
│   │       └── elasticsearch.ts
│   ├── Dockerfile
│   └── package.json
│
├── k8s-local/                          # Kubernetes 설정
│   ├── 01-namespace.yaml
│   ├── 02-postgresql.yaml
│   ├── 03-elasticsearch.yaml
│   ├── 04-prometheus.yaml
│   ├── 05-eng-study-backend.yaml
│   ├── 06-eng-study-frontend.yaml
│   ├── 07-monitoring-backend.yaml
│   ├── 08-monitoring-frontend.yaml
│   ├── 09-nginx.yaml
│   └── db-init-configmap.yaml
│
├── db/
│   └── init/                           # PostgreSQL 초기화
│       ├── 01-schema.sql
│       └── 02-seed-data.sql
│
├── prometheus/
│   └── prometheus.yml                  # Prometheus 설정
│
├── cleanup-local.sh                    # 전체 삭제
├── build-local.sh                      # Docker 빌드
└── deploy-local.sh                     # Kubernetes 배포


## 시작하기
사전 요구사항 필수 설치
- Docker Desktop(Kubernetes 활성화)
- Java 21(OpenJDK)
- Node.js 22+
- Maven 3.9+

### <font color="Yellow">1. Docker Desktop Kubernetes 활성화</font>
#### Docker Desktop -> Settings -> Kubernetes
#### Enable Kubernetes 체크
#### Apply & Restart

#### 확인
kubectl version --client
kubectl cluster-info

### <font color="Yellow">2. 초기설정</font>
1. 프로젝트 클론
- git init
- git clone <repository-url>
- cd Monitoring

### <font color="Yellow">3. 환경 변수 설정( 프론트 엔드 )</font>
#### <font color="Aquamarine">eng-study-frontend/.env.local 파일( 영어 학습 )</font>
<p>echo "NEXT_PUBLIC_API_URL=http://localhost:8080/api" > eng-study-frontend/.env.local</p> 
또는
<p>vi eng-study-frontend/.env.local</p>
<p>내용 추가 : "NEXT_PUBLIC_API_URL=http://localhost:8080/api"</p>

#### <font color="Aquamarine">study-monitoring-frontend/.env.local 파일( 모니터링 )</font>
<p>echo "NEXT_PUBLIC_MONITORING_API=http://localhost:8081/api" -> study-monitoring-frontend/.env.local</p>
또는
<p>vi study-monitoring-frontend</p>
<p>내용 추가 : "NEXT_PUBLIC_MONITORING_API=http://localhost:8081/api"</p>

<h1>💻 개발 가이드(로컬 개발 환경)</h1>

## <font color="Yellow">1. PostgreSQL 실행</font>
### <font color="Aquamarine">Docker Compose로 DB만 실행</font>
<p>docker-compose up -d postgres</p>

### <font color="Aquamarine">Kubernetes에서 DB만 실행</font>
<p>kubectl apply -f k8s-local/01-namespace.yaml</p>
<p>kubectl apply -f k8s-local/db-init-configmap.yaml</p>
<p>kubectl apply -f k8s-local/02-postgresql.yaml</p>

## <font color="Yellow">2.백엔드 실행(Spring Boot)</font>
<p>1. cd eng-study</p>
<p>2. ./mvnw spring-boot:run</p>
<p>-> http://localhost:8080</p>

또는 

eng-study/src/main/java/EngStudyApplication.java에서 부트 실행
## <font color="Yellow">3.프론트엔드 실행(Next.js)</font>
### <font color="Aquamarine">터미널 : eng-study-frontend</font>
<p>1. cd eng-study-frontend</p>
<p>2. npm install</p>
<p>3. npm run dev</p>
<p>-> http://localhost:3000</p>

### <font color = "Aquamarine">터미널 : study-monitoring-frontend</font>
<p>1. cd study-monitoring-frontend</p>
<p>2. npm install</p>
<p>3. npm run dev</p>
<p>-> httpL://localhost:3001</p>

## <font color="Yellow">4. 배포 가이드( Kubernetes 배포 )</font>
### <font color="Aquamarine">1. 전체 삭제(재시작)</font>
<p>명령어 : ./cleanup-local.sh</p>

### <font color="Aquamarine">2. Docker 이미지 빌드</font>
<p>명령어 : ./build-local.sh</p>

| 빌드 결과 | 프론트엔드                  | 백엔드                             |
|------|------------------------|---------------------------------|
| 영어 학습 | eng-study:local        | eng-study-frontend:local        |
| 모니터링 | study-monitoring:local | study-monitoring-frontend:local |

### <font color="Aquamarine">3. 배포 확인</font>
<p>[ Pod 상태 확인 ]</p>
<p>명령어 : kubectl get pods -n eng-study</p>
<p>명령어 : kubectl get pods -n monitoring</p>

<p>[ 서비스 확인 ]</p>
<p>명령어 : kubectl get svc -n eng-study</p>
<p>명령어 : kubectl get svc -n monitoring</p>

<p>[ 로그 확인 ]</p>
<p>명령어 : kubectl logs -f deployment/eng-study-backend -n eng-study</p>
<p>명령어 : kubectl logs -f deployment/monitoring-backend -n eng-study</p>

### <font color="Aquamarine">접속(웹 브라우저)</font>
<p>[ 영어 학습 사이트 ]</p>
-> http://localhost:30080

<p>[ 모니터링 대시보드 ]</p>
-> http://localhost:30080/monitoring

### <font color="Aquamarine">포트번호 설명</font>
#### 30080 : Kubernetes NodePort(외부 접속용)
- Nginx가 30080으로 노출
- 내부적으로 3000,30001, 8080, 8081로 라우팅

#### 3000, 3001 : Next.js 컨테이너 내부 포트
#### 8080, 8081 : SpringBoot 컨테이너 내부 포트

### <font color="Aquamarine">PostgreSQL 접속(DBEaver)</font>
#### 새로운 터미널 창에서 Port Forward
-> kubectl port-forward -n eng-study service/postgres-service 5432:5432

#### DBEaver 설정 정보

| 프로퍼티     | 값         |
|----------|-----------|
| Host     | localhost |
| Port     | 5432      |
| Database | DEV_DB    |
| Username | rnbsoft   |
| Password | rnbsoft   |


<h1>보안</h1>

### 적용된 보안 기능
#### 1. HttpOnly Cookie
```java
// ❌ 취약: localStorage
localStorage.setItem('token', token);        

// ✅ 안전: HttpOnly Cookie
// JavaScript 접근 불가, XSS 방어
Set-Cookie: accessToken=...; HttpOnly; SameSite=Lax
```

#### 2. Input Validation
```java
// Spring Boot 백엔드
@NotBlank(message = "사용자명은 필수입니다")
@Size(min = 3, max = 50)
@Pattern(regexp = "^[a-zA-Z0-9_-]+$")
private String username;

// Next.js 프론트엔드
<input
  pattern="[a-zA-Z0-9_-]+"
  minLength={3}
  maxLength={50}
  required
/>
```

#### 3. 비밀번호 암호화
```java
// BCrypt (Salt + Hash)
String hashed = passwordEncoder.encode("password");
// → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

#### 4. SQL Injection 방어
```xml
<!-- MyBatis PreparedStatement -->
<select id="findByUsername" resultMap="UserResultMap">
    SELECT * FROM users WHERE username = #{username}
</select>
<!-- #{username}은 자동으로 파라미터 바인딩 -->
```

#### 5. CORS 설정
```java
@CrossOrigin(
    origins = {"http://localhost:3000", "http://nginx-service"},
    allowCredentials = "true"  // Cookie 전송 허용
)
```

보안 체크리스트
- HttpOnly Cookie (XSS 방어)
- SameSite=Lax (CSRF 방어)
- BCrypt 비밀번호 암호화
- Input Validation (백엔드 + 프론트엔드)
- SQL Injection 방어 (MyBatis)
- XSS 방어 (React 자동 이스케이프)


<h1>데이터베이스</h1>

#### 1. USERS 테이블
```postgresql
-- 사용자 테이블
CREATE TABLE USERS (
    USERS_ID                    BIGSERIAL               PRIMARY KEY,
    LOGIN_ID                    VARCHAR(50)             UNIQUE NOT NULL,
    EMAIL                       VARCHAR(100)            UNIQUE NOT NULL,
    PASSWORD                    VARCHAR(255)            NOT NULL,
    FULL_NAME                   VARCHAR(100),
    -- 주소 정보(다음 주소 API)
    POSTAL_CODE                 VARCHAR(10),
    ADDRESS                     VARCHAR(255),
    ADDRESS_DETAIL              VARCHAR(255),
    ADDRESS_TYPE                VARCHAR(20),
    SIDO                        VARCHAR(50),
    SIGUNGU                     VARCHAR(50),
    BNAME                       VARCHAR(50),
    LAST_LOGIN                  TIMESTAMP,
    IS_ACTIVE                   BOOLEAN                 DEFAULT TRUE,
    ROLE                        VARCHAR(20)             DEFAULT 'USER' CHECK ( ROLE IN ('USER', 'ADMIN')),
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CREATED_ID                  VARCHAR(50)             NOT NULL,
    UPDATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    UPDATED_ID                  VARCHAR(50)
);

-- 테이블 코멘트
COMMENT ON TABLE USERS IS '사용자 정보 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN USERS.USERS_ID        IS '사용자 고유 ID(채번)';
COMMENT ON COLUMN USERS.LOGIN_ID        IS '로그인ID(3-50자, 영문/숫자/_/-만 가능 계정 역할)';
COMMENT ON COLUMN USERS.EMAIL           IS '이메일 주소(고유값)';
COMMENT ON COLUMN USERS.PASSWORD        IS '암호화된 비밀번호(BCrypt)';
COMMENT ON COLUMN USERS.FULL_NAME       IS '사용자 실명';
COMMENT ON COLUMN USERS.POSTAL_CODE     IS '우편번호';
COMMENT ON COLUMN USERS.ADDRESS         IS '기본주소(도로명 또는 지번주소)';
COMMENT ON COLUMN USERS.ADDRESS_DETAIL  IS '상세주소(동/호수 등)';
COMMENT ON COLUMN USERS.ADDRESS_TYPE    IS '주소 타입(R: 도로명 / J: 지번)';
COMMENT ON COLUMN USERS.SIDO            IS '시/도(예: 서울특별시, 경기도)';
COMMENT ON COLUMN USERS.SIGUNGU         IS '시/군/구(예: 영등포구, 수원시)';
COMMENT ON COLUMN USERS.BNAME           IS '동 이름(영등포본동/여의도동)';
COMMENT ON COLUMN USERS.LAST_LOGIN      IS '마지막 로그인 일시';
COMMENT ON COLUMN USERS.IS_ACTIVE       IS '계정 활성화 여부(true: 활성 / false: 비활성)';
COMMENT ON COLUMN USERS.ROLE            IS '사용자 권한(USER: 일반 사용자 / ADMIN: 관리자)';
COMMENT ON COLUMN USERS.CREATED_AT      IS '생성일시';
COMMENT ON COLUMN USERS.CREATED_ID      IS '생성자ID';
COMMENT ON COLUMN USERS.UPDATED_AT      IS '수정일시';
COMMENT ON COLUMN USERS.UPDATED_ID      IS '수정자ID';
```

2. COMMON_CODE 테이블
```postgresql
-- 계층형 공통코드 테이블
CREATE TABLE COMMON_CODE (
     COMMON_CODE_ID                  BIGSERIAL           PRIMARY KEY,
     CODE_TYPE                       VARCHAR(50)         NOT NULL,
     MAJOR_CODE                      VARCHAR(20)         NOT NULL,
     MIDDLE_CODE                     VARCHAR(20),
     MINOR_CODE                      VARCHAR(20),
     CODE_NAME                       VARCHAR(100)        NOT NULL,
     CODE_NAME_EN                    VARCHAR(100),
     CODE_VALUE                      VARCHAR(100),
     CODE_DESCRIPTION                TEXT,
     PARENT_CODE_ID                  BIGINT              REFERENCES COMMON_CODE(COMMON_CODE_ID) ON DELETE CASCADE,
     CODE_LEVEL                      INTEGER             NOT NULL DEFAULT 1,
     SORT_ORDER                      INTEGER             DEFAULT 0,
     IS_ACTIVE                       BOOLEAN             DEFAULT TRUE,
     CREATED_AT                      TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
     CREATED_ID                      VARCHAR(50)         NOT NULL,
     UPDATED_AT                      TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
     UPDATED_ID                      VARCHAR(50),
     UNIQUE(CODE_TYPE, MAJOR_CODE, MIDDLE_CODE, MINOR_CODE)
);

-- 테이블 코멘트
COMMENT ON TABLE COMMON_CODE IS '계층형 공통코드 관리 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN COMMON_CODE.COMMON_CODE_ID        IS '공통코드 고유 ID';
COMMENT ON COLUMN COMMON_CODE.CODE_TYPE             IS '코드 유형(LEVEL, QUESTION_TYPE 등)';
COMMENT ON COLUMN COMMON_CODE.MAJOR_CODE            IS '대분류 코드';
COMMENT ON COLUMN COMMON_CODE.MIDDLE_CODE           IS '중분류 코드';
COMMENT ON COLUMN COMMON_CODE.MINOR_CODE            IS '소분류 코드';
COMMENT ON COLUMN COMMON_CODE.CODE_NAME             IS '코드명(한글)';
COMMENT ON COLUMN COMMON_CODE.CODE_NAME_EN          IS '코드명(영문)';
COMMENT ON COLUMN COMMON_CODE.CODE_VALUE            IS '코드값';
COMMENT ON COLUMN COMMON_CODE.CODE_DESCRIPTION      IS '코드 설명';
COMMENT ON COLUMN COMMON_CODE.PARENT_CODE_ID        IS '상위 코드 ID(계층 구조)';
COMMENT ON COLUMN COMMON_CODE.CODE_LEVEL            IS '코드 레벨(1: 대분류, 2: 중분류, 3: 소분류)';
COMMENT ON COLUMN COMMON_CODE.SORT_ORDER            IS '정렬 순서';
COMMENT ON COLUMN COMMON_CODE.IS_ACTIVE             IS '사용 여부';
COMMENT ON COLUMN COMMON_CODE.CREATED_AT            IS '생성 일시';
COMMENT ON COLUMN COMMON_CODE.CREATED_ID            IS '생성자 ID';
COMMENT ON COLUMN COMMON_CODE.UPDATED_AT            IS '수정 일시';
COMMENT ON COLUMN COMMON_CODE.UPDATED_ID            IS '수정자 ID';

-- 인덱스 생성
CREATE INDEX IDX_COMMON_CODE_TYPE       ON COMMON_CODE(CODE_TYPE);
CREATE INDEX IDX_COMMON_CODE_MAJOR      ON COMMON_CODE(MAJOR_CODE);
CREATE INDEX IDX_COMMON_CODE_PARENT     ON COMMON_CODE(PARENT_CODE_ID);
CREATE INDEX IDX_COMMON_CODE_ACTIVE_ON  ON COMMON_CODE(IS_ACTIVE);
CREATE INDEX IDX_COMMON_CODE_TYPE_LEVEL ON COMMON_CODE(CODE_TYPE, CODE_LEVEL);
```

3. LESSONS 테이블
```postgresql
-- 레슨 테이블
CREATE TABLE LESSONS (
     LESSONS_ID                  BIGSERIAL               PRIMARY KEY,
     TITLE                       VARCHAR(200)            NOT NULL,
     DESCRIPTION                 TEXT,
     LEVEL                       VARCHAR(20)             CHECK (LEVEL IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
     CONTENT                     TEXT,
     DURATION_MINUTES            INTEGER,
     ORDER_INDEX                 INTEGER,
     IS_PUBLISHED                BOOLEAN                 DEFAULT FALSE,
     CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
     CREATED_ID                  VARCHAR(50)             NOT NULL,
     UPDATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
     UPDATED_ID                  VARCHAR(50)
);

-- 테이블 코멘트
COMMENT ON TABLE LESSONS IS '영어 학습 레슨 정보 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN LESSONS.LESSONS_ID        IS '레슨 고유 ID';
COMMENT ON COLUMN LESSONS.TITLE             IS '레슨 제목';
COMMENT ON COLUMN LESSONS.DESCRIPTION       IS '레슨 설명';
COMMENT ON COLUMN LESSONS.LEVEL             IS '난이도(BEGINNER: 초급, INTERMEDIATE: 중급, ADVANCED: 고급)';
COMMENT ON COLUMN LESSONS.CONTENT           IS '레슨 내용 (본문)';
COMMENT ON COLUMN LESSONS.DURATION_MINUTES  IS '예상 학습 시간 (분)';
COMMENT ON COLUMN LESSONS.ORDER_INDEX       IS '레슨 정렬 순서';
COMMENT ON COLUMN LESSONS.IS_PUBLISHED      IS '공개 여부';
COMMENT ON COLUMN LESSONS.CREATED_AT        IS '생성 일시';
COMMENT ON COLUMN LESSONS.CREATED_ID        IS '생성자 ID';
COMMENT ON COLUMN LESSONS.UPDATED_AT        IS '수정 일시';
COMMENT ON COLUMN LESSONS.UPDATED_ID        IS '수정자 ID';

-- 인덱스 생성
CREATE INDEX IDX_LESSONS_LEVEL              ON LESSONS(LEVEL);
CREATE INDEX IDX_LESSONS_PUBLISHED          ON LESSONS(IS_PUBLISHED);
CREATE INDEX IDX_LESSONS_ORDER              ON LESSONS(ORDER_INDEX);
```

3. VOCABULARY 테이블
```postgresql
-- 단어장 테이블
CREATE TABLE VOCABULARY (
    VOCABULARY_ID               BIGSERIAL               PRIMARY KEY,
    WORD                        VARCHAR(100)            NOT NULL,
    PRONUNCIATION               VARCHAR(100),
    MEANING                     TEXT                    NOT NULL,
    EXAMPLE_SENTENCE            TEXT,
    EXAMPLE_SENTENCE_MEANING    TEXT,
    IMAGE_URL                   VARCHAR(500),
    IMAGE_FILE_NAME             VARCHAR(255),
    LESSONS_ID                  BIGINT                  REFERENCES LESSONS(LESSONS_ID) ON DELETE SET NULL,
    DIFFICULTY_LEVEL            VARCHAR(20),
    WORD_TYPE                   VARCHAR(20),
    SYNONYMS                    TEXT,
    ANTONYMS                    TEXT,
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CREATED_ID                  VARCHAR(50)             NOT NULL,
    UPDATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    UPDATED_ID                  VARCHAR(50)
);

-- 테이블 코멘트
COMMENT ON TABLE VOCABULARY                             IS '영어 단어 정보 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN VOCABULARY.VOCABULARY_ID              IS '단어 고유 ID';
COMMENT ON COLUMN VOCABULARY.WORD                       IS '영어 단어';
COMMENT ON COLUMN VOCABULARY.PRONUNCIATION              IS '발음 기호(예: /həˈloʊ/)';
COMMENT ON COLUMN VOCABULARY.MEANING                    IS '단어 뜻(한글)';
COMMENT ON COLUMN VOCABULARY.EXAMPLE_SENTENCE           IS '예문(영어)';
COMMENT ON COLUMN VOCABULARY.EXAMPLE_SENTENCE_MEANING   IS '예문 뜻(한글)';
COMMENT ON COLUMN VOCABULARY.IMAGE_URL                  IS '단어 관련 이미지 URL';
COMMENT ON COLUMN VOCABULARY.IMAGE_FILE_NAME            IS '이미지 파일명';
COMMENT ON COLUMN VOCABULARY.LESSONS_ID                 IS '연관된 레슨 ID(외래키)';
COMMENT ON COLUMN VOCABULARY.DIFFICULTY_LEVEL           IS '단어 난이도';
COMMENT ON COLUMN VOCABULARY.WORD_TYPE                  IS '품사(noun, verb, abjective 등)';
COMMENT ON COLUMN VOCABULARY.SYNONYMS                   IS '유의어(쉼표로 구분)';
COMMENT ON COLUMN VOCABULARY.ANTONYMS                   IS '반의어(쉼표로 구분)';
COMMENT ON COLUMN VOCABULARY.CREATED_AT                 IS '생성 일시';
COMMENT ON COLUMN VOCABULARY.CREATED_ID                 IS '생성자 ID';
COMMENT ON COLUMN VOCABULARY.UPDATED_AT                 IS '수정 일시';
COMMENT ON COLUMN VOCABULARY.UPDATED_ID                 IS '수정자 ID';

-- 인덱스 생성
CREATE INDEX IDX_VOCABULARY_WORD                        ON VOCABULARY(WORD);
CREATE INDEX IDX_VOCABULARY_LESSONS                     ON VOCABULARY(LESSONS_ID);
CREATE INDEX IDX_VOCABULARY_DIFFICULTY                  ON VOCABULARY(DIFFICULTY_LEVEL);
```

5. VOCABULARY_IMAGES 테이블
```postgresql
-- 단어 이미지 테이블 (1개 단어에 여러 이미지 가능)
-- 단어 이미지 테이블 (1개 단어에 여러 이미지 가능)
CREATE TABLE VOCABULARY_IMAGES (
   VOCABULARY_IMAGES_ID        BIGSERIAL               PRIMARY KEY,
   VOCABULARY_ID               BIGINT                  NOT NULL REFERENCES VOCABULARY(VOCABULARY_ID) ON DELETE CASCADE,
   IMAGE_URL                   VARCHAR(500)            NOT NULL,
   IMAGE_FILE_NAME             VARCHAR(255),
   IMAGE_DESCRIPTION           TEXT,
   IMAGE_ORDER                 INTEGER                 DEFAULT 0,
   IS_PRIMARY                  BOOLEAN                 DEFAULT FALSE,
   CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
   CREATED_ID                  VARCHAR(50)             NOT NULL
);

-- 테이블 코멘트
COMMENT ON TABLE VOCABULARY_IMAGES                          IS '단어 이미지 관리 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN VOCABULARY_IMAGES.VOCABULARY_IMAGES_ID    IS '이미지 ID';
COMMENT ON COLUMN VOCABULARY_IMAGES.VOCABULARY_ID           IS '단어 ID(외래키)';
COMMENT ON COLUMN VOCABULARY_IMAGES.IMAGE_URL               IS '이미지 URL';
COMMENT ON COLUMN VOCABULARY_IMAGES.IMAGE_FILE_NAME         IS '이미지 파일명';
COMMENT ON COLUMN VOCABULARY_IMAGES.IMAGE_DESCRIPTION       IS '이미지 설명';
COMMENT ON COLUMN VOCABULARY_IMAGES.IMAGE_ORDER             IS '이미지 표준 순서';
COMMENT ON COLUMN VOCABULARY_IMAGES.IS_PRIMARY              IS '대표 이미지 여부';
COMMENT ON COLUMN VOCABULARY_IMAGES.CREATED_AT              IS '생성 일시';
COMMENT ON COLUMN VOCABULARY_IMAGES.CREATED_ID              IS '생성자 ID';

-- 인덱스 생성
CREATE INDEX IDX_VOCABULARY_IMAGES_VOCAB                    ON VOCABULARY_IMAGES(VOCABULARY_ID);
CREATE INDEX IDX_VOCABULARY_IMAGES_PRIMARY                  ON VOCABULARY_IMAGES(VOCABULARY_ID, IS_PRIMARY);
```

6. GRAMMAR_RULES 테이블
```postgresql
-- 문법 규칙 테이블
CREATE TABLE GRAMMAR_RULES (
   GRAMMAR_RULES_ID            BIGSERIAL               PRIMARY KEY,
   TITLE                       VARCHAR(200)            NOT NULL,
   RULE_DESCRIPTION            TEXT                    NOT NULL,
   EXAMPLES                    TEXT,
   LESSONS_ID                  BIGINT                  REFERENCES LESSONS(LESSONS_ID) ON DELETE CASCADE,
   CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
   CREATED_ID                  VARCHAR(50)             NOT NULL,
   UPDATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
   UPDATED_ID                  VARCHAR(50)
);

-- 테이블 코멘트
COMMENT ON TABLE GRAMMAR_RULES IS '영어 문법 규칙 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN GRAMMAR_RULES.GRAMMAR_RULES_ID        IS '문법 규칙 고유 ID';
COMMENT ON COLUMN GRAMMAR_RULES.TITLE                   IS '문법 규칙 제목';
COMMENT ON COLUMN GRAMMAR_RULES.RULE_DESCRIPTION        IS '문법 규칙 설명';
COMMENT ON COLUMN GRAMMAR_RULES.EXAMPLES                IS '예시 문장들';
COMMENT ON COLUMN GRAMMAR_RULES.LESSONS_ID              IS '연관된 레슨 ID(외래키)';
COMMENT ON COLUMN GRAMMAR_RULES.CREATED_AT              IS '생성 일시';
COMMENT ON COLUMN GRAMMAR_RULES.CREATED_ID              IS '생성자 ID';
COMMENT ON COLUMN GRAMMAR_RULES.UPDATED_AT              IS '수정 일시';
COMMENT ON COLUMN GRAMMAR_RULES.UPDATED_ID              IS '수정자 ID';

-- 인덱스 생성
CREATE INDEX IDX_GRAMMAR_RULES_LESSONS ON GRAMMAR_RULES(LESSONS_ID);
```

7. QUIZZES 테이블
```postgresql
-- 퀴즈 테이블
CREATE TABLE QUIZZES (
     QUIZZES_ID                  BIGSERIAL               PRIMARY KEY,
     LESSONS_ID                  BIGINT                  REFERENCES LESSONS(LESSONS_ID) ON DELETE CASCADE,
     QUESTION                    TEXT                    NOT NULL,
     QUESTION_TYPE               VARCHAR(50)             CHECK (QUESTION_TYPE IN ('MULTIPLE_CHOICE', 'TRUE_FALSE', 'FILL_BLANK', 'SHORT_ANSWER')),
     CORRECT_ANSWER              TEXT                    NOT NULL,
     OPTIONS                     JSONB,
     EXPLANATION                 TEXT,
     POINTS                      INTEGER                 DEFAULT 10,
     CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
     CREATED_ID                  VARCHAR(50)             NOT NULL,
     UPDATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
     UPDATED_ID                  VARCHAR(50)
);

-- 테이블 코멘트
COMMENT ON TABLE QUIZZES                    IS '퀴즈 문제 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN QUIZZES.QUIZZES_ID        IS '퀴즈 고유 ID';
COMMENT ON COLUMN QUIZZES.LESSONS_ID        IS '연관된 레슨 ID(외래키)';
COMMENT ON COLUMN QUIZZES.QUESTION          IS '문제 내용';
COMMENT ON COLUMN QUIZZES.QUESTION_TYPE     IS '문제 유형 (MULTIPLE_CHOICE: 객관식, TRUE_FALSE: O/X, FILL_BLANK: 빈칸, SHORT_ANSWER: 주관식)';
COMMENT ON COLUMN QUIZZES.CORRECT_ANSWER    IS '정답';
COMMENT ON COLUMN QUIZZES.OPTIONS           IS '선택지 (JSON 형식)';
COMMENT ON COLUMN QUIZZES.EXPLANATION       IS '정답 해설';
COMMENT ON COLUMN QUIZZES.POINTS            IS '획득 점수';
COMMENT ON COLUMN QUIZZES.CREATED_AT        IS '생성 일시';
COMMENT ON COLUMN QUIZZES.CREATED_ID        IS '생성자 ID';
COMMENT ON COLUMN QUIZZES.UPDATED_AT        IS '수정 일시';
COMMENT ON COLUMN QUIZZES.UPDATED_ID        IS '수정자 ID';

-- 인덱스 생성
CREATE INDEX IDX_QUIZZES_LESSONS            ON QUIZZES(LESSONS_ID);
CREATE INDEX IDX_QUIZZES_TYPE               ON QUIZZES(QUESTION_TYPE);
```

8. USER_PROGRESS 테이블
```postgresql
-- 사용자 학습 진도 테이블
CREATE TABLE USER_PROGRESS (
   USER_PROGRESS_ID            BIGSERIAL               PRIMARY KEY,
   USERS_ID                    BIGINT                  REFERENCES USERS(USERS_ID) ON DELETE CASCADE,
   LESSONS_ID                  BIGINT                  REFERENCES LESSONS(LESSONS_ID) ON DELETE CASCADE,
   COMPLETION_PERCENTAGE       INTEGER                 DEFAULT 0 CHECK (COMPLETION_PERCENTAGE >= 0 AND COMPLETION_PERCENTAGE <= 100),
   LAST_ACCESSED               TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
   COMPLETED_AT                TIMESTAMP,
   TOTAL_STUDY_TIME_MINUTES    INTEGER                 DEFAULT 0,
   CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
   UPDATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
   UNIQUE(USERS_ID, LESSONS_ID)
);

-- 테이블 코멘트
COMMENT ON TABLE USER_PROGRESS IS '사용자별 레슨 학습 진도 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN USER_PROGRESS.USER_PROGRESS_ID            IS '진도 기록 ID';
COMMENT ON COLUMN USER_PROGRESS.USERS_ID                    IS '사용자 ID(외래키)';
COMMENT ON COLUMN USER_PROGRESS.LESSONS_ID                  IS '레슨 ID(외래키)';
COMMENT ON COLUMN USER_PROGRESS.COMPLETION_PERCENTAGE       IS '완료율(0-100%)';
COMMENT ON COLUMN USER_PROGRESS.LAST_ACCESSED               IS '마지막 학습 일시';
COMMENT ON COLUMN USER_PROGRESS.COMPLETED_AT                IS '완료 일시(100% 달성)';
COMMENT ON COLUMN USER_PROGRESS.TOTAL_STUDY_TIME_MINUTES    IS '총 학습 시간(분)';
COMMENT ON COLUMN USER_PROGRESS.CREATED_AT                  IS '생성 일시';
COMMENT ON COLUMN USER_PROGRESS.UPDATED_AT                  IS '수정 일시';

-- 인덱스 생성
CREATE INDEX IDX_USER_PROGRESS_USERS                        ON USER_PROGRESS(USERS_ID);
CREATE INDEX IDX_USER_PROGRESS_LESSONS                      ON USER_PROGRESS(LESSONS_ID);
CREATE INDEX IDX_USER_PROGRESS_USERS_LESSONS                ON USER_PROGRESS(USERS_ID, LESSONS_ID);
```

9. QUIZ_RESULTS 테이블
```postgresql
-- 퀴즈 결과 테이블
CREATE TABLE QUIZ_RESULTS (
      QUIZ_RESULTS_ID             BIGSERIAL               PRIMARY KEY,
      USERS_ID                    BIGINT                  REFERENCES USERS(USERS_ID) ON DELETE CASCADE,
      QUIZZES_ID                  BIGINT                  REFERENCES QUIZZES(QUIZZES_ID) ON DELETE CASCADE,
      USER_ANSWER                 TEXT,
      IS_CORRECT                  BOOLEAN,
      POINTS_EARNED               INTEGER                 DEFAULT 0,
      ATTEMPTED_AT                TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

-- 테이블 코멘트
COMMENT ON TABLE QUIZ_RESULTS                   IS '사용자 퀴즈 결과 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN QUIZ_RESULTS.QUIZ_RESULTS_ID  IS '결과 기록 ID';
COMMENT ON COLUMN QUIZ_RESULTS.USERS_ID         IS '사용자 ID(외래키)';
COMMENT ON COLUMN QUIZ_RESULTS.QUIZZES_ID       IS '퀴즈 ID(외래키)';
COMMENT ON COLUMN QUIZ_RESULTS.USER_ANSWER      IS '사용자 답변';
COMMENT ON COLUMN QUIZ_RESULTS.IS_CORRECT       IS '정답 여부';
COMMENT ON COLUMN QUIZ_RESULTS.POINTS_EARNED    IS '획득 점수';
COMMENT ON COLUMN QUIZ_RESULTS.ATTEMPTED_AT     IS '시도 일시';

-- 인덱스 생성
CREATE INDEX IDX_QUIZ_RESULTS_USERS             ON QUIZ_RESULTS(USERS_ID);
CREATE INDEX IDX_QUIZ_RESULTS_QUIZZES           ON QUIZ_RESULTS(QUIZZES_ID);
CREATE INDEX IDX_QUIZ_RESULTS_USERS_QUIZZES     ON QUIZ_RESULTS(USERS_ID, QUIZZES_ID);
```

10. USER_VOCABULARY 테이블
```postgresql
-- 사용자 단어장 테이블
CREATE TABLE USER_VOCABULARY (
    USER_VOCABULARY_ID          BIGSERIAL               PRIMARY KEY,
    USERS_ID                    BIGINT                  REFERENCES USERS(USERS_ID) ON DELETE CASCADE,
    VOCABULARY_ID               BIGINT                  REFERENCES VOCABULARY(VOCABULARY_ID) ON DELETE CASCADE,
    MASTERY_LEVEL               INTEGER                 DEFAULT 0 CHECK (MASTERY_LEVEL >= 0 AND MASTERY_LEVEL <= 5),
    LAST_REVIEWED               TIMESTAMP,
    REVIEW_COUNT                INTEGER                 DEFAULT 0,
    ADDED_AT                    TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(USERS_ID, VOCABULARY_ID)
);

-- 테이블 코멘트
COMMENT ON TABLE USER_VOCABULARY IS '사용자별 단어 학습 상태 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN USER_VOCABULARY.USER_VOCABULARY_ID        IS '기록 ID';
COMMENT ON COLUMN USER_VOCABULARY.USERS_ID                  IS '사용자 ID(외래키)';
COMMENT ON COLUMN USER_VOCABULARY.VOCABULARY_ID             IS '단어 ID(외래키)';
COMMENT ON COLUMN USER_VOCABULARY.MASTERY_LEVEL             IS '숙달도(0: 모름, 5: 완벽히 알고 있음)';
COMMENT ON COLUMN USER_VOCABULARY.LAST_REVIEWED             IS '마지막 복습 일시';
COMMENT ON COLUMN USER_VOCABULARY.REVIEW_COUNT              IS '복습 횟수';
COMMENT ON COLUMN USER_VOCABULARY.ADDED_AT                  IS '단어장 추가 일시';

-- 인덱스 생성
CREATE INDEX IDX_USER_VOCABULARY_USERS                      ON USER_VOCABULARY(USERS_ID);
CREATE INDEX IDX_USER_VOCABULARY_VOCAB                      ON USER_VOCABULARY(VOCABULARY_ID);
```

11. USER_STATISTICS
```postgresql
-- 사용자 통계 테이블
CREATE TABLE USER_STATISTICS (
     USER_STATISTICS_ID          BIGSERIAL               PRIMARY KEY,
     USERS_ID                    BIGINT                  UNIQUE REFERENCES USERS(USERS_ID) ON DELETE CASCADE,
     TOTAL_LESSONS_COMPLETED     INTEGER                 DEFAULT 0,
     TOTAL_QUIZZES_TAKEN         INTEGER                 DEFAULT 0,
     TOTAL_CORRECT_ANSWERS       INTEGER                 DEFAULT 0,
     TOTAL_STUDY_TIME_MINUTES    INTEGER                 DEFAULT 0,
     CURRENT_STREAK_DAYS         INTEGER                 DEFAULT 0,
     LONGEST_STREAK_DAYS         INTEGER                 DEFAULT 0,
     LAST_STUDY_DATE             DATE,
     CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
     UPDATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

-- 테이블 코멘트
COMMENT ON TABLE USER_STATISTICS IS '사용자 학습 통계 테이블';

-- 컬럼 코멘트
COMMENT ON COLUMN USER_STATISTICS.USER_STATISTICS_ID        IS '통계 ID';
COMMENT ON COLUMN USER_STATISTICS.USERS_ID                  IS '사용자 ID(외래키)';
COMMENT ON COLUMN USER_STATISTICS.TOTAL_LESSONS_COMPLETED   IS '완료한 총 레슨 수';
COMMENT ON COLUMN USER_STATISTICS.TOTAL_QUIZZES_TAKEN       IS '푼 총 퀴즈 수';
COMMENT ON COLUMN USER_STATISTICS.TOTAL_CORRECT_ANSWERS     IS '정답 총 개수';
COMMENT ON COLUMN USER_STATISTICS.TOTAL_STUDY_TIME_MINUTES  IS '총 학습 시간(분)';
COMMENT ON COLUMN USER_STATISTICS.CURRENT_STREAK_DAYS       IS '현재 연속 학습 일수';
COMMENT ON COLUMN USER_STATISTICS.LONGEST_STREAK_DAYS       IS '최장 연속 학습 일수';
COMMENT ON COLUMN USER_STATISTICS.CREATED_AT                IS '생성 일시';
COMMENT ON COLUMN USER_STATISTICS.UPDATED_AT                IS '수정 일시';

-- 인덱스 생성
CREATE INDEX IDX_USER_STATISTICS_USERS ON USER_STATISTICS(USERS_ID);
```

12. 트리거 함수
```postgresql
-- 업데이트 시간 자동 갱신 함수
CREATE OR REPLACE FUNCTION UPDATE_UPDATED_AT_COLUMN()
RETURNS TRIGGER AS $$
BEGIN
    NEW.UPDATED_AT = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 적용
CREATE TRIGGER TRG_UPDATE_USERS_UPDATED_AT 
    BEFORE UPDATE ON USERS
    FOR EACH ROW EXECUTE FUNCTION UPDATE_UPDATED_AT_COLUMN();

CREATE TRIGGER TRG_UPDATE_LESSONS_UPDATED_AT 
    BEFORE UPDATE ON LESSONS
    FOR EACH ROW EXECUTE FUNCTION UPDATE_UPDATED_AT_COLUMN();

CREATE TRIGGER TRG_UPDATE_VOCABULARY_UPDATED_AT 
    BEFORE UPDATE ON VOCABULARY
    FOR EACH ROW EXECUTE FUNCTION UPDATE_UPDATED_AT_COLUMN();

CREATE TRIGGER TRG_UPDATE_GRAMMAR_RULES_UPDATED_AT 
    BEFORE UPDATE ON GRAMMAR_RULES
    FOR EACH ROW EXECUTE FUNCTION UPDATE_UPDATED_AT_COLUMN();

CREATE TRIGGER TRG_UPDATE_QUIZZES_UPDATED_AT 
    BEFORE UPDATE ON QUIZZES
    FOR EACH ROW EXECUTE FUNCTION UPDATE_UPDATED_AT_COLUMN();

CREATE TRIGGER TRG_UPDATE_COMMON_CODE_UPDATED_AT 
    BEFORE UPDATE ON COMMON_CODE
    FOR EACH ROW EXECUTE FUNCTION UPDATE_UPDATED_AT_COLUMN();

CREATE TRIGGER TRG_UPDATE_USER_PROGRESS_UPDATED_AT 
    BEFORE UPDATE ON USER_PROGRESS
    FOR EACH ROW EXECUTE FUNCTION UPDATE_UPDATED_AT_COLUMN();

CREATE TRIGGER TRG_UPDATE_USER_STATISTICS_UPDATED_AT 
    BEFORE UPDATE ON USER_STATISTICS
    FOR EACH ROW EXECUTE FUNCTION UPDATE_UPDATED_AT_COLUMN();

-- 사용자 통계 자동 생성 트리거
CREATE OR REPLACE FUNCTION CREATE_USER_STATISTICS()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO USER_STATISTICS (USERS_ID)
    VALUES (NEW.USERS_ID);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER TRG_CREATE_USER_STATISTICS
    AFTER INSERT ON USERS
    FOR EACH ROW EXECUTE FUNCTION CREATE_USER_STATISTICS();
```