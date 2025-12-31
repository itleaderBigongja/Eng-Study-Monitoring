# 영어 학습 플랫폼 백엔드 (eng-study)
Spring Boot 3.5.7 + PostgreSQL + MyBatis 기반의 RESTful API 서버

## 📋 목차
- [프로젝트 개요](#프로젝트-개요)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [시작하기](#시작하기)
- [API 엔드포인트](#api-엔드포인트)
- [보안](#보안)
- [데이터베이스](#데이터베이스)
- [모니터링](#모니터링)
- [배포](#배포)
- [트러블슈팅](#트러블슈팅)

---

## 🎯 프로젝트 개요

영어 학습을 위한 웹 플랫폼의 백엔드 서버입니다. JWT 기반 인증, BCrypt 비밀번호 암호화, HttpOnly Cookie를 사용한 보안 강화 등 현대적인 웹 보안 기술을 적용했습니다.

### 주요 특징
- **보안 강화**: JWT + HttpOnly Cookie + BCrypt
- **마이크로서비스 준비**: Kubernetes 배포 지원
- **모니터링**: Prometheus + Actuator 연동
- **유연한 쿼리 관리**: MyBatis 기반 SQL 매핑
- **타입 안정성**: Lombok을 활용한 보일러플레이트 감소

---

## ✨ 주요 기능

### 1. 사용자 인증

| 기능 | 설명 | 엔드포인트 |
|------|------|-----------|
| 회원가입 | 이메일/ID 중복 확인, 입력값 검증 | `POST /api/auth/register` |
| 로그인 | JWT 토큰 발급, HttpOnly Cookie 저장 | `POST /api/auth/login` |
| 로그아웃 | 토큰 무효화 | `POST /api/auth/logout` |
| 토큰 갱신 | Refresh Token을 통한 자동 갱신 | `POST /api/auth/refresh` |
| 내 정보 조회 | 인증된 사용자 정보 반환 | `GET /api/auth/me` |
| ID 중복 확인 | 사용 가능한 로그인 ID 확인 | `GET /api/auth/check-loginId` |
| 이메일 중복 확인 | 사용 가능한 이메일 확인 | `GET /api/auth/check-email` |

### 2. 보안 기술

| 기술 | 설명 | 구현 위치 |
|------|------|-----------|
| HttpOnly Cookie | XSS 공격 방어 | `CookieUtil.java` |
| SameSite = Lax | CSRF 공격 방어 | `CookieUtil.java` |
| BCrypt | Salt 기반 비밀번호 암호화 | `PasswordEncoderUtil.java` |
| JWT | 무상태 인증 토큰 | `JwtUtil.java` |
| Input Validation | Spring Validation 기반 입력값 검증 | `RegisterRequestDTO.java` |
| Jasypt | 설정 파일 민감 정보 암호화 | `JasyptConfig.java` |

---

## 🛠 기술 스택

### 핵심 환경
- **Java**: 21 (OpenJDK)
- **Spring Boot**: 3.5.7
- **빌드 도구**: Maven 3.9+

### 주요 의존성

#### 웹 개발
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

#### 데이터베이스
```xml
<!-- JDBC -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- MyBatis -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>
```

#### 보안
```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- Jasypt -->
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

#### 모니터링
```xml
<!-- Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

#### 유틸리티
```xml
<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

---

## 📁 프로젝트 구조

```
eng-study/
├── src/
│   ├── main/
│   │   ├── java/com/eng/study/engstudy/
│   │   │   │
│   │   │   ├── config/                     # 설정 클래스
│   │   │   │   ├── CorsConfig.java         # CORS 설정 ✅
│   │   │   │   ├── DatabaseConfig.java     # DB 및 MyBatis 설정 ✅
│   │   │   │   ├── JasyptConfig.java       # Jasypt 암호화 설정 ✅
│   │   │   │   └── SecurityConfig.java     # Spring Security 설정 ✅
│   │   │   │
│   │   │   ├── controller/                 # REST API 컨트롤러
│   │   │   │   ├── AuthController.java     # 인증 API ✅
│   │   │   │   ├── MainController.java     # 메인 페이지
│   │   │   │   └── TestController.java     # 테스트 API
│   │   │   │
│   │   │   ├── converter/                  # VO ↔ DTO 변환
│   │   │   │   └── UsersConverter.java     # 사용자 데이터 변환 ✅
│   │   │   │
│   │   │   ├── mapper/                     # MyBatis Mapper 인터페이스
│   │   │   │   └── UsersMapper.java        # 사용자 데이터 접근 ✅
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── dto/                    # 데이터 전송 객체
│   │   │   │   │   ├── request/
│   │   │   │   │   │   ├── LoginRequestDTO.java        ✅
│   │   │   │   │   │   └── RegisterRequestDTO.java     ✅
│   │   │   │   │   └── response/
│   │   │   │   │       └── AuthResponseDTO.java        ✅
│   │   │   │   │
│   │   │   │   └── vo/                     # Value Object (DB 매핑)
│   │   │   │       ├── SystemVO.java       # 공통 필드 (생성일, 수정일) ✅
│   │   │   │       └── UsersVO.java        # 사용자 테이블 VO ✅
│   │   │   │
│   │   │   ├── service/                    # 비즈니스 로직
│   │   │   │   ├── AuthService.java        # 인터페이스 ✅
│   │   │   │   └── impl/
│   │   │   │       └── AuthServiceImpl.java # 구현체 ✅
│   │   │   │
│   │   │   ├── util/                       # 유틸리티 클래스
│   │   │   │   ├── CookieUtil.java         # Cookie 헬퍼 ✅
│   │   │   │   ├── JwtUtil.java            # JWT 토큰 관리 ✅
│   │   │   │   └── PasswordEncoderUtil.java # 비밀번호 암호화 ✅
│   │   │   │
│   │   │   └── EngStudyApplication.java    # 메인 클래스 ✅
│   │   │
│   │   └── resources/
│   │       ├── mapper/                      # MyBatis XML 매퍼
│   │       │   └── Auth/
│   │       │       └── UsersMapper.xml      # SQL 쿼리 정의 ✅
│   │       │
│   │       ├── application.yml              # 기본 설정 ✅
│   │       ├── application-prod.yml         # 프로덕션 설정
│   │       ├── static/
│   │       │   └── index.html              # 정적 리소스
│   │       └── templates/                   # 템플릿 (미사용)
│   │
│   └── test/
│       └── java/com/eng/study/engstudy/
│           ├── BCryptPasswordEncoderTest.java      # BCrypt 테스트 ✅
│           ├── EngStudyApplicationTests.java       # 기본 테스트 ✅
│           ├── JasyptEncryptorTest.java            # Jasypt 테스트 ✅
│           └── JwtKeyGeneratorTest.java            # JWT 키 생성 ✅
│
├── target/                                  # 빌드 결과물
│   ├── classes/                            # 컴파일된 클래스
│   └── eng-study-0.0.1-SNAPSHOT.jar        # 실행 가능 JAR
│
├── Dockerfile                               # Docker 이미지 빌드 ✅
├── pom.xml                                  # Maven 설정 ✅
└── README.md                                # 이 문서
```

---

## 🚀 시작하기

### 사전 요구사항
- **Java**: 21 이상 (OpenJDK)
- **Maven**: 3.9 이상
- **PostgreSQL**: 실행 중 (localhost:5432 또는 Kubernetes Pod)
- **Docker**: (선택) Kubernetes 배포 시 필요

### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd Monitoring/eng-study
```

### 2. 데이터베이스 연결
#### 로컬 PostgreSQL 사용
```bash
# PostgreSQL 실행 확인
psql -U rnbsoft -d DEV_DB -h localhost -p 5432
```

#### Kubernetes PostgreSQL 사용 (포트 포워딩)
```bash
# 터미널 1: PostgreSQL Pod 포트 포워딩
kubectl port-forward -n eng-study service/postgres-service 5432:5432

# 터미널 2: 애플리케이션 실행
./mvnw spring-boot:run
```

### 3. 환경 변수 설정 (선택)
#### Jasypt 암호화 키 설정
```bash
# Linux/Mac
export JASYPT_ENCRYPTOR_PASSWORD=your-secret-key

# Windows (CMD)
set JASYPT_ENCRYPTOR_PASSWORD=your-secret-key

# Windows (PowerShell)
$env:JASYPT_ENCRYPTOR_PASSWORD="your-secret-key"
```

### 4. 애플리케이션 실행
#### Maven 명령어
```bash
# 개발 모드 실행
./mvnw spring-boot:run

# 프로덕션 프로파일 실행
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

#### JAR 파일 실행
```bash
# 빌드
./mvnw clean package -DskipTests

# 실행
java -jar target/eng-study-0.0.1-SNAPSHOT.jar
```

#### IDE에서 실행
```
1. IntelliJ IDEA / Eclipse에서 프로젝트 열기
2. EngStudyApplication.java 파일 찾기
3. main() 메서드 실행
```

### 5. 실행 확인
```bash
# Health Check
curl http://localhost:8080/actuator/health

# 응답 예시:
# {"status":"UP"}
```

**접속 URL**: http://localhost:8080

---

## 📡 API 엔드포인트
### 인증 API (`/api/auth`)
#### 1. 회원가입
```http
POST /api/auth/register
Content-Type: application/json

{
  "loginId": "john_doe",
  "password": "Password123!",
  "email": "john@example.com",
  "fullName": "John Doe",
  "postalCode": "12345",
  "address": "서울특별시 영등포구",
  "addressDetail": "101호",
  "addressType": "R",
  "sido": "서울특별시",
  "sigugun": "영등포구",
  "bname": "여의도동"
}
```

**응답 (201 Created)**:
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "user": {
      "usersId": 1,
      "loginId": "john_doe",
      "fullName": "John Doe",
      "email": "john@example.com"
    }
  }
}
```

**Set-Cookie 헤더**:
```
access_token=eyJhbGc...; HttpOnly; Path=/; Max-Age=3600; SameSite=Lax
refresh_token=eyJhbGc...; HttpOnly; Path=/; Max-Age=604800; SameSite=Lax
```

#### 2. 로그인
```http
POST /api/auth/login
Content-Type: application/json

{
  "loginId": "john_doe",
  "password": "Password123!"
}
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "message": "로그인에 성공했습니다.",
  "data": {
    "user": {
      "usersId": 1,
      "loginId": "john_doe",
      "fullName": "John Doe",
      "email": "john@example.com"
    }
  }
}
```

#### 3. 로그아웃
```http
POST /api/auth/logout
Cookie: access_token=...; refresh_token=...
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "message": "로그아웃되었습니다."
}
```

#### 4. 토큰 갱신
```http
POST /api/auth/refresh
Cookie: refresh_token=...
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "message": "토큰이 갱신되었습니다.",
  "data": {
    "user": {
      "usersId": 1,
      "loginId": "john_doe",
      "fullName": "John Doe",
      "email": "john@example.com"
    }
  }
}
```

#### 5. 내 정보 조회
```http
GET /api/auth/me
Cookie: access_token=...
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "usersId": 1,
    "loginId": "john_doe",
    "fullName": "John Doe",
    "email": "john@example.com"
  }
}
```

#### 6. 로그인 ID 중복 확인
```http
GET /api/auth/check-loginId?loginId=john_doe
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "available": false,
  "message": "이미 사용중인 아이디입니다."
}
```

#### 7. 이메일 중복 확인
```http
GET /api/auth/check-email
Content-Type: application/json

"john@example.com"
```

**응답 (200 OK)**:
```json
{
  "success": true,
  "available": true,
  "message": "사용 가능한 이메일입니다."
}
```

### 에러 응답 예시
```json
{
  "success": false,
  "message": "이미 사용중인 로그인 ID입니다."
}
```

---

## 🔒 보안
### 1. HttpOnly Cookie
```java
// CookieUtil.java
public Cookie createCookie(String name, String value, int maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(true);        // ✅ JavaScript 접근 불가 (XSS 방어)
    cookie.setSecure(false);         // ⚠️ 개발: false, 운영: true
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    cookie.setAttribute("SameSite", "Lax"); // ✅ CSRF 방어
    return cookie;
}
```

**장점**:
- JavaScript로 Cookie 접근 불가 → XSS 공격 방어
- SameSite 속성으로 CSRF 공격 방어
- 자동으로 요청에 포함됨

### 2. BCrypt 비밀번호 암호화
```java
// PasswordEncoderUtil.java
public String encode(String plainPassword) {
    return passwordEncoder.encode(plainPassword);
}

// 암호화 결과 예시
// 입력: "Password123!"
// 출력: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
```

**특징**:
- Salt 자동 생성 (같은 비밀번호도 매번 다른 해시)
- 단방향 암호화 (복호화 불가능)
- 의도적으로 느린 속도 (무차별 대입 공격 방어)

### 3. JWT 토큰
```java
// JwtUtil.java
public String generateAccessToken(Long usersId, String loginId, String role) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("usersId", usersId);
    claims.put("loginId", loginId);
    claims.put("role", role);
    claims.put("type", "access");
    
    return createToken(claims, loginId, accessTokenExpiration);
}
```

**토큰 구조**:
```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2Vyc0lkIjoxLCJsb2dpbklkIjoiam9obiIsInJvbGUiOiJVU0VSIn0.signature
└─ Header ────────┘ └─────────────── Payload ─────────────────────────────┘ └─ Signature ─┘
```

### 4. Input Validation
```java
// RegisterRequestDTO.java
@NotBlank(message = "로그인 ID는 필수입니다.")
@Size(min = 3, max = 50)
@Pattern(regexp = "^[a-zA-Z0-9_-]+$")
private String loginId;

@NotBlank(message = "비밀번호는 필수입니다.")
@Size(min = 8)
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
    message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다"
)
private String password;
```

### 5. CORS 설정
```java
// CorsConfig.java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",        // Next.js 개발 서버
    "http://localhost:30080",       // Kubernetes NodePort
    "http://nginx-service"          // Kubernetes 내부 통신
));
configuration.setAllowCredentials(true);  // ✅ Cookie 전송 허용
```

### 6. Jasypt 설정 암호화
```yaml
# application.yml
spring:
  datasource:
    password: ENC(encrypted-password-here)  # 암호화된 비밀번호
```

**암호화 방법**:
```bash
# JasyptEncryptorTest.java 실행
mvn test -Dtest=JasyptEncryptorTest

# 출력:
# Encrypted: ENC(XyZ123AbC...)
```

---

## 🗄 데이터베이스

### ERD (핵심 테이블)
```
┌─────────────────────────────┐
│         USERS               │
├─────────────────────────────┤
│ PK  users_id        BIGINT  │ ← Auto Increment
│ UK  login_id        VARCHAR │
│ UK  email           VARCHAR │
│     password        VARCHAR │ ← BCrypt 암호화
│     full_name       VARCHAR │
│     postal_code     VARCHAR │
│     address         VARCHAR │
│     address_detail  VARCHAR │
│     address_type    VARCHAR │
│     sido            VARCHAR │
│     sigungu         VARCHAR │
│     bname           VARCHAR │
│     last_login      TIMESTAMP│
│     is_active       BOOLEAN │ ← DEFAULT TRUE
│     role            VARCHAR │ ← DEFAULT 'USER'
│     created_at      TIMESTAMP│ ← DEFAULT NOW()
│     created_id      VARCHAR │
│     updated_at      TIMESTAMP│
│     updated_id      VARCHAR │
└─────────────────────────────┘
```

### MyBatis 사용 예시

#### Mapper Interface
```java
// UsersMapper.java
@Mapper
public interface UsersMapper {
    int insertUser(UsersVO usersVO);
    UsersVO findByLoginId(@Param("loginId") String loginId);
    int countByLoginId(@Param("loginId") String loginId);
}
```

#### XML 매퍼
```xml
<!-- UsersMapper.xml -->
<insert id="insertUser" parameterType="UsersVO" useGeneratedKeys="true" keyProperty="usersId">
    INSERT INTO users (
        login_id, email, password, full_name,
        postal_code, address, address_detail,
        is_active, role, created_id
    ) VALUES (
        #{loginId}, #{email}, #{password}, #{fullName},
        #{postalCode}, #{address}, #{addressDetail},
        COALESCE(#{isActive}, TRUE),
        COALESCE(#{role}, 'USER'),
        #{createdId}
    )
</insert>
```

### 데이터베이스 연결 확인
```bash
# Kubernetes Pod 접속
kubectl exec -it deployment/postgres -n eng-study -- psql -U rnbsoft -d DEV_DB

# 테이블 확인
\dt

# 사용자 조회
SELECT users_id, login_id, email, full_name FROM users;
```

---

## 📊 모니터링

### Actuator 엔드포인트

#### 1. Health Check
```bash
curl http://localhost:8080/actuator/health
```

**응답**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 300000000000
      }
    }
  }
}
```

#### 2. Prometheus Metrics

**주요 메트릭**:
```
# HELP jvm_memory_used_bytes Used memory
jvm_memory_used_bytes{area="heap"} 134217728.0

# HELP http_server_requests_seconds HTTP 요청 응답 시간
http_server_requests_seconds_count{method="POST",uri="/api/auth/login",status="200"} 42

# HELP hikaricp_connections HikariCP 연결 풀
hikaricp_connections{pool="EngStudy-HikariCP",state="active"} 5
```

#### 3. Application Info
```bash
curl http://localhost:8080/actuator/info
```

### Prometheus 연동
#### Prometheus 설정 (prometheus.yml)
```yaml
scrape_configs:
  - job_name: '모니터링 대상 JOB 이름( Application )'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['모니터링 대상-service:8080']
```

#### 주요 모니터링 지표
- JVM 메모리 사용량
- HTTP 요청 응답 시간
- 데이터베이스 커넥션 풀 상태
- 애플리케이션 상태 (UP/DOWN)

---

## 🐳 배포

### Docker 빌드

#### Dockerfile 구조
```dockerfile
# 1단계: 빌드
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# 2단계: 실행
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# 로그 디렉토리 생성
RUN mkdir -p /app/logs
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring /app/logs
USER spring:spring

EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Docker 이미지 빌드
```bash
# 프로젝트 루트에서
docker build -t eng-study:local .

# 이미지 확인
docker images | grep eng-study
```

### Kubernetes 배포

#### 1. 전체 시스템 배포
```bash
# 배포 스크립트 실행
./deploy-local.sh
```

#### 2. 개별 리소스 배포
```bash
# Namespace 생성
kubectl apply -f k8s-local/01-namespace.yaml

# PostgreSQL 배포
kubectl apply -f k8s-local/db-init-configmap.yaml
kubectl apply -f k8s-local/02-postgresql.yaml

# 백엔드 배포
kubectl apply -f k8s-local/05-eng-study-backend.yaml
```

#### 3. 배포 확인
```bash
# Pod 상태 확인
kubectl get pods -n eng-study

# 출력 예시:
# NAME                                  READY   STATUS    RESTARTS   AGE
# eng-study-backend-7f9d8c675-abc12    1/1     Running   0          2m
# postgres-6b8f9c675-def34              1/1     Running   0          5m

# 서비스 확인
kubectl get svc -n eng-study

# 로그 확인
kubectl logs -f deployment/eng-study-backend -n eng-study
```

### 접속 정보

| 서비스 | URL                             | 설명 |
|--------|---------------------------------|------|
| 백엔드 API | http://localhost:30080/api      | Nginx를 통한 접속 |
| Actuator | http://localhost:30080/actuator | 모니터링 엔드포인트 |
| Prometheus | http://localhost:30100          | 메트릭 수집 (터널링 필요) |

---

## 🐛 트러블슈팅

### 1. 데이터베이스 연결 실패
**증상**: `Connection refused` 에러

**해결**:
```bash
# 1. PostgreSQL Pod 상태 확인
kubectl get pods -n eng-study | grep postgres