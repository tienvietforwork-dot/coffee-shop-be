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
import com.coffeeshop.crm.repository.CustomerGroupMemberRepository;
import com.coffeeshop.crm.repository.CustomerRepository;
import com.coffeeshop.crm.repository.FeedbackRepository;
import com.coffeeshop.crm.repository.InteractionRepository;
import com.coffeeshop.crm.repository.FeedbackResponseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Locale;

/** Nghiệp vụ hồ sơ khách hàng: CRUD, tổng hợp Customer 360 và gộp hồ sơ trùng. */
@Service
@RequiredArgsConstructor
public class Customer360Service {
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final CustomerGroupMemberRepository groupMemberRepository;
    private final InteractionRepository interactionRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackResponseRepository feedbackResponseRepository;
    private final CustomerAnalyticsRepository analyticsRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<Customer> searchCustomers(String query, Pageable pageable) {
        String normalized = query == null || query.isBlank() ? null : query.trim();
        if (normalized == null) return customerRepository.findAllByDelFlagFalseOrderByIdDesc(pageable);
        return customerRepository.search(normalized, pageable);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> customerGroupNames(List<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) return Map.of();
        return groupMemberRepository.findGroupNamesByCustomerIds(customerIds).stream()
                .collect(Collectors.groupingBy(
                        CustomerGroupMemberRepository.CustomerGroupNameProjection::getCustomerId,
                        Collectors.mapping(CustomerGroupMemberRepository.CustomerGroupNameProjection::getGroupName,
                                Collectors.toList())));
    }

    @Transactional(readOnly = true)
    public Map<Long, CustomerAnalyticsRepository.CustomerListMetrics> customerListMetrics(List<Long> customerIds) {
        return analyticsRepository.customerListMetrics(customerIds);
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
        Map<String, Object> before = profileValues(customer);
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
        Customer saved = customerRepository.save(customer);
        Map<String, Object> after = profileValues(saved);
        if (!before.equals(after)) {
            try {
                String actor = SecurityContextHolder.getContext().getAuthentication() == null
                        ? "system" : SecurityContextHolder.getContext().getAuthentication().getName();
                jdbcTemplate.update("""
                        insert into customer_profile_history (customer_id, actor, before_values, after_values)
                        values (?, ?, cast(? as jsonb), cast(? as jsonb))
                        """, id, actor == null || actor.isBlank() ? "system" : actor,
                        objectMapper.writeValueAsString(before), objectMapper.writeValueAsString(after));
            } catch (Exception e) {
                throw new IllegalStateException("Không lưu được lịch sử thay đổi hồ sơ", e);
            }
        }
        return saved;
    }

    private Map<String, Object> profileValues(Customer c) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("fullName", c.getFullName()); values.put("phone", c.getPhone());
        values.put("email", c.getEmail()); values.put("dateOfBirth", c.getDateOfBirth());
        values.put("gender", c.getGender()); values.put("description", c.getDescription());
        return values;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> customerHistory(Long customerId) {
        findCustomer(customerId);
        return jdbcTemplate.query("""
                select id, actor, before_values::text as before_values, after_values::text as after_values, changed_at
                from customer_profile_history where customer_id = ? order by changed_at desc, id desc
                """, (rs, row) -> {
            Map<String, Object> history = new LinkedHashMap<>();
            history.put("id", rs.getLong("id")); history.put("actor", rs.getString("actor"));
            try {
                history.put("before", objectMapper.readValue(rs.getString("before_values"), Map.class));
                history.put("after", objectMapper.readValue(rs.getString("after_values"), Map.class));
            } catch (Exception e) { throw new IllegalStateException("Lịch sử hồ sơ không hợp lệ", e); }
            history.put("changedAt", rs.getTimestamp("changed_at").toLocalDateTime());
            return history;
        }, customerId);
    }

