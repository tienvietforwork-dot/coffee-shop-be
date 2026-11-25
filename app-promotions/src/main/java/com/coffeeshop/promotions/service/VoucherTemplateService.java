package com.coffeeshop.promotions.service;

import com.coffeeshop.promotions.dto.request.VoucherTemplateRequest;
import com.coffeeshop.promotions.dto.response.VoucherDetailResponse;
import com.coffeeshop.promotions.dto.response.VoucherTemplateResponse;
import com.coffeeshop.promotions.entity.Customer;
import com.coffeeshop.promotions.entity.CustomerGroup;
import com.coffeeshop.promotions.entity.Order;
import com.coffeeshop.promotions.entity.Voucher;
import com.coffeeshop.promotions.entity.VoucherDetail;
import com.coffeeshop.promotions.entity.VoucherTemplates;
import com.coffeeshop.promotions.entity.enums.VoucherTemplateCustomerType;
import com.coffeeshop.promotions.entity.enums.VoucherTemplateOrderType;
import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import com.coffeeshop.promotions.entity.enums.VoucherTypeStatus;
import com.coffeeshop.promotions.entity.enums.VoucherTemplatesStatus;
import com.coffeeshop.promotions.repository.CustomerGroupMemberRepository;
import com.coffeeshop.promotions.repository.CustomerGroupRepository;
import com.coffeeshop.promotions.repository.CustomerRepository;
import com.coffeeshop.promotions.repository.OrderRepository;
import com.coffeeshop.promotions.repository.VoucherDetailRepository;
import com.coffeeshop.promotions.repository.VoucherRepository;
import com.coffeeshop.promotions.repository.VoucherTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherTemplateService {

    private final VoucherTemplateRepository voucherTemplateRepository;
    private final VoucherRepository voucherRepository;
    private final CustomerGroupRepository customerGroupRepository;
    private final CustomerGroupMemberRepository customerGroupMemberRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final VoucherDetailRepository voucherDetailRepository;

    @Transactional(readOnly = true)
    public List<VoucherTemplateResponse> getAll() {
        return voucherTemplateRepository.findAll().stream()
                .sorted(Comparator.comparing(v -> getStatusPriority(v.getStatus())))
                .map(VoucherTemplateResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public VoucherTemplateResponse createVoucherTemplate(VoucherTemplateRequest request) {
        Voucher voucher = findVoucherById(request.getVoucherId());

        CustomerGroup customerGroup = null;
        if (request.getCustomerType() == VoucherTemplateCustomerType.CUSTOMER_GROUP
                && request.getCustomerGroupId() != null) {
            customerGroup = findCustomerGroupById(request.getCustomerGroupId());
        }

        VoucherTemplates voucherTemplate = VoucherTemplates.builder()
                .name(request.getName())
                .voucher(voucher)
                .customerType(request.getCustomerType())
                .customerGroup(customerGroup)
                .orderType(request.getOrderType())
                .completedOrderCount(request.getCompletedOrderCount())
                .minOrderAmount(request.getMinOrderAmount())
                .requiredPoints(request.getRequiredPoints())
                .maxQuantity(request.getMaxQuantity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(VoucherTemplatesStatus.DRAFT)
                .build();

        VoucherTemplates saved = voucherTemplateRepository.save(voucherTemplate);
        return new VoucherTemplateResponse(saved);
    }

    @Transactional
    public VoucherTemplateResponse updateVoucherTemplate(Long id, VoucherTemplateRequest request) {
        Voucher voucher = findVoucherById(request.getVoucherId());

        CustomerGroup customerGroup = null;
        if (request.getCustomerType() == VoucherTemplateCustomerType.CUSTOMER_GROUP
                && request.getCustomerGroupId() != null) {
            customerGroup = findCustomerGroupById(request.getCustomerGroupId());
        }

        VoucherTemplates voucherTemplates = findVoucherTemplateById(id);
        voucherTemplates.setName(request.getName());
        voucherTemplates.setVoucher(voucher);
        voucherTemplates.setCustomerType(request.getCustomerType());
        voucherTemplates.setCustomerGroup(customerGroup);
        voucherTemplates.setOrderType(request.getOrderType());
        voucherTemplates.setCompletedOrderCount(request.getCompletedOrderCount());
        voucherTemplates.setMinOrderAmount(request.getMinOrderAmount());
        voucherTemplates.setRequiredPoints(request.getRequiredPoints());
        voucherTemplates.setMaxQuantity(request.getMaxQuantity());
        voucherTemplates.setStartDate(request.getStartDate());
        voucherTemplates.setEndDate(request.getEndDate());

        VoucherTemplates updated = voucherTemplateRepository.save(voucherTemplates);
        return new VoucherTemplateResponse(updated);
    }

    @Transactional
    public void activeVoucherTemplate(Long id) {
        VoucherTemplates template = findVoucherTemplateById(id);
        if (template.getStatus() != VoucherTemplatesStatus.DRAFT) {
            throw new RuntimeException("Chỉ có thể kích hoạt khi nhiệm vụ voucher đang ở trạng thái DRAFT");
        }

        template.setStatus(VoucherTemplatesStatus.ACTIVE);
        voucherTemplateRepository.save(template);
    }

    @Transactional
    public void endVoucherTemplate(Long id) {
        VoucherTemplates template = findVoucherTemplateById(id);
        if (template.getStatus() != VoucherTemplatesStatus.ACTIVE) {
            throw new RuntimeException("Chỉ có thể kết thúc khi nhiệm vụ voucher đang ở trạng thái ACTIVE");
        }

        template.setStatus(VoucherTemplatesStatus.ENDED);
        voucherTemplateRepository.save(template);
    }

    @Transactional
    public VoucherTemplateResponse cloneVoucherTemplate(Long id) {
        VoucherTemplates template = findVoucherTemplateById(id);

        VoucherTemplates cloned = VoucherTemplates.builder()
                .name(template.getName())
                .voucher(template.getVoucher())
                .customerType(template.getCustomerType())
                .customerGroup(template.getCustomerGroup())
                .orderType(template.getOrderType())
                .completedOrderCount(template.getCompletedOrderCount())
                .minOrderAmount(template.getMinOrderAmount())
                .requiredPoints(template.getRequiredPoints())
                .maxQuantity(template.getMaxQuantity())
                .startDate(template.getStartDate())
                .endDate(template.getEndDate())
                .status(VoucherTemplatesStatus.DRAFT)
                .build();

        VoucherTemplates saved = voucherTemplateRepository.save(cloned);
        return new VoucherTemplateResponse(saved);
    }

    @Transactional
    public void deleteVoucherTemplate(Long id){
        VoucherTemplates voucherTemplate = findVoucherTemplateById(id);
        voucherTemplateRepository.delete(voucherTemplate);
    }

    private int getStatusPriority(VoucherTemplatesStatus status) {
        if (status == null) return 4;
        return switch (status) {
            case ACTIVE -> 1;
            case DRAFT -> 2;
            case ENDED -> 3;
        };
    }

    @Transactional(readOnly = true)
    public List<VoucherTemplateResponse> getAvailableVoucherTemplatesForCustomer() {
        String username = getCurrentUsername();
        Set<Long> joinedGroupIds = customerGroupMemberRepository.findGroupIdsByUsername(username);
        Long customerId = customerRepository.findCustomerIdByUsername(username).orElse(null);

        return voucherTemplateRepository.findAll().stream()
                .filter(template -> isActive(template) && isVisibleToCustomer(template, joinedGroupIds))
                .map(template -> {
                    VoucherTemplateResponse response = new VoucherTemplateResponse(template);
                    if (customerId != null && template.getVoucher() != null) {
                        response.setRedeemedQuantity(voucherDetailRepository
                                .countByCustomerIdAndVoucher_Id(customerId, template.getVoucher().getId()));
                    } else {
                        response.setRedeemedQuantity(0L);
                    }
                    return response;
                })
                .toList();
    }

    private boolean isActive(VoucherTemplates template) {
        return template != null && template.getStatus() == VoucherTemplatesStatus.ACTIVE;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username is missing from token");
        }
        return username;
    }

    private boolean isVisibleToCustomer(VoucherTemplates template, Set<Long> joinedGroupIds) {
        if (template.getCustomerType() == null) {
            return false;
        }

        return switch (template.getCustomerType()) {
            case ALL -> true;
            case CUSTOMER_GROUP -> {
                CustomerGroup targetGroup = template.getCustomerGroup();
                yield targetGroup != null && joinedGroupIds.contains(targetGroup.getId());
            }
        };
    }

    @Transactional
    public VoucherDetailResponse redeemVoucherTemplate(Long templateId) {
        String username = getCurrentUsername();
        Long customerId = customerRepository.findCustomerIdByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy khách hàng liên kết với tài khoản"));
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        VoucherTemplates template = voucherTemplateRepository.findByIdForUpdate(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhiệm vụ voucher"));
        if (template.getVoucher() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nhiệm vụ voucher chưa được gắn với voucher");
        }
        Voucher voucher = voucherRepository.findByIdForUpdate(template.getVoucher().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher"));

        if (voucher.getQuantity() != null && voucher.getMaxQuantity() != null
                && voucher.getQuantity() >= voucher.getMaxQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher đã hết");
        }

        long customerIssuedCount = voucherDetailRepository.countByCustomerIdAndVoucher_Id(customerId, voucher.getId());
        if (template.getMaxQuantity() != null && template.getMaxQuantity() > 0
                && customerIssuedCount >= template.getMaxQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã đạt giới hạn số lần đổi voucher này");
        }

        validateOrderCondition(customerId, template);

        long requiredPoints = template.getRequiredPoints() != null ? template.getRequiredPoints() : 0;
        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        if (currentPoints < requiredPoints) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không đủ điểm thưởng để đổi voucher");
        }

        if (requiredPoints > 0) {
            customer.setLoyaltyPoints(Math.toIntExact(currentPoints - requiredPoints));
        }

        LocalDateTime now = LocalDateTime.now();

        VoucherDetail voucherDetail = VoucherDetail.builder()
                .voucher(voucher)
                .customer(customer)
                .code(getRandomCode())
                .status(VoucherStatus.ISSUED)
                .issuedAt(now)
                .expiresAt(now.plusDays(voucher.getValidDays() == null ? 30 : voucher.getValidDays()))
                .build();

        VoucherDetail savedDetail = voucherDetailRepository.save(voucherDetail);
        voucher.setQuantity((voucher.getQuantity() == null ? 0 : voucher.getQuantity()) + 1);
        return new VoucherDetailResponse(savedDetail);
    }

    private void validateOrderCondition(Long customerId, VoucherTemplates template) {
        if (template.getOrderType() == null || template.getOrderType() == VoucherTemplateOrderType.ALL) {
            return;
        }

        List<Order> completedOrders = orderRepository.findCompletedOrdersSince(customerId, template.getStartDate());
        long requiredCount = template.getCompletedOrderCount() != null ? template.getCompletedOrderCount() : 0;
        java.math.BigDecimal minAmount = template.getMinOrderAmount() != null
                ? template.getMinOrderAmount() : java.math.BigDecimal.ZERO;

        switch (template.getOrderType()) {
            case COUNT -> {
                if (completedOrders.size() < requiredCount) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Chưa đạt số đơn hoàn thành (" + completedOrders.size() + "/" + requiredCount + ")"
                    );
                }
            }
            case AMOUNT -> {
                java.math.BigDecimal totalAmount = completedOrders.stream()
                        .map(Order::getTotalAmount)
                        .filter(Objects::nonNull)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                if (totalAmount.compareTo(minAmount) < 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Chưa đạt tổng giá trị đơn hàng tối thiểu"
                    );
                }
            }
            case COUNT_AMOUNT -> {
                long qualifyingCount = completedOrders.stream()
                        .filter(order -> order.getTotalAmount() != null
                                && order.getTotalAmount().compareTo(minAmount) >= 0)
                        .count();

                if (qualifyingCount < requiredCount) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Chưa đạt số đơn đạt giá trị tối thiểu (" + qualifyingCount + "/" + requiredCount + ")"
                    );
                }
            }
            case ALL -> {
            }
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


    private Voucher findVoucherById(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher not found with id: " + id));
    }

    private CustomerGroup findCustomerGroupById(Long id){
        return customerGroupRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer Group not found with id: " + id));
    }

    private VoucherTemplates findVoucherTemplateById(Long id){
        return voucherTemplateRepository.findById(id).orElseThrow(() -> new RuntimeException("Voucher Templates not found with id: " + id));
    }
}