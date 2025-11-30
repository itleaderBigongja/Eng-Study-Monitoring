package com.eng.study.engstudy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 설정
 *
 * 주요 기능:
 * - CROS 설정(CorsConfig에서 주입)
 * - JWT 기반 인증(Stateless)
 * - URL별 접근 권한 관리
 * - BCrypt 비밀번호 암호화
 *
 * 보안 설정:
 * - CSRF: 비활성화( JWT 사용으로 불필요 )
 * - Session: 사용 안 함(Stateless)
 * - Password: BCrypt(Strength 10)
 *
 * 연동 정보:
 * CORS(Cross-Origin Resource Sharing) 설정
 * Next.js 프론트엔드와 Spring Boot 백엔드 간 통신 허용
 **/
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * CorsConfig에서 정의한 CORS 설정 주입
     * - 모든 Controller에 공통 적용
     * - @CrossOrigin 어노테이션 불필요
     */
    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Security Filter Chain 설정
     * - HTTP 요청에 대한 보안 규칙 정의
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // ========================================
                // CORS 설정 (CorsConfig에서 정의된 설정 적용)
                // ========================================
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ========================================
                // CSRF 비활성화
                // - JWT 사용으로 CSRF 토큰 불필요
                // - Stateless 방식에서는 CSRF 공격 위험 낮음
                // ========================================
                .csrf(AbstractHttpConfigurer::disable)

                // ========================================
                // 세션 관리 정책: STATELESS
                // - 서버에 세션 저장 안 함
                // - JWT 토큰으로만 인증
                // ========================================
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ========================================
                // URL별 접근 권한 설정
                // ========================================
                .authorizeHttpRequests(auth ->
                        // 인증 없이 접근 가능한 경로
                        auth.requestMatchers(
                                        "/api/auth/**",         // 회원가입, 로그인, 토큰 갱신
                                        // 추후 메인화면도 추가해야 함
                                        "/api/test/**",         // 테스트 API
                                        "/actuator/**",         // Spring Actuator(모니터링)
                                        "/error"                // 에러 페이지
                                ).permitAll()

                                // 나머지 모든 요청은 인증 필요
                                .anyRequest().authenticated()
                )

                // ========================================
                // HTTP Basic 인증 비활성화
                // - JWT 토큰 방식 사용
                // ========================================
                .httpBasic(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 암호화(String Security 권장)
        // strength: 암호화 강도(기본값 10, 범위 4~31)
        return new BCryptPasswordEncoder(10);
    }
}