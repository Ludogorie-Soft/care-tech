package com.techstore.service;

import com.techstore.entity.Order;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formatting helpers for the customer emails. Kept free of Spring and Thymeleaf so they can be
 * unit-tested on their own; the templates only print what these return.
 */
final class EmailFormatting {

    static final String SPEEDY_TRACKING_URL = "https://www.speedy.bg/public/index.php/bg/track-shipment";

    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"']+");
    /** Punctuation that ends a sentence rather than the URL ("…review." or "(…review)"). */
    private static final String URL_TRAILING_PUNCTUATION = ".,;:!?)]}»”\"'";
    private static final String LINK_STYLE = "color:#ffffff; text-decoration:underline;";

    private EmailFormatting() {
    }

    /**
     * Turns the admin's plain-text note into safe HTML: everything is escaped, http(s) links become
     * clickable and line breaks are kept. Returns null for a blank note so the template can hide the block.
     */
    static String noteHtml(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String text = note.strip().replace("\r\n", "\n").replace('\r', '\n');

        // Find links in the raw text, then escape text and links separately: matching on
        // already-escaped text would pull entities such as &quot; into the URL.
        StringBuilder html = new StringBuilder();
        Matcher matcher = URL.matcher(text);
        int position = 0;
        while (matcher.find()) {
            String url = stripTrailingPunctuation(matcher.group());
            int urlEnd = matcher.start() + url.length();
            html.append(escape(text.substring(position, matcher.start())));
            String escapedUrl = escape(url);
            html.append("<a href=\"").append(escapedUrl).append("\" style=\"").append(LINK_STYLE).append("\">")
                    .append(escapedUrl).append("</a>");
            position = urlEnd;
        }
        html.append(escape(text.substring(position)));
        return html.toString().replace("\n", "<br/>");
    }

    static boolean toSpeedyOffice(Order order) {
        return Boolean.TRUE.equals(order.getIsToSpeedyOffice());
    }

    /**
     * Where the parcel goes, as lines for the email: the Speedy office (or the street address) and
     * then the town. Speedy office names often already contain the town and postal code, and the
     * stored town often already contains the postal code, so repeats are dropped.
     */
    static List<String> deliveryLines(Order order) {
        String place = toSpeedyOffice(order)
                ? firstNonBlank(order.getShippingSpeedyOfficeName(), order.getShippingAddress())
                : trimToNull(order.getShippingAddress());
        String town = firstNonBlank(order.getShippingCity(), order.getShippingSpeedySiteName());
        String postalCode = trimToNull(order.getShippingPostalCode());

        String townLine = town;
        if (postalCode != null && (town == null || !town.contains(postalCode))) {
            townLine = town == null ? postalCode : town + ", " + postalCode;
        }

        List<String> lines = new ArrayList<>();
        if (place != null) {
            lines.add(place);
        }
        if (townLine != null && !alreadyNamesTown(place, town, postalCode, townLine)) {
            if (place != null && normalize(townLine).contains(normalize(place))) {
                // The town line is the fuller version of the same text — keep only that one
                lines.clear();
            }
            lines.add(townLine);
        }
        return lines;
    }

    /** Speedy's tracking page, with the shipment number filled in when there is one. */
    static String trackingUrl(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return SPEEDY_TRACKING_URL;
        }
        return SPEEDY_TRACKING_URL + "?shipmentNumber="
                + URLEncoder.encode(trackingNumber.trim(), StandardCharsets.UTF_8);
    }

    static String deliveryDaysText(int days) {
        return days <= 1 ? "до 1 работен ден" : "до " + days + " работни дни";
    }

    private static boolean alreadyNamesTown(String place, String town, String postalCode, String townLine) {
        if (place == null) {
            return false;
        }
        String normalizedPlace = normalize(place);
        if (normalizedPlace.contains(normalize(townLine))) {
            return true;
        }
        // "гр. Сливен [8800] пл. Васил Левски No 11" already says what "гр. Сливен, Сливен, 8800" does
        if (town != null && postalCode != null && normalizedPlace.contains(postalCode)) {
            String townName = town.split(",")[0];
            return normalizedPlace.contains(normalize(townName));
        }
        return false;
    }

    private static String stripTrailingPunctuation(String url) {
        int end = url.length();
        while (end > 0 && URL_TRAILING_PUNCTUATION.indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
    }

    private static String escape(String text) {
        return HtmlUtils.htmlEscape(text, StandardCharsets.UTF_8.name());
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").replaceAll("[\\s,.]+$", "").strip();
    }

    private static String firstNonBlank(String first, String second) {
        String value = trimToNull(first);
        return value != null ? value : trimToNull(second);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
