package com.eng.study.engstudy.model.dto.request;

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

    @NotBlank(message = "로그인 ID는 필수 입니다.")
    @Size(min = 3, max = 50, message = "로그인 ID는 3~50자 사이어야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "로그인 ID는 영문, 숫자, _, - 만 사용 가능합니다")
    private String loginId;

    @NotBlank(message = "이메일은 필수 입니다")
    @Email(message = "유효한 이메일 주소를 입력해주세요")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다"
    )
    private String password;

    @NotBlank(message = "이름은 필수 입니다")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다")
    private String fullName;

    // 주소 정보
    private String postalCode;
    private String address;
    private String addressDetail;
    private String addressType;
    private String sido;
    private String sigugun;
    private String bname;
}
