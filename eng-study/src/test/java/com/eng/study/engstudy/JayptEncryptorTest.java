package com.eng.study.engstudy;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Jasypt 암호화 테스트
 *
 * [ 목적 ]
 * application.yml에 저장되는 민감한 정보(DB 비밀번호, API 키 등)를 암호화 한다.
 *
 * [ Jasypt란? ]
 * - Java Simplified Encryption의 약자
 * - 설정 파일의 민감 정보를 암호화하여 저장
 * - 애플리케이션 실행 시 자동으로 복호화
 *
 * [ 왜 필요한가? ]
 * 1. application.yml을 Git에 커밋해도 안전
 *    - 암호화된 값: ENC(......)
 *    - 원본 비밀번호는 노출되지 않음
 *
 * 2. 환경별로 다른 비밀번호 사용 가능
 *    - 개발: ENC(dev_encrypted_value)
 *    - 프로덕션: ENC(prod_encrypted_value)
 *
 * 3. 보안 규정 준수
 *    - 평문 비밀번호 저장 금지 정책 충족
 *
 * [ 사용 시기 ]
 * 1. 프로젝트 초기 설정 시
 * 2. 새로운 외부 서비스 연동 시( API 키 등 )
 * 3. 비밀번호 변경 시
 *
 * [ 동작 흐름 ]
 * 1. 테스트로 암호화 키 생성
 * 2. 환경 변수에 암호화 키 설정
 * 3. 민감 정보를 암호화
 * 4. application.yml에 ENC(...)형태로 저장
 * 5. 애플리케이션 실행 시 자동 복호화
 *
 * [ 실행 방법 ]
 * ./mvnw test -Dtest=JasyptEncryptorTest#generateJasyptPassword
 **/
public class JayptEncryptorTest {

    private StandardPBEStringEncryptor encryptor;

    /**
     * 각 테스트 실행 전에 암호화 객체 초기화
     *
     * [설정 내용]
     * - password: 암호화/복호화에 사용할 마스터 키
     * - algorithm: PBEWithMD5AndDES (Password-Based Encryption)
     * - keyObtentionIterations: 1000번 해싱 (무차별 대입 공격 방어)
     * - saltGenerator: 랜덤 Salt 생성 (같은 값도 다르게 암호화)
     *
     * [환경 변수 사용]
     * - JASYPT_ENCRYPTOR_PASSWORD 환경 변수에서 키를 가져옴
     * - 설정되지 않으면 기본값 사용 (개발용)
     */
    @BeforeEach
    void setup() {
        encryptor = new StandardPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        // 환경 변수에서 암호화 키 가져오기
        // 프로덕션에서는 반드시 환경 변수로 설정해야 함
        String password = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        if (password == null) {
            password = "default-encryption-key-change-this";
            System.out.println("⚠️  환경 변수 JASYPT_ENCRYPTOR_PASSWORD가 설정되지 않았습니다.");
            System.out.println("기본 키를 사용합니다: " + password);
            System.out.println("\n프로덕션에서는 반드시 환경 변수를 설정하세요:");
            System.out.println("export JASYPT_ENCRYPTOR_PASSWORD=<생성된키>");
        }

        // Jasypt 설정
        config.setPassword(password);  // 마스터 암호화 키
        config.setAlgorithm("PBEWithMD5AndDES");  // 암호화 알고리즘
        config.setKeyObtentionIterations("1000");  // 키 유도 반복 횟수
        config.setPoolSize("1");  // 암호화 인스턴스 풀 크기
        config.setProviderName("SunJCE");  // JCE Provider
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");  // Salt 생성기
        config.setStringOutputType("base64");  // 출력 인코딩

        encryptor.setConfig(config);
    }

