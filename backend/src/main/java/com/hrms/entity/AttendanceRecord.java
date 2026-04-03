package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "attendance_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "att_date"})
})
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "att_date", nullable = false)
    private LocalDate attDate;

    private Instant clockIn;
    private Instant clockOut;

    /** normal, late, absent, leave */
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
