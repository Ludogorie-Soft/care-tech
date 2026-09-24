package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processor clock in GHz, the highest figure in the text: "Base Clock: 3.6GHz; Max Boost Clock: 4GHz"
 * is sold as 4 GHz. Also reads the "4.4G" product names carry and MHz figures above 1000.
 */
@Component
public class FrequencyGhzParser implements FilterValueParser {

    private static final Pattern GHZ = Pattern.compile("(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:ghz|ггц|g(?![a-zа-я0-9]))");
    private static final Pattern MHZ = Pattern.compile("(\\d{4})\\s*(?:mhz|мхц)");

    @Override
    public String code() {
        return "FREQ_GHZ";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        BigDecimal max = null;
        Matcher g = GHZ.matcher(text);
        while (g.find()) {
            max = higher(max, new BigDecimal(g.group(1)));
        }
        if (max == null) {
            Matcher m = MHZ.matcher(text);
            while (m.find()) {
                max = higher(max, new BigDecimal(m.group(1)).divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP));
            }
        }
        if (max == null || max.compareTo(new BigDecimal("0.5")) < 0 || max.compareTo(new BigDecimal("7")) > 0) {
            return Optional.empty();
        }
        BigDecimal value = max.setScale(1, RoundingMode.HALF_UP);
        String key = ParserSupport.plain(value);
        return Optional.of(new ParsedValue(key, key + " GHz", key + " GHz", value));
    }

    private static BigDecimal higher(BigDecimal current, BigDecimal candidate) {
        return current == null || candidate.compareTo(current) > 0 ? candidate : current;
    }
}
