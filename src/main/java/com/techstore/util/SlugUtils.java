package com.techstore.util;

import java.util.Map;

public final class SlugUtils {

    private SlugUtils() {}

    private static final Map<String, String> CYRILLIC_TO_LATIN = Map.ofEntries(
            Map.entry("а","a"), Map.entry("А","a"), Map.entry("б","b"), Map.entry("Б","b"),
            Map.entry("в","v"), Map.entry("В","v"), Map.entry("г","g"), Map.entry("Г","g"),
            Map.entry("д","d"), Map.entry("Д","d"), Map.entry("е","e"), Map.entry("Е","e"),
            Map.entry("ж","zh"),Map.entry("Ж","zh"),Map.entry("з","z"), Map.entry("З","z"),
            Map.entry("и","i"), Map.entry("И","i"), Map.entry("й","y"), Map.entry("Й","y"),
            Map.entry("к","k"), Map.entry("К","k"), Map.entry("л","l"), Map.entry("Л","l"),
            Map.entry("м","m"), Map.entry("М","m"), Map.entry("н","n"), Map.entry("Н","n"),
            Map.entry("о","o"), Map.entry("О","o"), Map.entry("п","p"), Map.entry("П","p"),
            Map.entry("р","r"), Map.entry("Р","r"), Map.entry("с","s"), Map.entry("С","s"),
            Map.entry("т","t"), Map.entry("Т","t"), Map.entry("у","u"), Map.entry("У","u"),
            Map.entry("ф","f"), Map.entry("Ф","f"), Map.entry("х","h"), Map.entry("Х","h"),
            Map.entry("ц","ts"),Map.entry("Ц","ts"),Map.entry("ч","ch"),Map.entry("Ч","ch"),
            Map.entry("ш","sh"),Map.entry("Ш","sh"),Map.entry("щ","sht"),Map.entry("Щ","sht"),
            Map.entry("ъ","a"), Map.entry("Ъ","a"), Map.entry("ь","y"), Map.entry("Ь","y"),
            Map.entry("ю","yu"),Map.entry("Ю","yu"),Map.entry("я","ya"),Map.entry("Я","ya")
    );

    public static String generateSlug(String text) {
        if (text == null || text.isBlank()) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            if (CYRILLIC_TO_LATIN.containsKey(ch)) {
                result.append(CYRILLIC_TO_LATIN.get(ch));
            } else if (Character.isLetterOrDigit(text.charAt(i))) {
                result.append(Character.toLowerCase(text.charAt(i)));
            } else if (Character.isWhitespace(text.charAt(i)) || text.charAt(i) == '-') {
                result.append('-');
            }
        }
        return result.toString().replaceAll("-+", "-").replaceAll("^-+|-+$", "");
    }
}
