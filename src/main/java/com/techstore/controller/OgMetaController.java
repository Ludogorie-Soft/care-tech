package com.techstore.controller;

import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Returns minimal HTML with Open Graph meta tags for social media crawlers
 * (Facebook, WhatsApp, Viber, Telegram, etc.).
 *
 * Strategy: nginx routes ALL /product/ and /category/ requests here (not just known bots).
 * - Bots (no cookie): read OG tags and stop — goal achieved.
 * - Browsers (no cookie): get OG HTML + Set-Cookie(__og=1, 30 min) + instant meta-refresh
 *   back to the same URL. On the second request they have the cookie, nginx serves React directly.
 * - Browsers (has cookie): never reach here — nginx proxies straight to React.
 *
 * This approach works for ANY messenger/bot regardless of User-Agent.
 *
 * nginx passes the URL the visitor asked for in X-Original-URI. When it is not the canonical URL
 * (old slug, shifted category ID, parent category without /list/) the answer is a 301 to the
 * canonical one; an unknown ID is a 404.
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
    public ResponseEntity<String> productOg(@PathVariable Long id, HttpServletRequest request) {
        Optional<Product> opt = productRepository.findById(id);

        boolean googlebot = isGooglebot(request.getHeader(HttpHeaders.USER_AGENT));

        if (opt.isEmpty()) {
            return notFound();
        }

        Product p = opt.get();
        String productUrl = siteUrl + "/product/" + p.getSlug() + "/" + p.getId();

        // The ID is the key, as in ProductPage.jsx: an old or missing slug gets a 301 to the current URL.
        String requested = requestedPath(request, "/product/");
        if (requested != null && StringUtils.hasText(p.getSlug())
                && !requested.equals(p.getSlug() + "/" + p.getId())) {
            return movedPermanently(productUrl);
        }

        String image = buildImageUrl(p.getPrimaryImageUrl(), p.getId());
        String title = p.getNameBg() != null ? p.getNameBg() + " | Caretech" : "Caretech";
        String description = buildProductDescription(p);
        String imageAlt = p.getNameBg() != null ? p.getNameBg() : p.getNameEn();

        var builder = ResponseEntity.ok().contentType(MediaType.TEXT_HTML);
        if (!googlebot) {
            builder = builder.header(HttpHeaders.SET_COOKIE, "__og=1; Max-Age=1800; Path=/; SameSite=Lax");
        }
        return builder.body(ogHtml(title, description, image, productUrl, "product", productUrl, imageAlt, !googlebot));
    }

    // ── Category ───────────────────────────────────────────────────────────────

    @GetMapping(value = "/category/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> categoryOg(@PathVariable Long id, HttpServletRequest request) {
        String requested = requestedPath(request, "/category/");

        // As in Category.jsx, the slug picks the category among the visible ones and the ID is only a
        // fallback: Google still holds URLs whose ID has shifted (torches/213 while torches is 214).
        Optional<Category> opt = Optional.ofNullable(slugOf(requested))
                .flatMap(categoryRepository::findFirstBySlugAndShowTrueOrderByIdAsc)
                .or(() -> categoryRepository.findById(id));

        boolean googlebot = isGooglebot(request.getHeader(HttpHeaders.USER_AGENT));

        if (opt.isEmpty()) {
            return notFound();
        }

        Category c = opt.get();
        String categoryUrl = categoryUrl(c);
        if (requested != null && StringUtils.hasText(c.getSlug())
                && !categoryUrl.equals(siteUrl + "/category/" + requested)) {
            return movedPermanently(categoryUrl);
        }

        String title = c.getNameBg() != null ? c.getNameBg() + " | Caretech" : "Caretech";
        String description = "Разгледайте продуктите от категория " + c.getNameBg()
                + ". Онлайн магазин за технологични продукти с най-добри цени.";

        var builder = ResponseEntity.ok().contentType(MediaType.TEXT_HTML);
        if (!googlebot) {
            builder = builder.header(HttpHeaders.SET_COOKIE, "__og=1; Max-Age=1800; Path=/; SameSite=Lax");
        }
        return builder.body(ogHtml(title, description, siteUrl + "/logo.png", categoryUrl, "website", categoryUrl, null, !googlebot));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Categories with visible children live under /category/list/ (CategoryList.jsx), as React links them. */
    private String categoryUrl(Category c) {
        String list = categoryRepository.existsByParentIdAndShowTrue(c.getId()) ? "list/" : "";
        return siteUrl + "/category/" + list + c.getSlug() + "/" + c.getId();
    }

    /**
     * The part after {@code prefix} of the URL the visitor asked for, e.g. "list/laptops/36".
     * Null when nginx did not pass X-Original-URI (a direct /api/og call): no redirect then.
     */
    private static String requestedPath(HttpServletRequest request, String prefix) {
        String uri = request.getHeader("X-Original-URI");
        if (uri == null) return null;
        int query = uri.indexOf('?');
        String path;
        try {
            path = UriUtils.decode(query >= 0 ? uri.substring(0, query) : uri, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!path.startsWith(prefix)) return null;
        return StringUtils.trimTrailingCharacter(path.substring(prefix.length()), '/');
    }

    /** "list/laptops/36" → "laptops"; "36" → null. */
    private static String slugOf(String requested) {
        if (requested == null) return null;
        int lastSlash = requested.lastIndexOf('/');
        if (lastSlash <= 0) return null;
        String slug = requested.substring(0, lastSlash);
        return slug.startsWith("list/") ? slug.substring("list/".length()) : slug;
    }

    private static ResponseEntity<String> movedPermanently(String url) {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, url).build();
    }

    /** 404 for search engines; browsers still follow the meta refresh to the homepage. */
    private ResponseEntity<String> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML).body(redirect(siteUrl));
    }

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

    private boolean isGooglebot(String userAgent) {
        return userAgent != null && userAgent.toLowerCase().contains("googlebot");
    }

    private String ogHtml(String title, String description, String image,
                           String url, String type, String canonical) {
        return ogHtml(title, description, image, url, type, canonical, null, true);
    }

    private String ogHtml(String title, String description, String image,
                           String url, String type, String canonical, String imageAlt) {
        return ogHtml(title, description, image, url, type, canonical, imageAlt, true);
    }

    private String ogHtml(String title, String description, String image,
                           String url, String type, String canonical, String imageAlt,
                           boolean includeJsRedirect) {
        String t = escape(title);
        String d = escape(description);
        String i = escape(image);
        String u = escape(url);
        String alt = escape(imageAlt != null ? imageAlt : title);
        String body = includeJsRedirect
                ? "  <!-- JS redirect: browsers follow this, social crawlers do not execute JS -->\n  <script>window.location.replace('" + u + "');</script>"
                : "  <!-- Googlebot: no redirect, canonical is authoritative -->";
        return """
                <!DOCTYPE html>
                <html lang="bg">
                <head>
                  <meta charset="UTF-8">
                  <title>%s</title>
                  <meta name="description" content="%s">
                  <link rel="canonical" href="%s">

                  <!-- Open Graph -->
                  <meta property="og:type"              content="%s">
                  <meta property="og:title"             content="%s">
                  <meta property="og:description"       content="%s">
                  <meta property="og:image"             content="%s">
                  <meta property="og:image:secure_url"  content="%s">
                  <meta property="og:image:alt"         content="%s">
                  <meta property="og:url"               content="%s">
                  <meta property="og:site_name"         content="Caretech">
                  <meta property="og:locale"            content="bg_BG">

                  <!-- Twitter Card -->
                  <meta name="twitter:card"        content="summary_large_image">
                  <meta name="twitter:title"       content="%s">
                  <meta name="twitter:description" content="%s">
                  <meta name="twitter:image"       content="%s">
                  <meta name="twitter:image:alt"   content="%s">

                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(t, d, u, type, t, d, i, i, alt, u, t, d, i, alt, body);
    }

    private String redirect(String url) {
        return "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"0; url="
                + escape(url) + "\"></head><body></body></html>";
    }
}
