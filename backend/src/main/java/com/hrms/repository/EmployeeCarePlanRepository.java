package com.hrms.repository;

import com.hrms.entity.EmployeeCarePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EmployeeCarePlanRepository extends JpaRepository<EmployeeCarePlan, Long>, JpaSpecificationExecutor<EmployeeCarePlan> {
    Optional<EmployeeCarePlan> findTopByOrderByIdDesc();
}
