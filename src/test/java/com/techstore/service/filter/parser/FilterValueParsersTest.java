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
                "200 (Hz)              | 200",
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
                "16G             | 16",
                "8G (1x8GB)      | 8",
                "2X32G           | 64",
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
                "2.560 x 1.600          | 2560x1600",
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

    @Nested
    @DisplayName("COUNT")
    class Count {
        private final CountParser parser = new CountParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "8                                                  | 8",
                "Total Cores: 14;# of Performance-cores: 6         | 14",
                "4 x DIMM                                           | 4",
                "16 (8P+8E)                                         | 16",
        })
        void readsTheFirstWholeNumber(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "Dual channel", "1.5", "0"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("FREQ_GHZ")
    class FrequencyGhz {
        private final FrequencyGhzParser parser = new FrequencyGhzParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "5.3 GHz                                  | 5.3",
                "5.70 Ghz                                 | 5.7",
                "Base Clock:3.6GHz; Max Boost Clock:4GHz  | 4",
                "AMD RYZEN 9 9900X 4.4G 64M BOX           | 4.4",
                "I5-14600K 5.3GHZ 20MB BOX 1700           | 5.3",
                "4400 MHz                                 | 4.4",
        })
        void readsTheHighestClock(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "AMD RYZEN 7 7800X3D BOX", "16GB", "20 GHz"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("MEM_MHZ")
    class MemoryMhz {
        private final MemoryMhzParser parser = new MemoryMhzParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "6000 MHz                                   | 6000",
                "6000MT/s                                   | 6000",
                "3200                                       | 3200",
                "DDR5-6000                                  | 6000",
                "KINGSTON FURY 32GB (2x16GB) DDR5 6000 CL30 | 6000",
                "3200MHz(PC4-25600)/5600MHz(PC5-44800)      | 5600",
                "LPDDR5X-7500                               | 7500",
        })
        void readsTheSpeed(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "DDR5", "16 GB", "25600"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("CAS_CL")
    class CasLatency {
        private final CasLatencyParser parser = new CasLatencyParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "CL16            | 16",
                "CL 16-20-20     | 16",
                "CL16-18-18-38   | 16",
                "16-18-18-38     | 16",
                "DDR5 6000 CL30  | 30",
                "22              | 22",
        })
        void readsTheFirstTiming(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @Test
        void labelsWithCl() {
            assertEquals("CL30", parser.parse("CL30", null).orElseThrow().labelBg());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "DDR5", "6000 MHz"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("GPU_MODEL")
    class GpuModel {
        private final GpuModelParser parser = new GpuModelParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "NVIDIA GeForce RTX 5060 Ti 8GB    | rtx 5060 ti",
                "GeForce RTX5060TI                 | rtx 5060 ti",
                "RTX 4070 Ti SUPER                 | rtx 4070 ti super",
                "GeForce RTX 4070 SUPER            | rtx 4070 super",
                "Nvidia GeForce GT 1030            | gt 1030",
                "GTX 1650                          | gtx 1650",
                "AMD Radeon RX 7900 XTX            | rx 7900 xtx",
                "Radeon RX 9070 XT                 | rx 9070 xt",
                "AMD Radeon RX 7600                | rx 7600",
                "Intel Arc B580                    | arc b580",
                "GIGABYTE RTX 5060 GAMING OC 8G    | rtx 5060",
                "NVIDIA® GeForce RTX™ 5060         | rtx 5060",
        })
        void readsTheChipModel(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @Test
        void labelsInShopNotation() {
            assertEquals("RTX 4070 Ti Super", parser.parse("rtx 4070 ti super", null).orElseThrow().labelBg());
            assertEquals("RX 7900 XTX", parser.parse("RX 7900 XTX", null).orElseThrow().labelBg());
        }

        @Test
        void ordersNvidiaBeforeAmdAndByNumber() {
            java.math.BigDecimal rtx3050 = parser.parse("RTX 3050", null).orElseThrow().number();
            java.math.BigDecimal rtx5060 = parser.parse("RTX 5060", null).orElseThrow().number();
            java.math.BigDecimal rx7600 = parser.parse("RX 7600", null).orElseThrow().number();
            assertTrue(rtx3050.compareTo(rtx5060) < 0);
            assertTrue(rtx5060.compareTo(rx7600) < 0);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "AMD Radeon Graphics", "RTX 4060 / RTX 4070"})
        void leavesAmbiguousTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Test
    void everyParserHasADistinctCode() {
        assertEquals(10, java.util.stream.Stream.of(new DiagonalInchParser(), new RefreshHzParser(),
                new CapacityGbParser(), new ResolutionParser(), new ResponseMsParser(), new CountParser(),
                new FrequencyGhzParser(), new MemoryMhzParser(), new CasLatencyParser(), new GpuModelParser())
                .map(FilterValueParser::code).distinct().count());
        assertTrue(Optional.ofNullable(new DiagonalInchParser().code()).isPresent());
    }
}
