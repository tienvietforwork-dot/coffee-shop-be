package com.coffeeshop.service;

import com.coffeeshop.dto.request.MaterialRequest;
import com.coffeeshop.dto.request.MaterialTransactionRequest;
import com.coffeeshop.dto.response.MaterialResponse;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.MaterialTransaction;
import com.coffeeshop.entity.User;
import com.coffeeshop.entity.enums.MaterialTransactionType;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.InsufficientStockException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.MaterialRepository;
import com.coffeeshop.repository.MaterialTransactionRepository;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialTransactionRepository materialTransactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<MaterialResponse> findAll() {
        return materialRepository.findAll().stream().map(MaterialResponse::from).collect(Collectors.toList());
    }

    public MaterialResponse findById(Long id) {
        return MaterialResponse.from(getEntity(id));
    }

    public List<MaterialResponse> findLowStock() {
        return materialRepository.findLowStock().stream().map(MaterialResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public MaterialResponse create(MaterialRequest request) {
        Material material = Material.builder()
                .name(request.getName())
                .unit(request.getUnit())
                .quantityInStock(request.getQuantityInStock())
                .minThreshold(request.getMinThreshold())
                .unitPrice(request.getUnitPrice())
                .build();
        return MaterialResponse.from(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse update(Long id, MaterialRequest request) {
        Material material = getEntity(id);
        material.setName(request.getName());
        material.setUnit(request.getUnit());
        material.setQuantityInStock(request.getQuantityInStock());
        material.setMinThreshold(request.getMinThreshold());
        material.setUnitPrice(request.getUnitPrice());
        return MaterialResponse.from(materialRepository.save(material));
    }

    @Transactional
    public void delete(Long id) {
        materialRepository.delete(getEntity(id));
    }

    /**
     * Records a manual material transaction (restock IN, manual OUT, or ADJUST) and
     * updates the stock level accordingly. Automatic OUT transactions triggered by
     * completed orders go through {@link #deductForOrder}.
     */
    @Transactional
    public MaterialResponse recordTransaction(MaterialTransactionRequest request, String actorUsername) {
        Material material = getEntity(request.getMaterialId());
        boolean wasBelow = material.isBelowThreshold();

        applyDelta(material, request.getType(), request.getQuantity());

        User actor = actorUsername == null ? null : userRepository.findByUsername(actorUsername).orElse(null);
        materialTransactionRepository.save(MaterialTransaction.builder()
                .material(material)
                .type(request.getType())
                .quantity(request.getQuantity())
                .reason(request.getReason())
                .createdBy(actor)
                .build());

        materialRepository.save(material);
        alertIfNewlyBelowThreshold(material, wasBelow);
        return MaterialResponse.from(material);
    }

    /**
     * Deducts stock for every material consumed by the given recipe map
     * (materialId -> quantity to deduct) and writes an audit trail row per material.
     * Called when an order transitions to COMPLETED.
     */
    @Transactional
    public void deductForOrder(Map<Long, BigDecimal> materialUsage, Long orderId, String actorUsername) {
        User actor = actorUsername == null ? null : userRepository.findByUsername(actorUsername).orElse(null);

        for (Map.Entry<Long, BigDecimal> entry : materialUsage.entrySet()) {
            Material material = getEntity(entry.getKey());
            boolean wasBelow = material.isBelowThreshold();

            if (material.getQuantityInStock().compareTo(entry.getValue()) < 0) {
                throw new InsufficientStockException(
                        "Not enough stock for material '" + material.getName() + "' to complete order " + orderId);
            }
            material.setQuantityInStock(material.getQuantityInStock().subtract(entry.getValue()));
            materialRepository.save(material);

            materialTransactionRepository.save(MaterialTransaction.builder()
                    .material(material)
                    .type(MaterialTransactionType.OUT)
                    .quantity(entry.getValue())
                    .reason("Auto deduction for completed order #" + orderId)
                    .createdBy(actor)
                    .build());

            alertIfNewlyBelowThreshold(material, wasBelow);
        }
    }

    private void applyDelta(Material material, MaterialTransactionType type, BigDecimal quantity) {
        switch (type) {
            case IN -> material.setQuantityInStock(material.getQuantityInStock().add(quantity));
            case OUT -> {
                if (material.getQuantityInStock().compareTo(quantity) < 0) {
                    throw new InsufficientStockException("Not enough stock for material '" + material.getName() + "'");
                }
                material.setQuantityInStock(material.getQuantityInStock().subtract(quantity));
            }
            case ADJUST -> material.setQuantityInStock(quantity);
            default -> throw new BadRequestException("Unsupported transaction type: " + type);
        }
    }

    private void alertIfNewlyBelowThreshold(Material material, boolean wasBelow) {
        if (!wasBelow && material.isBelowThreshold()) {
            notificationService.notifyInventoryAlert(MaterialResponse.from(material));
        }
    }

    private Material getEntity(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + id));
    }
}
