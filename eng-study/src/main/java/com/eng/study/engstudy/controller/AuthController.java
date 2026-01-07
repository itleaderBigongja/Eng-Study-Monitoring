package com.eng.study.engstudy.controller;

import com.eng.study.engstudy.model.dto.request.LoginRequestDTO;
import com.eng.study.engstudy.model.dto.request.RegisterRequestDTO;
import com.eng.study.engstudy.model.dto.response.AuthResponseDTO;
import com.eng.study.engstudy.service.AuthService;
import com.eng.study.engstudy.util.CookieUtil;
import com.eng.study.engstudy.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;

import java.util.HashMap;
import java.util.Map;

/**
 * 인증 관련 REST API Controller
 *
 * 주요 기능
 * - 회원가입(  POST : /api/auth/register )
 * - 로그인(   POST /api/auth/login )
 * - 로그아웃(  POST :/api/auth/logout )
 * - 토큰 갱신( POST : /api/auth/refresh )
 * - 내정보 조회( GET : /api/auth/check-* )
 *
 * - 보안
 * - HttpOnly Cookie로 토큰 전달(XSS 방어)
 **/

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;
    private final View error;

    /** 회원가입 */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid
            @RequestBody RegisterRequestDTO registerRequestDTO,
            HttpServletResponse response
            ) {
        try {
            log.info("회원가입 요청: loginId = {}", registerRequestDTO.getLoginId());

            AuthResponseDTO authResponseDTO = authService.register(registerRequestDTO);

            // HttpOnly 쿠키에 토큰 저장
            cookieUtil.addAccessTokenCookie(
                    response,
                    authResponseDTO.getAccessToken(),
                    jwtUtil.getAccessTokenExpiration()      // 접근 토큰 만료시간
            );
            cookieUtil.addRefreshTokenCookie(
                    response,
                    authResponseDTO.getRefreshToken(),
                    jwtUtil.getRefreshTokenExpiration()     // 리프레시 토큰 만료시간
            );

            // 응답에서 토큰 제거(쿠키로만 전달)
            authResponseDTO.setAccessToken(null);
            authResponseDTO.setRefreshToken(null);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "회원가입이 완료되었습니다.");
            result.put("data", authResponseDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }catch (IllegalArgumentException e) {
            log.error("회원가입 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /** 로그인 */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid
            @RequestBody LoginRequestDTO loginRequestDTO,
            HttpServletResponse response
            ) {
        try {
            log.info("로그인 요청: loginId = {}", loginRequestDTO.getLoginId());

            AuthResponseDTO authResponseDTO = authService.login(loginRequestDTO);

            // HttpOnly 쿠키에 토큰 저장
            cookieUtil.addAccessTokenCookie(
                    response,
                    authResponseDTO.getAccessToken(),
                    jwtUtil.getAccessTokenExpiration()
            );
            cookieUtil.addRefreshTokenCookie(
                    response,
                    authResponseDTO.getRefreshToken(),
                    jwtUtil.getRefreshTokenExpiration()
            );

            // 응답에서 토큰 제거( 쿠키로만 전달 )
            authResponseDTO.setAccessToken(null);
            authResponseDTO.setRefreshToken(null);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "로그인에 성공했습니다.");
            result.put("data", authResponseDTO);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("로그인 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "로그인 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            log.error("로그인 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "로그인 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {

        try {
            log.info("로그아웃 요청");

            // 쿠키 삭제
            cookieUtil.deleteAllAuthCookies(response);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "로그아웃되었습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 토큰 갱신 */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshTokenRenewal(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            log.info("토큰 갱신 요청");

            // Refresh Token 추출
            String refreshToken = cookieUtil.getRefreshToken(request)
                    .orElseThrow(() -> new IllegalArgumentException("Refresh Token이 없습니다."));

            AuthResponseDTO authResponseDTO = authService.refreshTokenRenewal(refreshToken);

            // 새로운 토큰을 쿠키에 저장
            cookieUtil.addAccessTokenCookie(
                    response,
                    authResponseDTO.getAccessToken(),
                    jwtUtil.getAccessTokenExpiration()
            );
            cookieUtil.addRefreshTokenCookie(
                    response,
                    authResponseDTO.getRefreshToken(),
                    jwtUtil.getRefreshTokenExpiration()
            );

            // 응답에서 토큰 제거
            authResponseDTO.setAccessToken(null);
            authResponseDTO.setRefreshToken(null);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "토큰이 갱신되었습니다.");
            result.put("data", authResponseDTO);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("토큰 갱신 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyInfo(HttpServletRequest request) {

        try {
            // Access Token에서 사용자 ID 추출
            String accessToken = cookieUtil.getAccessToken(request)
                    .orElseThrow(() -> new IllegalArgumentException("인증 토큰이 없습니다."));

            if (!jwtUtil.validateToken(accessToken)) {
                throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
            }

            Long usersId = jwtUtil.getUsersId(accessToken);
            AuthResponseDTO.UserInfo userInfo = authService.getMyInfo(usersId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", userInfo);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 로그인 ID 중복 확인 */
    @GetMapping("/check-loginId")
    public ResponseEntity<Map<String, Object>> checkLoginId(@RequestParam String loginId) {

        try {
            boolean available = authService.checkLoginIdAvailable(loginId);
            Map<String, Object> result = new HashMap<>();

            result.put("success", true);
            result.put("available", available);
            result.put("message", available ? "사용 가능한 아이디입니다." : "이미 사용중인 아이디입니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("로그인 ID 중복 확인 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "중복 확인 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 이메일 중복 확인 */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestBody String email) {

        try {
            boolean available = authService.checkEmailAvailable(email);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("available", available);
            result.put("message", available ? "사용 가능한 이메일입니다." : "이미 사용중인 이메일입니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("이메일 중복 확인 중 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "중복 확인 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
