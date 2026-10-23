package com.coffeeshop.crm.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Phân loại cảm xúc dựa trên từ khóa; dữ liệu nhân viên đã nhập luôn được ưu tiên. */
@Component
public class FeedbackSentimentAnalyzer {
    private static final Set<String> NEGATIVE = Set.of(
            "khong hai long", "that vong", "te", "kem", "cham", "lau", "loi", "hong",
            "ca phe nguoi", "do uong nguoi", "nguoi lanh", "sai", "thieu", "khong ngon", "khong quay lai", "khieu nai");
    private static final Set<String> POSITIVE = Set.of(
            "hai long", "tot", "tuyet voi", "xuat sac", "ngon", "thom", "nhanh", "than thien",
            "chuyen nghiep", "yeu thich", "cam on", "khen", "uy tin");

    public String analyze(String content, Integer rating) {
        String normalized = normalize(content == null ? "" : content);
        int negative = score(normalized, NEGATIVE);
        String positiveText = normalized.replace("khong hai long", "")
                .replace("khong ngon", "").replace("khong tot", "")
                .replace("khong thich", "").replace("khong quay lai", "");
        int positive = score(positiveText, POSITIVE);
        if (rating != null) {
            if (rating <= 2) negative += 2;
            if (rating >= 4) positive += 2;
        }
        if (negative > positive) return "NEGATIVE";
        if (positive > negative) return "POSITIVE";
        return "NEUTRAL";
    }

    private static int score(String text, Set<String> words) {
        return (int) words.stream().filter(text::contains).count();
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return decomposed.replace('đ', 'd').replaceAll("[^a-z0-9]+", " ").trim();
    }
}
