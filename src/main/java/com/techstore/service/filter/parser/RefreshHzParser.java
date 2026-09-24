package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Refresh rate in hertz. When a text carries several rates ("60 Hz / 75 Hz (OC)") the highest one is
 * the rate the display is sold on, so that is the value.
 */
@Component
public class RefreshHzParser implements FilterValueParser {

    private static final Pattern HZ = Pattern.compile("(\\d{2,3})\\s*\\(?\\s*(?:hz|хц|херц)");
    private static final Pattern BARE = Pattern.compile("^\\s*(\\d{2,3})\\s*$");

    @Override
    public String code() {
        return "REFRESH_HZ";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        Integer max = highest(HZ, text);
        if (max == null) {
            max = highest(BARE, text);
        }
        if (max == null || max < 24 || max > 600) {
            return Optional.empty();
        }
        String key = String.valueOf(max);
        return Optional.of(new ParsedValue(key, key + " Hz", key + " Hz", BigDecimal.valueOf(max)));
    }

    private static Integer highest(Pattern pattern, String text) {
        Integer max = null;
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            int value = Integer.parseInt(m.group(1));
            if (max == null || value > max) {
                max = value;
            }
        }
        return max;
    }
}
