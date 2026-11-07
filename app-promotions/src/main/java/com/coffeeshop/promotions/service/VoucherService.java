package com.coffeeshop.promotions.service;

import com.coffeeshop.promotions.dto.request.VoucherDetailRequest;
import com.coffeeshop.promotions.dto.request.VoucherRequest;
import com.coffeeshop.promotions.dto.response.VoucherDetailResponse;
import com.coffeeshop.promotions.dto.response.VoucherResponse;
import com.coffeeshop.promotions.entity.*;
import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import com.coffeeshop.promotions.entity.enums.VoucherTypeStatus;
import com.coffeeshop.promotions.repository.*;
import lombok.RequiredArgsConstructor;
import org.aspectj.apache.bcel.generic.RET;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherDetailRepository voucherDetailRepository;
    private final CustomerRepository customerRepository;
    private final IncidentRepository incidentRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<VoucherResponse> getAll(){
        return voucherRepository.findAll().stream()
                .sorted(Comparator.comparing(v -> getStatusPriority(v.getStatus())))
                .map(VoucherResponse::new).collect(Collectors.toList());
    }

    @Transactional
    public VoucherResponse create(VoucherRequest request){



        VoucherTypeStatus defaultStatus = request.getStatus() != null ? request.getStatus() : VoucherTypeStatus.DRAFT;
        Voucher voucher = Voucher.builder()
                .name(request.getName())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .quantity(0)
                .maxQuantity(request.getMaxQuantity())
                .validDays(request.getValidDate())
                .status(defaultStatus)
                .build();

        int quantity = request.getQuantity() != null ? request.getQuantity() : 0;
        int maxQuantity = request.getMaxQuantity() != null ? request.getMaxQuantity() : Integer.MAX_VALUE;

        if (quantity > maxQuantity) {
            throw new RuntimeException("Số lượng ban đầu không được vượt quá số lượng tối đa (" + maxQuantity + ")!");
        }

        Voucher saved = voucherRepository.save(voucher);
        return new VoucherResponse(saved);
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherRequest request){
        int count = 0;

        Voucher voucher = findVoucherById(id);

        voucher.setName(request.getName());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(request.getMinOrderValue());

        voucher.setMaxQuantity(request.getMaxQuantity());
        voucher.setValidDays(request.getValidDate());
        voucher.setStatus(request.getStatus());


        if (request.getItems() != null){
            List<Long> requestNoNull = request.getItems().stream()
                    .map(VoucherDetailRequest::getId).filter(itemId -> itemId != null)
                    .collect(Collectors.toList());

            voucher.getVoucherDetails().removeIf(item -> !requestNoNull.contains(item.getId()));

            for (VoucherDetailRequest itemReq : request.getItems()){
                Customer customer = findCustomerById(itemReq.getCustomerId());

                Order order = null;
                if (itemReq.getOrderId() != null){
                    order = findOrderById(itemReq.getOrderId());
                }

                Incident incident = null;
                if (itemReq.getIncidentId() != null){
                    incident = findIncidentById(itemReq.getIncidentId());
                }

                if (itemReq.getId() != null) {

                    VoucherDetail existingItem = voucher.getVoucherDetails().stream()
                            .filter(i -> i.getId().equals(itemReq.getId())).findFirst()
                            .orElseThrow(() -> new RuntimeException("VoucherDetail not fount width id: " + id));

                    existingItem.setCustomer(customer);
                    existingItem.setOrder(order);
                    existingItem.setIncident(incident);
                    existingItem.setCode(itemReq.getCode());
                    existingItem.setStatus(itemReq.getStatus());
                    existingItem.setIssuedAt(itemReq.getIssuedAt());
                    existingItem.setExpiresAt(itemReq.getExpiresAt());
                } else {
                    count += 1;
                    VoucherDetail newItem = VoucherDetail.builder()
                            .customer(customer)
                            .order(order)
                            .incident(incident)
                            .code(itemReq.getCode())
                            .status(itemReq.getStatus())
                            .issuedAt(itemReq.getIssuedAt())
                            .expiresAt(itemReq.getExpiresAt())
                            .build();

                    voucher.addVoucherDetail(newItem);
                }
            }
        } else {
            voucher.getVoucherDetails().clear();
        }

        int requestQty = request.getQuantity() != null ? request.getQuantity() : 0;
        int newTotalQuantity = requestQty + count;
        int maxQty = voucher.getMaxQuantity() != null ? voucher.getMaxQuantity() : Integer.MAX_VALUE;

        if (newTotalQuantity <= maxQty){
            voucher.setQuantity(newTotalQuantity);
        } else {
            throw new RuntimeException("Số lượng voucher vượt quá giới hạn tối đa cho phép (" + maxQty + ")!");
        }

        Voucher update = voucherRepository.save(voucher);
        return new VoucherResponse(update);
    }

    @Transactional
    public void delete(@PathVariable Long id){
        Voucher voucher = findVoucherById(id);

        if (voucher.getStatus() != VoucherTypeStatus.DRAFT){
            throw new RuntimeException("Chỉ có thể xóa Voucher đang ở trạng thái DRAFT");
        }

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

        voucher.setQuantity(voucher.getQuantity() + 1);


        Customer customer;
        if (request.getCustomerId() != null) {
            customer = findCustomerById(request.getCustomerId());
        } else {
            Long currentCustomerId = getCurrentCustomerId();
            customer = findCustomerById(currentCustomerId);
        }

        Order order = null;
        if (request.getOrderId() != null) {
            order = findOrderById(request.getOrderId());
        }

        Incident incident = null;
        if (request.getIncidentId() != null) {
            incident = findIncidentById(request.getIncidentId());
        }

        String code = request.getCode();
        if (code == null || code.isBlank()){
            code = getRandomCode();
        }

        VoucherDetail voucherDetail = VoucherDetail.builder()
                .voucher(voucher)
                .customer(customer)
                .order(order)
                .incident(incident)
                .code(code)
                .status(request.getStatus() != null ? request.getStatus() : VoucherStatus.ISSUED)
                .issuedAt(request.getIssuedAt() != null ? request.getIssuedAt() : LocalDateTime.now())
                .expiresAt(request.getExpiresAt() != null ? request.getExpiresAt() : LocalDateTime.now().plusDays(voucher.getValidDays()))
                .build();

        voucher.addVoucherDetail(voucherDetail);

        Voucher savedVoucher = voucherRepository.save(voucher);

        return new VoucherResponse(savedVoucher);
    }
