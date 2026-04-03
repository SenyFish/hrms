package com.hrms.config;

import com.hrms.entity.Menu;
import com.hrms.repository.MenuRepository;
import com.hrms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RecruitmentMenuInitializer implements CommandLineRunner {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        Menu recruitment = ensureMenu("招聘管理", "/recruitment", "User", 60, 0L);
        Menu requirements = ensureMenu("招聘需求", "/recruitment/requirements", "Document", 61, recruitment.getId());
        Menu positions = ensureMenu("招聘职位", "/recruitment/positions", "OfficeBuilding", 62, recruitment.getId());
        Menu candidates = ensureMenu("候选人管理", "/recruitment/candidates", "Avatar", 63, recruitment.getId());
        Menu referrals = ensureMenu("内推记录", "/recruitment/referrals", "User", 64, recruitment.getId());

        attachMenus("ADMIN", recruitment, requirements, positions, candidates);
        attachMenus("HR", recruitment, requirements, positions, candidates);
        attachMenus("EMP", recruitment, positions, referrals);
    }

    private Menu ensureMenu(String title, String path, String icon, int sortOrder, Long parentId) {
        Optional<Menu> existing = menuRepository.findByPath(path);
        if (existing.isPresent()) {
            Menu menu = existing.get();
            menu.setTitle(title);
            menu.setIcon(icon);
            menu.setSortOrder(sortOrder);
            menu.setParentId(parentId);
            return menuRepository.save(menu);
        }
        Menu menu = new Menu();
        menu.setTitle(title);
        menu.setPath(path);
        menu.setIcon(icon);
        menu.setSortOrder(sortOrder);
        menu.setParentId(parentId);
        return menuRepository.save(menu);
    }

    private void attachMenus(String roleCode, Menu... menus) {
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
            if (changed) {
                roleRepository.save(role);
            }
        });
    }
}
