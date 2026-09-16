package com.coffeeshop.crm.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ping")
public class PingController {

    @GetMapping
    public Map<String, Object> ping(Authentication authentication) {
        return Map.of(
                "module", "app-crm",
                "authenticatedAs", authentication.getName(),
                "authorities", authentication.getAuthorities());
    }
}
