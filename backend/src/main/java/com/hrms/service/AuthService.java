package com.hrms.service;

import com.hrms.dto.LoginRequest;
import com.hrms.entity.User;
import com.hrms.repository.UserRepository;
import com.hrms.security.JwtUtil;
import com.hrms.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已禁用");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String roleCode = user.getRole() != null ? user.getRole().getCode() : "";
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), roleCode);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("employeeNo", user.getEmployeeNo());
        profile.put("positionName", user.getPositionName());
        profile.put("departmentId", user.getDepartmentId());
        profile.put("phone", user.getPhone());
        profile.put("email", user.getEmail());
        profile.put("roleId", user.getRole() != null ? user.getRole().getId() : null);
        profile.put("roleName", user.getRole() != null ? user.getRole().getName() : null);
        profile.put("roleCode", roleCode);

        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("user", profile);
        return res;
    }

    public LoginUser require(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        String roleCode = user.getRole() != null ? user.getRole().getCode() : "";
        return new LoginUser(user.getId(), user.getUsername(), roleCode);
    }
}
