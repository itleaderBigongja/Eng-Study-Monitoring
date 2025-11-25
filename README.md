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
- 보안
- 데이터베이스( 영어학습 )
- 데이터베이스( 모니터링 )
- 모니터링 대시보드 기능


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
![시스템 전체 구조](capture-images/system_achitecture.png)

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


<h1>데이터베이스( 영어학습 )</h1>

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

<h1>데이터베이스( 모니터링 )</h1>

## <font color = "Yellow">모니터링 테이블 구조</font>
### <font color="Aquamarine">1. MONITORING_PROCESS( 프로세스 현황 테이블 )</font>
<p>-> 실행 중인 모든 프로세스의 상태 및 리소스 사용량 추적</p>
<p>-> CPU/메모리 사용률, 가동 시간, 헬스체크 정보</p>

```postgresql
-- 프로세스 현황 테이블
CREATE TABLE MONITORING_PROCESS (
    PROCESS_ID                  BIGSERIAL               PRIMARY KEY,
    PROCESS_NAME                VARCHAR(100)            NOT NULL,
    PROCESS_TYPE                VARCHAR(50)             NOT NULL,  -- BACKEND, FRONTEND, DATABASE, etc
    HOST_NAME                   VARCHAR(100)            NOT NULL,
    IP_ADDRESS                  VARCHAR(50)             NOT NULL,
    PORT                        INTEGER,
    PID                         INTEGER,                -- Process ID
    STATUS                      VARCHAR(20)             NOT NULL CHECK (STATUS IN ('RUNNING', 'STOPPED', 'ERROR', 'STARTING', 'STOPPING')),
    CPU_USAGE                   DECIMAL(5,2),          -- CPU 사용률 (%)
    MEMORY_USAGE                DECIMAL(5,2),          -- 메모리 사용률 (%)
    UPTIME_SECONDS              BIGINT                  DEFAULT 0,
    LAST_HEALTH_CHECK           TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    ERROR_MESSAGE               TEXT,
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE MONITORING_PROCESS                     IS '프로세스 현황 모니터링 테이블';
COMMENT ON COLUMN MONITORING_PROCESS.PROCESS_ID         IS '프로세스 고유 ID';
COMMENT ON COLUMN MONITORING_PROCESS.PROCESS_NAME       IS '프로세스명 (eng-study-backend, nginx 등)';
COMMENT ON COLUMN MONITORING_PROCESS.PROCESS_TYPE       IS '프로세스 유형';
COMMENT ON COLUMN MONITORING_PROCESS.HOST_NAME          IS '호스트명';
COMMENT ON COLUMN MONITORING_PROCESS.IP_ADDRESS         IS 'IP 주소';
COMMENT ON COLUMN MONITORING_PROCESS.PORT               IS '포트번호';
COMMENT ON COLUMN MONITORING_PROCESS.PID                IS '프로세스 ID';
COMMENT ON COLUMN MONITORING_PROCESS.STATUS             IS '프로세스 상태';
COMMENT ON COLUMN MONITORING_PROCESS.CPU_USAGE          IS 'CPU 사용률(%)';
COMMENT ON COLUMN MONITORING_PROCESS.MEMORY_USAGE       IS '메모리 사용률(%)';
COMMENT ON COLUMN MONITORING_PROCESS.UPTIME_SECONDS     IS '가동 시간(초)';
COMMENT ON COLUMN MONITORING_PROCESS.LAST_HEALTH_CHECK  IS '마지막 헬스체크 시간';
COMMENT ON COLUMN MONITORING_PROCESS.ERROR_MESSAGE      IS '에러 메시지';

-- 인덱스 생성
CREATE INDEX IDX_MONITORING_PROCESS_STATUS              ON MONITORING_PROCESS(STATUS);
CREATE INDEX IDX_MONITORING_PROCESS_TYPE                ON MONITORING_PROCESS(PROCESS_TYPE);
CREATE INDEX IDX_MONITORING_PROCESS_NAME                ON MONITORING_PROCESS(PROCESS_NAME);
```

### <font color="Aquamarine">2.MONITORING_REALTIME ( 실시간 감시 현황 테이블 )</font>
<p>-> TPS, 응답시간, 에러 카운트 등 실시간 메트릭</p>
<p>-> 임계값, 기반 알람 기능</p>

```postgresql
-- 실시간 감시 현황 테이블
CREATE TABLE MONITORING_REALTIME (
    REALTIME_ID                 BIGSERIAL               PRIMARY KEY,
    PROCESS_ID                  BIGINT                  REFERENCES MONITORING_PROCESS(PROCESS_ID) ON DELETE CASCADE,
    METRIC_TYPE                 VARCHAR(50)             NOT NULL,  -- TPS, ERROR_COUNT, RESPONSE_TIME, etc
    METRIC_VALUE                DECIMAL(15,4)           NOT NULL,
    METRIC_UNIT                 VARCHAR(20),           -- req/s, ms, count, etc
    THRESHOLD_MIN               DECIMAL(15,4),
    THRESHOLD_MAX               DECIMAL(15,4),
    IS_ALERT                    BOOLEAN                 DEFAULT FALSE,
    ALERT_MESSAGE               TEXT,
    COLLECTED_AT                TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE MONITORING_REALTIME                    IS '실시간 감시 현황 테이블';
COMMENT ON COLUMN MONITORING_REALTIME.REALTIME_ID       IS '실시간 감시 ID';
COMMENT ON COLUMN MONITORING_REALTIME.PROCESS_ID        IS '프로세스 ID(외래키)';
COMMENT ON COLUMN MONITORING_REALTIME.METRIC_TYPE       IS '메트릭 유형(TPS, ERROR_COUNT 등)';
COMMENT ON COLUMN MONITORING_REALTIME.METRIC_VALUE      IS '메트릭 값';
COMMENT ON COLUMN MONITORING_REALTIME.METRIC_UNIT       IS '메트릭 단위';
COMMENT ON COLUMN MONITORING_REALTIME.THRESHOLD_MIN     IS '임계치 최소';
COMMENT ON COLUMN MONITORING_REALTIME.THRESHOLD_MAX     IS '임계치 최대';
COMMENT ON COLUMN MONITORING_REALTIME.IS_ALERT          IS '알람 발생 여부';
COMMENT ON COLUMN MONITORING_REALTIME.ALERT_MESSAGE     IS '알람 메시지';
COMMENT ON COLUMN MONITORING_REALTIME.COLLECTED_AT      IS '수집 시간';

-- 인덱스 생성
CREATE INDEX IDX_MONITORING_REALTIME_PROCESS            ON MONITORING_REALTIME(PROCESS_ID);
CREATE INDEX IDX_MONITORING_REALTIME_TYPE               ON MONITORING_REALTIME(METRIC_TYPE);
CREATE INDEX IDX_MONITORING_REALTIME_COLLECTED          ON MONITORING_REALTIME(COLLECTED_AT);
CREATE INDEX IDX_MONITORING_REALTIME_ALERT              ON MONITORING_REALTIME(IS_ALERT);
```

