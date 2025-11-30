package com.eng.study.engstudy.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 암호화 유틸리티
 *
 * [목적]
 * BCrypt를 사용한 비밀번호 암호화 및 검증 기능 제공
 *
 * [주요 기능]
 * 1. 비밀번호 암호화 (회원가입 시)
 * 2. 비밀번호 검증 (로그인 시)
 * 3. 비밀번호 일치 여부 확인
 *
 * [BCrypt 특징]
 * - Salt 자동 생성 (같은 비밀번호도 매번 다른 해시)
 * - 단방향 암호화 (복호화 불가능)
 * - 의도적으로 느린 속도 (무차별 대입 공격 방어)
 *
 * [사용 예시]
 * <pre>
 * // 회원가입 시
 * String hashedPassword = passwordEncoderUtil.encode(plainPassword);
 * user.setPassword(hashedPassword);
 *
 * // 로그인 시
 * boolean matches = passwordEncoderUtil.matches(inputPassword, user.getPassword());
 * if (matches) {
 *     // 로그인 성공
 * }
 * </pre>
 *
 * @author eng-study
 * @since 2024-11-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordEncoderUtil {

    /**
     * Spring Security의 PasswordEncoder
     * SecurityConfig에서 BCryptPasswordEncoder로 설정됨
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 비밀번호 암호화
     *
     * [동작 과정]
     * 1. BCrypt가 랜덤 Salt 생성
     * 2. Salt + 비밀번호를 여러 번 해싱 (기본 10번)
     * 3. Salt와 Hash를 결합하여 반환
     *
     * [특징]
     * - 같은 비밀번호를 암호화해도 매번 다른 결과
     * - 결과 형식: $2a$10$[22자 Salt][31자 Hash]
     * - 총 길이: 60자 (고정)
     *
     * [사용 시나리오]
     * 1. 회원가입 시 비밀번호 암호화
     * 2. 비밀번호 변경 시 새 비밀번호 암호화
     * 3. 관리자의 비밀번호 초기화
     *
     * @param plainPassword 평문 비밀번호
     * @return BCrypt로 암호화된 비밀번호 (60자)
     * @throws IllegalArgumentException plainPassword가 null이거나 빈 문자열인 경우
     *
     * @example
     * <pre>
     * String plainPassword = "mySecurePassword123!";
     * String encoded = passwordEncoderUtil.encode(plainPassword);
     * // 결과: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
     * </pre>
     */
    public String encode(String plainPassword) {
        // 입력 검증
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            log.error("비밀번호 암호화 실패: 비밀번호가 null 또는 빈 문자열입니다.");
            throw new IllegalArgumentException("비밀번호는 필수 입력값입니다.");
        }

        try {
            // BCrypt 암호화 수행
            String encodedPassword = passwordEncoder.encode(plainPassword);

            log.debug("비밀번호 암호화 성공 (길이: {}자)", encodedPassword.length());

            return encodedPassword;

        } catch (Exception e) {
            log.error("비밀번호 암호화 중 오류 발생", e);
            throw new RuntimeException("비밀번호 암호화에 실패했습니다.", e);
        }
    }

    /**
     * 비밀번호 검증
     *
     * [동작 과정]
     * 1. 저장된 해시에서 Salt 추출
     * 2. 입력 비밀번호 + 추출한 Salt로 해싱
     * 3. 결과를 저장된 해시와 비교
     *
     * [왜 다른 해시값인데 비교가 가능한가?]
     * - BCrypt 해시에는 Salt 정보가 포함되어 있음
     * - matches() 메서드가 Salt를 추출하여 동일한 방식으로 검증
     *
     * [사용 시나리오]
     * 1. 로그인 시 비밀번호 확인
     * 2. 비밀번호 변경 시 현재 비밀번호 확인
     * 3. 중요 작업 전 비밀번호 재확인
     *
     * @param plainPassword 입력받은 평문 비밀번호
     * @param encodedPassword DB에 저장된 암호화된 비밀번호
     * @return 비밀번호 일치 여부 (true: 일치, false: 불일치)
     *
     * @example
     * <pre>
     * // 로그인 시나리오
     * String inputPassword = "userInputPassword";
     * String storedPassword = user.getPassword(); // DB에서 가져온 해시
     *
     * boolean isValid = passwordEncoderUtil.matches(inputPassword, storedPassword);
     * if (isValid) {
     *     // 로그인 성공
     * } else {
     *     // 로그인 실패
     * }
     * </pre>
     */
    public boolean matches(String plainPassword, String encodedPassword) {
        // 입력 검증
        if (plainPassword == null || encodedPassword == null) {
            log.warn("비밀번호 검증 실패: 입력값이 null입니다.");
            return false;
        }

        try {
            // BCrypt 검증 수행
            boolean matches = passwordEncoder.matches(plainPassword, encodedPassword);

            if (matches) {
                log.debug("비밀번호 검증 성공");
            } else {
                log.debug("비밀번호 검증 실패: 비밀번호가 일치하지 않습니다.");
            }

            return matches;

        } catch (Exception e) {
            log.error("비밀번호 검증 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 비밀번호 강도 검증
     *
     * [검증 기준]
     * 1. 길이: 8자 이상
     * 2. 대문자 포함
     * 3. 소문자 포함
     * 4. 숫자 포함
     * 5. 특수문자 포함
     *
     * [점수 체계]
     * - 0~2점: 약함 (사용 불가 권장)
     * - 3~4점: 중간 (사용 가능하나 개선 권장)
     * - 5~6점: 강함 (안전)
     *
     * @param password 검증할 비밀번호
     * @return 비밀번호 강도 점수 (0~6점)
     *
     * @example
     * <pre>
     * int strength = passwordEncoderUtil.getPasswordStrength("Password123!");
     * if (strength < 3) {
     *     throw new WeakPasswordException("비밀번호가 너무 약합니다.");
     * }
     * </pre>
     */
    public int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // 각 조건 충족 시 점수 부여
        if (password.length() >= 8) score++;           // 1점: 8자 이상
        if (password.length() >= 12) score++;          // 1점: 12자 이상 (보너스)
        if (password.matches(".*[a-z].*")) score++;    // 1점: 소문자 포함
        if (password.matches(".*[A-Z].*")) score++;    // 1점: 대문자 포함
        if (password.matches(".*[0-9].*")) score++;    // 1점: 숫자 포함
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++;  // 1점: 특수문자 포함

        log.debug("비밀번호 강도 점수: {}/6", score);

        return score;
    }

    /**
     * 비밀번호 강도 평가 (문자열 반환)
     *
     * @param password 검증할 비밀번호
     * @return 강도 평가 문자열 ("약함", "중간", "강함")
     *
     * @example
     * <pre>
     * String evaluation = passwordEncoderUtil.evaluatePasswordStrength("Password123!");
     * // 결과: "강함"
     * </pre>
     */
    public String evaluatePasswordStrength(String password) {
        int strength = getPasswordStrength(password);

        if (strength <= 2) return "약함";
        if (strength <= 4) return "중간";
        return "강함";
    }

    /**
     * 비밀번호 형식 유효성 검사
     *
     * [검증 규칙]
     * - 최소 8자 이상
     * - 최대 100자 이하
     * - 영문, 숫자, 특수문자 중 2가지 이상 조합
     *
     * @param password 검증할 비밀번호
     * @return 유효성 검사 결과 (true: 유효, false: 무효)
     *
     * @example
     * <pre>
     * if (!passwordEncoderUtil.isValidPasswordFormat("12345")) {
     *     throw new InvalidPasswordException("비밀번호 형식이 올바르지 않습니다.");
     * }
     * </pre>
     */
    public boolean isValidPasswordFormat(String password) {
        if (password == null || password.length() < 8 || password.length() > 100) {
            log.debug("비밀번호 형식 검증 실패: 길이 제한 위반");
            return false;
        }

        // 영문, 숫자, 특수문자 포함 여부 확인
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        // 2가지 이상 조합 필요
        int combinationCount = (hasLetter ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);

        boolean isValid = combinationCount >= 2;

        if (!isValid) {
            log.debug("비밀번호 형식 검증 실패: 영문/숫자/특수문자 중 2가지 이상 필요");
        }

        return isValid;
    }

    /**
     * 암호화된 비밀번호 형식 검증
     *
     * BCrypt 해시 형식인지 확인
     * 형식: $2a$10$ 또는 $2b$10$ 으로 시작
     *
     * @param encodedPassword 검증할 암호화된 비밀번호
     * @return BCrypt 형식 여부
     */
    public boolean isEncodedPassword(String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }

        // BCrypt 형식: $2a$10$ 또는 $2b$10$로 시작, 총 60자
        boolean isBCryptFormat = encodedPassword.matches("^\\$2[ayb]\\$\\d{2}\\$.{53}$");

        if (!isBCryptFormat) {
            log.debug("BCrypt 형식이 아닙니다: {}", encodedPassword.substring(0, Math.min(10, encodedPassword.length())));
        }

        return isBCryptFormat;
    }
}