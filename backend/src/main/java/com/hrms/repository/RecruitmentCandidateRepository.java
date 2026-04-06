package com.hrms.repository;

import com.hrms.entity.RecruitmentCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RecruitmentCandidateRepository extends JpaRepository<RecruitmentCandidate, Long>, JpaSpecificationExecutor<RecruitmentCandidate> {
    List<RecruitmentCandidate> findByPosition_Id(Long positionId);

    @Transactional
    void deleteByPosition_Requirement_Id(Long requirementId);

    @Transactional
    void deleteByReferrerId(Long referrerId);
}
