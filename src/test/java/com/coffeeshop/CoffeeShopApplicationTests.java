package com.coffeeshop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CoffeeShopApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context (incl. Flyway migrations against H2) starts cleanly.
    }
}
