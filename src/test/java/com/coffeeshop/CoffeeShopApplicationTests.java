package com.coffeeshop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CoffeeShopApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts cleanly (Hibernate creates the
        // schema on the in-memory H2 test database from the entities).
    }
}
