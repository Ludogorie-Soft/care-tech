package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A count: cores, threads, memory slots. The first whole number in the text wins, which reads
 * "Total Cores: 14;# of Performance-cores: 6" as 14 and "4 x DIMM" as 4.
 */
@Component
public class CountParser implements FilterValueParser {

    private static final Pattern NUMBER = Pattern.compile("(?<![\\d.])(\\d{1,3})(?![\\d.])");

    @Override
    public String code() {
        return "COUNT";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        Matcher m = NUMBER.matcher(text);
        if (!m.find()) {
            return Optional.empty();
        }
        int value = Integer.parseInt(m.group(1));
        if (value < 1 || value > 512) {
            return Optional.empty();
        }
        String key = String.valueOf(value);
        return Optional.of(new ParsedValue(key, key, key, BigDecimal.valueOf(value)));
    }
}
