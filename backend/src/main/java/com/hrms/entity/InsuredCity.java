package com.hrms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "insured_city")
public class InsuredCity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String regionCode;

    /** 当地社平工资参考（元） */
    private BigDecimal socialAvgSalary;

    @Column(precision = 8, scale = 4)
    private BigDecimal pensionPersonalRate;

    @Column(precision = 8, scale = 4)
    private BigDecimal pensionCompanyRate;

    @Column(precision = 8, scale = 4)
    private BigDecimal medicalPersonalRate;

    @Column(precision = 8, scale = 4)
    private BigDecimal medicalCompanyRate;

    @Column(precision = 8, scale = 4)
    private BigDecimal unemploymentPersonalRate;

    @Column(precision = 8, scale = 4)
    private BigDecimal unemploymentCompanyRate;

    @Column(precision = 8, scale = 4)
    private BigDecimal injuryCompanyRate;

    @Column(precision = 8, scale = 4)
    private BigDecimal maternityCompanyRate;

    @Column(precision = 8, scale = 4)
    private BigDecimal housingFundPersonalRate;

    @Column(precision = 8, scale = 4)
    private BigDecimal housingFundCompanyRate;

    @Column(length = 1000)
    private String remark;
}
