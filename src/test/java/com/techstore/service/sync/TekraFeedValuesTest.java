package com.techstore.service.sync;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * One TEKRA property can hold several values; each has to become its own option. Before, a repeated
 * tag kept only its last value, "<br/>"-joined values stayed one option, and anything over 200
 * characters was dropped.
 */
class TekraFeedValuesTest {

    @Test
    @DisplayName("a single value passes through trimmed")
    void singleValue() {
        assertEquals(List.of("IP67"), TekraFeedValues.values("  IP67 ", null));
    }

    @Test
    @DisplayName("every occurrence of a repeated tag is a value, duplicates once")
    void repeatedTag() {
        TekraFeedValues.Stats stats = new TekraFeedValues.Stats();
        assertEquals(List.of("Бял", "Черен"), TekraFeedValues.values(List.of("Бял", "Черен", "Бял"), stats));
        assertEquals(1, stats.repeatedTags);
        assertEquals(1, stats.multiValueProperties);
    }

    @Test
    @DisplayName("values separated by <br/> in any spelling are split")
    void lineBreaks() {
        assertEquals(List.of("IP55", "IK07"), TekraFeedValues.values("IP55<br/>IK07", null));
        assertEquals(List.of("Remote Control", "Touch Control"),
                TekraFeedValues.values("Remote Control <BR />Touch Control", null));
        assertEquals(List.of("a", "b"), TekraFeedValues.values("a<br>b<br/>", null));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Взривозащитен,Взривозащитен | Взривозащитен",
            "IP66, IK10,IP66, IK10       | IP66, IK10",
            "IP65, IK08                  | IP65, IK08",
            "2,2                         | 2,2",
            "2.4 GHz: 300 Mbps, 5 GHz    | 2.4 GHz: 300 Mbps, 5 GHz"
    })
    @DisplayName("text joined to itself collapses; decimals and real lists stay")
    void collapseDoubled(String raw, String expected) {
        assertEquals(expected, TekraFeedValues.collapseDoubled(raw));
    }

    @Test
    @DisplayName("long descriptive values are kept up to 2000 characters")
    void lengthCap() {
        TekraFeedValues.Stats stats = new TekraFeedValues.Stats();
        String kept = "x".repeat(TekraFeedValues.MAX_VALUE_LENGTH);
        String dropped = "y".repeat(TekraFeedValues.MAX_VALUE_LENGTH + 1);

        assertEquals(List.of(kept), TekraFeedValues.values(kept, stats));
        assertEquals(List.of(), TekraFeedValues.values(dropped, stats));
        assertEquals(1, stats.tooLongValues);
    }

    @Test
    @DisplayName("empty, placeholder and link values are dropped")
    void junk() {
        assertEquals(List.of(), TekraFeedValues.values(" ", null));
        assertEquals(List.of(), TekraFeedValues.values("null", null));
        assertEquals(List.of(), TekraFeedValues.values("-", null));
        assertEquals(List.of(), TekraFeedValues.values("https://tekra.bg/x.pdf", null));
        assertEquals(List.of(), TekraFeedValues.values(42, null));
    }
}
