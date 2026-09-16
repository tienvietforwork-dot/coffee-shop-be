package com.coffeeshop.service;

import com.coffeeshop.dto.request.LoginRequest;
import com.coffeeshop.dto.response.LoginResponse;
import com.coffeeshop.security.JwtUtil;
import com.coffeeshop.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtUtil.generateToken(principal.getUsername(), principal.getRole(), principal.getId());

        return LoginResponse.builder()
                .token(token)
                .username(principal.getUsername())
                .role(principal.getRole())
                .userId(principal.getId())
                .build();
    }
}
