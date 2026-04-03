package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.RecruitmentPosition;
import com.hrms.entity.RecruitmentRequirement;
import com.hrms.repository.RecruitmentCandidateRepository;
import com.hrms.repository.RecruitmentPositionRepository;
import com.hrms.repository.RecruitmentRequirementRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/recruitment/requirements")
@RequiredArgsConstructor
public class RecruitmentRequirementController {

    private final RecruitmentRequirementRepository requirementRepository;
    private final RecruitmentPositionRepository positionRepository;
    private final RecruitmentCandidateRepository candidateRepository;
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
        Page<RecruitmentRequirement> result = requirementRepository.findAll(
                spec(keyword, status),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        return ApiResponse.ok(pageData(result, pageNo, pageSize));
    }

    @PostMapping
    public ApiResponse<RecruitmentRequirement> create(@RequestBody RequirementSave body,
                                                      @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        RecruitmentRequirement requirement = new RecruitmentRequirement();
        apply(body, requirement, loginUser);
        requirement.setRequirementCode(nextRequirementCode());
        return ApiResponse.ok(requirementRepository.save(requirement));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<RecruitmentRequirement> update(@PathVariable Long id,
                                                      @RequestBody RequirementSave body,
                                                      @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        RecruitmentRequirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("招聘需求不存在"));
        apply(body, requirement, loginUser);
        RecruitmentRequirement saved = requirementRepository.save(requirement);
        syncLinkedPositions(saved);
        return ApiResponse.ok(saved);
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ApiResponse<RecruitmentRequirement> approve(@PathVariable Long id,
                                                       @RequestParam String status,
                                                       @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        if (!"已通过".equals(status) && !"已驳回".equals(status)) {
            throw new IllegalArgumentException("审批状态不合法");
        }
        RecruitmentRequirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("招聘需求不存在"));
        requirement.setStatus(status);
        requirement.setApproverName(resolveDisplayName(loginUser));
        requirement.setApproveTime(Instant.now());
        return ApiResponse.ok(requirementRepository.save(requirement));
    }