### <font color="Aquamarine">3. MONITORING_HEAP_MEMORY( Heap 메모리 사용 현황 테이블 )</font>
<p>-> JVM Heap 메모리 사용량 모니터링</p>
<p>-> GC 발생 횟수 및 소요 시간 추적</p>
<p>-> 클러스터/노드별 메모리 현황</p>

```postgresql
-- Heap 메모리 사용 현황 테이블
CREATE TABLE MONITORING_HEAP_MEMORY (
    HEAP_MEMORY_ID              BIGSERIAL               PRIMARY KEY,
    PROCESS_ID                  BIGINT                  REFERENCES MONITORING_PROCESS(PROCESS_ID) ON DELETE CASCADE,
    CLUSTER_NAME                VARCHAR(100),
    NODE_NAME                   VARCHAR(100),
    HEAP_USED_MB                DECIMAL(15,2)           NOT NULL,
    HEAP_MAX_MB                 DECIMAL(15,2)           NOT NULL,
    HEAP_USAGE_PERCENT          DECIMAL(5,2)            NOT NULL,
    NON_HEAP_USED_MB            DECIMAL(15,2),
    NON_HEAP_MAX_MB             DECIMAL(15,2),
    GC_COUNT                    INTEGER                 DEFAULT 0,
    GC_TIME_MS                  BIGINT                  DEFAULT 0,
    IS_WARNING                  BOOLEAN                 DEFAULT FALSE,  -- 80% 이상 경고
    IS_CRITICAL                 BOOLEAN                 DEFAULT FALSE,  -- 90% 이상 심각
    COLLECTED_AT                TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE MONITORING_HEAP_MEMORY                         IS 'Heap 메모리 사용 현황 테이블';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.HEAP_MEMORY_ID         IS 'Heap 메모리 ID';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.PROCESS_ID             IS '프로세스 ID(외래키)';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.CLUSTER_NAME           IS '클러스터명';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.NODE_NAME              IS '노드명';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.HEAP_USED_MB           IS '사용 중인 Heap 메모리(MB)';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.HEAP_MAX_MB            IS '최대 Heap 메모리(MB)';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.HEAP_USAGE_PERCENT     IS 'Heap 사용률(%)';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.NON_HEAP_USED_MB       IS 'Non-Heap 사용 메모리(MB)';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.NON_HEAP_MAX_MB        IS 'Non-Heap 최대 메모리(MB)';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.GC_COUNT               IS 'GC 발생 횟수';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.GC_TIME_MS             IS 'GC 소요 시간(ms)';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.IS_WARNING             IS '경고 상태(80% 이상)';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.IS_CRITICAL            IS '심각 상태(90% 이상)';
COMMENT ON COLUMN MONITORING_HEAP_MEMORY.COLLECTED_AT           IS '수집 시간';

-- 인덱스 생성
CREATE INDEX IDX_MONITORING_HEAP_PROCESS                        ON MONITORING_HEAP_MEMORY(PROCESS_ID);
CREATE INDEX IDX_MONITORING_HEAP_CLUSTER                        ON MONITORING_HEAP_MEMORY(CLUSTER_NAME);
CREATE INDEX IDX_MONITORING_HEAP_COLLECTED                      ON MONITORING_HEAP_MEMORY(COLLECTED_AT);
CREATE INDEX IDX_MONITORING_HEAP_WARNING                        ON MONITORING_HEAP_MEMORY(IS_WARNING);
```

### <font color="Aquamarine">4. MONITORING_EVENT( 주요 모니터링 이벤트 테이블 )</font>
<p>-> Eviction Mode, SUB LOG, Error, DB Log 등 이벤트 추적</p>
<p>-> 이벤트 유형별 카운팅 및 상세 정보 저장</p>

```postgresql
-- 주요 모니터링 이벤트 테이블
CREATE TABLE MONITORING_EVENT (
    EVENT_ID                    BIGSERIAL               PRIMARY KEY,
    PROCESS_ID                  BIGINT                  REFERENCES MONITORING_PROCESS(PROCESS_ID) ON DELETE CASCADE,
    EVENT_TYPE                  VARCHAR(50)             NOT NULL CHECK (EVENT_TYPE IN ('EVICTION_MODE', 'SUB_LOG', 'ERROR', 'DB_LOG', 'WARNING', 'INFO')),
    EVENT_LEVEL                 VARCHAR(20)             NOT NULL CHECK (EVENT_LEVEL IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    EVENT_MESSAGE               TEXT                    NOT NULL,
    EVENT_DETAIL                JSONB,
    SOURCE_FILE                 VARCHAR(255),
    SOURCE_LINE                 INTEGER,
    STACK_TRACE                 TEXT,
    IS_RESOLVED                 BOOLEAN                 DEFAULT FALSE,
    RESOLVED_AT                 TIMESTAMP,
    RESOLVED_BY                 VARCHAR(50),
    OCCURRED_AT                 TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE MONITORING_EVENT                       IS '주요 모니터링 이벤트 테이블';
COMMENT ON COLUMN MONITORING_EVENT.EVENT_ID             IS '이벤트 ID';
COMMENT ON COLUMN MONITORING_EVENT.PROCESS_ID           IS '프로세스 ID(외래키)';
COMMENT ON COLUMN MONITORING_EVENT.EVENT_TYPE           IS '이벤트 유형';
COMMENT ON COLUMN MONITORING_EVENT.EVENT_LEVEL          IS '이벤트 심각도';
COMMENT ON COLUMN MONITORING_EVENT.EVENT_MESSAGE        IS '이벤트 메시지';
COMMENT ON COLUMN MONITORING_EVENT.EVENT_DETAIL         IS '이벤트 상세 정보( JSON )';
COMMENT ON COLUMN MONITORING_EVENT.SOURCE_FILE          IS '소스 파일명';
COMMENT ON COLUMN MONITORING_EVENT.SOURCE_LINE          IS '소스 라인 번호';
COMMENT ON COLUMN MONITORING_EVENT.STACK_TRACE          IS '스택 트레이스';
COMMENT ON COLUMN MONITORING_EVENT.IS_RESOLVED          IS '해결 여부';
COMMENT ON COLUMN MONITORING_EVENT.RESOLVED_AT          IS '해결 시간';
COMMENT ON COLUMN MONITORING_EVENT.RESOLVED_BY          IS '해결자';
COMMENT ON COLUMN MONITORING_EVENT.OCCURRED_AT          IS '발생 시간';

-- 인덱스
CREATE INDEX IDX_MONITORING_EVENT_PROCESS               ON MONITORING_EVENT(PROCESS_ID);
CREATE INDEX IDX_MONITORING_EVENT_TYPE                  ON MONITORING_EVENT(EVENT_TYPE);
CREATE INDEX IDX_MONITORING_EVENT_LEVEL                 ON MONITORING_EVENT(EVENT_LEVEL);
CREATE INDEX IDX_MONITORING_EVENT_OCCURRED              ON MONITORING_EVENT(OCCURRED_AT);
CREATE INDEX IDX_MONITORING_EVENT_RESOLVED              ON MONITORING_EVENT(IS_RESOLVED);
```

