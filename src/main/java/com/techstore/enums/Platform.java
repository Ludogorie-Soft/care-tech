package com.techstore.enums;

/**
 * Platform/Source of data integration
 * Represents which external system the data comes from
 */
public enum Platform {
    VALI("VALI", "VALI"),
    TEKRA("Tekra", "Tekra"),
    ASBIS("Asbis", "Asbis");

    private final String displayName;
    private final String code;

    Platform(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    /**
     * Get platform by code (case-insensitive)
     */
    public static Platform fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        for (Platform platform : values()) {
            if (platform.code.equalsIgnoreCase(code.trim())) {
                return platform;
            }
        }

        throw new IllegalArgumentException("Unknown platform code: " + code);
    }

    /**
     * Check if this is an external platform (not VALI)
     */
    public boolean isExternal() {
        return this != VALI;
    }
}