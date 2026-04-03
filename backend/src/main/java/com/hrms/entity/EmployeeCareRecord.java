package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "employee_care_record")
public class EmployeeCareRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String recordCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id")
    private EmployeeCarePlan plan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100)
    private String careType;

    private Instant careTime;

    @Column(length = 100)
    private String handlerName;

    @Column(length = 50)
    private String status;

    @Column(length = 2000)
    private String content;

    @Column(length = 1000)
    private String remark;

    @Column(precision = 12, scale = 2)
    private BigDecimal costAmount;

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
            status = "已执行";
        }
        if (costAmount == null) {
            costAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
