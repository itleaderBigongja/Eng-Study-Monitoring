package com.eng.study.engstudy.model.vo;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Users 테이블 VO (Value Object)
 *
 * SystemVO를 상속받아 공통 필드 사용
 * - createdAt, createdId, updatedAt, updatedId
 */
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class UsersVO extends SystemVO {

    private Long usersId;                           // 사용자 ID (Primary Key, Auto Increment)
    private String loginId;                         // 로그인 ID (Unique, Not Null)
    private String email;                           // 이메일 (Unique, Not Null)
    private String password;                        // 비밀번호 (BCrypt 암호화, Not Null)
    private String fullName;                        // 전체 이름 (Not Null)
    private String postalCode;                      // 우편번호 (Optional)
    private String address;                         // 기본 주소 (Optional)
    private String addressDetail;                   // 상세 주소 (Optional)
    private String addressType;                     // 주소 타입 (R: 도로명, J: 지번)
    private String sido;                            // 시/도 (Optional)
    private String sigungu;                         // 시/군/구 (Optional)
    private String bname;                           // 법정동/법정리 (Optional)
    private LocalDateTime lastLogin;                // 마지막 로그인 일시
    private Boolean isActive;                       // 계정 활성화 여부 (true: 활성, false: 비활성)
    private String role;                            // 사용자 역할 (USER, ADMIN 등)

    // SystemVO로부터 상속받는 필드들:
    // - createdAt: 생성 일시
    // - createdId: 생성자 ID
    // - updatedAt: 수정 일시
    // - updatedId: 수정자 ID
}