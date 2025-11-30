package com.eng.study.engstudy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 비밀번호 암호화 테스트
 *
 * [ 목적 ]
 * 사용자 비밀번호를 안전하게 암호화하고 검증하는 BCrypt 알고리즘의 동작을 확인합니다.
 *
 * [ BCrypt란? ]
 * - Salt를 자동으로 생성하여 같은 비밀번호도 매번 다른 해시값을 생성
 * - 해시 계산에 의도적으로 시간이 걸리게 하여 무차별 대입 공격(Brute Force ) 방어
 * - 단방향 해시: 암호화된 값에서 원본을 알아낼 수 없음
 *
 * [ 왜 필요한가? ]
 * - DB에 평문 비밀번호 저장은 매우 위험( DB 해킹 시 모든 비밀번호 노출 )
 * - MD5, SHA-1 같은 단순 해시는 Rainbow Table 공격에 취약
 * - BCrypt는 현대적인 비밀번호 저장 표준
 *
 * [ 사용 시기 ]
 * 1. 회원가입 시 : 사용자 비밀번호 암호화
 * 2. 로그인 시 : 입력한 비밀번호와 저장된 해시 비교
 * 3. 비밀번호 변경 시 : 새 비밀번호 암호화
 *
 * [ 실행 방법 ]
 * ./mvnw test -Dtest=BCryptPasswordEncoderTest#encodePassword
 * */
public class BCryptPasswordEncoderTest {

    // BCrypt 암호화 객체 생성
    // 기본 strength: 10 (2^10 = 1024번 해싱)
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 비밀번호 암호화 테스트
     *
     * [동작 과정]
     * 1. 평문 비밀번호 입력
     * 2. BCrypt가 랜덤 Salt 생성
     * 3. Salt + 비밀번호를 여러 번 해싱
     * 4. Salt와 Hash를 합쳐서 최종 결과 생성
     *
     * [주요 특징]
     * - 같은 비밀번호를 암호화해도 매번 다른 결과
     * - 이유: Salt가 매번 랜덤하게 생성되기 때문
     * - 결과 형식: $2a$10$[22자 Salt][31자 Hash]
     *   $2a: BCrypt 버전
     *   $10: Cost factor (2^10번 해싱)
     *
     * [실행 방법]
     * ./mvnw test -Dtest=BCryptPasswordEncoderTest#encodePassword
     */
    @Test
    void encodePassword() {
        // 테스트할 평문 비밀번호
        String plainPassword = "password123";

        System.out.println("=".repeat(80));
        System.out.println("BCrypt 비밀번호 암호화 테스트");
        System.out.println("=".repeat(80));
        System.out.println("\n원본 비밀번호: " + plainPassword);
        System.out.println("\n암호화된 비밀번호:");

        // 같은 비밀번호를 3번 암호화
        // 결과가 모두 다른 이유: Salt가 매번 랜덤하게 생성
        for (int i = 1; i <= 3; i++) {
            String encodedPassword = encoder.encode(plainPassword);
            System.out.println(i + ". " + encodedPassword);

            // 암호화된 비밀번호 구조 설명 (첫 번째만)
            if (i == 1) {
                System.out.println("   구조: $2a (버전) $10 (강도) $[Salt][Hash]");
            }
        }

        System.out.println("\n특징:");
        System.out.println("- BCrypt는 같은 비밀번호도 매번 다른 해시값을 생성합니다 (Salt 사용)");
        System.out.println("- 해시값 길이: 60자 (고정)");
        System.out.println("- 형식: $2a$10$[22자 Salt][31자 Hash]");
        System.out.println("- 계산 시간: 의도적으로 느림 (무차별 대입 공격 방어)");
        System.out.println("=".repeat(80));
    }

    /**
     * 비밀번호 검증 테스트
     *
     * [동작 과정]
     * 1. 평문 비밀번호를 BCrypt로 암호화하여 DB에 저장
     * 2. 로그인 시 입력받은 비밀번호와 DB의 해시값 비교
     * 3. BCrypt가 저장된 해시에서 Salt를 추출
     * 4. 입력 비밀번호 + 추출한 Salt로 다시 해싱
     * 5. 결과가 저장된 해시와 일치하면 비밀번호 맞음
     *
     * [왜 다른 해시값인데 비교가 가능한가?]
     * - BCrypt 해시에는 Salt 정보가 포함되어 있음
     * - matches() 메서드가 Salt를 추출하여 동일한 방식으로 검증
     *
     * [실행 방법]
     * ./mvnw test -Dtest=BCryptPasswordEncoderTest#verifyPassword
     */
    @Test
    void verifyPassword() {
        String plainPassword = "password123";
        String wrongPassword = "wrongPassword";

        // 1. 회원가입 시: 비밀번호 암호화하여 DB 저장
        String encodedPassword = encoder.encode(plainPassword);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("BCrypt 비밀번호 검증 테스트");
        System.out.println("=".repeat(80));
        System.out.println("\n[회원가입 시나리오]");
        System.out.println("원본 비밀번호: " + plainPassword);
        System.out.println("DB 저장 (암호화): " + encodedPassword);

        // 2. 로그인 시: 입력 비밀번호와 DB 해시 비교
        boolean isMatch = encoder.matches(plainPassword, encodedPassword);
        boolean isWrongMatch = encoder.matches(wrongPassword, encodedPassword);

        System.out.println("\n[로그인 시나리오]");
        System.out.println("검증 결과:");
        System.out.println("- 올바른 비밀번호 입력: " + (isMatch ? "✓ 로그인 성공" : "✗ 로그인 실패"));
        System.out.println("- 잘못된 비밀번호 입력: " + (isWrongMatch ? "✗ 보안 문제!" : "✓ 로그인 차단됨"));

        System.out.println("\n동작 원리:");
        System.out.println("1. DB에 저장된 해시에서 Salt 추출");
        System.out.println("2. 입력 비밀번호 + 추출한 Salt로 해싱");
        System.out.println("3. 결과를 DB 해시와 비교");
        System.out.println("=".repeat(80));
    }