### <font color="Aquamarine">5. MONITORING_TPS( TPS 실시간 감시 테이블)</font>
<p>-> 초당 트랜잭션 수(Transaction PerSecond</p>
<p>-> 요청/성공/실패 카운트</p>
<p>-> 평균/최소/최대 응답 시간</p>

```postgresql
-- TPS 실시간 감시 테이블
CREATE TABLE MONITORING_TPS (
    TPS_ID                      BIGSERIAL               PRIMARY KEY,
    PROCESS_ID                  BIGINT                  REFERENCES MONITORING_PROCESS(PROCESS_ID) ON DELETE CASCADE,
    TPS_VALUE                   DECIMAL(15,2)           NOT NULL,  -- Transaction Per Second
    REQUEST_COUNT               BIGINT                  DEFAULT 0,
    SUCCESS_COUNT               BIGINT                  DEFAULT 0,
    ERROR_COUNT                 BIGINT                  DEFAULT 0,
    AVG_RESPONSE_TIME_MS        DECIMAL(10,2),
    MIN_RESPONSE_TIME_MS        DECIMAL(10,2),
    MAX_RESPONSE_TIME_MS        DECIMAL(10,2),
    PEAK_TPS                    DECIMAL(15,2),
    IS_PEAK                     BOOLEAN                 DEFAULT FALSE,
    COLLECTED_AT                TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE MONITORING_TPS                         IS 'TPS 실시간 감시 테이블';
COMMENT ON COLUMN MONITORING_TPS.TPS_ID                 IS 'TPS_ID';
COMMENT ON COLUMN MONITORING_TPS.PROCESS_ID             IS '프로세스 ID(외래키)';
COMMENT ON COLUMN MONITORING_TPS.TPS_VALUE              IS 'TPS 값(초당 트랜잭션 수)';
COMMENT ON COLUMN MONITORING_TPS.REQUEST_COUNT          IS '요청 수';
COMMENT ON COLUMN MONITORING_TPS.SUCCESS_COUNT          IS '성공 수';
COMMENT ON COLUMN MONITORING_TPS.ERROR_COUNT            IS '에러 수';
COMMENT ON COLUMN MONITORING_TPS.AVG_RESPONSE_TIME_MS   IS '평균 응답 시간(ms)';
COMMENT ON COLUMN MONITORING_TPS.MIN_RESPONSE_TIME_MS   IS '최소 응답 시간(ms)';
COMMENT ON COLUMN MONITORING_TPS.MAX_RESPONSE_TIME_MS   IS '최대 응답 시간(ms)';
COMMENT ON COLUMN MONITORING_TPS.PEAK_TPS               IS '피크 TPS';
COMMENT ON COLUMN MONITORING_TPS.IS_PEAK                IS '피크 여부';
COMMENT ON COLUMN MONITORING_TPS.COLLECTED_AT           IS '수집 시간';

-- 인덱스 생성
CREATE INDEX IDX_MONITORING_TPS_PROCESS                 ON MONITORING_TPS(PROCESS_ID);
CREATE INDEX IDX_MONITORING_TPS_COLLECTED               ON MONITORING_TPS(COLLECTED_AT);
CREATE INDEX IDX_MONITORING_TPS_PEAK                    ON MONITORING_TPS(IS_PEAK);
```

### <font color="Aquamarine">6. MONITORING_ERROR(Error Count 실시간 감시 테이블 )</font>
<p>-> 에러 유형별 발생 횟수 추적</p>
<p>-> HTTP 상태 코드, 요청 URL, 사용자 정보</p>
<p>-> 스택 트레이스 및 심각도 분류</p>

```postgresql
-- Error Count 실시간 감시 테이블
CREATE TABLE MONITORING_ERROR (
    ERROR_ID                    BIGSERIAL               PRIMARY KEY,
    PROCESS_ID                  BIGINT                  REFERENCES MONITORING_PROCESS(PROCESS_ID) ON DELETE CASCADE,
    ERROR_TYPE                  VARCHAR(100)            NOT NULL,  -- NullPointerException, IOException 등
    ERROR_COUNT                 INTEGER                 NOT NULL DEFAULT 1,
    ERROR_MESSAGE               TEXT,
    ERROR_CODE                  VARCHAR(50),
    HTTP_STATUS                 INTEGER,
    REQUEST_URL                 VARCHAR(500),
    REQUEST_METHOD              VARCHAR(10),
    USER_ID                     BIGINT,
    IP_ADDRESS                  VARCHAR(50),
    USER_AGENT                  TEXT,
    STACK_TRACE                 TEXT,
    IS_CRITICAL                 BOOLEAN                 DEFAULT FALSE,
    FIRST_OCCURRED_AT           TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    LAST_OCCURRED_AT            TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE MONITORING_ERROR                       IS 'Error 실시간 감시 테이블';
COMMENT ON COLUMN MONITORING_ERROR.ERROR_ID             IS 'Error ID';
COMMENT ON COLUMN MONITORING_ERROR.PROCESS_ID           IS '프로세스 ID(외래키)';
COMMENT ON COLUMN MONITORING_ERROR.ERROR_TYPE           IS '에러 유형';
COMMENT ON COLUMN MONITORING_ERROR.ERROR_COUNT          IS '에러 발생 횟수';
COMMENT ON COLUMN MONITORING_ERROR.ERROR_MESSAGE        IS '에러 메시지';
COMMENT ON COLUMN MONITORING_ERROR.ERROR_CODE           IS '에러 코드';
COMMENT ON COLUMN MONITORING_ERROR.HTTP_STATUS          IS 'HTTP 상태 코드';
COMMENT ON COLUMN MONITORING_ERROR.REQUEST_URL          IS '요청 URL';
COMMENT ON COLUMN MONITORING_ERROR.REQUEST_METHOD       IS '요청 메소드';
COMMENT ON COLUMN MONITORING_ERROR.USER_ID              IS '사용자 ID';
COMMENT ON COLUMN MONITORING_ERROR.IP_ADDRESS           IS 'IP 주소';
COMMENT ON COLUMN MONITORING_ERROR.USER_AGENT           IS 'User Agent';
COMMENT ON COLUMN MONITORING_ERROR.STACK_TRACE          IS '스택 트레이스';
COMMENT ON COLUMN MONITORING_ERROR.IS_CRITICAL          IS '심각 여부';
COMMENT ON COLUMN MONITORING_ERROR.FIRST_OCCURRED_AT    IS '최초 발생 시간';
COMMENT ON COLUMN MONITORING_ERROR.LAST_OCCURRED_AT     IS '마지막 발생 시간';

CREATE INDEX IDX_MONITORING_ERROR_PROCESS ON MONITORING_ERROR(PROCESS_ID);
CREATE INDEX IDX_MONITORING_ERROR_TYPE ON MONITORING_ERROR(ERROR_TYPE);
CREATE INDEX IDX_MONITORING_ERROR_CRITICAL ON MONITORING_ERROR(IS_CRITICAL);
CREATE INDEX IDX_MONITORING_ERROR_OCCURRED ON MONITORING_ERROR(LAST_OCCURRED_AT);
```

### <font color = "Aquamarine">7. MONITORING_TCP_PEER (TCP/Peer 실시간 감시)</font>
<p>-> TCP 연결 상태 모니터링</p>
<p>-> 송수신 패킷/바이트 통계</p>
<p>-> 재전송 횟수 및 지연 시간</p>

```postgresql
-- TCP/Peer 실시간 감시 테이블
CREATE TABLE MONITORING_TCP_PEER (
    TCP_PEER_ID                 BIGSERIAL               PRIMARY KEY,
    PROCESS_ID                  BIGINT                  REFERENCES MONITORING_PROCESS(PROCESS_ID) ON DELETE CASCADE,
    SOURCE_IP                   VARCHAR(50)             NOT NULL,
    SOURCE_PORT                 INTEGER                 NOT NULL,
    DESTINATION_IP              VARCHAR(50)             NOT NULL,
    DESTINATION_PORT            INTEGER                 NOT NULL,
    CONNECTION_STATE            VARCHAR(20)             NOT NULL CHECK (CONNECTION_STATE IN ('ESTABLISHED', 'SYN_SENT', 'SYN_RECV', 'FIN_WAIT1', 'FIN_WAIT2', 'TIME_WAIT', 'CLOSE', 'CLOSE_WAIT', 'LAST_ACK', 'LISTEN', 'CLOSING')),
    CONNECTION_COUNT            INTEGER                 DEFAULT 0,
    ACTIVE_CONNECTIONS          INTEGER                 DEFAULT 0,
    BYTES_SENT                  BIGINT                  DEFAULT 0,
    BYTES_RECEIVED              BIGINT                  DEFAULT 0,
    PACKETS_SENT                BIGINT                  DEFAULT 0,
    PACKETS_RECEIVED            BIGINT                  DEFAULT 0,
    RETRANSMISSIONS             INTEGER                 DEFAULT 0,
    LATENCY_MS                  DECIMAL(10,2),
    IS_HEALTHY                  BOOLEAN                 DEFAULT TRUE,
    COLLECTED_AT                TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE MONITORING_TCP_PEER                        IS 'TCP/Peer 실시간 감시 테이블';
COMMENT ON COLUMN MONITORING_TCP_PEER.TCP_PEER_ID           IS 'TCP Peer ID';
COMMENT ON COLUMN MONITORING_TCP_PEER.PROCESS_ID            IS '프로세스 ID (외래키)';
COMMENT ON COLUMN MONITORING_TCP_PEER.SOURCE_IP             IS '소스 IP';
COMMENT ON COLUMN MONITORING_TCP_PEER.SOURCE_PORT           IS '소스 포트';
COMMENT ON COLUMN MONITORING_TCP_PEER.DESTINATION_IP        IS '목적지 IP';
COMMENT ON COLUMN MONITORING_TCP_PEER.DESTINATION_PORT      IS '목적지 포트';
COMMENT ON COLUMN MONITORING_TCP_PEER.CONNECTION_STATE      IS '연결 상태';
COMMENT ON COLUMN MONITORING_TCP_PEER.CONNECTION_COUNT      IS '연결 수';
COMMENT ON COLUMN MONITORING_TCP_PEER.ACTIVE_CONNECTIONS    IS '활성 연결 수';
COMMENT ON COLUMN MONITORING_TCP_PEER.BYTES_SENT            IS '전송 바이트';
COMMENT ON COLUMN MONITORING_TCP_PEER.BYTES_RECEIVED        IS '수신 바이트';
COMMENT ON COLUMN MONITORING_TCP_PEER.PACKETS_SENT          IS '전송 패킷';
COMMENT ON COLUMN MONITORING_TCP_PEER.PACKETS_RECEIVED      IS '수신 패킷';
COMMENT ON COLUMN MONITORING_TCP_PEER.RETRANSMISSIONS       IS '재전송 횟수';
COMMENT ON COLUMN MONITORING_TCP_PEER.LATENCY_MS            IS '지연 시간 (ms)';
COMMENT ON COLUMN MONITORING_TCP_PEER.IS_HEALTHY            IS '정상 여부';
COMMENT ON COLUMN MONITORING_TCP_PEER.COLLECTED_AT          IS '수집 시간';

-- 인덱스 생성
CREATE INDEX IDX_MONITORING_TCP_PROCESS             ON MONITORING_TCP_PEER(PROCESS_ID);
CREATE INDEX IDX_MONITORING_TCP_STATE               ON MONITORING_TCP_PEER(CONNECTION_STATE);
CREATE INDEX IDX_MONITORING_TCP_COLLECTED           ON MONITORING_TCP_PEER(COLLECTED_AT);
CREATE INDEX IDX_MONITORING_TCP_HEALTHY             ON MONITORING_TCP_PEER(IS_HEALTHY);
```

### <font color = "Aquamarine">8. MONITORING_STATISTICS (통계 정보 테이블)</font>
<p>-> 시간 주기별 (분/시간/일/주/월) 통계 데이터</p>
<p>-> 집계 방식 (AVG/SUM/MIN/MAX/COUNT)</p>
<p>-> 조회 시작/종료 날짜 범위 검색 지원</p>

```postgresql
-- 통계 정보 테이블
CREATE TABLE MONITORING_STATISTICS (
    STATISTICS_ID               BIGSERIAL               PRIMARY KEY,
    PROCESS_ID                  BIGINT                  REFERENCES MONITORING_PROCESS(PROCESS_ID) ON DELETE CASCADE,
    METRIC_TYPE                 VARCHAR(50)             NOT NULL,  -- TPS, ERROR_COUNT, HEAP_MEMORY 등
    TIME_PERIOD                 VARCHAR(20)             NOT NULL CHECK (TIME_PERIOD IN ('MINUTE', 'HOUR', 'DAY', 'WEEK', 'MONTH')),
    AGGREGATION_TYPE            VARCHAR(20)             NOT NULL CHECK (AGGREGATION_TYPE IN ('AVG', 'SUM', 'MIN', 'MAX', 'COUNT')),
    START_TIME                  TIMESTAMP               NOT NULL,
    END_TIME                    TIMESTAMP               NOT NULL,
    METRIC_VALUE                DECIMAL(15,4)           NOT NULL,
    SAMPLE_COUNT                INTEGER                 DEFAULT 0,
    MIN_VALUE                   DECIMAL(15,4),
    MAX_VALUE                   DECIMAL(15,4),
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE MONITORING_STATISTICS                      IS '모니터링 통계 정보 테이블';
COMMENT ON COLUMN MONITORING_STATISTICS.STATISTICS_ID       IS '통계 ID';
COMMENT ON COLUMN MONITORING_STATISTICS.PROCESS_ID          IS '프로세스 ID (외래키)';
COMMENT ON COLUMN MONITORING_STATISTICS.METRIC_TYPE         IS '메트릭 유형';
COMMENT ON COLUMN MONITORING_STATISTICS.TIME_PERIOD         IS '시간 주기 (분/시간/일/주/월)';
COMMENT ON COLUMN MONITORING_STATISTICS.AGGREGATION_TYPE    IS '집계 방식 (AVG/SUM/MIN/MAX/COUNT)';
COMMENT ON COLUMN MONITORING_STATISTICS.START_TIME          IS '시작 시간';
COMMENT ON COLUMN MONITORING_STATISTICS.END_TIME            IS '종료 시간';
COMMENT ON COLUMN MONITORING_STATISTICS.METRIC_VALUE        IS '메트릭 값';
COMMENT ON COLUMN MONITORING_STATISTICS.SAMPLE_COUNT        IS '샘플 수';
COMMENT ON COLUMN MONITORING_STATISTICS.MIN_VALUE           IS '최소값';
COMMENT ON COLUMN MONITORING_STATISTICS.MAX_VALUE           IS '최대값';
    
-- 인덱스 생성
CREATE INDEX IDX_MONITORING_STATS_PROCESS ON MONITORING_STATISTICS(PROCESS_ID);
CREATE INDEX IDX_MONITORING_STATS_TYPE ON MONITORING_STATISTICS(METRIC_TYPE);
CREATE INDEX IDX_MONITORING_STATS_PERIOD ON MONITORING_STATISTICS(TIME_PERIOD);
CREATE INDEX IDX_MONITORING_STATS_TIME ON MONITORING_STATISTICS(START_TIME, END_TIME);
```

### <font color = "Aquamarine">9. MONITORING_ALERT (알람 설정 및 이력 테이블)</font>
<p>-> 임계값 기반 알람 설정</p>
<p>-> 이메일/Slack 알림 연동</p>
<p>-> 알람 발생 이력 추적</p>

```postgresql
-- 알람 설정 및 이력 테이블
CREATE TABLE MONITORING_ALERT (
    ALERT_ID                    BIGSERIAL               PRIMARY KEY,
    ALERT_NAME                  VARCHAR(100)            NOT NULL,
    ALERT_TYPE                  VARCHAR(50)             NOT NULL,  -- THRESHOLD, PATTERN, ANOMALY
    METRIC_TYPE                 VARCHAR(50)             NOT NULL,
    CONDITION_OPERATOR          VARCHAR(20)             NOT NULL CHECK (CONDITION_OPERATOR IN ('GT', 'GTE', 'LT', 'LTE', 'EQ', 'NEQ')),
    THRESHOLD_VALUE             DECIMAL(15,4)           NOT NULL,
    SEVERITY                    VARCHAR(20)             NOT NULL CHECK (SEVERITY IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    IS_ACTIVE                   BOOLEAN                 DEFAULT TRUE,
    NOTIFICATION_EMAIL          VARCHAR(255),
    NOTIFICATION_SLACK          VARCHAR(255),
    LAST_TRIGGERED_AT           TIMESTAMP,
    TRIGGER_COUNT               INTEGER                 DEFAULT 0,
    CREATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    CREATED_ID                  VARCHAR(50)             NOT NULL,
    UPDATED_AT                  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    UPDATED_ID                  VARCHAR(50)
);

COMMENT ON TABLE MONITORING_ALERT                       IS '모니터링 알람 설정 테이블';
COMMENT ON COLUMN MONITORING_ALERT.ALERT_ID             IS '알람 ID';
COMMENT ON COLUMN MONITORING_ALERT.ALERT_NAME           IS '알람명';
COMMENT ON COLUMN MONITORING_ALERT.ALERT_TYPE           IS '알람 유형';
COMMENT ON COLUMN MONITORING_ALERT.METRIC_TYPE          IS '메트릭 유형';
COMMENT ON COLUMN MONITORING_ALERT.CONDITION_OPERATOR   IS '조건 연산자';
COMMENT ON COLUMN MONITORING_ALERT.THRESHOLD_VALUE      IS '임계값';
COMMENT ON COLUMN MONITORING_ALERT.SEVERITY             IS '심각도';
COMMENT ON COLUMN MONITORING_ALERT.IS_ACTIVE            IS '활성 여부';
COMMENT ON COLUMN MONITORING_ALERT.NOTIFICATION_EMAIL   IS '알림 이메일';
COMMENT ON COLUMN MONITORING_ALERT.NOTIFICATION_SLACK   IS '알림 Slack 채널';
COMMENT ON COLUMN MONITORING_ALERT.LAST_TRIGGERED_AT    IS '마지막 발생 시간';
COMMENT ON COLUMN MONITORING_ALERT.TRIGGER_COUNT        IS '발생 횟수';

-- 인덱스 생성
CREATE INDEX IDX_MONITORING_ALERT_TYPE                  ON MONITORING_ALERT(ALERT_TYPE);
CREATE INDEX IDX_MONITORING_ALERT_ACTIVE                ON MONITORING_ALERT(IS_ACTIVE);
CREATE INDEX IDX_MONITORING_ALERT_SEVERITY              ON MONITORING_ALERT(SEVERITY);
```

### <font color = "Aquamarine">10. 트리거 함수</font>
```postgresql
-- 업데이트 시간 자동 갱신 함수
CREATE TRIGGER TRG_UPDATE_MONITORING_PROCESS_UPDATED_AT 
    BEFORE UPDATE ON MONITORING_PROCESS
    FOR EACH ROW EXECUTE FUNCTION UPDATE_UPDATED_AT_COLUMN();

CREATE TRIGGER TRG_UPDATE_MONITORING_ALERT_UPDATED_AT 
    BEFORE UPDATE ON MONITORING_ALERT
    FOR EACH ROW EXECUTE FUNCTION UPDATE_UPDATED_AT_COLUMN();
```

## <font color="Yellow">모니터링 대시보드 기능</font>
### <font color="Aquamarine">실시간 모니터링</font>
#### <p>1. 프로세스 현황</p>
<p>-> 실행 중인 프로세스 목록</p>
<p>-> CPU/메모리 사용률</p>
<p>-> 프로세스 상태( RUNNING / STOPPED / ERROR )</p></br>

#### <p>2. 실시간 감시 현황</p>
<p>-> TPS 실시간 그래프</p>
<p>-> Error Count 실시간 그래프</p>
<p>-> 응답 시간 모니터링</p></br>

#### <p>3. Heap 메모리 사용 현황</p>
<p>-> 클러스터별 메모리 사용량</p>
<p>-> GC 발생 빈도 및 소요 시간</p>
<p>-> 경고/심각 상태 알림</p></br>

#### <p>4. 주요 Monitoring 현황</p>
<p>-> Eviction Mode 카운트</p>
<p>-> SUB LOG 카운트</p>
<p>-> Error 카운트</p>
<p>-> DB Log 카운트</p></br>

#### <p>5. 통신율 및 TPS</p>
<p>-> 실시간 TPS 그래프</p>
<p>-> 요청 성공/실패율</p>
<p>-> 피크 TPS 표시</p></br>

#### <p>6. TCP/Peer 실시간 감시</p>
<p>-> 연결 상태 (ESTABLISHED, TIME_WAIT 등)
<p>-> 활성 연결 수</p>
<p>-> 송수신 패킷/바이트 통계</p>

### <font color="Aquamarine">통계 화면</font>
#### 모니터링 데이터 수집 주기

| 매트릭 유형   | 수집주기 | 보관 기간 |
|----------|------|-------|
| 프로세스 현황  | 10초  | 7일    |
| 실시간 메트릭  | 5초   | 24시간  |
| Heap 메모리 | 30초  | 30일   |
| TPS      | 5초   | 7일    |
| Error    | 즉시   | 90일   |
| TCP/Peer | 30초  | 7일    |
| 통계(분)    | 1분   | 30일   |
| 통계(시간)   | 1시간  | 90일   |
| 통계(일)    | 1일   | 1년    |
</br>

#### 알람 설정 예시
```postgresql
-- Heap 메모리 80% 이상 경고
INSERT INTO MONITORING_ALERT (
    ALERT_NAME, ALERT_TYPE, METRIC_TYPE, 
    CONDITION_OPERATOR, THRESHOLD_VALUE, SEVERITY, 
    NOTIFICATION_EMAIL, CREATED_ID
) VALUES (
    'Heap Memory Warning', 'THRESHOLD', 'HEAP_USAGE_PERCENT',
    'GTE', 80.0, 'WARNING',
    'admin@example.com', 'SYSTEM'
);

-- TPS 급증 알람 (100 이상)
INSERT INTO MONITORING_ALERT (
ALERT_NAME, ALERT_TYPE, METRIC_TYPE,
CONDITION_OPERATOR, THRESHOLD_VALUE, SEVERITY,
NOTIFICATION_SLACK, CREATED_ID
) VALUES (
'High TPS Alert', 'THRESHOLD', 'TPS',
'GT', 100.0, 'WARNING',
'#monitoring-alerts', 'SYSTEM'
);

-- Error 발생 알람
INSERT INTO MONITORING_ALERT (
ALERT_NAME, ALERT_TYPE, METRIC_TYPE,
CONDITION_OPERATOR, THRESHOLD_VALUE, SEVERITY,
NOTIFICATION_EMAIL, CREATED_ID
) VALUES (
'Error Alert', 'THRESHOLD', 'ERROR_COUNT',
'GT', 0, 'ERROR',
'dev-team@example.com', 'SYSTEM'
);
```

### <font color = "Aquamarine">통계 조회 API 예시</font>
```bash
# 시간별 평균 TPS 조회
GET /api/monitoring/statistics?metric=TPS&period=HOUR&aggregation=AVG&start=2024-11-01&end=2024-11-25

# 일별 에러 합계 조회
GET /api/monitoring/statistics?metric=ERROR_COUNT&period=DAY&aggregation=SUM&start=2024-11-01&end=2024-11-25

# 분별 Heap 메모리 최대값 조회
GET /api/monitoring/statistics?metric=HEAP_USAGE&period=MINUTE&aggregation=MAX&start=2024-11-25T09:00:00&end=2024-11-25T10:00:00
```
</br>

## <font color = "Yellow">모니터링 Elasticsearch 인덱스 설계</font>
### <font color = "Aquamarine">1. 시계열 인덱스 패턴(Time-based Index Pattern)</font>
#### Elasticsearch에서 로그 데이터는 날짜 기반 인덱스 패턴을 사용하는 것이 Best
<p>날짜별 인덱스 분리의 장점</p>

| 효율적인 데이터 삭제 | 오래된 인덱스 전체를 삭제(DELETE가 아닌 DROP 개념)  |
|-------------|-------------------------------------|
| 검색 성능 향상    | 날짜 범위 검색 시 관련 인덱스만 조회               |
| 샤드 관리 용이    | 인덱스별로 샤드 크기 조절 가능                   |
| 롤오버 자동화     | ILM(Index Lifecycle Management)정책 적용 |
| 백업 용이       | 인덱스 단위로 스냅샷 생성 가능                   |

### <font color = "aquamarine">인덱스 명명 규칙</font>
#### 패턴: {인덱스명}-{YYYY-MM}-{일련번호}
<p>예시 : </p>
<p>예시 : </p>
<p>예시 : </p>

### <font color = "aquamarine">ILM 단계별 설명</font>

| 단계     | 시점  | 동작             | 목적        |
|--------|-----|----------------|-----------|
| Host   | 0일  | 데이터 입력, 빠른 검색  | 실시간 로그 수집 |
| Warn   | 7일  | 샤드 축소, 세그먼트 병합 | 저장 공간 최적화 |
| Cold   | 30일 | 읽기 전용 전환       | 장기 보관     |
| Delete | 90일 | 인덱스 삭제         | 디스크 공간 확보 |

### <font color = "aquamarine">템플릿의 장점</font>
#### 1. 새로운 인덱스 자동 설정
#### 2. 일관된 매핑 구조 유지
#### 3. ILM 정책 자동 적용
#### 4. 샤드 및 복제본 설정 자동화
![인덱스 템플릿 동작 방식](capture-images/Index_Template.png)

### <font color = "aquamarine">인덱스 목록</font>

| 인덱스 템플릿 | 패턴 | 롤오버 주기 | 보관 기간 |
|-------------|------|-----------|----------|
| application-logs-template | application-logs-* | 30일 또는 50GB | 90일 |
| access-logs-template | access-logs-* | 30일 또는 50GB | 90일 |
| error-logs-template | error-logs-* | 30일 또는 50GB | 90일 |
| performance-metrics-template | performance-metrics-* | 1일 | 30일 |
| database-logs-template | database-logs-* | 30일 | 30일 |
| audit-logs-template | audit-logs-* | 30일 또는 50GB | 365일 |
| security-logs-template | security-logs-* | 30일 또는 50GB | 365일 |


## Elasticsearch Index 생성
### 1. Kibana Dev Tools 접속
```text
1. 브라우저에서 http://localhost:30601 접속
2. 좌측 메뉴에서 "Management" 클릭
3. "Dev Tools" 클릭 (또는 좌측 하단의 렌치 모양 아이콘)
```

### 2.1 Elasticsearch 상태 확인
```json
# Elasticsearch 클러스터 정보
GET /

# 응답 예시:
{
  "name" : "elasticsearch-7ff9d8c675-l2gv8",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "...",
  "version" : {
    "number" : "8.11.0"
  },
  "tagline" : "You Know, for Search"
}
```

### 2.2 현재 인덱스 목록 확인
```json
# 모든 인덱스 보기
GET /_cat/indices?v

# 응답: 아직 인덱스가 없으면 빈 결과
```

## 3. Index Template 생성(자동 설정)
### 3.1 애플리케이션 로그용 Template
```json
PUT _index_template/application-logs-template
{
  "index_patterns": ["application-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "index.lifecycle.name": "logs-policy",
      "index.lifecycle.rollover_alias": "application-logs"
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date",
          "format": "strict_date_optional_time||epoch_millis"
        },
        "log_level": {
          "type": "keyword"
        },
        "logger_name": {
          "type": "keyword"
        },
        "thread_name": {
          "type": "keyword"
        },
        "message": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "stack_trace": {
          "type": "text"
        },
        "application": {
          "type": "keyword"
        },
        "environment": {
          "type": "keyword"
        },
        "host": {
          "properties": {
            "name": { "type": "keyword" },
            "ip": { "type": "ip" }
          }
        },
        "process": {
          "properties": {
            "pid": { "type": "long" },
            "name": { "type": "keyword" }
          }
        },
        "user_id": {
          "type": "long"
        },
        "request_id": {
          "type": "keyword"
        }
      }
    }
  }
}

# 응답:
{
  "acknowledged" : true
}
```

### 3.2 접근 로그용 Template
```json
PUT _index_template/access-logs-template
{
  "index_patterns": ["access-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "index.lifecycle.name": "logs-policy"
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "http": {
          "properties": {
            "method": { "type": "keyword" },
            "url": {
              "type": "text",
              "fields": {
                "keyword": { "type": "keyword" }
              }
            },
            "status_code": { "type": "integer" },
            "response_time_ms": { "type": "long" }
          }
        },
        "client": {
          "properties": {
            "ip": { "type": "ip" },
            "user_agent": {
              "type": "text",
              "fields": {
                "keyword": { "type": "keyword" }
              }
            }
          }
        },
        "user": {
          "properties": {
            "id": { "type": "long" },
            "login_id": { "type": "keyword" }
          }
        },
        "application": {
          "type": "keyword"
        }
      }
    }
  }
}
```

### 3.3 에러 로그용 Template
```json
PUT _index_template/error-logs-template
{
  "index_patterns": ["error-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "index.lifecycle.name": "logs-policy"
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "error": {
          "properties": {
            "type": { "type": "keyword" },
            "message": { "type": "text" },
            "code": { "type": "keyword" },
            "severity": { "type": "keyword" },
            "stack_trace": { "type": "text" }
          }
        },
        "source": {
          "properties": {
            "file": { "type": "keyword" },
            "line": { "type": "integer" },
            "method": { "type": "keyword" }
          }
        },
        "user": {
          "properties": {
            "id": { "type": "long" },
            "login_id": { "type": "keyword" }
          }
        },
        "application": {
          "type": "keyword"
        }
      }
    }
  }
}
```

## 4. ILM(Index Lifecycle Management) 정책 생성
```json
PUT _ilm/policy/logs-policy
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_primary_shard_size": "50gb",
            "max_age": "30d"
          },
          "set_priority": {
            "priority": 100
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": {
            "number_of_shards": 1
          },
          "forcemerge": {
            "max_num_segments": 1
          },
          "set_priority": {
            "priority": 50
          }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "set_priority": {
            "priority": 0
          }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}

# 응답:
{
  "acknowledged" : true
}
```

### 4.2 ILM 정책 확인
```json
# 모든 ILM 정책 보기
GET _ilm/policy

# 특정 정책 보기
GET _ilm/policy/logs-policy
```

## 5. 초기 인덱스 생성 및 Alias 설정
### 5.1 애플리케이션 로그 인덱스
```json
# 첫 번째 인덱스 생성 (write 인덱스 지정)
PUT application-logs-2025-11-000001
{
  "aliases": {
    "application-logs": {
      "is_write_index": true
    }
  }
}

# 응답:
{
  "acknowledged" : true,
  "shards_acknowledged" : true,
  "index" : "application-logs-2025-11-000001"
}
```

### 5.2 접근 로그 인덱스
```json
PUT access-logs-2025-11-000001
{
  "aliases": {
    "access-logs": {
      "is_write_index": true
    }
  }
}
```

### 5.3 에러 로그 인덱스
```json
PUT error-logs-2025-11-000001
{
  "aliases": {
    "error-logs": {
      "is_write_index": true
    }
  }
}
```

### 5.4 생성된 인덱스 확인
```json
# 모든 인덱스 목록
GET /_cat/indices?v

# 응답 예시:
health status index                          pri rep docs.count store.size
yellow open   application-logs-2025-11-000001  1   0          0       225b
yellow open   access-logs-2025-11-000001       1   0          0       225b
yellow open   error-logs-2025-11-000001        1   0          0       225b

# Alias 확인
GET /_cat/aliases?v

# 응답 예시:
alias              index                          
application-logs   application-logs-2025-11-000001
access-logs        access-logs-2025-11-000001    
error-logs         error-logs-2025-11-000001
```

## 6. 테스트 데이터 삽입
### 6.1 애플리케이션 로그 삽입
```json
# 단일 문서 삽입 (Alias 사용)
POST application-logs/_doc
{
  "@timestamp": "2025-11-25T10:00:00",
  "log_level": "INFO",
  "logger_name": "com.example.UserService",
  "thread_name": "http-nio-8080-exec-1",
  "message": "사용자 로그인 성공",
  "application": "eng-study",
  "environment": "production",
  "host": {
    "name": "eng-study-backend",
    "ip": "10.0.0.1"
  },
  "user_id": 1,
  "request_id": "req-12345"
}

# 응답:
{
  "_index" : "application-logs-2025-11-000001",
  "_id" : "자동생성된ID",
  "result" : "created"
}
```

6.2 여러 개 데이터 삽입(Bulk API)
```json
POST application-logs/_bulk
{"index":{}}
{"@timestamp":"2025-11-25T10:00:00","log_level":"INFO","message":"서버 시작됨","application":"eng-study"}
{"index":{}}
{"@timestamp":"2025-11-25T10:01:00","log_level":"WARN","message":"느린 쿼리 감지: 2.5초","application":"eng-study"}
{"index":{}}
{"@timestamp":"2025-11-25T10:02:00","log_level":"ERROR","message":"데이터베이스 연결 실패","application":"eng-study"}
{"index":{}}
{"@timestamp":"2025-11-25T10:03:00","log_level":"INFO","message":"데이터베이스 연결 복구","application":"eng-study"}
```

6.3 접근 로그 삽입
```json
POST access-logs/_bulk
{"index":{}}
{"@timestamp":"2025-11-25T10:00:00","http":{"method":"GET","url":"/api/users","status_code":200,"response_time_ms":45},"client":{"ip":"192.168.1.100"},"application":"eng-study"}
{"index":{}}
{"@timestamp":"2025-11-25T10:01:00","http":{"method":"POST","url":"/api/login","status_code":200,"response_time_ms":120},"client":{"ip":"192.168.1.101"},"user":{"id":1,"login_id":"john_doe"},"application":"eng-study"}
{"index":{}}
{"@timestamp":"2025-11-25T10:02:00","http":{"method":"GET","url":"/api/lessons","status_code":500,"response_time_ms":5000},"client":{"ip":"192.168.1.100"},"application":"eng-study"}
```

6.4 에러 로그 삽입
```json
POST error-logs/_bulk
{"index":{}}
{"@timestamp":"2025-11-25T10:00:00","error":{"type":"NullPointerException","message":"Cannot invoke method on null object","severity":"ERROR","stack_trace":"at com.example.UserService.getUser(UserService.java:45)"},"source":{"file":"UserService.java","line":45,"method":"getUser"},"user":{"id":1,"login_id":"john_doe"},"application":"eng-study"}
{"index":{}}
{"@timestamp":"2025-11-25T10:05:00","error":{"type":"SQLException","message":"Connection timeout","severity":"CRITICAL"},"application":"eng-study"}
```

## 7. 데이터 검색 및 조회
### 7.1 전체 데이터 조회
```json
# 모든 애플리케이션 로그 조회
GET application-logs/_search

# 최근 10개만 조회
GET application-logs/_search
{
  "size": 10,
  "sort": [
    {
      "@timestamp": {
        "order": "desc"
      }
    }
  ]
}
```

### 7.2 조건 검색
```json
# ERROR 레벨 로그만 검색
GET application-logs/_search
{
  "query": {
    "term": {
      "log_level": "ERROR"
    }
  }
}

# 특정 메시지 포함 검색
GET application-logs/_search
{
  "query": {
    "match": {
      "message": "데이터베이스"
    }
  }
}

# 최근 1시간 로그 검색
GET application-logs/_search
{
  "query": {
    "range": {
      "@timestamp": {
        "gte": "now-1h"
      }
    }
  }
}

# 복합 조건 (AND)
GET application-logs/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "term": {
            "log_level": "ERROR"
          }
        },
        {
          "range": {
            "@timestamp": {
              "gte": "now-1h"
            }
          }
        }
      ]
    }
  }
}
```

### 7.3 집계(Aggregation)
```json
# 로그 레벨별 카운트
GET application-logs/_search
{
  "size": 0,
  "aggs": {
    "by_log_level": {
      "terms": {
        "field": "log_level"
      }
    }
  }
}

# 시간대별 로그 수
GET application-logs/_search
{
  "size": 0,
  "aggs": {
    "logs_over_time": {
      "date_histogram": {
        "field": "@timestamp",
        "fixed_interval": "1h"
      }
    }
  }
}
```

## 8. 인덱스 관리 명령어
```json
# 인덱스 통계
GET application-logs-2025-11-000001/_stats

# 인덱스 매핑 확인
GET application-logs-2025-11-000001/_mapping

# 인덱스 설정 확인
GET application-logs-2025-11-000001/_settings

# ILM 상태 확인
GET application-logs-*/_ilm/explain

# 인덱스 삭제 (주의!)
DELETE application-logs-2025-11-000001

# 모든 데이터 삭제 (주의!)
POST application-logs/_delete_by_query
{
  "query": {
    "match_all": {}
  }
}
```