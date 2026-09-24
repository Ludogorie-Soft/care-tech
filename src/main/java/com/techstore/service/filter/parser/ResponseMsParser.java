package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Response time in milliseconds. "1 ms (GTG)", "1ms MPRT" and "1 мс" are one value; when a text gives
 * several ("5 ms (GTG), 1 ms MPRT") the lowest is the figure the display is sold on.
 */
@Component
public class ResponseMsParser implements FilterValueParser {

    private static final Pattern MS = Pattern.compile("(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:ms|мс)(?![a-zа-я])");

    @Override
    public String code() {
        return "RESPONSE_MS";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        BigDecimal lowest = null;
        Matcher m = MS.matcher(text);
        while (m.find()) {
            BigDecimal value = new BigDecimal(m.group(1));
            if (lowest == null || value.compareTo(lowest) < 0) {
                lowest = value;
            }
        }
        if (lowest == null || lowest.signum() <= 0 || lowest.compareTo(new BigDecimal("50")) > 0) {
            return Optional.empty();
        }
        String key = ParserSupport.plain(lowest);
        return Optional.of(new ParsedValue(key, key + " ms", key + " ms", lowest));
    }
}
