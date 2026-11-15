package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.SubscriptionFrequency;
import com.coffeeshop.entity.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Gói giao cà phê định kỳ: các món, địa chỉ giao, lịch (mọi ngày / các thứ trong tuần) và giờ giao cố định. Trước giờ
 * giao, hệ thống tự tạo một đơn giao (khách trả khi nhận) — xem SubscriptionService.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "delivery_subscriptions")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE delivery_subscriptions SET del_flag = true WHERE id = ?")
public class DeliverySubscription extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private CustomerAddress address;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SubscriptionFrequency frequency;
    /** WEEKLY: 1 (thứ 2) … 7 (CN), phân cách dấu phẩy */
    @Column(length = 20)
    private String days;
    /** giờ giao cố định (giờ Việt Nam) */
    @Convert(converter = HhmmConverter.class)
    @Column(name = "delivery_time", nullable = false, length = 5)
    private LocalTime deliveryTime;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    /** null = không hạn */
    @Column(name = "end_date")
    private LocalDate endDate;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;
    @Column(columnDefinition = "TEXT")
    private String note;
    /** ngày giao gần nhất đã tạo đơn (hoặc đã thử tạo) — mỗi ngày tối đa 1 đơn */
    @Column(name = "last_run_date")
    private LocalDate lastRunDate;
    /** lỗi lần tạo đơn gần nhất (hết món, ngoài khu vực…); null = ổn */
    @Column(name = "last_error", length = 255)
    private String lastError;
    @Builder.Default
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliverySubscriptionItem> items = new ArrayList<>();

    public List<Integer> dayList() {
        if (days == null || days.isBlank()) return List.of();
        return Arrays.stream(days.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Integer::valueOf).toList();
    }

    /** có giao vào ngày {@code date} không (theo lịch, ngày bắt đầu / kết thúc; không xét trạng thái) */
    public boolean runsOn(LocalDate date) {
        if (date.isBefore(startDate) || (endDate != null && date.isAfter(endDate))) return false;
        if (frequency == SubscriptionFrequency.DAILY) return true;
        return dayList().contains(date.getDayOfWeek().getValue());
    }

    /** ngày giao kế tiếp từ {@code from} (kể cả {@code from}); null = hết hạn / không có ngày nào */
    public LocalDate nextDate(LocalDate from) {
        LocalDate d = from.isBefore(startDate) ? startDate : from;
        for (int i = 0; i < 8; i++, d = d.plusDays(1)) {
            if (endDate != null && d.isAfter(endDate)) return null;
            if (runsOn(d)) return d;
        }
        return null;
    }

    public static String dayText(List<Integer> days) {
        return String.join(", ", days.stream().map(d -> d == DayOfWeek.SUNDAY.getValue() ? "CN" : "T" + (d + 1)).toList());
    }
}
