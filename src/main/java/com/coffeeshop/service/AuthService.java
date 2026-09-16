package com.coffeeshop.service;

import com.coffeeshop.dto.request.LoginRequest;
import com.coffeeshop.dto.response.LoginResponse;
import com.coffeeshop.dto.response.UserResponse;
import com.coffeeshop.entity.User;
import com.coffeeshop.repository.UserRepository;
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
    private final UserRepository userRepository;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtUtil.generateToken(principal.getUsername(), principal.getRole(), principal.getId());

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + principal.getId()));

        return LoginResponse.builder()
                .token(token)
                .user(UserResponse.from(user))
                .build();
    }
}
