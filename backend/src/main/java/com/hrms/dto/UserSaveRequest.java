package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserSaveRequest {
    private Long id;

    @NotBlank(message = "\u7528\u6237\u540d\u4e0d\u80fd\u4e3a\u7a7a")
    private String username;

    /** 新建时必填，更新时留空表示不修改 */
    private String password;

    @NotBlank(message = "\u59d3\u540d\u4e0d\u80fd\u4e3a\u7a7a")
    private String realName;

    private String employeeNo;
    private String positionName;
    private Long departmentId;
    private Long roleId;
    private String phone;
    private String email;
    private String birthday;
    private String hireDate;
    private Integer status;
}
