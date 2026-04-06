package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "employee_contract")
public class EmployeeContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String serialNo;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 100)
    private String employeeName;

    @Column(nullable = false, length = 50)
    private String contractType;

    @Column(length = 100)
    private String contractTitle;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 30)
    private String status;

    private Integer reminderDays;

    private LocalDate reminderDate;

    @Transient
    private String reminderStatus;

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
