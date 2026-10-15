package com.coffeeshop.service;

import com.coffeeshop.dto.request.MaterialRequest;
import com.coffeeshop.dto.request.StockAdjustRequest;
import com.coffeeshop.dto.request.StockOutRequest;
import com.coffeeshop.dto.request.StockImportRequest;
import com.coffeeshop.dto.response.BatchResponse;
import com.coffeeshop.dto.response.InventoryAlertsResponse;
import com.coffeeshop.dto.response.MaterialResponse;
import com.coffeeshop.dto.response.MaterialTransactionResponse;
import com.coffeeshop.entity.*;
import com.coffeeshop.entity.enums.BatchStatus;
import com.coffeeshop.entity.enums.MaterialKind;
import com.coffeeshop.entity.enums.MaterialStatus;
import com.coffeeshop.entity.enums.MaterialTransactionType;
import com.coffeeshop.entity.enums.TransactionReason;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.*;
import com.coffeeshop.websocket.NotificationService;
import org.hibernate.Hibernate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Materials, batches (lô) and stock movements. Stock always leaves FEFO: nearest expiry first. */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final MaterialRepository materialRepository;
    private final MaterialBatchRepository batchRepository;
    private final MaterialTransactionRepository transactionRepository;
    private final RecipeRepository recipeRepository;
    private final MaterialComponentRepository componentRepository;
    private final StaffService staffService;
    private final NotificationService notificationService;
    private final CoffeeStatusBackgroundService coffeeStatusBackgroundService;

    @Value("${app.inventory.expiry-warning-days:7}")
    private int expiryWarningDays;

    // ---- materials -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MaterialResponse> findAll() {
        // where each material is used: coffees through their active recipe, prepared materials through their formula
        Map<Long, List<MaterialResponse.Usage>> usedIn = new HashMap<>();
        for (var recipe : recipeRepository.findAllActiveWithMaterials()) {
            for (var line : recipe.getMaterials()) {
                usedIn.computeIfAbsent(line.getMaterial().getId(), k -> new ArrayList<>()).add(new MaterialResponse.Usage(
                        "COFFEE", recipe.getCoffee().getId(), recipe.getCoffee().getName(), line.getQuantity()));
            }
        }
        for (var c : componentRepository.findAllWithMaterials()) {
            usedIn.computeIfAbsent(c.getComponent().getId(), k -> new ArrayList<>()).add(new MaterialResponse.Usage(
                    "PREPARED", c.getMaterial().getId(), c.getMaterial().getName(), c.getQuantity()));
        }
        Map<Long, LocalDate> nextExpiry = new HashMap<>();
        for (Object[] row : batchRepository.findNextExpiryDates()) nextExpiry.put((Long) row[0], (LocalDate) row[1]);
        return materialRepository.findAllByOrderByNameAsc().stream().map(m -> {
            Hibernate.initialize(m.getComponents());
            Hibernate.initialize(m.getPackages());
            LocalDate next = nextExpiry.get(m.getId());
            return MaterialResponse.from(m, usedIn.getOrDefault(m.getId(), List.of()), next, next != null && inWarningWindow(m, next));
        }).toList();
    }

    /** Lots still in stock whose expiry is inside their material's warning window (per material, or the shop default). */
    @Transactional(readOnly = true)
    public List<MaterialBatch> expiringLots() {
        Integer longest = materialRepository.maxExpiryWarningDays();
        int window = Math.max(expiryWarningDays, longest == null ? 0 : longest);
        return batchRepository.findExpiringBefore(LocalDate.now().plusDays(window)).stream()
                .filter(b -> inWarningWindow(b.getMaterial(), b.getExpiryDate())).toList();
    }

    /** unset: raw materials use the shop default; prepared ones (shelf life of hours) are not warned about */
    private boolean inWarningWindow(Material m, LocalDate expiry) {
        int days = m.getExpiryWarningDays() != null ? m.getExpiryWarningDays()
                : m.getKind() == MaterialKind.PREPARED ? 0 : expiryWarningDays;
        return days > 0 && !expiry.isAfter(LocalDate.now().plusDays(days));
    }

    @Transactional
    public MaterialResponse create(MaterialRequest request) {
        if (materialRepository.existsByNameIgnoreCase(request.name())) {
            throw new BadRequestException("Nguyên liệu đã tồn tại: " + request.name());
        }
        Material material = Material.builder().build();
        apply(material, request);
        return withFormula(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse update(Long id, MaterialRequest request) {
        Material material = get(id);
        if (!material.getName().equalsIgnoreCase(request.name()) && materialRepository.existsByNameIgnoreCase(request.name())) {
            throw new BadRequestException("Nguyên liệu đã tồn tại: " + request.name());
        }
        apply(material, request);
        return withFormula(material);
    }

    /** Materials with history are kept; deactivate instead of deleting. */
    @Transactional
    public void delete(Long id) {
        get(id).setStatus(MaterialStatus.INACTIVE);
    }

    // ---- batches & transactions -----------------------------------------

    @Transactional(readOnly = true)
    public List<BatchResponse> batches(Long materialId) {
        return batchRepository.findByMaterialIdOrderByIdDesc(materialId).stream()
                .map(b -> b.getMaterial().getKind() == MaterialKind.PREPARED
                        ? BatchResponse.from(b, transactionRepository.findInputsOf(b.getId()))
                        : BatchResponse.from(b))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialTransactionResponse> transactions(Long materialId) {
        return transactionRepository.search(materialId).stream().map(MaterialTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InventoryAlertsResponse alerts() {
        return new InventoryAlertsResponse(
                materialRepository.findLowStock().stream().map(MaterialResponse::from).toList(),
                expiringLots().stream().map(BatchResponse::from).toList());
    }

    /** Phiếu nhập: creates a new batch. */
    @Transactional
    public BatchResponse importStock(StockImportRequest request) {
        Material material = get(request.materialId());
        if (material.getKind() == MaterialKind.PREPARED) {
            throw new BadRequestException("'" + material.getName() + "' là nguyên liệu chế biến / đang sử dụng — tạo bằng chức năng Chế biến, không nhập mua");
        }
        if (request.expiryDate() != null && request.expiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Hạn sử dụng đã qua");
        }
        MaterialPackage pkg = null;
        BigDecimal quantity = request.quantity();
        if (request.packageId() != null) {
            pkg = material.getPackages().stream().filter(p -> p.getId().equals(request.packageId())).findFirst()
                    .orElseThrow(() -> new BadRequestException("Quy cách không thuộc nguyên liệu '" + material.getName() + "'"));
            if (request.packageCount() == null) throw new BadRequestException("Nhập số " + pkg.getName());
            quantity = pkg.getSize().multiply(request.packageCount()).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        if (quantity == null) throw new BadRequestException("Nhập số lượng");
        MaterialBatch batch = batchRepository.save(MaterialBatch.builder()
                .material(material)
                .importQuantity(quantity)
                .remainingQuantity(quantity)
                .unitCost(request.unitCost())
                .expiryDate(request.expiryDate())
                .packageName(pkg == null ? null : pkg.getName())
                .packageSize(pkg == null ? null : pkg.getSize())
                .packageCount(pkg == null ? null : request.packageCount())
                .build());
        material.setStockQuantity(material.getStockQuantity().add(quantity));
        if (material.getStatus() == MaterialStatus.INACTIVE) material.setStatus(MaterialStatus.ACTIVE);
        record(batch, MaterialTransactionType.IMPORT, quantity, staffService.current(), null,
                request.note());
        coffeeStatusBackgroundService.requestRun();
        return BatchResponse.from(batch);
    }

    /** Phiếu hủy: write off part or all of one lot (hết hạn, hỏng, đổ vỡ, pha hỏng). */
    @Transactional
    public MaterialTransactionResponse dispose(StockOutRequest request) {
        if (!request.reason().isDisposal()) throw new BadRequestException("Lý do không dùng cho phiếu hủy");
        return takeOut(request, MaterialTransactionType.DISPOSAL);
    }

    /** Xuất khác: stock leaving for something other than sales or production (trả nhà cung cấp, dùng nội bộ). */
    @Transactional
    public MaterialTransactionResponse exportOther(StockOutRequest request) {
        if (request.reason().isDisposal()) throw new BadRequestException("Hàng hỏng / hết hạn dùng phiếu hủy");
        return takeOut(request, MaterialTransactionType.EXPORT);
    }

    private MaterialTransactionResponse takeOut(StockOutRequest request, MaterialTransactionType type) {
        MaterialBatch batch = batchRepository.findById(request.batchId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô: " + request.batchId()));
        if (batch.getStatus() != BatchStatus.AVAILABLE || batch.getRemainingQuantity().compareTo(request.quantity()) < 0) {
            throw new BadRequestException("Lô #" + batch.getId() + " chỉ còn "
                    + batch.getRemainingQuantity().stripTrailingZeros().toPlainString() + " " + batch.getMaterial().getUnit());
        }
        batch.setRemainingQuantity(batch.getRemainingQuantity().subtract(request.quantity()));
        if (batch.getRemainingQuantity().signum() == 0) batch.setStatus(BatchStatus.DEPLETED);
        Material material = batch.getMaterial();
        material.setStockQuantity(material.getStockQuantity().subtract(request.quantity()));
        checkLowStock(material);
        var tx = transactionRepository.save(MaterialTransaction.builder().batch(batch).type(type)
                .quantity(request.quantity().negate()).reason(request.reason()).staff(staffService.current())
                .note(request.note()).build());
        coffeeStatusBackgroundService.requestRun();
        return MaterialTransactionResponse.from(tx);
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
        var tx = MaterialTransactionResponse.from(record(batch, MaterialTransactionType.ADJUSTMENT, delta,
                staffService.current(), null, request.note() == null ? "Kiểm kê" : request.note()));
        coffeeStatusBackgroundService.requestRun();
        return tx;
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
        coffeeStatusBackgroundService.requestRun();
    }

    /** Marks batches past their expiry date as EXPIRED and writes them off. Returns how many were expired. */
    @Transactional
    public int expireBatches() {
        // by day (expiry_date passed) and, for prepared lots, by the exact expires_at
        Set<MaterialBatch> expired = new java.util.LinkedHashSet<>(batchRepository.findExpiringBefore(LocalDate.now().minusDays(1)));
        expired.addAll(batchRepository.findExpiredAt(java.time.LocalDateTime.now()));
        for (MaterialBatch batch : expired) {
            BigDecimal remaining = batch.getRemainingQuantity();
            batch.setRemainingQuantity(BigDecimal.ZERO);
            batch.setStatus(BatchStatus.EXPIRED);
            Material material = batch.getMaterial();
            material.setStockQuantity(material.getStockQuantity().subtract(remaining));
            transactionRepository.save(MaterialTransaction.builder().batch(batch).type(MaterialTransactionType.DISPOSAL)
                    .reason(TransactionReason.EXPIRED).quantity(remaining.negate()).note("Tự hủy lô hết hạn").build());
            checkLowStock(material);
        }
        if (!expired.isEmpty()) coffeeStatusBackgroundService.requestRun();
        return expired.size();
    }

    private List<MaterialTransaction> consume(Material material, BigDecimal quantity, MaterialTransactionType type,
                                              Staff staff, OrderItem orderItem, String note) {
        return consume(material, quantity, type, staff, orderItem, note, null);
    }

    /** FEFO stock-out; {@code producedBatch}: the prepared-material batch this stock goes into (PRODUCTION_USE). */
    List<MaterialTransaction> consume(Material material, BigDecimal quantity, MaterialTransactionType type,
                                      Staff staff, OrderItem orderItem, String note, MaterialBatch producedBatch) {
        return consume(material, quantity, type, staff, orderItem, note, producedBatch, null);
    }

    /** {@code firstBatchId}: take from this lot first (staff picked it), the rest FEFO as usual. */
    List<MaterialTransaction> consume(Material material, BigDecimal quantity, MaterialTransactionType type,
                                      Staff staff, OrderItem orderItem, String note, MaterialBatch producedBatch,
                                      Long firstBatchId) {
        if (material.getStockQuantity().compareTo(quantity) < 0) {
            throw new BadRequestException("Không đủ tồn kho '" + material.getName() + "': còn "
                    + material.getStockQuantity().stripTrailingZeros().toPlainString() + " " + material.getUnit());
        }
        BigDecimal left = quantity;
        List<MaterialTransaction> result = new java.util.ArrayList<>();
        List<MaterialBatch> lots = new java.util.ArrayList<>(batchRepository.findAvailableFefo(material.getId()));
        if (firstBatchId != null) {
            MaterialBatch first = lots.stream().filter(b -> b.getId().equals(firstBatchId)).findFirst()
                    .orElseThrow(() -> new BadRequestException("Lô #" + firstBatchId + " không còn hàng của '" + material.getName() + "'"));
            lots.remove(first);
            lots.add(0, first);
        }
        for (MaterialBatch batch : lots) {
            if (left.signum() <= 0) break;
            BigDecimal take = left.min(batch.getRemainingQuantity());
            batch.setRemainingQuantity(batch.getRemainingQuantity().subtract(take));
            if (batch.getRemainingQuantity().signum() == 0) batch.setStatus(BatchStatus.DEPLETED);
            result.add(record(batch, type, take.negate(), staff, orderItem, note, producedBatch));
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
        return record(batch, type, quantity, staff, orderItem, note, null);
    }

    MaterialTransaction record(MaterialBatch batch, MaterialTransactionType type, BigDecimal quantity,
                               Staff staff, OrderItem orderItem, String note, MaterialBatch producedBatch) {
        return transactionRepository.save(MaterialTransaction.builder()
                .batch(batch).type(type).quantity(quantity).staff(staff).orderItem(orderItem).note(note)
                .producedBatch(producedBatch).build());
    }

    /** Response including the prepared-material formula (loaded here, inside the transaction). */
    private MaterialResponse withFormula(Material material) {
        Hibernate.initialize(material.getComponents());
        Hibernate.initialize(material.getPackages());
        return MaterialResponse.from(material);
    }

    void checkLowStock(Material material) {
        if (material.isLowStock()) {
            notificationService.notifyInventoryAlert(MaterialResponse.from(material));
        }
    }

    Material get(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu: " + id));
    }

    private void apply(Material material, MaterialRequest request) {
        material.setName(request.name().trim());
        material.setUnit(request.unit().trim());
        material.setMinStock(request.minStock() == null ? BigDecimal.ZERO : request.minStock());
        material.setExpiryWarningDays(request.expiryWarningDays());
        if (request.status() != null) material.setStatus(request.status());
        material.setKind(request.kind() == null ? MaterialKind.RAW : request.kind());
        // replace the formula (soft delete keeps history; flush before re-adding because of the unique index)
        material.getComponents().forEach(MaterialComponent::softDelete);
        material.getComponents().clear();
        materialRepository.flush();
        // replace the packaging list the same way (raw materials only)
        material.getPackages().forEach(MaterialPackage::softDelete);
        material.getPackages().clear();
        materialRepository.flush();
        if (material.getKind() == MaterialKind.RAW && request.packages() != null) {
            Set<String> names = new HashSet<>();
            for (MaterialRequest.Package p : request.packages()) {
                if (!names.add(p.name().trim().toLowerCase())) throw new BadRequestException("Quy cách bị lặp: " + p.name());
                material.getPackages().add(MaterialPackage.builder().material(material).name(p.name().trim()).size(p.size()).build());
            }
        }
        if (material.getKind() == MaterialKind.RAW) {
            material.setYieldQuantity(null);
            material.setShelfLifeMinutes(null);
            material.setInstructions(null);
            return;
        }
        if (request.yieldQuantity() == null) throw new BadRequestException("Nguyên liệu chế biến cần định lượng chuẩn");
        if (request.components() == null || request.components().isEmpty()) {
            throw new BadRequestException("Nguyên liệu chế biến cần định mức nguyên liệu thô");
        }
        material.setYieldQuantity(request.yieldQuantity());
        material.setShelfLifeMinutes(request.shelfLifeMinutes());
        material.setInstructions(request.instructions() == null || request.instructions().isBlank() ? null : request.instructions().trim());
        Set<Long> seen = new HashSet<>();
        for (MaterialRequest.Component line : request.components()) {
            Material component = get(line.componentId());
            if (!seen.add(component.getId())) throw new BadRequestException("Nguyên liệu bị lặp trong định mức: " + component.getName());
            // only raw inputs: keeps formulas one level deep, so no material can end up made from itself
            if (component.getKind() != MaterialKind.RAW) {
                throw new BadRequestException("Định mức chỉ dùng nguyên liệu thô: " + component.getName());
            }
            material.getComponents().add(MaterialComponent.builder()
                    .material(material).component(component).quantity(line.quantity()).build());
        }
    }
}
