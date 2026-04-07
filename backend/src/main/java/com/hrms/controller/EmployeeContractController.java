package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.EmployeeContract;
import com.hrms.repository.EmployeeContractRepository;
import com.hrms.security.LoginUser;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/relations/contracts")
@RequiredArgsConstructor
public class EmployeeContractController {

    private static final String STATUS_ACTIVE = "生效中";
    private static final String STATUS_EXPIRING = "即将到期";
    private static final String STATUS_EXPIRED = "已到期";
    private static final String STATUS_TERMINATED = "已终止";

    private final EmployeeContractRepository employeeContractRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertLoggedIn(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Long visibleEmployeeId = isAdmin(loginUser) || isHr(loginUser) ? null : loginUser.getUserId();
        Page<EmployeeContract> result = employeeContractRepository.findAll(
                keywordSpec(keyword, visibleEmployeeId),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "endDate", "id"))
        );
        result.getContent().forEach(this::refreshReminderMeta);
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<EmployeeContract> create(@RequestBody EmployeeContract body,
                                                @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        EmployeeContract contract = new EmployeeContract();
        apply(body, contract);
        contract.setSerialNo(nextSerialNo());
        refreshReminderMeta(contract);
        return ApiResponse.ok(employeeContractRepository.save(contract));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeContract> update(@PathVariable Long id,
                                                @RequestBody EmployeeContract body,
                                                @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        EmployeeContract contract = employeeContractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("合同记录不存在"));
        apply(body, contract);
        refreshReminderMeta(contract);
        return ApiResponse.ok(employeeContractRepository.save(contract));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        employeeContractRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void apply(EmployeeContract body, EmployeeContract contract) {
        if (body.getEmployeeId() == null) {
            throw new IllegalArgumentException("请选择员工");
        }
        if (body.getEmployeeName() == null || body.getEmployeeName().isBlank()) {
            throw new IllegalArgumentException("员工姓名不能为空");
        }
        if (body.getContractType() == null || body.getContractType().isBlank()) {
            throw new IllegalArgumentException("请选择合同类型");
        }
        if (body.getStartDate() == null || body.getEndDate() == null) {
            throw new IllegalArgumentException("请选择合同起止日期");
        }
        if (body.getEndDate().isBefore(body.getStartDate())) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        int reminderDays = body.getReminderDays() != null ? Math.max(body.getReminderDays(), 0) : 30;

        contract.setEmployeeId(body.getEmployeeId());
        contract.setEmployeeName(body.getEmployeeName().trim());
        contract.setContractType(body.getContractType().trim());
        contract.setContractTitle(trimToNull(body.getContractTitle()));
        contract.setStartDate(body.getStartDate());
        contract.setEndDate(body.getEndDate());
        contract.setReminderDays(reminderDays);
        contract.setReminderDate(body.getEndDate().minusDays(reminderDays));
        contract.setStatus(body.getStatus() == null || body.getStatus().isBlank() ? STATUS_ACTIVE : body.getStatus().trim());
        contract.setRemark(body.getRemark());
    }

    private void refreshReminderMeta(EmployeeContract contract) {
        if (contract.getReminderDays() == null) {
            contract.setReminderDays(30);
        }
        if (contract.getEndDate() != null) {
            contract.setReminderDate(contract.getEndDate().minusDays(contract.getReminderDays()));
        }
        contract.setReminderStatus(resolveReminderStatus(contract));
        contract.setStatus(resolveContractStatus(contract));
    }

    private String resolveReminderStatus(EmployeeContract contract) {
        if (STATUS_TERMINATED.equals(contract.getStatus())) {
            return "无需提醒";
        }
        if (contract.getEndDate() == null) {
            return "未设置";
        }
        LocalDate today = LocalDate.now();
        if (today.isAfter(contract.getEndDate())) {
            return "已过期";
        }
        LocalDate reminderDate = contract.getReminderDate();
        if (reminderDate == null) {
            return "未设置";
        }
        if (!today.isBefore(reminderDate)) {
            return "提醒中";
        }
        return "未到提醒期";
    }

    private String resolveContractStatus(EmployeeContract contract) {
        if (STATUS_TERMINATED.equals(contract.getStatus())) {
            return STATUS_TERMINATED;
        }
        if (contract.getEndDate() == null) {
            return STATUS_ACTIVE;
        }
        LocalDate today = LocalDate.now();
        if (today.isAfter(contract.getEndDate())) {
            return STATUS_EXPIRED;
        }
        LocalDate reminderDate = contract.getReminderDate();
        if (reminderDate != null && !today.isBefore(reminderDate)) {
            return STATUS_EXPIRING;
        }
        return STATUS_ACTIVE;
    }

    private Specification<EmployeeContract> keywordSpec(String keyword, Long visibleEmployeeId) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            if (visibleEmployeeId != null) {
                predicates.add(cb.equal(root.get("employeeId"), visibleEmployeeId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("serialNo"), value),
                        cb.like(root.get("employeeName"), value),
                        cb.like(root.get("contractType"), value),
                        cb.like(root.get("contractTitle"), value),
                        cb.like(root.get("status"), value)
                ));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String nextSerialNo() {
        String prefix = "CT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long next = employeeContractRepository.findTopByOrderByIdDesc().map(EmployeeContract::getId).orElse(0L) + 1;
        return prefix + String.format("%04d", next);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isAdmin(LoginUser user) {
        return user != null && "ADMIN".equals(user.getRoleCode());
    }

    private static boolean isHr(LoginUser user) {
        return user != null && "HR".equals(user.getRoleCode());
    }

    private static void assertLoggedIn(LoginUser user) {
        if (user == null) {
            throw new AccessDeniedException("未登录");
        }
    }

    private static void assertAdminOrHr(LoginUser user) {
        if (!isAdmin(user) && !isHr(user)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }
}
