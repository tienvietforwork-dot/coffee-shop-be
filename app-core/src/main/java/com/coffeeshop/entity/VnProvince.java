package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

/** Tỉnh / thành phố trong danh mục hành chính 2 cấp (34 đơn vị, từ 1/7/2025); {@code code} là mã đơn vị hành chính. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vn_provinces")
public class VnProvince {
    @Id
    private Integer code;
    @Column(nullable = false, length = 100)
    private String name;
}
