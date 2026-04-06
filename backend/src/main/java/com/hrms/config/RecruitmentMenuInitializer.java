package com.hrms.config;

import com.hrms.entity.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentMenuInitializer implements CommandLineRunner {

    private final MenuBootstrapSupport menuBootstrapSupport;

    @Override
    public void run(String... args) {
        Menu recruitment = menuBootstrapSupport.ensureMenu("招聘管理", "/recruitment", "User", 60, 0L);
        Menu requirements = menuBootstrapSupport.ensureMenu("招聘需求", "/recruitment/requirements", "Document", 61, recruitment.getId());
        Menu positions = menuBootstrapSupport.ensureMenu("招聘职位", "/recruitment/positions", "OfficeBuilding", 62, recruitment.getId());
        Menu candidates = menuBootstrapSupport.ensureMenu("候选人管理", "/recruitment/candidates", "Avatar", 63, recruitment.getId());
        Menu referrals = menuBootstrapSupport.ensureMenu("内推记录", "/recruitment/referrals", "User", 64, recruitment.getId());

        menuBootstrapSupport.attachMenus("ADMIN", recruitment, requirements, positions, candidates);
        menuBootstrapSupport.attachMenus("HR", recruitment, requirements, positions, candidates);
        menuBootstrapSupport.attachMenus("EMP", recruitment, positions, referrals);
    }
}
