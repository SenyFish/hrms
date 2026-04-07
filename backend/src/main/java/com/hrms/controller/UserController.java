package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.dto.ProfileUpdateRequest;
import com.hrms.dto.UserSaveRequest;
import com.hrms.entity.User;
import com.hrms.security.LoginUser;
import com.hrms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer page,
                               @RequestParam(required = false) Integer size,
                               @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        if (page == null && size == null && (keyword == null || keyword.isBlank())) {
            return ApiResponse.ok(userService.listAll());
        }

        int pageNo = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 10;
        var result = userService.listPage(keyword, pageNo, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo);
        data.put("size", pageSize);
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<User> get(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        if (!loginUser.getUserId().equals(id) && !isAdmin(loginUser) && !isHr(loginUser)) {
            throw new AccessDeniedException("无权限");
        }
        return ApiResponse.ok(userService.get(id));
    }

    @PostMapping
    public ApiResponse<User> create(@Valid @RequestBody UserSaveRequest req,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        return ApiResponse.ok(userService.save(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<User> update(@PathVariable Long id,
                                    @Valid @RequestBody UserSaveRequest req,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        req.setId(id);
        return ApiResponse.ok(userService.save(req));
    }

    @PutMapping("/profile")
    public ApiResponse<User> updateOwnProfile(@Valid @RequestBody ProfileUpdateRequest req,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        return ApiResponse.ok(userService.updateOwnProfile(loginUser.getUserId(), req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        if (loginUser.getUserId().equals(id)) {
            throw new AccessDeniedException("不能删除当前登录账号");
        }
        User target = userService.get(id);
        if (isHr(loginUser) && target.getRole() != null && "ADMIN".equals(target.getRole().getCode())) {
            throw new AccessDeniedException("人事无权删除管理员账号");
        }
        userService.delete(id);
        return ApiResponse.ok();
    }

    private static boolean isAdmin(LoginUser u) {
        return u != null && "ADMIN".equals(u.getRoleCode());
    }

    private static boolean isHr(LoginUser u) {
        return u != null && "HR".equals(u.getRoleCode());
    }

    private static void assertAdmin(LoginUser u) {
        if (!isAdmin(u)) {
            throw new AccessDeniedException("需要管理员权限");
        }
    }

    private static void assertAdminOrHr(LoginUser u) {
        if (!isAdmin(u) && !isHr(u)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }
}
