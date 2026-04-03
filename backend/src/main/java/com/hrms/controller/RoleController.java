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
        assertAdmin(loginUser);
        return ApiResponse.ok(roleRepository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        Role r = roleRepository.findById(id).orElseThrow();
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("name", r.getName());
        map.put("code", r.getCode());
        map.put("description", r.getDescription());
        map.put("menuIds", r.getMenus().stream().map(Menu::getId).collect(Collectors.toList()));
        return ApiResponse.ok(map);
    }

    @PostMapping
    public ApiResponse<Role> create(@RequestBody RoleSave body, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        Role r = new Role();
        r.setName(body.getName());
        r.setCode(body.getCode());
        r.setDescription(body.getDescription());
        if (body.getMenuIds() != null) {
            r.setMenus(loadMenus(body.getMenuIds()));
        }
        return ApiResponse.ok(roleRepository.save(r));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<Role> update(@PathVariable Long id, @RequestBody RoleSave body,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        Role r = roleRepository.findById(id).orElseThrow();
        r.setName(body.getName());
        r.setCode(body.getCode());
        r.setDescription(body.getDescription());
        if (body.getMenuIds() != null) {
            r.getMenus().clear();
            r.getMenus().addAll(loadMenus(body.getMenuIds()));
        }
        return ApiResponse.ok(roleRepository.save(r));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        roleRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private Set<Menu> loadMenus(List<Long> ids) {
        Set<Menu> set = new HashSet<>();
        for (Long mid : ids) {
            menuRepository.findById(mid).ifPresent(set::add);
        }
        return set;
    }

    private static void assertAdmin(LoginUser u) {
        if (u == null || !"ADMIN".equals(u.getRoleCode())) {
            throw new AccessDeniedException("需要管理员权限");
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
