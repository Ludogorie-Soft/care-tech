package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fan size in millimetres. "120 x 120 x 25 mm" is a 120 mm fan, not 25: a "W x H" pair wins over a
 * lone "... mm" figure. 240, 280 and 360 mm are radiators, not fans — "240mm Liquid Cooler, 2x120mm
 * Fans" is a 120 mm fan, so figures above the largest fan made are skipped.
 */
@Component
public class FanSizeParser implements FilterValueParser {

    private static final Pattern PAIR = Pattern.compile("(?<!\\d)(\\d{2,3})\\s*(?:mm|мм)?\\s*[x×х*]\\s*(\\d{2,3})");
    private static final Pattern MM = Pattern.compile("(?<![\\d.])(\\d{2,3})\\s*(?:mm|мм)");
    private static final int MIN_SIZE = 40;
    private static final int MAX_SIZE = 230;

    @Override
    public String code() {
        return "FAN_MM";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        Integer size = null;
        Matcher pair = PAIR.matcher(text);
        if (pair.find() && pair.group(1).equals(pair.group(2))) {
            size = Integer.parseInt(pair.group(1));
        } else {
            Matcher mm = MM.matcher(text);
            while (size == null && mm.find()) {
                int candidate = Integer.parseInt(mm.group(1));
                if (candidate >= MIN_SIZE && candidate <= MAX_SIZE) {
                    size = candidate;
                }
            }
        }
        if (size == null || size < MIN_SIZE || size > MAX_SIZE) {
            return Optional.empty();
        }
        String key = String.valueOf(size);
        return Optional.of(new ParsedValue(key, key + " mm", key + " mm", BigDecimal.valueOf(size)));
    }
}
