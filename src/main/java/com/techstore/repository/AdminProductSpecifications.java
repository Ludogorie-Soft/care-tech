package com.techstore.repository;

import com.techstore.entity.Manufacturer;
import com.techstore.entity.Product;
import com.techstore.util.CyrillicTransliterator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Admin product search. Unlike the storefront search it also returns hidden,
 * unavailable and image-less products, so the admin can find everything they manage.
 */
public final class AdminProductSpecifications {

    private static final int MAX_WORDS = 6;

    private AdminProductSpecifications() {
    }

    /** Every word of the query must match a name, a product code or the brand. */
    public static Specification<Product> adminSearch(String query, boolean onSale, boolean withMarkup) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (onSale) {
                predicates.add(cb.greaterThan(root.get("discount"), BigDecimal.ZERO));
            }
            if (withMarkup) {
                predicates.add(cb.greaterThan(root.get("markupPercentage"), BigDecimal.ZERO));
            }
            if (StringUtils.hasText(query)) {
                predicates.add(matchesQuery(root, cb, query.trim()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate matchesQuery(Root<Product> root, CriteriaBuilder cb, String query) {
        Join<Product, Manufacturer> manufacturer = root.join("manufacturer", JoinType.LEFT);
        List<Expression<String>> fields = List.of(
                cb.lower(root.get("nameBg")),
                cb.lower(root.get("nameEn")),
                cb.lower(root.get("sku")),
                cb.lower(root.get("model")),
                cb.lower(root.get("referenceNumber")),
                cb.lower(manufacturer.get("name")));

        Predicate[] words = Arrays.stream(query.toLowerCase().split("\\s+"))
                .limit(MAX_WORDS)
                .map(word -> matchesWord(cb, fields, word))
                .toArray(Predicate[]::new);

        List<Predicate> alternatives = new ArrayList<>();
        alternatives.add(cb.and(words));
        alternatives.add(cb.equal(root.get("barcode"), query));
        if (query.matches("\\d{1,18}")) {
            alternatives.add(cb.equal(root.get("id"), Long.valueOf(query)));
        }
        return cb.or(alternatives.toArray(Predicate[]::new));
    }

    private static Predicate matchesWord(CriteriaBuilder cb, List<Expression<String>> fields, String word) {
        List<String> forms = new ArrayList<>(List.of(word));
        String latin = CyrillicTransliterator.transliterate(word);
        if (latin != null) {
            forms.add(latin);
        }
        return cb.or(forms.stream()
                .flatMap(form -> fields.stream().map(field -> cb.like(field, "%" + escapeLike(form) + "%", '\\')))
                .toArray(Predicate[]::new));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
