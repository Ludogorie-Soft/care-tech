package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Newest Wi-Fi generation a device supports: "802.11 a/b/g/n/ac/ax" is Wi-Fi 6, not four values.
 * Read from the 802.11 letters, from "Wi-Fi 6E" and from the speed class in names ("AX3000", "BE3600",
 * "AC1200", "N300"). Wi-Fi 6 with a 6 GHz band is Wi-Fi 6E.
 */
@Component
public class WifiStandardParser implements FilterValueParser {

    private static final Pattern IEEE = Pattern.compile("802\\.11\\s*([a-z]{1,2}(?:\\s*[/,]\\s*[a-z]{1,2})*)");
    private static final Pattern WIFI = Pattern.compile("wi-?fi\\s*([4-7])(e)?(?![0-9])");
    private static final Pattern SPEED_CLASS = Pattern.compile("(?<![a-z])(be|ax|ac|n)(\\d{3,5})(?!\\d)");
    private static final Pattern SIX_GHZ = Pattern.compile("(?<![0-9.])6\\s*ghz");

    @Override
    public String code() {
        return "WIFI_GEN";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        int generation = 0;
        boolean sixE = false;

        Matcher ieee = IEEE.matcher(text);
        while (ieee.find()) {
            for (String letters : ieee.group(1).split("[/,\\s]+")) {
                generation = Math.max(generation, generation(letters));
            }
        }
        Matcher wifi = WIFI.matcher(text);
        while (wifi.find()) {
            generation = Math.max(generation, Integer.parseInt(wifi.group(1)));
            sixE |= wifi.group(2) != null;
        }
        Matcher speedClass = SPEED_CLASS.matcher(text);
        while (speedClass.find()) {
            generation = Math.max(generation, generation(speedClass.group(1)));
        }
        if (generation == 0) {
            return Optional.empty();
        }
        if (generation == 6 && (sixE || SIX_GHZ.matcher(text).find())) {
            return Optional.of(new ParsedValue("wifi6e", "Wi-Fi 6E (802.11ax, 6 GHz)", "Wi-Fi 6E (802.11ax, 6 GHz)",
                    new BigDecimal("6.5")));
        }
        String letters = switch (generation) {
            case 7 -> "802.11be";
            case 6 -> "802.11ax";
            case 5 -> "802.11ac";
            default -> "802.11n";
        };
        String label = "Wi-Fi " + generation + " (" + letters + ")";
        return Optional.of(new ParsedValue("wifi" + generation, label, label, BigDecimal.valueOf(generation)));
    }

    private static int generation(String letters) {
        return switch (letters) {
            case "be" -> 7;
            case "ax" -> 6;
            case "ac" -> 5;
            case "n" -> 4;
            default -> 0;
        };
    }
}
