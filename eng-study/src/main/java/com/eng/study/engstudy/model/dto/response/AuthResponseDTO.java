package com.eng.study.engstudy.model.dto.response;

import com.eng.study.engstudy.model.vo.SystemVO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 인증 관련 응답 DTO
 *
 * 로그인, 회원가입, 토큰 갱신 시 사용
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder  // ⚠️ 이 어노테이션이 필수!
public class AuthResponseDTO {

    // 로그인/회원가입 시 반환되는 기본 정보
    private Long usersId;
    private String loginId;
    private String email;
    private String fullName;
    private String role;
    private String accessToken;
    private String refreshToken;

    // UserInfo 객체로 사용자 정보 전달
    private UserInfo user;

    /**
     * 내부 클래스: 사용자 상세 정보
     * 마이페이지, 프로필 조회용
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder  // ⚠️ 이 어노테이션이 필수!
    public static class UserInfo {

        private Long usersId;
        private String loginId;
        private String email;
        private String fullName;  // 추가 필드

        // 주소 정보
        private String postalCode;
        private String address;
        private String addressDetail;
        private String sido;
        private String sigugun;
        private String bname;

        // 계정 정보
        private String role;
        private LocalDateTime lastLogin;

        // SystemVO 필드들 (필요시)
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}