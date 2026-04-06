package com.hrms.config;

import com.hrms.entity.Menu;
import com.hrms.repository.MenuRepository;
import com.hrms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FinanceModuleCleanupRunner implements CommandLineRunner {

    private static final List<String> FINANCE_PATHS = List.of(
            "/finance",
            "/finance/expenses",
            "/finance/assets",
            "/finance/approvals"
    );

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        List<Menu> financeMenus = FINANCE_PATHS.stream()
                .map(menuRepository::findByPath)
                .flatMap(java.util.Optional::stream)
                .toList();
        if (financeMenus.isEmpty()) {
            return;
        }

        var menuIds = financeMenus.stream().map(Menu::getId).collect(java.util.stream.Collectors.toSet());

        roleRepository.findAll().forEach(role -> {
            if (role.getMenus() != null && role.getMenus().removeIf(menu -> menuIds.contains(menu.getId()))) {
                roleRepository.save(role);
            }
        });

        menuRepository.deleteAll(financeMenus);
    }
}
