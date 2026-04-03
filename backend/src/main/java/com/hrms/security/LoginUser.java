package com.hrms.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {
    private Long userId;
    private String username;
    private String roleCode;
}
