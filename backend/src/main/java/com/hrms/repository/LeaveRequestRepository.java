package com.hrms.repository;

import com.hrms.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {
    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<LeaveRequest> findByUser_IdOrderByCreatedAtDesc(Long userId);

    void deleteByUser_Id(Long userId);
}
