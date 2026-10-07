package com.coffeeshop.dto.response;

/** {@code url} is relative to the API origin, e.g. {@code /api/public/images/12}; store it as the coffee's image_url. */
public record ImageResponse(Long id, String url, String contentType, Integer sizeBytes) {
}
