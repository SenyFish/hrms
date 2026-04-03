package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** 上级部门，顶级为 0 */
    private Long parentId;

    private Integer sortOrder;

    /** 考勤：上班时间 HH:mm */
    private String workStartTime;
    /** 考勤：下班时间 HH:mm */
    private String workEndTime;
    /** 迟到宽限（分钟） */
    private Integer lateGraceMinutes;
    /** 请假规则说明 */
    @Column(length = 2000)
    private String leaveSettingsNote;
    /** 迟到单次罚款（元） */
    private BigDecimal finePerLate;
    /** 加班计薪倍数 */
    private BigDecimal overtimeRate;
}