    @PostMapping("/{id}/generate-position")
    @Transactional
    public ApiResponse<RecruitmentPosition> generatePosition(@PathVariable Long id,
                                                             @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        RecruitmentRequirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("招聘需求不存在"));
        if (!"已通过".equals(requirement.getStatus()) && !"招聘中".equals(requirement.getStatus()) && !"已完成".equals(requirement.getStatus())) {
            throw new IllegalArgumentException("只有已通过或招聘中的需求才能生成职位");
        }
        RecruitmentPosition position = new RecruitmentPosition();
        position.setPositionCode(nextPositionCode());
        position.setPositionName(requirement.getJobTitle());
        position.setRequirement(requirement);
        position.setDepartmentId(requirement.getDepartmentId());
        position.setPlannedCount(requirement.getHeadcount());
        position.setFilledCount(requirement.getCompletedCount());
        position.setJobDescription(requirement.getJobDescription());
        position.setJobRequirements(requirement.getReason());
        position.setStatus("招聘中");
        position.setPublishTime(Instant.now());
        RecruitmentPosition saved = positionRepository.save(position);
        requirement.setStatus("招聘中");
        requirementRepository.save(requirement);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        requirementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("招聘需求不存在"));
        candidateRepository.deleteByPosition_Requirement_Id(id);
        positionRepository.deleteByRequirement_Id(id);
        requirementRepository.deleteById(id);
        return ApiResponse.ok();
    }

    void syncRequirementProgress(RecruitmentPosition position) {
        RecruitmentRequirement requirement = position.getRequirement();
        if (requirement == null) {
            return;
        }
        int completed = position.getFilledCount() != null ? position.getFilledCount() : 0;
        requirement.setCompletedCount(completed);
        int planned = requirement.getHeadcount() != null ? requirement.getHeadcount() : 0;
        if (completed >= planned && planned > 0) {
            requirement.setStatus("已完成");
        } else if ("已通过".equals(requirement.getStatus()) || "招聘中".equals(requirement.getStatus()) || "已完成".equals(requirement.getStatus())) {
            requirement.setStatus("招聘中");
        }
        requirementRepository.save(requirement);
    }

    private void syncLinkedPositions(RecruitmentRequirement requirement) {
        positionRepository.findByRequirement_Id(requirement.getId()).forEach(position -> {
            position.setPositionName(requirement.getJobTitle());
            position.setDepartmentId(requirement.getDepartmentId());
            position.setPlannedCount(requirement.getHeadcount());
            position.setJobDescription(requirement.getJobDescription());
            position.setJobRequirements(requirement.getReason());
            positionRepository.save(position);
        });
    }

    private void apply(RequirementSave body, RecruitmentRequirement requirement, LoginUser loginUser) {
        if (body.getJobTitle() == null || body.getJobTitle().isBlank()) {
            throw new IllegalArgumentException("岗位名称不能为空");
        }
        requirement.setJobTitle(body.getJobTitle().trim());
        requirement.setDepartmentId(body.getDepartmentId());
        requirement.setHeadcount(body.getHeadcount() != null ? Math.max(body.getHeadcount(), 1) : 1);
        requirement.setBudgetSalaryMin(n(body.getBudgetSalaryMin()));
        requirement.setBudgetSalaryMax(n(body.getBudgetSalaryMax()));
        requirement.setExpectedOnboardDate(body.getExpectedOnboardDate() != null ? LocalDate.parse(body.getExpectedOnboardDate()) : null);
        requirement.setJobDescription(body.getJobDescription());
        requirement.setReason(body.getReason());
        requirement.setRemark(body.getRemark());
        requirement.setApplicantUserId(loginUser != null ? loginUser.getUserId() : null);
        requirement.setApplicantName(resolveDisplayName(loginUser));
        if (body.getStatus() != null && !body.getStatus().isBlank()) {
            requirement.setStatus(body.getStatus());
        }
    }

    private Specification<RecruitmentRequirement> spec(String keyword, String status) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (status != null && !status.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("requirementCode"), value),
                        cb.like(root.get("jobTitle"), value),
                        cb.like(root.get("reason"), value),
                        cb.like(root.get("status"), value),
                        cb.like(root.get("applicantName"), value),
                        cb.like(root.get("approverName"), value)
                ));
            }
            return predicate;
        };
    }

    private Map<String, Object> pageData(Page<RecruitmentRequirement> result, int pageNo, int pageSize) {
        result.getContent().forEach(this::syncApplicantDisplayName);
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return data;
    }

    private void syncApplicantDisplayName(RecruitmentRequirement requirement) {
        if (requirement.getApplicantUserId() == null) {
            return;
        }
        userRepository.findById(requirement.getApplicantUserId()).ifPresent(user -> {
            String displayName = user.getRealName() != null && !user.getRealName().isBlank() ? user.getRealName() : user.getUsername();
            requirement.setApplicantName(displayName);
        });
    }

    private String nextRequirementCode() {
        String prefix = "REQ" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long next = requirementRepository.findTopByOrderByIdDesc().map(RecruitmentRequirement::getId).orElse(0L) + 1;
        return prefix + String.format("%04d", next);
    }

    private String nextPositionCode() {
        String prefix = "POS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long next = positionRepository.findTopByOrderByIdDesc().map(RecruitmentPosition::getId).orElse(0L) + 1;
        return prefix + String.format("%04d", next);
    }

    private String resolveDisplayName(LoginUser loginUser) {
        return userRepository.findById(loginUser.getUserId())
                .map(user -> user.getRealName() != null && !user.getRealName().isBlank() ? user.getRealName() : user.getUsername())
                .orElse(loginUser.getUsername());
    }

    private BigDecimal n(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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
    public static class RequirementSave {
        private String jobTitle;
        private Long departmentId;
        private Integer headcount;
        private BigDecimal budgetSalaryMin;
        private BigDecimal budgetSalaryMax;
        private String expectedOnboardDate;
        private String jobDescription;
        private String reason;
        private String status;
        private String remark;
    }
}
