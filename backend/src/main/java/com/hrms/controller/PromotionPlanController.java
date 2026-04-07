package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.PerformanceReview;
import com.hrms.entity.PromotionPlan;
import com.hrms.entity.User;
import com.hrms.repository.PerformanceReviewRepository;
import com.hrms.repository.PromotionPlanRepository;
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/training/promotions")
@RequiredArgsConstructor
public class PromotionPlanController {

    private static final String STATUS_PENDING = "\u5f85\u8bc4\u4f30";
    private static final String PERF_STATUS_CONFIRMED = "\u5df2\u786e\u8ba4";
    private static final String PERF_STATUS_ARCHIVED = "\u5df2\u5f52\u6863";
    private static final String GRADE_A = "A";

    private final PromotionPlanRepository promotionPlanRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Page<PromotionPlan> result = promotionPlanRepository.findAll(
                keywordSpec(keyword),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "plannedDate", "id"))
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return ApiResponse.ok(data);
    }

    @GetMapping("/eligible-employees")
    public ApiResponse<List<Map<String, Object>>> eligibleEmployees(@AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);

        Map<Long, PerformanceReview> latestReviewByEmployee = new LinkedHashMap<>();
        for (PerformanceReview review : performanceReviewRepository.findAllByOrderByEmployeeIdAscIdDesc()) {
            if (review.getEmployeeId() != null && !latestReviewByEmployee.containsKey(review.getEmployeeId())) {
                latestReviewByEmployee.put(review.getEmployeeId(), review);
            }
        }

        List<Map<String, Object>> data = userRepository.findAll().stream()
                .filter(user -> {
                    PerformanceReview latestReview = latestReviewByEmployee.get(user.getId());
                    return latestReview != null
                            && GRADE_A.equalsIgnoreCase(trimToEmpty(latestReview.getGrade()))
                            && isEligiblePerformanceStatus(latestReview.getStatus());
                })
                .sorted(Comparator.comparing(User::getId))
                .map(user -> {
                    PerformanceReview latestReview = latestReviewByEmployee.get(user.getId());
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", user.getId());
                    item.put("realName", user.getRealName());
                    item.put("employeeNo", user.getEmployeeNo());
                    item.put("departmentId", user.getDepartmentId());
                    item.put("currentPosition", trimToEmpty(user.getPositionName()).isEmpty()
                            ? (user.getRole() != null ? user.getRole().getName() : "")
                            : user.getPositionName());
                    item.put("latestGrade", latestReview != null ? latestReview.getGrade() : "");
                    item.put("latestAssessmentPeriod", latestReview != null ? latestReview.getAssessmentPeriod() : "");
                    return item;
                })
                .toList();
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<PromotionPlan> create(@RequestBody PromotionPlan body,
                                             @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        PromotionPlan plan = new PromotionPlan();
        apply(body, plan);
        return ApiResponse.ok(promotionPlanRepository.save(plan));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromotionPlan> update(@PathVariable Long id,
                                             @RequestBody PromotionPlan body,
                                             @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        PromotionPlan plan = promotionPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("\u664b\u5347\u89c4\u5212\u4e0d\u5b58\u5728"));
        apply(body, plan);
        return ApiResponse.ok(promotionPlanRepository.save(plan));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        promotionPlanRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void apply(PromotionPlan body, PromotionPlan plan) {
        if (body.getEmployeeId() == null) {
            throw new IllegalArgumentException("\u8bf7\u9009\u62e9\u5458\u5de5");
        }
        if (body.getEmployeeName() == null || body.getEmployeeName().isBlank()) {
            throw new IllegalArgumentException("\u5458\u5de5\u59d3\u540d\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (body.getTargetPosition() == null || body.getTargetPosition().isBlank()) {
            throw new IllegalArgumentException("\u8bf7\u8f93\u5165\u76ee\u6807\u5c97\u4f4d");
        }
        if (body.getPlannedDate() == null) {
            throw new IllegalArgumentException("\u8bf7\u9009\u62e9\u8ba1\u5212\u65e5\u671f");
        }
        assertEligibleEmployee(body.getEmployeeId());
        plan.setEmployeeId(body.getEmployeeId());
        plan.setEmployeeName(body.getEmployeeName().trim());
        plan.setCurrentPosition(trimToEmpty(body.getCurrentPosition()));
        plan.setTargetPosition(body.getTargetPosition().trim());
        plan.setPlannedDate(body.getPlannedDate());
        plan.setStatus(body.getStatus() == null || body.getStatus().isBlank() ? STATUS_PENDING : body.getStatus().trim());
        plan.setRemark(body.getRemark());
    }

    private void assertEligibleEmployee(Long employeeId) {
        PerformanceReview latestReview = performanceReviewRepository.findTopByEmployeeIdOrderByIdDesc(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("\u8be5\u5458\u5de5\u6682\u65e0\u7ee9\u6548\u8003\u6838\u8bb0\u5f55\uff0c\u4e0d\u80fd\u65b0\u589e\u664b\u5347\u89c4\u5212"));
        if (!GRADE_A.equalsIgnoreCase(trimToEmpty(latestReview.getGrade()))) {
            throw new IllegalArgumentException("\u53ea\u80fd\u4ece\u6700\u8fd1\u4e00\u6b21\u7ee9\u6548\u8003\u6838\u7b49\u7ea7\u4e3a A \u7684\u5458\u5de5\u4e2d\u9009\u62e9\u664b\u5347\u5bf9\u8c61");
        }
        if (!isEligiblePerformanceStatus(latestReview.getStatus())) {
            throw new IllegalArgumentException("\u53ea\u6709\u7ee9\u6548\u72b6\u6001\u4e3a\u5df2\u786e\u8ba4\u6216\u5df2\u5f52\u6863\u7684\u5458\u5de5\u624d\u53ef\u52a0\u5165\u664b\u5347\u89c4\u5212");
        }
    }

    private Specification<PromotionPlan> keywordSpec(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String value = "%" + keyword.trim() + "%";
            Predicate[] predicates = new Predicate[]{
                    cb.like(root.get("employeeName"), value),
                    cb.like(root.get("currentPosition"), value),
                    cb.like(root.get("targetPosition"), value),
                    cb.like(root.get("status"), value)
            };
            return cb.or(predicates);
        };
    }

    private static boolean isAdmin(LoginUser user) {
        return user != null && "ADMIN".equals(user.getRoleCode());
    }

    private static boolean isHr(LoginUser user) {
        return user != null && "HR".equals(user.getRoleCode());
    }

    private static void assertAdminOrHr(LoginUser user) {
        if (!isAdmin(user) && !isHr(user)) {
            throw new AccessDeniedException("\u9700\u8981\u7ba1\u7406\u5458\u6216\u4eba\u4e8b\u6743\u9650");
        }
    }

    private static boolean isEligiblePerformanceStatus(String status) {
        String normalized = trimToEmpty(status);
        return PERF_STATUS_CONFIRMED.equals(normalized) || PERF_STATUS_ARCHIVED.equals(normalized);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
