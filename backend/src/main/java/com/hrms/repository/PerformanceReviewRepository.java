package com.hrms.repository;

import com.hrms.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long>, JpaSpecificationExecutor<PerformanceReview> {
    List<PerformanceReview> findTop3ByEmployeeIdOrderByIdDesc(Long employeeId);

    List<PerformanceReview> findAllByOrderByEmployeeIdAscIdDesc();

    java.util.Optional<PerformanceReview> findTopByEmployeeIdOrderByIdDesc(Long employeeId);

    @Modifying
    @Transactional
    @Query("""
            update PerformanceReview p
               set p.employeeId = :employeeId,
                   p.employeeName = :employeeName,
                   p.assessmentPeriod = :assessmentPeriod,
                   p.score = :score,
                   p.grade = :grade,
                   p.status = :status,
                   p.evaluatorName = :evaluatorName,
                   p.remark = :remark
             where p.id = :id
            """)
    int updateReviewById(@Param("id") Long id,
                         @Param("employeeId") Long employeeId,
                         @Param("employeeName") String employeeName,
                         @Param("assessmentPeriod") String assessmentPeriod,
                         @Param("score") BigDecimal score,
                         @Param("grade") String grade,
                         @Param("status") String status,
                         @Param("evaluatorName") String evaluatorName,
                         @Param("remark") String remark);

    void deleteByEmployeeId(Long employeeId);
}
