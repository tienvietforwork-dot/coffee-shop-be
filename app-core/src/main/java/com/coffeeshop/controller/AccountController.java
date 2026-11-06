package com.coffeeshop.controller;

import com.coffeeshop.dto.request.ProfileRequest;
import com.coffeeshop.dto.response.AccountResponse;
import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.dto.response.PointHistoryResponse;
import com.coffeeshop.dto.response.VoucherResponse;
import com.coffeeshop.security.Perm;
import com.coffeeshop.repository.CustomerAddressRepository;
import com.coffeeshop.security.CurrentUser;
import com.coffeeshop.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Khách hàng đã đăng nhập: hồ sơ, đơn hàng, voucher, điểm tích lũy, địa chỉ giao hàng đã lưu của chính mình. */
@RestController
@RequestMapping("/api/account")
@PreAuthorize(Perm.CAN_ACCOUNT)
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final CustomerAddressRepository addressRepository;
    private final com.coffeeshop.service.CustomerService customerService;

    /** địa chỉ giao hàng đã lưu (mặc định trước) — chọn ở trang đặt hàng để tự điền */
    public record SavedAddress(Long id, String label, String recipientName, String recipientPhone, String address, boolean isDefault) {
    }

    /** thêm / sửa địa chỉ trong sổ địa chỉ của mình */
    public record AddressRequest(@jakarta.validation.constraints.Size(max = 30) String label,
                                 @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String recipientName,
                                 @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại không hợp lệ") String recipientPhone,
                                 @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 255) String address,
                                 Boolean isDefault) {
    }

    @GetMapping("/addresses")
    public List<SavedAddress> addresses() {
        Long id = CurrentUser.customerId();
        if (id == null) return List.of();
        return addressRepository.findByCustomerIdOrderByDefaultAddressDescIdDesc(id).stream().map(AccountController::view).toList();
    }

    @PostMapping("/addresses")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public SavedAddress addAddress(@Valid @RequestBody AddressRequest r) {
        return view(customerService.saveMyAddress(CurrentUser.customerId(), null, r.label(), r.recipientName(), r.recipientPhone(), r.address(), Boolean.TRUE.equals(r.isDefault())));
    }

    @PutMapping("/addresses/{addressId}")
    public SavedAddress updateAddress(@PathVariable Long addressId, @Valid @RequestBody AddressRequest r) {
        return view(customerService.saveMyAddress(CurrentUser.customerId(), addressId, r.label(), r.recipientName(), r.recipientPhone(), r.address(), Boolean.TRUE.equals(r.isDefault())));
    }

    @DeleteMapping("/addresses/{addressId}")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Long addressId) {
        customerService.deleteMyAddress(CurrentUser.customerId(), addressId);
    }

    private static SavedAddress view(com.coffeeshop.entity.CustomerAddress a) {
        return new SavedAddress(a.getId(), a.getLabel(), a.getRecipientName(), a.getRecipientPhone(), a.getAddress(), a.isDefaultAddress());
    }

    @GetMapping
    public AccountResponse profile() {
        return accountService.profile();
    }

    @PutMapping
    public AccountResponse updateProfile(@Valid @RequestBody ProfileRequest request) {
        return accountService.updateProfile(request);
    }

    @GetMapping("/orders")
    public List<OrderResponse> orders() {
        return accountService.orders();
    }

    @GetMapping("/vouchers")
    public List<VoucherResponse> vouchers() {
        return accountService.vouchers();
    }

    @GetMapping("/points")
    public List<PointHistoryResponse> points() {
        return accountService.points();
    }
}
