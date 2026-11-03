package com.coffeeshop.service;

import com.coffeeshop.dto.response.ShiftResponses;
import com.coffeeshop.entity.ShiftAssignment;
import com.coffeeshop.entity.Staff;
import com.coffeeshop.entity.WorkShift;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.ShiftAssignmentRepository;
import com.coffeeshop.repository.StaffRepository;
import com.coffeeshop.repository.WorkShiftRepository;
import com.coffeeshop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Phân ca & chấm công: ca mẫu → xếp ca theo tuần (nhiều nhân viên × nhiều ngày; ca gãy = 2 ca trong ngày) →
 * nhân viên tự chấm vào / ra đúng ca của mình trong ngày → quản lý sửa giờ (ghi lý do), ghi nghỉ phép →
 * bảng chấm công tháng (mẫu 01a-LĐTL rút gọn).
 */
@Service
@RequiredArgsConstructor
public class WorkScheduleService {

    /** chấm vào được sớm nhất trước giờ vào ca */
    private static final int EARLY_CHECK_IN_MINUTES = 60;

    private final WorkShiftRepository shiftRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final StaffRepository staffRepository;

    // ---- ca mẫu ----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ShiftResponses.Shift> shifts() {
        return shiftRepository.findAllByOrderByStartTimeAsc().stream().map(ShiftResponses.Shift::from).toList();
    }

    @Transactional
    public ShiftResponses.Shift saveShift(Long id, String name, LocalTime start, LocalTime end, Integer grace, String color, Boolean active) {
        if (start.equals(end)) throw new BadRequestException("Giờ vào và giờ ra trùng nhau");
        WorkShift s = id == null ? new WorkShift() : shift(id);
        s.setName(name.trim());
        s.setStartTime(start);
        s.setEndTime(end);
        s.setGraceMinutes(grace == null ? 5 : grace);
        s.setColor(color);
        s.setActive(active == null || active);
        return ShiftResponses.Shift.from(shiftRepository.save(s));
    }

    /** a shift already used from today on is deactivated, not deleted */
    @Transactional
    public void deleteShift(Long id) {
        WorkShift s = shift(id);
        if (assignmentRepository.existsByWorkShiftIdAndWorkDateGreaterThanEqual(id, LocalDate.now())) {
            throw new BadRequestException("Ca đang được xếp từ hôm nay trở đi — gỡ lịch trước hoặc tạm ngưng ca");
        }
        s.softDelete();
    }

    // ---- xếp ca ----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ShiftResponses.Assignment> schedule(LocalDate from, LocalDate to, Long staffId) {
        return assignmentRepository.findBetween(from, to, staffId).stream().map(ShiftResponses.Assignment::from).toList();
    }

    /** Xếp ca: every staff member × every date on the shift; existing ones are skipped. Returns what was added. */
    @Transactional
    public List<ShiftResponses.Assignment> assign(List<Long> staffIds, List<LocalDate> dates, Long shiftId, String position) {
        WorkShift shift = shift(shiftId);
        if (!shift.isActive()) throw new BadRequestException("Ca '" + shift.getName() + "' đang tạm ngưng");
        List<ShiftResponses.Assignment> added = new ArrayList<>();
        for (Long staffId : staffIds) {
            Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên: " + staffId));
            for (LocalDate d : dates) {
                if (d.isBefore(LocalDate.now())) throw new BadRequestException("Không xếp ca cho ngày đã qua: " + d);
                if (assignmentRepository.existsByStaffIdAndWorkDateAndWorkShiftId(staffId, d, shiftId)) continue;
                // the same person cannot be on two overlapping shifts the same day
                for (var other : assignmentRepository.findBetween(d, d, staffId)) {
                    var o = other.getWorkShift();
                    if (shift.getStartTime().isBefore(o.getEndTime()) && o.getStartTime().isBefore(shift.getEndTime())) {
                        throw new BadRequestException(staff.getFullName() + " đã có " + o.getName() + " ngày " + d + " trùng giờ");
                    }
                }
                added.add(ShiftResponses.Assignment.from(assignmentRepository.save(ShiftAssignment.builder().staff(staff)
                        .workShift(shift).workDate(d).position(position == null || position.isBlank() ? null : position.trim()).build())));
            }
        }
        return added;
    }

    /** Gỡ ca: only before the staff member checked in */
    @Transactional
    public void unassign(Long id) {
        ShiftAssignment a = assignment(id);
        if (a.getCheckInAt() != null) throw new BadRequestException("Đã chấm công, không gỡ ca được — sửa chấm công thay vì gỡ");
        a.softDelete();
    }

    /** Quản lý sửa chấm công / ghi nghỉ phép; the reason is required. */
    @Transactional
    public ShiftResponses.Assignment adjust(Long id, LocalDateTime checkIn, LocalDateTime checkOut, String leaveType, String note) {
        ShiftAssignment a = assignment(id);
        if (note == null || note.isBlank()) throw new BadRequestException("Ghi lý do sửa chấm công");
        if (leaveType != null && !leaveType.isBlank() && !"P".equals(leaveType) && !"KL".equals(leaveType)) {
            throw new BadRequestException("Loại nghỉ: P (có phép) hoặc KL (không lương)");
        }
        if (checkIn != null && checkOut != null && !checkOut.isAfter(checkIn)) throw new BadRequestException("Giờ ra phải sau giờ vào");
        boolean leave = leaveType != null && !leaveType.isBlank();
        a.setLeaveType(leave ? leaveType : null);
        a.setCheckInAt(leave ? null : checkIn);
        a.setCheckOutAt(leave ? null : checkOut);
        a.setNote(note.trim());
        a.setAdjustedBy(CurrentUser.username());
        return ShiftResponses.Assignment.from(a);
    }

    // ---- chấm công (nhân viên tự chấm) ------------------------------------

    @Transactional(readOnly = true)
    public List<ShiftResponses.Assignment> myToday() {
        Long staffId = requireStaff();
        LocalDate today = LocalDate.now();
        return assignmentRepository.findBetween(today.minusDays(1), today, staffId).stream()
                .filter(a -> a.getWorkDate().equals(today) || (a.getCheckInAt() != null && a.getCheckOutAt() == null))
                .map(ShiftResponses.Assignment::from).toList();
    }

    @Transactional
    public ShiftResponses.Assignment checkIn(Long id) {
        ShiftAssignment a = mine(id);
        LocalDateTime now = LocalDateTime.now();
        if (a.getLeaveType() != null) throw new BadRequestException("Ca này đã ghi nghỉ");
        if (a.getCheckInAt() != null) throw new BadRequestException("Đã chấm vào lúc " + a.getCheckInAt().toLocalTime().withNano(0));
        if (now.isBefore(a.plannedStart().minusMinutes(EARLY_CHECK_IN_MINUTES))) {
            throw new BadRequestException("Chưa tới giờ chấm vào (sớm nhất " + EARLY_CHECK_IN_MINUTES + " phút trước giờ vào ca)");
        }
        if (now.isAfter(a.plannedEnd())) throw new BadRequestException("Ca đã kết thúc — nhờ quản lý chấm bù");
        a.setCheckInAt(now);
        return ShiftResponses.Assignment.from(a);
    }

    @Transactional
    public ShiftResponses.Assignment checkOut(Long id) {
        ShiftAssignment a = mine(id);
        if (a.getCheckInAt() == null) throw new BadRequestException("Chưa chấm vào");
        if (a.getCheckOutAt() != null) throw new BadRequestException("Đã chấm ra lúc " + a.getCheckOutAt().toLocalTime().withNano(0));
        a.setCheckOutAt(LocalDateTime.now());
        return ShiftResponses.Assignment.from(a);
    }

    // ---- bảng chấm công ----------------------------------------------------

    @Transactional(readOnly = true)
    public ShiftResponses.Timesheet timesheet(YearMonth month) {
        LocalDate from = month.atDay(1), to = month.atEndOfMonth();
        LocalDateTime now = LocalDateTime.now();
        Map<Long, List<ShiftAssignment>> byStaff = new LinkedHashMap<>();
        for (ShiftAssignment a : assignmentRepository.findBetween(from, to, null)) {
            byStaff.computeIfAbsent(a.getStaff().getId(), k -> new ArrayList<>()).add(a);
        }
        List<ShiftResponses.TimesheetRow> rows = new ArrayList<>();
        for (var e : byStaff.entrySet()) {
            List<ShiftAssignment> list = e.getValue();
            Staff staff = list.get(0).getStaff();
            int worked = 0, lateTimes = 0, earlyTimes = 0, absent = 0, leaveP = 0, leaveKl = 0, missing = 0;
            long lateMin = 0, minutes = 0;
            Map<Integer, Long> dayMinutes = new TreeMap<>();
            Map<Integer, String> marks = new TreeMap<>();
            for (ShiftAssignment a : list) {
                int day = a.getWorkDate().getDayOfMonth();
                switch (a.state(now)) {
                    case "DONE" -> {
                        worked++;
                        minutes += a.workedMinutes();
                        dayMinutes.merge(day, a.workedMinutes(), Long::sum);
                    }
                    case "ABSENT" -> {
                        absent++;
                        marks.putIfAbsent(day, "V");
                    }
                    case "LEAVE" -> {
                        if ("P".equals(a.getLeaveType())) leaveP++;
                        else leaveKl++;
                        marks.putIfAbsent(day, a.getLeaveType());
                    }
                    case "MISSING_OUT" -> {
                        missing++;
                        marks.putIfAbsent(day, "?");
                    }
                    default -> {
                    }
                }
                if (a.lateMinutes() > 0) {
                    lateTimes++;
                    lateMin += a.lateMinutes();
                }
                if (a.earlyLeaveMinutes() > 0) earlyTimes++;
            }
            dayMinutes.forEach((d, m) -> marks.put(d, String.format(java.util.Locale.ROOT, "%.1f", m / 60.0)));
            rows.add(new ShiftResponses.TimesheetRow(staff.getId(), staff.getFullName(), staff.getPosition(), list.size(),
                    worked, Math.round(minutes / 6.0) / 10.0, lateTimes, lateMin, earlyTimes, absent, leaveP, leaveKl,
                    missing, marks));
        }
        rows.sort((a, b) -> a.staffName().compareToIgnoreCase(b.staffName()));
        return new ShiftResponses.Timesheet(month.getYear(), month.getMonthValue(), month.lengthOfMonth(), rows);
    }

    // ---- helpers ---------------------------------------------------------

    private Long requireStaff() {
        Long id = CurrentUser.staffId();
        if (id == null) throw new BadRequestException("Tài khoản không gắn với nhân viên nào");
        return id;
    }

    private ShiftAssignment mine(Long id) {
        ShiftAssignment a = assignment(id);
        if (!a.getStaff().getId().equals(requireStaff())) throw new BadRequestException("Không phải ca của bạn");
        return a;
    }

    private WorkShift shift(Long id) {
        return shiftRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca: " + id));
    }

    private ShiftAssignment assignment(Long id) {
        return assignmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch ca: " + id));
    }
}
