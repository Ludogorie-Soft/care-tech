package com.techstore.service.filter.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

final class ParserSupport {

    private ParserSupport() {
    }

    /**
     * Lower-cased text without trademark signs, with decimal commas turned into points and the many
     * inch marks suppliers use folded into a plain double quote — the same clean-up filter_norm() does
     * in SQL ("GeForce RTX™ 5060" reads as "rtx 5060"). The Bulgarian text wins; the English one is the
     * fallback.
     */
    static String prepare(String textBg, String textEn) {
        String text = textBg != null && !textBg.isBlank() ? textBg : textEn;
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[™®©]", "")
                .replaceAll("(\\d),(\\d)", "$1.$2")
                .replace("''", "\"")
                .replace("``", "\"")
                .replace('″', '"')
                .replace('”', '"')
                .replace('“', '"')
                .replace('\u00a0', ' ');
    }

    /** "27.00" -> "27", "23.80" -> "23.8": the key and label form of a number. */
    static String plain(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0, RoundingMode.UNNECESSARY).toPlainString()
                : stripped.toPlainString();
    }
}
