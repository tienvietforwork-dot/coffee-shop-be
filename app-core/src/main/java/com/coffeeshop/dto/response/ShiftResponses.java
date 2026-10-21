package com.coffeeshop.dto.response;

import com.coffeeshop.entity.ShiftAssignment;
import com.coffeeshop.entity.WorkShift;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** Ca mẫu, phân ca + chấm công, bảng chấm công tháng. */
public final class ShiftResponses {

    private ShiftResponses() {
    }

    public record Shift(Long id, String name, LocalTime startTime, LocalTime endTime, int graceMinutes, String color,
                        boolean active, long minutes) {
        public static Shift from(WorkShift s) {
            return new Shift(s.getId(), s.getName(), s.getStartTime(), s.getEndTime(), s.getGraceMinutes(), s.getColor(),
                    s.isActive(), s.minutes());
        }
    }

    public record Assignment(Long id, Long staffId, String staffName, Long shiftId, String shiftName, String color,
                             LocalDate workDate, LocalTime startTime, LocalTime endTime, String position,
                             LocalDateTime checkInAt, LocalDateTime checkOutAt, String leaveType, String state,
                             long lateMinutes, long earlyLeaveMinutes, long workedMinutes, String note, String adjustedBy) {
        public static Assignment from(ShiftAssignment a) {
            var s = a.getWorkShift();
            return new Assignment(a.getId(), a.getStaff().getId(), a.getStaff().getFullName(), s.getId(), s.getName(),
                    s.getColor(), a.getWorkDate(), s.getStartTime(), s.getEndTime(), a.getPosition(), a.getCheckInAt(),
                    a.getCheckOutAt(), a.getLeaveType(), a.state(LocalDateTime.now()), a.lateMinutes(),
                    a.earlyLeaveMinutes(), a.workedMinutes(), a.getNote(), a.getAdjustedBy());
        }
    }

    /**
     * Bảng chấm công of one staff member for a month: {@code days} day-of-month → mark (giờ công "6.0", P, KL, V = vắng,
     * "?" = quên chấm ra) as on mẫu 01a-LĐTL.
     */
    public record TimesheetRow(Long staffId, String staffName, String position, int scheduled, int worked,
                               double workedHours, int lateTimes, long lateMinutes, int earlyTimes, int absent,
                               int leavePaid, int leaveUnpaid, int missingOut, Map<Integer, String> days) {
    }

    public record Timesheet(int year, int month, int daysInMonth, List<TimesheetRow> rows) {
    }
}
