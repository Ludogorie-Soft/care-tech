package com.techstore.service.filter.parser;

import java.util.Optional;

/**
 * Turns the free text suppliers send for a numeric attribute ("27\" (68.6 cm)", "68,6 см", "27 инча")
 * into one canonical value. A parser returns empty rather than guess: an ambiguous text lands in the
 * unmapped report, where a value rule can place it.
 */
public interface FilterValueParser {

    /** Value of {@code filter_attributes.parser} this parser serves. */
    String code();

    Optional<ParsedValue> parse(String textBg, String textEn);
}
