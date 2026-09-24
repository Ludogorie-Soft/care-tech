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
                "6+1                                                | 7",
                "2+1                                                | 3",
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

    @Nested
    @DisplayName("POWER_W")
    class PowerWatt {
        private final PowerWattParser parser = new PowerWattParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "850 W                                         | 850",
                "1300W, Power Good Signal: 100-150ms           | 1300",
                "750W : ATX 3.1                                | 750",
                "SEASONIC FOCUS GX-850 (2024) 850W 80+ Gold    | 850",
        })
        void readsTheRatedPower(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "80+ Gold", "50W", "38Wh"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("FAN_MM")
    class FanSize {
        private final FanSizeParser parser = new FanSizeParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "120 x 120 x 25 mm            | 120",
                "140 x 140 x 25 mm            | 140",
                "120 mm                       | 120",
                "1  x 120мм*120мм*25мм        | 120",
                "ARCTIC P12 PWM PST 120mm     | 120",
                "DeepCool LE520, 240mm CPU Liquid Cooler, 2x120mm ARGB PWM Fans | 120",
        })
        void readsTheFanSize(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "4-Pin (PWM)", "25 mm", "CORSAIR NAUTILUS 240, 240mm Radiator"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("DPI_MAX")
    class Dpi {
        private final DpiParser parser = new DpiParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "1000/1400/1800 DPI     | 1800",
                "100~10,000 CPI         | 10000",
                "26000 dpi              | 26000",
                "1000;1600;2400         | 2400",
                "26K DPI                | 26000",
        })
        void readsTheHighestSensitivity(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "Оптичен", "5"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("LENGTH_MM")
    class LengthMm {
        private final LengthMmParser parser = new LengthMmParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "до 410 mm     | 410",
                "410 мм        | 410",
                "164.5 mm      | 165",
                "до 165        | 165",
        })
        void readsTheClearance(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "ATX", "5 mm"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("PAGE_YIELD")
    class PageYield {
        private final PageYieldParser parser = new PageYieldParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "4000                                          | from-3000",
                "Up to 10 200 pages                            | from-10000",
                "Up to 2 500 pages (ISO/IEC 19752)             | from-1000",
                "3,100 pages                                   | from-3000",
                "519 Pages                                     | from-500",
                "200                                           | from-0",
                "15 ml, Up to 400 pages of A4 documents        | from-0",
                "400 pages (А4) / 4973 photos (10x15cm)        | from-0",
                "Тонер касета Ricoh IM C300, 17000 копия, IMC300, Черен | from-10000",
                "Тонер касета UPRINT CF230X, HP LJ Pro M203/M227, 3500 k, Черен | from-3000",
                "HP Color Pro M255/ Pro MFP M282/ 283, 3150k, Black | from-3000",
                "CANON 708 BLACK 2.5K                          | from-1000",
                "CANON 710H (12K)                              | from-10000",
        })
        void readsTheYieldRange(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "9 ml", "5.6 ml", "CANON 729 CYAN", "Тонер касета UPRINT MLT-D116L, SAMSUNG, Черен",
                "CANON CLI-551XL CYAN"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Test
    void everyParserHasADistinctCode() {
        assertEquals(15, java.util.stream.Stream.of(new DiagonalInchParser(), new RefreshHzParser(),
                new CapacityGbParser(), new ResolutionParser(), new ResponseMsParser(), new CountParser(),
                new FrequencyGhzParser(), new MemoryMhzParser(), new CasLatencyParser(), new GpuModelParser(),
                new PowerWattParser(), new FanSizeParser(), new DpiParser(), new LengthMmParser(),
                new PageYieldParser())
                .map(FilterValueParser::code).distinct().count());
        assertTrue(Optional.ofNullable(new DiagonalInchParser().code()).isPresent());
    }
}
