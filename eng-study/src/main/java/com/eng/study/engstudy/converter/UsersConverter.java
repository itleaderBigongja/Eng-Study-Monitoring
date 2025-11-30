package com.eng.study.engstudy.converter;

import com.eng.study.engstudy.model.dto.request.RegisterRequestDTO;
import com.eng.study.engstudy.model.dto.response.AuthResponseDTO;
import com.eng.study.engstudy.model.vo.UsersVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// RegisterRequestDTO   -> UsersVO 변환( 회원가입 요청 )
// UsersVO              -> AuthResponseDTO 변환 ( 로그인/회원가입 응답용 )
// UsersVO              -> UserInfo 변환( 사용자 정보 조회용 )
@Component
public class UsersConverter {

    /**
     * RegisterRequestDTO -> UsersVO 변환
     **/
    public UsersVO toVO(RegisterRequestDTO registerRequestDTO, String encodedPassword) {

        UsersVO usersVO = new UsersVO();
        usersVO.setLoginId(registerRequestDTO.getLoginId());            // 로그인 ID
        usersVO.setEmail(registerRequestDTO.getEmail());                // 이메일
        usersVO.setPassword(encodedPassword);                           // 암호화된 비밀번호
        usersVO.setFullName(registerRequestDTO.getFullName());          // 사용자 실명

        // 주소 정보
        usersVO.setPostalCode(registerRequestDTO.getPostalCode());      // 우편번호
        usersVO.setAddress(registerRequestDTO.getAddress());            // 주소
        usersVO.setAddressDetail(registerRequestDTO.getAddressDetail());// 상세주소
        usersVO.setAddressType(registerRequestDTO.getAddressType());    // 주소유형
        usersVO.setSido(registerRequestDTO.getSido());                  // 시/도
        usersVO.setSigungu(registerRequestDTO.getSigugun());            // 시/군/구
        usersVO.setBname(registerRequestDTO.getBname());                // 동/리(ex : 영등포본동/여의도동)

        // 기본값 설정
        usersVO.setIsActive(true);                                      // 활성화 여부
        usersVO.setRole("USER");                                        // 유형
        usersVO.setCreatedAt(LocalDateTime.now());                      // 생성일시
        usersVO.setCreatedId(registerRequestDTO.getLoginId());          // 생성자 ID

        return usersVO;
    }

    /**
     * UsersVO -> AuthResponseDTO 변환 ( 로그인/회원가입 응답용 )
     **/
    public AuthResponseDTO toAuthResponseDTO(UsersVO usersVO, String accessToken, String refreshToken) {

        AuthResponseDTO authResponseDTO = new AuthResponseDTO();

        authResponseDTO.setUsersId(usersVO.getUsersId());
        authResponseDTO.setLoginId(usersVO.getLoginId());
        authResponseDTO.setEmail(usersVO.getEmail());
        authResponseDTO.setFullName(usersVO.getFullName());
        authResponseDTO.setRole(usersVO.getRole());
        authResponseDTO.setAccessToken(accessToken);
        authResponseDTO.setRefreshToken(refreshToken);
        return authResponseDTO;
    }

    /**
     * UsersVO -> UserInfo 변환( 사용자 정보 조회용 )
     * 주소 정보 포함, 토큰 제외
     **/
    public AuthResponseDTO.UserInfo toUserInfoDTO(UsersVO usersVO) {
        AuthResponseDTO.UserInfo usersInfoDTO = new AuthResponseDTO.UserInfo();

        // 기본 정보
        usersInfoDTO.setUsersId(usersVO.getUsersId());
        usersInfoDTO.setLoginId(usersVO.getLoginId());
        usersInfoDTO.setEmail(usersVO.getEmail());
        usersInfoDTO.setFullName(usersVO.getFullName());

        // 주소 정보
        usersInfoDTO.setPostalCode(usersVO.getPostalCode());
        usersInfoDTO.setAddress(usersVO.getAddress());
        usersInfoDTO.setAddressDetail(usersVO.getAddressDetail());
        usersInfoDTO.setSido(usersVO.getSido());
        usersInfoDTO.setSigugun(usersVO.getSigungu());
        usersInfoDTO.setBname(usersVO.getBname());

        // 계정 정보
        usersInfoDTO.setRole(usersVO.getRole());
        usersInfoDTO.setLastLogin(usersVO.getLastLogin());
        usersInfoDTO.setCreatedAt(usersVO.getCreatedAt());

        return usersInfoDTO;
    }
}
