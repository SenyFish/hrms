package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "business_trip_request")
public class BusinessTripRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String serialNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String employeeName;

    @Column(length = 50)
    private String employeeNo;

    private Long departmentId;

    @Column(length = 100)
    private String departmentName;

    @Column(length = 100)
    private String positionName;

    @Column(nullable = false, length = 50)
    private String tripType;

    @Column(nullable = false, length = 500)
    private String destination;

    @Column(nullable = false, precision = 10, scale = 1)
    private BigDecimal estimatedDays;

    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedExpense;

    private Long attachmentFileId;

    @Column(length = 255)
    private String attachmentName;

    @Column(length = 1000)
    private String attachmentRemark;

    @Column(length = 30)
    private String status;

    private Long approverId;

    @Column(length = 100)
    private String approverName;

    private Instant approveTime;

    @Column(length = 500)
    private String approveRemark;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null || status.isBlank()) {
            status = "待审批";
        }
    }
}
