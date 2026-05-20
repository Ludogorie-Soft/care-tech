package com.techstore.controller;

import com.techstore.dto.response.CategoryResponseDTO;
import com.techstore.dto.response.ProductResponseDTO;
import com.techstore.service.CategoryService;
import com.techstore.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/og")
@RequiredArgsConstructor
@Slf4j
public class OgController {

    private static final String SITE_NAME    = "Care Tech";
    private static final String SITE_URL     = "https://caretech.bg";
    private static final String DEFAULT_IMG  = SITE_URL + "/logo.png";
    private static final String SITE_DESC    = "Магазин и СЕРВИЗ за Компютри, Лаптопи, Принтери, Видеонаблюдение, Периферия.";

    private final ProductService  productService;
    private final CategoryService categoryService;

    // ── Product ─────────────────────────────────────────────────────────────

    @GetMapping(value = "/product/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> productOg(@PathVariable Long id) {
        try {
            ProductResponseDTO p = productService.getProductById(id, "bg");

            String name  = p.getNameBg() != null ? p.getNameBg() : p.getNameEn();
            String desc  = buildProductDescription(p);
            String image = p.getPrimaryImageUrl() != null ? p.getPrimaryImageUrl() : DEFAULT_IMG;
            String url   = SITE_URL + "/product/" + slug(p.getSlug(), id) + "/" + id;

            return ResponseEntity.ok(buildHtml(name, desc, image, url, "product"));
        } catch (Exception e) {
            log.debug("OG product fallback for id {}: {}", id, e.getMessage());
            return fallback(SITE_URL + "/product/" + id);
        }
    }

    // ── Category ─────────────────────────────────────────────────────────────

    @GetMapping(value = "/category/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> categoryOg(@PathVariable Long id) {
        try {
            CategoryResponseDTO c = categoryService.getCategoryById(id);

            String name  = c.getNameBg() != null ? c.getNameBg() : c.getNameEn();
            String desc  = name + " — " + SITE_DESC;
            String url   = SITE_URL + "/category/" + slug(c.getSlug(), id) + "/" + id;

            return ResponseEntity.ok(buildHtml(name, desc, DEFAULT_IMG, url, "website"));
        } catch (Exception e) {
            log.debug("OG category fallback for id {}: {}", id, e.getMessage());
            return fallback(SITE_URL + "/category/" + id);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildProductDescription(ProductResponseDTO p) {
        StringBuilder sb = new StringBuilder();

        if (p.getDescriptionBg() != null && !p.getDescriptionBg().isBlank()) {
            String desc = p.getDescriptionBg().replaceAll("<[^>]+>", "").trim();
            if (desc.length() > 200) desc = desc.substring(0, 200) + "…";
            sb.append(desc);
        } else {
            if (p.getManufacturer() != null && p.getManufacturer().getName() != null) {
                sb.append(p.getManufacturer().getName()).append(" — ");
            }
            sb.append(SITE_DESC);
        }

        if (p.getFinalPrice() != null) {
            sb.append(" | ").append(p.getFinalPrice().setScale(2)).append(" лв.");
        }
        return sb.toString();
    }

    private String slug(String slug, Long id) {
        return (slug != null && !slug.isBlank()) ? slug : String.valueOf(id);
    }

    private ResponseEntity<String> fallback(String url) {
        return ResponseEntity.ok(buildHtml(SITE_NAME, SITE_DESC, DEFAULT_IMG, url, "website"));
    }

    private String buildHtml(String title, String description, String image, String url, String type) {
        String safeTitle = escape(title);
        String safeDesc  = escape(description);
        String safeImg   = escape(image);
        String safeUrl   = escape(url);

        return """
                <!DOCTYPE html>
                <html lang="bg">
                <head>
                  <meta charset="UTF-8">
                  <title>%s | %s</title>

                  <!-- Open Graph -->
                  <meta property="og:type"        content="%s" />
                  <meta property="og:site_name"   content="%s" />
                  <meta property="og:title"       content="%s | %s" />
                  <meta property="og:description" content="%s" />
                  <meta property="og:image"       content="%s" />
                  <meta property="og:url"         content="%s" />
                  <meta property="og:image:width"  content="1200" />
                  <meta property="og:image:height" content="630" />

                  <!-- Twitter Card -->
                  <meta name="twitter:card"        content="summary_large_image" />
                  <meta name="twitter:title"       content="%s | %s" />
                  <meta name="twitter:description" content="%s" />
                  <meta name="twitter:image"       content="%s" />

                  <!-- Redirect real users immediately -->
                  <meta http-equiv="refresh" content="0; url=%s" />
                  <link rel="canonical" href="%s" />
                </head>
                <body>
                  <a href="%s">%s</a>
                </body>
                </html>
                """.formatted(
                safeTitle, SITE_NAME,
                type,
                SITE_NAME,
                safeTitle, SITE_NAME,
                safeDesc,
                safeImg,
                safeUrl,
                safeTitle, SITE_NAME,
                safeDesc,
                safeImg,
                safeUrl,
                safeUrl,
                safeUrl,
                safeTitle
        );
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
