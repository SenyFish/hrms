package com.hrms.config;

import com.hrms.entity.Menu;
import com.hrms.entity.Role;
import com.hrms.repository.MenuRepository;
import com.hrms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FinanceMenuInitializer implements CommandLineRunner {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        Menu finance = ensureMenu("财务管理", "/finance", "Money", 50, 0L);
        Menu expenses = ensureMenu("财务支出", "/finance/expenses", "List", 51, finance.getId());
        Menu assets = ensureMenu("资产信息", "/finance/assets", "Document", 52, finance.getId());
        Menu approvals = ensureMenu("资产审批", "/finance/approvals", "Avatar", 53, finance.getId());

        attachMenus("ADMIN", finance, expenses, assets, approvals);
        attachMenus("HR", finance, expenses, assets, approvals);
        attachMenus("EMP", finance, assets, approvals);
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
                if (role.getMenus().stream().noneMatch(x -> x.getId().equals(menu.getId()))) {
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
