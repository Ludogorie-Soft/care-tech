package com.techstore.service;

import com.techstore.config.ShippingConfig;
import com.techstore.dto.pazaruvaj.PazaruvajAttributeProjection;
import com.techstore.dto.pazaruvaj.PazaruvajFeedConfig;
import com.techstore.dto.pazaruvaj.PazaruvajProductProjection;
import com.techstore.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class PazaruvajFeedService {

    private static final BigDecimal VAT = new BigDecimal("1.20");

    private final ProductRepository productRepository;
    private final ShippingConfig    shippingConfig;

    @Value("${app.url:https://www.caretech.bg}")
    private String appUrl;

    @Value("${pazaruvaj.delivery.home-price:6.00}")
    private BigDecimal homeDeliveryPrice;

    @Value("${pazaruvaj.delivery.days:3}")
    private int deliveryDays;

    /** Thread-safe holder for the pre-generated XML feed. */
    private final AtomicReference<String> cachedFeed = new AtomicReference<>();

    private final AtomicReference<java.time.LocalDateTime> lastRefreshedAt = new AtomicReference<>();
    private final java.util.concurrent.atomic.AtomicInteger productCount = new java.util.concurrent.atomic.AtomicInteger(0);

    /** Live feed configuration — defaults to ALL products. */
    private final AtomicReference<PazaruvajFeedConfig> feedConfig =
            new AtomicReference<>(new PazaruvajFeedConfig());

    // ── Public API ─────────────────────────────────────────────────────────────

    public String getCachedFeed() {
        return cachedFeed.get();
    }

    public boolean isFeedReady() {
        return cachedFeed.get() != null;
    }

    public java.time.LocalDateTime getLastRefreshedAt() {
        return lastRefreshedAt.get();
    }

    public int getProductCount() {
        return productCount.get();
    }

    public PazaruvajFeedConfig getFeedConfig() {
        return feedConfig.get();
    }

    public void updateFeedConfig(PazaruvajFeedConfig config) {
        feedConfig.set(config);
    }

    // ── On-demand generation ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String generateFeedForAll() {
        List<PazaruvajProductProjection> products = productRepository.findAllForPazaruvajFeed();
        return buildXml(products, loadAttributes(products), false);
    }

    @Transactional(readOnly = true)
    public String generateFeedForCategory(Long categoryId) {
        List<PazaruvajProductProjection> products = productRepository.findForPazaruvajFeedByCategory(categoryId);
        return buildXml(products, loadAttributes(products), false);
    }

    @Transactional(readOnly = true)
    public String generateFeedForProducts(List<Long> productIds) {
        List<PazaruvajProductProjection> products = productRepository.findForPazaruvajFeedByProductIds(productIds);
        return buildXml(products, loadAttributes(products), false);
    }

    @Transactional(readOnly = true)
    public String generateCsvForAll() {
        return buildCsv(productRepository.findAllForPazaruvajFeed());
    }

    @Transactional(readOnly = true)
    public String generateCsvForCategory(Long categoryId) {
        return buildCsv(productRepository.findForPazaruvajFeedByCategory(categoryId));
    }

    @Transactional(readOnly = true)
    public String generateCsvForProducts(List<Long> productIds) {
        return buildCsv(productRepository.findForPazaruvajFeedByProductIds(productIds));
    }

    // ── Scheduled refresh ──────────────────────────────────────────────────────

    /**
     * Generate the feed once 2 minutes after startup (allows the DB to warm up),
     * then refresh every 2 hours.
     */
    @PostConstruct
    @Async
    public void generateOnStartup() {
        // Small delay so the full application context is ready before hitting the DB
        try { Thread.sleep(30_000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        refreshFeed();
    }

    @Scheduled(fixedRate = 2 * 60 * 60 * 1000) // every 2 hours
    public void scheduledRefresh() {
        refreshFeed();
    }

    @Transactional(readOnly = true)
    public void refreshFeed() {
        log.info("[Pazaruvaj] Starting feed generation...");
        long start = System.currentTimeMillis();

        try {
            List<PazaruvajProductProjection> products = loadProductsForConfig(feedConfig.get());
            String xml = buildXml(products, loadAttributes(products), true);
            cachedFeed.set(xml);
            lastRefreshedAt.set(java.time.LocalDateTime.now());
            productCount.set(products.size());
            log.info("[Pazaruvaj] Feed generated: {} products, {}ms", products.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[Pazaruvaj] Feed generation failed", e);
        }
    }

    private List<PazaruvajProductProjection> loadProductsForConfig(PazaruvajFeedConfig config) {
        return switch (config.getType().toUpperCase()) {
            case "CATEGORY" -> productRepository.findForPazaruvajFeedByCategory(config.getCategoryId());
            case "PRODUCTS" -> productRepository.findForPazaruvajFeedByProductIds(
                    config.getProducts().stream()
                            .map(PazaruvajFeedConfig.ConfigProduct::getId)
                            .toList());
            default -> productRepository.findAllForPazaruvajFeed();
        };
    }

    // ── Attribute loader ───────────────────────────────────────────────────────

    private Map<Long, List<PazaruvajAttributeProjection>> loadAttributes(
            List<PazaruvajProductProjection> products) {
        if (products.isEmpty()) return Map.of();
        List<Long> ids = products.stream().map(PazaruvajProductProjection::getId).toList();
        return productRepository.findAttributesForPazaruvajFeed(ids)
                .stream()
                .collect(Collectors.groupingBy(PazaruvajAttributeProjection::getProductId));
    }

    // ── XML builder ────────────────────────────────────────────────────────────

    private String buildXml(List<PazaruvajProductProjection> products,
                             Map<Long, List<PazaruvajAttributeProjection>> attributesMap,
                             boolean includeDelivery) {
        String deliveryDaysStr = deliveryDays + " дни";

        StringBuilder sb = new StringBuilder(products.size() * 600);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<Products>\n");

        for (PazaruvajProductProjection p : products) {
            if (p.getFinalPrice() == null || p.getFinalPrice().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal priceVat = p.getFinalPrice()
                    .multiply(VAT)
                    .setScale(2, RoundingMode.HALF_UP);

            sb.append("  <Product>\n");
            appendTag(sb, "Identifier",  String.valueOf(p.getId()));
            if (isNotBlank(p.getManufacturerName())) {
                appendCdata(sb, "Manufacturer", p.getManufacturerName());
            }
            appendCdata(sb, "Name",      truncate(p.getProductName(), 200));
            appendTag(sb, "ProductUrl",  appUrl + "/product/" + p.getSlug());
            appendTag(sb, "Price", priceVat.toPlainString());

            if (isNotBlank(p.getPrimaryImageUrl())) {
                appendTag(sb, "ImageUrl", p.getPrimaryImageUrl());
            }

            String categoryText = buildCategoryText(p.getParentCategoryName(), p.getCategoryName());
            appendCdata(sb, "Category", categoryText);

            if (isNotBlank(p.getSku())) {
                appendCdata(sb, "ProductNumber", p.getSku());
            }

            if (isNotBlank(p.getDescriptionBg())) {
                appendCdata(sb, "Description", truncate(p.getDescriptionBg(), 500));
            }

            if (includeDelivery) {
                appendTag(sb, "DeliveryTime", deliveryDaysStr);
                appendTag(sb, "DeliveryCost", homeDeliveryPrice.toPlainString() + " EUR");
            }

            if (isValidEan(p.getBarcode())) {
                appendTag(sb, "EanCode", p.getBarcode());
            }

            List<PazaruvajAttributeProjection> attrs = attributesMap.get(p.getId());
            if (attrs != null && !attrs.isEmpty()) {
                sb.append("    <Attributes>\n");
                for (PazaruvajAttributeProjection attr : attrs) {
                    sb.append("      <Attribute>\n");
                    sb.append("        <Attribute_name><![CDATA[").append(attr.getParamName()).append("]]></Attribute_name>\n");
                    sb.append("        <Attribute_value><![CDATA[").append(attr.getParamValue()).append("]]></Attribute_value>\n");
                    sb.append("      </Attribute>\n");
                }
                sb.append("    </Attributes>\n");
            }

            sb.append("  </Product>\n");
        }

        sb.append("</Products>");
        return sb.toString();
    }

    // ── CSV builder ────────────────────────────────────────────────────────────

    private String buildCsv(List<PazaruvajProductProjection> products) {
        StringBuilder sb = new StringBuilder(products.size() * 200);
        sb.append("id,name,url,image_url,price_eur_vat,category,manufacturer,ean,description\n");

        for (PazaruvajProductProjection p : products) {
            if (p.getFinalPrice() == null || p.getFinalPrice().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal priceVat = p.getFinalPrice()
                    .multiply(VAT)
                    .setScale(2, RoundingMode.HALF_UP);

            String categoryText = buildCategoryText(p.getParentCategoryName(), p.getCategoryName());

            sb.append(p.getId()).append(",")
              .append(csvEscape(truncate(p.getProductName(), 200))).append(",")
              .append(csvEscape(appUrl + "/product/" + p.getSlug())).append(",")
              .append(csvEscape(p.getPrimaryImageUrl())).append(",")
              .append(priceVat.toPlainString()).append(",")
              .append(csvEscape(categoryText)).append(",")
              .append(csvEscape(p.getManufacturerName())).append(",")
              .append(isValidEan(p.getBarcode()) ? p.getBarcode() : "").append(",")
              .append(csvEscape(truncate(p.getDescriptionBg(), 500))).append("\n");
        }

        return sb.toString();
    }

    private String csvEscape(String value) {
        if (value == null || value.isBlank()) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String buildCategoryText(String parent, String category) {
        if (isNotBlank(parent) && isNotBlank(category) && !parent.equals(category)) {
            return parent + " > " + category;
        }
        return isNotBlank(category) ? category : (isNotBlank(parent) ? parent : "");
    }

    private void appendTag(StringBuilder sb, String tag, String value) {
        sb.append("    <").append(tag).append(">")
          .append(escapeXml(value))
          .append("</").append(tag).append(">\n");
    }

    private void appendCdata(StringBuilder sb, String tag, String value) {
        sb.append("    <").append(tag).append("><![CDATA[")
          .append(value)
          .append("]]></").append(tag).append(">\n");
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&apos;");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isValidEan(String barcode) {
        if (barcode == null) return false;
        String digits = barcode.trim();
        return digits.matches("\\d{8}") || digits.matches("\\d{13}");
    }
}
