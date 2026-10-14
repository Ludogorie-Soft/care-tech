package com.techstore.service.filter.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read speed of a flash drive, memory card or external SSD in MB/s, filed into a range
 * ("201 – 500 MB/s"): suppliers round differently ("до 150", "150MB/s", "Up to 150 MB/sec").
 * <p>
 * "200MB/s read, 60MB/s write" and "Read 100MB/s;Write N/A" are read at the part that says read. A
 * bare number ("до 100", "17~20") is read only when it is the whole text — the unit is then in the
 * parameter name. Gbps is the speed of the bus, not of the drive, and is not read.
 */
@Component
public class ReadSpeedParser implements FilterValueParser {

    private static final Pattern WITH_UNIT = Pattern.compile(
            "(?<![\\d.])(\\d{1,5})\\s*(?:mb/s|mb/sec|mb\\.s|mbps|мб/с|mb(?![a-zа-я]))");
    private static final Pattern BARE = Pattern.compile("^\\s*(?:до|up to)?\\s*(\\d{1,5})(?:\\s*[~-]\\s*(\\d{1,5}))?\\s*$");
    private static final Pattern READ = Pattern.compile("read|четене");

    private static final int MIN_MBS = 2;
    private static final int MAX_MBS = 20_000;

    private static final int[] LOWER = {0, 101, 201, 501, 1001};
    private static final String[] LABEL = {"До 100 MB/s", "101 – 200 MB/s", "201 – 500 MB/s",
            "501 – 1 000 MB/s", "Над 1 000 MB/s"};
    private static final String[] LABEL_EN = {"Up to 100 MB/s", "101 – 200 MB/s", "201 – 500 MB/s",
            "501 – 1,000 MB/s", "Over 1,000 MB/s"};

    @Override
    public String code() {
        return "READ_MBS";
    }

    @Override
    public Optional<ParsedValue> parse(String textBg, String textEn) {
        String text = ParserSupport.prepare(textBg, textEn);
        if (text == null) {
            return Optional.empty();
        }
        text = text.replaceAll("(\\d)[.,](\\d{3})(?!\\d)", "$1$2");

        Integer speed = null;
        if (READ.matcher(text).find()) {
            for (String part : text.split("[;,\\n]")) {
                Matcher m = WITH_UNIT.matcher(part);
                if (READ.matcher(part).find() && m.find()) {
                    speed = Integer.parseInt(m.group(1));
                    break;
                }
            }
        }
        if (speed == null) {
            Matcher m = WITH_UNIT.matcher(text);
            if (m.find()) {
                speed = Integer.parseInt(m.group(1));
            } else {
                Matcher bare = BARE.matcher(text);
                if (bare.find()) {
                    speed = Integer.parseInt(bare.group(bare.group(2) != null ? 2 : 1));
                }
            }
        }
        if (speed == null || speed < MIN_MBS || speed > MAX_MBS) {
            return Optional.empty();
        }
        int range = 0;
        while (range + 1 < LOWER.length && speed >= LOWER[range + 1]) {
            range++;
        }
        return Optional.of(new ParsedValue("from-" + LOWER[range], LABEL[range], LABEL_EN[range],
                BigDecimal.valueOf(LOWER[range])));
    }
}
