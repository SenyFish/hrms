package com.hrms.repository;

import com.hrms.entity.PromotionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PromotionPlanRepository extends JpaRepository<PromotionPlan, Long>, JpaSpecificationExecutor<PromotionPlan> {
    void deleteByEmployeeId(Long employeeId);
}
