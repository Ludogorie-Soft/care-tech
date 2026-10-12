package com.techstore.controller;

import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.service.ProductService;
import com.techstore.service.seo.SpaHead;
import com.techstore.service.seo.SpaShellService;
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
 * Product and category pages: the React index.html with the page's own head tags and HTTP status.
 *
 * nginx sends every /product/ and /category/ request here, passing the URL the visitor asked for
 * in X-Original-URI. Everyone gets the same answer:
 * - 200: the React app, its title, description, canonical and Open Graph tags already filled in, so
 *   social crawlers (no JS) get a preview and search engines get the canonical before rendering.
 * - 301: the URL is not the canonical one (old slug, shifted category ID, parent without /list/).
 * - 404: unknown or hidden product/category, with noindex; React shows its "not available" page.
 * - 503: index.html is not available; nginx then serves the plain React app.
 */
@RestController
@RequestMapping("/api/og")
@RequiredArgsConstructor
public class OgMetaController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SpaShellService spaShell;

    private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

    @Value("${app.url:https://caretech.bg}")
    private String siteUrl;

    // ── Product ────────────────────────────────────────────────────────────────

    @GetMapping("/product/{id}")
    public ResponseEntity<String> productPage(@PathVariable Long id, HttpServletRequest request) {
        Optional<Product> opt = productRepository.findWithManufacturerById(id).filter(ProductService::isVisibleOnSite);
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

        // Titles as ProductPage.jsx sets them, so the head does not change when React starts.
        String name = p.getNameBg() != null ? p.getNameBg() : p.getNameEn();
        String title = p.getManufacturer() != null && p.getManufacturer().getName() != null
                ? name + " | " + p.getManufacturer().getName()
                : name;
        return page(new SpaHead.Tags(title, name + " | Care Tech", buildProductDescription(p, name), productUrl,
                buildImageUrl(p.getPrimaryImageUrl(), p.getId()), name, "product", true));
    }

    // ── Category ───────────────────────────────────────────────────────────────

    @GetMapping("/category/{id}")
    public ResponseEntity<String> categoryPage(@PathVariable Long id, HttpServletRequest request) {
        String requested = requestedPath(request, "/category/");

        // As in Category.jsx, the slug picks the category among the visible ones and the ID is only a
        // fallback: Google still holds URLs whose ID has shifted (torches/213 while torches is 214).
        Optional<Category> opt = Optional.ofNullable(slugOf(requested))
                .flatMap(categoryRepository::findFirstBySlugAndShowTrueOrderByIdAsc)
                .or(() -> categoryRepository.findById(id))
                .filter(c -> Boolean.TRUE.equals(c.getShow()));
        if (opt.isEmpty()) {
            return notFound();
        }

        Category c = opt.get();
        boolean parent = categoryRepository.existsByParentIdAndShowTrue(c.getId());
        String categoryUrl = siteUrl + "/category/" + (parent ? "list/" : "") + c.getSlug() + "/" + c.getId();
        if (requested != null && StringUtils.hasText(c.getSlug())
                && !categoryUrl.equals(siteUrl + "/category/" + requested)) {
            return movedPermanently(categoryUrl);
        }

        // Parents render CategoryList.jsx, the others Category.jsx; titles as those pages set them.
        String name = c.getNameBg() != null ? c.getNameBg() : c.getNameEn();
        String title = parent ? name + " | Care Tech" : name + " - Категория";
        String description = parent
                ? "Разгледайте подкатегориите на " + name + " в Care Tech."
                : "Разгледайте продуктите от категория " + name + ". Онлайн магазин за технологични продукти с най-добри цени.";
        return page(new SpaHead.Tags(title, name + " | Care Tech", description, categoryUrl,
                siteUrl + "/logo.png", name, "website", true));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private ResponseEntity<String> page(SpaHead.Tags tags) {
        return spaShell.indexHtml()
                .map(html -> html(HttpStatus.OK, SpaHead.render(html, tags)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    /** 404 with noindex. Without index.html, a bare page that sends browsers to the homepage. */
    private ResponseEntity<String> notFound() {
        SpaHead.Tags tags = new SpaHead.Tags("Страницата не е намерена | Care Tech", "Care Tech",
                "Страницата не е намерена или продуктът вече не е наличен.", null,
                siteUrl + "/logo.png", "Care Tech", "website", false);
        return html(HttpStatus.NOT_FOUND, spaShell.indexHtml()
                .map(html -> SpaHead.render(html, tags))
                .orElseGet(() -> redirect(siteUrl)));
    }

    /**
     * No {@code produces} on the mappings: WebConfig resolves an Accept header of "any type" (curl, some
     * crawlers) to JSON, which a text/html-only mapping would answer with 406. The type is set here instead.
     */
    private static ResponseEntity<String> html(HttpStatus status, String body) {
        // index.html points to hashed bundles that change with every frontend deploy
        return ResponseEntity.status(status).contentType(TEXT_HTML_UTF8)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache").body(body);
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

    private String buildImageUrl(String stored, Long productId) {
        if (stored != null && (stored.startsWith("http://") || stored.startsWith("https://"))) {
            return stored;
        }
        return siteUrl + "/api/images/product/" + productId + "/primary";
    }

    /** As ProductPage.jsx: the description cut at 155 characters, else the name and the price. */
    private static String buildProductDescription(Product p, String name) {
        if (p.getDescriptionBg() != null && !p.getDescriptionBg().isBlank()) {
            String plain = p.getDescriptionBg().replaceAll("<[^>]*>", "").trim();
            if (!plain.isEmpty()) {
                return plain.length() > 155 ? plain.substring(0, 155) + "..." : plain;
            }
        }
        StringBuilder sb = new StringBuilder(name);
        if (p.getFinalPrice() != null) {
            BigDecimal withVat = p.getFinalPrice().multiply(BigDecimal.valueOf(1.2))
                    .setScale(2, RoundingMode.HALF_UP);
            sb.append(" — ").append(withVat).append(" €");
        }
        return sb.toString();
    }

    private static String redirect(String url) {
        return "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"0; url="
                + url + "\"></head><body></body></html>";
    }
}
