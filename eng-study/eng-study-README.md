경로 : /Monitoring/eng-study/README.md

<h1>목차</h1>
<p>- 프로젝트 개요</p>
<p>- 주요 기능</p>
<p>- 기술 스택</p>
<p>- 프로젝트 구조</p>
<p>- 시작하기</p>
<p>- API 문서</p>
<p>- 보안</p>
<p>- 테스트</p>
<p>- 배포</p>
<p>- 트러블 슈팅</p>


<h1>프로젝트 개요</h1>
영어 학습을 위한 웹 플랫폼의 백엔드 서버입니다. JWT기반 인증, BCrypt 비밀번호 암호화,
HttpOnly Cookie를 사용한 보안 강화 등 현대적인 웹 보안 기술을 적용했습니다.

## 주요 특징
- 보안 강화 : JWT + HttpOnly Cookie + BCrypt
- 마이크로서비스 준비: Kubernetes 배포 지원
- 모니터링 : Prometheus + Actuator
- 확장 기능 : Mybatis 기반 유연한 쿼리 관리


## 주요 기능
### 1. 사용자 인증

| 기능      | 설명                           |
|---------|------------------------------|
| 회원가입    | 이메일/ID중복 확인, 입력값 검증          |
| 로그인     | JWT토큰 발급, HttpOnly Cookie 저장 |
| 로그아웃    | 토큰 무효화                       |
| 토큰 갱신   | Refresh Token을 통한 자동 갱신      |
| 내 정보 조회 | 인증된 사용자 정보 반환                |


### 2. 보안

| 기술               | 설명                         |
|------------------|----------------------------|
| HttpOnly Cookie  | XSS 공격 방어                  |
| SameSite = Lax   | CSRF 공격 방어                 |
| BCrypt           | Salt 기반 비밀번호 암호화           |
| JWT              | 무상태 인증 토큰                  |
| Input Validation | Spring Validation 기반 입력값 검증 |
| Jasypt           | 설정 파일 민감 정보 암호화            |

## 기술 스택
### 핵심 환경
<p>-> Java: 21(OpenJDK)
<p>-> Spring Boot: 3.5.7</p>
<p>-> 빌드 도구: Maven 3.9+</p>

## 주요 의존성
### 웹 개발
<p>-> spring-boot-starter-web: RESTful API</p>
<p>-> spring-boot-starter-validation: 입력값 검증</p>

### 데이터베이스
<p>-> spring-boot-starter-jdbc: JDBC 연동</p>
<p>-> postgresql: PostgreSQL 드라이버</p>
<p>-> mybatis-spring-boot-starter: MyBatis ORM</p>

### 보안
<p>-> spring-boot-starter-security: Spring Security</p>
<p>-> jjwt-api, jjwt-impl, jjwt-jackson: JWT 구현</p>
<p>-> jasypt-spring-boot-starter: 설정 암호화</p>

### 모니터링
<p>-> spring-boot-starter-actuator: 헬스체크, 메트릭</p>
<p>-> micrometer-registry-prometheus: Prometheus 메트릭</p>

### 유틸리티
<p>-> lombok: 보일러플레이트 코드 감소</p>

