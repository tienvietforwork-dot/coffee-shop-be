package com.coffeeshop.service;

import com.coffeeshop.dto.request.UserRequest;
import com.coffeeshop.dto.response.UserResponse;
import com.coffeeshop.entity.Role;
import com.coffeeshop.entity.User;
import com.coffeeshop.entity.UserRole;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CustomerRepository;
import com.coffeeshop.repository.RoleRepository;
import com.coffeeshop.repository.StaffRepository;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAllWithRoles().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsername(request.username().trim())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại: " + request.username());
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new BadRequestException("Mật khẩu tối thiểu 6 ký tự");
        }
        User user = User.builder().passwordHash(passwordEncoder.encode(request.password())).build();
        apply(user, request);
        setRoles(user, request.roles());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = get(id);
        if (!user.getUsername().equals(request.username().trim()) && userRepository.existsByUsername(request.username().trim())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại: " + request.username());
        }
        if (id.equals(CurrentUser.userId()) && (Boolean.FALSE.equals(request.active()) || !request.roles().contains(Role.ADMIN)
                && user.getRoles().stream().anyMatch(r -> r.getCode().equals(Role.ADMIN)))) {
            throw new BadRequestException("Không thể tự khóa hoặc bỏ quyền Quản lý của chính mình");
        }
        apply(user, request);
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) throw new BadRequestException("Mật khẩu tối thiểu 6 ký tự");
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        setRoles(user, request.roles());
        permissionService.invalidateAll();
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        if (id.equals(CurrentUser.userId())) throw new BadRequestException("Không thể xóa tài khoản đang đăng nhập");
        User user = get(id);
        user.getUserRoles().forEach(UserRole::softDelete);
        user.softDelete();
        permissionService.invalidateAll();
    }

    private void apply(User user, UserRequest r) {
        user.setUsername(r.username().trim());
        user.setFullName(r.fullName());
        user.setEmail(r.email() == null || r.email().isBlank() ? null : r.email().trim());
        user.setPhone(r.phone());
        if (r.active() != null) user.setActive(r.active());
        Long selfId = user.getId() == null ? -1L : user.getId();
        if (r.staffId() != null && userRepository.existsByStaffIdAndIdNot(r.staffId(), selfId)) {
            throw new BadRequestException("Nhân viên này đã có tài khoản khác");
        }
        if (r.customerId() != null && userRepository.existsByCustomerIdAndIdNot(r.customerId(), selfId)) {
            throw new BadRequestException("Khách hàng này đã có tài khoản khác");
        }
        user.setStaff(r.staffId() == null ? null : staffRepository.findById(r.staffId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên: " + r.staffId())));
        user.setCustomer(r.customerId() == null ? null : customerRepository.findById(r.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng: " + r.customerId())));
    }

    /** Soft-deletes roles no longer wanted and adds the new ones. */
    private void setRoles(User user, List<String> codes) {
        Set<String> wanted = new HashSet<>(codes);
        List<Role> roles = roleRepository.findByCodeIn(wanted);
        if (roles.size() != wanted.size()) throw new BadRequestException("Role không hợp lệ: " + codes);
        user.getUserRoles().removeIf(ur -> {
            if (wanted.contains(ur.getRole().getCode())) return false;
            ur.softDelete();
            return true;
        });
        Set<String> have = new HashSet<>(user.getRoles().stream().map(Role::getCode).toList());
        for (Role role : roles) {
            if (!have.contains(role.getCode())) user.getUserRoles().add(UserRole.builder().user(user).role(role).build());
        }
    }

    private User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + id));
    }
}
