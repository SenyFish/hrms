package com.hrms.config;

import com.hrms.entity.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeRelationsMenuInitializer implements CommandLineRunner {

    private final MenuBootstrapSupport menuBootstrapSupport;

    @Override
    public void run(String... args) {
        Menu relations = menuBootstrapSupport.ensureMenu("员工关系", "/relations", "User", 70, 0L);
        Menu contracts = menuBootstrapSupport.ensureMenu("签合同", "/relations/contracts", "Document", 71, relations.getId());
        Menu disputes = menuBootstrapSupport.ensureMenu("处理纠纷", "/relations/disputes", "Avatar", 72, relations.getId());

        menuBootstrapSupport.attachMenus("ADMIN", relations, contracts, disputes);
        menuBootstrapSupport.attachMenus("HR", relations, contracts, disputes);
        menuBootstrapSupport.attachMenus("EMP", relations, contracts, disputes);
    }
}
