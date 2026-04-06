package com.hrms.config;

import com.hrms.entity.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingMenuInitializer implements CommandLineRunner {

    private final MenuBootstrapSupport menuBootstrapSupport;

    @Override
    public void run(String... args) {
        Menu training = menuBootstrapSupport.ensureMenu("培训发展", "/training", "OfficeBuilding", 80, 0L);
        Menu sessions = menuBootstrapSupport.ensureMenu("组织培训", "/training/sessions", "Calendar", 81, training.getId());
        Menu promotions = menuBootstrapSupport.ensureMenu("规划员工晋升", "/training/promotions", "DataAnalysis", 82, training.getId());

        menuBootstrapSupport.attachMenus("ADMIN", training, sessions, promotions);
        menuBootstrapSupport.attachMenus("HR", training, sessions, promotions);
        menuBootstrapSupport.attachMenus("EMP", training, sessions);
    }
}
