package com.techstore.service.sync;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns the text of one TEKRA feed property into the values it holds.
 *
 * <p>A property can carry several values: its {@code <prop_x>} tag repeated inside one item (kept as a
 * list by {@code TekraApiService}), values separated by {@code <br/>}, or the same text joined to itself
 * with a comma ("Взривозащитен,Взривозащитен"). Each value becomes a parameter option of its own, like
 * the multi-option parameters of VALI.
 */
final class TekraFeedValues {

    /** parameter_options.name_bg is TEXT; the cap only guards against runaway text. It was 200. */
    static final int MAX_VALUE_LENGTH = 2000;

    private static final Pattern LINE_BREAK = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern HAS_LETTER = Pattern.compile("\\p{L}");

    private TekraFeedValues() {
    }

    /** What one sync run met in the feed, for its log line. */
    static final class Stats {
        int repeatedTags;
        int multiValueProperties;
        int tooLongValues;
    }

    /**
     * The distinct values of one property, in feed order. Empty, "null", "-", links and values over
     * {@link #MAX_VALUE_LENGTH} characters are dropped.
     *
     * @param raw   the feed value: a string, or a list of strings for a repeated tag
     * @param stats counts to update, or {@code null}
     */
    static List<String> values(Object raw, Stats stats) {
        List<String> texts;
        if (raw instanceof List<?> list) {
            texts = list.stream().filter(Objects::nonNull).map(Object::toString).toList();
            if (stats != null && texts.size() > 1) stats.repeatedTags++;
        } else if (raw instanceof String text) {
            texts = List.of(text);
        } else {
            return List.of();
        }

        Set<String> values = new LinkedHashSet<>();
        for (String text : texts) {
            for (String piece : LINE_BREAK.split(text)) {
                String value = collapseDoubled(piece.trim());
                if (value.isEmpty() || "null".equalsIgnoreCase(value) || "-".equals(value)
                        || value.startsWith("http://") || value.startsWith("https://")) {
                    continue;
                }
                if (value.length() > MAX_VALUE_LENGTH) {
                    if (stats != null) stats.tooLongValues++;
                    continue;
                }
                values.add(value);
            }
        }
        if (stats != null && values.size() > 1) stats.multiValueProperties++;
        return List.copyOf(values);
    }

    /**
     * "A,A" becomes "A" and "A, B,A, B" becomes "A, B": the feed sometimes joins a value to itself.
     * Only text with a letter in it is collapsed, so a decimal such as "2,2" stays as it is.
     */
    static String collapseDoubled(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length < 2 || parts.length % 2 != 0) return value;
        int half = parts.length / 2;
        for (int i = 0; i < half; i++) {
            if (!parts[i].trim().equals(parts[i + half].trim())) return value;
        }
        String first = String.join(",", Arrays.copyOfRange(parts, 0, half)).trim();
        return HAS_LETTER.matcher(first).find() ? first : value;
    }
}
