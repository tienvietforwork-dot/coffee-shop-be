package com.coffeeshop.crm.service;

import com.coffeeshop.crm.dto.request.CustomerRequest;
import com.coffeeshop.crm.dto.response.Customer360Response;
import com.coffeeshop.crm.entity.Customer;
import com.coffeeshop.crm.entity.CustomerAddress;
import com.coffeeshop.crm.entity.Feedback;
import com.coffeeshop.crm.entity.Interaction;
import com.coffeeshop.crm.entity.FeedbackResponse;
import com.coffeeshop.crm.exception.CrmConflictException;
import com.coffeeshop.crm.exception.CrmNotFoundException;
import com.coffeeshop.crm.repository.CustomerAddressRepository;
import com.coffeeshop.crm.repository.CustomerAnalyticsRepository;
import com.coffeeshop.crm.repository.CustomerRepository;
import com.coffeeshop.crm.repository.FeedbackRepository;
import com.coffeeshop.crm.repository.InteractionRepository;
import com.coffeeshop.crm.repository.FeedbackResponseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Customer360Service {
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final InteractionRepository interactionRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackResponseRepository feedbackResponseRepository;
    private final CustomerAnalyticsRepository analyticsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<Customer> searchCustomers(String query, Pageable pageable) {
        String normalized = query == null || query.isBlank() ? null : query.trim();
        if (normalized == null) return customerRepository.findAllByDelFlagFalseOrderByIdDesc(pageable);
        return customerRepository.search(normalized, pageable);
    }

    @Transactional
    public Customer createCustomer(CustomerRequest request) {
        if (customerRepository.existsByPhoneAndDelFlagFalse(request.getPhone())) {
            throw new CrmConflictException("Số điện thoại đã thuộc một hồ sơ khách hàng khác");
        }
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone().trim());
        customer.setEmail(request.getEmail());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setGender(request.getGender());
        customer.setDescription(request.getDescription());
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        String phone = request.getPhone().trim();
        if (!phone.equals(customer.getPhone()) && customerRepository.existsByPhoneAndDelFlagFalse(phone)) {
            throw new CrmConflictException("Số điện thoại đã thuộc một hồ sơ khách hàng khác");
        }
        customer.setFullName(request.getFullName());
        customer.setPhone(phone);
        customer.setEmail(request.getEmail());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setGender(request.getGender());
        customer.setDescription(request.getDescription());
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer360Response getCustomer360(Long customerId, LocalDate fromDate, LocalDate toDate) {
        Customer customer = findCustomer(customerId);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate phải nhỏ hơn hoặc bằng toDate");
        }
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
        var metrics = analyticsRepository.metrics(customerId, from, toExclusive);
        var orders = analyticsRepository.recentOrders(customerId, from, toExclusive, 20);
        Pageable recentPage = PageRequest.of(0, 20);
        var interactions = from == null && toExclusive == null
                ? interactionRepository.findAllByCustomerIdAndDelFlagFalseOrderByOccurredAtDesc(customerId, recentPage)
                : from == null
                    ? interactionRepository.findForCustomerBefore(customerId, toExclusive, recentPage)
                    : toExclusive == null
                        ? interactionRepository.findForCustomerSince(customerId, from, recentPage)
                        : interactionRepository.findForCustomerBetween(customerId, from, toExclusive, recentPage);
        var feedbacks = from == null && toExclusive == null
                ? feedbackRepository.findAllByCustomerIdAndDelFlagFalseOrderByCreatedAtDesc(customerId, recentPage)
                : from == null
                    ? feedbackRepository.findForCustomerBefore(customerId, toExclusive, recentPage)
                    : toExclusive == null
                        ? feedbackRepository.findForCustomerSince(customerId, from, recentPage)
                        : feedbackRepository.findForCustomerBetween(customerId, from, toExclusive, recentPage);
        List<Customer360Response.AddressDto> addresses = addressRepository
                .findAllByCustomerIdAndDelFlagFalseOrderByIdDesc(customerId).stream()
                .map(a -> new Customer360Response.AddressDto(a.getId(), a.getLabel(), a.getRecipientName(),
                        a.getRecipientPhone(), a.getAddress(), Boolean.TRUE.equals(a.getIsDefault())))
                .toList();

        List<Customer360Response.OrderDto> orderDtos = orders.stream()
                .map(o -> new Customer360Response.OrderDto(o.orderCode(), o.status(), o.totalAmount(), o.orderedAt(), o.items()))
                .toList();
        List<Customer360Response.InteractionDto> interactionDtos = interactions.getContent().stream()
                .map(this::toInteractionDto).toList();
        List<Customer360Response.FeedbackDto> feedbackDtos = feedbacks.getContent().stream()
                .map(this::toFeedbackDto).toList();

        List<Customer360Response.TimelineEventDto> timeline = new ArrayList<>();
        if ((from == null || !customer.getRegisteredAt().isBefore(from)) && (toExclusive == null || customer.getRegisteredAt().isBefore(toExclusive))) {
            timeline.add(new Customer360Response.TimelineEventDto("CUSTOMER_REGISTERED", "Khách hàng đăng ký hồ sơ", customer.getRegisteredAt()));
        }
        orderDtos.forEach(o -> timeline.add(new Customer360Response.TimelineEventDto("ORDER",
                "Đơn " + o.orderCode() + " - " + o.status() + " - " + o.totalAmount(), o.orderedAt())));
        interactionDtos.forEach(i -> timeline.add(new Customer360Response.TimelineEventDto("INTERACTION",
                i.channel() + ": " + (i.content() == null ? "" : i.content()) +
                        (i.outcome() == null ? "" : " — Kết quả: " + i.outcome()), i.occurredAt())));
        feedbackDtos.forEach(f -> timeline.add(new Customer360Response.TimelineEventDto("FEEDBACK",
                f.category() + ": " + f.status(), f.createdAt())));
        feedbackDtos.forEach(f -> f.responses().forEach(reply -> timeline.add(new Customer360Response.TimelineEventDto(
                "FEEDBACK_RESPONSE", "Phản hồi khách: " + reply.message(), reply.sentAt()))));
        analyticsRepository.recentVouchers(customerId, from, toExclusive, 20).forEach(v -> timeline.add(
                new Customer360Response.TimelineEventDto("VOUCHER", v.code() + ": " + v.status(), v.issuedAt())));
        analyticsRepository.recentPromotionUses(customerId, from, toExclusive, 20).forEach(p -> timeline.add(
                new Customer360Response.TimelineEventDto("PROMOTION_USED",
                        p.promotionName() + " — đơn " + p.orderCode() + " (" + p.orderStatus() + ")", p.orderedAt())));
        timeline.sort(Comparator.comparing(Customer360Response.TimelineEventDto::eventTime,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return new Customer360Response(
                new Customer360Response.ProfileDto(customer.getId(), customer.getFullName(), customer.getPhone(),
                        customer.getEmail(), customer.getDateOfBirth(), customer.getGender(), customer.getDescription(),
                        customer.getLoyaltyPoints(), customer.getRegisteredAt(), addresses),
                new Customer360Response.MetricsDto(metrics.totalSpend(), metrics.totalOrders(),
                        metrics.averageOrderValue(), metrics.ordersLast30Days(), metrics.lastOrderAt(),
                        analyticsRepository.favoriteCoffee(customerId, from, toExclusive),
                        analyticsRepository.favoriteCategory(customerId, from, toExclusive)),
                orderDtos, interactionDtos, feedbackDtos, timeline);
    }

    @Transactional
    public void mergeCustomers(Long primaryCustomerId, Long duplicateCustomerId) {
        if (primaryCustomerId.equals(duplicateCustomerId)) {
            throw new IllegalArgumentException("Hai hồ sơ cần gộp phải khác nhau");
        }
        Customer primary = findCustomer(primaryCustomerId);
        Customer duplicate = findCustomer(duplicateCustomerId);

        long primaryAccounts = count("select count(*) from users where customer_id = :id and del_flag = false", primaryCustomerId);
        long duplicateAccounts = count("select count(*) from users where customer_id = :id and del_flag = false", duplicateCustomerId);
        if (primaryAccounts > 0 && duplicateAccounts > 0) {
            throw new CrmConflictException("Cả hai hồ sơ đều có tài khoản đăng nhập; cần xử lý liên kết tài khoản trước khi gộp");
        }

        boolean primaryHasDefaultAddress = addressRepository.findDefaultByCustomerId(primaryCustomerId).isPresent();
        if (primaryHasDefaultAddress) {
            entityManager.createNativeQuery("update customer_addresses set is_default = false where customer_id = :id and is_default = true and del_flag = false")
                    .setParameter("id", duplicateCustomerId).executeUpdate();
        }

        entityManager.createNativeQuery("""
                update customer_group_members duplicate_member set del_flag = true, updated_at = current_timestamp
                where duplicate_member.customer_id = :duplicateId and duplicate_member.del_flag = false
                  and exists (select 1 from customer_group_members primary_member
                              where primary_member.group_id = duplicate_member.group_id
                                and primary_member.customer_id = :primaryId and primary_member.del_flag = false)
                """).setParameter("duplicateId", duplicateCustomerId).setParameter("primaryId", primaryCustomerId).executeUpdate();

        reassign("customer_group_members", duplicateCustomerId, primaryCustomerId);
        reassign("customer_addresses", duplicateCustomerId, primaryCustomerId);
        reassign("interactions", duplicateCustomerId, primaryCustomerId);
        reassign("feedbacks", duplicateCustomerId, primaryCustomerId);
        reassign("orders", duplicateCustomerId, primaryCustomerId);
        reassign("carts", duplicateCustomerId, primaryCustomerId);
        reassign("loyalty_point_history", duplicateCustomerId, primaryCustomerId);
        reassign("vouchers", duplicateCustomerId, primaryCustomerId);
        if (primaryAccounts == 0 && duplicateAccounts > 0) reassign("users", duplicateCustomerId, primaryCustomerId);

        primary.setLoyaltyPoints((primary.getLoyaltyPoints() == null ? 0 : primary.getLoyaltyPoints()) +
                (duplicate.getLoyaltyPoints() == null ? 0 : duplicate.getLoyaltyPoints()));
        duplicate.setDelFlag(true);
        duplicate.setDelUser(null);
        customerRepository.save(primary);
        customerRepository.save(duplicate);
    }

    private long count(String sql, Long customerId) {
        return ((Number) entityManager.createNativeQuery(sql).setParameter("id", customerId).getSingleResult()).longValue();
    }

    private void reassign(String table, Long sourceId, Long targetId) {
        // Table names are constants owned by this service; values are always bound parameters.
        entityManager.createNativeQuery("update " + table + " set customer_id = :targetId where customer_id = :sourceId and del_flag = false")
                .setParameter("sourceId", sourceId).setParameter("targetId", targetId).executeUpdate();
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new CrmNotFoundException("Không tìm thấy khách hàng ID " + id));
    }

    private Customer360Response.InteractionDto toInteractionDto(Interaction i) {
        return new Customer360Response.InteractionDto(i.getId(), i.getChannel(), i.getStatus(), i.getContent(),
                i.getNote(), i.getRemindAt(), i.getStaffId(), i.getOccurredAt(), i.getOutcome(), i.getCreatedAt());
    }

    private Customer360Response.FeedbackDto toFeedbackDto(Feedback f) {
        return new Customer360Response.FeedbackDto(f.getId(), f.getTopic(), f.getIssueType(), f.getCategory(), f.getSentiment(),
                f.getContent(), f.getStatus(), Boolean.TRUE.equals(f.getEscalated()), f.getResolution(),
                f.getSatisfaction(), f.getRating(), f.getStaffId(), f.getTopics().stream().sorted().toList(),
                feedbackResponseRepository.findAllByFeedbackIdAndDelFlagFalseOrderBySentAtAsc(f.getId()).stream()
                        .map(reply -> new Customer360Response.FeedbackReplyDto(reply.getId(), reply.getMessage(),
                                reply.getStaffId(), reply.getSentAt(), reply.getReadAt()))
                        .toList(), f.getCreatedAt());
    }
}
