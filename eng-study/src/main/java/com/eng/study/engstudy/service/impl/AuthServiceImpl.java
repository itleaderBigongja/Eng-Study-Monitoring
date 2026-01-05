package com.eng.study.engstudy.service.impl;

import com.eng.study.engstudy.mapper.UsersMapper;
import com.eng.study.engstudy.model.dto.request.LoginRequestDTO;
import com.eng.study.engstudy.model.dto.request.RegisterRequestDTO;
import com.eng.study.engstudy.model.dto.response.AuthResponseDTO;
import com.eng.study.engstudy.model.vo.UsersVO;
import com.eng.study.engstudy.service.AuthService;
import com.eng.study.engstudy.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsersMapper usersMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 형변환 제거, org.slf4j.Logger 사용
    private static final Logger auditLogger = LoggerFactory.getLogger("com.eng.study.audit");

    /**
     * 회원가입
     */
    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO registerRequestDTO) {
        log.info("회원가입 처리 시작: loginId = {}", registerRequestDTO.getLoginId());

        // 1. 로그인 ID 중복 체크
//        if (!checkLoginIdAvailable(registerRequestDTO.getLoginId())) {
//            throw new IllegalArgumentException("이미 사용중인 로그인 ID입니다.");
//        }

        // 2. 이메일 중복 체크
        if (!checkEmailAvailable(registerRequestDTO.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }

        // 3. UsersVO 생성
        UsersVO usersVO = UsersVO.builder()
                .loginId(registerRequestDTO.getLoginId())
                .email(registerRequestDTO.getEmail())
                .fullName(registerRequestDTO.getFullName())
                .password(passwordEncoder.encode(registerRequestDTO.getPassword()))
                .postalCode(registerRequestDTO.getPostalCode())
                .address(registerRequestDTO.getAddress())
                .addressDetail(registerRequestDTO.getAddressDetail())
                .addressType(registerRequestDTO.getAddressType())
                .sido(registerRequestDTO.getSido())
                .sigungu(registerRequestDTO.getSigugun())
                .bname(registerRequestDTO.getBname())
                .isActive(true)
                .role("USER")
                .createdId(registerRequestDTO.getLoginId())
                .build();

        // 4. DB 저장 및 감사 로그
        try {
            usersMapper.insertUser(usersVO);

            // ✅ 감사 로그 구조 수정 (템플릿과 완벽히 일치)
            Map<String, Object> auditData = new HashMap<>();

            // Event 정보
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("action", "user.register");
            eventMap.put("category", "account");
            eventMap.put("result", "success");
            eventMap.put("description", "New user registered");
            auditData.put("event", eventMap);

            // User 정보
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("login_id", registerRequestDTO.getLoginId());
            userMap.put("email", registerRequestDTO.getEmail());
            userMap.put("role", "USER");
            auditData.put("user", userMap);

            // Resource 정보 (선택사항)
            Map<String, Object> resourceMap = new HashMap<>();
            resourceMap.put("type", "user");
            resourceMap.put("id", String.valueOf(usersVO.getUsersId()));
            resourceMap.put("name", registerRequestDTO.getFullName());
            auditData.put("resource", resourceMap);

            // ✅ 로그 전송 (Markers 사용)
            auditLogger.info(Markers.appendEntries(auditData), "User registration completed");

        } catch (Exception e) {
            log.error("사용자 등록 실패: {}", e.getMessage(), e);

            // ✅ 실패 시에도 감사 로그 남기기
            Map<String, Object> failAuditData = new HashMap<>();
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("action", "user.register");
            eventMap.put("category", "account");
            eventMap.put("result", "failure");
            eventMap.put("description", "User registration failed: " + e.getMessage());
            failAuditData.put("event", eventMap);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("login_id", registerRequestDTO.getLoginId());
            userMap.put("email", registerRequestDTO.getEmail());
            failAuditData.put("user", userMap);

            auditLogger.error(Markers.appendEntries(failAuditData), "User registration failed");

            throw new RuntimeException("회원가입 중 오류가 발생했습니다.");
        }

        // 5. 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(usersVO.getUsersId(), usersVO.getLoginId(), usersVO.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(usersVO.getUsersId(), usersVO.getLoginId());

        // 6. 응답 반환
        AuthResponseDTO.UserInfo userInfo = AuthResponseDTO.UserInfo.builder()
                .usersId(usersVO.getUsersId())
                .loginId(usersVO.getLoginId())
                .fullName(usersVO.getFullName())
                .email(usersVO.getEmail())
                .build();

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userInfo)
                .build();
    }

    /**
     * 로그인
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        log.info("로그인 처리 시작: loginId = {}", loginRequestDTO.getLoginId());

        // 1. 사용자 조회
        UsersVO usersVO = usersMapper.findByLoginId(loginRequestDTO.getLoginId());

        if (usersVO == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 2. 계정 활성화 여부 확인
        if (usersVO.getIsActive() == null || !usersVO.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), usersVO.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 4. JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(usersVO.getUsersId(), usersVO.getLoginId(), usersVO.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(usersVO.getUsersId(), usersVO.getLoginId());

        // 5. 응답 DTO 생성
        AuthResponseDTO.UserInfo userInfo = AuthResponseDTO.UserInfo.builder()
                .usersId(usersVO.getUsersId())
                .loginId(usersVO.getLoginId())
                .fullName(usersVO.getFullName())
                .email(usersVO.getEmail())
                .build();

        log.info("로그인 성공: usersId = {}", usersVO.getUsersId());

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userInfo)
                .build();
    }

    /**
     * 내 정보 조회
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO.UserInfo getMyInfo(Long usersId) {
        log.info("사용자 정보 조회: usersId = {}", usersId);

        UsersVO usersVO = usersMapper.findByUsersId(usersId);

        if (usersVO == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        return AuthResponseDTO.UserInfo.builder()
                .usersId(usersVO.getUsersId())
                .loginId(usersVO.getLoginId())
                .fullName(usersVO.getFullName())
                .email(usersVO.getEmail())
                .build();
    }

    /**
     * 토큰 갱신
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO refreshTokenRenewal(String refreshToken) {
        log.info("토큰 갱신 요청");

        // 1. Refresh Token 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. 사용자 ID 추출
        Long usersId = jwtUtil.getUsersId(refreshToken);
        UsersVO usersVO = usersMapper.findByUsersId(usersId);

        if (usersVO == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 3. 새 토큰 생성
        String newAccessToken = jwtUtil.generateAccessToken(usersId, usersVO.getLoginId(), usersVO.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(usersId, usersVO.getLoginId());

        // 4. 응답 DTO 생성
        AuthResponseDTO.UserInfo userInfo = AuthResponseDTO.UserInfo.builder()
                .usersId(usersVO.getUsersId())
                .loginId(usersVO.getLoginId())
                .fullName(usersVO.getFullName())
                .email(usersVO.getEmail())
                .build();

        log.info("토큰 갱신 성공: usersId = {}", usersId);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userInfo)
                .build();
    }

    /**
     * 로그인 ID 중복 확인
     * @return true: 사용 가능, false: 이미 존재
     */
    @Override
    @Transactional(readOnly = true)
    public boolean checkLoginIdAvailable(String loginId) {
        int count = usersMapper.countByLoginId(loginId);
        return count == 0;
    }

    /**
     * 이메일 중복 확인
     * @return true: 사용 가능, false: 이미 존재
     */
    @Override
    @Transactional(readOnly = true)
    public boolean checkEmailAvailable(String email) {
        int count = usersMapper.countByEmail(email);
        return count == 0;
    }
}