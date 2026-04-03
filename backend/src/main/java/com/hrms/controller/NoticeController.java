package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.SysNotice;
import com.hrms.repository.SysNoticeRepository;
import com.hrms.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final SysNoticeRepository noticeRepository;

    @GetMapping
    public ApiResponse<List<SysNotice>> list() {
        return ApiResponse.ok(noticeRepository.findAll());
    }

    @PostMapping
    public ApiResponse<SysNotice> create(@RequestBody SysNotice n, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        return ApiResponse.ok(noticeRepository.save(n));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysNotice> update(@PathVariable Long id, @RequestBody SysNotice n,
                                         @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        n.setId(id);
        return ApiResponse.ok(noticeRepository.save(n));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        noticeRepository.deleteById(id);
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