    /**
     * 데이터베이스 자격증명 암호화
     *
     * [목적]
     * PostgreSQL 연결 정보(URL, Username, Password)를 암호화합니다.
     *
     * [암호화 대상]
     * 1. datasource.url: DB 연결 URL
     * 2. datasource.username: DB 사용자명
     * 3. datasource.password: DB 비밀번호
     *
     * [사용 흐름]
     * 1. 이 테스트 실행하여 암호화된 값 얻기
     * 2. application-prod.yml에 ENC(...) 형태로 저장
     * 3. JasyptConfig.java가 자동으로 복호화
     *
     * [주의사항]
     * - 암호화 키를 잃어버리면 복호화 불가!
     * - 암호화 키는 환경 변수로만 관리
     * - Git에 절대 커밋하지 말 것
     *
     * [실행 방법]
     * export JASYPT_ENCRYPTOR_PASSWORD=<생성된키>
     * ./mvnw test -Dtest=JasyptEncryptorTest#encryptDatabaseCredentials
     */
    @Test
    void encryptDatabaseCredentials() {
        System.out.println("=".repeat(80));
        System.out.println("Jasypt 데이터베이스 자격증명 암호화");
        System.out.println("=".repeat(80));

        // 암호화할 DB 정보 (실제 값으로 변경하세요)
        String dbUrl = "jdbc:postgresql://localhost:5432/DEV_DB";
        String dbUsername = "rnbsoft";
        String dbPassword = "rnbsoft";

        // 각 값을 Jasypt로 암호화
        // 같은 값도 매번 다른 결과 (Salt 사용)
        String encryptedUrl = encryptor.encrypt(dbUrl);
        String encryptedUsername = encryptor.encrypt(dbUsername);
        String encryptedPassword = encryptor.encrypt(dbPassword);

        System.out.println("\n📋 원본 값:");
        System.out.println("- URL: " + dbUrl);
        System.out.println("- Username: " + dbUsername);
        System.out.println("- Password: " + dbPassword);

        System.out.println("\n🔒 암호화된 값:");
        System.out.println("- URL: ENC(" + encryptedUrl + ")");
        System.out.println("- Username: ENC(" + encryptedUsername + ")");
        System.out.println("- Password: ENC(" + encryptedPassword + ")");

        System.out.println("\n📝 application.yml 사용 예시:");
        System.out.println("spring:");
        System.out.println("  datasource:");
        System.out.println("    url: ENC(" + encryptedUrl + ")");
        System.out.println("    username: ENC(" + encryptedUsername + ")");
        System.out.println("    password: ENC(" + encryptedPassword + ")");

        System.out.println("\n" + "=".repeat(80));

        // 복호화가 정상 동작하는지 검증
        System.out.println("\n✅ 복호화 검증:");
        System.out.println("- URL 복호화: " + encryptor.decrypt(encryptedUrl));
        System.out.println("- Username 복호화: " + encryptor.decrypt(encryptedUsername));
        System.out.println("- Password 복호화: " + encryptor.decrypt(encryptedPassword));
        System.out.println("\n👉 복호화 성공! 암호화된 값을 안전하게 사용할 수 있습니다.");
        System.out.println("=".repeat(80));
    }

    /**
     * JWT Secret 암호화
     *
     * [목적]
     * application.yml의 jwt.secret 값을 암호화합니다.
     *
     * [왜 JWT Secret도 암호화?]
     * - JWT Secret이 노출되면 공격자가 토큰 위조 가능
     * - 여러 환경(개발/스테이징/프로덕션)의 Secret을 다르게 관리
     *
     * [실행 방법]
     * export JASYPT_ENCRYPTOR_PASSWORD=<생성된키>
     * ./mvnw test -Dtest=JasyptEncryptorTest#encryptJwtSecret
     */
    @Test
    void encryptJwtSecret() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Jasypt JWT Secret 암호화");
        System.out.println("=".repeat(80));

        // 암호화할 JWT Secret (JwtKeyGeneratorTest로 생성한 값 사용)
        String jwtSecret = "local-jwt-secret-key-for-kubernetes-development-only";
        String encryptedSecret = encryptor.encrypt(jwtSecret);

        System.out.println("\n📋 원본 JWT Secret:");
        System.out.println(jwtSecret);

        System.out.println("\n🔒 암호화된 JWT Secret:");
        System.out.println("ENC(" + encryptedSecret + ")");

