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
        assertAdminOrHr(loginUser);
        return ApiResponse.ok(departmentRepository.findAll());
    }

    @PostMapping
    public ApiResponse<Department> create(@RequestBody Department d, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        return ApiResponse.ok(departmentRepository.save(d));
    }

    @PutMapping("/{id}")
    public ApiResponse<Department> update(@PathVariable Long id, @RequestBody Department d,
                                        @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        d.setId(id);
        return ApiResponse.ok(departmentRepository.save(d));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        departmentRepository.deleteById(id);
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
