package com.coffeeshop.service;

import com.coffeeshop.dto.request.SupplierRequest;
import com.coffeeshop.dto.response.SupplierResponse;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.Supplier;
import com.coffeeshop.entity.SupplierMaterial;
import com.coffeeshop.entity.enums.SupplierStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Nhà cung cấp and the materials each one sells. */
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        return supplierRepository.findAllByOrderByNameAsc().stream().map(SupplierResponse::from).toList();
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        if (supplierRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BadRequestException("Nhà cung cấp đã tồn tại: " + request.name());
        }
        Supplier supplier = supplierRepository.save(Supplier.builder().name(request.name().trim()).build());
        apply(supplier, request);
        return SupplierResponse.from(supplier);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = get(id);
        if (!supplier.getName().equalsIgnoreCase(request.name().trim()) && supplierRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BadRequestException("Nhà cung cấp đã tồn tại: " + request.name());
        }
        apply(supplier, request);
        return SupplierResponse.from(supplier);
    }

    /** Ngừng hợp tác: purchase orders and receipts keep referencing it, so it is deactivated, not deleted. */
    @Transactional
    public void delete(Long id) {
        get(id).setStatus(SupplierStatus.INACTIVE);
    }

    Supplier get(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp: " + id));
    }

    /** A supplier new orders and receipts can be made with. */
    Supplier active(Long id) {
        Supplier supplier = get(id);
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new BadRequestException("Đã ngừng hợp tác với nhà cung cấp '" + supplier.getName() + "'");
        }
        return supplier;
    }

    private void apply(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.name().trim());
        supplier.setContactName(blankToNull(request.contactName()));
        supplier.setPhone(blankToNull(request.phone()));
        supplier.setEmail(blankToNull(request.email()));
        supplier.setAddress(blankToNull(request.address()));
        supplier.setTaxCode(blankToNull(request.taxCode()));
        supplier.setNote(blankToNull(request.note()));
        if (request.status() != null) supplier.setStatus(request.status());
        // replace the material list (soft delete keeps history; flush before re-adding because of the unique index)
        supplier.getMaterials().forEach(SupplierMaterial::softDelete);
        supplier.getMaterials().clear();
        supplierRepository.flush();
        if (request.materials() == null) return;
        Set<Long> seen = new HashSet<>();
        for (SupplierRequest.Item line : request.materials()) {
            Material material = inventoryService.purchasable(line.materialId());
            if (!seen.add(material.getId())) throw new BadRequestException("Nguyên liệu bị lặp: " + material.getName());
            supplier.getMaterials().add(SupplierMaterial.builder().supplier(supplier).material(material)
                    .materialPackage(inventoryService.packageOf(material, line.packageId()))
                    .unitPrice(line.unitPrice()).build());
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