## 프로젝트 구조
eng-study/
├── src/
│   ├── main/
│   │   ├── java/com/eng/study/engstudy/
│   │   │   ├── config/                 # 설정 클래스
│   │   │   │   ├── CorsConfig.java     # CORS 설정
│   │   │   │   ├── DatabaseConfig.java # DB 설정
│   │   │   │   ├── JasyptConfig.java   # Jasypt 암호화 설정
│   │   │   │   └── SecurityConfig.java # Spring Security 설정
│   │   │   │
│   │   │   ├── controller/             # REST API 컨트롤러
│   │   │   │   ├── AuthController.java # 인증 API
│   │   │   │   ├── MainController.java # 메인 페이지
│   │   │   │   └── TestController.java # 테스트 API
│   │   │   │
│   │   │   ├── converter/              # VO ↔ DTO 변환
│   │   │   │   └── UsersConverter.java
│   │   │   │
│   │   │   ├── mapper/                 # MyBatis Mapper 인터페이스
│   │   │   │   └── UsersMapper.java
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── dto/                # 데이터 전송 객체
│   │   │   │   │   ├── request/        # 요청 DTO
│   │   │   │   │   │   ├── LoginRequestDTO.java
│   │   │   │   │   │   └── RegisterRequestDTO.java
│   │   │   │   │   └── response/       # 응답 DTO
│   │   │   │   │       └── AuthResponseDTO.java
│   │   │   │   └── vo/                 # Value Object (DB 매핑)
│   │   │   │       ├── SystemVO.java
│   │   │   │       └── UsersVO.java
│   │   │   │
│   │   │   ├── service/                # 비즈니스 로직
│   │   │   │   ├── AuthService.java   # 인터페이스
│   │   │   │   └── impl/
│   │   │   │       └── AuthServiceImpl.java
│   │   │   │
│   │   │   ├── util/                   # 유틸리티 클래스
│   │   │   │   ├── CookieUtil.java     # Cookie 헬퍼
│   │   │   │   ├── JwtUtil.java        # JWT 토큰 관리
│   │   │   │   └── PasswordEncoderUtil # BCrypt를 사용한 비밀번호 암호화 및 검증
│   │   │   │
│   │   │   └── EngStudyApplication.java # 메인 클래스
│   │   │
│   │   └── resources/
│   │       ├── mapper/                  # MyBatis XML 매퍼
│   │       │   └── Auth/
│   │       │       └── UsersMapper.xml
│   │       ├── application.yml          # 기본 설정
│   │       ├── application-prod.yml     # 프로덕션 설정
│   │       ├── static/                  # 정적 리소스
│   │       │   └── index.html
│   │       └── templates/               # 템플릿
│   │
│   └── test/
│       └── java/com/eng/study/engstudy/
│           ├── BCryptPasswordEncoderTest.java      # BCrypt 테스트
│           ├── EngStudyApplicationTests.java       # 기본 테스트
│           ├── JasyptEncryptorTest.java            # Jasypt 테스트
│           └── JwtKeyGeneratorTest.java            # JWT 키 생성
│
├── Dockerfile                           # Docker 이미지 빌드
├── pom.xml                              # Maven 설정
└── README.md                            # 이 문서

<h1>시작하기</h1>

## 개발 환경 접속( 데이터베이스 접속 )
### K8S PostgreSQL DB Pod 포트 포워딩(로컬 환경과 연결 하기 위한 터널링 명령어)
<p> PostgreSQL DB 터널링 연결</p> 
<p> 명령어 : kubectl port-forward -n eng-study service/postgres-service 5432:5432</p>
<p> -> PostgreSQL 데이터베이스는 현재 K8S의 Pod에 설치가 되어 있는 상황</p>

<p> K8S PostgreSQL Pod 접속</p>
<p> 명령어 : kubectl exec -it deployment/postgres -n eng-study -- psql -U rnbsoft -d DEV_DB</p>
<p> -> K8S에 설치된 PostgreSQL Pod에 접속</p>

#### DBEaver에서 K8S PostgreSQL Pod 접속 정보

| 이름       | 설정 값      |
|----------|-----------|
| Host     | localhost |
| Port     | 5432      |
| Database | DEV_DB    |
| Username | rnbsoft   |
| Password | rnbsoft   |

### K8S Elasticsearch DB Pod 포트 포워딩(로컬 환경과 연결 하기 위한 터널링 명령어)
<p> ElasticSearch DB 터널링 연결</p>
<p> 명령어 : kubectl port-forward -n monitoring service/elasticsearch-service 9200:9200</p>
<p> -> Elasticsearch 데이터베이스는 현재 K8S의 Pod에 설치가 되어 있는 상황</p>

## 개발 환경 접속( UI 접속 )
### K8S Kibana 웹 브라우저 포트 포워딩(로컬 환경과 연결 하기 위한 터널링 명령어)
<p> Kibana 웹 브라우저 터널링 연결</p>
<p> 명령어 : kubectl port-forward -n monitoring service/kibana-service 5601:5601</p>
<p> -> Kibana는 현재 K8S의 Pod에 설치가 되어 있는 상황</p>

### K8S Prometheus 웹 브라우저 포트 포워딩(로컬 환경과 연결 하기 위한 터널링 명령어)
<p> prometheus 웹 브라우저 터널링 연결</p>
<p> 명령어 : kubectl port-forward -n monitoring service/prometheus-service 9090:9090</p>
<p> -> Prometheus는 현재 K8S의 Pod에 설치가 되어 있는 상황</p>
