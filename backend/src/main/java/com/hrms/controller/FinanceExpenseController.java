package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.FinanceExpense;
import com.hrms.repository.FinanceExpenseRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/expenses")
@RequiredArgsConstructor
public class FinanceExpenseController {

    private final FinanceExpenseRepository financeExpenseRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Page<FinanceExpense> result = financeExpenseRepository.findAll(keywordSpec(keyword),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "expenseTime", "id")));

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<FinanceExpense> create(@RequestBody FinanceExpense body,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        FinanceExpense expense = new FinanceExpense();
        apply(body, expense);
        expense.setSerialNo(nextSerialNo());
        return ApiResponse.ok(financeExpenseRepository.save(expense));
    }

    @PutMapping("/{id}")
    public ApiResponse<FinanceExpense> update(@PathVariable Long id,
                                              @RequestBody FinanceExpense body,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        FinanceExpense expense = financeExpenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("财务支出记录不存在"));
        apply(body, expense);
        return ApiResponse.ok(financeExpenseRepository.save(expense));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        financeExpenseRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void apply(FinanceExpense body, FinanceExpense expense) {
        if (body.getDescription() == null || body.getDescription().isBlank()) {
            throw new IllegalArgumentException("支出说明不能为空");
        }
        expense.setDescription(body.getDescription().trim());
        expense.setAmount(body.getAmount() != null ? body.getAmount() : BigDecimal.ZERO);
        expense.setExpenseTime(body.getExpenseTime() != null ? body.getExpenseTime() : LocalDateTime.now());
        expense.setDepartmentId(body.getDepartmentId());
    }

    private String nextSerialNo() {
        String prefix = "EXP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long next = financeExpenseRepository.findTopByOrderByIdDesc()
                .map(FinanceExpense::getId)
                .orElse(0L) + 1;
        return prefix + String.format("%04d", next);
    }

    private Specification<FinanceExpense> keywordSpec(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String value = "%" + keyword.trim() + "%";
            Predicate[] predicates = new Predicate[]{
                    cb.like(root.get("serialNo"), value),
                    cb.like(root.get("description"), value)
            };
            return cb.or(predicates);
        };
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
}
