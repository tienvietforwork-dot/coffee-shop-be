package com.coffeeshop.service;

import com.coffeeshop.dto.request.PaymentRequest;
import com.coffeeshop.dto.response.PaymentResponse;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.Payment;
import com.coffeeshop.entity.enums.PaymentStatus;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public List<PaymentResponse> findByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream().map(PaymentResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));
        Payment payment = Payment.builder()
                .order(order)
                .method(request.getMethod())
                .amount(request.getAmount())
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();
        return PaymentResponse.from(paymentRepository.save(payment));
    }
}
