package com.eng.study.engstudy;


import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.SecureRandom; // SecureRandom 추가
import java.util.Base64;

/**
 * JWT Secret Key 생성 테스트
 * [ 목적 ]
 * JWT(JSON Web Token)토큰의 서명에 사용할 안전한 시크릿 키를 생성합니다.
 *
 * [ 왜 필요한가? ]
 * - JWT는 사용자 인증 정보를담고 있어서 반드시 안전한 키로 서명해야 합니다.
 * - 약한 키를 사용하면 공격자가 토큰을 위조할 수 있습니다.
 * - 최소 256비트(32바이트) 이상의 강력한 키가 필요합니다.
 *
 * [ 사용시기 ]
 * 1. 프로젝트 최초 설정 시
 * 2. 보안 정책 변경으로 키를 교체할 때
 * 3. 환경별(개발/스테이징/프로덕션) 다른 키가 필요할 때
 *
 * [ 실행 방법 ]
 * ./mvnw test -Dtest=JwtKeyGeneratorTest#generateJwtSecretKey
 *
 * [ 실행된 키 사용 방법 ]
 * 1. application.yml 파일에 생성된 Secret Key 추가
 *    jwt:
 *      secret: <생성된 Base64키>
 *      access-token-expiration: 3600000      # 1시간 (밀리초)
 *      refresh-token-expiration: 604800000   # 7일 (밀리초)
 * 2. 환경 변수로 설정
 *    export JWT_SECRET=<생성된 Base64키>
 * 3. Kubernetes Secret 설정
 *    Kubernetes Secret: kubectl create secret generic jwt-secret --from-literal=JWT_SECRET=<생성된 Base64키>
 */
public class JwtKeyGeneratorTest {

    /**
     * 단일 JWT 시크릿 키 생성
     *
     * [동작 과정]
     * 1. HS256 알고리즘용 256비트 랜덤 키 생성
     * 2. Base64로 인코딩 (application.yml에 저장하기 위해)
     * 3. 콘솔에 출력 및 사용 방법 안내
     *
     * [출력 정보]
     * - Base64 인코딩된 키 문자열
     * - 키 길이 정보 (256 bits)
     * - 사용 방법 예시
     *
     * [참고]
     * - JJWT 0.12.0 이상부터 Jwts.SIG.HS256.key().build() 사용 권장
     * - 이전의 Keys.secretKeyFor()와 SignatureAlgorithm enum 방식은 deprecated
     */

    @Test
    void generateJwtSecretKey() {
        // HS256 알고리즘에 적합한 암호학적으로 안전한 256비트 키 생성
        // SecureRandom을 내부적으로 사용하여 예측 불가능한 키 생성
        // JJWT 0.12.0+ 최신 방식
        SecretKey key = Jwts.SIG.HS256.key().build();

        // 바이너리 키를 Base64 문자열로 변환
        // 이유 : application.yml 같은 텍스트 파일에 저장하기 위해
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());

        // 키 생성완료
        System.out.println("=".repeat(80));
        System.out.println("JWT Secret Key 생성 완료");
        System.out.println("=".repeat(80));
        System.out.println("\n생성된 키 (Base64 인코딩):");
        System.out.println(base64Key);

        // 생성된 키의 상세 정보 출력
        System.out.println("\n키 정보:");
        System.out.println("- 알고리즘: " + key.getAlgorithm());  // HmacSHA256
        System.out.println("- 키 길이: " + key.getEncoded().length * 8 + " bits");  // 256 bits
        System.out.println("- Base64 길이: " + base64Key.length() + " characters");
    }

    /**
     * 여러 환경용 JWT 키 생성
     *
     * [ 목적 ]
     * 개발, 스테이징, 프로덕션 환경별로 서로 다른 키를 생성
     *
     * [ 왜 환경별로 다른 키를 생성해야 하나? ]
     * 1. 보안 격리: 개발 환경의 키가 노출되어도 프로덕션은 안전
     * 2. 토큰 혼용 방지: 개발 토큰으로 프로덕션 접근 불가
     * 3. 보안 정책: 환경별로 다른 만료 시간/권한 적용 가능
     *
     * [ 실행 방법 ]
     * ./mvnw test -Dtest=JwtKeyGeneratorTest#generateMultipleKeys
     **/
    @Test
    void generateMultipleKeys() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("여러 개의 JWT Secret Key 생성 (용도별 사용)");
        System.out.println("=".repeat(80));

        // 환경별로 다른 키 생성
        String[] purposes = {"개발(Development)", "스테이징(Staging)", "프로덕션(Production)"};

        for (String purpose : purposes) {
            // 각 환경마다 완전히 다른 랜덤 . 생성
            // JJWT 최신 방식 사용
            SecretKey key = Jwts.SIG.HS256.key().build();
            String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
            System.out.println("\n[" + purpose + " 환경용 키] = " + base64Key);
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("⚠️  주의사항:");
        System.out.println("- 각 환경마다 다른 키를 사용하세요 (보안 격리)");
        System.out.println("- 키는 안전하게 보관하고 절대 Git에 커밋하지 마세요 (.gitignore 확인!)");
        System.out.println("- 프로덕션 키는 시크릿 관리 시스템에 저장하세요");
        System.out.println("  (예: Kubernetes Secret, AWS Secrets Manager, HashiCorp Vault)");
        System.out.println("- 키를 잃어버리면 모든 사용자가 다시 로그인해야 합니다");
        System.out.println("=".repeat(80));
    }

    /**
     * HS512 알고리즘용 키 생성 (더 강력한 보안)
     *
     * [HS256 vs HS512]
     * - HS256: 256비트 키, 256비트 해시 (일반적 사용)
     * - HS512: 512비트 키, 512비트 해시 (더 강력한 보안)
     *
     * [언제 HS512를 사용?]
     * - 매우 민감한 데이터 처리
     * - 금융 서비스
     * - 의료 정보 시스템
     * - 정부/공공기관 시스템
     *
     * [실행 방법]
     * ./mvnw test -Dtest=JwtKeyGeneratorTest#generateHS512Key
     */
    @Test
    void generateHS512Key() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("HS512 JWT Secret Key 생성 (더 강력한 보안)");
        System.out.println("=".repeat(80));

        // HS512 알고리즘용 512비트 키 생성
        SecretKey key = Jwts.SIG.HS512.key().build();
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());

        System.out.println("\n생성된 HS512 키:");
        System.out.println(base64Key);

        System.out.println("\n키 정보:");
        System.out.println("- 알고리즘: " + key.getAlgorithm());  // HmacSHA512
        System.out.println("- 키 길이: " + key.getEncoded().length * 8 + " bits");  // 512 bits
        System.out.println("- Base64 길이: " + base64Key.length() + " characters");

        System.out.println("\n📝 JwtUtil.java 설정 변경 필요:");
        System.out.println("// HS256 대신 HS512 사용");
        System.out.println("return Jwts.builder()");
        System.out.println("    .setClaims(claims)");
        System.out.println("    .setSubject(subject)");
        System.out.println("    .setIssuedAt(now)");
        System.out.println("    .setExpiration(expiryDate)");
        System.out.println("    .signWith(secretKey)  // HS512 키 자동 인식");
        System.out.println("    .compact();");

        System.out.println("\n💡 HS256 vs HS512:");
        System.out.println("- HS256: 빠른 속도, 일반적 사용, 256비트");
        System.out.println("- HS512: 더 강력한 보안, 약간 느림, 512비트");
        System.out.println("- 대부분의 경우 HS256으로 충분");
        System.out.println("- 매우 민감한 데이터는 HS512 권장");
        System.out.println("=".repeat(80));
    }
}