    /**
     * 여러 비밀번호 암호화 테스트
     *
     * [목적]
     * 다양한 형태의 비밀번호가 어떻게 암호화되는지 확인
     *
     * [실행 방법]
     * ./mvnw test -Dtest=BCryptPasswordEncoderTest#encodeMultiplePasswords
     */
    @Test
    void encodeMultiplePasswords() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("여러 비밀번호 암호화 테스트");
        System.out.println("=".repeat(80));

        // 다양한 형태의 비밀번호
        String[] passwords = {
                "admin123",      // 일반적인 비밀번호
                "user1234",      // 숫자 포함
                "test5678",      // 다른 숫자
                "password!@#"    // 특수문자 포함
        };

        for (String password : passwords) {
            String encoded = encoder.encode(password);
            System.out.println("\n원본: " + password);
            System.out.println("암호화: " + encoded);
            System.out.println("길이: " + encoded.length() + "자 (항상 60자)");
        }

        System.out.println("\n" + "=".repeat(80));
    }

    /**
     * 비밀번호 강도 및 암호화 성능 테스트
     *
     * [목적]
     * 1. 비밀번호 강도 평가 (약함/중간/강함)
     * 2. BCrypt 암호화 소요 시간 측정
     *
     * [비밀번호 강도 평가 기준]
     * - 길이: 8자 이상
     * - 대문자 포함
     * - 소문자 포함
     * - 숫자 포함
     * - 특수문자 포함
     *
     * [BCrypt의 의도적인 느린 속도]
     * - 약 100~300ms 소요 (일반 해시는 1ms 미만)
     * - 이유: 무차별 대입 공격 방어
     * - 예: 1초에 10번만 시도 가능 vs MD5는 수백만 번 가능
     *
     * [실행 방법]
     * ./mvnw test -Dtest=BCryptPasswordEncoderTest#testPasswordStrength
     */
    @Test
    void testPasswordStrength() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("비밀번호 강도 및 암호화 성능 테스트");
        System.out.println("=".repeat(80));

        String[] testPasswords = {
                "123456",           // 약함: 숫자만
                "password",         // 약함: 소문자만
                "Password123",      // 중간: 대소문자+숫자
                "P@ssw0rd!2024",   // 강함: 모든 조건 충족
                "MyS3cur3P@ss!"    // 강함: 모든 조건 충족
        };

        for (String password : testPasswords) {
            // 암호화 시간 측정
            long startTime = System.nanoTime();
            String encoded = encoder.encode(password);
            long endTime = System.nanoTime();

            double milliseconds = (endTime - startTime) / 1_000_000.0;

            System.out.println("\n비밀번호: " + password);
            System.out.println("암호화 시간: " + String.format("%.2f", milliseconds) + " ms");
            System.out.println("강도 평가: " + evaluatePasswordStrength(password));
            System.out.println("암호화 결과: " + encoded.substring(0, 29) + "...");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("💡 권장사항:");
        System.out.println("- 최소 8자 이상");
        System.out.println("- 대소문자, 숫자, 특수문자 조합");
        System.out.println("- 일반적인 단어나 패턴 사용 금지");
        System.out.println("\n⚠️  BCrypt 특징:");
        System.out.println("- 암호화 시간: 약 100~300ms (의도적으로 느림)");
        System.out.println("- 목적: 무차별 대입 공격(Brute Force) 방어");
        System.out.println("- 공격자가 1초에 3~10번만 시도 가능하도록 제한");
        System.out.println("=".repeat(80));
    }

    /**
     * 비밀번호 강도 평가 헬퍼 메서드
     *
     * [평가 기준]
     * - 0~2점: 약함 (위험)
     * - 3~4점: 중간 (개선 필요)
     * - 5~6점: 강함 (안전)
     *
     * @param password 평가할 비밀번호
     * @return 강도 문자열 (약함/중간/강함)
     */
    private String evaluatePasswordStrength(String password) {
        int score = 0;

        // 각 조건 충족 시 점수 부여
        if (password.length() >= 8) score++;           // 1점: 8자 이상
        if (password.length() >= 12) score++;          // 1점: 12자 이상 (보너스)
        if (password.matches(".*[a-z].*")) score++;    // 1점: 소문자 포함
        if (password.matches(".*[A-Z].*")) score++;    // 1점: 대문자 포함
        if (password.matches(".*[0-9].*")) score++;    // 1점: 숫자 포함
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++;  // 1점: 특수문자 포함

        // 점수별 평가
        if (score <= 2) return "약함 ⚠️  (보안 위험!)";
        if (score <= 4) return "중간 ⚡ (개선 권장)";
        return "강함 ✓ (안전)";
    }
}
