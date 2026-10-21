package com.coffeeshop.entity.enums;

public enum MaterialTransactionType {
    IMPORT,
    /** xuất khác: dùng nội bộ (reason); trả nhà cung cấp is SUPPLIER_RETURN */
    EXPORT,
    SALE,
    /** kiểm kê: chênh lệch so với thực tế */
    ADJUSTMENT,
    /** nguyên liệu thô xuất để chế biến / mở dùng (material_transactions.produced_batch_id) */
    PRODUCTION_USE,
    /** lô nguyên liệu chế biến / đang sử dụng vào kho */
    PRODUCE,
    /** phiếu hủy: hết hạn, hỏng, đổ vỡ, pha hỏng (reason) */
    DISPOSAL,
    /** hủy phiếu nhập: the untouched lot leaves again */
    IMPORT_CANCEL,
    /** phiếu trả hàng nhà cung cấp */
    SUPPLIER_RETURN
}
