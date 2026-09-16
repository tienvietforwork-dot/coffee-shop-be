package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "product_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ProductMaterial.ProductMaterialId.class)
public class ProductMaterial {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "quantity_required", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityRequired;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductMaterialId implements Serializable {
        private Long product;
        private Long material;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProductMaterialId that)) return false;
            return Objects.equals(product, that.product) && Objects.equals(material, that.material);
        }

        @Override
        public int hashCode() {
            return Objects.hash(product, material);
        }
    }
}
