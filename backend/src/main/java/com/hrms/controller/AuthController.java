package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.dto.LoginRequest;
import com.hrms.entity.Menu;
import com.hrms.entity.Role;
import com.hrms.entity.User;
import com.hrms.repository.UserRepository;
import com.hrms.security.LoginUser;
import com.hrms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return ApiResponse.fail(401, "未登录");
        }
        User user = userRepository.findById(loginUser.getUserId()).orElseThrow();
        Role role = user.getRole();
        List<Map<String, Object>> menus = role.getMenus().stream()
                .sorted(Comparator.comparing(Menu::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(this::menuToMap)
                .collect(Collectors.toList());

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("employeeNo", user.getEmployeeNo());
        profile.put("positionName", user.getPositionName());
        profile.put("departmentId", user.getDepartmentId());
        profile.put("phone", user.getPhone());
        profile.put("email", user.getEmail());
        profile.put("roleCode", role != null ? role.getCode() : null);
        profile.put("roleName", role != null ? role.getName() : null);
        profile.put("menus", menus);

        return ApiResponse.ok(profile);
    }

    private Map<String, Object> menuToMap(Menu m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("parentId", m.getParentId());
        map.put("title", m.getTitle());
        map.put("path", m.getPath());
        map.put("icon", m.getIcon());
        map.put("sortOrder", m.getSortOrder());
        return map;
    }
}
