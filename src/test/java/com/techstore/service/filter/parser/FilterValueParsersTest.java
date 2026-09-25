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
                "610 Hz                | 610",
                "1000 Hz               | 1000",
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
                "128             | 128",
                "1000            | 1000",
                "Kingston 512GB microSDXC Canvas Select Plus Gen3 150MB/s A1 Card + Adapter | 512",
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
        @ValueSource(strings = {"", "DDR5", "8GB / 16GB", "0", "0 GB", "Kingston DataTraveler 3.0"})
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
                "2.8K                   | 2880x1800",
                "2.8K OLED              | 2880x1800",
                "2.5K                   | 2560x1600",
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
                "3200Mbps                                   | 3200",
                "5600 Mbps                                  | 5600",
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
                "NVIDIA GeForce RTXTM 4050 with 6 GB | rtx 4050",
                "NVIDIA GeForce 3060 Ti            | rtx 3060 ti",
                "GeForce 210                       | geforce 210",
                "NVIDIA RTX A1000                  | rtx a1000",
                "NVIDIA GeForce RTX PRO 6000       | rtx pro 6000",
                "Nvidia RTX PRO 1000 Blackwell Laptop GPU 8GB GDDR7 | rtx pro 1000",
                "Intel Arc Pro B60 GPU             | arc pro b60",
                "AMD Radeon AI PRO R9700           | radeon ai pro r9700",
                "AMD Radeon PRO W7900              | radeon pro w7900",
        })
        void readsTheChipModel(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @Test
        void labelsInShopNotation() {
            assertEquals("RTX 4070 Ti Super", parser.parse("rtx 4070 ti super", null).orElseThrow().labelBg());
            assertEquals("RX 7900 XTX", parser.parse("RX 7900 XTX", null).orElseThrow().labelBg());
            assertEquals("RTX PRO 6000", parser.parse("NVIDIA RTX PRO 6000", null).orElseThrow().labelBg());
            assertEquals("Arc Pro B60", parser.parse("intel arc pro b60", null).orElseThrow().labelBg());
            assertEquals("Radeon AI PRO R9700", parser.parse("AMD Radeon AI PRO R9700", null).orElseThrow().labelBg());
            assertEquals("GTX 1660", parser.parse("GeForce 1660", null).orElseThrow().labelBg());
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
                "3600 VA / 3600 W                              | 3600",
                "10000 VA / 10000 W                            | 10000",
                "1200VA / 840W                                 | 840",
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
                "44 000 dpi             | 44000",
                "16 000 DPI             | 16000",
                "400 800 1600 dpi       | 1600",
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

    @Nested
    @DisplayName("READ_MBS")
    class ReadSpeed {
        private final ReadSpeedParser parser = new ReadSpeedParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "До 150 Mb/s                                   | from-101",
                "70 Mb/s                                       | from-0",
                "1000 Mb/s                                     | from-501",
                "200MB/s read, 60MB/s write                    | from-101",
                "Read 100MB/s;Write N/A                        | from-0",
                "Up to : 90MB/s (Read) ; 40MB/s (Write)        | from-0",
                "Read up to 1,050MB;Write up to 1,000MB        | from-1001",
                "2,000MB/s read, 2,000MB/s write               | from-1001",
                "Write 40MB/s, Read 400MB/s                    | from-201",
                "150 MB/sec                                    | from-101",
                "до 190                                        | from-101",
                "17~20                                         | from-0",
                "150MB/s read, UHS-I speed class, U3, V30      | from-101",
                "Kingston 512GB microSDXC Canvas Select Plus Gen3 150MB/s A1 Card + Adapter | from-101",
                "SanDisk 8Tb Extreme Pro Portable 1050 Mb.s read/write, USB 3.2 Gen2,IP55 | from-1001",
        })
        void readsTheSpeedRange(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "UHS-I", "10 Gbps", "USB 3.2 Gen 2", "Карта памет TEAM micro SDHC, 16GB"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("WIFI_GEN")
    class WifiStandard {
        private final WifiStandardParser parser = new WifiStandardParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "802.11 a/b/g/n/ac                             | wifi5",
                "802.11 a/b/g/n/ac/ax                          | wifi6",
                "802.11 a/b/g/n/ac/ax/be                       | wifi7",
                "802.11 b/g/n                                  | wifi4",
                "IEEE 802.11ac/a/n @5GHz;IEEE 802.11b/g/n @2.4GHz | wifi5",
                "IEEE 802.11ax 6GHz;IEEE 802.11ac/a/n/ax 5GHz;IEEE 802.11b/g/n/ax 2.4GHz | wifi6e",
                "WiFi 7                                        | wifi7",
                "Безжичен рутер TP-Link Archer C50 AC1200, 2.4/5 GHz, 300 - 867 Mbps | wifi5",
                "TENDA TX12L PRO AX3000 WIFI6                  | wifi6",
                "TENDA TE3L BE3600 GB WI-FI 7                  | wifi7",
                "UBIQUITI UniFi U6 Enterprise, Access Point, WiFi 6E | wifi6e",
                "Мрежова карта Intel Wi-Fi 6E AX211 Gig+2230 2x2 AX R2 6GHz+ AX211.NGWG | wifi6e",
                "TP-Link TL-WR840N N300                        | wifi4",
        })
        void readsTheNewestGeneration(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "2.4 GHz", "TENDA NOVA MW3(2-PACK) MESH", "Суич D-Link DGS-1008D/E, 8 портов"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Nested
    @DisplayName("PORT_COUNT")
    class PortCount {
        private final PortCountParser parser = new PortCountParser();

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "16 x 10/100/1000M PoE                         | 16",
                "16 x RJ-45                                    | 16",
                "24x10/100/1000Base-T                          | 24",
                "24x 100/1000Mbps PoE; 4x Gigabit combo (RJ-45/SFP) | 24",
                "16 (RJ-45)                                    | 16",
                "Суич ZYXEL GS1100-16, 16 портов, Gigabit, за монтиране в шкаф | 16",
                "Суич D-Link GO-SW-5G, 5 портов 10/100/1000, Gigabit, Desktop | 5",
                "Imou 8-port Gigabit Switch                    | 8",
                "Суич 8-портов ZyXEL GS1920-8HPV2, Gigabit, управляем, PoE | 8",
        })
        void readsThePortCount(String text, String expected) {
            assertEquals(expected, key(parser, text));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "2× 1.25G SFP", "1 x 1000Base-LX SC port", "UBIQUITI UniFi Switch Pro 8 PoE",
                "Инжектор D-Link DPE-101GI, Gigabit, 15,4W"})
        void leavesOtherTextUnmapped(String text) {
            assertTrue(parser.parse(text, null).isEmpty());
        }
    }

    @Test
    void everyParserHasADistinctCode() {
        assertEquals(18, java.util.stream.Stream.of(new DiagonalInchParser(), new RefreshHzParser(),
                new CapacityGbParser(), new ResolutionParser(), new ResponseMsParser(), new CountParser(),
                new FrequencyGhzParser(), new MemoryMhzParser(), new CasLatencyParser(), new GpuModelParser(),
                new PowerWattParser(), new FanSizeParser(), new DpiParser(), new LengthMmParser(),
                new PageYieldParser(), new ReadSpeedParser(), new WifiStandardParser(), new PortCountParser())
                .map(FilterValueParser::code).distinct().count());
        assertTrue(Optional.ofNullable(new DiagonalInchParser().code()).isPresent());
    }
}
