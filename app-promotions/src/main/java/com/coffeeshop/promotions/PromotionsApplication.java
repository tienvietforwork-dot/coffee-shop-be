package com.coffeeshop.promotions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PromotionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromotionsApplication.class, args);
    }
}
