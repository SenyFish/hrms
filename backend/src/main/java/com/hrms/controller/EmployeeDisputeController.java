package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.EmployeeDispute;
import com.hrms.entity.User;
import com.hrms.repository.EmployeeDisputeRepository;
import com.hrms.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/relations/disputes")
@RequiredArgsConstructor
public class EmployeeDisputeController {

    private final EmployeeDisputeRepository employeeDisputeRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Specification<EmployeeDispute> spec = isAdmin(loginUser) || isHr(loginUser)
                ? keywordSpec(keyword)
                : myKeywordSpec(loginUser.getUserId(), keyword);
        Page<EmployeeDispute> result = employeeDisputeRepository.findAll(
                spec,
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "disputeDate", "id"))
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<EmployeeDispute> create(@RequestBody EmployeeDispute body,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        EmployeeDispute dispute = new EmployeeDispute();
        apply(body, dispute, loginUser);
        dispute.setSerialNo(nextSerialNo());
        return ApiResponse.ok(employeeDisputeRepository.save(dispute));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeDispute> update(@PathVariable Long id,
                                               @RequestBody EmployeeDispute body,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        EmployeeDispute dispute = employeeDisputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("纠纷记录不存在"));
        apply(body, dispute, loginUser);
        return ApiResponse.ok(employeeDisputeRepository.save(dispute));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        if (!employeeDisputeRepository.existsById(id)) {
            throw new IllegalArgumentException("纠纷记录不存在");
        }
        employeeDisputeRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void apply(EmployeeDispute body, EmployeeDispute dispute, LoginUser loginUser) {
        User currentUser = userRepository.findById(loginUser.getUserId()).orElseThrow();
        boolean adminOrHr = isAdmin(loginUser) || isHr(loginUser);
        Long employeeId = adminOrHr ? body.getEmployeeId() : currentUser.getId();
        String employeeName = adminOrHr ? body.getEmployeeName() : currentUser.getRealName();
        if (employeeId == null) {
            throw new IllegalArgumentException("请选择员工");
        }
        if (employeeName == null || employeeName.isBlank()) {
            throw new IllegalArgumentException("员工姓名不能为空");
        }
        if (body.getDisputeType() == null || body.getDisputeType().isBlank()) {
            throw new IllegalArgumentException("请输入纠纷类型");
        }
        if (body.getDisputeDate() == null) {
            throw new IllegalArgumentException("请选择纠纷日期");
        }
        if (body.getDescription() == null || body.getDescription().isBlank()) {
            throw new IllegalArgumentException("请输入纠纷说明");
        }
        dispute.setEmployeeId(employeeId);
        dispute.setEmployeeName(employeeName.trim());
        dispute.setDisputeType(body.getDisputeType().trim());
        dispute.setDisputeDate(body.getDisputeDate());
        dispute.setStatus(resolveStatus(body.getStatus(), adminOrHr, dispute.getStatus()));
        dispute.setDescription(body.getDescription().trim());
        dispute.setResolution(body.getResolution() == null ? null : body.getResolution().trim());
    }

    private Specification<EmployeeDispute> keywordSpec(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String value = "%" + keyword.trim() + "%";
            Predicate[] predicates = new Predicate[]{
                    cb.like(root.get("serialNo"), value),
                    cb.like(root.get("employeeName"), value),
                    cb.like(root.get("disputeType"), value),
                    cb.like(root.get("status"), value),
                    cb.like(root.get("description"), value)
            };
            return cb.or(predicates);
        };
    }

    private Specification<EmployeeDispute> myKeywordSpec(Long userId, String keyword) {
        return (root, query, cb) -> {
            Predicate predicate = cb.equal(root.get("employeeId"), userId);
            if (keyword == null || keyword.isBlank()) {
                return predicate;
            }
            String value = "%" + keyword.trim() + "%";
            return cb.and(predicate, cb.or(
                    cb.like(root.get("serialNo"), value),
                    cb.like(root.get("employeeName"), value),
                    cb.like(root.get("disputeType"), value),
                    cb.like(root.get("status"), value),
                    cb.like(root.get("description"), value)
            ));
        };
    }

    private String resolveStatus(String requestedStatus, boolean adminOrHr, String currentStatus) {
        if (!adminOrHr) {
            return currentStatus == null || currentStatus.isBlank() ? "待处理" : currentStatus;
        }
        return requestedStatus == null || requestedStatus.isBlank() ? "待处理" : requestedStatus.trim();
    }

    private String nextSerialNo() {
        String prefix = "DP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long next = employeeDisputeRepository.findTopByOrderByIdDesc().map(EmployeeDispute::getId).orElse(0L) + 1;
        return prefix + String.format("%04d", next);
    }

    private static boolean isAdmin(LoginUser user) {
        return user != null && "ADMIN".equals(user.getRoleCode());
    }

    private static boolean isHr(LoginUser user) {
        return user != null && "HR".equals(user.getRoleCode());
    }

    private static void assertAdminOrHr(LoginUser user) {
        if (!isAdmin(user) && !isHr(user)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }
}
