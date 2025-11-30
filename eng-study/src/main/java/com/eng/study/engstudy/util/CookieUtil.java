package com.eng.study.engstudy.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtil {

    // 쿠키 이름 상수
    public static final String ACCESS_TOKEN_NAME = "access_token";
    public static final String REFRESH_TOKEN_NAME = "refresh_token";

    /**
     * HttpOnly 쿠키 생성
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 만료 시간(초)
     * @return Cookie 객체
     */
    public Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);            // 입력 받은 쿠키 명과 토큰 값, 최대 시간을 받아 쿠키를 생성한다.
        cookie.setHttpOnly(true);                           // JavaScript 접근 불가(XSS 방어)
        cookie.setSecure(false);                            // 개발 환경: false, 운영 환경: true(HTTPS만 전송)
        cookie.setPath("/");                                // 모든 경로에서 접근 가능
        cookie.setMaxAge(maxAge);                           // 쿠키 만료 시간(초)
        cookie.setAttribute("SameSite", "Lax"); // CSRF 방어
        return cookie;
    }

    /** Access Token 쿠키 생성 */
    public void addAccessTokenCookie(HttpServletResponse response, String token, long expirationMs) {
        int maxAge = (int) (expirationMs / 1000);   // 밀리초 -> 초 변환
        Cookie cookie = createCookie(ACCESS_TOKEN_NAME, token, maxAge);
        response.addCookie(cookie);
    }

    /** Refresh Token 쿠키 생성 */
    public void addRefreshTokenCookie(HttpServletResponse response, String token, long expirationMs) {
        int maxAge = (int) (expirationMs / 1000); // 밀리초 -> 초 변환
        Cookie cookie = createCookie(REFRESH_TOKEN_NAME, token, maxAge);
        response.addCookie(cookie);
    }

    /** 요청에서 특정 쿠키 값 추출 */
    public Optional<String> getCookieValue(HttpServletRequest request, String name) {

        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /** Access Token 추출 */
    public Optional<String> getAccessToken(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_NAME);
    }

    /** Refresh Token 추출 */
    public Optional<String> getRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_NAME);
    }

    /** 쿠키 삭제 (로그아웃 시 사용) */
    public void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 즉시 만료
        response.addCookie(cookie);
    }

    /** Access Token 쿠키 삭제 */
    public void deleteAccessTokenCookie(HttpServletResponse response) {
        deleteCookie(response, ACCESS_TOKEN_NAME);
    }

    /** Refresh Token 쿠키 삭제 */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        deleteCookie(response, REFRESH_TOKEN_NAME);
    }

    /** 모든 인증 쿠키 삭제 */
    public void deleteAllAuthCookies(HttpServletResponse response) {
        deleteAccessTokenCookie(response);
        deleteRefreshTokenCookie(response);
    }
}
