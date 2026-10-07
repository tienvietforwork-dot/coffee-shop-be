package com.coffeeshop.service;

import com.coffeeshop.dto.request.FinishBatchRequest;
import com.coffeeshop.dto.request.ProduceRequest;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.MaterialBatch;
import com.coffeeshop.entity.MaterialComponent;
import com.coffeeshop.entity.MaterialTransaction;
import com.coffeeshop.entity.enums.BatchStatus;
import com.coffeeshop.entity.enums.MaterialKind;
import com.coffeeshop.entity.enums.MaterialTransactionType;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.repository.MaterialBatchRepository;
import com.coffeeshop.repository.MaterialTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionServiceTest {

    @Mock InventoryService inventoryService;
    @Mock MaterialBatchRepository batchRepository;
    @Mock MaterialTransactionRepository transactionRepository;
    @Mock StaffService staffService;
    @Mock CoffeeStatusBackgroundService coffeeStatusBackgroundService;
    @InjectMocks ProductionService productionService;

    Material beans = Material.builder().id(2L).name("Hạt Arabica").unit("g").stockQuantity(new BigDecimal("5000")).build();
    Material coldBrew = Material.builder().id(20L).name("Cốt cold brew").unit("ml").kind(MaterialKind.PREPARED)
            .yieldQuantity(new BigDecimal("1000")).prepMinutes(18 * 60).shelfLifeMinutes(7 * 24 * 60).build();

    @BeforeEach
    void setUp() {
        coldBrew.getComponents().add(MaterialComponent.builder().material(coldBrew).component(beans).quantity(new BigDecimal("250")).build());
    }

    @Test
    void startTakesTheFormulaTimesBatchesAndOpensAPreparingBatch() {
        when(inventoryService.get(20L)).thenReturn(coldBrew);
        when(batchRepository.save(any())).thenAnswer(i -> { MaterialBatch b = i.getArgument(0); b.setId(7L); return b; });

        var res = productionService.start(new ProduceRequest(20L, new BigDecimal("2"), null));

        verify(inventoryService).consume(eq(beans), argThat(q -> q.compareTo(new BigDecimal("500")) == 0), eq(MaterialTransactionType.PRODUCTION_USE),
                any(), eq(null), any(), any(MaterialBatch.class));
        assertThat(res.status()).isEqualTo(BatchStatus.PREPARING);
        assertThat(res.importQuantity()).isEqualByComparingTo("2000");   // expected yield
        assertThat(res.remainingQuantity()).isEqualByComparingTo("0");   // not usable yet
        assertThat(res.readyAt()).isNotNull();
        verify(coffeeStatusBackgroundService).requestRun();
    }

    @Test
    void finishTakesAPerLotExpiry() {
        MaterialBatch batch = MaterialBatch.builder().id(8L).material(coldBrew).importQuantity(new BigDecimal("1000"))
                .remainingQuantity(BigDecimal.ZERO).status(BatchStatus.PREPARING).build();
        when(batchRepository.findById(8L)).thenReturn(Optional.of(batch));
        var shorter = java.time.LocalDateTime.now().plusDays(3);
        var res = productionService.finish(8L, new FinishBatchRequest(new BigDecimal("1000"), shorter, null));
        assertThat(res.expiresAt()).isEqualTo(shorter);
        assertThat(res.expiryDate()).isEqualTo(shorter.toLocalDate());
    }

    @Test
    void startChecksStockBeforeTouchingAnything() {
        when(inventoryService.get(20L)).thenReturn(coldBrew);
        assertThatThrownBy(() -> productionService.start(new ProduceRequest(20L, new BigDecimal("21"), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Không đủ Hạt Arabica: cần 5250 g, còn 5000 g");
        verify(batchRepository, never()).save(any());
    }

    @Test
    void finishPutsTheActualYieldInStockWithExpiryAndCost() {
        MaterialBatch batch = MaterialBatch.builder().id(7L).material(coldBrew).importQuantity(new BigDecimal("2000"))
                .remainingQuantity(BigDecimal.ZERO).status(BatchStatus.PREPARING).build();
        MaterialBatch beanLot = MaterialBatch.builder().id(3L).material(beans).unitCost(new BigDecimal("450")).build();
        when(batchRepository.findById(7L)).thenReturn(Optional.of(batch));
        when(transactionRepository.findInputsOf(7L)).thenReturn(List.of(
                MaterialTransaction.builder().batch(beanLot).quantity(new BigDecimal("-500")).build()));

        var res = productionService.finish(7L, new FinishBatchRequest(new BigDecimal("1950"), null, null));

        assertThat(res.status()).isEqualTo(BatchStatus.AVAILABLE);
        assertThat(res.remainingQuantity()).isEqualByComparingTo("1950");
        assertThat(res.expiryDate()).isEqualTo(LocalDate.now().plusDays(7));
        assertThat(res.expiresAt()).isAfter(java.time.LocalDateTime.now().plusDays(7).minusMinutes(1));
        assertThat(res.unitCost()).isEqualByComparingTo("115.38");     // 500 g × 450đ / 1950 ml
        assertThat(coldBrew.getStockQuantity()).isEqualByComparingTo("1950");
        assertThat(res.inputs()).singleElement().satisfies(i -> assertThat(i.quantity()).isEqualByComparingTo("500"));
    }
}
