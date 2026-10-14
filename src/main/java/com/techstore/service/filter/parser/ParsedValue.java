package com.techstore.service.filter.parser;

import java.math.BigDecimal;

/**
 * A raw supplier value turned into a canonical filter value.
 *
 * @param normKey stable key within the attribute; value ids, and with them filter URLs, hang off it
 * @param labelBg what the shopper sees
 * @param labelEn English label, same as {@code labelBg} where the notation is language-neutral
 * @param number  numeric value used to order the values in the filter
 */
public record ParsedValue(String normKey, String labelBg, String labelEn, BigDecimal number) {
}
