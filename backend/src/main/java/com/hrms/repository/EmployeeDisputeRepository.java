package com.hrms.repository;

import com.hrms.entity.EmployeeDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EmployeeDisputeRepository extends JpaRepository<EmployeeDispute, Long>, JpaSpecificationExecutor<EmployeeDispute> {
    Optional<EmployeeDispute> findTopByOrderByIdDesc();

    void deleteByEmployeeId(Long employeeId);
}
