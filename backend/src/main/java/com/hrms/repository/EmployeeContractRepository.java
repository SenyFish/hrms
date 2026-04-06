package com.hrms.repository;

import com.hrms.entity.EmployeeContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EmployeeContractRepository extends JpaRepository<EmployeeContract, Long>, JpaSpecificationExecutor<EmployeeContract> {
    Optional<EmployeeContract> findTopByOrderByIdDesc();

    void deleteByEmployeeId(Long employeeId);
}
