package com.eng.study.engstudy.mapper;

import com.eng.study.engstudy.model.vo.UsersVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UsersMapper {

    // 회원가입
    int insertUser(UsersVO usersVO);

    // 로그인 ID로 사용자 정보 전체 조회
    UsersVO findByLoginId(@Param("loginId") String loginId);

    // 로그인 ID 중복 확인
    int countByLoginId(@Param("loginId") String loginId);

    // 이메일로 조회
    UsersVO findByEmail(@Param("email") String email);

    // 이메일 중복 확인
    int countByEmail(@Param("email") String email);

    // 유저 ID로 사용자 정보 전체 조회
    UsersVO findByUsersId(@Param("usersId") Long usersId);

    // 마지막 로그인 시간 업데이트
    int updateLastLogin(@Param("usersId") Long usersId);

    // 사용자 정보 수정
    int updateUser(UsersVO user);
}
