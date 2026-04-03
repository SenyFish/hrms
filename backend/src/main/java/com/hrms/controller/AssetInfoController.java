package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.AssetInfo;
import com.hrms.repository.AssetInfoRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/assets")
@RequiredArgsConstructor
public class AssetInfoController {

    private final AssetInfoRepository assetInfoRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        assertLoggedIn(loginUser);
        int pageNo = page != null && page > 0 ? page - 1 : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Page<AssetInfo> result = assetInfoRepository.findAll(keywordSpec(keyword),
                PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "id")));
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", pageNo + 1);
        data.put("size", pageSize);
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<AssetInfo> create(@RequestBody AssetInfo body, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        AssetInfo asset = new AssetInfo();
        apply(body, asset);
        asset.setAssetCode(nextAssetCode());
        return ApiResponse.ok(assetInfoRepository.save(asset));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetInfo> update(@PathVariable Long id,
                                         @RequestBody AssetInfo body,
                                         @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        AssetInfo asset = assetInfoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资产信息不存在"));
        apply(body, asset);
        return ApiResponse.ok(assetInfoRepository.save(asset));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertAdminOrHr(loginUser);
        assetInfoRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void apply(AssetInfo body, AssetInfo asset) {
        if (body.getAssetName() == null || body.getAssetName().isBlank()) {
            throw new IllegalArgumentException("资产名称不能为空");
        }
        asset.setAssetName(body.getAssetName().trim());
        asset.setCategory(body.getCategory());
        asset.setQuantity(body.getQuantity() != null ? body.getQuantity() : 1);
        asset.setUnitPrice(body.getUnitPrice() != null ? body.getUnitPrice() : BigDecimal.ZERO);
        asset.setPurchaseDate(body.getPurchaseDate() != null ? body.getPurchaseDate() : LocalDate.now());
        asset.setDepartmentId(body.getDepartmentId());
        asset.setStatus(body.getStatus() != null && !body.getStatus().isBlank() ? body.getStatus() : "在库");
        asset.setRemark(body.getRemark());
    }

    private String nextAssetCode() {
        String prefix = "AST" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long next = assetInfoRepository.findAll().stream()
                .map(AssetInfo::getId)
                .filter(x -> x != null)
                .max(Long::compareTo)
                .orElse(0L) + 1;
        return prefix + String.format("%04d", next);
    }

    private Specification<AssetInfo> keywordSpec(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String value = "%" + keyword.trim() + "%";
            Predicate[] predicates = new Predicate[]{
                    cb.like(root.get("assetCode"), value),
                    cb.like(root.get("assetName"), value),
                    cb.like(root.get("category"), value),
                    cb.like(root.get("status"), value),
                    cb.like(root.get("remark"), value)
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

    private static void assertLoggedIn(LoginUser u) {
        if (u == null) {
            throw new AccessDeniedException("未登录");
        }
    }
}
