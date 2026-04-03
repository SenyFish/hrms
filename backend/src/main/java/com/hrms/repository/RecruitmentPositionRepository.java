package com.hrms.repository;

import com.hrms.entity.RecruitmentPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface RecruitmentPositionRepository extends JpaRepository<RecruitmentPosition, Long>, JpaSpecificationExecutor<RecruitmentPosition> {
    Optional<RecruitmentPosition> findTopByOrderByIdDesc();

    List<RecruitmentPosition> findByRequirement_Id(Long requirementId);

    @Transactional
    void deleteByRequirement_Id(Long requirementId);
}
