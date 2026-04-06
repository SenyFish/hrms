package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @NotBlank(message = "账号不能为空")
    private String username;

    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String phone;
    private String email;
    private String birthday;
}
