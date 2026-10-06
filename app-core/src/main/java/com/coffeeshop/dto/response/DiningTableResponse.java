package com.coffeeshop.dto.response;

import com.coffeeshop.entity.DiningTable;
import com.coffeeshop.entity.enums.TableStatus;

public record DiningTableResponse(Long id, String tableNo, String qrCode, String area, Integer capacity,
                                  TableStatus status) {
    public static DiningTableResponse from(DiningTable t) {
        return new DiningTableResponse(t.getId(), t.getTableNo(), t.getQrCode(), t.getArea(), t.getCapacity(),
                t.getStatus());
    }
}
