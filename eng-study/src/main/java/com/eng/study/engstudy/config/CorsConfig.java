package com.eng.study.engstudy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS(Cross-Origin Resource Sharing) 설정
 *
 * Next.js 프론트엔드와 Spring Boot 백엔드 간 통신 허용
 *
 * 주요 설정:
 * - 허용 Origin: localhost:3000 (개발), localhost:30080 (k8s)
 * - 허용 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS
 * - Credentials: true (Cookie 전송 허용)
 * - allowedHeaders: * (모든 헤더 허용)
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:30080,http://nginx-service}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ========================================
        // 1. 허용할 Origin 설정
        // - localhost:3000: Next.js 개발 서버
        // - localhost:30080: Kubernetes NodePort
        // - nginx-service: Kubernetes 내부 통신
        // ========================================
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // ========================================
        // 2. 허용할 HTTP 메서드
        // ========================================
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // ========================================
        // 3. 허용할 헤더
        // "*" 사용하여 모든 헤더 허용
        // ========================================
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // ========================================
        // 4. 인증 정보 포함 허용 (Cookie 전송)
        // ⚠️ 이 설정이 없으면 HttpOnly Cookie가 전송되지 않음!
        // ========================================
        configuration.setAllowCredentials(true);

        // ========================================
        // 5. 노출할 헤더 (프론트엔드에서 접근 가능한 헤더)
        // ========================================
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Set-Cookie",
                "Content-Type"
        ));

        // ========================================
        // 6. Preflight 요청 캐시 시간 (초)
        // OPTIONS 요청 결과를 1시간 동안 캐시
        // ========================================
        configuration.setMaxAge(3600L);

        // ========================================
        // 7. 모든 경로에 CORS 설정 적용
        // ========================================
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}