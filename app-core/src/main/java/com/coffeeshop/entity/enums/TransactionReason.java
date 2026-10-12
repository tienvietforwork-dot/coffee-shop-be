package com.coffeeshop.entity.enums;

/** Why stock left by hand: DISPOSAL reasons (phiếu hủy) and EXPORT reasons (xuất khác). */
public enum TransactionReason {
    EXPIRED, SPOILED, SPILLED, BREW_FAILED, OTHER_DISPOSAL,
    RETURN_SUPPLIER, INTERNAL_USE;

    public boolean isDisposal() {
        return ordinal() <= OTHER_DISPOSAL.ordinal();
    }
}
