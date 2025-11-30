package com.eng.study.engstudy.service;

import com.eng.study.engstudy.model.dto.request.LoginRequestDTO;
import com.eng.study.engstudy.model.dto.request.RegisterRequestDTO;
import com.eng.study.engstudy.model.dto.response.AuthResponseDTO;
import org.apache.ibatis.annotations.Param;

public interface AuthService {

    /** 회원가입 기능 */
    AuthResponseDTO register(RegisterRequestDTO registerRequestDTO);

    /** 로그인 */
    AuthResponseDTO login(LoginRequestDTO loginRequestDTO);

    /** 내 정보 조회 */
    AuthResponseDTO.UserInfo getMyInfo(Long usersId);

    /** 토큰 갱신 */
    AuthResponseDTO refreshTokenRenewal(String refreshToken);

    /** 로그인 ID 중복 확인 */
    boolean checkLoginIdAvailable(@Param("loginId") String loginId);

    /** 이메일 중복 확인 */
    boolean checkEmailAvailable(@Param("email") String email);
}
