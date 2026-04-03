package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.EmployeeCarePlan;
import com.hrms.entity.EmployeeCareRecord;
import com.hrms.entity.User;
import com.hrms.repository.EmployeeCarePlanRepository;
import com.hrms.repository.EmployeeCareRecordRepository;
import com.hrms.repository.UserRepository;
import com.hrms.security.LoginUser;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/care/records")
@RequiredArgsConstructor
public class EmployeeCareRecordController {

    private final EmployeeCareRecordRepository recordRepository;
    private final EmployeeCarePlanRepository planRepository;
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
        Page<EmployeeCareRecord> result = recordRepository.findAll(
                spec(keyword, status),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "careTime", "id"))
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<EmployeeCareRecord> create(@RequestBody CareRecordSave body,
                                                  @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        EmployeeCareRecord record = new EmployeeCareRecord();
        record.setRecordCode(nextCode());
        apply(record, body);
        EmployeeCareRecord saved = recordRepository.save(record);
        syncPlanStatus(saved.getPlan());
        return ApiResponse.ok(saved);
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeCareRecord> update(@PathVariable Long id,
                                                  @RequestBody CareRecordSave body,
                                                  @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        EmployeeCareRecord record = recordRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("关怀记录不存在"));
        EmployeeCarePlan previousPlan = record.getPlan();
        apply(record, body);
        EmployeeCareRecord saved = recordRepository.save(record);
        syncPlanStatus(previousPlan);
        syncPlanStatus(saved.getPlan());
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        EmployeeCareRecord record = recordRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("关怀记录不存在"));
        EmployeeCarePlan plan = record.getPlan();
        recordRepository.deleteById(id);
        syncPlanStatus(plan);
        return ApiResponse.ok();
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(@AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        long totalPlans = planRepository.count();
        long totalRecords = recordRepository.count();
        long pendingPlans = planRepository.count((root, query, cb) -> cb.equal(root.get("status"), "待执行"));
        long completedPlans = planRepository.count((root, query, cb) -> cb.equal(root.get("status"), "已完成"));
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        Instant startTime = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
        long monthRecords = recordRepository.count((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("careTime"), startTime));
        Map<String, Object> data = new HashMap<>();
        data.put("totalPlans", totalPlans);
        data.put("totalRecords", totalRecords);
        data.put("pendingPlans", pendingPlans);
        data.put("completedPlans", completedPlans);
        data.put("monthRecords", monthRecords);
        return ApiResponse.ok(data);
    }

    private void apply(EmployeeCareRecord record, CareRecordSave body) {
        if (body.getUserId() == null) {
            throw new IllegalArgumentException("请选择关怀员工");
        }
        User user = userRepository.findById(body.getUserId()).orElseThrow(() -> new IllegalArgumentException("员工不存在"));
        record.setUser(user);
        if (body.getPlanId() != null) {
            EmployeeCarePlan plan = planRepository.findById(body.getPlanId()).orElseThrow(() -> new IllegalArgumentException("关怀计划不存在"));
            record.setPlan(plan);
        } else {
            record.setPlan(null);
        }
        record.setCareType(hasText(body.getCareType()) ? body.getCareType().trim() : "日常关怀");
        record.setCareTime(hasText(body.getCareTime()) ? Instant.parse(body.getCareTime().trim()) : null);
        record.setHandlerName(hasText(body.getHandlerName()) ? body.getHandlerName().trim() : null);
        record.setStatus(hasText(body.getStatus()) ? body.getStatus().trim() : "已执行");
        record.setContent(hasText(body.getContent()) ? body.getContent().trim() : null);
        record.setRemark(hasText(body.getRemark()) ? body.getRemark().trim() : null);
        record.setCostAmount(body.getCostAmount() != null ? body.getCostAmount() : BigDecimal.ZERO);
    }

    private void syncPlanStatus(EmployeeCarePlan plan) {
        if (plan == null || plan.getId() == null) {
            return;
        }
        long count = recordRepository.count((root, query, cb) -> cb.equal(root.join("plan").get("id"), plan.getId()));
        planRepository.findById(plan.getId()).ifPresent(item -> {
            item.setStatus(count > 0 ? "已完成" : "待执行");
            planRepository.save(item);
        });
    }

    private Specification<EmployeeCareRecord> spec(String keyword, String status) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (hasText(status)) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status.trim()));
            }
            if (hasText(keyword)) {
                String value = "%" + keyword.trim() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("recordCode"), value),
                        cb.like(root.get("careType"), value),
                        cb.like(root.get("handlerName"), value),
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
    public static class CareRecordSave {
        private Long planId;
        private Long userId;
        private String careType;
        private String careTime;
        private String handlerName;
        private String status;
        private String content;
        private String remark;
        private BigDecimal costAmount;
    }
}
