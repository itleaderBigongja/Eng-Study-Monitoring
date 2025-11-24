package com.eng.study.engstudy.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class RegisterRequestDTO {

    @NotBlank(message = "로그인 계정은 필수입니다.")
    @Size(min = 3, max = 50, message = "로그인 계정은 3-50자 사이어야 합니다.")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "로그인계정은 영문,숫자,_,-까지만 사용가능 합니다."
    )
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 6, max = 100, message = "비밀번호는 6~100자 사이여야 합니다.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "비밀번호는 대문자, 소문자, 숫자를 각각 1개 이상 포함해야 합니다"
    )
    private String password;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    private String email;

    @Size(max = 100, message = "이름은 100자 이하여야 합니다")
    @Pattern(
            regexp = "^[a-zA-Z\\s가-힣]*$",
            message = "이름은 한글, 영문, 공백만 사용 가능합니다"
    )
    private String fullName;

    public RegisterRequestDTO(String username, String password, String email, String fullName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }
}