        System.out.println("\n📝 application.yml 사용 예시:");
        System.out.println("jwt:");
        System.out.println("  secret: ENC(" + encryptedSecret + ")");
        System.out.println("  access-token-expiration: 3600000");
        System.out.println("  refresh-token-expiration: 604800000");

        System.out.println("\n✅ 복호화 검증: " + encryptor.decrypt(encryptedSecret));
        System.out.println("=".repeat(80));
    }

    /**
     * 커스텀 값 암호화
     *
     * [목적]
     * API 키, 외부 서비스 비밀번호 등 다양한 민감 정보를 암호화합니다.
     *
     * [암호화 가능한 값들]
     * - API 키 (Google, AWS, 결제 등)
     * - SMTP 비밀번호
     * - Redis 비밀번호
     * - OAuth Client Secret
     * - 외부 서비스 토큰
     *
     * [실행 방법]
     * export JASYPT_ENCRYPTOR_PASSWORD=<생성된키>
     * ./mvnw test -Dtest=JasyptEncryptorTest#encryptCustomValues
     */
    @Test
    void encryptCustomValues() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Jasypt 커스텀 값 암호화");
        System.out.println("=".repeat(80));

        // 암호화할 다양한 민감 정보들
        String[] valuesToEncrypt = {
                "api-key-12345",        // API 키
                "secret-token-xyz",     // 시크릿 토큰
                "smtp-password",        // SMTP 비밀번호
                "redis-password"        // Redis 비밀번호
        };

        System.out.println("\n🔒 암호화 결과:");
        for (String value : valuesToEncrypt) {
            String encrypted = encryptor.encrypt(value);
            System.out.println("\n원본: " + value);
            System.out.println("암호화: ENC(" + encrypted + ")");
            System.out.println("복호화 검증: " + encryptor.decrypt(encrypted) + " ✓");
        }

        System.out.println("\n" + "=".repeat(80));
    }

    /**
     * Jasypt 암호화 키(마스터 키) 생성
     *
     * [목적]
     * Jasypt 암호화/복호화에 사용할 강력한 마스터 키를 생성합니다.
     *
     * [생성 방법]
     * - UUID 2개를 연결하여 64자 길이의 강력한 키 생성
     * - 예: a1b2c3d4e5f6...
     *
     * [중요!]
     * 1. 이 키는 절대 Git에 커밋하면 안 됩니다
     * 2. 키를 잃어버리면 암호화된 값을 복호화할 수 없습니다
     * 3. 프로덕션에서는 반드시 안전한 곳에 보관하세요
     *    - Kubernetes Secret
     *    - AWS Secrets Manager
     *    - HashiCorp Vault
     *
     * [사용 흐름]
     * 1. 이 테스트 실행하여 키 생성
     * 2. 환경 변수로 설정: export JASYPT_ENCRYPTOR_PASSWORD=<생성된키>
     * 3. 다른 테스트 실행하여 값 암호화
     * 4. 애플리케이션 실행 시 동일한 환경 변수 설정
     *
     * [실행 방법]
     * ./mvnw test -Dtest=JasyptEncryptorTest#generateJasyptPassword
     */
    @Test
    void generateJasyptPassword() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Jasypt 암호화 키(마스터 키) 생성");
        System.out.println("=".repeat(80));

        // UUID 2개를 결합하여 강력한 64자 키 생성
        // 하이픈 제거: a1b2c3d4-e5f6-... → a1b2c3d4e5f6...
        String generatedKey = UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");

        System.out.println("\n🔑 생성된 Jasypt 암호화 키:");
        System.out.println(generatedKey);
        System.out.println("\n키 정보:");
        System.out.println("- 길이: " + generatedKey.length() + "자 (권장: 32자 이상)");
        System.out.println("- 강도: 매우 강함 (UUID 기반)");

        System.out.println("\n📝 사용 방법:");

        System.out.println("\n1️⃣  환경 변수로 설정 (권장):");
        System.out.println("   # Linux/Mac");
        System.out.println("   export JASYPT_ENCRYPTOR_PASSWORD=" + generatedKey);
        System.out.println("\n   # Windows CMD");
        System.out.println("   set JASYPT_ENCRYPTOR_PASSWORD=" + generatedKey);
        System.out.println("\n   # Windows PowerShell");
        System.out.println("   $env:JASYPT_ENCRYPTOR_PASSWORD=\"" + generatedKey + "\"");

        System.out.println("\n2️⃣  application.yml에 설정 (비권장, 개발용만):");
        System.out.println("   jasypt:");
        System.out.println("     encryptor:");
        System.out.println("       password: " + generatedKey);

        System.out.println("\n3️⃣  Kubernetes Secret으로 설정 (프로덕션 권장):");
        System.out.println("   kubectl create secret generic jasypt-secret \\");
        System.out.println("     --from-literal=JASYPT_ENCRYPTOR_PASSWORD=" + generatedKey + " \\");
        System.out.println("     -n eng-study");

        System.out.println("\n4️⃣  애플리케이션 실행 시 전달:");
        System.out.println("   java -Djasypt.encryptor.password=" + generatedKey + " -jar app.jar");

        System.out.println("\n" + "=".repeat(80));
        System.out.println("⚠️  주의사항:");
        System.out.println("1. 이 키는 절대 Git에 커밋하지 마세요!");
        System.out.println("2. .gitignore에 application-prod.yml이 있는지 확인하세요");
        System.out.println("3. 프로덕션 환경에서는 반드시 안전하게 보관하세요");
        System.out.println("   - Kubernetes Secret");
        System.out.println("   - AWS Secrets Manager");
        System.out.println("   - HashiCorp Vault");
        System.out.println("4. 키를 잃어버리면 암호화된 값을 복호화할 수 없습니다!");
        System.out.println("5. 환경별(개발/스테이징/프로덕션)로 다른 키를 사용하세요");
        System.out.println("=".repeat(80));
    }

    /**
     * 암호화/복호화 동작 테스트
     *
     * [목적]
     * Jasypt의 Salt 기반 암호화 특성을 확인합니다.
     *
     * [테스트 내용]
     * 1. 같은 값을 여러 번 암호화
     * 2. 매번 다른 암호화 결과 확인 (Salt 때문)
     * 3. 모든 암호화 결과가 정상 복호화되는지 확인
     *
     * [Jasypt의 특징]
     * - 같은 값도 매번 다르게 암호화 (랜덤 Salt 사용)
     * - 하지만 복호화하면 모두 원본으로 돌아옴
     * - BCrypt와 유사한 방식
     *
     * [실행 방법]
     * export JASYPT_ENCRYPTOR_PASSWORD=<생성된키>
     * ./mvnw test -Dtest=JasyptEncryptorTest#testEncryptionDecryption
     */
    @Test
    void testEncryptionDecryption() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("Jasypt 암호화/복호화 동작 테스트");
        System.out.println("=".repeat(80));

        String testValue = "test-secret-value-123!@#";

        System.out.println("\n📋 원본 값: " + testValue);

        // 같은 값을 3번 암호화 (매번 다른 결과)
        System.out.println("\n🔄 같은 값을 여러 번 암호화 (매번 다른 결과):");
        System.out.println("이유: Salt가 랜덤하게 생성되기 때문");

        for (int i = 1; i <= 3; i++) {
            String encrypted = encryptor.encrypt(testValue);
            String decrypted = encryptor.decrypt(encrypted);

            System.out.println("\n" + i + "번째 암호화:");
            System.out.println("  🔒 암호화: " + encrypted);
            System.out.println("  🔓 복호화: " + decrypted);
            System.out.println("  ✅ 일치 여부: " + (testValue.equals(decrypted) ? "성공" : "실패"));
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("💡 Jasypt 특징 요약:");
        System.out.println("1. 같은 값도 매번 다르게 암호화 (Salt 사용)");
        System.out.println("2. 하지만 복호화하면 모두 원본으로 돌아옴");
        System.out.println("3. 암호화 키(JASYPT_ENCRYPTOR_PASSWORD)가 같으면 복호화 가능");
        System.out.println("4. 키가 다르면 복호화 불가 (주의!)");
        System.out.println("=".repeat(80));
    }
}
