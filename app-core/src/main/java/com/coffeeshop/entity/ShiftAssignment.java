package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Phân ca: a staff member on a shift on a day, at a position (pha chế, thu ngân, phục vụ), and its chấm công —
 * giờ vào / ra thực tế. {@code leaveType}: P = nghỉ có phép, KL = nghỉ không lương (set by the manager).
 * Per Bộ luật Lao động (Điều 127) lateness is only recorded, never fined: pay follows the hours actually worked.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "shift_assignments")
@SQLRestriction("del_flag = false")
@SQLDelete(sql = "UPDATE shift_assignments SET del_flag = true WHERE id = ?")
public class ShiftAssignment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_shift_id", nullable = false)
    private WorkShift workShift;
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;
    @Column(length = 30)
    private String position;
    @Column(name = "check_in_at")
    private LocalDateTime checkInAt;
    @Column(name = "check_out_at")
    private LocalDateTime checkOutAt;
    @Column(name = "leave_type", length = 5)
    private String leaveType;
    /** ghi chú / lý do sửa giờ chấm công */
    @Column(length = 255)
    private String note;
    /** quản lý đã sửa giờ chấm công */
    @Column(name = "adjusted_by", length = 50)
    private String adjustedBy;

    public LocalDateTime plannedStart() {
        return workDate.atTime(workShift.getStartTime());
    }

    public LocalDateTime plannedEnd() {
        LocalDateTime end = workDate.atTime(workShift.getEndTime());
        return end.isAfter(plannedStart()) ? end : end.plusDays(1);
    }

    public long lateMinutes() {
        if (checkInAt == null) return 0;
        long m = Duration.between(plannedStart(), checkInAt).toMinutes();
        return m > workShift.getGraceMinutes() ? m : 0;
    }

    public long earlyLeaveMinutes() {
        if (checkOutAt == null) return 0;
        long m = Duration.between(checkOutAt, plannedEnd()).toMinutes();
        return m > workShift.getGraceMinutes() ? m : 0;
    }

    /** giờ công thực tế, within the shift (coming early or staying late is not counted without a manager edit) */
    public long workedMinutes() {
        if (checkInAt == null || checkOutAt == null) return 0;
        LocalDateTime from = checkInAt.isBefore(plannedStart()) ? plannedStart() : checkInAt;
        LocalDateTime to = checkOutAt.isAfter(plannedEnd()) ? plannedEnd() : checkOutAt;
        return Math.max(0, Duration.between(from, to).toMinutes());
    }

    /** SCHEDULED, WORKING, DONE, MISSING_OUT (quên chấm ra), ABSENT (vắng), LEAVE (nghỉ phép / không lương) */
    public String state(LocalDateTime now) {
        if (leaveType != null) return "LEAVE";
        if (checkInAt == null) return now.isAfter(plannedEnd()) ? "ABSENT" : "SCHEDULED";
        if (checkOutAt == null) return now.isAfter(plannedEnd().plusHours(2)) ? "MISSING_OUT" : "WORKING";
        return "DONE";
    }
}
