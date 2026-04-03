package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserSaveRequest {
    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 新建时必填，更新时留空表示不修改 */
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String employeeNo;
    private Long departmentId;
    private Long roleId;
    private String phone;
    private String email;
    private String birthday;
    private String hireDate;
    private Integer status;
}
