package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.AttendanceRecord;
import com.hrms.entity.EmployeeCarePlan;
import com.hrms.entity.User;
import com.hrms.repository.AttendanceRecordRepository;
import com.hrms.repository.EmployeeCarePlanRepository;
import com.hrms.repository.LeaveRequestRepository;
import com.hrms.repository.UserRepository;
import com.hrms.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeCarePlanRepository carePlanRepository;

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@AuthenticationPrincipal LoginUser loginUser) {
        long empCount = userRepository.count();
        long pendingLeaves = leaveRequestRepository.findByStatusOrderByCreatedAtDesc("待审批").size();
        LocalDate today = LocalDate.now();
        List<AttendanceRecord> todayAtt = attendanceRecordRepository.findByAttDateBetweenOrderByAttDateAsc(today, today);
        long clocked = todayAtt.stream().filter(a -> a.getClockIn() != null).count();
        long pendingCarePlans = carePlanRepository.count((root, query, cb) -> cb.equal(root.get("status"), "待执行"));

        Map<String, Object> m = new HashMap<>();
        m.put("employeeCount", empCount);
        m.put("pendingLeaveCount", pendingLeaves);
        m.put("todayAttendanceCount", todayAtt.size());
        m.put("todayClockInCount", clocked);
        m.put("pendingCarePlanCount", pendingCarePlans);

        if (loginUser != null) {
            userRepository.findById(loginUser.getUserId()).ifPresent(u -> {
                m.put("profile", Map.of(
                        "realName", safe(u.getRealName()),
                        "employeeNo", safe(u.getEmployeeNo()),
                        "departmentId", u.getDepartmentId() != null ? u.getDepartmentId() : 0
                ));
            });
        }
        return ApiResponse.ok(m);
    }

    @GetMapping("/attendance-trend")
    public ApiResponse<Map<String, Object>> trend(@AuthenticationPrincipal LoginUser loginUser) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        List<AttendanceRecord> list = attendanceRecordRepository.findByAttDateBetweenOrderByAttDateAsc(start, end);
        Map<String, Long> byDay = list.stream()
                .collect(Collectors.groupingBy(a -> a.getAttDate().toString(), Collectors.counting()));
        Map<String, Object> m = new HashMap<>();
        m.put("labels", byDay.keySet().stream().sorted().toList());
        m.put("values", byDay.keySet().stream().sorted().map(byDay::get).toList());
        return ApiResponse.ok(m);
    }

    @GetMapping("/care-reminders")
    public ApiResponse<List<Map<String, Object>>> careReminders(@AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = today.plusDays(30);
        List<Map<String, Object>> reminders = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (user.getStatus() != null && user.getStatus() == 0) {
                continue;
            }
            if (user.getBirthday() != null) {
                LocalDate nextBirthday = nextOccurrence(user.getBirthday(), today);
                if (!nextBirthday.isAfter(end)) {
                    reminders.add(reminderMap(user, "生日慰问", nextBirthday, existsPlan(user, "生日慰问", nextBirthday)));
                }
            }
            if (user.getHireDate() != null) {
                LocalDate nextAnniversary = nextOccurrence(user.getHireDate(), today);
                if (!nextAnniversary.isAfter(end)) {
                    long years = ChronoUnit.YEARS.between(user.getHireDate(), nextAnniversary);
                    String careType = "入职" + years + "周年";
                    reminders.add(reminderMap(user, careType, nextAnniversary, existsPlan(user, careType, nextAnniversary)));
                }
            }
        }
        reminders.sort(Comparator
                .comparing((Map<String, Object> item) -> LocalDate.parse((String) item.get("targetDate")))
                .thenComparing(item -> String.valueOf(item.get("careType"))));
        return ApiResponse.ok(reminders);
    }

    private boolean existsPlan(User user, String careType, LocalDate targetDate) {
        return carePlanRepository.findAll().stream().anyMatch(plan ->
                plan.getUser() != null
                        && Objects.equals(plan.getUser().getId(), user.getId())
                        && careType.equals(plan.getCareType())
                        && plan.getPlannedTime() != null
                        && plan.getPlannedTime().atZone(ZoneId.systemDefault()).toLocalDate().equals(targetDate)
        );
    }

    private Map<String, Object> reminderMap(User user, String careType, LocalDate date, boolean planned) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("realName", safe(user.getRealName()));
        data.put("employeeNo", safe(user.getEmployeeNo()));
        data.put("careType", careType);
        data.put("targetDate", date.toString());
        data.put("daysLeft", Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(ZoneId.systemDefault()), date)));
        data.put("planned", planned);
        return data;
    }

    private LocalDate nextOccurrence(LocalDate source, LocalDate today) {
        MonthDay monthDay = MonthDay.from(source);
        LocalDate target = monthDay.atYear(today.getYear());
        if (target.isBefore(today)) {
            target = monthDay.atYear(today.getYear() + 1);
        }
        return target;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private static boolean isAdmin(LoginUser u) {
        return u != null && "ADMIN".equals(u.getRoleCode());
    }

    private static boolean isHr(LoginUser u) {
        return u != null && "HR".equals(u.getRoleCode());
    }

    private static void assertAdminOrHr(LoginUser u) {
        if (!isAdmin(u) && !isHr(u)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }
}
