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
public class TrainingMenuInitializer implements CommandLineRunner {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        Menu training = ensureMenu("培训发展", "/training", "OfficeBuilding", 80, 0L);
        Menu sessions = ensureMenu("组织培训", "/training/sessions", "Calendar", 81, training.getId());
        Menu promotions = ensureMenu("规划员工晋升", "/training/promotions", "DataAnalysis", 82, training.getId());

        attachMenus("ADMIN", training, sessions, promotions);
        attachMenus("HR", training, sessions, promotions);
        attachMenus("EMP", training, sessions);
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
