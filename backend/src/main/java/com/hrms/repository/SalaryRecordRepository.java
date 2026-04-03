package com.hrms.repository;

import com.hrms.entity.SalaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {
    List<SalaryRecord> findBySalaryMonthOrderByIdDesc(String salaryMonth);

    Optional<SalaryRecord> findByUser_IdAndSalaryMonth(Long userId, String salaryMonth);

    List<SalaryRecord> findByUser_IdOrderBySalaryMonthDesc(Long userId);

    List<SalaryRecord> findByInsuredCity_Id(Long insuredCityId);
}
