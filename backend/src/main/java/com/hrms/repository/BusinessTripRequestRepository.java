package com.hrms.repository;

import com.hrms.entity.BusinessTripRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BusinessTripRequestRepository extends JpaRepository<BusinessTripRequest, Long>, JpaSpecificationExecutor<BusinessTripRequest> {
    Optional<BusinessTripRequest> findTopByOrderByIdDesc();

    void deleteByUser_Id(Long userId);
}
