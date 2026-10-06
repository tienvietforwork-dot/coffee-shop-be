package com.coffeeshop.dto.request;

/** tableQr is the code printed on the table QR sticker; null for online carts. */
public record CreateCartRequest(String tableQr) {
}
