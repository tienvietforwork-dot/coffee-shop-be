package com.coffeeshop.promotions.service;

import com.coffeeshop.promotions.dto.request.VoucherDetailRequest;
import com.coffeeshop.promotions.dto.request.VoucherRequest;
import com.coffeeshop.promotions.dto.response.VoucherDetailResponse;
import com.coffeeshop.promotions.dto.response.VoucherResponse;
import com.coffeeshop.promotions.entity.Promotion;
import com.coffeeshop.promotions.entity.Voucher;
import com.coffeeshop.promotions.entity.VoucherDetail;
import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import com.coffeeshop.promotions.entity.enums.VoucherTypeStatus;
import com.coffeeshop.promotions.repository.VoucherDetailRepository;
import com.coffeeshop.promotions.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherDetailRepository voucherDetailRepository;

    @Transactional(readOnly = true)
    public List<VoucherResponse> getAll(){
        return voucherRepository.findAll().stream().map(VoucherResponse::new).collect(Collectors.toList());
    }

    @Transactional
    public VoucherResponse create(VoucherRequest request){

        VoucherTypeStatus defaultStatus = request.getStatus() != null ? request.getStatus() : VoucherTypeStatus.ACTIVE;
        Voucher voucher = Voucher.builder()
                .name(request.getName())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .quantity(request.getQuantity())
                .validDays(request.getValidDate())
                .status(defaultStatus)
                .build();

        Voucher saved = voucherRepository.save(voucher);
        return new VoucherResponse(saved);
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherRequest request){
        Voucher voucher = findVoucherById(id);

        voucher.setName(request.getName());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setQuantity(request.getQuantity());
        voucher.setValidDays(request.getValidDate());
        voucher.setStatus(request.getStatus());

        Voucher update = voucherRepository.save(voucher);
        return new VoucherResponse(update);
    }

    @Transactional
    public void delete(@PathVariable Long id){
        Voucher voucher = findVoucherById(id);
        for(VoucherDetail items : voucher.getVoucherDetails()){
            items.setDelFlag(true);
        }
        voucherRepository.delete(voucher);
    }

    @Transactional
    public VoucherResponse createVoucherDetail(Long id, VoucherDetailRequest request) {
        Voucher voucher = findVoucherById(id);

        if (voucher.getStatus() != VoucherTypeStatus.ACTIVE) {
            throw new RuntimeException("Voucher đã hết hạn hoặc không còn hoạt động!");
        }

        if (voucher.getQuantity() == null || voucher.getQuantity() <= 0){
            throw new RuntimeException("Voucher đã phát hàng hết số lượng!");
        }

        voucher.setQuantity(voucher.getQuantity() - 1);

        Long customerId = getCurrentCustomerId();

        VoucherDetail voucherDetail = VoucherDetail.builder()
                .voucher(voucher)
                .customer(customerId)
                .order(request.getOrderId() != null ? request.getOrderId() : 0L)
                .incident(request.getIncidentId() != null ? request.getIncidentId() : 0L)
                .code(request.getCode())
                .status(request.getStatus() != null ? request.getStatus() : VoucherStatus.ISSUED)
                .issuedAt(request.getIssuedAt() != null ? request.getIssuedAt() : LocalDateTime.now())
                .expiresAt(request.getExpiresAt() != null ? request.getExpiresAt() : LocalDateTime.now().plusDays(voucher.getValidDays()))
                .build();

        voucher.addVoucherDetail(voucherDetail);

        Voucher savedVoucher = voucherRepository.save(voucher);

        return new VoucherResponse(savedVoucher);
    }

    @Transactional
    public VoucherDetailResponse useVoucherInOrder(Long voucherDetailId, Long orderId) {
        VoucherDetail voucherDetail = voucherDetailRepository.findById(voucherDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã voucher chi tiết!"));

        Long currentCustomerId = getCurrentCustomerId();
        if (!voucherDetail.getCustomer().equals(currentCustomerId)) {
            throw new RuntimeException("Voucher này không thuộc sở hữu của bạn!");
        }

        if (voucherDetail.getStatus() != VoucherStatus.ISSUED) {
            throw new RuntimeException("Voucher không khả dụng (Đã được sử dụng, thu hồi hoặc hết hạn)!");
        }

        if (voucherDetail.getExpiresAt().isBefore(LocalDateTime.now())) {
            voucherDetail.setStatus(VoucherStatus.EXPIRED);
            voucherDetailRepository.save(voucherDetail);
            throw new RuntimeException("Voucher đã quá hạn sử dụng!");
        }

        voucherDetail.setStatus(VoucherStatus.USED);
        voucherDetail.setOrder(orderId);

        VoucherDetail updated = voucherDetailRepository.save(voucherDetail);

        return new VoucherDetailResponse(updated);
    }




    private Voucher findVoucherById(Long id){
        return voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found with id: " + id));
    }

    private Long getCurrentCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("User not authenticated");
        }

        String actor = authentication.getName();
        if (actor == null || actor.isBlank()) {
            throw new RuntimeException("Actor is empty");
        }

        try {
            return Long.valueOf(actor);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid customer ID format: " + actor);
        }
    }




}
