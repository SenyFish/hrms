package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.RecruitmentPosition;
import com.hrms.entity.RecruitmentRequirement;
import com.hrms.repository.RecruitmentPositionRepository;
import com.hrms.repository.RecruitmentRequirementRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/recruitment/positions")
@RequiredArgsConstructor
public class RecruitmentPositionController {

    private final RecruitmentPositionRepository positionRepository;
    private final RecruitmentRequirementRepository requirementRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertRecruitmentViewer(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        String statusFilter = isEmployee(loginUser) ? "招聘中" : status;
        Page<RecruitmentPosition> result = positionRepository.findAll(
                spec(keyword, statusFilter),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<RecruitmentPosition> create(@RequestBody PositionSave body,
                                                   @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        RecruitmentPosition position = new RecruitmentPosition();
        position.setPositionCode(nextPositionCode());
        apply(body, position);
        return ApiResponse.ok(positionRepository.save(position));
    }

    @PutMapping("/{id}")
    public ApiResponse<RecruitmentPosition> update(@PathVariable Long id,
                                                   @RequestBody PositionSave body,
                                                   @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        RecruitmentPosition position = positionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("招聘职位不存在"));
        apply(body, position);
        return ApiResponse.ok(positionRepository.save(position));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<RecruitmentPosition> updateStatus(@PathVariable Long id,
                                                         @RequestParam String status,
                                                         @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        RecruitmentPosition position = positionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("招聘职位不存在"));
        position.setStatus(status);
        if ("招聘中".equals(status) && position.getPublishTime() == null) {
            position.setPublishTime(Instant.now());
        }
        if ("已关闭".equals(status) || "已招满".equals(status)) {
            position.setCloseTime(Instant.now());
        } else {
            position.setCloseTime(null);
        }
        return ApiResponse.ok(positionRepository.save(position));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        positionRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void apply(PositionSave body, RecruitmentPosition position) {
        if (body.getPositionName() == null || body.getPositionName().isBlank()) {
            throw new IllegalArgumentException("职位名称不能为空");
        }
        position.setPositionName(body.getPositionName().trim());
        position.setDepartmentId(body.getDepartmentId());
        position.setPlannedCount(body.getPlannedCount() != null ? Math.max(body.getPlannedCount(), 1) : 1);
        position.setFilledCount(body.getFilledCount() != null ? Math.max(body.getFilledCount(), 0) : 0);
        position.setJobDescription(body.getJobDescription());
        position.setJobRequirements(body.getJobRequirements());
        position.setRemark(body.getRemark());
        position.setStatus(body.getStatus() != null && !body.getStatus().isBlank()
                ? body.getStatus().trim()
                : (position.getStatus() != null ? position.getStatus() : "待发布"));
        if (body.getRequirementId() != null) {
            RecruitmentRequirement requirement = requirementRepository.findById(body.getRequirementId())
                    .orElseThrow(() -> new IllegalArgumentException("招聘需求不存在"));
            position.setRequirement(requirement);
        } else {
            position.setRequirement(null);
        }
        if ("招聘中".equals(position.getStatus()) && position.getPublishTime() == null) {
            position.setPublishTime(Instant.now());
        }
        if (!"已关闭".equals(position.getStatus()) && !"已招满".equals(position.getStatus())) {
            position.setCloseTime(null);
        }
    }

    private Specification<RecruitmentPosition> spec(String keyword, String status) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (status != null && !status.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("positionCode"), value),
                        cb.like(root.get("positionName"), value),
                        cb.like(root.get("jobDescription"), value),
                        cb.like(root.get("jobRequirements"), value),
                        cb.like(root.get("status"), value)
                ));
            }
            return predicate;
        };
    }

    private String nextPositionCode() {
        String prefix = "POS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long next = positionRepository.findTopByOrderByIdDesc().map(RecruitmentPosition::getId).orElse(0L) + 1;
        return prefix + String.format("%04d", next);
    }

    private static boolean isAdmin(LoginUser u) {
        return u != null && "ADMIN".equals(u.getRoleCode());
    }

    private static boolean isHr(LoginUser u) {
        return u != null && "HR".equals(u.getRoleCode());
    }

    private static boolean isEmployee(LoginUser u) {
        return u != null && "EMP".equals(u.getRoleCode());
    }

    private static void assertAdminOrHr(LoginUser u) {
        if (!isAdmin(u) && !isHr(u)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }

    private static void assertRecruitmentViewer(LoginUser u) {
        if (!isAdmin(u) && !isHr(u) && !isEmployee(u)) {
            throw new AccessDeniedException("没有招聘模块访问权限");
        }
    }

    @Data
    public static class PositionSave {
        private Long requirementId;
        private String positionName;
        private Long departmentId;
        private Integer plannedCount;
        private Integer filledCount;
        private String jobDescription;
        private String jobRequirements;
        private String status;
        private String remark;
    }
}
