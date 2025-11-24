package com.eng.study.engstudy.domain.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
public class UsersVO {
    private Long id;                        // 사용자 고유 ID
    private String username;                // 사용자 명
    private String email;                   // 이메일
    private String password;                // 암호화된 비밀번호
    private String fullName;                // 사용자 실명
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private Boolean isActive;
    private String role;

    public UsersVO(Long id, String username, String email, String password, String fullName,
                   Boolean isActive, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.isActive = isActive;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
