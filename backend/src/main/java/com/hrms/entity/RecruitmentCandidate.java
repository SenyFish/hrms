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

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "recruitment_candidate")
public class RecruitmentCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 100)
    private String education;

    @Column(length = 100)
    private String sourceChannel;

    private Long referrerId;

    @Column(length = 100)
    private String referrerName;

    @Column(length = 50)
    private String referrerEmployeeNo;

    private Instant referralTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "position_id")
    private RecruitmentPosition position;

    @Column(precision = 12, scale = 2)
    private BigDecimal expectedSalary;

    private Instant interviewTime;

    @Column(length = 100)
    private String interviewerName;

    @Column(length = 50)
    private String status;

    @Column(length = 100)
    private String result;

    @Column(length = 2000)
    private String resumeRemark;

    private Long resumeFileId;

    @Column(length = 255)
    private String resumeFileName;

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
            status = "待筛选";
        }
        if (sourceChannel == null || sourceChannel.isBlank()) {
            sourceChannel = "社会招聘";
        }
        if (referralTime == null && referrerId != null) {
            referralTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
