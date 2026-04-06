package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "promotion_plan")
public class PromotionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 100)
    private String employeeName;

    @Column(length = 100)
    private String currentPosition;

    @Column(nullable = false, length = 100)
    private String targetPosition;

    @Column(nullable = false)
    private LocalDate plannedDate;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 500)
    private String remark;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
