package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Number of network ports on a switch: "16 портов", "8-port", "16 x 10/100/1000M PoE", "16 (RJ-45)".
 * "24x RJ-45; 4x SFP" is a 24-port switch, so the largest group wins; one or two ports are uplinks or
 * a media converter, not a switch size. Model numbers ("GS1100-16") are not read.
 */
@Component
public class PortCountParser implements FilterValueParser {

    private static final Pattern WORD = Pattern.compile("(?<![\\d.])(\\d{1,2})\\s*-?\\s*(?:портов|порта|ports?)(?![a-zа-я])");
    private static final Pattern TIMES = Pattern.compile(
            "(?<![\\d.])(\\d{1,2})\\s*[x×х]\\s*(?=\\d|\\(|rj|giga|ge\\b|fe\\b|poe|ethernet|fast)");
    private static final Pattern RJ45 = Pattern.compile("(?<![\\d.])(\\d{1,2})\\s*\\(rj-?45\\)");
    private static final int MIN_PORTS = 3;
    private static final int MAX_PORTS = 64;

    @Override
    public String code() {
        return "PORT_COUNT";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        int ports = 0;
        for (Pattern pattern : new Pattern[] {WORD, TIMES, RJ45}) {
            Matcher m = pattern.matcher(text);
            while (m.find()) {
                ports = Math.max(ports, Integer.parseInt(m.group(1)));
            }
        }
        if (ports < MIN_PORTS || ports > MAX_PORTS) {
            return Optional.empty();
        }
        String label = ports + " порта";
        return Optional.of(new ParsedValue(String.valueOf(ports), label, ports + " ports", BigDecimal.valueOf(ports)));
    }
}
