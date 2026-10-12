package com.coffeeshop.service;

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
 * Batches of prepared materials (e.g. cold brew concentrate), recorded once they are made:
 * the formula's inputs leave stock (FEFO, linked by produced_batch_id) and the new batch goes straight into stock
 * with its expiry (standard shelf life from now unless given) and its cost.
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
    public BatchResponse produce(ProduceRequest request) {
        Material material = inventoryService.get(request.materialId());
        if (material.getKind() != MaterialKind.PREPARED || material.getComponents().isEmpty()) {
            throw new BadRequestException("'" + material.getName() + "' không phải bán thành phẩm có định mức");
        }
        if (material.getStatus() != MaterialStatus.ACTIVE) throw new BadRequestException("Nguyên liệu đã ngưng dùng");
        BigDecimal batches = request.batches();

        // check every input first so a shortage does not leave a half-made batch
        for (MaterialComponent c : material.getComponents()) {
            BigDecimal need = scaled(c.getQuantity(), batches);
            Material in = c.getComponent();
            if (in.getStockQuantity().compareTo(need) < 0) {
                throw new BadRequestException("Không đủ " + in.getName() + ": cần " + plain(need) + " " + in.getUnit()
                        + ", còn " + plain(in.getStockQuantity()) + " " + in.getUnit());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = request.expiresAt() != null ? request.expiresAt()
                : material.getShelfLifeMinutes() == null ? null : now.plusMinutes(material.getShelfLifeMinutes());
        if (expiresAt != null && !expiresAt.isAfter(now)) throw new BadRequestException("Hạn dùng phải sau thời điểm hiện tại");
        BigDecimal quantity = scaled(material.getYieldQuantity(), batches);

        MaterialBatch batch = batchRepository.save(MaterialBatch.builder()
                .material(material)
                .importQuantity(quantity)
                .remainingQuantity(quantity)
                .status(BatchStatus.AVAILABLE)
                .readyAt(now)
                .expiresAt(expiresAt)
                .expiryDate(expiresAt == null ? null : expiresAt.toLocalDate())
                .build());
        String note = "Chế biến " + material.getName() + " lô #" + batch.getId() + (request.note() == null ? "" : " · " + request.note());
        var staff = staffService.current();
        for (MaterialComponent c : material.getComponents()) {
            inventoryService.consume(c.getComponent(), scaled(c.getQuantity(), batches),
                    MaterialTransactionType.PRODUCTION_USE, staff, null, note, batch);
        }
        List<MaterialTransaction> inputs = transactionRepository.findInputsOf(batch.getId());
        batch.setUnitCost(unitCost(inputs, quantity));
        material.setStockQuantity(material.getStockQuantity().add(quantity));
        inventoryService.record(batch, MaterialTransactionType.PRODUCE, quantity, staff, null, note, null);
        inventoryService.checkLowStock(material);
        coffeeStatusBackgroundService.requestRun(); // raw stock went down, prepared stock went up
        return BatchResponse.from(batch, inputs);
    }

    /** Cost of the inputs (by the price of each raw batch used) spread over the yield; null if a price is missing. */
    private static BigDecimal unitCost(List<MaterialTransaction> inputs, BigDecimal quantity) {
        BigDecimal total = BigDecimal.ZERO;
        for (MaterialTransaction t : inputs) {
            if (t.getBatch().getUnitCost() == null) return null;
            total = total.add(t.getBatch().getUnitCost().multiply(t.getQuantity().negate()));
        }
        return total.divide(quantity, 2, RoundingMode.HALF_UP);
    }

    /** standard quantity × batches, kept to the 2 decimals the stock columns hold */
    private static BigDecimal scaled(BigDecimal standard, BigDecimal batches) {
        return standard.multiply(batches).setScale(2, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
