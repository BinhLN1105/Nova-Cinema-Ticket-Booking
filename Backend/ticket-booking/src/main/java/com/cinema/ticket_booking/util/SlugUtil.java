package com.cinema.ticket_booking.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Chuyển chuỗi tiếng Việt sang slug URL-friendly.
 * VD: "Hành Động" → "hanh-dong"
 */
public final class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    private SlugUtil() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank())
            return "";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return NON_LATIN.matcher(
                MULTI_DASH.matcher(
                        normalized
                                .replaceAll("\\p{M}", "") // bỏ dấu
                                .toLowerCase()
                                .trim()
                                .replace(" ", "-"))
                        .replaceAll("-"))
                .replaceAll("")
                .replaceAll("^-|-$", "");
    }
}
