package com.hrms.config;

import com.hrms.entity.Menu;
import com.hrms.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CareMenuInitializer implements CommandLineRunner {

    private final MenuRepository menuRepository;
    private final MenuBootstrapSupport menuBootstrapSupport;

    @Override
    public void run(String... args) {
        Menu care = menuBootstrapSupport.ensureMenu("员工关怀", "/care", "Calendar", 70, 0L);
        Menu plans = menuBootstrapSupport.ensureMenu("关怀计划", "/care/plans", "Document", 71, care.getId());
        Menu stats = menuBootstrapSupport.ensureMenu("关怀统计", "/care/stats", "DataAnalysis", 73, care.getId());

        menuBootstrapSupport.attachMenus("ADMIN", care, plans, stats);
        menuBootstrapSupport.attachMenus("HR", care, plans, stats);

        menuRepository.findByPath("/care/records").ifPresent(records -> {
            menuBootstrapSupport.detachMenu("ADMIN", records);
            menuBootstrapSupport.detachMenu("HR", records);
            menuRepository.delete(records);
        });
    }
}
