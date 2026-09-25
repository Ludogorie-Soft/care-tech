package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rated power in watts: "850 W", "850W 80+ Gold", "1300W, Power Good Signal: ..." — the first figure. */
@Component
public class PowerWattParser implements FilterValueParser {

    private static final Pattern WATT = Pattern.compile("(?<![\\d.])(\\d{2,5})\\s*(?:w|вт)(?![a-zа-я])");

    @Override
    public String code() {
        return "POWER_W";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        Matcher m = WATT.matcher(text);
        if (!m.find()) {
            return Optional.empty();
        }
        int watts = Integer.parseInt(m.group(1));
        // Inverters go up to 15 kW; the cap of 3000 was written for PC power supplies.
        if (watts < 100 || watts > 20000) {
            return Optional.empty();
        }
        String key = String.valueOf(watts);
        return Optional.of(new ParsedValue(key, key + " W", key + " W", BigDecimal.valueOf(watts)));
    }
}
