package com.eng.study.engstudy.config;

import com.eng.study.engstudy.interceptor.AccessLogInterceptor; // [1] 만든 인터셉터를 가져옵니다.
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final AccessLogInterceptor accessLogInterceptor;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:30080,http://nginx-service}")
    private String[] allowedOrigins;

    // =========================================================
    // 여기서 인터셉터를 등록합니다! (WebConfig 역할 대체)
    // =========================================================
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessLogInterceptor)
                .addPathPatterns("/**") // 모든 URL에 적용
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/favicon.ico", "/error"); // 정적 파일 제외
    }

    // =========================================================
    // 기존 CORS 설정 (변경 없음)
    // =========================================================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}