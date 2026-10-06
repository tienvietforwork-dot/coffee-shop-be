package com.coffeeshop.service;

import com.coffeeshop.dto.request.ProfileRequest;
import com.coffeeshop.dto.response.AccountResponse;
import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.dto.response.PointHistoryResponse;
import com.coffeeshop.dto.response.VoucherResponse;
import com.coffeeshop.entity.Customer;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CustomerRepository;
import com.coffeeshop.repository.LoyaltyPointHistoryRepository;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.repository.VoucherRepository;
import com.coffeeshop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** "Tài khoản của tôi" – a logged-in customer only ever sees their own data. */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final VoucherRepository voucherRepository;
    private final LoyaltyPointHistoryRepository pointRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AccountResponse profile() {
        Customer c = me();
        List<Order> orders = orderRepository.findByCustomerIdOrderByOrderedAtDesc(c.getId());
        BigDecimal spent = orders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AccountResponse(c.getId(), c.getFullName(), c.getPhone(), c.getEmail(), c.getLoyaltyPoints(),
                c.getRegisteredAt(), orders.size(), spent);
    }

    @Transactional
    public AccountResponse updateProfile(ProfileRequest request) {
        Customer c = me();
        c.setFullName(request.fullName() == null || request.fullName().isBlank() ? null : request.fullName().trim());
        c.setEmail(request.email() == null || request.email().isBlank() ? null : request.email().trim());
        userRepository.findById(CurrentUser.userId()).ifPresent(u -> {
            u.setFullName(c.getFullName());
            u.setEmail(c.getEmail());
        });
        return profile();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> orders() {
        return orderRepository.findByCustomerIdOrderByOrderedAtDesc(me().getId()).stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> vouchers() {
        return voucherRepository.findByCustomerIdOrderByIssuedAtDesc(me().getId()).stream().map(VoucherResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PointHistoryResponse> points() {
        return pointRepository.findByCustomerIdOrderByCreatedAtDesc(me().getId()).stream().map(PointHistoryResponse::from).toList();
    }

    private Customer me() {
        Long id = CurrentUser.customerId();
        if (id == null) throw new BadRequestException("Tài khoản không gắn với hồ sơ khách hàng");
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khách hàng"));
    }
}
