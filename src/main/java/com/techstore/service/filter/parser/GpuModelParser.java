package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Graphics chip model: "NVIDIA GeForce RTX 5060 Ti 8GB", "RTX5060TI" and "GeForce RTX 5060 Ti" are all
 * "RTX 5060 Ti". Covers GeForce RTX/GTX/GT, Radeon RX and Intel Arc. The number orders the values:
 * NVIDIA first, then AMD, then Intel, each by model number. Two different models in one text are
 * ambiguous.
 */
@Component
public class GpuModelParser implements FilterValueParser {

    private static final Pattern NVIDIA = Pattern.compile(
            "(?<![a-z])(rtx|gtx|gt)\\s*(\\d{3,4})\\s*(ti\\s*super|ti|super)?(?![a-z0-9])");
    private static final Pattern AMD = Pattern.compile("(?<![a-z])rx\\s*(\\d{3,4})\\s*(xtx|xt|gre)?(?![a-z0-9])");
    private static final Pattern INTEL = Pattern.compile("(?<![a-z])arc\\s*([ab])\\s*(\\d{3})(?![a-z0-9])");

    @Override
    public String code() {
        return "GPU_MODEL";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        Set<String> labels = new LinkedHashSet<>();
        long order = 0;

        Matcher n = NVIDIA.matcher(text);
        while (n.find()) {
            String suffix = n.group(3) == null ? "" : " " + suffixLabel(n.group(3));
            labels.add(n.group(1).toUpperCase() + " " + n.group(2) + suffix);
            order = 100_000L + Long.parseLong(n.group(2)) * 10 + rank(n.group(3));
        }
        Matcher a = AMD.matcher(text);
        while (a.find()) {
            String suffix = a.group(2) == null ? "" : " " + a.group(2).toUpperCase();
            labels.add("RX " + a.group(1) + suffix);
            order = 200_000L + Long.parseLong(a.group(1)) * 10 + rank(a.group(2));
        }
        Matcher i = INTEL.matcher(text);
        while (i.find()) {
            labels.add("Arc " + i.group(1).toUpperCase() + i.group(2));
            order = 300_000L + Long.parseLong(i.group(2)) * 10;
        }
        if (labels.size() != 1) {
            return Optional.empty();
        }
        String label = labels.iterator().next();
        return Optional.of(new ParsedValue(label.toLowerCase(), label, label, BigDecimal.valueOf(order)));
    }

    private static String suffixLabel(String suffix) {
        return switch (suffix.replaceAll("\\s+", " ")) {
            case "ti super" -> "Ti Super";
            case "ti" -> "Ti";
            case "super" -> "Super";
            default -> suffix;
        };
    }

    private static int rank(String suffix) {
        if (suffix == null) {
            return 0;
        }
        return switch (suffix.replaceAll("\\s+", " ")) {
            case "super", "gre" -> 1;
            case "ti", "xt" -> 2;
            case "ti super", "xtx" -> 3;
            default -> 0;
        };
    }
}
