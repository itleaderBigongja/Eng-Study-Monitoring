package com.eng.study.engstudy.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration:3600000}") long accessTokenExpiration,  // 기본 1시간
            @Value("${jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration  // 기본 7일
    ) {
        // JWT 서명에 사용할 키 생성
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /** Access Token 생성 */
    public String generateAccessToken(Long usersId, String loginId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("usersId", usersId);
        claims.put("loginId", loginId);
        claims.put("role", role);
        claims.put("type", "access");

        return createToken(claims, loginId, accessTokenExpiration);
    }

    /** Refresh Token 생성 */
    public String generateRefreshToken(Long usersId, String loginId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("usersId", usersId);
        claims.put("loginId", loginId);
        claims.put("type", "refresh");

        return createToken(claims, loginId, refreshTokenExpiration);
    }

    /** JWT 토큰 생성 로직 */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 토큰에서 사용자 ID 추출 */
    public Long getUsersId(String token) {
        Claims claims = parseClaims(token);
        return claims.get("usersId", Long.class);
    }

    /** 토큰에서 로그인 ID 추출 */
    public String getLoginId(String token) {
        return parseClaims(token).getSubject();
    }

    /** 토큰에서 역할(Role) 추출 */
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /** 토큰 타입 확인 (access / refresh) */
    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    /** 토큰 유효성 검증 */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다: {}", e.getMessage());
        }
        return false;
    }

    /** 토큰이 만료되었는지 확인 */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /** 토큰 파싱 */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 토큰 만료 시간 반환 (밀리초) */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}