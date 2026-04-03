package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.EmployeeCarePlan;
import com.hrms.entity.EmployeeCareRecord;
import com.hrms.entity.User;
import com.hrms.repository.EmployeeCarePlanRepository;
import com.hrms.repository.EmployeeCareRecordRepository;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/care/plans")
@RequiredArgsConstructor
public class EmployeeCarePlanController {

    private final EmployeeCarePlanRepository planRepository;
    private final EmployeeCareRecordRepository recordRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Page<EmployeeCarePlan> result = planRepository.findAll(
                spec(keyword, status),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "plannedTime", "id"))
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        return ApiResponse.ok(data);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status,
                                         @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        List<EmployeeCarePlan> list = planRepository.findAll(
                spec(keyword, status),
                Sort.by(Sort.Direction.DESC, "plannedTime", "id")
        );
        String title = "关怀计划";
        byte[] bytes = ExcelExportUtil.carePlanReport(title, list);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=care-plans.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping
    public ApiResponse<EmployeeCarePlan> create(@RequestBody CarePlanSave body,
                                                @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        EmployeeCarePlan plan = new EmployeeCarePlan();
        plan.setPlanCode(nextCode());
        apply(plan, body);
        return ApiResponse.ok(planRepository.save(plan));
    }

    @PostMapping("/quick-create")
    public ApiResponse<EmployeeCarePlan> quickCreate(@RequestBody QuickCreateRequest body,
                                                     @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        if (body.getUserId() == null) {
            throw new IllegalArgumentException("请选择员工");
        }
        User user = userRepository.findById(body.getUserId()).orElseThrow(() -> new IllegalArgumentException("员工不存在"));
        String careType = hasText(body.getCareType()) ? body.getCareType().trim() : "日常关怀";
        LocalDate targetDate = body.getTargetDate() != null && !body.getTargetDate().isBlank()
                ? LocalDate.parse(body.getTargetDate().trim())
                : LocalDate.now();
        boolean exists = planRepository.count((root, query, cb) -> cb.and(
                cb.equal(root.join("user").get("id"), user.getId()),
                cb.equal(root.get("careType"), careType),
                cb.equal(cb.function("date", LocalDate.class, root.get("plannedTime")), targetDate)
        )) > 0;
        if (exists) {
            throw new IllegalArgumentException("该提醒已生成关怀计划");
        }
        EmployeeCarePlan plan = new EmployeeCarePlan();
        plan.setPlanCode(nextCode());
        plan.setUser(user);
        plan.setCareType(careType);
        plan.setPlannedTime(targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        plan.setStatus("待执行");
        plan.setBudgetAmount(BigDecimal.ZERO);
        plan.setContent(buildQuickContent(user, careType, targetDate));
        plan.setRemark("首页提醒一键生成");
        return ApiResponse.ok(planRepository.save(plan));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeCarePlan> update(@PathVariable Long id,
                                                @RequestBody CarePlanSave body,
                                                @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        EmployeeCarePlan plan = planRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("关怀计划不存在"));
        String previousStatus = plan.getStatus();
        apply(plan, body);
        EmployeeCarePlan saved = planRepository.save(plan);
        autoCreateRecordIfCompleted(saved, previousStatus);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        planRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private String buildQuickContent(User user, String careType, LocalDate targetDate) {
        String name = user.getRealName() != null ? user.getRealName() : "员工";
        return switch (careType) {
            case "生日慰问" -> String.format("为%s安排生日关怀，关怀日期：%s。", name, targetDate);
            case "入职周年" -> String.format("为%s安排入职周年关怀，关怀日期：%s。", name, targetDate);
            default -> String.format("为%s安排%s，计划日期：%s。", name, careType, targetDate);
        };
    }

    private void apply(EmployeeCarePlan plan, CarePlanSave body) {
        if (body.getUserId() == null) {
            throw new IllegalArgumentException("请选择关怀员工");
        }
        User user = userRepository.findById(body.getUserId()).orElseThrow(() -> new IllegalArgumentException("员工不存在"));
        plan.setUser(user);
        plan.setCareType(hasText(body.getCareType()) ? body.getCareType().trim() : "日常关怀");
        plan.setPlannedTime(hasText(body.getPlannedTime()) ? Instant.parse(body.getPlannedTime().trim()) : null);
        plan.setStatus(hasText(body.getStatus()) ? body.getStatus().trim() : "待执行");
        plan.setContent(hasText(body.getContent()) ? body.getContent().trim() : null);
        plan.setRemark(hasText(body.getRemark()) ? body.getRemark().trim() : null);
        plan.setBudgetAmount(body.getBudgetAmount() != null ? body.getBudgetAmount() : BigDecimal.ZERO);
    }

    private void autoCreateRecordIfCompleted(EmployeeCarePlan plan, String previousStatus) {
        if (plan == null || plan.getId() == null) {
            return;
        }
        boolean becameCompleted = !"已完成".equals(previousStatus) && "已完成".equals(plan.getStatus());
        if (!becameCompleted || recordRepository.existsByPlan_Id(plan.getId())) {
            return;
        }
        EmployeeCareRecord record = new EmployeeCareRecord();
        record.setRecordCode(nextRecordCode());
        record.setPlan(plan);
        record.setUser(plan.getUser());
        record.setCareType(plan.getCareType());
        record.setCareTime(plan.getPlannedTime() != null ? plan.getPlannedTime() : Instant.now());
        record.setHandlerName("系统自动补录");
        record.setStatus("已执行");
        record.setContent(plan.getContent());
        record.setRemark("关怀计划完成后自动生成");
        record.setCostAmount(plan.getBudgetAmount() != null ? plan.getBudgetAmount() : BigDecimal.ZERO);
        recordRepository.save(record);
    }

    private Specification<EmployeeCarePlan> spec(String keyword, String status) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (hasText(status)) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status.trim()));
            }
            if (hasText(keyword)) {
                String value = "%" + keyword.trim() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("planCode"), value),
                        cb.like(root.get("careType"), value),
                        cb.like(root.get("status"), value),
                        cb.like(root.get("content"), value),
                        cb.like(root.join("user").get("realName"), value),
                        cb.like(root.join("user").get("employeeNo"), value)
                ));
            }
            return predicate;
        };
    }

    private String nextCode() {
        String prefix = "CAREP" + LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long next = planRepository.findTopByOrderByIdDesc().map(EmployeeCarePlan::getId).orElse(0L) + 1;
        return prefix + String.format("%04d", next);
    }

    private String nextRecordCode() {
        String prefix = "CARER" + LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long next = recordRepository.findTopByOrderByIdDesc().map(EmployeeCareRecord::getId).orElse(0L) + 1;
        return prefix + String.format("%04d", next);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
    public static class CarePlanSave {
        private Long userId;
        private String careType;
        private String plannedTime;
        private String status;
        private String content;
        private String remark;
        private BigDecimal budgetAmount;
    }

    @Data
    public static class QuickCreateRequest {
        private Long userId;
        private String careType;
        private String targetDate;
    }
}
