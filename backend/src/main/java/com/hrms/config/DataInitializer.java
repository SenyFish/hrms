package com.hrms.config;

import com.hrms.entity.*;
import com.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final InsuredCityRepository insuredCityRepository;
    private final PasswordEncoder passwordEncoder;
    private final SysNoticeRepository sysNoticeRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        Menu mHome = menu("首页", "/home", "HomeFilled", 1, 0L);
        Menu mSys = menu("系统管理", "/system", "Setting", 10, 0L);
        menu("文件管理", "/system/files", "Folder", 11, mSys.getId());
        menu("员工管理", "/system/employees", "User", 12, mSys.getId());
        menu("部门管理", "/system/departments", "OfficeBuilding", 13, mSys.getId());

        Menu mPerm = menu("权限管理", "/permission", "Lock", 20, 0L);
        menu("角色管理", "/permission/roles", "Avatar", 21, mPerm.getId());
        menu("菜单管理", "/permission/menus", "Menu", 22, mPerm.getId());

        Menu mSal = menu("薪资管理", "/salary", "Money", 30, 0L);
        menu("五险一金与工资", "/salary/records", "Document", 31, mSal.getId());
        menu("参保城市", "/salary/cities", "Location", 32, mSal.getId());

        Menu mAtt = menu("考勤管理", "/attendance", "Clock", 40, 0L);
        menu("请假审批", "/attendance/leaves", "Calendar", 41, mAtt.getId());
        menu("考勤记录", "/attendance/records", "List", 42, mAtt.getId());

        var allMenus = menuRepository.findAll();

        Role admin = new Role();
        admin.setName("系统管理员");
        admin.setCode("ADMIN");
        admin.setDescription("全部功能");
        admin.setMenus(new java.util.HashSet<>(allMenus));
        admin = roleRepository.save(admin);

        Role hr = new Role();
        hr.setName("人事");
        hr.setCode("HR");
        hr.setDescription("人事与考勤薪资");
        hr.setMenus(new java.util.HashSet<>(allMenus.stream()
                .filter(x -> !x.getPath().startsWith("/permission"))
                .toList()));
        hr = roleRepository.save(hr);

        Role emp = new Role();
        emp.setName("员工");
        emp.setCode("EMP");
        emp.setDescription("个人考勤与申请");
        var empMenus = allMenus.stream()
                .filter(x -> x.getPath().equals("/home")
                        || x.getPath().startsWith("/attendance")
                        || x.getPath().equals("/salary")
                        || x.getPath().startsWith("/salary"))
                .toList();
        emp.setMenus(new java.util.HashSet<>(empMenus));
        emp = roleRepository.save(emp);

        Department dept = new Department();
        dept.setName("研发中心");
        dept.setParentId(0L);
        dept.setSortOrder(1);
        dept.setWorkStartTime("09:00");
        dept.setWorkEndTime("18:00");
        dept.setLateGraceMinutes(10);
        dept.setLeaveSettingsNote("年假需提前 3 天申请");
        dept.setFinePerLate(new BigDecimal("50"));
        dept.setOvertimeRate(new BigDecimal("1.5"));
        dept = departmentRepository.save(dept);

        InsuredCity city = new InsuredCity();
        city.setName("上海市");
        city.setRegionCode("310100");
        city.setSocialAvgSalary(new BigDecimal("12183"));
        city.setRemark("示例社平工资");
        city = insuredCityRepository.save(city);

        User u1 = user("admin", "admin123", "系统管理员", "E001", dept.getId(), admin);
        User u2 = user("hr", "hr123", "王人事", "E002", dept.getId(), hr);
        User u3 = user("emp", "emp123", "赵员工", "E003", dept.getId(), emp);
        userRepository.save(u1);
        userRepository.save(u2);
        userRepository.save(u3);

        SysNotice n1 = new SysNotice();
        n1.setTitle("欢迎使用人力资源管理系统");
        n1.setContent("请使用 Navicat 连接 MySQL 数据库 hrms 进行数据维护。");
        n1.setPriority("high");
        sysNoticeRepository.save(n1);

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String month = String.format("%d-%02d", today.getYear(), today.getMonthValue());

        SalaryRecord sr = new SalaryRecord();
        sr.setUser(u3);
        sr.setSalaryMonth(month);
        sr.setBaseSalary(new BigDecimal("15000"));
        sr.setPensionPersonal(new BigDecimal("1200"));
        sr.setPensionCompany(new BigDecimal("2400"));
        sr.setMedicalPersonal(new BigDecimal("300"));
        sr.setMedicalCompany(new BigDecimal("1500"));
        sr.setUnemploymentPersonal(new BigDecimal("75"));
        sr.setUnemploymentCompany(new BigDecimal("75"));
        sr.setInjuryCompany(new BigDecimal("60"));
        sr.setMaternityCompany(new BigDecimal("120"));
        sr.setHousingFundPersonal(new BigDecimal("1800"));
        sr.setHousingFundCompany(new BigDecimal("1800"));
        sr.setSocialSecurityBase(new BigDecimal("12000"));
        sr.setHousingFundBase(new BigDecimal("12000"));
        sr.setInsuredCity(city);
        sr.setRemark("示例五险一金数据");
        salaryRecordRepository.save(sr);

        AttendanceRecord ar = new AttendanceRecord();
        ar.setUser(u3);
        ar.setAttDate(today);
        ar.setClockIn(Instant.now());
        ar.setStatus("上班正常");
        attendanceRecordRepository.save(ar);

        LeaveRequest lr = new LeaveRequest();
        lr.setUser(u3);
        lr.setLeaveType("事假");
        lr.setStartTime(Instant.now());
        lr.setEndTime(Instant.now().plusSeconds(3600 * 8));
        lr.setReason("示例请假待审批");
        lr.setStatus("待审批");
        leaveRequestRepository.save(lr);
    }

    private Menu menu(String title, String path, String icon, int sort, Long parentId) {
        Menu m = new Menu();
        m.setTitle(title);
        m.setPath(path);
        m.setIcon(icon);
        m.setSortOrder(sort);
        m.setParentId(parentId);
        return menuRepository.save(m);
    }

    private User user(String username, String raw, String realName, String no, Long deptId, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(raw));
        u.setRealName(realName);
        u.setEmployeeNo(no);
        u.setDepartmentId(deptId);
        u.setRole(role);
        u.setPhone("13800000000");
        u.setEmail(username + "@company.com");
        u.setBirthday(LocalDate.now().plusDays(7));
        u.setHireDate(LocalDate.now().plusDays(15).minusYears(1));
        u.setStatus(1);
        return u;
    }
}
