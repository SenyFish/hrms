package com.hrms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "recruitment_requirement")
public class RecruitmentRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String requirementCode;

    @Column(nullable = false, length = 120)
    private String jobTitle;

    private Long departmentId;

    private Integer headcount;

    private Integer completedCount;

    @Column(precision = 12, scale = 2)
    private BigDecimal budgetSalaryMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal budgetSalaryMax;

    private LocalDate expectedOnboardDate;

    @Column(length = 2000)
    private String jobDescription;

    @Column(length = 1000)
    private String reason;

    @Column(length = 50)
    private String status;

    @Column(length = 100)
    private String applicantName;

    private Long applicantUserId;

    @Column(length = 100)
    private String approverName;

    private Instant approveTime;

    @Column(length = 1000)
    private String remark;

    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null || status.isBlank()) {
            status = "待审批";
        }
        if (headcount == null || headcount < 1) {
            headcount = 1;
        }
        if (completedCount == null || completedCount < 0) {
            completedCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (completedCount == null || completedCount < 0) {
            completedCount = 0;
        }
    }
}
