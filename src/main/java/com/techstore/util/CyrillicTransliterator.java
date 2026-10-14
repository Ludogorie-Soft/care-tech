package com.techstore.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns a Cyrillic search word into the Latin form the catalogue actually stores.
 *
 * <p>Customers type brand names in Cyrillic — "леново", "гейминг" — while product names,
 * models and manufacturers are almost entirely Latin. Measured against production before
 * this existed: "леново" returned nothing against 95 for "lenovo", and "гейминг" returned
 * 16 against 729 for "gaming".
 *
 * <p>Two steps, in order:
 * <ol>
 *   <li>{@link #ALIASES} for words whose Bulgarian spelling is phonetic rather than a
 *       transliteration. Rules alone give "сони", "филипс" and "логитек" as "soni",
 *       "filips" and "logitek", none of which match Sony, Philips or Logitech.</li>
 *   <li>otherwise the official Bulgarian transliteration, which handles the regular
 *       majority on its own: леново → lenovo, асус → asus, самсунг → samsung,
 *       тошиба → toshiba, интел → intel.</li>
 * </ol>
 *
 * <p>Note this is transliteration, not translation. Bulgarian common nouns ("мишка",
 * "клавиатура", "принтер") already match through the category name, so they are
 * deliberately absent here.
 */
public final class CyrillicTransliterator {

    private CyrillicTransliterator() {
    }

    /**
     * Words the rules get wrong. Keyed by the Bulgarian spelling a customer would type.
     * Cheap to extend — add a row when a brand turns up in the logs with no results.
     */
    private static final Map<String, String> ALIASES = Map.ofEntries(
            // brands whose Bulgarian spelling is phonetic
            Map.entry("сони", "sony"),
            Map.entry("филипс", "philips"),
            Map.entry("логитек", "logitech"),
            Map.entry("логитех", "logitech"),
            Map.entry("хуавей", "huawei"),
            Map.entry("ксиаоми", "xiaomi"),
            Map.entry("сяоми", "xiaomi"),
            Map.entry("епъл", "apple"),
            Map.entry("ейсър", "acer"),
            Map.entry("ейсер", "acer"),
            Map.entry("канон", "canon"),
            Map.entry("каньон", "canyon"),
            Map.entry("кингстън", "kingston"),
            Map.entry("кингстон", "kingston"),
            Map.entry("гигабайт", "gigabyte"),
            Map.entry("арктик", "arctic"),
            Map.entry("корсар", "corsair"),
            Map.entry("корсеър", "corsair"),
            Map.entry("криейтив", "creative"),
            Map.entry("майкрософт", "microsoft"),
            Map.entry("сийгейт", "seagate"),
            Map.entry("нокия", "nokia"),
            Map.entry("делл", "dell"),
            Map.entry("самсунг", "samsung"),
            Map.entry("сандиск", "sandisk"),
            Map.entry("кеничрон", "keychron"),
            Map.entry("убиквити", "ubiquiti"),
            Map.entry("интензо", "intenso"),
            Map.entry("едифаер", "edifier"),
            // common Latin tech terms customers write in Cyrillic
            Map.entry("гейминг", "gaming"),
            Map.entry("геймърска", "gaming"),
            Map.entry("геймърски", "gaming"),
            Map.entry("уайрлес", "wireless"),
            Map.entry("безжична", "wireless"),
            Map.entry("безжичен", "wireless"),
            Map.entry("блутут", "bluetooth"),
            Map.entry("суич", "switch"),
            Map.entry("ъпгрейд", "upgrade")
    );

    /** Digraphs first — otherwise 'щ' would be handled by the single-character pass as 'sht'. */
    private static final Map<Character, String> DIGRAPHS = new HashMap<>();

    static {
        DIGRAPHS.put('щ', "sht");
        DIGRAPHS.put('ш', "sh");
        DIGRAPHS.put('ч', "ch");
        DIGRAPHS.put('ц', "ts");
        DIGRAPHS.put('ю', "yu");
        DIGRAPHS.put('я', "ya");
        DIGRAPHS.put('ж', "zh");
    }

    private static final String CYRILLIC = "абвгдезийклмнопрстуфхъь";
    private static final String LATIN = "abvgdeziyklmnoprstufhay";

    /**
     * @return the Latin form to search for in addition to the original word, or {@code null}
     *         when the word holds no Cyrillic and transliterating it would be pointless.
     */
    public static String transliterate(String word) {
        if (word == null || word.isBlank() || !containsCyrillic(word)) {
            return null;
        }

        String lower = word.toLowerCase();
        String alias = ALIASES.get(lower);
        if (alias != null) {
            return alias;
        }

        StringBuilder out = new StringBuilder(lower.length());
        for (char c : lower.toCharArray()) {
            String digraph = DIGRAPHS.get(c);
            if (digraph != null) {
                out.append(digraph);
                continue;
            }
            int idx = CYRILLIC.indexOf(c);
            out.append(idx >= 0 ? LATIN.charAt(idx) : c);
        }
        return out.toString();
    }

    public static boolean containsCyrillic(String value) {
        for (char c : value.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                return true;
            }
        }
        return false;
    }
}
