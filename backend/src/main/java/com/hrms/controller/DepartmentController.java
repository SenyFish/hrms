package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.Department;
import com.hrms.repository.DepartmentRepository;
import com.hrms.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping
    public ApiResponse<List<Department>> list(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        return ApiResponse.ok(departmentRepository.findAll());
    }

    @PostMapping
    public ApiResponse<Department> create(@RequestBody Department department,
                                          @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        return ApiResponse.ok(departmentRepository.save(department));
    }

    @PutMapping("/{id}")
    public ApiResponse<Department> update(@PathVariable Long id,
                                          @RequestBody Department department,
                                          @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        department.setId(id);
        return ApiResponse.ok(departmentRepository.save(department));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        departmentRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private static boolean isAdmin(LoginUser user) {
        return user != null && "ADMIN".equals(user.getRoleCode());
    }

    private static boolean isHr(LoginUser user) {
        return user != null && "HR".equals(user.getRoleCode());
    }

    private static void assertAdminOrHr(LoginUser user) {
        if (!isAdmin(user) && !isHr(user)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }
}
