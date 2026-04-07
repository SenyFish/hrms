package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.dto.LoginRequest;
import com.hrms.entity.User;
import com.hrms.repository.UserRepository;
import com.hrms.security.LoginUser;
import com.hrms.service.AuthService;
import com.hrms.service.UserProfileAssembler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserProfileAssembler userProfileAssembler;

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
        return ApiResponse.ok(userProfileAssembler.toProfile(user, true));
    }
}
