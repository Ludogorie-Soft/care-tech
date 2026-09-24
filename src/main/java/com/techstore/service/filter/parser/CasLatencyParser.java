package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** CAS latency: "CL16", "CL 16-20-20", "16-18-18-38" and a bare "16" all read as CL16 (the first timing). */
@Component
public class CasLatencyParser implements FilterValueParser {

    private static final Pattern CL = Pattern.compile("cl\\s*-?\\s*(\\d{1,2})(?!\\d)");
    private static final Pattern TIMINGS = Pattern.compile("^\\s*(\\d{1,2})\\s*(?:-\\s*\\d{1,2}|$)");

    @Override
    public String code() {
        return "CAS_CL";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        Matcher m = CL.matcher(text);
        if (!m.find()) {
            m = TIMINGS.matcher(text);
            if (!m.find()) {
                return Optional.empty();
            }
        }
        int value = Integer.parseInt(m.group(1));
        if (value < 4 || value > 70) {
            return Optional.empty();
        }
        return Optional.of(new ParsedValue(String.valueOf(value), "CL" + value, "CL" + value, BigDecimal.valueOf(value)));
    }
}
