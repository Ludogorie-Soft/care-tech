package com.techstore.dto.filter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Requests and responses of the filter admin (/api/admin/filters). Everything here edits the canonical
 * filter layer: attributes, their values, the rules that map supplier data onto them, and the groups a
 * category shows. Rules take effect with the next rebuild; category groups right away.
 */
public final class FilterConfigDto {

    private FilterConfigDto() {
    }

    // ── Attributes ──────────────────────────────────────────────────────────────────────────────────

    public record AttributeSummary(Long id, String slug, String nameBg, String nameEn, String valueType,
                                   String unit, String parser, String origin, boolean autoValues,
                                   int categories, int values, long products) {
    }

    public record AttributeDetail(AttributeSummary attribute, List<Source> sources, List<Value> values,
                                  List<ValueRule> valueRules, List<NameRule> nameRules,
                                  List<CategoryRef> categories) {
    }

    /** @param slug optional; derived from the Bulgarian name when missing */
    public record AttributeRequest(String slug, String nameBg, String nameEn, String valueType, String unit,
                                   String parser, Boolean autoValues, Integer sortOrder) {
    }

    public record Value(Long id, String valueBg, String valueEn, Integer sortOrder, String origin,
                        BigDecimal number, long products) {
    }

    public record ValueRequest(String valueBg, String valueEn, Integer sortOrder) {
    }

    /** Exactly one of pattern (a regular expression) and rawNorm (one exact supplier value) is set. */
    public record ValueRule(Long id, String pattern, String rawNorm, Long valueId, String valueBg, String note) {
    }

    public record ValueRuleRequest(String pattern, String rawNorm, Long valueId) {
    }

    /** Either pattern + value, or useParser: the attribute's parser reads the product name. */
    public record NameRule(Long id, Long categoryId, String categoryName, String pattern, Long valueId,
                           String valueBg, boolean useParser, String note) {
    }

    public record NameRuleRequest(Long categoryId, String pattern, Long valueId, boolean useParser) {
    }

    // ── Supplier parameters ─────────────────────────────────────────────────────────────────────────

    /** @param nameNorm the supplier parameter name as filter_norm() writes it */
    public record Source(Long id, String action, String nameNorm, Long parameterId, String platform,
                         Long categoryId, String categoryName, Long attributeId, String attribute,
                         String origin, String note) {
    }

    /** @param action MAP (attributeId required), IGNORE (specification only) or HIDE (nowhere) */
    public record SourceRequest(String action, Long attributeId, String name, Long categoryId, String platform) {
    }

    /** A supplier parameter name on the visible products of a category and what the index does with it. */
    public record RawParameter(String name, String platforms, long products, String samples, String action,
                               Long attributeId, String attribute) {
    }

    public record UnmappedValue(Long attributeId, String attribute, String rawNorm, String sampleText,
                                int optionCount) {
    }

    /** A supplier value of the attribute that a pattern would match, and the values it has today. */
    public record PreviewRow(String text, long products, String currentValues) {
    }

    public record PreviewRequest(String pattern) {
    }

    // ── Categories ──────────────────────────────────────────────────────────────────────────────────

    public record CategoryRef(Long id, String name, boolean visible, String origin) {
    }

    public record CategorySummary(Long id, String name, long products, String mode, int groups) {
    }

    public record CategoryConfig(Long id, String name, String mode, long products, List<CategoryGroup> groups,
                                 List<CandidateAttribute> candidates) {
    }

    public record CategoryGroup(Long attributeId, String name, String slug, String origin, boolean visible,
                                int sortOrder, BigDecimal coverage, long products, int values) {
    }

    /** An attribute with values on the category's products that the category does not show yet. */
    public record CandidateAttribute(Long attributeId, String name, String origin, long products, int values) {
    }

    public record ModeRequest(String mode) {
    }

    public record OrderRequest(List<Long> attributeIds) {
    }
}
