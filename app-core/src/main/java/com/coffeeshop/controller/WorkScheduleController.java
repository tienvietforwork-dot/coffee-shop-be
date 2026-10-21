package com.coffeeshop.controller;

import com.coffeeshop.dto.response.ShiftResponses;
import com.coffeeshop.export.ExcelSheet;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.WorkScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Ca mẫu, phân ca, chấm công (tự chấm / quản lý sửa) và bảng chấm công tháng (+ Excel). */
@RestController
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleService service;

    public record ShiftRequest(@NotBlank @Size(max = 50) String name, @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                               @Min(0) @Max(60) Integer graceMinutes, @Size(max = 20) String color, Boolean active) {
    }

    public record AssignRequest(@NotEmpty List<Long> staffIds, @NotEmpty List<LocalDate> dates, @NotNull Long shiftId,
                                @Size(max = 30) String position) {
    }

    public record AdjustRequest(LocalDateTime checkInAt, LocalDateTime checkOutAt, String leaveType,
                                @NotBlank @Size(max = 255) String note) {
    }

    @GetMapping("/api/work-shifts")
    @PreAuthorize(Perm.CAN_READ_WORK_SHIFTS)
    public List<ShiftResponses.Shift> shifts() {
        return service.shifts();
    }

    @PostMapping("/api/work-shifts")
    @PreAuthorize(Perm.CAN_WORK_SHIFTS)
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponses.Shift createShift(@Valid @RequestBody ShiftRequest r) {
        return service.saveShift(null, r.name(), r.startTime(), r.endTime(), r.graceMinutes(), r.color(), r.active());
    }

    @PutMapping("/api/work-shifts/{id}")
    @PreAuthorize(Perm.CAN_WORK_SHIFTS)
    public ShiftResponses.Shift updateShift(@PathVariable Long id, @Valid @RequestBody ShiftRequest r) {
        return service.saveShift(id, r.name(), r.startTime(), r.endTime(), r.graceMinutes(), r.color(), r.active());
    }

    @DeleteMapping("/api/work-shifts/{id}")
    @PreAuthorize(Perm.CAN_WORK_SHIFTS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShift(@PathVariable Long id) {
        service.deleteShift(id);
    }

    @GetMapping("/api/shift-assignments")
    @PreAuthorize(Perm.CAN_READ_WORK_SHIFTS)
    public List<ShiftResponses.Assignment> schedule(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                    @RequestParam(required = false) Long staffId) {
        return service.schedule(from, to, staffId);
    }

    @PostMapping("/api/shift-assignments")
    @PreAuthorize(Perm.CAN_WORK_SHIFTS)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ShiftResponses.Assignment> assign(@Valid @RequestBody AssignRequest r) {
        return service.assign(r.staffIds(), r.dates(), r.shiftId(), r.position());
    }

    @DeleteMapping("/api/shift-assignments/{id}")
    @PreAuthorize(Perm.CAN_WORK_SHIFTS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable Long id) {
        service.unassign(id);
    }

    /** Quản lý sửa giờ chấm công / ghi nghỉ (P, KL); a reason is required. */
    @PutMapping("/api/shift-assignments/{id}/attendance")
    @PreAuthorize(Perm.CAN_WORK_SHIFTS)
    public ShiftResponses.Assignment adjust(@PathVariable Long id, @Valid @RequestBody AdjustRequest r) {
        return service.adjust(id, r.checkInAt(), r.checkOutAt(), r.leaveType(), r.note());
    }

    // ---- self service: any staff account --------------------------------

    @GetMapping("/api/attendance/today")
    @PreAuthorize("isAuthenticated()")
    public List<ShiftResponses.Assignment> myToday() {
        return service.myToday();
    }

    @PostMapping("/api/attendance/{assignmentId}/check-in")
    @PreAuthorize("isAuthenticated()")
    public ShiftResponses.Assignment checkIn(@PathVariable Long assignmentId) {
        return service.checkIn(assignmentId);
    }

    @PostMapping("/api/attendance/{assignmentId}/check-out")
    @PreAuthorize("isAuthenticated()")
    public ShiftResponses.Assignment checkOut(@PathVariable Long assignmentId) {
        return service.checkOut(assignmentId);
    }

    // ---- bảng chấm công ----------------------------------------------------

    @GetMapping("/api/timesheets")
    @PreAuthorize(Perm.CAN_WORK_SHIFTS)
    public ShiftResponses.Timesheet timesheet(@RequestParam String month) {
        return service.timesheet(YearMonth.parse(month));
    }

    /** Bảng chấm công (mẫu 01a-LĐTL rút gọn) as .xlsx */
    @GetMapping("/api/timesheets.xlsx")
    @PreAuthorize(Perm.CAN_WORK_SHIFTS)
    public ResponseEntity<byte[]> timesheetExcel(@RequestParam String month) {
        ShiftResponses.Timesheet t = service.timesheet(YearMonth.parse(month));
        List<String> header = new ArrayList<>(List.of("Họ và tên", "Chức vụ"));
        for (int d = 1; d <= t.daysInMonth(); d++) header.add(String.valueOf(d));
        header.addAll(List.of("Số ca", "Giờ công", "Đi muộn (lần)", "Về sớm (lần)", "Vắng", "Nghỉ P", "Nghỉ KL"));
        ExcelSheet x = new ExcelSheet("Cham cong")
                .title("BẢNG CHẤM CÔNG THÁNG " + t.month() + "/" + t.year())
                .line("Ký hiệu: số = giờ công; P = nghỉ có phép; KL = nghỉ không lương; V = vắng; ? = quên chấm ra")
                .header(header.toArray(String[]::new));
        for (var r : t.rows()) {
            List<Object> row = new ArrayList<>(List.of(r.staffName(), r.position() == null ? "" : r.position()));
            for (int d = 1; d <= t.daysInMonth(); d++) row.add(r.days().getOrDefault(d, ""));
            row.addAll(List.of(r.worked(), r.workedHours(), r.lateTimes(), r.earlyTimes(), r.absent(), r.leavePaid(), r.leaveUnpaid()));
            x.row(row.toArray());
        }
        return x.download("bang-cham-cong-" + month);
    }
}
