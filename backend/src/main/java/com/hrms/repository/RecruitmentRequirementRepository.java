package com.hrms.repository;

import com.hrms.entity.RecruitmentRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RecruitmentRequirementRepository extends JpaRepository<RecruitmentRequirement, Long>, JpaSpecificationExecutor<RecruitmentRequirement> {
    Optional<RecruitmentRequirement> findTopByOrderByIdDesc();

    void deleteByApplicantUserId(Long applicantUserId);
}
