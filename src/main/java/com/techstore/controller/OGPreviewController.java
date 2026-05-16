package com.techstore.controller;

import com.techstore.dto.response.ProductResponseDTO;
import com.techstore.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/og")
@RequiredArgsConstructor
@Slf4j
public class OGPreviewController {

    private final ProductService productService;

    @Value("${app.url:https://www.caretech.bg}")
    private String appUrl;

    @GetMapping("/product/{id}")
    public ResponseEntity<String> productOgPreview(@PathVariable Long id) {
        try {
            ProductResponseDTO product = productService.getProductById(id, "bg");

            String title = product.getNameBg() != null ? product.getNameBg() : product.getNameEn();
            String imageUrl = product.getPrimaryImageUrl() != null
                    ? product.getPrimaryImageUrl()
                    : appUrl + "/logo.png";
            String description = buildDescription(product);
            String productUrl = (product.getSlug() != null && !product.getSlug().isBlank())
                    ? appUrl + "/product/" + product.getSlug() + "/" + id
                    : appUrl + "/product/" + id;

            String html = buildOgHtml(title, description, imageUrl, productUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));
            return new ResponseEntity<>(html, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.warn("OG preview failed for product {}: {}", id, e.getMessage());
            HttpHeaders headers = new HttpHeaders();
            headers.set("Location", appUrl + "/product/" + id);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }

    private String buildDescription(ProductResponseDTO product) {
        String desc = product.getDescriptionBg() != null
                ? product.getDescriptionBg()
                : (product.getDescriptionEn() != null ? product.getDescriptionEn() : "");

        // Strip HTML tags if any
        desc = desc.replaceAll("<[^>]*>", "").trim();

        if (desc.length() > 200) {
            desc = desc.substring(0, 200) + "...";
        }

        if (desc.isEmpty() && product.getManufacturer() != null) {
            desc = product.getManufacturer().getName() + " - " + (product.getNameBg() != null ? product.getNameBg() : product.getNameEn());
        }

        return desc;
    }

    private String buildOgHtml(String title, String description, String imageUrl, String url) {
        String safeTitle = escape(title + " | Care Tech");
        String safeDesc = escape(description);
        String safeImage = escape(ensureAbsoluteHttps(imageUrl));
        String safeUrl = escape(url);
        String imageType = imageUrl.toLowerCase().contains(".png") ? "image/png" : "image/jpeg";

        return "<!DOCTYPE html>" +
                "<html lang=\"bg\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                "<title>" + safeTitle + "</title>" +
                "<meta name=\"description\" content=\"" + safeDesc + "\">" +
                "<meta property=\"og:type\" content=\"product\">" +
                "<meta property=\"og:site_name\" content=\"Care Tech\">" +
                "<meta property=\"og:locale\" content=\"bg_BG\">" +
                "<meta property=\"og:title\" content=\"" + safeTitle + "\">" +
                "<meta property=\"og:description\" content=\"" + safeDesc + "\">" +
                "<meta property=\"og:image\" content=\"" + safeImage + "\">" +
                "<meta property=\"og:image:secure_url\" content=\"" + safeImage + "\">" +
                "<meta property=\"og:image:type\" content=\"" + imageType + "\">" +
                "<meta property=\"og:url\" content=\"" + safeUrl + "\">" +
                "<meta name=\"twitter:card\" content=\"summary_large_image\">" +
                "<meta name=\"twitter:title\" content=\"" + safeTitle + "\">" +
                "<meta name=\"twitter:description\" content=\"" + safeDesc + "\">" +
                "<meta name=\"twitter:image\" content=\"" + safeImage + "\">" +
                "<link rel=\"canonical\" href=\"" + safeUrl + "\">" +
                "</head>" +
                "<body>" +
                "<p><a href=\"" + safeUrl + "\">Care Tech</a></p>" +
                "</body>" +
                "</html>";
    }

    private String ensureAbsoluteHttps(String imageUrl) {
        if (imageUrl == null) return appUrl + "/logo.png";
        if (imageUrl.startsWith("https://")) return imageUrl;
        if (imageUrl.startsWith("http://")) return imageUrl.replaceFirst("http://", "https://");
        if (imageUrl.startsWith("//")) return "https:" + imageUrl;
        if (imageUrl.startsWith("/")) return appUrl + imageUrl;
        return imageUrl;
    }

    private String escape(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
