package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A clearance in millimetres ("до 410 mm", "410 мм", "164.5 mm") — the first figure, whole mm. */
@Component
public class LengthMmParser implements FilterValueParser {

    private static final Pattern MM = Pattern.compile("(?<![\\d.])(\\d{2,3}(?:\\.\\d)?)\\s*(?:mm|мм)");
    private static final Pattern BARE = Pattern.compile("^\\s*(?:до\\s*)?(\\d{2,3}(?:\\.\\d)?)\\s*$");

    @Override
    public String code() {
        return "LENGTH_MM";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        Matcher m = MM.matcher(text);
        if (!m.find()) {
            m = BARE.matcher(text);
            if (!m.find()) {
                return Optional.empty();
            }
        }
        int mm = new BigDecimal(m.group(1)).setScale(0, RoundingMode.HALF_UP).intValue();
        if (mm < 30 || mm > 600) {
            return Optional.empty();
        }
        String key = String.valueOf(mm);
        return Optional.of(new ParsedValue(key, key + " mm", key + " mm", BigDecimal.valueOf(mm)));
    }
}
