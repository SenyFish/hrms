package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.BusinessTripRequest;
import com.hrms.entity.Department;
import com.hrms.entity.User;
import com.hrms.repository.BusinessTripRequestRepository;
import com.hrms.repository.DepartmentRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance/trips")
@RequiredArgsConstructor
public class BusinessTripController {

    private final BusinessTripRequestRepository businessTripRequestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Page<BusinessTripRequest> result = businessTripRequestRepository.findAll(
                adminSpec(status, keyword),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        return ApiResponse.ok(pageData(result, pageNo, pageSize));
    }

    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> my(@RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Page<BusinessTripRequest> result = businessTripRequestRepository.findAll(
                mySpec(loginUser.getUserId(), keyword),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        return ApiResponse.ok(pageData(result, pageNo, pageSize));
    }

    @PostMapping
    @Transactional
    public ApiResponse<BusinessTripRequest> create(@RequestBody TripApply body,
                                                   @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        User user = userRepository.findById(loginUser.getUserId()).orElseThrow();
        BusinessTripRequest request = new BusinessTripRequest();
        request.setSerialNo(nextSerialNo());
        request.setUser(user);
        fillUserInfo(request, user);
        applyForm(request, body);
        request.setStatus("待审批");
        return ApiResponse.ok(businessTripRequestRepository.save(request));
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ApiResponse<BusinessTripRequest> approve(@PathVariable Long id,
                                                    @RequestBody ApproveBody body,
                                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        BusinessTripRequest request = businessTripRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("出差申请不存在"));
        request.setStatus(Boolean.TRUE.equals(body.getApproved()) ? "已通过" : "已驳回");
        request.setApproverId(loginUser.getUserId());
        request.setApproverName(loginUser.getUsername());
        request.setApproveTime(Instant.now());
        request.setApproveRemark(body.getRemark());
        return ApiResponse.ok(businessTripRequestRepository.save(request));
    }

    private void applyForm(BusinessTripRequest request, TripApply body) {
        if (body.getTripType() == null || body.getTripType().isBlank()) {
            throw new IllegalArgumentException("请选择出差类型");
        }
        if (body.getDestination() == null || body.getDestination().isBlank()) {
            throw new IllegalArgumentException("请输入目的地");
        }
        if (body.getEstimatedDays() == null || body.getEstimatedDays().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("预计出差天数必须大于 0");
        }
        request.setTripType(body.getTripType().trim());
        request.setDestination(body.getDestination().trim());
        request.setEstimatedDays(body.getEstimatedDays().setScale(1, RoundingMode.HALF_UP));
        request.setEstimatedExpense(n(body.getEstimatedExpense()));
        request.setAttachmentFileId(body.getAttachmentFileId());
        request.setAttachmentName(trimToNull(body.getAttachmentName()));
        request.setAttachmentRemark(trimToNull(body.getAttachmentRemark()));
    }

    private void fillUserInfo(BusinessTripRequest request, User user) {
        request.setEmployeeName(user.getRealName());
        request.setEmployeeNo(user.getEmployeeNo());
        request.setDepartmentId(user.getDepartmentId());
        request.setDepartmentName(resolveDepartmentName(user.getDepartmentId()));
        request.setPositionName(user.getPositionName());
    }

    private String resolveDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return "";
        }
        return departmentRepository.findById(departmentId).map(Department::getName).orElse("");
    }

    private Specification<BusinessTripRequest> adminSpec(String status, String keyword) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (status != null && !status.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("serialNo"), value),
                        cb.like(root.get("employeeName"), value),
                        cb.like(root.get("employeeNo"), value),
                        cb.like(root.get("departmentName"), value),
                        cb.like(root.get("tripType"), value),
                        cb.like(root.get("destination"), value),
                        cb.like(root.get("status"), value)
                ));
            }
            return predicate;
        };
    }

    private Specification<BusinessTripRequest> mySpec(Long userId, String keyword) {
        return (root, query, cb) -> {
            Predicate predicate = cb.equal(root.join("user").get("id"), userId);
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("serialNo"), value),
                        cb.like(root.get("tripType"), value),
                        cb.like(root.get("destination"), value),
                        cb.like(root.get("status"), value),
                        cb.like(root.get("departmentName"), value)
                ));
            }
            return predicate;
        };
    }

    private Map<String, Object> pageData(Page<BusinessTripRequest> result, int pageNo, int pageSize) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return data;
    }

    private String nextSerialNo() {
        String prefix = "BT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long next = businessTripRequestRepository.findTopByOrderByIdDesc().map(BusinessTripRequest::getId).orElse(0L) + 1;
        return prefix + String.format("%04d", next);
    }

    private BigDecimal n(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
    public static class TripApply {
        private String tripType;
        private String destination;
        private BigDecimal estimatedDays;
        private BigDecimal estimatedExpense;
        private Long attachmentFileId;
        private String attachmentName;
        private String attachmentRemark;
    }

    @Data
    public static class ApproveBody {
        private Boolean approved;
        private String remark;
    }
}
