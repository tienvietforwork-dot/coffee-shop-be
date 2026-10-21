package com.coffeeshop.service;

import com.coffeeshop.dto.request.CashShiftCloseRequest;
import com.coffeeshop.dto.response.CashShiftResponse;
import com.coffeeshop.entity.CashMovement;
import com.coffeeshop.entity.CashShift;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.CashShiftRepository;
import com.coffeeshop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Ca thu ngân: mở ca (tiền đầu ca, mặc định = tiền ca trước để lại) → thu / chi ngoài bán hàng trong ca →
 * chốt ca (đếm tiền theo mệnh giá, so với tiền mặt lý thuyết, ghi lý do chênh lệch, để lại / nộp tiền) → in biên bản
 * bàn giao ca. Sales are the payments collected while the shift was open, by method.
 */
@Service
@RequiredArgsConstructor
public class CashShiftService {

    /** tiền Việt Nam đang lưu hành */
    private static final Set<Integer> DENOMINATIONS = Set.of(500_000, 200_000, 100_000, 50_000, 20_000, 10_000, 5_000,
            2_000, 1_000, 500, 200);

    private final CashShiftRepository shiftRepository;
    private final PaymentRepository paymentRepository;
    private final StaffService staffService;

    @Transactional(readOnly = true)
    public CashShiftResponse current() {
        return shiftRepository.findFirstByClosedAtIsNullOrderByIdDesc().map(s -> CashShiftResponse.from(s, sales(s, LocalDateTime.now())))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CashShiftResponse> search(LocalDate from, LocalDate to) {
        return shiftRepository.search(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .map(s -> CashShiftResponse.from(s, s.isOpen() ? sales(s, LocalDateTime.now()) : null)).toList();
    }

    /** cash the previous shift left in the till: the default opening cash */
    @Transactional(readOnly = true)
    public BigDecimal suggestedOpening() {
        return shiftRepository.findFirstByClosedAtIsNotNullOrderByClosedAtDesc().map(CashShift::getNextOpeningCash)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public CashShiftResponse open(BigDecimal openingCash, String note) {
        if (shiftRepository.findFirstByClosedAtIsNullOrderByIdDesc().isPresent()) {
            throw new BadRequestException("Đang có ca chưa chốt — chốt ca đó trước khi mở ca mới");
        }
        CashShift s = shiftRepository.save(CashShift.builder().code(newCode()).staff(staffService.current())
                .openedAt(LocalDateTime.now()).openingCash(openingCash).note(note == null || note.isBlank() ? null : note.trim()).build());
        return CashShiftResponse.from(s, sales(s, LocalDateTime.now()));
    }

    /** Thu / chi tiền mặt ngoài bán hàng. */
    @Transactional
    public CashShiftResponse move(String type, BigDecimal amount, String reason) {
        CashShift s = openShift();
        if (!"IN".equals(type) && !"OUT".equals(type)) throw new BadRequestException("Loại phải là IN (thu) hoặc OUT (chi)");
        s.getMovements().add(CashMovement.builder().shift(s).type(type).amount(amount).reason(reason.trim())
                .staff(staffService.current()).build());
        shiftRepository.flush();
        return CashShiftResponse.from(s, sales(s, LocalDateTime.now()));
    }

    /** Hoàn tiền mặt cho khách: the money leaves the till of the shift now open (none open: nothing to record). */
    @Transactional
    public void recordCashRefund(String orderCode, BigDecimal amount) {
        shiftRepository.findFirstByClosedAtIsNullOrderByIdDesc().ifPresent(s -> s.getMovements().add(CashMovement.builder()
                .shift(s).type("OUT").amount(amount).reason("Hoàn tiền mặt đơn " + orderCode).staff(staffService.current()).build()));
    }

    @Transactional
    public CashShiftResponse close(CashShiftCloseRequest request) {
        CashShift s = openShift();
        LocalDateTime now = LocalDateTime.now();
        CashShiftResponse live = CashShiftResponse.from(s, sales(s, now));
        BigDecimal counted = BigDecimal.ZERO;
        Map<Integer, Integer> sorted = new TreeMap<>(java.util.Comparator.reverseOrder());
        sorted.putAll(request.denominations());
        for (var e : sorted.entrySet()) {
            if (!DENOMINATIONS.contains(e.getKey())) throw new BadRequestException("Mệnh giá không hợp lệ: " + e.getKey());
            if (e.getValue() == null || e.getValue() < 0) throw new BadRequestException("Số tờ không hợp lệ");
            counted = counted.add(BigDecimal.valueOf((long) e.getKey() * e.getValue()));
        }
        BigDecimal difference = counted.subtract(live.expectedCash());
        if (difference.signum() != 0 && (request.differenceReason() == null || request.differenceReason().isBlank())) {
            throw new BadRequestException("Tiền thực đếm lệch " + difference.stripTrailingZeros().toPlainString()
                    + "đ so với lý thuyết — ghi lý do chênh lệch");
        }
        BigDecimal next = request.nextOpeningCash() == null ? BigDecimal.ZERO : request.nextOpeningCash();
        if (next.compareTo(counted) > 0) throw new BadRequestException("Tiền để lại ca sau lớn hơn tiền thực đếm");
        s.setClosedAt(now);
        s.setClosedBy(staffService.current());
        s.setOrderCount(live.orderCount());
        s.setCashSales(live.cashSales());
        s.setTransferSales(live.transferSales());
        s.setCardSales(live.cardSales());
        s.setEwalletSales(live.ewalletSales());
        s.setCashIn(live.cashIn());
        s.setCashOut(live.cashOut());
        s.setExpectedCash(live.expectedCash());
        s.setCountedCash(counted);
        s.setDenominations(sorted.entrySet().stream().filter(e -> e.getValue() > 0)
                .map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(",")));
        s.setDifferenceReason(difference.signum() == 0 ? null : request.differenceReason().trim());
        s.setNextOpeningCash(next);
        s.setHandoverCash(counted.subtract(next));
        s.setReceivedBy(request.receivedBy() == null || request.receivedBy().isBlank() ? null : request.receivedBy().trim());
        if (request.note() != null && !request.note().isBlank()) s.setNote(request.note().trim());
        return CashShiftResponse.from(s, null);
    }

    @Transactional(readOnly = true)
    public CashShiftResponse find(Long id) {
        CashShift s = shiftRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca: " + id));
        return CashShiftResponse.from(s, s.isOpen() ? sales(s, LocalDateTime.now()) : null);
    }

    private CashShift openShift() {
        return shiftRepository.findFirstByClosedAtIsNullOrderByIdDesc()
                .orElseThrow(() -> new BadRequestException("Chưa mở ca — mở ca trước"));
    }

    /** payments collected since the shift opened */
    private CashShiftResponse.Sales sales(CashShift s, LocalDateTime until) {
        BigDecimal cash = BigDecimal.ZERO, transfer = BigDecimal.ZERO, card = BigDecimal.ZERO, ewallet = BigDecimal.ZERO;
        int count = 0;
        for (Object[] r : paymentRepository.collectedBetween(s.getOpenedAt(), until.plusSeconds(1))) {
            PaymentMethod m = (PaymentMethod) r[0];
            BigDecimal amount = (BigDecimal) r[2];
            count += ((Number) r[1]).intValue();
            switch (m) {
                case CASH -> cash = cash.add(amount);
                case BANK_TRANSFER -> transfer = transfer.add(amount);
                case CARD -> card = card.add(amount);
                default -> ewallet = ewallet.add(amount);
            }
        }
        return new CashShiftResponse.Sales(count, cash, transfer, card, ewallet);
    }

    /** CA + yyMMdd + running number of the day */
    private String newCode() {
        String prefix = "CA" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        return prefix + String.format("%02d", shiftRepository.countByCodeStartingWith(prefix) + 1);
    }
}
