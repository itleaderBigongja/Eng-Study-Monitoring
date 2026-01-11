package com.eng.study.engstudy.controller;

import com.eng.study.engstudy.model.dto.request.LoginRequestDTO;
import com.eng.study.engstudy.model.dto.request.RegisterRequestDTO;
import com.eng.study.engstudy.model.dto.response.AuthResponseDTO;
import com.eng.study.engstudy.service.AuthService;
import com.eng.study.engstudy.util.CookieUtil;
import com.eng.study.engstudy.util.JwtUtil;
import com.eng.study.engstudy.util.SecurityEventLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 인증 관련 REST API Controller (보안 로깅 적용됨)
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;

    /** 회원가입 */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequestDTO registerRequestDTO,
            HttpServletRequest request, // ✅ IP 로깅을 위해 추가
            HttpServletResponse response
    ) {
        String clientIp = getClientIp(request);
        try {
            log.info("회원가입 요청: loginId = {}", registerRequestDTO.getLoginId());

            AuthResponseDTO authResponseDTO = authService.register(registerRequestDTO);

            cookieUtil.addAccessTokenCookie(response, authResponseDTO.getAccessToken(), jwtUtil.getAccessTokenExpiration());
            cookieUtil.addRefreshTokenCookie(response, authResponseDTO.getRefreshToken(), jwtUtil.getRefreshTokenExpiration());

            authResponseDTO.setAccessToken(null);
            authResponseDTO.setRefreshToken(null);

            // ✅ [Security Log] 회원가입 성공
            SecurityEventLogger.logSecurityEvent(
                    String.format("New user registered: %s", registerRequestDTO.getLoginId()),
                    "account_created",
                    false,
                    clientIp,
                    "low"
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "회원가입이 완료되었습니다.");
            result.put("data", authResponseDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (IllegalArgumentException e) {
            log.error("회원가입 실패 (입력값 오류): {}", e.getMessage());

            // ✅ [Security Log] 회원가입 실패 (입력값 오류 등)
            SecurityEventLogger.logSecurityEvent(
                    String.format("Registration failed for: %s - %s", registerRequestDTO.getLoginId(), e.getMessage()),
                    "registration_failure",
                    true,
                    clientIp,
                    "low"
            );

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) { // 👈 이 부분을 추가하세요!
            log.error("회원가입 중 시스템 오류 발생", e); // 👈 여기서 메시지를 주면 null이 안 뜹니다.

            // 보안 로그
            SecurityEventLogger.logSecurityEvent(
                    "System error during registration", "registration_error", true, clientIp, "high"
            );

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "회원가입 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 로그인 */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequestDTO loginRequestDTO,
            HttpServletRequest request, // ✅ IP 로깅을 위해 추가
            HttpServletResponse response
    ) {
        String clientIp = getClientIp(request);
        try {
            log.info("로그인 요청: loginId = {}", loginRequestDTO.getLoginId());

            AuthResponseDTO authResponseDTO = authService.login(loginRequestDTO);

            cookieUtil.addAccessTokenCookie(response, authResponseDTO.getAccessToken(), jwtUtil.getAccessTokenExpiration());
            cookieUtil.addRefreshTokenCookie(response, authResponseDTO.getRefreshToken(), jwtUtil.getRefreshTokenExpiration());

            authResponseDTO.setAccessToken(null);
            authResponseDTO.setRefreshToken(null);

            // ✅ [Security Log] 로그인 성공
            SecurityEventLogger.logSecurityEvent(
                    String.format("Login successful for user: %s", loginRequestDTO.getLoginId()),
                    "login_success",
                    false,
                    clientIp,
                    "low"
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "로그인에 성공했습니다.");
            result.put("data", authResponseDTO);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("로그인 실패: {}", e.getMessage());

            // ✅ [Security Log] 로그인 실패 (비밀번호 틀림, 존재하지 않는 ID 등)
            // attemptCount는 메모리나 DB에서 관리하지 않는 경우 기본값 1 또는 -1로 설정
            SecurityEventLogger.logLoginFailure(loginRequestDTO.getLoginId(), clientIp, 1);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "아이디 또는 비밀번호가 올바르지 않습니다."); // 보안상 모호한 메시지 권장
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse); // 500 -> 401 변경 권장
        } catch (Exception e) {
            log.error("로그인 중 시스템 오류 발생", e);

            // ✅ [Security Log] 시스템 오류로 인한 로그인 실패
            SecurityEventLogger.logSecurityEvent(
                    "System error during login process",
                    "login_error",
                    true,
                    clientIp,
                    "high"
            );

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "로그인 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /** 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        String clientIp = getClientIp(request);
        try {
            log.info("로그아웃 요청");
            cookieUtil.deleteAllAuthCookies(response);

            // ✅ [Security Log] 로그아웃
            SecurityEventLogger.logSecurityEvent(
                    "User logged out",
                    "logout",
                    false,
                    clientIp,
                    "low"
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "로그아웃되었습니다.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** 토큰 갱신 */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshTokenRenewal(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String clientIp = getClientIp(request);
        try {
            log.info("토큰 갱신 요청");

            String refreshToken = cookieUtil.getRefreshToken(request)
                    .orElseThrow(() -> new IllegalArgumentException("Refresh Token이 없습니다."));

            AuthResponseDTO authResponseDTO = authService.refreshTokenRenewal(refreshToken);

            cookieUtil.addAccessTokenCookie(response, authResponseDTO.getAccessToken(), jwtUtil.getAccessTokenExpiration());
            cookieUtil.addRefreshTokenCookie(response, authResponseDTO.getRefreshToken(), jwtUtil.getRefreshTokenExpiration());

            authResponseDTO.setAccessToken(null);
            authResponseDTO.setRefreshToken(null);

            // ✅ [Security Log] 토큰 갱신 성공
            SecurityEventLogger.logSecurityEvent(
                    "Access token refreshed",
                    "token_refresh",
                    false,
                    clientIp,
                    "low"
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "토큰이 갱신되었습니다.");
            result.put("data", authResponseDTO);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("토큰 갱신 실패 (유효하지 않은 토큰): {}", e.getMessage());

            // ✅ [Security Log] 토큰 갱신 실패 (잠재적 공격 시도 가능성)
            SecurityEventLogger.logSecurityEvent(
                    "Token renewal failed: Invalid token",
                    "token_theft_attempt", // 공격 유형으로 간주
                    true,
                    clientIp,
                    "high"
            );

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** 내 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyInfo(HttpServletRequest request) {
        String clientIp = getClientIp(request);

        try {
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
            // ✅ [Security Log] 권한 없는 접근 시도
            SecurityEventLogger.logAccessDenied(
                    "anonymous",
                    "/api/auth/me",
                    clientIp
            );

            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ... (check-loginId, check-email 메서드는 중요도가 낮아 로그 생략 가능, 필요시 추가)

    /**
     * 클라이언트 IP 추출 유틸리티 메서드
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}