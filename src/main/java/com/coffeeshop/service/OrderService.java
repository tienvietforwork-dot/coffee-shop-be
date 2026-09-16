package com.coffeeshop.service;

import com.coffeeshop.dto.request.OrderItemRequest;
import com.coffeeshop.dto.request.OrderRequest;
import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.OrderItem;
import com.coffeeshop.entity.Product;
import com.coffeeshop.entity.ProductMaterial;
import com.coffeeshop.entity.User;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.InsufficientStockException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.ProductMaterialRepository;
import com.coffeeshop.repository.ProductRepository;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final List<OrderStatus> FORWARD_TRANSITIONS = List.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
            OrderStatus.READY, OrderStatus.COMPLETED
    );

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductMaterialRepository productMaterialRepository;
    private final UserRepository userRepository;
    private final MaterialService materialService;
    private final NotificationService notificationService;

    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream().map(OrderResponse::from).collect(Collectors.toList());
    }

    public OrderResponse findById(Long id) {
        return OrderResponse.from(getEntity(id));
    }

    @Transactional
    public OrderResponse create(OrderRequest request, String actorUsername) {
        Map<Long, Product> products = new HashMap<>();
        Map<Long, BigDecimal> requiredMaterials = new HashMap<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.getProductId()));
            products.put(itemRequest.getProductId(), product);

            List<ProductMaterial> recipe = productMaterialRepository.findByProductId(product.getId());
            for (ProductMaterial pm : recipe) {
                BigDecimal needed = pm.getQuantityRequired().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
                requiredMaterials.merge(pm.getMaterial().getId(), needed, BigDecimal::add);
                if (pm.getMaterial().getQuantityInStock().compareTo(
                        requiredMaterials.get(pm.getMaterial().getId())) < 0) {
                    throw new InsufficientStockException(
                            "Not enough stock of '" + pm.getMaterial().getName() + "' to fulfill this order");
                }
            }
        }

        User actor = actorUsername == null ? null : userRepository.findByUsername(actorUsername).orElse(null);

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .orderType(request.getOrderType())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .createdBy(actor)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = products.get(itemRequest.getProductId());
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            total = total.add(subtotal);
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build());
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        notificationService.notifyOrderChanged(OrderResponse.from(saved));
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus, String actorUsername) {
        Order order = getEntity(id);
        validateTransition(order.getStatus(), newStatus);

        if (newStatus == OrderStatus.COMPLETED) {
            Map<Long, BigDecimal> materialUsage = computeMaterialUsage(order);
            materialService.deductForOrder(materialUsage, order.getId(), actorUsername);
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        notificationService.notifyOrderChanged(OrderResponse.from(saved));
        return OrderResponse.from(saved);
    }

    private Map<Long, BigDecimal> computeMaterialUsage(Order order) {
        Map<Long, BigDecimal> usage = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            List<ProductMaterial> recipe = productMaterialRepository.findByProductId(item.getProduct().getId());
            for (ProductMaterial pm : recipe) {
                BigDecimal needed = pm.getQuantityRequired().multiply(BigDecimal.valueOf(item.getQuantity()));
                usage.merge(pm.getMaterial().getId(), needed, BigDecimal::add);
            }
        }
        return usage;
    }

    private void validateTransition(OrderStatus current, OrderStatus target) {
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already " + current + " and cannot be changed");
        }
        if (target == OrderStatus.CANCELLED) {
            return; // cancellation allowed from any non-terminal state
        }
        int currentIdx = FORWARD_TRANSITIONS.indexOf(current);
        int targetIdx = FORWARD_TRANSITIONS.indexOf(target);
        if (targetIdx <= currentIdx) {
            throw new BadRequestException("Invalid status transition from " + current + " to " + target);
        }
    }

    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + datePart + "-" + randomPart;
    }

    private Order getEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}
