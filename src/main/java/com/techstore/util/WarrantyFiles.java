package com.techstore.util;

import java.util.Locale;

/**
 * An order's warranty card file. Its S3 key is a UUID with the original extension
 * ("warranties/3f2c….pdf"); the customer gets it as "garantsia-<order number>.<ext>", both from the
 * download in the profile and attached to the warranty email.
 */
public final class WarrantyFiles {

    private WarrantyFiles() {
    }

    /** Lower-case extension of the key, or "" when it has none. */
    public static String extension(String key) {
        String name = key.substring(key.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Stated rather than guessed from the name: Spring's mime.types has no entry for docx, xlsx, odt,
     * webp or heic, and gives ods the wrong type.
     */
    public static String contentType(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "odt" -> "application/vnd.oasis.opendocument.text";
            case "ods" -> "application/vnd.oasis.opendocument.spreadsheet";
            case "rtf" -> "application/rtf";
            case "txt" -> "text/plain";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "heic" -> "image/heic";
            default -> "application/octet-stream";
        };
    }

    public static String customerFileName(String orderNumber, String key) {
        String extension = extension(key);
        return "garantsia-" + orderNumber + (extension.isEmpty() ? "" : "." + extension);
    }
}
