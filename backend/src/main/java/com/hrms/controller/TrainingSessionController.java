package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.TrainingSession;
import com.hrms.repository.DepartmentRepository;
import com.hrms.repository.TrainingSessionRepository;
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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/training/sessions")
@RequiredArgsConstructor
public class TrainingSessionController {

    private final TrainingSessionRepository trainingSessionRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertLoggedIn(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Long departmentId = resolveVisibleDepartmentId(loginUser);
        Page<TrainingSession> result = trainingSessionRepository.findAll(listSpec(keyword, departmentId),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "trainingTime", "id")));
        result.getContent().forEach(this::enrichDepartmentName);
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<TrainingSession> create(@RequestBody TrainingSession body,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        TrainingSession session = new TrainingSession();
        apply(body, session);
        TrainingSession saved = trainingSessionRepository.save(session);
        enrichDepartmentName(saved);
        return ApiResponse.ok(saved);
    }

    @PutMapping("/{id}")
    public ApiResponse<TrainingSession> update(@PathVariable Long id,
                                               @RequestBody TrainingSession body,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        TrainingSession session = trainingSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("培训记录不存在"));
        apply(body, session);
        TrainingSession saved = trainingSessionRepository.save(session);
        enrichDepartmentName(saved);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        trainingSessionRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void apply(TrainingSession body, TrainingSession session) {
        if (body.getTitle() == null || body.getTitle().isBlank()) {
            throw new IllegalArgumentException("请输入培训主题");
        }
        if (body.getTrainingTime() == null) {
            throw new IllegalArgumentException("请选择培训时间");
        }
        session.setTitle(body.getTitle().trim());
        session.setTrainerName(body.getTrainerName() == null ? "" : body.getTrainerName().trim());
        session.setDepartmentId(body.getDepartmentId());
        session.setTrainingTime(body.getTrainingTime());
        session.setStatus(body.getStatus() == null || body.getStatus().isBlank() ? "待开展" : body.getStatus().trim());
        session.setContent(body.getContent());
    }

    private void enrichDepartmentName(TrainingSession session) {
        if (session.getDepartmentId() == null) {
            session.setDepartmentName("");
            return;
        }
        String name = departmentRepository.findById(session.getDepartmentId())
                .map(com.hrms.entity.Department::getName)
                .orElse("");
        session.setDepartmentName(name);
    }

    private Specification<TrainingSession> listSpec(String keyword, Long departmentId) {
        return (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("departmentId"), departmentId));
            }
            if (keyword == null || keyword.isBlank()) {
                return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
            }
            String value = "%" + keyword.trim() + "%";
            Predicate[] keywordPredicates = new Predicate[] {
                    cb.like(root.get("title"), value),
                    cb.like(root.get("trainerName"), value),
                    cb.like(root.get("status"), value),
                    cb.like(root.get("content"), value)
            };
            predicates.add(cb.or(keywordPredicates));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Long resolveVisibleDepartmentId(LoginUser loginUser) {
        if (isAdmin(loginUser) || isHr(loginUser)) {
            return null;
        }
        return userRepository.findById(loginUser.getUserId())
                .map(com.hrms.entity.User::getDepartmentId)
                .orElse(null);
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
