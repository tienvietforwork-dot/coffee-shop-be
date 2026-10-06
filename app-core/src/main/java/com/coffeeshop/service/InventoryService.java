package com.coffeeshop.service;

import com.coffeeshop.dto.request.MaterialRequest;
import com.coffeeshop.dto.request.StockAdjustRequest;
import com.coffeeshop.dto.request.StockExportRequest;
import com.coffeeshop.dto.request.StockImportRequest;
import com.coffeeshop.dto.response.BatchResponse;
import com.coffeeshop.dto.response.InventoryAlertsResponse;
import com.coffeeshop.dto.response.MaterialResponse;
import com.coffeeshop.dto.response.MaterialTransactionResponse;
import com.coffeeshop.entity.*;
import com.coffeeshop.entity.enums.BatchStatus;
import com.coffeeshop.entity.enums.MaterialStatus;
import com.coffeeshop.entity.enums.MaterialTransactionType;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.*;
import com.coffeeshop.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Materials, batches (lô) and stock movements. Stock always leaves FEFO: nearest expiry first. */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final MaterialRepository materialRepository;
    private final MaterialBatchRepository batchRepository;
    private final MaterialTransactionRepository transactionRepository;
    private final RecipeRepository recipeRepository;
    private final StaffService staffService;
    private final NotificationService notificationService;

    @Value("${app.inventory.expiry-warning-days:7}")
    private int expiryWarningDays;

    // ---- materials -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MaterialResponse> findAll() {
        return materialRepository.findAllByOrderByNameAsc().stream().map(MaterialResponse::from).toList();
    }

    @Transactional
    public MaterialResponse create(MaterialRequest request) {
        if (materialRepository.existsByNameIgnoreCase(request.name())) {
            throw new BadRequestException("Nguyên liệu đã tồn tại: " + request.name());
        }
        Material material = Material.builder().build();
        apply(material, request);
        return MaterialResponse.from(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse update(Long id, MaterialRequest request) {
        Material material = get(id);
        if (!material.getName().equalsIgnoreCase(request.name()) && materialRepository.existsByNameIgnoreCase(request.name())) {
            throw new BadRequestException("Nguyên liệu đã tồn tại: " + request.name());
        }
        apply(material, request);
        return MaterialResponse.from(material);
    }

    /** Materials with history are kept; deactivate instead of deleting. */
    @Transactional
    public void delete(Long id) {
        get(id).setStatus(MaterialStatus.INACTIVE);
    }

    // ---- batches & transactions -----------------------------------------

    @Transactional(readOnly = true)
    public List<BatchResponse> batches(Long materialId) {
        return batchRepository.findByMaterialIdOrderByIdDesc(materialId).stream().map(BatchResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialTransactionResponse> transactions(Long materialId) {
        return transactionRepository.search(materialId).stream().map(MaterialTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InventoryAlertsResponse alerts() {
        return new InventoryAlertsResponse(
                materialRepository.findLowStock().stream().map(MaterialResponse::from).toList(),
                batchRepository.findExpiringBefore(LocalDate.now().plusDays(expiryWarningDays)).stream()
                        .map(BatchResponse::from).toList());
    }

    /** Phiếu nhập: creates a new batch. */
    @Transactional
    public BatchResponse importStock(StockImportRequest request) {
        Material material = get(request.materialId());
        if (request.expiryDate() != null && request.expiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Hạn sử dụng đã qua");
        }
        MaterialBatch batch = batchRepository.save(MaterialBatch.builder()
                .material(material)
                .importQuantity(request.quantity())
                .remainingQuantity(request.quantity())
                .unitCost(request.unitCost())
                .expiryDate(request.expiryDate())
                .build());
        material.setStockQuantity(material.getStockQuantity().add(request.quantity()));
        if (material.getStatus() == MaterialStatus.INACTIVE) material.setStatus(MaterialStatus.ACTIVE);
        record(batch, MaterialTransactionType.IMPORT, request.quantity(), staffService.current(), null,
                request.note());
        return BatchResponse.from(batch);
    }

    /** Phiếu xuất: takes stock out FEFO. */
    @Transactional
    public List<MaterialTransactionResponse> exportStock(StockExportRequest request) {
        Material material = get(request.materialId());
        return consume(material, request.quantity(), MaterialTransactionType.EXPORT,
                staffService.current(), null, request.note()).stream()
                .map(MaterialTransactionResponse::from).toList();
    }

    /** Kiểm kê: set the counted quantity of one batch. */
    @Transactional
    public MaterialTransactionResponse adjust(StockAdjustRequest request) {
        MaterialBatch batch = batchRepository.findById(request.batchId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô: " + request.batchId()));
        if (request.actualQuantity().compareTo(batch.getImportQuantity()) > 0) {
            throw new BadRequestException("Số lượng thực tế lớn hơn số lượng nhập của lô");
        }
        BigDecimal delta = request.actualQuantity().subtract(batch.getRemainingQuantity());
        if (delta.signum() == 0) throw new BadRequestException("Số lượng không thay đổi");
        batch.setRemainingQuantity(request.actualQuantity());
        batch.setStatus(request.actualQuantity().signum() == 0 ? BatchStatus.DEPLETED : BatchStatus.AVAILABLE);
        Material material = batch.getMaterial();
        material.setStockQuantity(material.getStockQuantity().add(delta));
        checkLowStock(material);
        return MaterialTransactionResponse.from(record(batch, MaterialTransactionType.ADJUSTMENT, delta,
                staffService.current(), null, request.note() == null ? "Kiểm kê" : request.note()));
    }

    /** Deducts materials for every item of an order according to the coffee's active recipe. */
    @Transactional
    public void consumeForOrder(Order order, Staff staff) {
        Map<OrderItem, Recipe> recipes = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            recipeRepository.findByCoffeeIdAndActiveTrue(item.getCoffee().getId()).ifPresent(r -> recipes.put(item, r));
        }
        // check everything first so a shortage does not leave a half-deducted order
        Map<Material, BigDecimal> needed = new LinkedHashMap<>();
        recipes.forEach((item, recipe) -> recipe.getMaterials().forEach(rm -> needed.merge(rm.getMaterial(),
                rm.getQuantity().multiply(BigDecimal.valueOf(item.getQuantity())), BigDecimal::add)));
        needed.forEach((material, qty) -> {
            if (material.getStockQuantity().compareTo(qty) < 0) {
                throw new BadRequestException("Không đủ nguyên liệu '" + material.getName() + "' (cần "
                        + qty.stripTrailingZeros().toPlainString() + " " + material.getUnit() + ")");
            }
        });
        recipes.forEach((item, recipe) -> recipe.getMaterials().forEach(rm -> consume(rm.getMaterial(),
                rm.getQuantity().multiply(BigDecimal.valueOf(item.getQuantity())), MaterialTransactionType.SALE,
                staff, item, "Đơn " + order.getOrderCode())));
    }

    /** Marks batches past their expiry date as EXPIRED and writes them off. Returns how many were expired. */
    @Transactional
    public int expireBatches() {
        List<MaterialBatch> expired = batchRepository.findExpiringBefore(LocalDate.now().minusDays(1));
        for (MaterialBatch batch : expired) {
            BigDecimal remaining = batch.getRemainingQuantity();
            batch.setRemainingQuantity(BigDecimal.ZERO);
            batch.setStatus(BatchStatus.EXPIRED);
            Material material = batch.getMaterial();
            material.setStockQuantity(material.getStockQuantity().subtract(remaining));
            record(batch, MaterialTransactionType.ADJUSTMENT, remaining.negate(), null, null, "Hủy lô hết hạn");
            checkLowStock(material);
        }
        return expired.size();
    }

    private List<MaterialTransaction> consume(Material material, BigDecimal quantity, MaterialTransactionType type,
                                              Staff staff, OrderItem orderItem, String note) {
        if (material.getStockQuantity().compareTo(quantity) < 0) {
            throw new BadRequestException("Không đủ tồn kho '" + material.getName() + "': còn "
                    + material.getStockQuantity().stripTrailingZeros().toPlainString() + " " + material.getUnit());
        }
        BigDecimal left = quantity;
        List<MaterialTransaction> result = new java.util.ArrayList<>();
        for (MaterialBatch batch : batchRepository.findAvailableFefo(material.getId())) {
            if (left.signum() <= 0) break;
            BigDecimal take = left.min(batch.getRemainingQuantity());
            batch.setRemainingQuantity(batch.getRemainingQuantity().subtract(take));
            if (batch.getRemainingQuantity().signum() == 0) batch.setStatus(BatchStatus.DEPLETED);
            result.add(record(batch, type, take.negate(), staff, orderItem, note));
            left = left.subtract(take);
        }
        if (left.signum() > 0) {
            throw new BadRequestException("Tồn kho các lô của '" + material.getName() + "' không khớp, cần kiểm kê");
        }
        material.setStockQuantity(material.getStockQuantity().subtract(quantity));
        checkLowStock(material);
        return result;
    }

    private MaterialTransaction record(MaterialBatch batch, MaterialTransactionType type, BigDecimal quantity,
                                       Staff staff, OrderItem orderItem, String note) {
        return transactionRepository.save(MaterialTransaction.builder()
                .batch(batch).type(type).quantity(quantity).staff(staff).orderItem(orderItem).note(note)
                .build());
    }

    private void checkLowStock(Material material) {
        if (material.isLowStock()) {
            notificationService.notifyInventoryAlert(MaterialResponse.from(material));
        }
    }

    private Material get(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu: " + id));
    }

    private void apply(Material material, MaterialRequest request) {
        material.setName(request.name().trim());
        material.setUnit(request.unit().trim());
        material.setMinStock(request.minStock() == null ? BigDecimal.ZERO : request.minStock());
        if (request.status() != null) material.setStatus(request.status());
    }
}
