package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "asset_approval")
public class AssetApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_id", nullable = false)
    private AssetInfo asset;

    @Column(nullable = false, length = 100)
    private String applicantName;

    private Long applicantUserId;

    @Column(length = 500)
    private String applyReason;

    private Integer requestedQuantity;

    @Column(nullable = false, length = 50)
    private String status;

    private Instant applyTime;

    @Column(length = 100)
    private String approverName;

    private Instant approveTime;

    @Column(length = 1000)
    private String remark;

    @PrePersist
    public void prePersist() {
        if (applyTime == null) {
            applyTime = Instant.now();
        }
        if (status == null || status.isBlank()) {
            status = "待审批";
        }
    }
}
