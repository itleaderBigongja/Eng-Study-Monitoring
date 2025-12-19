package com.study.monitoring.studymonitoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS(Cross-Origin Resource Sharing) 설정
 *
 * 역할:
 * - 프론트엔드(Next.js)와의 CORS 정책 관리
 * - HttpOnly Cookie 전송 허용
 * - 허용된 Origin, Method, Header 설정
 *
 * 참고:
 * - eng-study 프로젝트의 CorsConfig.java와 동일한 구조
 * - 중앙화된 CORS 관리로 @CrossOrigin 어노테이션 불필요
 */
@Configuration
public class CorsConfig {

    /**
     * CORS 필터 Bean 생성
     *
     * 설정:
     * - 허용 Origin: Next.js 개발 서버, Kubernetes 서비스
     * - 허용 Method: GET, POST, PUT, DELETE, OPTIONS
     * - Credentials: true (Cookie 전송 허용)
     *
     * @return CorsFilter 인스턴스
     */
    @Bean
    public CorsFilter corsFilter() {
        // 1. CORS 설정 객체 생성
        CorsConfiguration configuration = new CorsConfiguration();

        // 2. 허용할 Origin 설정
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3001",        // Next.js 로컬 개발 서버
                "http://localhost:30080",       // Kubernetes NodePort
                "http://nginx-service",         // Kubernetes 내부 통신
                "http://eng-study-frontend-service",  // 영어학습 프론트엔드
                "http://monitoring-frontend-service"  // 모니터링 프론트엔드
        ));

        // 3. 허용할 HTTP 메서드 설정
        configuration.setAllowedMethods(Arrays.asList(
                "GET",      // 조회
                "POST",     // 생성
                "PUT",      // 수정
                "DELETE",   // 삭제
                "OPTIONS"   // Preflight 요청
        ));

        // 4. 허용할 헤더 설정
        configuration.setAllowedHeaders(Arrays.asList(
                "Content-Type",         // JSON 요청
                "Authorization",        // 인증 토큰 (향후 사용 가능)
                "X-Requested-With",     // AJAX 요청 식별
                "Accept",               // 응답 형식
                "Origin"                // 요청 출처
        ));

        // 5. 노출할 헤더 설정 (응답 헤더 중 프론트엔드에서 접근 가능한 헤더)
        configuration.setExposedHeaders(Arrays.asList(
                "Content-Type",
                "Content-Length"
        ));

        // 6. ✅ 중요: Credentials 허용 (HttpOnly Cookie 전송)
        configuration.setAllowCredentials(true);

        // 7. Preflight 요청 캐시 시간 (초 단위)
        configuration.setMaxAge(3600L);  // 1시간

        // 8. URL 패턴별 CORS 설정 소스 생성
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);  // /api/** 경로에 적용

        // 9. CorsFilter 생성 및 반환
        return new CorsFilter(source);
    }
}