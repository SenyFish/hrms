package com.hrms.repository;

import com.hrms.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long>, JpaSpecificationExecutor<AttendanceRecord> {
    Optional<AttendanceRecord> findByUser_IdAndAttDate(Long userId, LocalDate attDate);

    List<AttendanceRecord> findByUser_IdAndAttDateBetweenOrderByAttDateAsc(Long userId, LocalDate from, LocalDate to);

    List<AttendanceRecord> findByAttDateBetweenOrderByAttDateAsc(LocalDate from, LocalDate to);
}
