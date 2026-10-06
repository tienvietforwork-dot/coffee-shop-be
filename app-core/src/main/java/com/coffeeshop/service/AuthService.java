package com.coffeeshop.service;

import com.coffeeshop.dto.request.ChangePasswordRequest;
import com.coffeeshop.dto.request.LoginRequest;
import com.coffeeshop.dto.request.RegisterRequest;
import com.coffeeshop.dto.response.LoginResponse;
import com.coffeeshop.dto.response.MeResponse;
import com.coffeeshop.dto.response.PermissionResponse;
import com.coffeeshop.dto.response.UserResponse;
import com.coffeeshop.entity.Customer;
import com.coffeeshop.entity.Role;
import com.coffeeshop.entity.User;
import com.coffeeshop.entity.UserRole;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CustomerRepository;
import com.coffeeshop.repository.PermissionRepository;
import com.coffeeshop.repository.RoleRepository;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.security.CurrentUser;
import com.coffeeshop.security.CustomUserDetailsService;
import com.coffeeshop.security.JwtUtil;
import com.coffeeshop.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword()));
        } catch (DisabledException e) {
            throw new BadRequestException("Tài khoản đã bị khóa");
        }
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        userRepository.touchLastLogin(principal.getId(), LocalDateTime.now());
        return toLoginResponse(principal);
    }

    /** Khách hàng tự đăng ký (tùy chọn): SĐT làm tên đăng nhập, gắn với hồ sơ khách hàng có sẵn nếu đã từng mua. */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String phone = request.phone().trim();
        if (userRepository.existsByUsername(phone)) {
            throw new BadRequestException("Số điện thoại đã có tài khoản, vui lòng đăng nhập");
        }
        Customer customer = customerRepository.findByPhone(phone)
                .orElseGet(() -> customerRepository.save(Customer.builder().phone(phone).build()));
        if (userRepository.existsByCustomerId(customer.getId())) {
            throw new BadRequestException("Số điện thoại đã có tài khoản, vui lòng đăng nhập");
        }
        if (request.fullName() != null && !request.fullName().isBlank()) customer.setFullName(request.fullName().trim());
        if (request.email() != null && !request.email().isBlank()) customer.setEmail(request.email().trim());

        Role customerRole = roleRepository.findByCode(Role.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Role CUSTOMER missing – run database/seed.sql"));
        User user = User.builder()
                .username(phone)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(phone)
                .customer(customer)
                .build();
        user.getUserRoles().add(UserRole.builder().user(user).role(customerRole).build());
        userRepository.saveAndFlush(user);
        return toLoginResponse((UserPrincipal) userDetailsService.loadUserByUsername(phone));
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        UserPrincipal principal = CurrentUser.principal();
        if (principal == null) throw new ResourceNotFoundException("Chưa đăng nhập");
        User user = userRepository.findWithRolesByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        return new MeResponse(UserResponse.from(user), permissions(principal));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = userRepository.findById(CurrentUser.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private LoginResponse toLoginResponse(UserPrincipal principal) {
        User user = userRepository.findWithRolesByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + principal.getId()));
        return new LoginResponse(jwtUtil.generateToken(principal), UserResponse.from(user), permissions(principal));
    }

    private List<PermissionResponse> permissions(UserPrincipal principal) {
        if (principal.getPermissions().isEmpty()) return List.of();
        return permissionRepository.findByCodeInOrderBySortOrderAsc(principal.getPermissions()).stream()
                .map(PermissionResponse::from).toList();
    }
}
