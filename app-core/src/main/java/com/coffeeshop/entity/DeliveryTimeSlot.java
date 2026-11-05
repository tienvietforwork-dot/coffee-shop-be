package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Khung giờ đặc biệt: trong [startTime, endTime) của các ngày đã chọn, phí ship tính theo option {@code plan} thay cho
 * option mặc định. endTime &lt; startTime = qua nửa đêm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "delivery_time_slots")
public class DeliveryTimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Convert(converter = HhmmConverter.class)
    @Column(name = "start_time", nullable = false, length = 5)
    private LocalTime startTime;
    @Convert(converter = HhmmConverter.class)
    @Column(name = "end_time", nullable = false, length = 5)
    private LocalTime endTime;
    /** ISO day numbers 1 (thứ 2) … 7 (chủ nhật), comma separated; empty = mọi ngày */
    @Column(length = 20)
    private String days;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private DeliveryFeePlan plan;

    public List<Integer> dayList() {
        if (days == null || days.isBlank()) return List.of();
        return Arrays.stream(days.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Integer::valueOf).toList();
    }

    /** does this slot cover {@code day} at {@code time}? */
    public boolean covers(DayOfWeek day, LocalTime time) {
        boolean overnight = endTime.isBefore(startTime);
        boolean inTime = overnight ? !time.isBefore(startTime) || time.isBefore(endTime)
                : !time.isBefore(startTime) && time.isBefore(endTime);
        if (!inTime) return false;
        List<Integer> d = dayList();
        // after midnight in an overnight slot, the slot belongs to the previous day
        DayOfWeek owner = overnight && time.isBefore(endTime) ? day.minus(1) : day;
        return d.isEmpty() || d.contains(owner.getValue());
    }
}
