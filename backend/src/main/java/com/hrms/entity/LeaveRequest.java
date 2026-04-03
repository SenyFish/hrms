package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "leave_request")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String leaveType;

    private Instant startTime;
    private Instant endTime;

    @Column(length = 2000)
    private String reason;

    /** 待审批 已通过 已驳回 */
    private String status;

    private Long approverId;

    private Instant approveTime;

    @Column(length = 500)
    private String approveRemark;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "待审批";
        }
    }
}
