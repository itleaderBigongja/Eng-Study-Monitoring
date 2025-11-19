package com.eng.study.engstudy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // 개발 중 CSRF 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/test/**", "/actuator/**").permitAll()  // 테스트 API 허용
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}