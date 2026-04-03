package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.Menu;
import com.hrms.repository.MenuRepository;
import com.hrms.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuRepository menuRepository;

    @GetMapping
    public ApiResponse<List<Menu>> list(@AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        return ApiResponse.ok(menuRepository.findAllByOrderBySortOrderAsc());
    }

    @PostMapping
    public ApiResponse<Menu> create(@RequestBody Menu m, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        return ApiResponse.ok(menuRepository.save(m));
    }

    @PutMapping("/{id}")
    public ApiResponse<Menu> update(@PathVariable Long id, @RequestBody Menu m,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        m.setId(id);
        return ApiResponse.ok(menuRepository.save(m));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        menuRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private static void assertAdmin(LoginUser u) {
        if (u == null || !"ADMIN".equals(u.getRoleCode())) {
            throw new AccessDeniedException("需要管理员权限");
        }
    }
}
