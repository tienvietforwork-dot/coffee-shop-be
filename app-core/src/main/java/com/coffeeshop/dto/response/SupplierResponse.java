package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Supplier;
import com.coffeeshop.entity.SupplierMaterial;
import com.coffeeshop.entity.enums.SupplierStatus;

import java.math.BigDecimal;
import java.util.List;

public record SupplierResponse(Long id, String name, String contactName, String phone, String email, String address,
                               String taxCode, String note, int paymentTermDays, SupplierStatus status,
                               List<Item> materials) {

    public record Item(Long materialId, String materialName, String unit, Long packageId, String packageName,
                       BigDecimal packageSize, BigDecimal unitPrice) {
        static Item from(SupplierMaterial sm) {
            var pkg = sm.getMaterialPackage();
            return new Item(sm.getMaterial().getId(), sm.getMaterial().getName(), sm.getMaterial().getUnit(),
                    pkg == null ? null : pkg.getId(), pkg == null ? null : pkg.getName(),
                    pkg == null ? null : pkg.getSize(), sm.getUnitPrice());
        }
    }

    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getContactName(), s.getPhone(), s.getEmail(),
                s.getAddress(), s.getTaxCode(), s.getNote(), s.getPaymentTermDays(), s.getStatus(),
                s.getMaterials().stream().map(Item::from).toList());
    }
}
