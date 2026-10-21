package com.coffeeshop.dto.response;

import com.coffeeshop.entity.CashMovement;
import com.coffeeshop.entity.CashShift;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ca thu ngân. While open the sales figures are live (so far); once closed they are what was frozen at closing.
 * {@code difference} = thực đếm − lý thuyết (âm = thiếu tiền).
 */
public record CashShiftResponse(Long id, String code, boolean open, String staffName, LocalDateTime openedAt,
                                BigDecimal openingCash, LocalDateTime closedAt, String closedByName, int orderCount,
                                BigDecimal cashSales, BigDecimal transferSales, BigDecimal cardSales, BigDecimal ewalletSales,
                                BigDecimal totalSales, BigDecimal cashIn, BigDecimal cashOut, BigDecimal expectedCash,
                                BigDecimal countedCash, BigDecimal difference, String denominations, String differenceReason,
                                BigDecimal nextOpeningCash, BigDecimal handoverCash, String receivedBy, String note,
                                List<Movement> movements) {

    public record Movement(Long id, String type, BigDecimal amount, String reason, String staffName, LocalDateTime at) {
        static Movement from(CashMovement m) {
            return new Movement(m.getId(), m.getType(), m.getAmount(), m.getReason(),
                    m.getStaff() == null ? m.getCreatedBy() : m.getStaff().getFullName(), m.getCreatedAt());
        }
    }

    /** live figures of an open shift */
    public record Sales(int orderCount, BigDecimal cash, BigDecimal transfer, BigDecimal card, BigDecimal ewallet) {
    }

    public static CashShiftResponse from(CashShift s, Sales live) {
        BigDecimal in = s.isOpen() ? sum(s, "IN") : s.getCashIn();
        BigDecimal out = s.isOpen() ? sum(s, "OUT") : s.getCashOut();
        int orders = s.isOpen() ? live.orderCount() : nz(s.getOrderCount());
        BigDecimal cash = s.isOpen() ? live.cash() : s.getCashSales();
        BigDecimal transfer = s.isOpen() ? live.transfer() : s.getTransferSales();
        BigDecimal card = s.isOpen() ? live.card() : s.getCardSales();
        BigDecimal ewallet = s.isOpen() ? live.ewallet() : s.getEwalletSales();
        BigDecimal expected = s.isOpen() ? s.getOpeningCash().add(cash).add(in).subtract(out) : s.getExpectedCash();
        return new CashShiftResponse(s.getId(), s.getCode(), s.isOpen(),
                s.getStaff() == null ? s.getCreatedBy() : s.getStaff().getFullName(), s.getOpenedAt(), s.getOpeningCash(),
                s.getClosedAt(), s.getClosedBy() == null ? null : s.getClosedBy().getFullName(), orders, cash, transfer, card,
                ewallet, cash.add(transfer).add(card).add(ewallet), in, out, expected, s.getCountedCash(),
                s.getCountedCash() == null ? null : s.getCountedCash().subtract(expected), s.getDenominations(),
                s.getDifferenceReason(), s.getNextOpeningCash(), s.getHandoverCash(), s.getReceivedBy(), s.getNote(),
                s.getMovements().stream().map(Movement::from).toList());
    }

    private static BigDecimal sum(CashShift s, String type) {
        return s.getMovements().stream().filter(m -> type.equals(m.getType())).map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
