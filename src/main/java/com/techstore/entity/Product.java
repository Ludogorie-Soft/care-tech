package com.techstore.entity;

import com.techstore.enums.Platform;
import com.techstore.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Entity
@Table(name = "products")
@Data
@EqualsAndHashCode(callSuper = false)
public class Product extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    private Platform platform;

    @Column(name = "tekra_id")
    private String tekraId;

    private String sku;

    @Column(name = "asbis_id")
    private String asbisId;

    @Column(name = "asbis_code")
    private String asbisCode;

    @Column(name = "asbis_part_number")
    private String asbisPartNumber;

    @FullTextField
    @Column(name = "name_bg", columnDefinition = "TEXT")
    private String nameBg;

    @FullTextField
    @Column(name = "name_en", columnDefinition = "TEXT")
    private String nameEn;

    @FullTextField
    @Column(name = "description_bg", columnDefinition = "TEXT")
    private String descriptionBg;

    @FullTextField
    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(name = "workflow_id")
    private Long workflowId;

    @FullTextField
    @Column(name = "reference_number", unique = true)
    private String referenceNumber;

    @FullTextField
    @Column(name = "model")
    private String model;

    @Column(name = "barcode")
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    @IndexedEmbedded
    private Manufacturer manufacturer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProductStatus status;

    @Column(name = "price_client", precision = 10, scale = 2)
    private BigDecimal priceClient;

    @Column(name = "price_partner", precision = 10, scale = 2)
    private BigDecimal pricePartner;

    @Column(name = "price_promo", precision = 10, scale = 2)
    private BigDecimal pricePromo;

    @Column(name = "price_client_promo", precision = 10, scale = 2)
    private BigDecimal priceClientPromo;

    @Column(name = "markup_percentage", precision = 5, scale = 2)
    private BigDecimal markupPercentage = BigDecimal.valueOf(20.0);

    @Column(name = "final_price", precision = 10, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "show_flag")
    private Boolean show = true;

    private Integer warranty;

    @Column(precision = 8, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    private Boolean active = true;

    private Boolean featured = false;

    @Column(name = "image_url", length = 1000)
    private String primaryImageUrl;

    @ElementCollection
    @CollectionTable(name = "additional_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "additional_urls", length = 1000)
    private List<String> additionalImages = new ArrayList<>();

    private BigDecimal weight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<ProductParameter> productParameters = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ProductFlag> productFlags = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserFavorite> favorites = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CartItem> cartItems = new HashSet<>();

    private String slug;

    @PrePersist
    @PreUpdate
    public void generateSlug() {
        if (this.slug == null || this.slug.isEmpty()) {
            String nameSlug = transliterate(nameEn);

            if (nameEn == null || nameEn.isEmpty()) {
                nameSlug = transliterate(nameBg);
            }

            this.slug = String.format("%s", nameSlug);
        }
    }

    public void calculateFinalPrice() {
        if (priceClient == null || markupPercentage == null) {
            this.finalPrice = null;
            return;
        }

        BigDecimal markup = priceClient.multiply(markupPercentage.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        BigDecimal basePrice = priceClient.add(markup);

        if (discount != null && discount.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal discountPercent = discount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal discountAmount = basePrice.multiply(discountPercent);
            this.finalPrice = basePrice.add(discountAmount);
        } else {
            this.finalPrice = basePrice;
        }
    }

    public boolean isOnSale() {
        return discount != null && discount.compareTo(BigDecimal.ZERO) < 0;
    }

    private String transliterate(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        text = removeAllQuotes(text);

        Map<String, String> cyrillicToLatin = new HashMap<>();
        cyrillicToLatin.put("а", "a"); cyrillicToLatin.put("А", "a");
        cyrillicToLatin.put("б", "b"); cyrillicToLatin.put("Б", "b");
        cyrillicToLatin.put("в", "v"); cyrillicToLatin.put("В", "v");
        cyrillicToLatin.put("г", "g"); cyrillicToLatin.put("Г", "g");
        cyrillicToLatin.put("д", "d"); cyrillicToLatin.put("Д", "d");
        cyrillicToLatin.put("е", "e"); cyrillicToLatin.put("Е", "e");
        cyrillicToLatin.put("ж", "zh"); cyrillicToLatin.put("Ж", "zh");
        cyrillicToLatin.put("з", "z"); cyrillicToLatin.put("З", "z");
        cyrillicToLatin.put("и", "i"); cyrillicToLatin.put("И", "i");
        cyrillicToLatin.put("й", "y"); cyrillicToLatin.put("Й", "y");
        cyrillicToLatin.put("к", "k"); cyrillicToLatin.put("К", "k");
        cyrillicToLatin.put("л", "l"); cyrillicToLatin.put("Л", "l");
        cyrillicToLatin.put("м", "m"); cyrillicToLatin.put("М", "m");
        cyrillicToLatin.put("н", "n"); cyrillicToLatin.put("Н", "n");
        cyrillicToLatin.put("о", "o"); cyrillicToLatin.put("О", "o");
        cyrillicToLatin.put("п", "p"); cyrillicToLatin.put("П", "p");
        cyrillicToLatin.put("р", "r"); cyrillicToLatin.put("Р", "r");
        cyrillicToLatin.put("с", "s"); cyrillicToLatin.put("С", "s");
        cyrillicToLatin.put("т", "t"); cyrillicToLatin.put("Т", "t");
        cyrillicToLatin.put("у", "u"); cyrillicToLatin.put("У", "u");
        cyrillicToLatin.put("ф", "f"); cyrillicToLatin.put("Ф", "f");
        cyrillicToLatin.put("х", "h"); cyrillicToLatin.put("Х", "h");
        cyrillicToLatin.put("ц", "ts"); cyrillicToLatin.put("Ц", "ts");
        cyrillicToLatin.put("ч", "ch"); cyrillicToLatin.put("Ч", "ch");
        cyrillicToLatin.put("ш", "sh"); cyrillicToLatin.put("Ш", "sh");
        cyrillicToLatin.put("щ", "sht"); cyrillicToLatin.put("Щ", "sht");
        cyrillicToLatin.put("ъ", "a"); cyrillicToLatin.put("Ъ", "a");
        cyrillicToLatin.put("ь", "y"); cyrillicToLatin.put("Ь", "y");
        cyrillicToLatin.put("ю", "yu"); cyrillicToLatin.put("Ю", "yu");
        cyrillicToLatin.put("я", "ya"); cyrillicToLatin.put("Я", "ya");

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            String currentChar = String.valueOf(text.charAt(i));

            if (cyrillicToLatin.containsKey(currentChar)) {
                // Транслитерирай кирилицата
                result.append(cyrillicToLatin.get(currentChar));
            } else if (Character.isLetterOrDigit(text.charAt(i))) {
                // Запази латински букви и цифри
                result.append(Character.toLowerCase(text.charAt(i)));
            } else if (Character.isWhitespace(text.charAt(i))) {
                // Whitespace става тире
                result.append('-');
            } else if (text.charAt(i) == '-') {
                // Запази тиретата
                result.append('-');
            }
            // Всички останали символи се игнорират (включително препинателни знаци)
        }

        // Почисти резултата
        String cleaned = result.toString()
                .replaceAll("-+", "-")  // Множество тирета -> едно тире
                .replaceAll("^-+", "")  // Премахни тирета в началото
                .replaceAll("-+$", ""); // Премахни тирета в края

        return cleaned;
    }

    private String removeAllQuotes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text
                .replace("\"", "")          // " обикновени двойни кавички
                .replace("'", "")           // ' обикновени единични кавички
                .replace("`", "")           // ` backtick
                .replace("\u00B4", "")      // ´ acute accent
                .replace("\u2018", "")      // ' left single quotation mark
                .replace("\u2019", "")      // ' right single quotation mark
                .replace("\u201C", "")      // " left double quotation mark
                .replace("\u201D", "")      // " right double quotation mark
                .replace("\u00AB", "")      // « left guillemet
                .replace("\u00BB", "")      // » right guillemet
                .replace("\u201E", "")      // „ double low-9 quotation mark
                .replace("\u201F", "")      // ‟ double high-reversed-9 quotation mark
                .replace("\u201B", "")      // ‛ single high-reversed-9 quotation mark
                .replace("\u201A", "");     // ‚ single low-9 quotation mark
    }
}