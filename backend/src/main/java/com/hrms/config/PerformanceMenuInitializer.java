package com.hrms.config;

import com.hrms.entity.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PerformanceMenuInitializer implements CommandLineRunner {

    private final MenuBootstrapSupport menuBootstrapSupport;

    @Override
    public void run(String... args) {
        Menu salary = menuBootstrapSupport.ensureMenu("薪资管理", "/salary", "Money", 30, 0L);
        Menu performance = menuBootstrapSupport.ensureMenu("绩效考核", "/salary/performance", "DataAnalysis", 33, salary.getId());

        menuBootstrapSupport.attachMenus("ADMIN", salary, performance);
        menuBootstrapSupport.attachMenus("HR", salary, performance);
    }
}
