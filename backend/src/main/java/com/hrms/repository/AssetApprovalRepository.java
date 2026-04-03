package com.hrms.repository;

import com.hrms.entity.AssetApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetApprovalRepository extends JpaRepository<AssetApproval, Long> {
    List<AssetApproval> findByApplicantUserIdOrderByApplyTimeDesc(Long applicantUserId);
}
