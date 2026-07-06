package com.techstore.dto.pazaruvaj;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PazaruvajFeedConfig {

    /** ALL | CATEGORY | PRODUCTS */
    private String type = "ALL";

    /** Used when type = CATEGORY */
    private Long categoryId;
    private String categoryName;

    /** Used when type = PRODUCTS */
    private List<ConfigProduct> products = new ArrayList<>();

    @Data
    public static class ConfigProduct {
        private Long id;
        private String name;
    }
}
