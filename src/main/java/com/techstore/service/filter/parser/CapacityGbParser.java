package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Memory or storage capacity. A kit ("2x8GB") counts as its total. Terabytes use the decimal factor
 * storage is sold with, and 1024-multiples of GB ("2048 GB") are read as the same terabytes, so
 * "2 TB" and "2048GB" become one value. Several different capacities in one text are ambiguous.
 */
@Component
public class CapacityGbParser implements FilterValueParser {

    // "g" alone counts as GB: memory is written "16G" and "8G (1x8GB)" as often as "16GB".
    private static final Pattern KIT = Pattern.compile("(\\d{1,2})\\s*[x×х]\\s*(\\d+(?:\\.\\d+)?)\\s*(gb|tb|гб|тб|g)(?![a-zа-я])");
    // "150MB/s" is a speed, not a capacity.
    private static final Pattern SINGLE = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(mb|gb|tb|мб|гб|тб|g)(?![a-zа-я]|/s)");
    // A bare number is GB only as the whole text: the unit is then in the parameter name ("Капацитет (GB)").
    private static final Pattern BARE = Pattern.compile("^\\s*(\\d{1,5})\\s*$");
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal KIBI = new BigDecimal("1024");

    @Override
    public String code() {
        return "CAPACITY_GB";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }

        Matcher kit = KIT.matcher(text);
        if (kit.find()) {
            BigDecimal modules = new BigDecimal(kit.group(1));
            BigDecimal each = toGb(new BigDecimal(kit.group(2)), kit.group(3));
            return Optional.of(value(modules.multiply(each)));
        }

        Set<BigDecimal> values = new TreeSet<>();
        Matcher m = SINGLE.matcher(text);
        while (m.find()) {
            values.add(toGb(new BigDecimal(m.group(1)), m.group(2)).stripTrailingZeros());
        }
        if (values.isEmpty()) {
            Matcher bare = BARE.matcher(text);
            if (bare.find() && Integer.parseInt(bare.group(1)) > 0) {
                values.add(new BigDecimal(bare.group(1)));
            }
        }
        return values.size() == 1 ? Optional.of(value(values.iterator().next())) : Optional.empty();
    }

    private static BigDecimal toGb(BigDecimal number, String unit) {
        return switch (unit) {
            case "tb", "тб" -> number.multiply(THOUSAND);
            case "mb", "мб" -> number.divide(KIBI, 4, java.math.RoundingMode.HALF_UP);
            // "g" and "gb" fall through to GB.
            default -> number;
        };
    }

    private static ParsedValue value(BigDecimal gb) {
        BigDecimal normalized = gb;
        // 2048 GB is 2 TB in the shop's eyes: fold binary terabytes onto the decimal key.
        if (gb.compareTo(KIBI) >= 0 && gb.remainder(KIBI).signum() == 0 && gb.remainder(THOUSAND).signum() != 0) {
            normalized = gb.divide(KIBI).multiply(THOUSAND);
        }
        String label;
        if (normalized.compareTo(THOUSAND) >= 0 && normalized.remainder(new BigDecimal("100")).signum() == 0) {
            label = ParserSupport.plain(normalized.divide(THOUSAND)) + " TB";
        } else if (normalized.compareTo(BigDecimal.ONE) < 0) {
            label = ParserSupport.plain(normalized.multiply(KIBI).setScale(0, java.math.RoundingMode.HALF_UP)) + " MB";
        } else {
            label = ParserSupport.plain(normalized) + " GB";
        }
        return new ParsedValue(ParserSupport.plain(normalized), label, label, normalized);
    }
}
