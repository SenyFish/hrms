package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.entity.AssetApproval;
import com.hrms.entity.AssetInfo;
import com.hrms.entity.User;
import com.hrms.repository.AssetApprovalRepository;
import com.hrms.repository.AssetInfoRepository;
import com.hrms.repository.UserRepository;
import com.hrms.security.LoginUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/finance/approvals")
@RequiredArgsConstructor
public class AssetApprovalController {

    private final AssetApprovalRepository assetApprovalRepository;
    private final AssetInfoRepository assetInfoRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<AssetApproval>> list(@AuthenticationPrincipal LoginUser loginUser) {
        assertLoggedIn(loginUser);
        List<AssetApproval> list;
        if (isAdmin(loginUser) || isHr(loginUser)) {
            list = assetApprovalRepository.findAll().stream()
                    .sorted(Comparator.comparing(AssetApproval::getApplyTime, Comparator.nullsLast(Instant::compareTo)).reversed())
                    .toList();
        } else {
            list = assetApprovalRepository.findByApplicantUserIdOrderByApplyTimeDesc(loginUser.getUserId());
        }
        return ApiResponse.ok(list);
    }

    @PostMapping
    public ApiResponse<AssetApproval> create(@RequestBody ApprovalSave body,
                                             @AuthenticationPrincipal LoginUser loginUser) {
        assertLoggedIn(loginUser);
        AssetApproval approval = new AssetApproval();
        apply(body, approval, loginUser, false);
        return ApiResponse.ok(assetApprovalRepository.save(approval));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetApproval> update(@PathVariable Long id,
                                             @RequestBody ApprovalSave body,
                                             @AuthenticationPrincipal LoginUser loginUser) {
        assertLoggedIn(loginUser);
        AssetApproval approval = assetApprovalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资产审批记录不存在"));
        assertCanEdit(loginUser, approval);
        apply(body, approval, loginUser, true);
        return ApiResponse.ok(assetApprovalRepository.save(approval));
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ApiResponse<AssetApproval> approve(@PathVariable Long id,
                                              @RequestParam String status,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        assertApprover(loginUser);
        if (!"已通过".equals(status) && !"已驳回".equals(status)) {
            throw new IllegalArgumentException("审批状态不合法");
        }

        AssetApproval approval = assetApprovalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资产审批记录不存在"));
        if ("已通过".equals(approval.getStatus()) && !"已通过".equals(status)) {
            throw new IllegalArgumentException("已通过的审批记录不允许改为其他状态");
        }
        if (!"已通过".equals(approval.getStatus()) && "已通过".equals(status)) {
            deductAssetQuantity(approval);
        }

        approval.setStatus(status);
        approval.setApproverName(resolveDisplayName(loginUser.getUserId(), loginUser.getUsername()));
        approval.setApproveTime(Instant.now());
        return ApiResponse.ok(assetApprovalRepository.save(approval));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        assertLoggedIn(loginUser);
        AssetApproval approval = assetApprovalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("资产审批记录不存在"));
        if (!isAdmin(loginUser) && !isHr(loginUser) && !approval.getApplicantUserId().equals(loginUser.getUserId())) {
            throw new AccessDeniedException("只能删除自己的审批记录");
        }
        assetApprovalRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void apply(ApprovalSave body, AssetApproval approval, LoginUser loginUser, boolean updating) {
        AssetInfo asset = assetInfoRepository.findById(body.getAssetId())
                .orElseThrow(() -> new IllegalArgumentException("资产不存在"));
        approval.setAsset(asset);
        approval.setApplyReason(body.getApplyReason());
        approval.setRemark(body.getRemark());
        approval.setRequestedQuantity(body.getRequestedQuantity() != null ? body.getRequestedQuantity() : 1);

        if (isAdmin(loginUser) || isHr(loginUser)) {
            if (body.getApplicantUserId() != null) {
                User user = userRepository.findById(body.getApplicantUserId())
                        .orElseThrow(() -> new IllegalArgumentException("申请人不存在"));
                approval.setApplicantUserId(user.getId());
                approval.setApplicantName(displayName(user));
            } else if (body.getApplicantName() != null && !body.getApplicantName().isBlank()) {
                approval.setApplicantName(body.getApplicantName().trim());
            } else if (!updating) {
                approval.setApplicantUserId(loginUser.getUserId());
                approval.setApplicantName(resolveDisplayName(loginUser.getUserId(), loginUser.getUsername()));
            }

            if (body.getStatus() != null && !body.getStatus().isBlank()) {
                approval.setStatus(body.getStatus());
            } else if (!updating || approval.getStatus() == null || approval.getStatus().isBlank()) {
                approval.setStatus("待审批");
            }
            return;
        }

        if (updating && !"待审批".equals(approval.getStatus())) {
            throw new IllegalArgumentException("已审核的审批记录不能再修改");
        }

        approval.setApplicantUserId(loginUser.getUserId());
        approval.setApplicantName(resolveDisplayName(loginUser.getUserId(), loginUser.getUsername()));
        approval.setStatus("待审批");
        approval.setApproverName(null);
        approval.setApproveTime(null);
    }

    private void deductAssetQuantity(AssetApproval approval) {
        AssetInfo asset = approval.getAsset();
        int current = asset.getQuantity() != null ? asset.getQuantity() : 0;
        int requested = approval.getRequestedQuantity() != null ? approval.getRequestedQuantity() : 1;
        if (requested <= 0) {
            throw new IllegalArgumentException("申请数量必须大于 0");
        }
        if (current < requested) {
            throw new IllegalArgumentException("对应资产库存不足，无法审核通过");
        }

        int remain = current - requested;
        asset.setQuantity(remain);
        asset.setStatus(remain > 0 ? "在库" : "已领完");
        assetInfoRepository.save(asset);
    }

    private void assertCanEdit(LoginUser loginUser, AssetApproval approval) {
        if (isAdmin(loginUser) || isHr(loginUser)) {
            return;
        }
        if (!approval.getApplicantUserId().equals(loginUser.getUserId())) {
            throw new AccessDeniedException("只能修改自己的审批记录");
        }
    }

    private String resolveDisplayName(Long userId, String fallback) {
        return userRepository.findById(userId)
                .map(this::displayName)
                .orElse(fallback);
    }

    private String displayName(User user) {
        return user.getRealName() != null && !user.getRealName().isBlank()
                ? user.getRealName()
                : user.getUsername();
    }

    private static boolean isAdmin(LoginUser u) {
        return u != null && "ADMIN".equals(u.getRoleCode());
    }

    private static boolean isHr(LoginUser u) {
        return u != null && "HR".equals(u.getRoleCode());
    }

    private static void assertApprover(LoginUser u) {
        if (!isAdmin(u) && !isHr(u)) {
            throw new AccessDeniedException("需要管理员或人事权限");
        }
    }

    private static void assertLoggedIn(LoginUser u) {
        if (u == null) {
            throw new AccessDeniedException("未登录");
        }
    }

    @Data
    public static class ApprovalSave {
        private Long assetId;
        private Long applicantUserId;
        private String applicantName;
        private String applyReason;
        private Integer requestedQuantity;
        private String status;
        private String remark;
    }
}