//
//    @Transactional
//    public VoucherDetailResponse useVoucherInOrder(Long voucherDetailId, Long orderId) {
//        VoucherDetail voucherDetail = voucherDetailRepository.findById(voucherDetailId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã voucher chi tiết!"));
//
//        Long currentCustomerId = getCurrentCustomerId();
//        if (!voucherDetail.getCustomer().equals(currentCustomerId)) {
//            throw new RuntimeException("Voucher này không thuộc sở hữu của bạn!");
//        }
//
//        if (voucherDetail.getStatus() != VoucherStatus.ISSUED) {
//            throw new RuntimeException("Voucher không khả dụng (Đã được sử dụng, thu hồi hoặc hết hạn)!");
//        }
//
//        if (voucherDetail.getExpiresAt().isBefore(LocalDateTime.now())) {
//            voucherDetail.setStatus(VoucherStatus.EXPIRED);
//            voucherDetailRepository.save(voucherDetail);
//            throw new RuntimeException("Voucher đã quá hạn sử dụng!");
//        }
//
//        voucherDetail.setStatus(VoucherStatus.USED);
//        voucherDetail.setOrder(orderId);
//
//        VoucherDetail updated = voucherDetailRepository.save(voucherDetail);
//
//        return new VoucherDetailResponse(updated);
//    }
//
//
//
//
    @Transactional
    public void activeVoucher(Long id) {
        Voucher voucher = findVoucherById(id);
        if (voucher.getStatus() != VoucherTypeStatus.DRAFT){
            throw new RuntimeException("Chỉ có thể kích hoạt khi voucher đang ở trạng thái DRAFT");
        }

        voucher.setStatus(VoucherTypeStatus.ACTIVE);
        voucherRepository.save(voucher);
    }

    @Transactional
    public void endVoucher(Long id){
        Voucher voucher = findVoucherById(id);
        if (voucher.getStatus() != VoucherTypeStatus.ACTIVE){
            throw new RuntimeException("Chỉ có thể kích hoạt khi voucher đang ở trạng thái ACTIVE");
        }

        voucher.setStatus(VoucherTypeStatus.ENDED);
        if (voucher.getVoucherDetails() != null ) {
            for (VoucherDetail items: voucher.getVoucherDetails()){
                items.setStatus(VoucherStatus.REVOKED);
            }
        }
        voucherRepository.save(voucher);
    }


    @Transactional
    public VoucherResponse cloneVoucher(Long id) {
        Voucher voucher = findVoucherById(id);
        String baseName = voucher.getName() + "( Lần ";
        long count = voucherRepository.countByNameStartingWith(baseName);
        String cloneName = count == 0 ? voucher.getName() + "( Lần 2 )" : voucher.getName() + "( Lần "+ (count + 1)+ " )";

        Voucher cloneVoucher = Voucher.builder()
                .name(cloneName)
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .minOrderValue(voucher.getMinOrderValue())
                .quantity(0)
                .maxQuantity(voucher.getMaxQuantity())
                .validDays(voucher.getValidDays())
                .status(VoucherTypeStatus.DRAFT)
                .build();

        Voucher saveVoucher = voucherRepository.save(cloneVoucher);
        return new VoucherResponse(saveVoucher);
    }


    private Voucher findVoucherById(Long id){
        return voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found with id: " + id));
    }

    private Customer findCustomerById(Long id){
        return customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not fount with id: " + id));
    }

    private Order findOrderById(Long id){
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not fount with id: " + id));
    }

    private Incident findIncidentById(Long id) {
        return incidentRepository.findById(id).orElseThrow(() -> new RuntimeException("Incident not fount with id: "+ id));
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


    private int getStatusPriority(VoucherTypeStatus status){
        if (status == null) return 4;
        switch (status){
            case ACTIVE:
                return 1;
            case DRAFT:
                return 2;
            case ENDED:
            default:
                return 3;

        }
    }


    private String getRandomCode(){
        String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++){
            code.append(text.charAt(random.nextInt(text.length())));
        }

        return code.toString();

    }




}
