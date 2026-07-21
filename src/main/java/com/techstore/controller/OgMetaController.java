package com.techstore.controller;

import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Returns minimal HTML with Open Graph meta tags for social media crawlers
 * (Facebook, WhatsApp, Viber, Telegram, etc.).
 *
 * Nginx routes requests from known bot User-Agents to these endpoints.
 * Regular browsers are never sent here — they always get the React SPA.
 * A <meta http-equiv="refresh"> acts as a safety fallback for any browser
 * that does land here.
 */
@RestController
@RequestMapping("/api/og")
@RequiredArgsConstructor
public class OgMetaController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.url:https://caretech.bg}")
    private String siteUrl;

    private static final double EURO_RATE = 1.95583;

    // ── Product ────────────────────────────────────────────────────────────────

    @GetMapping(value = "/product/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> productOg(@PathVariable Long id) {
        Optional<Product> opt = productRepository.findById(id);

        if (opt.isEmpty()) {
            return ResponseEntity.ok(redirect(siteUrl));
        }

        Product p = opt.get();
        String productUrl = siteUrl + "/product/" + p.getSlug() + "/" + p.getId();
        String image = buildImageUrl(p.getPrimaryImageUrl(), p.getId());
        String title = p.getNameBg() != null ? p.getNameBg() + " | Caretech" : "Caretech";
        String description = buildProductDescription(p);

        return ResponseEntity.ok(ogHtml(title, description, image, productUrl, "product", productUrl));
    }

    // ── Category ───────────────────────────────────────────────────────────────

    @GetMapping(value = "/category/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> categoryOg(@PathVariable Long id) {
        Optional<Category> opt = categoryRepository.findById(id);

        if (opt.isEmpty()) {
            return ResponseEntity.ok(redirect(siteUrl));
        }

        Category c = opt.get();
        String categoryUrl = siteUrl + "/category/" + c.getSlug() + "/" + c.getId();
        String title = c.getNameBg() != null ? c.getNameBg() + " | Caretech" : "Caretech";
        String description = "Разгледайте продуктите от категория " + c.getNameBg()
                + ". Онлайн магазин за технологични продукти с най-добри цени.";

        return ResponseEntity.ok(ogHtml(title, description, siteUrl + "/logo.png", categoryUrl, "website", categoryUrl));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String buildImageUrl(String stored, Long productId) {
        if (stored != null && (stored.startsWith("http://") || stored.startsWith("https://"))) {
            return stored;
        }
        return siteUrl + "/api/images/product/" + productId + "/primary";
    }

    private String buildProductDescription(Product p) {
        if (p.getDescriptionBg() != null && !p.getDescriptionBg().isBlank()) {
            String plain = p.getDescriptionBg().replaceAll("<[^>]*>", "").trim();
            if (!plain.isEmpty()) {
                return plain.length() > 160 ? plain.substring(0, 157) + "..." : plain;
            }
        }
        StringBuilder sb = new StringBuilder();
        if (p.getNameBg() != null) sb.append(p.getNameBg());
        if (p.getFinalPrice() != null) {
            BigDecimal withVat = p.getFinalPrice().multiply(BigDecimal.valueOf(1.2))
                    .setScale(2, RoundingMode.HALF_UP);
            sb.append(" — ").append(withVat).append(" €");
        }
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String ogHtml(String title, String description, String image,
                           String url, String type, String canonical) {
        String t = escape(title);
        String d = escape(description);
        String i = escape(image);
        String u = escape(url);
        return """
                <!DOCTYPE html>
                <html lang="bg">
                <head>
                  <meta charset="UTF-8">
                  <title>%s</title>
                  <meta name="description" content="%s">
                  <link rel="canonical" href="%s">

                  <!-- Open Graph -->
                  <meta property="og:type"        content="%s">
                  <meta property="og:title"       content="%s">
                  <meta property="og:description" content="%s">
                  <meta property="og:image"       content="%s">
                  <meta property="og:url"         content="%s">
                  <meta property="og:site_name"   content="Caretech">
                  <meta property="og:locale"      content="bg_BG">

                  <!-- Twitter Card -->
                  <meta name="twitter:card"        content="summary_large_image">
                  <meta name="twitter:title"       content="%s">
                  <meta name="twitter:description" content="%s">
                  <meta name="twitter:image"       content="%s">

                  <!-- Redirect browsers immediately — bots ignore this -->
                  <meta http-equiv="refresh" content="0; url=%s">
                </head>
                <body></body>
                </html>
                """.formatted(t, d, u, type, t, d, i, u, t, d, i, u);
    }

    private String redirect(String url) {
        return "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"0; url="
                + escape(url) + "\"></head><body></body></html>";
    }
}
