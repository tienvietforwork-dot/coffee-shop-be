package com.coffeeshop.service;

import com.coffeeshop.dto.request.MaterialTransactionRequest;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.enums.MaterialTransactionType;
import com.coffeeshop.exception.InsufficientStockException;
import com.coffeeshop.repository.MaterialRepository;
import com.coffeeshop.repository.MaterialTransactionRepository;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.websocket.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private MaterialTransactionRepository materialTransactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MaterialService materialService;

    private Material material;

    @BeforeEach
    void setUp() {
        material = Material.builder()
                .id(1L)
                .name("Coffee Beans")
                .unit("kg")
                .quantityInStock(new BigDecimal("10.000"))
                .minThreshold(new BigDecimal("5.000"))
                .unitPrice(new BigDecimal("200000"))
                .build();
        org.mockito.Mockito.lenient().when(materialRepository.findById(1L)).thenReturn(Optional.of(material));
        org.mockito.Mockito.lenient().when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void inTransactionIncreasesStock() {
        MaterialTransactionRequest request = new MaterialTransactionRequest();
        request.setMaterialId(1L);
        request.setType(MaterialTransactionType.IN);
        request.setQuantity(new BigDecimal("5.000"));

        var response = materialService.recordTransaction(request, null);

        assertThat(response.getQuantityInStock()).isEqualByComparingTo("15.000");
    }

    @Test
    void outTransactionBeyondStockThrows() {
        MaterialTransactionRequest request = new MaterialTransactionRequest();
        request.setMaterialId(1L);
        request.setType(MaterialTransactionType.OUT);
        request.setQuantity(new BigDecimal("50.000"));

        assertThatThrownBy(() -> materialService.recordTransaction(request, null))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void outTransactionCrossingThresholdTriggersAlert() {
        MaterialTransactionRequest request = new MaterialTransactionRequest();
        request.setMaterialId(1L);
        request.setType(MaterialTransactionType.OUT);
        request.setQuantity(new BigDecimal("6.000")); // 10 - 6 = 4 < threshold 5

        materialService.recordTransaction(request, null);

        org.mockito.Mockito.verify(notificationService).notifyInventoryAlert(any());
    }
}
