package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Display resolution as width x height. Marketing names stand in when no numbers are given; plain
 * "HD" is left alone because suppliers use it for both 1280x720 and 1366x768.
 */
@Component
public class ResolutionParser implements FilterValueParser {

    private static final Pattern PAIR = Pattern.compile("(\\d{3,4})\\s*[x×х*]\\s*(\\d{3,4})");
    // Longest names first: "uwqhd" contains "wqhd", which contains "qhd". "2.8k" and "2.5k" are the
    // laptop names for 2880x1800 and 2560x1600.
    private static final String[][] NAMES = {
            {"2.8k", "2880x1800"}, {"2.5k", "2560x1600"}, {"uwqhd", "3440x1440"}, {"wqhd", "2560x1440"}, {"qhd", "2560x1440"}, {"2k", "2560x1440"},
            {"4k", "3840x2160"}, {"uhd", "3840x2160"}, {"full hd", "1920x1080"}, {"fullhd", "1920x1080"},
            {"fhd", "1920x1080"}, {"1080p", "1920x1080"},
    };

    @Override
    public String code() {
        return "RESOLUTION";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        // "2.560 x 1.600": thousands separators, not decimals.
        text = text.replaceAll("(\\d)[.,](\\d{3})(?!\\d)", "$1$2");

        Set<String> pairs = new LinkedHashSet<>();
        Matcher m = PAIR.matcher(text);
        while (m.find()) {
            pairs.add(Integer.parseInt(m.group(1)) + "x" + Integer.parseInt(m.group(2)));
        }
        if (pairs.size() > 1) {
            return Optional.empty();
        }
        if (pairs.isEmpty()) {
            for (String[] name : NAMES) {
                if (text.contains(name[0])) {
                    pairs.add(name[1]);
                    break;
                }
            }
        }
        if (pairs.isEmpty()) {
            return Optional.empty();
        }

        String key = pairs.iterator().next();
        String[] wh = key.split("x");
        BigDecimal pixels = BigDecimal.valueOf((long) Integer.parseInt(wh[0]) * Integer.parseInt(wh[1]));
        return Optional.of(new ParsedValue(key, key, key, pixels));
    }
}
