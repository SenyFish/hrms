package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.PerformanceReview;
import com.hrms.entity.SalaryRecord;
import com.hrms.repository.PerformanceReviewRepository;
import com.hrms.repository.SalaryRecordRepository;
import com.hrms.security.LoginUser;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salary/performance")
@RequiredArgsConstructor
public class PerformanceReviewController {

    private final PerformanceReviewRepository performanceReviewRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Page<PerformanceReview> result = performanceReviewRepository.findAll(
                keywordSpec(keyword),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "assessmentPeriod", "id"))
        );
        result.getContent().forEach(this::enrichReview);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<PerformanceReview> create(@RequestBody PerformanceReview body,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        PerformanceReview review = new PerformanceReview();
        apply(body, review);
        PerformanceReview saved = performanceReviewRepository.save(review);
        enrichReview(saved);
        return ApiResponse.ok(saved);
    }

    @PutMapping("/{id}")
    public ApiResponse<PerformanceReview> update(@PathVariable Long id,
                                                 @RequestBody PerformanceReview body,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        PerformanceReview review = performanceReviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("绩效考核记录不存在"));
        apply(body, review);
        int affectedRows = jdbcTemplate.update(
                """
                update performance_review
                   set employee_id = ?,
                       employee_name = ?,
                       assessment_period = ?,
                       score = ?,
                       grade = ?,
                       status = ?,
                       evaluator_name = ?,
                       remark = ?
                 where id = ?
                """,
                review.getEmployeeId(),
                review.getEmployeeName(),
                review.getAssessmentPeriod(),
                review.getScore(),
                review.getGrade(),
                review.getStatus(),
                review.getEvaluatorName(),
                review.getRemark(),
                id
        );
        if (affectedRows <= 0) {
            throw new IllegalStateException("绩效考核记录更新失败");
        }
        PerformanceReview saved = performanceReviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("绩效考核记录不存在"));
        enrichReview(saved);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        performanceReviewRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/preview")
    public ApiResponse<Map<String, Object>> preview(@RequestParam Long employeeId,
                                                    @RequestParam BigDecimal score,
                                                    @RequestParam(required = false) Long currentId,
                                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);

        BigDecimal safeScore = score != null ? score : BigDecimal.ZERO;
        Map<String, Object> data = new HashMap<>();
        data.put("grade", resolveGrade(safeScore));
        data.put("annualBonusLevel", resolveBonusLevel(safeScore));
        data.put("annualBonusAmount", resolveAnnualBonus(employeeId, safeScore));

        List<PerformanceReview> history = performanceReviewRepository.findTop3ByEmployeeIdOrderByIdDesc(employeeId).stream()
                .filter(item -> currentId == null || !currentId.equals(item.getId()))
                .limit(2)
                .toList();
        data.put("scoreTrend", resolveScoreTrend(buildPreviewReviews(safeScore, history)));
        data.put("developmentSuggestion", resolveDevelopmentSuggestion(safeScore, buildPreviewReviews(safeScore, history)));
        return ApiResponse.ok(data);
    }

    private void apply(PerformanceReview body, PerformanceReview review) {
        if (body.getEmployeeId() == null) {
            throw new IllegalArgumentException("请选择员工");
        }
        if (body.getEmployeeName() == null || body.getEmployeeName().isBlank()) {
            throw new IllegalArgumentException("员工姓名不能为空");
        }
        if (body.getAssessmentPeriod() == null || body.getAssessmentPeriod().isBlank()) {
            throw new IllegalArgumentException("请输入考核周期");
        }

        BigDecimal score = body.getScore() == null ? BigDecimal.ZERO : body.getScore();
        review.setEmployeeId(body.getEmployeeId());
        review.setEmployeeName(body.getEmployeeName().trim());
        review.setAssessmentPeriod(body.getAssessmentPeriod().trim());
        review.setScore(score);
        review.setGrade(resolveGrade(score));
        review.setStatus(body.getStatus() == null || body.getStatus().isBlank() ? "待确认" : body.getStatus().trim());
        review.setEvaluatorName(body.getEvaluatorName() == null || body.getEvaluatorName().isBlank() ? "王人事" : body.getEvaluatorName().trim());
        review.setRemark(body.getRemark());
    }

    private void enrichReview(PerformanceReview review) {
        BigDecimal score = review.getScore() != null ? review.getScore() : BigDecimal.ZERO;
        review.setAnnualBonusLevel(resolveBonusLevel(score));
        review.setAnnualBonusAmount(resolveAnnualBonus(review.getEmployeeId(), score));

        List<PerformanceReview> latestReviews = performanceReviewRepository.findTop3ByEmployeeIdOrderByIdDesc(review.getEmployeeId());
        review.setScoreTrend(resolveScoreTrend(latestReviews));
        review.setDevelopmentSuggestion(resolveDevelopmentSuggestion(score, latestReviews));
    }

    private List<PerformanceReview> buildPreviewReviews(BigDecimal score, List<PerformanceReview> history) {
        PerformanceReview current = new PerformanceReview();
        current.setScore(score);

        List<PerformanceReview> reviews = new java.util.ArrayList<>();
        reviews.add(current);
        reviews.addAll(history);
        return reviews;
    }

    private BigDecimal resolveAnnualBonus(Long employeeId, BigDecimal score) {
        BigDecimal baseSalary = salaryRecordRepository.findTopByUser_IdOrderBySalaryMonthDescIdDesc(employeeId)
                .map(SalaryRecord::getBaseSalary)
                .orElse(BigDecimal.ZERO);
        return baseSalary.multiply(resolveBonusMultiplier(score)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveBonusMultiplier(BigDecimal score) {
        if (score.compareTo(new BigDecimal("95")) >= 0) return new BigDecimal("3.00");
        if (score.compareTo(new BigDecimal("90")) >= 0) return new BigDecimal("2.50");
        if (score.compareTo(new BigDecimal("85")) >= 0) return new BigDecimal("2.00");
        if (score.compareTo(new BigDecimal("80")) >= 0) return new BigDecimal("1.50");
        if (score.compareTo(new BigDecimal("70")) >= 0) return new BigDecimal("1.00");
        if (score.compareTo(new BigDecimal("60")) >= 0) return new BigDecimal("0.50");
        return BigDecimal.ZERO;
    }

    private String resolveBonusLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("95")) >= 0) return "特优年终奖";
        if (score.compareTo(new BigDecimal("90")) >= 0) return "优秀年终奖";
        if (score.compareTo(new BigDecimal("80")) >= 0) return "良好年终奖";
        if (score.compareTo(new BigDecimal("70")) >= 0) return "达标年终奖";
        if (score.compareTo(new BigDecimal("60")) >= 0) return "基础年终奖";
        return "无年终奖";
    }

    private String resolveGrade(BigDecimal score) {
        if (score.compareTo(new BigDecimal("90")) >= 0) return "A";
        if (score.compareTo(new BigDecimal("80")) >= 0) return "B";
        if (score.compareTo(new BigDecimal("70")) >= 0) return "C";
        return "D";
    }

    private String resolveScoreTrend(List<PerformanceReview> reviews) {
        if (reviews.isEmpty()) {
            return "暂无趋势";
        }
        if (reviews.size() == 1) {
            return "单次考核";
        }
        BigDecimal latest = scoreOf(reviews.get(0));
        BigDecimal previous = scoreOf(reviews.get(1));
        int compare = latest.compareTo(previous);
        if (compare > 0) return "持续提升";
        if (compare < 0) return "有所下滑";
        return "基本持平";
    }

    private String resolveDevelopmentSuggestion(BigDecimal score, List<PerformanceReview> reviews) {
        boolean threeHigh = reviews.size() >= 3 && reviews.stream().limit(3)
                .allMatch(item -> scoreOf(item).compareTo(new BigDecimal("90")) >= 0);
        if (threeHigh) {
            return "连续高分，建议优先升职、加薪";
        }

        boolean threeLow = reviews.size() >= 3 && reviews.stream().limit(3)
                .allMatch(item -> scoreOf(item).compareTo(new BigDecimal("60")) < 0);
        if (threeLow) {
            return "长期低分，建议调岗、降级";
        }

        if (score.compareTo(new BigDecimal("90")) >= 0) {
            return "本期表现突出，建议优先纳入调薪和晋升观察名单";
        }
        if (score.compareTo(new BigDecimal("80")) >= 0) {
            return "整体表现良好，建议持续培养并结合岗位评估加薪";
        }
        if (score.compareTo(new BigDecimal("60")) >= 0) {
            return "绩效达标但仍有提升空间，建议制定专项提升计划";
        }
        return "当前绩效偏低，建议先辅导改进并评估调岗风险";
    }

    private BigDecimal scoreOf(PerformanceReview review) {
        return review.getScore() != null ? review.getScore() : BigDecimal.ZERO;
    }

    private Specification<PerformanceReview> keywordSpec(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String value = "%" + keyword.trim() + "%";
            Predicate[] predicates = new Predicate[] {
                    cb.like(root.get("employeeName"), value),
                    cb.like(root.get("assessmentPeriod"), value),
                    cb.like(root.get("grade"), value),
                    cb.like(root.get("status"), value),
                    cb.like(root.get("evaluatorName"), value)
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
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }
}
