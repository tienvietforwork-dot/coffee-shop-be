package com.coffeeshop.service;

import com.coffeeshop.dto.request.FinishBatchRequest;
import com.coffeeshop.dto.request.ProduceRequest;
import com.coffeeshop.dto.response.BatchResponse;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.MaterialBatch;
import com.coffeeshop.entity.MaterialComponent;
import com.coffeeshop.entity.MaterialTransaction;
import com.coffeeshop.entity.enums.BatchStatus;
import com.coffeeshop.entity.enums.MaterialKind;
import com.coffeeshop.entity.enums.MaterialStatus;
import com.coffeeshop.entity.enums.MaterialTransactionType;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.MaterialBatchRepository;
import com.coffeeshop.repository.MaterialTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Batches of prepared materials (e.g. cold brew concentrate):
 * start = take the formula's inputs out of stock (FEFO, linked by produced_batch_id) and open a PREPARING batch;
 * finish = the batch gets the actual yield, an expiry date and its cost, and goes into stock;
 * discard = a ruined batch is closed (its inputs stay used up).
 */
@Service
@RequiredArgsConstructor
public class ProductionService {

    private final InventoryService inventoryService;
    private final MaterialBatchRepository batchRepository;
    private final MaterialTransactionRepository transactionRepository;
    private final StaffService staffService;
    private final CoffeeStatusBackgroundService coffeeStatusBackgroundService;

    @Transactional
    public BatchResponse start(ProduceRequest request) {
        Material material = inventoryService.get(request.materialId());
        if (material.getKind() != MaterialKind.PREPARED || material.getComponents().isEmpty()) {
            throw new BadRequestException("'" + material.getName() + "' không phải bán thành phẩm có định mức");
        }
        if (material.getStatus() != MaterialStatus.ACTIVE) throw new BadRequestException("Nguyên liệu đã ngưng dùng");
        BigDecimal batches = request.batches();

        // check every input first so a shortage does not leave a half-started batch
        for (MaterialComponent c : material.getComponents()) {
            BigDecimal need = scaled(c.getQuantity(), batches);
            Material in = c.getComponent();
            if (in.getStockQuantity().compareTo(need) < 0) {
                throw new BadRequestException("Không đủ " + in.getName() + ": cần " + plain(need) + " " + in.getUnit()
                        + ", còn " + plain(in.getStockQuantity()) + " " + in.getUnit());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        MaterialBatch batch = batchRepository.save(MaterialBatch.builder()
                .material(material)
                .importQuantity(scaled(material.getYieldQuantity(), batches)) // expected; set to the actual yield on finish
                .remainingQuantity(BigDecimal.ZERO)
                .status(BatchStatus.PREPARING)
                .readyAt(material.getPrepMinutes() == null ? now : now.plusMinutes(material.getPrepMinutes()))
                .build());
        String note = "Chế biến " + material.getName() + " lô #" + batch.getId() + (request.note() == null ? "" : " · " + request.note());
        var staff = staffService.current();
        for (MaterialComponent c : material.getComponents()) {
            inventoryService.consume(c.getComponent(), scaled(c.getQuantity(), batches),
                    MaterialTransactionType.PRODUCTION_USE, staff, null, note, batch);
        }
        coffeeStatusBackgroundService.requestRun(); // raw stock went down
        return BatchResponse.from(batch, transactionRepository.findInputsOf(batch.getId()));
    }

    @Transactional
    public BatchResponse finish(Long batchId, FinishBatchRequest request) {
        MaterialBatch batch = preparing(batchId);
        if (request.actualQuantity() == null) throw new BadRequestException("Nhập sản lượng thực tế");
        Material material = batch.getMaterial();
        BigDecimal actual = request.actualQuantity();
        List<MaterialTransaction> inputs = transactionRepository.findInputsOf(batchId);

        batch.setImportQuantity(actual);
        batch.setRemainingQuantity(actual);
        batch.setStatus(BatchStatus.AVAILABLE);
        LocalDateTime now = LocalDateTime.now();
        batch.setReadyAt(now);
        LocalDateTime expiresAt = request.expiresAt() != null ? request.expiresAt()
                : material.getShelfLifeMinutes() == null ? null : now.plusMinutes(material.getShelfLifeMinutes());
        if (expiresAt != null && !expiresAt.isAfter(now)) throw new BadRequestException("Hạn dùng phải sau thời điểm hiện tại");
        batch.setExpiresAt(expiresAt);
        batch.setExpiryDate(expiresAt == null ? null : expiresAt.toLocalDate());
        batch.setUnitCost(unitCost(inputs, actual));
        material.setStockQuantity(material.getStockQuantity().add(actual));
        inventoryService.record(batch, MaterialTransactionType.PRODUCE, actual, staffService.current(), null,
                request.note() == null ? "Hoàn tất lô chế biến" : request.note(), null);
        inventoryService.checkLowStock(material);
        coffeeStatusBackgroundService.requestRun(); // prepared stock went up
        return BatchResponse.from(batch, inputs);
    }

    @Transactional
    public BatchResponse discard(Long batchId, FinishBatchRequest request) {
        MaterialBatch batch = preparing(batchId);
        batch.setStatus(BatchStatus.DEPLETED);
        inventoryService.record(batch, MaterialTransactionType.ADJUSTMENT, BigDecimal.ZERO, staffService.current(), null,
                "Hủy lô chế biến" + (request == null || request.note() == null ? "" : ": " + request.note()), null);
        return BatchResponse.from(batch, transactionRepository.findInputsOf(batchId));
    }

    private MaterialBatch preparing(Long batchId) {
        MaterialBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô: " + batchId));
        if (batch.getStatus() != BatchStatus.PREPARING) throw new BadRequestException("Lô #" + batchId + " không ở trạng thái đang làm");
        return batch;
    }

    /** Cost of the inputs (by the price of each raw batch used) spread over the actual yield; null if a price is missing. */
    private static BigDecimal unitCost(List<MaterialTransaction> inputs, BigDecimal actual) {
        BigDecimal total = BigDecimal.ZERO;
        for (MaterialTransaction t : inputs) {
            if (t.getBatch().getUnitCost() == null) return null;
            total = total.add(t.getBatch().getUnitCost().multiply(t.getQuantity().negate()));
        }
        return total.divide(actual, 2, RoundingMode.HALF_UP);
    }

    /** standard quantity × batches, kept to the 2 decimals the stock columns hold */
    private static BigDecimal scaled(BigDecimal standard, BigDecimal batches) {
        return standard.multiply(batches).setScale(2, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
