package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Screen diagonal in inches. Suppliers write 27", 27 inch, 27 инча, 15.6-inch, 68.6 cm or both at
 * once; an inch figure wins over a centimetre one, and a bare number is read as inches.
 */
@Component
public class DiagonalInchParser implements FilterValueParser {

    // 27", 27 inch, 27 инча, 15.6-inch, 14 (inch), 15.6 in
    private static final Pattern INCH = Pattern.compile(
            "(\\d{1,3}(?:\\.\\d{1,2})?)\\s*[-(]?\\s*(?:\"|inch|инч|in(?![a-zа-я]))");
    private static final Pattern CM = Pattern.compile("(\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:cm|см)(?![a-zа-я])");
    private static final Pattern BARE = Pattern.compile("^\\s*(\\d{1,3}(?:\\.\\d{1,2})?)\\s*$");
    private static final BigDecimal CM_PER_INCH = new BigDecimal("2.54");
    private static final BigDecimal MIN = new BigDecimal("1");
    private static final BigDecimal MAX = new BigDecimal("120");

    @Override
    public String code() {
        return "DIAGONAL_INCH";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }

        Set<BigDecimal> inches = collect(INCH, text, false);
        if (inches.isEmpty()) {
            inches = collect(CM, text, true);
        }
        if (inches.isEmpty()) {
            inches = collect(BARE, text, false);
        }
        if (inches.size() != 1) {
            return Optional.empty();
        }

        BigDecimal value = inches.iterator().next();
        if (value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) {
            return Optional.empty();
        }
        String key = ParserSupport.plain(value);
        return Optional.of(new ParsedValue(key, key + "\"", key + "\"", value));
    }

    private static Set<BigDecimal> collect(Pattern pattern, String text, boolean centimetres) {
        Set<BigDecimal> values = new TreeSet<>();
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            BigDecimal number = new BigDecimal(m.group(1));
            if (centimetres) {
                number = number.divide(CM_PER_INCH, 1, RoundingMode.HALF_UP);
            } else {
                number = number.setScale(1, RoundingMode.HALF_UP);
            }
            values.add(number.stripTrailingZeros());
        }
        return values;
    }
}
