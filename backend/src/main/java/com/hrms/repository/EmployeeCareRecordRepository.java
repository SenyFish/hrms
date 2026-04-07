package com.hrms.repository;

import com.hrms.entity.EmployeeCareRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EmployeeCareRecordRepository extends JpaRepository<EmployeeCareRecord, Long>, JpaSpecificationExecutor<EmployeeCareRecord> {
    Optional<EmployeeCareRecord> findTopByOrderByIdDesc();

    boolean existsByPlan_Id(Long planId);

    void deleteByUser_Id(Long userId);
}
