package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "performance_review")
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 100)
    private String employeeName;

    @Column(nullable = false, length = 50)
    private String assessmentPeriod;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, length = 20)
    private String grade;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 100)
    private String evaluatorName;

    @Column(length = 500)
    private String remark;

    @Transient
    private BigDecimal annualBonusAmount;

    @Transient
    private String annualBonusLevel;

    @Transient
    private String scoreTrend;

    @Transient
    private String developmentSuggestion;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
