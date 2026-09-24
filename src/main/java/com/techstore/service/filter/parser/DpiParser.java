package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Highest mouse sensitivity: "1000/1400/1800 DPI" is 1800, "100~10,000 CPI" is 10000, "26K DPI" is
 * 26000. Thousands separators are removed before reading.
 */
@Component
public class DpiParser implements FilterValueParser {

    private static final Pattern KILO = Pattern.compile("(?<![\\d.])(\\d{1,2})\\s*k(?![a-zа-я])");
    private static final Pattern NUMBER = Pattern.compile("(?<![\\d.])(\\d{3,5})(?![\\d.])");

    @Override
    public String code() {
        return "DPI_MAX";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        text = text.replaceAll("(\\d)[.,](\\d{3})(?!\\d)", "$1$2");
        int max = 0;
        Matcher k = KILO.matcher(text);
        while (k.find()) {
            max = Math.max(max, Integer.parseInt(k.group(1)) * 1000);
        }
        Matcher n = NUMBER.matcher(text);
        while (n.find()) {
            max = Math.max(max, Integer.parseInt(n.group(1)));
        }
        if (max < 200 || max > 60000) {
            return Optional.empty();
        }
        String key = String.valueOf(max);
        return Optional.of(new ParsedValue(key, key + " DPI", key + " DPI", BigDecimal.valueOf(max)));
    }
}
