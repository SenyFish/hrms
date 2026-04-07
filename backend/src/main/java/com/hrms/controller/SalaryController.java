package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.InsuredCity;
import com.hrms.entity.SalaryRecord;
import com.hrms.entity.User;
import com.hrms.repository.InsuredCityRepository;
import com.hrms.repository.SalaryRecordRepository;
import com.hrms.repository.UserRepository;
import com.hrms.security.LoginUser;
import com.hrms.service.SalaryCalculationService;
import com.hrms.util.ExcelExportUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
public class SalaryController {

    private static final String MONTH_PATTERN = "^\\d{4}-(0[1-9]|1[0-2])$";

    private final SalaryRecordRepository salaryRecordRepository;
    private final UserRepository userRepository;
    private final InsuredCityRepository insuredCityRepository;
    private final SalaryCalculationService salaryCalculationService;

    @GetMapping("/records")
    public ApiResponse<List<SalaryRecord>> list(@RequestParam(required = false) String month,
                                                @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        if (month != null && !month.isBlank()) {
            validateMonth(month);
            return ApiResponse.ok(salaryRecordRepository.findBySalaryMonthOrderByIdDesc(month));
        }
        List<SalaryRecord> list = salaryRecordRepository.findAll().stream()
                .sorted(Comparator.comparing(SalaryRecord::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/records/my")
    public ApiResponse<List<SalaryRecord>> my(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        return ApiResponse.ok(salaryRecordRepository.findByUser_IdOrderBySalaryMonthDesc(loginUser.getUserId()));
    }

    @GetMapping("/records/{id}")
    public ApiResponse<SalaryRecord> detail(@PathVariable Long id,
                                            @AuthenticationPrincipal LoginUser loginUser) {
        SalaryRecord record = salaryRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工资记录不存在"));
        assertCanViewRecord(loginUser, record);
        return ApiResponse.ok(record);
    }

    @PostMapping("/records")
    @Transactional
    public ApiResponse<SalaryRecord> save(@RequestBody SalarySave body,
                                          @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        if (body.getUserId() == null) {
            throw new IllegalArgumentException("请选择员工");
        }
        validateMonth(body.getSalaryMonth());

        User user = userRepository.findById(body.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("员工不存在"));

        SalaryRecord record;
        if (body.getId() != null) {
            record = salaryRecordRepository.findById(body.getId())
                    .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
            ensureNoDuplicate(body, user.getId());
            record.setUser(user);
        } else {
            ensureNoDuplicate(body, user.getId());
            record = new SalaryRecord();
            record.setUser(user);
        }

        record.setSalaryMonth(body.getSalaryMonth());
        record.setBaseSalary(n(body.getBaseSalary()));
        record.setSocialSecurityBase(n(body.getSocialSecurityBase()));
        record.setHousingFundBase(n(body.getHousingFundBase()));
        record.setRemark(body.getRemark());

        if (body.getInsuredCityId() != null) {
            InsuredCity city = insuredCityRepository.findById(body.getInsuredCityId())
                    .orElseThrow(() -> new IllegalArgumentException("参保城市不存在"));
            record.setInsuredCity(city);
        } else {
            record.setInsuredCity(null);
        }

        salaryCalculationService.applyCityRules(record);
        return ApiResponse.ok(salaryRecordRepository.save(record));
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdmin(loginUser);
        salaryRecordRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/records/{id}/payslip")
    public ResponseEntity<byte[]> payslip(@PathVariable Long id,
                                          @AuthenticationPrincipal LoginUser loginUser) {
        SalaryRecord record = salaryRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工资记录不存在"));
        assertCanViewRecord(loginUser, record);

        byte[] bytes = ExcelExportUtil.salaryPayslip(record);
        String employeeNo = record.getUser() != null && record.getUser().getEmployeeNo() != null
                ? record.getUser().getEmployeeNo()
                : "employee";
        String month = record.getSalaryMonth() != null ? record.getSalaryMonth() : "salary";
        String filename = "payslip-" + employeeNo + "-" + month + ".xlsx";
        String contentDisposition = "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset="
                                + StandardCharsets.UTF_8.name())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String month,
                                         @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        validateMonth(month);

        List<SalaryRecord> list = salaryRecordRepository.findBySalaryMonthOrderByIdDesc(month);
        byte[] bytes = ExcelExportUtil.salaryMonthReport(month, list);
        String filename = "salary-" + month + ".xlsx";
        String contentDisposition = "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset="
                                + StandardCharsets.UTF_8.name())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private void assertCanViewRecord(LoginUser loginUser, SalaryRecord record) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        if (isAdmin(loginUser) || isHr(loginUser)) {
            return;
        }
        Long ownerId = record.getUser() != null ? record.getUser().getId() : null;
        if (ownerId == null || !ownerId.equals(loginUser.getUserId())) {
            throw new AccessDeniedException("无权限查看该工资记录");
        }
    }

    private void ensureNoDuplicate(SalarySave body, Long userId) {
        salaryRecordRepository.findByUser_IdAndSalaryMonth(userId, body.getSalaryMonth())
                .ifPresent(existing -> {
                    if (body.getId() == null || !existing.getId().equals(body.getId())) {
                        throw new IllegalArgumentException("该员工该月份已存在薪资记录");
                    }
                });
    }

    private static BigDecimal n(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static void validateMonth(String month) {
        if (month == null || !month.matches(MONTH_PATTERN)) {
            throw new IllegalArgumentException("月份格式必须是 yyyy-MM");
        }
    }

    private static boolean isAdmin(LoginUser u) {
        return u != null && "ADMIN".equals(u.getRoleCode());
    }

    private static boolean isHr(LoginUser u) {
        return u != null && "HR".equals(u.getRoleCode());
    }

    private static void assertAdmin(LoginUser u) {
        if (!isAdmin(u)) {
            throw new AccessDeniedException("需要管理员权限");
        }
    }

    private static void assertAdminOrHr(LoginUser u) {
        if (!isAdmin(u) && !isHr(u)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }

    @Data
    public static class SalarySave {
        private Long id;
        private Long userId;
        private String salaryMonth;
        private BigDecimal baseSalary;
        private BigDecimal socialSecurityBase;
        private BigDecimal housingFundBase;
        private Long insuredCityId;
        private String remark;
    }
}
