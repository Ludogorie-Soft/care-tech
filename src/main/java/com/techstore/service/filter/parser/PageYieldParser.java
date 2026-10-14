package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page yield of a toner or ink cartridge, filed into a range ("3 000 – 5 999 стр."): the exact figure
 * differs per supplier and per colour, the range is what a shopper compares.
 * <p>
 * "Up to 10 200 pages", "3,100 pages", "4200 копия" and "3150k" are pages; "2.5K" and "(12K)" are
 * thousands. A bare number is read only when it is the whole text — in a product name "CANON 729" is a
 * model, not a yield. A volume ("9 ml") is not a yield.
 */
@Component
public class PageYieldParser implements FilterValueParser {

    private static final String NUMBER = "(\\d{1,3}(?:[ .]\\d{3})+(?!\\d)|\\d+(?:\\.\\d)?)";
    private static final Pattern WITH_UNIT = Pattern.compile(
            "(?<![\\p{L}\\d.])" + NUMBER + "\\s*(k(?![a-zа-я])|к\\.|копия|pages?|стр)");
    private static final Pattern BARE = Pattern.compile("^\\s*(?:up to\\s*)?" + NUMBER + "\\s*$");

    private static final int MIN_PAGES = 20;
    private static final int MAX_PAGES = 100_000;

    private static final int[] LOWER = {0, 500, 1000, 3000, 6000, 10000};
    private static final String[] LABEL_BG = {"Под 500 стр.", "500 – 999 стр.", "1 000 – 2 999 стр.",
            "3 000 – 5 999 стр.", "6 000 – 9 999 стр.", "10 000+ стр."};
    private static final String[] LABEL_EN = {"Under 500 pages", "500 – 999 pages", "1,000 – 2,999 pages",
            "3,000 – 5,999 pages", "6,000 – 9,999 pages", "10,000+ pages"};

    @Override
    public String code() {
        return "PAGE_YIELD";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        Integer pages = null;
        Matcher unit = WITH_UNIT.matcher(text);
        if (unit.find()) {
            pages = pages(unit.group(1), unit.group(2).startsWith("k") || unit.group(2).startsWith("к"));
        } else {
            Matcher bare = BARE.matcher(text);
            if (bare.find()) {
                pages = pages(bare.group(1), false);
            }
        }
        if (pages == null || pages < MIN_PAGES || pages > MAX_PAGES) {
            return Optional.empty();
        }
        int range = 0;
        while (range + 1 < LOWER.length && pages >= LOWER[range + 1]) {
            range++;
        }
        return Optional.of(new ParsedValue("from-" + LOWER[range], LABEL_BG[range], LABEL_EN[range],
                BigDecimal.valueOf(LOWER[range])));
    }

    /** "10 200" and "3.100" are thousands groups; "2.5" with a k is 2500; "3150k" is 3150 pages. */
    private static Integer pages(String number, boolean kilo) {
        String digits = number.matches("\\d{1,3}(?:[ .]\\d{3})+") ? number.replaceAll("[ .]", "") : number;
        BigDecimal value = new BigDecimal(digits);
        if (kilo && value.compareTo(BigDecimal.valueOf(100)) < 0) {
            value = value.multiply(BigDecimal.valueOf(1000));
        }
        return value.stripTrailingZeros().scale() > 0 ? null : value.intValueExact();
    }
}
