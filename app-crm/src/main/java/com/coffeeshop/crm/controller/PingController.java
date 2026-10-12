package com.coffeeshop.crm.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.coffeeshop.crm.dto.response.PingResponse;

@RestController
@RequestMapping("/api/ping")
public class PingController {

    @GetMapping
    public PingResponse ping(Authentication authentication) {
        return new PingResponse("app-crm", authentication.getName(), authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).toList());
    }
}
