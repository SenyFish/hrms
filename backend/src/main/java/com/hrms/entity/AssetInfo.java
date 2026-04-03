package com.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "asset_info")
public class AssetInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String assetCode;

    @Column(nullable = false, length = 200)
    private String assetName;

    @Column(length = 100)
    private String category;

    private Integer quantity;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;

    private LocalDate purchaseDate;

    private Long departmentId;

    @Column(length = 50)
    private String status;

    @Column(length = 1000)
    private String remark;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null || status.isBlank()) {
            status = "在库";
        }
        if (quantity == null) {
            quantity = 1;
        }
    }
}
