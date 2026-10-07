package com.coffeeshop.service;

import com.coffeeshop.dto.request.CartItemRequest;
import com.coffeeshop.dto.request.CartItemUpdateRequest;
import com.coffeeshop.dto.request.CreateCartRequest;
import com.coffeeshop.dto.response.CartResponse;
import com.coffeeshop.entity.Cart;
import com.coffeeshop.entity.CartItem;
import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.DiningTable;
import com.coffeeshop.entity.enums.CartStatus;
import com.coffeeshop.entity.enums.CoffeeStatus;
import com.coffeeshop.entity.enums.TableStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CartRepository;
import com.coffeeshop.repository.CustomerRepository;
import com.coffeeshop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Guest carts identified by a random session code kept in the browser. */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CoffeeService coffeeService;
    private final DiningTableService tableService;
    private final PricingService pricingService;
    private final CustomerRepository customerRepository;

    @Transactional
    public CartResponse create(CreateCartRequest request) {
        DiningTable table = null;
        if (request != null && request.tableQr() != null && !request.tableQr().isBlank()) {
            table = tableService.getByQr(request.tableQr().trim());
            if (table.getStatus() == TableStatus.INACTIVE) throw new BadRequestException("Bàn này đang tạm ngưng phục vụ");
        }
        Cart cart = cartRepository.save(Cart.builder()
                .sessionCode(UUID.randomUUID().toString().replace("-", ""))
                .table(table)
                .customer(CurrentUser.customerId() == null ? null
                        : customerRepository.findById(CurrentUser.customerId()).orElse(null))
                .build());
        return toResponse(cart);
    }

    /** Khôi phục giỏ hàng – prices are refreshed to the current price on every read. */
    @Transactional
    public CartResponse get(String sessionCode) {
        Cart cart = getEntity(sessionCode);
        refreshPrices(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String sessionCode, CartItemRequest request) {
        Cart cart = getOpen(sessionCode);
        Coffee coffee = coffeeService.get(request.coffeeId());
        if (coffee.getStatus() != CoffeeStatus.AVAILABLE) throw new BadRequestException(coffee.getName() + " hiện đã hết");
        String note = blankToNull(request.note());
        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getCoffee().getId().equals(coffee.getId()) && Objects.equals(i.getNote(), note))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setQuantity(Math.min(99, existing.getQuantity() + request.quantity()));
        } else {
            cart.getItems().add(CartItem.builder().cart(cart).coffee(coffee).quantity(request.quantity()).note(note)
                    .subtotal(BigDecimal.ZERO).build());
        }
        refreshPrices(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(String sessionCode, Long itemId, CartItemUpdateRequest request) {
        Cart cart = getOpen(sessionCode);
        CartItem item = findItem(cart, itemId);
        item.setQuantity(request.quantity());
        item.setNote(blankToNull(request.note()));
        refreshPrices(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(String sessionCode, Long itemId) {
        Cart cart = getOpen(sessionCode);
        CartItem item = findItem(cart, itemId);
        item.softDelete();
        cart.getItems().remove(item);
        refreshPrices(cart);
        return toResponse(cart);
    }

    Cart getOpen(String sessionCode) {
        Cart cart = getEntity(sessionCode);
        if (cart.getStatus() != CartStatus.OPEN) throw new BadRequestException("Giỏ hàng đã được đặt hoặc đã hết hạn");
        return cart;
    }

    private Cart getEntity(String sessionCode) {
        return cartRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));
    }

    private CartItem findItem(Cart cart, Long itemId) {
        return cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Món không có trong giỏ"));
    }

    private void refreshPrices(Cart cart) {
        if (cart.getStatus() != CartStatus.OPEN) return;
        LocalDateTime now = LocalDateTime.now();
        for (CartItem item : cart.getItems()) {
            item.setSubtotal(pricingService.currentUnitPrice(item.getCoffee(), now)
                    .multiply(BigDecimal.valueOf(item.getQuantity())));
        }
    }

    private CartResponse toResponse(Cart cart) {
        var items = cart.getItems().stream().map(i -> new CartResponse.Item(i.getId(), i.getCoffee().getId(),
                i.getCoffee().getName(), i.getCoffee().imageSrc(), i.getQuantity(),
                i.getSubtotal().divide(BigDecimal.valueOf(i.getQuantity()), 2, java.math.RoundingMode.HALF_UP),
                i.getNote(), i.getSubtotal(), i.getCoffee().getStatus() == CoffeeStatus.AVAILABLE)).toList();
        BigDecimal subtotal = cart.getItems().stream().map(CartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getSessionCode(), cart.getStatus(),
                cart.getTable() == null ? null : cart.getTable().getId(),
                cart.getTable() == null ? null : cart.getTable().getTableNo(), items, subtotal);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
