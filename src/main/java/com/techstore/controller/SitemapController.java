package com.techstore.controller;

import com.techstore.enums.BlogPostStatus;
import com.techstore.repository.BlogPostRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.SitemapEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SitemapController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BlogPostRepository blogPostRepository;

    @Value("${app.url:https://www.caretech.bg}")
    private String appUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Pages without an id in the URL. Each path must be a route in care-tech-ui/src/App.js: the
     * frontend answers 200 for any path, so a wrong one here shows up in Search Console as a
     * soft 404 rather than as an error.
     */
    static final String[][] STATIC_PAGES = {
            {"/",                     "1.0", "daily"},
            {"/promotions",           "0.9", "daily"},
            {"/blog",                 "0.7", "weekly"},
            {"/contacts",             "0.6", "monthly"},
            {"/services",             "0.6", "monthly"},
            {"/about",                "0.5", "monthly"},
            {"/why-caretech",         "0.5", "monthly"},
            {"/certifications",       "0.5", "monthly"},
            {"/clients",              "0.4", "monthly"},
            {"/faq",                  "0.5", "monthly"},
            {"/delivery-and-payment", "0.5", "monthly"},
            {"/installment-purchase", "0.5", "monthly"},
            {"/device-insurance",     "0.4", "monthly"},
            {"/return-policy",        "0.4", "monthly"},
            {"/policy",               "0.3", "yearly"},
            {"/cookies",              "0.2", "yearly"},
            {"/site-map",             "0.3", "monthly"},
    };

    @GetMapping(value = "/api/sitemap.xml")
    public ResponseEntity<String> sitemap() {
        try {
            String today = LocalDateTime.now().format(DATE_FMT);
            StringBuilder xml = new StringBuilder(512 * 1024);
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

            // Static pages
            for (String[] page : STATIC_PAGES) {
                appendUrl(xml, appUrl + page[0], today, page[2], page[1]);
            }

            // Categories
            List<SitemapEntry> categories = categoryRepository.findSitemapEntries();
            Set<Long> parentIds = categoryRepository.findVisibleParentIds();
            for (SitemapEntry cat : categories) {
                String path = parentIds.contains(cat.getId()) ? "/category/list/" : "/category/";
                String loc = appUrl + path + cat.getSlug() + "/" + cat.getId();
                String lastmod = cat.getUpdatedAt() != null ? cat.getUpdatedAt().format(DATE_FMT) : today;
                appendUrl(xml, loc, lastmod, "weekly", "0.8");
            }

            // Products
            List<SitemapEntry> products = productRepository.findSitemapEntries();
            for (SitemapEntry p : products) {
                String loc = appUrl + "/product/" + p.getSlug() + "/" + p.getId();
                String lastmod = p.getUpdatedAt() != null ? p.getUpdatedAt().format(DATE_FMT) : today;
                appendUrl(xml, loc, lastmod, "weekly", "0.9");
            }

            // Blog posts
            List<SitemapEntry> blogPosts = blogPostRepository.findPublishedSitemapEntries(BlogPostStatus.PUBLISHED);
            for (SitemapEntry bp : blogPosts) {
                String loc = appUrl + "/blog/" + bp.getSlug();
                String lastmod = bp.getUpdatedAt() != null ? bp.getUpdatedAt().format(DATE_FMT) : today;
                appendUrl(xml, loc, lastmod, "monthly", "0.6");
            }

            xml.append("</urlset>");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "xml", StandardCharsets.UTF_8));
            return new ResponseEntity<>(xml.toString(), headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Sitemap generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void appendUrl(StringBuilder xml, String loc, String lastmod, String changefreq, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
