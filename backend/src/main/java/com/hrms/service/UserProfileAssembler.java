package com.hrms.service;

import com.hrms.entity.Menu;
import com.hrms.entity.Role;
import com.hrms.entity.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserProfileAssembler {

    public Map<String, Object> toProfile(User user, boolean includeMenus) {
        Role role = user.getRole();
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("employeeNo", user.getEmployeeNo());
        profile.put("positionName", user.getPositionName());
        profile.put("departmentId", user.getDepartmentId());
        profile.put("phone", user.getPhone());
        profile.put("email", user.getEmail());
        profile.put("birthday", user.getBirthday());
        profile.put("hireDate", user.getHireDate());
        profile.put("status", user.getStatus());
        profile.put("roleId", role != null ? role.getId() : null);
        profile.put("roleCode", role != null ? role.getCode() : null);
        profile.put("roleName", role != null ? role.getName() : null);

        if (includeMenus) {
            profile.put("menus", toMenus(role));
        }

        return profile;
    }

    private List<Map<String, Object>> toMenus(Role role) {
        if (role == null || role.getMenus() == null) {
            return List.of();
        }

        return role.getMenus().stream()
                .sorted(Comparator.comparing(Menu::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toMenu)
                .toList();
    }

    private Map<String, Object> toMenu(Menu menu) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", menu.getId());
        data.put("parentId", menu.getParentId());
        data.put("title", menu.getTitle());
        data.put("path", menu.getPath());
        data.put("icon", menu.getIcon());
        data.put("sortOrder", menu.getSortOrder());
        return data;
    }
}
