package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "salary_record")
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 工资月份 yyyy-MM */
    private String salaryMonth;

    private BigDecimal baseSalary;

    private BigDecimal pensionPersonal;
    private BigDecimal pensionCompany;
    private BigDecimal medicalPersonal;
    private BigDecimal medicalCompany;
    private BigDecimal unemploymentPersonal;
    private BigDecimal unemploymentCompany;
    private BigDecimal injuryCompany;
    private BigDecimal maternityCompany;
    private BigDecimal housingFundPersonal;
    private BigDecimal housingFundCompany;

    private BigDecimal socialSecurityBase;
    private BigDecimal housingFundBase;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "insured_city_id")
    private InsuredCity insuredCity;

    @Column(length = 1000)
    private String remark;

    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }
}
