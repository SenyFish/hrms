package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.LeaveRequest;
import com.hrms.entity.User;
import com.hrms.repository.LeaveRequestRepository;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Page<LeaveRequest> result = leaveRequestRepository.findAll(
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
        Page<LeaveRequest> result = leaveRequestRepository.findAll(
                mySpec(loginUser.getUserId(), keyword),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        return ApiResponse.ok(pageData(result, pageNo, pageSize));
    }

    @PostMapping
    @Transactional
    public ApiResponse<LeaveRequest> apply(@RequestBody LeaveApply body, @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            throw new AccessDeniedException("未登录");
        }
        User u = userRepository.findById(loginUser.getUserId()).orElseThrow();
        LeaveRequest r = new LeaveRequest();
        r.setUser(u);
        r.setLeaveType(body.getLeaveType());
        r.setStartTime(Instant.parse(body.getStartTime()));
        r.setEndTime(Instant.parse(body.getEndTime()));
        r.setReason(body.getReason());
        r.setStatus("待审批");
        return ApiResponse.ok(leaveRequestRepository.save(r));
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ApiResponse<LeaveRequest> approve(@PathVariable Long id, @RequestBody ApproveBody body,
                                             @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        LeaveRequest r = leaveRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("申请不存在"));
        r.setStatus(Boolean.TRUE.equals(body.getApproved()) ? "已通过" : "已驳回");
        r.setApproverId(loginUser.getUserId());
        r.setApproveTime(Instant.now());
        r.setApproveRemark(body.getRemark());
        return ApiResponse.ok(leaveRequestRepository.save(r));
    }

    private Specification<LeaveRequest> adminSpec(String status, String keyword) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (status != null && !status.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("leaveType"), value),
                        cb.like(root.get("reason"), value),
                        cb.like(root.get("status"), value),
                        cb.like(root.join("user").get("realName"), value),
                        cb.like(root.join("user").get("employeeNo"), value)
                ));
            }
            return predicate;
        };
    }

    private Specification<LeaveRequest> mySpec(Long userId, String keyword) {
        return (root, query, cb) -> {
            Predicate predicate = cb.equal(root.join("user").get("id"), userId);
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("leaveType"), value),
                        cb.like(root.get("reason"), value),
                        cb.like(root.get("status"), value)
                ));
            }
            return predicate;
        };
    }

    private Map<String, Object> pageData(Page<LeaveRequest> result, int pageNo, int pageSize) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return data;
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
    public static class LeaveApply {
        private String leaveType;
        private String startTime;
        private String endTime;
        private String reason;
    }

    @Data
    public static class ApproveBody {
        private Boolean approved;
        private String remark;
    }
}
