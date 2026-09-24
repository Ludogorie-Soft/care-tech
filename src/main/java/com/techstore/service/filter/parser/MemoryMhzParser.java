package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Memory speed in MHz (MT/s): "6000 MHz", "6000MT/s", "DDR5-6000", "3200MHz(PC4-25600)", or a bare
 * "3200". The highest figure wins, so a processor supporting "3200/5600" lands on 5600.
 */
@Component
public class MemoryMhzParser implements FilterValueParser {

    private static final Pattern SPEED = Pattern.compile(
            "(?:ddr\\d[a-z]?[- ]?(\\d{4}))|(?:(\\d{3,5})\\s*(?:mhz|mt/s|мхц))");
    private static final Pattern BARE = Pattern.compile("^\\s*(\\d{4})\\s*$");

    @Override
    public String code() {
        return "MEM_MHZ";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        Integer max = null;
        Matcher m = SPEED.matcher(text);
        while (m.find()) {
            int value = Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(2));
            max = max == null ? value : Math.max(max, value);
        }
        if (max == null) {
            Matcher bare = BARE.matcher(text);
            if (bare.find()) {
                max = Integer.parseInt(bare.group(1));
            }
        }
        if (max == null || max < 800 || max > 12000) {
            return Optional.empty();
        }
        String key = String.valueOf(max);
        return Optional.of(new ParsedValue(key, key + " MHz", key + " MHz", BigDecimal.valueOf(max)));
    }
}
