package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.Menu;
import com.hrms.entity.Role;
import com.hrms.repository.MenuRepository;
import com.hrms.repository.RoleRepository;
import com.hrms.security.LoginUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    @GetMapping
    public ApiResponse<List<Role>> list(@AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        return ApiResponse.ok(roleRepository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id,
                                                @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        Role role = roleRepository.findById(id).orElseThrow();
        Map<String, Object> map = new HashMap<>();
        map.put("id", role.getId());
        map.put("name", role.getName());
        map.put("code", role.getCode());
        map.put("description", role.getDescription());
        map.put("menuIds", role.getMenus().stream().map(Menu::getId).collect(Collectors.toList()));
        return ApiResponse.ok(map);
    }

    @PostMapping
    public ApiResponse<Role> create(@RequestBody RoleSave body,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        Role role = new Role();
        role.setName(body.getName());
        role.setCode(body.getCode());
        role.setDescription(body.getDescription());
        if (body.getMenuIds() != null) {
            role.setMenus(loadMenus(body.getMenuIds()));
        }
        return ApiResponse.ok(roleRepository.save(role));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<Role> update(@PathVariable Long id,
                                    @RequestBody RoleSave body,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        Role role = roleRepository.findById(id).orElseThrow();
        role.setName(body.getName());
        role.setCode(body.getCode());
        role.setDescription(body.getDescription());
        if (body.getMenuIds() != null) {
            role.getMenus().clear();
            role.getMenus().addAll(loadMenus(body.getMenuIds()));
        }
        return ApiResponse.ok(roleRepository.save(role));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        roleRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private Set<Menu> loadMenus(List<Long> ids) {
        Set<Menu> set = new HashSet<>();
        for (Long menuId : ids) {
            menuRepository.findById(menuId).ifPresent(set::add);
        }
        return set;
    }

    private static boolean isAdmin(LoginUser user) {
        return user != null && "ADMIN".equals(user.getRoleCode());
    }

    private static boolean isHr(LoginUser user) {
        return user != null && "HR".equals(user.getRoleCode());
    }

    private static void assertAdmin(LoginUser user) {
        if (!isAdmin(user)) {
            throw new AccessDeniedException("需要管理员权限");
        }
    }

    private static void assertAdminOrHr(LoginUser user) {
        if (!isAdmin(user) && !isHr(user)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }

    @Data
    public static class RoleSave {
        private String name;
        private String code;
        private String description;
        private List<Long> menuIds;
    }
}
