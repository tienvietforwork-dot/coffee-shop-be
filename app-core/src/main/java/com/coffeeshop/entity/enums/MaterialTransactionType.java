package com.coffeeshop.entity.enums;

public enum MaterialTransactionType {
    IMPORT, EXPORT, SALE, ADJUSTMENT,
    /** nguyên liệu xuất cho một mẻ bán thành phẩm (material_transactions.produced_batch_id) */
    PRODUCTION_USE,
    /** mẻ bán thành phẩm làm xong, nhập kho */
    PRODUCE
}
