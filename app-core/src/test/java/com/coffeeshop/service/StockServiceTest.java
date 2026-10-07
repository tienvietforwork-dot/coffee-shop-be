package com.coffeeshop.service;

import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.Recipe;
import com.coffeeshop.entity.RecipeMaterial;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.entity.enums.CoffeeStatus;
import com.coffeeshop.repository.CoffeeRepository;
import com.coffeeshop.repository.RecipeRepository;
import com.coffeeshop.websocket.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    RecipeRepository recipeRepository;
    @Mock
    CoffeeRepository coffeeRepository;
    @Mock
    NotificationService notificationService;
    @InjectMocks
    StockService stockService;

    Material beans = Material.builder().id(1L).name("Hạt Arabica").unit("g").stockQuantity(new BigDecimal("900")).build();
    Material milk = Material.builder().id(2L).name("Sữa tươi").unit("ml").stockQuantity(new BigDecimal("1000")).build();
    Coffee latte = Coffee.builder().id(10L).name("Latte").build();
    Coffee espresso = Coffee.builder().id(11L).name("Espresso").build();

    @BeforeEach
    void setUp() {
        when(recipeRepository.findAllActiveWithMaterials()).thenReturn(List.of(
                recipe(latte, line(beans, "18"), line(milk, "180")),
                recipe(espresso, line(beans, "18"))));
    }

    @Test
    void servingsIsTheScarcestMaterial() {
        StockService.Snapshot snap = stockService.snapshot();
        assertThat(snap.servings(10L)).isEqualTo(5);   // milk: floor(1000 / 180) = 5, beans would allow 50
        assertThat(snap.servings(11L)).isEqualTo(50);
        assertThat(snap.servings(99L)).isNull();       // no recipe = not limited
    }

    @Test
    void orderWithinStockPasses() {
        stockService.requireStock(List.of(new PricingService.InputLine(latte, 5, null)));
    }

    @Test
    void sharedMaterialIsCountedAcrossLines() {
        // 5 lattes use 90g beans; 45 espressos need 810g more -> 900g total is fine, 46 is not
        stockService.requireStock(List.of(new PricingService.InputLine(latte, 5, null), new PricingService.InputLine(espresso, 45, null)));
        assertThatThrownBy(() -> stockService.requireStock(List.of(
                new PricingService.InputLine(latte, 5, null), new PricingService.InputLine(espresso, 46, null))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Không đủ nguyên liệu cho đơn này: Hạt Arabica (cần 918 g, còn 900 g)");
    }

    @Test
    void outOfStockSwitchesToSoldOutAndBack() {
        latte.setStatus(CoffeeStatus.AVAILABLE);
        espresso.setStatus(CoffeeStatus.SOLD_OUT); // set by hand: must stay
        when(coffeeRepository.findByStatusInOrderByNameAsc(any())).thenReturn(List.of(latte, espresso));

        milk.setStockQuantity(new BigDecimal("100")); // < 180 ml per latte
        assertThat(stockService.syncCoffeeStatus()).extracting(StockService.StatusChange::status).containsExactly(CoffeeStatus.SOLD_OUT);
        assertThat(latte.getStatus()).isEqualTo(CoffeeStatus.SOLD_OUT);
        assertThat(latte.isAutoSoldOut()).isTrue();
        verify(notificationService).notifyMenuChanged(any());

        milk.setStockQuantity(new BigDecimal("2000"));
        stockService.syncCoffeeStatus();
        assertThat(latte.getStatus()).isEqualTo(CoffeeStatus.AVAILABLE);
        assertThat(espresso.getStatus()).isEqualTo(CoffeeStatus.SOLD_OUT);
    }

    private static RecipeMaterial line(Material m, String qty) {
        return RecipeMaterial.builder().material(m).quantity(new BigDecimal(qty)).build();
    }

    private static Recipe recipe(Coffee coffee, RecipeMaterial... lines) {
        Recipe r = Recipe.builder().coffee(coffee).active(true).build();
        r.getMaterials().addAll(List.of(lines));
        return r;
    }
}
