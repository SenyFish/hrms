package com.hrms.config;

import com.hrms.entity.Menu;
import com.hrms.entity.Role;
import com.hrms.repository.MenuRepository;
import com.hrms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class MenuBootstrapSupport {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;

    public Menu ensureMenu(String title, String path, String icon, int sortOrder, Long parentId) {
        return menuRepository.findByPath(path)
                .map(menu -> updateMenu(menu, title, icon, sortOrder, parentId))
                .orElseGet(() -> createMenu(title, path, icon, sortOrder, parentId));
    }

    public void attachMenus(String roleCode, Menu... menus) {
        roleRepository.findByCode(roleCode).ifPresent(role -> {
            if (role.getMenus() == null) {
                role.setMenus(new HashSet<>());
            }

            boolean changed = false;
            for (Menu menu : menus) {
                if (role.getMenus().stream().noneMatch(item -> item.getId().equals(menu.getId()))) {
                    role.getMenus().add(menu);
                    changed = true;
                }
            }

            saveRoleIfChanged(role, changed);
        });
    }

    public void detachMenu(String roleCode, Menu menu) {
        roleRepository.findByCode(roleCode).ifPresent(role -> {
            if (role.getMenus() == null) {
                return;
            }

            boolean changed = role.getMenus().removeIf(item -> item.getId().equals(menu.getId()));
            saveRoleIfChanged(role, changed);
        });
    }

    private Menu updateMenu(Menu menu, String title, String icon, int sortOrder, Long parentId) {
        menu.setTitle(title);
        menu.setIcon(icon);
        menu.setSortOrder(sortOrder);
        menu.setParentId(parentId);
        return menuRepository.save(menu);
    }

    private Menu createMenu(String title, String path, String icon, int sortOrder, Long parentId) {
        Menu menu = new Menu();
        menu.setTitle(title);
        menu.setPath(path);
        menu.setIcon(icon);
        menu.setSortOrder(sortOrder);
        menu.setParentId(parentId);
        return menuRepository.save(menu);
    }

    private void saveRoleIfChanged(Role role, boolean changed) {
        if (changed) {
            roleRepository.save(role);
        }
    }
}
