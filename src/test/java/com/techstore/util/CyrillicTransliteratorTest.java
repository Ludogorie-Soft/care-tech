package com.techstore.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CyrillicTransliteratorTest {

    @ParameterizedTest
    @CsvSource({
            "леново,   lenovo",
            "асус,     asus",
            "самсунг,  samsung",
            "тошиба,   toshiba",
            "интел,    intel",
            "аорус,    aorus",
            "дахуа,    dahua"
    })
    @DisplayName("regular brands fall out of the transliteration rules")
    void transliteratesRegularBrands(String cyrillic, String expected) {
        assertEquals(expected, CyrillicTransliterator.transliterate(cyrillic));
    }

    @ParameterizedTest
    @CsvSource({
            "сони,     sony",
            "филипс,   philips",
            "логитек,  logitech",
            "хуавей,   huawei",
            "ксиаоми,  xiaomi",
            "гейминг,  gaming"
    })
    @DisplayName("phonetic spellings need the alias table — the rules would give soni, filips, logitek")
    void aliasesBeatTheRules(String cyrillic, String expected) {
        assertEquals(expected, CyrillicTransliterator.transliterate(cyrillic));
    }

    @ParameterizedTest
    @CsvSource({
            "щора,     shtora",
            "шина,     shina",
            "чанта,    chanta",
            "цена,     tsena",
            "юни,      yuni",
            "ябълка,   yabalka",
            "жица,     zhitsa"
    })
    @DisplayName("digraphs are applied before the single-character pass")
    void handlesDigraphs(String cyrillic, String expected) {
        assertEquals(expected, CyrillicTransliterator.transliterate(cyrillic));
    }

    @ParameterizedTest
    @ValueSource(strings = {"lenovo", "RTX5080", "15IAU7", "123"})
    @DisplayName("a Latin word returns null — searching for it twice would be pointless")
    void returnsNullWithoutCyrillic(String latin) {
        assertNull(CyrillicTransliterator.transliterate(latin));
    }

    @Test
    @DisplayName("null and blank input are tolerated")
    void toleratesEmptyInput() {
        assertNull(CyrillicTransliterator.transliterate(null));
        assertNull(CyrillicTransliterator.transliterate(""));
        assertNull(CyrillicTransliterator.transliterate("   "));
    }

    @Test
    @DisplayName("matching is case insensitive")
    void isCaseInsensitive() {
        assertEquals("lenovo", CyrillicTransliterator.transliterate("ЛЕНОВО"));
        assertEquals("gaming", CyrillicTransliterator.transliterate("Гейминг"));
    }

    @Test
    @DisplayName("mixed scripts keep the Latin characters as they are")
    void keepsLatinCharactersInMixedWords() {
        assertEquals("asus15", CyrillicTransliterator.transliterate("асус15"));
    }

    @Test
    void detectsCyrillic() {
        assertTrue(CyrillicTransliterator.containsCyrillic("лаптоп"));
        assertTrue(CyrillicTransliterator.containsCyrillic("asus монитор"));
        assertFalse(CyrillicTransliterator.containsCyrillic("gaming laptop"));
    }
}
