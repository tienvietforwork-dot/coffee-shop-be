package com.coffeeshop.controller;

import com.coffeeshop.dto.request.ChangePasswordRequest;
import com.coffeeshop.dto.request.LoginRequest;
import com.coffeeshop.dto.request.RegisterRequest;
import com.coffeeshop.dto.response.LoginResponse;
import com.coffeeshop.dto.response.MeResponse;
import com.coffeeshop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Staff, managers and customers all log in here. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Optional customer sign-up (role CUSTOMER). */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** Current account + permissions (with screen paths) to build the menu. */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public MeResponse me() {
        return authService.me();
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
    }
}