    @Transactional
    public Map<String, Object> importCustomers(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Chọn tệp CSV không rỗng");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("Tệp CSV không được vượt quá 5 MB");
        String raw;
        try { raw = new String(file.getBytes(), StandardCharsets.UTF_8); }
        catch (Exception e) { throw new IllegalArgumentException("Không đọc được tệp CSV UTF-8"); }
        if (raw.startsWith("\uFEFF")) raw = raw.substring(1);
        List<List<String>> rows = parseCsv(raw);
        if (rows.size() < 2) throw new IllegalArgumentException("CSV cần có dòng tiêu đề và ít nhất một dòng dữ liệu");
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < rows.get(0).size(); i++) columns.put(rows.get(0).get(i).trim().toLowerCase(Locale.ROOT), i);
        int phoneCol = column(columns, "phone", "sđt", "so dien thoai", "số điện thoại");
        int nameCol = column(columns, "fullname", "full_name", "ho ten", "họ tên");
        if (phoneCol < 0 || nameCol < 0) throw new IllegalArgumentException("CSV bắt buộc có cột fullName và phone (hoặc Họ tên, Số điện thoại)");
        int imported = 0; List<Map<String, Object>> errors = new java.util.ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (row.stream().allMatch(String::isBlank)) continue;
            try {
                String phone = cell(row, phoneCol).trim();
                String name = cell(row, nameCol).trim();
                if (!phone.matches("0\\d{9,10}")) throw new IllegalArgumentException("Số điện thoại phải gồm 10 hoặc 11 số, bắt đầu bằng 0");
                if (name.isBlank() || name.length() > 100) throw new IllegalArgumentException("Họ tên bắt buộc và tối đa 100 ký tự");
                if (customerRepository.existsByPhoneAndDelFlagFalse(phone)) throw new IllegalArgumentException("Số điện thoại đã tồn tại");
                Customer c = new Customer(); c.setFullName(name); c.setPhone(phone);
                String email = optionalCell(row, column(columns, "email"));
                if (email != null && (email.length() > 100 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")))
                    throw new IllegalArgumentException("Email sai định dạng hoặc dài quá 100 ký tự");
                c.setEmail(email);
                String dob = optionalCell(row, column(columns, "dateofbirth", "date_of_birth", "ngay sinh", "ngày sinh"));
                if (dob != null) {
                    LocalDate dateOfBirth = LocalDate.parse(dob);
                    if (!dateOfBirth.isBefore(LocalDate.now())) throw new IllegalArgumentException("Ngày sinh phải trước ngày hiện tại");
                    c.setDateOfBirth(dateOfBirth);
                }
                String gender = optionalCell(row, column(columns, "gender", "gioi tinh", "giới tính"));
                if (gender != null && !List.of("MALE", "FEMALE", "OTHER").contains(gender.toUpperCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Giới tính nhận MALE, FEMALE hoặc OTHER");
                c.setGender(gender == null ? null : gender.toUpperCase(Locale.ROOT));
                String description = optionalCell(row, column(columns, "description", "mo ta", "mô tả"));
                if (description != null && description.length() > 10000) throw new IllegalArgumentException("Mô tả tối đa 10.000 ký tự");
                c.setDescription(description);
                customerRepository.save(c); imported++;
            } catch (Exception e) {
                Map<String, Object> issue = new LinkedHashMap<>(); issue.put("row", rowIndex + 1);
                issue.put("message", e.getMessage() == null ? "Dữ liệu không hợp lệ" : e.getMessage()); errors.add(issue);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>(); result.put("imported", imported);
        result.put("failed", errors.size()); result.put("errors", errors); return result;
    }

    private int column(Map<String, Integer> columns, String... names) {
        for (String name : names) { Integer i = columns.get(name.toLowerCase(Locale.ROOT)); if (i != null) return i; }
        return -1;
    }
    private String cell(List<String> row, int index) { return index >= 0 && index < row.size() ? row.get(index) : ""; }
    private String optionalCell(List<String> row, int index) { String v = cell(row, index).trim(); return v.isEmpty() ? null : v; }
    private List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new java.util.ArrayList<>(); List<String> row = new java.util.ArrayList<>();
        StringBuilder value = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted && ch == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') { value.append('"'); i++; }
            else if (ch == '"') quoted = !quoted;
            else if (!quoted && ch == ',') { row.add(value.toString()); value.setLength(0); }
            else if (!quoted && (ch == '\n' || ch == '\r')) {
                row.add(value.toString()); value.setLength(0); rows.add(row); row = new java.util.ArrayList<>();
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
            } else value.append(ch);
        }
        if (quoted) throw new IllegalArgumentException("CSV có dấu ngoặc kép chưa đóng");
        if (value.length() > 0 || !row.isEmpty()) { row.add(value.toString()); rows.add(row); }
        return rows;
    }

    @Transactional(readOnly = true)
    public Customer360Response getCustomer360(Long customerId, LocalDate fromDate, LocalDate toDate) {
        Customer customer = findCustomer(customerId);
        List<String> customerGroups = customerGroupNames(List.of(customerId)).getOrDefault(customerId, List.of());
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
                        customer.getLoyaltyPoints(), customer.getRegisteredAt(), addresses, customerGroups),
                new Customer360Response.MetricsDto(metrics.totalSpend(), metrics.totalOrders(),
                        metrics.averageOrderValue(), metrics.ordersLast30Days(), metrics.lastOrderAt(),
                        analyticsRepository.favoriteCoffee(customerId, from, toExclusive),
                        analyticsRepository.favoriteCategory(customerId, from, toExclusive)),
                orderDtos, interactionDtos, feedbackDtos, timeline,
                analyticsRepository.pointHistory(customerId, from, toExclusive, 100).stream()
                        .map(p -> new Customer360Response.PointHistoryDto(p.id(), p.pointsChange(), p.reason(),
                                p.orderCode(), p.createdAt()))
                        .toList());
    }

    @Transactional
    public void mergeCustomers(Long primaryCustomerId, Long duplicateCustomerId) {
        if (primaryCustomerId.equals(duplicateCustomerId)) {
            throw new IllegalArgumentException("Hai hồ sơ cần gộp phải khác nhau");
        }
        Customer primary = findCustomer(primaryCustomerId);
        Customer duplicate = findCustomer(duplicateCustomerId);

        if (!isLikelyDuplicate(primary, duplicate)) {
            throw new CrmConflictException("Không thể gộp: cần họ tên trùng cùng với số điện thoại hoặc email trùng. Hãy giữ riêng nếu chưa xác nhận đây là cùng một khách.");
        }

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
        reassign("voucher_details", duplicateCustomerId, primaryCustomerId);
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

    private boolean isLikelyDuplicate(Customer first, Customer second) {
        String firstName = normalize(first.getFullName());
        String secondName = normalize(second.getFullName());
        if (firstName.isEmpty() || !firstName.equals(secondName)) return false;

        String firstPhone = normalize(first.getPhone());
        String secondPhone = normalize(second.getPhone());
        if (!firstPhone.isEmpty() && firstPhone.equals(secondPhone)) return true;

        String firstEmail = normalize(first.getEmail());
        String secondEmail = normalize(second.getEmail());
        return !firstEmail.isEmpty() && firstEmail.equals(secondEmail);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
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
