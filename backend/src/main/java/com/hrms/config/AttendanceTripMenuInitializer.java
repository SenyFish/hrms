package com.hrms.config;

import com.hrms.entity.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceTripMenuInitializer implements CommandLineRunner {

    private final MenuBootstrapSupport menuBootstrapSupport;

    @Override
    public void run(String... args) {
        Menu attendance = menuBootstrapSupport.ensureMenu("考勤管理", "/attendance", "Clock", 40, 0L);
        Menu trips = menuBootstrapSupport.ensureMenu("出差申请", "/attendance/trips", "Location", 43, attendance.getId());
        menuBootstrapSupport.attachMenus("ADMIN", attendance, trips);
        menuBootstrapSupport.attachMenus("HR", attendance, trips);
        menuBootstrapSupport.attachMenus("EMP", attendance, trips);
    }
}
