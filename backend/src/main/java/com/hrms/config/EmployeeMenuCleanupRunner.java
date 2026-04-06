package com.hrms.config;

import com.hrms.repository.MenuRepository;
import com.hrms.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeMenuCleanupRunner implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    @Override
    public void run(String... args) {
        roleRepository.findByCode("EMP").ifPresent(role ->
                menuRepository.findByPath("/salary/cities").ifPresent(menu -> {
                    if (role.getMenus() != null && role.getMenus().removeIf(item -> item.getId().equals(menu.getId()))) {
                        roleRepository.save(role);
                    }
                })
        );
    }
}
