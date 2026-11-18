package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

/** Phường / xã / đặc khu thuộc một tỉnh trong danh mục hành chính 2 cấp. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vn_wards")
public class VnWard {
    @Id
    private Integer code;
    @Column(name = "province_code", nullable = false)
    private Integer provinceCode;
    @Column(nullable = false, length = 100)
    private String name;
}
