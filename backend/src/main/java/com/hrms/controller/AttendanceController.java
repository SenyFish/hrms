package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.AttendanceRecord;
import com.hrms.entity.Department;
import com.hrms.entity.User;
import com.hrms.repository.AttendanceRecordRepository;
import com.hrms.repository.DepartmentRepository;
import com.hrms.repository.UserRepository;
import com.hrms.security.LoginUser;
import com.hrms.util.ExcelExportUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) Long userId,
                                                 @RequestParam(required = false) String from,
                                                 @RequestParam(required = false) String to,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        LocalDate fromD = from != null ? LocalDate.parse(from) : LocalDate.now().withDayOfMonth(1);
        LocalDate toD = to != null ? LocalDate.parse(to) : LocalDate.now();
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Page<AttendanceRecord> result = attendanceRecordRepository.findAll(
                adminSpec(userId, fromD, toD, keyword),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "attDate", "id"))
        );
        return ApiResponse.ok(pageData(result, pageNo, pageSize));
    }

    @GetMapping("/records/my")
    public ApiResponse<Map<String, Object>> my(@RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        LocalDate fromD = from != null ? LocalDate.parse(from) : LocalDate.now().minusDays(30);
        LocalDate toD = to != null ? LocalDate.parse(to) : LocalDate.now();
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Page<AttendanceRecord> result = attendanceRecordRepository.findAll(
                mySpec(loginUser.getUserId(), fromD, toD, keyword),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "attDate", "id"))
        );
        return ApiResponse.ok(pageData(result, pageNo, pageSize));
    }

    @PostMapping("/clock")
    @Transactional
    public ApiResponse<AttendanceRecord> clock(@RequestBody ClockBody body, @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        LocalDate day = body.getDate() != null ? LocalDate.parse(body.getDate()) : LocalDate.now(ZoneId.systemDefault());
        User user = userRepository.findById(loginUser.getUserId()).orElseThrow();
        Department department = resolveDepartment(user);
        AttendanceRecord rec = attendanceRecordRepository.findByUser_IdAndAttDate(user.getId(), day)
                .orElseGet(() -> {
                    AttendanceRecord a = new AttendanceRecord();
                    a.setUser(user);
                    a.setAttDate(day);
                    a.setStatus("未打卡");
                    return a;
                });

        Instant now = Instant.now();
        if ("IN".equalsIgnoreCase(body.getType())) {
            rec.setClockIn(now);
        } else if ("OUT".equalsIgnoreCase(body.getType())) {
            rec.setClockOut(now);
        } else {
            throw new IllegalArgumentException("type 必须是 IN 或 OUT");
        }

        rec.setStatus(calculateStatus(rec, department));
        return ApiResponse.ok(attendanceRecordRepository.save(rec));
    }

    @PostMapping("/records")
    @Transactional
    public ApiResponse<AttendanceRecord> saveOrUpdate(@RequestBody AttendanceSave body,
                                                      @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        User user = userRepository.findById(body.getUserId()).orElseThrow(() -> new IllegalArgumentException("员工不存在"));
        LocalDate day = LocalDate.parse(body.getDate());
        AttendanceRecord rec = attendanceRecordRepository.findByUser_IdAndAttDate(user.getId(), day)
                .orElseGet(() -> {
                    AttendanceRecord a = new AttendanceRecord();
                    a.setUser(user);
                    a.setAttDate(day);
                    return a;
                });
        rec.setStatus(body.getStatus() != null ? body.getStatus() : "正常");
        rec.setRemark(body.getRemark());
        if (body.getClockIn() != null) {
            rec.setClockIn(Instant.parse(body.getClockIn()));
        }
        if (body.getClockOut() != null) {
            rec.setClockOut(Instant.parse(body.getClockOut()));
        }
        return ApiResponse.ok(attendanceRecordRepository.save(rec));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String from, @RequestParam String to,
                                         @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        LocalDate fromD = LocalDate.parse(from);
        LocalDate toD = LocalDate.parse(to);
        List<AttendanceRecord> list = attendanceRecordRepository.findByAttDateBetweenOrderByAttDateAsc(fromD, toD);
        String title = "考勤-" + from + "_" + to;
        byte[] bytes = ExcelExportUtil.attendanceMonthReport(title, list);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-" + from + "-" + to + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping("/import")
    @Transactional
    public ApiResponse<Integer> importCsv(@RequestParam("file") MultipartFile file,
                                          @AuthenticationPrincipal LoginUser loginUser) throws Exception {
        assertAdminOrHr(loginUser);
        int n = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                String[] p = line.split(",");
                if (p.length < 3) {
                    continue;
                }
                String employeeNo = p[0].trim();
                LocalDate day = LocalDate.parse(p[1].trim());
                String status = p[2].trim();
                User u = userRepository.findAll().stream()
                        .filter(x -> employeeNo.equals(x.getEmployeeNo()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("找不到工号 " + employeeNo));
                AttendanceRecord rec = attendanceRecordRepository.findByUser_IdAndAttDate(u.getId(), day)
                        .orElseGet(() -> {
                            AttendanceRecord a = new AttendanceRecord();
                            a.setUser(u);
                            a.setAttDate(day);
                            return a;
                        });
                rec.setStatus(status);
                attendanceRecordRepository.save(rec);
                n++;
            }
        }
        return ApiResponse.ok("导入完成", n);
    }

    private Specification<AttendanceRecord> adminSpec(Long userId, LocalDate from, LocalDate to, String keyword) {
        return (root, query, cb) -> {
            Predicate predicate = cb.between(root.get("attDate"), from, to);
            if (userId != null) {
                predicate = cb.and(predicate, cb.equal(root.join("user").get("id"), userId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("status"), value),
                        cb.like(root.get("remark"), value),
                        cb.like(root.join("user").get("realName"), value),
                        cb.like(root.join("user").get("employeeNo"), value)
                ));
            }
            return predicate;
        };
    }

    private Specification<AttendanceRecord> mySpec(Long userId, LocalDate from, LocalDate to, String keyword) {
        return (root, query, cb) -> {
            Predicate predicate = cb.and(
                    cb.equal(root.join("user").get("id"), userId),
                    cb.between(root.get("attDate"), from, to)
            );
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("status"), value),
                        cb.like(root.get("remark"), value)
                ));
            }
            return predicate;
        };
    }

    private Map<String, Object> pageData(Page<AttendanceRecord> result, int pageNo, int pageSize) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return data;
    }

    private Department resolveDepartment(User user) {
        if (user.getDepartmentId() == null) {
            return null;
        }
        return departmentRepository.findById(user.getDepartmentId()).orElse(null);
    }

    private String calculateStatus(AttendanceRecord rec, Department department) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalTime startTime = parseWorkTime(department != null ? department.getWorkStartTime() : null, LocalTime.of(9, 0));
        LocalTime endTime = parseWorkTime(department != null ? department.getWorkEndTime() : null, LocalTime.of(18, 0));
        LocalDate day = rec.getAttDate() != null ? rec.getAttDate() : LocalDate.now(zoneId);

        LocalDateTime workStart = LocalDateTime.of(day, startTime);
        LocalDateTime workEnd = LocalDateTime.of(day, endTime);
        LocalDateTime clockIn = toLocalDateTime(rec.getClockIn(), zoneId);
        LocalDateTime clockOut = toLocalDateTime(rec.getClockOut(), zoneId);

        boolean late = clockIn != null && clockIn.isAfter(workStart);
        boolean earlyLeave = clockOut != null && clockOut.isBefore(workEnd);

        if (clockIn == null && clockOut == null) {
            return "未打卡";
        }
        if (clockIn != null && clockOut == null) {
            return late ? "迟到待下班" : "上班正常";
        }
        if (clockIn == null) {
            return earlyLeave ? "缺上班卡且早退" : "缺上班卡";
        }
        if (late && earlyLeave) {
            return "迟到早退";
        }
        if (late) {
            return "迟到";
        }
        if (earlyLeave) {
            return "早退";
        }
        return "正常";
    }

    private LocalTime parseWorkTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(instant, zoneId).toLocalDateTime();
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

    @Data
    public static class ClockBody {
        private String type;
        private String date;
    }

    @Data
    public static class AttendanceSave {
        private Long userId;
        private String date;
        private String clockIn;
        private String clockOut;
        private String status;
        private String remark;
    }
}
