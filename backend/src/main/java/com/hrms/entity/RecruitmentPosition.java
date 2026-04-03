package com.hrms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "recruitment_position")
public class RecruitmentPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String positionCode;

    @Column(nullable = false, length = 120)
    private String positionName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requirement_id")
    private RecruitmentRequirement requirement;

    private Long departmentId;

    private Integer plannedCount;

    private Integer filledCount;

    @Column(length = 2000)
    private String jobDescription;

    @Column(length = 2000)
    private String jobRequirements;

    @Column(length = 50)
    private String status;

    private Instant publishTime;

    private Instant closeTime;

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
            status = "待发布";
        }
        if (plannedCount == null || plannedCount < 1) {
            plannedCount = 1;
        }
        if (filledCount == null || filledCount < 0) {
            filledCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (filledCount == null || filledCount < 0) {
            filledCount = 0;
        }
    }
}
