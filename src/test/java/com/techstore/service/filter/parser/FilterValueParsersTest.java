package com.techstore.service.filter.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The formats below are taken from the raw option texts of the four suppliers.
 */
class FilterValueParsersTest {

    private static String key(FilterValueParser parser, String text) {
        return parser.parse(text, null).map(ParsedValue::normKey).orElse(null);
    }

    @Nested
    @DisplayName("DIAGONAL_INCH")
    class Diagonal {
        private final DiagonalInchParser parser = new DiagonalInchParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "27\"                 | 27",
                "27 \"                | 27",
                "27''                 | 27",
                "23.8\"               | 23.8",
                "23,8 инча            | 23.8",
                "27 inch              | 27",
                "68.6 cm              | 27",
                "68,6 см              | 27",
                "60.5 cm              | 23.8",
                "27\" (68.6 cm)       | 27",
                "15.6                 | 15.6",
                "34″                  | 34",
                "15.6-inch            | 15.6",
                "14 (inch)            | 14",
                "15.60 (inch)         | 15.6",
                "15.6 in              | 15.6",
                "16 ``                | 16",
        })
        void readsInchesFromEveryNotation(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @Test
        void labelsWithAnInchMark() {
            assertEquals("27\"", parser.parse("68.6 cm", null).orElseThrow().labelBg());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "IPS", "24\" / 27\"", "500 cm"})
        void leavesAmbiguousOrImplausibleTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }

        @Test
        void fallsBackToTheEnglishText() {
            assertEquals("27", parser.parse(null, "27 inch").map(ParsedValue::normKey).orElse(null));
        }
    }

    @Nested
    @DisplayName("REFRESH_HZ")
    class Refresh {
        private final RefreshHzParser parser = new RefreshHzParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "144 Hz                | 144",
                "144Hz                 | 144",
                "165 Hz (OC)           | 165",
                "60 Hz / 75 Hz (OC)    | 75",
                "240 херца             | 240",
                "75                    | 75",
        })
        void readsTheHighestRate(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "1 ms", "5000 Hz"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("CAPACITY_GB")
    class Capacity {
        private final CapacityGbParser parser = new CapacityGbParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "16 GB           | 16",
                "16GB            | 16",
                "2x8GB           | 16",
                "2 x 16 GB       | 32",
                "16GB (2x8GB)    | 16",
                "1 TB            | 1000",
                "1TB             | 1000",
                "2048 GB         | 2000",
                "512 GB          | 512",
                "512 MB          | 0.5",
        })
        void readsCapacityInGigabytes(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @Test
        void labelsTerabytesAndMegabytes() {
            assertEquals("1 TB", parser.parse("1000 GB", null).orElseThrow().labelBg());
            assertEquals("2 TB", parser.parse("2048GB", null).orElseThrow().labelBg());
            assertEquals("512 MB", parser.parse("512 MB", null).orElseThrow().labelBg());
            assertEquals("16 GB", parser.parse("16GB", null).orElseThrow().labelBg());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "DDR5", "8GB / 16GB"})
        void leavesAmbiguousTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("RESOLUTION")
    class Resolution {
        private final ResolutionParser parser = new ResolutionParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "1920x1080              | 1920x1080",
                "1920 x 1080            | 1920x1080",
                "1920×1080              | 1920x1080",
                "1920х1080              | 1920x1080",
                "FHD (1920x1080)        | 1920x1080",
                "3840 x 2160 (4K UHD)   | 3840x2160",
                "Full HD                | 1920x1080",
                "4K UHD                 | 3840x2160",
                "WQHD                   | 2560x1440",
                "UWQHD                  | 3440x1440",
        })
        void readsWidthByHeight(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "HD", "1920x1080 / 2560x1440"})
        void leavesAmbiguousTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("RESPONSE_MS")
    class Response {
        private final ResponseMsParser parser = new ResponseMsParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "1 ms                  | 1",
                "1 ms (GTG)            | 1",
                "1ms MPRT              | 1",
                "0.5 мс                | 0.5",
                "0.5ms(GTG)            | 0.5",
                "0.03 ms (GTG)         | 0.03",
                "5 ms (GTG), 1 ms MPRT | 1",
                "8 мс                  | 8",
        })
        void readsTheLowestTime(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "Fast", "144 Hz", "100 ms"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Test
    void everyParserHasADistinctCode() {
        assertEquals(5, java.util.stream.Stream.of(new DiagonalInchParser(), new RefreshHzParser(),
                new CapacityGbParser(), new ResolutionParser(), new ResponseMsParser())
                .map(FilterValueParser::code).distinct().count());
        assertTrue(Optional.ofNullable(new DiagonalInchParser().code()).isPresent());
    }
}
