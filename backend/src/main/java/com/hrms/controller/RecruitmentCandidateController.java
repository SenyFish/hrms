package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.RecruitmentCandidate;
import com.hrms.entity.RecruitmentPosition;
import com.hrms.entity.RecruitmentRequirement;
import com.hrms.entity.User;
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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/recruitment/candidates")
@RequiredArgsConstructor
public class RecruitmentCandidateController {

    private final RecruitmentCandidateRepository candidateRepository;
    private final RecruitmentPositionRepository positionRepository;
    private final RecruitmentRequirementRepository requirementRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertRecruitmentViewer(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Long referrerId = isEmployee(loginUser) ? loginUser.getUserId() : null;
        Page<RecruitmentCandidate> result = candidateRepository.findAll(
                spec(keyword, status, referrerId),
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
    @Transactional
    public ApiResponse<RecruitmentCandidate> create(@RequestBody CandidateSave body,
                                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertRecruitmentEditor(loginUser);
        RecruitmentCandidate candidate = new RecruitmentCandidate();
        if (isEmployee(loginUser)) {
            applyEmployeeReferral(body, candidate, loginUser);
        } else {
            applyAdminOrHr(body, candidate);
        }
        RecruitmentCandidate saved = candidateRepository.save(candidate);
        syncPositionProgress(saved.getPosition());
        return ApiResponse.ok(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<RecruitmentCandidate> update(@PathVariable Long id,
                                                    @RequestBody CandidateSave body,
                                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertRecruitmentEditor(loginUser);
        RecruitmentCandidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("候选人不存在"));
        RecruitmentPosition previousPosition = candidate.getPosition();
        if (isEmployee(loginUser)) {
            assertOwnReferral(candidate, loginUser);
            applyEmployeeReferral(body, candidate, loginUser);
        } else {
            applyAdminOrHr(body, candidate);
        }
        RecruitmentCandidate saved = candidateRepository.save(candidate);
        syncPositionProgress(previousPosition);
        syncPositionProgress(saved.getPosition());
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertRecruitmentEditor(loginUser);
        RecruitmentCandidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("候选人不存在"));
        if (isEmployee(loginUser)) {
            assertOwnReferral(candidate, loginUser);
            if ("已入职".equals(candidate.getStatus())) {
                throw new IllegalArgumentException("已入职候选人不能删除");
            }
        }
        RecruitmentPosition position = candidate.getPosition();
        candidateRepository.deleteById(id);
        syncPositionProgress(position);
        return ApiResponse.ok();
    }

    private void applyAdminOrHr(CandidateSave body, RecruitmentCandidate candidate) {
        if (body.getName() == null || body.getName().isBlank()) {
            throw new IllegalArgumentException("候选人姓名不能为空");
        }
        candidate.setName(body.getName().trim());
        candidate.setPhone(trimToNull(body.getPhone()));
        candidate.setEmail(trimToNull(body.getEmail()));
        candidate.setEducation(trimToNull(body.getEducation()));
        candidate.setSourceChannel(hasText(body.getSourceChannel()) ? body.getSourceChannel().trim() : "社会招聘");
        candidate.setExpectedSalary(body.getExpectedSalary() != null ? body.getExpectedSalary() : BigDecimal.ZERO);
        candidate.setInterviewerName(trimToNull(body.getInterviewerName()));
        candidate.setInterviewTime(parseInstant(body.getInterviewTime()));
        candidate.setStatus(hasText(body.getStatus()) ? body.getStatus().trim() : "待筛选");
        candidate.setResult(trimToNull(body.getResult()));
        candidate.setResumeRemark(trimToNull(body.getResumeRemark()));
        candidate.setRemark(trimToNull(body.getRemark()));
        if (body.getReferrerId() != null) {
            User referrer = userRepository.findById(body.getReferrerId())
                    .orElseThrow(() -> new IllegalArgumentException("内推员工不存在"));
            candidate.setReferrerId(referrer.getId());
            candidate.setReferrerName(referrer.getRealName());
            candidate.setReferrerEmployeeNo(referrer.getEmployeeNo());
            if (candidate.getReferralTime() == null) {
                candidate.setReferralTime(Instant.now());
            }
        } else if (!"员工内推".equals(candidate.getSourceChannel())) {
            candidate.setReferrerId(null);
            candidate.setReferrerName(null);
            candidate.setReferrerEmployeeNo(null);
            candidate.setReferralTime(null);
        }
        candidate.setPosition(resolvePosition(body.getPositionId(), false));
    }

    private void applyEmployeeReferral(CandidateSave body, RecruitmentCandidate candidate, LoginUser loginUser) {
        if (body.getName() == null || body.getName().isBlank()) {
            throw new IllegalArgumentException("请输入候选人姓名");
        }
        if (body.getPositionId() == null) {
            throw new IllegalArgumentException("请选择应聘职位");
        }
        User referrer = userRepository.findById(loginUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("当前员工不存在"));
        candidate.setName(body.getName().trim());
        candidate.setPhone(trimToNull(body.getPhone()));
        candidate.setEmail(trimToNull(body.getEmail()));
        candidate.setEducation(trimToNull(body.getEducation()));
        candidate.setSourceChannel("员工内推");
        candidate.setExpectedSalary(body.getExpectedSalary() != null ? body.getExpectedSalary() : BigDecimal.ZERO);
        candidate.setResumeRemark(trimToNull(body.getResumeRemark()));
        candidate.setRemark(trimToNull(body.getRemark()));
        candidate.setPosition(resolvePosition(body.getPositionId(), true));
        candidate.setReferrerId(referrer.getId());
        candidate.setReferrerName(referrer.getRealName());
        candidate.setReferrerEmployeeNo(referrer.getEmployeeNo());
        if (candidate.getReferralTime() == null) {
            candidate.setReferralTime(Instant.now());
        }
        if (!"已入职".equals(candidate.getStatus())) {
            candidate.setStatus("待筛选");
        }
        if (candidate.getResult() == null || candidate.getResult().isBlank()) {
            candidate.setResult("待处理");
        }
    }

    private RecruitmentPosition resolvePosition(Long positionId, boolean employeeOnlyOpen) {
        if (positionId == null) {
            return null;
        }
        RecruitmentPosition position = positionRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("招聘职位不存在"));
        if (employeeOnlyOpen && !"招聘中".equals(position.getStatus())) {
            throw new IllegalArgumentException("当前职位暂不接受内推");
        }
        return position;
    }

    private void syncPositionProgress(RecruitmentPosition position) {
        if (position == null || position.getId() == null) {
            return;
        }
        RecruitmentPosition latest = positionRepository.findById(position.getId()).orElse(position);
        int filled = (int) candidateRepository.findByPosition_Id(latest.getId()).stream()
                .filter(item -> "已入职".equals(item.getStatus()))
                .count();
        latest.setFilledCount(filled);
        int planned = latest.getPlannedCount() != null ? latest.getPlannedCount() : 0;
        if (planned > 0 && filled >= planned) {
            latest.setStatus("已招满");
            latest.setCloseTime(Instant.now());
        } else if (!"已关闭".equals(latest.getStatus())) {
            latest.setStatus("招聘中");
            latest.setCloseTime(null);
        }
        RecruitmentPosition saved = positionRepository.save(latest);
        RecruitmentRequirement requirement = saved.getRequirement();
        if (requirement != null) {
            int completed = positionRepository.findByRequirement_Id(requirement.getId()).stream()
                    .mapToInt(item -> item.getFilledCount() != null ? item.getFilledCount() : 0)
                    .sum();
            requirement.setCompletedCount(completed);
            int need = requirement.getHeadcount() != null ? requirement.getHeadcount() : 0;
            if (need > 0 && completed >= need) {
                requirement.setStatus("已完成");
            } else if (!"已驳回".equals(requirement.getStatus())) {
                requirement.setStatus("招聘中");
            }
            requirementRepository.save(requirement);
        }
    }

    private Specification<RecruitmentCandidate> spec(String keyword, String status, Long referrerId) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (referrerId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("referrerId"), referrerId));
            }
            if (status != null && !status.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String value = "%" + keyword.trim() + "%";
                Predicate positionMatch = cb.like(root.join("position", jakarta.persistence.criteria.JoinType.LEFT).get("positionName"), value);
                predicate = cb.and(predicate, cb.or(
                        cb.like(root.get("name"), value),
                        cb.like(root.get("phone"), value),
                        cb.like(root.get("email"), value),
                        cb.like(root.get("sourceChannel"), value),
                        cb.like(root.get("status"), value),
                        cb.like(root.get("result"), value),
                        cb.like(root.get("referrerName"), value),
                        positionMatch
                ));
            }
            return predicate;
        };
    }

    private void assertOwnReferral(RecruitmentCandidate candidate, LoginUser loginUser) {
        if (candidate.getReferrerId() == null || !candidate.getReferrerId().equals(loginUser.getUserId())) {
            throw new AccessDeniedException("只能操作本人提交的内推记录");
        }
    }

    private static Instant parseInstant(String value) {
        return hasText(value) ? Instant.parse(value.trim()) : null;
    }

    private static String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
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

    private static boolean isEmployee(LoginUser u) {
        return u != null && "EMP".equals(u.getRoleCode());
    }

    private static void assertRecruitmentViewer(LoginUser u) {
        if (!isAdmin(u) && !isHr(u) && !isEmployee(u)) {
            throw new AccessDeniedException("没有招聘模块访问权限");
        }
    }

    private static void assertRecruitmentEditor(LoginUser u) {
        if (!isAdmin(u) && !isHr(u) && !isEmployee(u)) {
            throw new AccessDeniedException("没有招聘模块操作权限");
        }
    }

    @Data
    public static class CandidateSave {
        private String name;
        private String phone;
        private String email;
        private String education;
        private String sourceChannel;
        private Long positionId;
        private BigDecimal expectedSalary;
        private String interviewTime;
        private String interviewerName;
        private String status;
        private String result;
        private String resumeRemark;
        private String remark;
        private Long referrerId;
    }
}
