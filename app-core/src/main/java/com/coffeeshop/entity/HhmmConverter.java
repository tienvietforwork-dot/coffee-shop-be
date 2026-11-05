package com.coffeeshop.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalTime;

/**
 * Giờ trong ngày (giờ mở cửa, khung giờ) lưu dạng "HH:mm": a TIME column would be shifted by
 * hibernate.jdbc.time_zone=UTC, while these are wall-clock times in Việt Nam.
 */
@Converter
public class HhmmConverter implements AttributeConverter<LocalTime, String> {
    @Override
    public String convertToDatabaseColumn(LocalTime t) {
        return t == null ? null : String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    @Override
    public LocalTime convertToEntityAttribute(String s) {
        return s == null || s.isBlank() ? null : LocalTime.parse(s.trim().length() > 5 ? s.trim().substring(0, 5) : s.trim());
    }
}
