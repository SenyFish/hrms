package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "employee_dispute")
public class EmployeeDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String serialNo;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 100)
    private String employeeName;

    @Column(nullable = false, length = 100)
    private String disputeType;

    @Column(nullable = false)
    private LocalDate disputeDate;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(length = 1000)
    private String resolution;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
