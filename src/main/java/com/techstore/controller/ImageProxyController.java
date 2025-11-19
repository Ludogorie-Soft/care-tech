package com.techstore.controller;

import com.techstore.service.ProductService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Hidden
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageProxyController {

    private final ProductService productService;

    @GetMapping("/product/{productId}/primary")
    public void getPrimaryImage(@PathVariable Long productId, HttpServletResponse response) {
        try {
            String originalImageUrl = productService.getOriginalImageUrl(productId, true, 0);
            if (originalImageUrl != null) {
                proxyImage(originalImageUrl, response);
            } else {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            log.error("Error serving primary image for product {}: {}", productId, e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @GetMapping("/product/{productId}/additional/{index}")
    public void getAdditionalImage(@PathVariable Long productId, @PathVariable int index,
                                   HttpServletResponse response) {
        try {
            String originalImageUrl = productService.getOriginalImageUrl(productId, false, index);
            if (originalImageUrl != null) {
                proxyImage(originalImageUrl, response);
            } else {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            log.error("Error serving additional image {} for product {}: {}", index, productId, e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private void proxyImage(String imageUrl, HttpServletResponse response) {
        // Convert vali.bg HTTPS URLs to HTTP to avoid SSL timeout issues
        if (imageUrl.contains("vali.bg") && imageUrl.startsWith("https://")) {
            String originalUrl = imageUrl;
            imageUrl = imageUrl.replace("https://", "http://");
            log.debug("Converted Vali HTTPS URL to HTTP: {} -> {}", originalUrl, imageUrl);
        }

        int maxRetries = 2;
        int retryCount = 0;

        while (retryCount <= maxRetries) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(imageUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Increased timeouts for external APIs
                connection.setConnectTimeout(15000); // 15 seconds
                connection.setReadTimeout(30000);    // 30 seconds

                // Add headers to avoid being blocked
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                connection.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
                connection.setRequestProperty("Accept-Language", "bg-BG,bg;q=0.9,en;q=0.8");

                // Set appropriate content type
                String contentType = connection.getContentType();
                if (contentType != null && contentType.startsWith("image/")) {
                    response.setContentType(contentType);
                } else {
                    response.setContentType(getContentTypeFromUrl(imageUrl));
                }

                // Set cache headers (24 hours cache for external images)
                response.setHeader("Cache-Control", "public, max-age=86400");
                response.setHeader("Vary", "Accept-Encoding");

                // Copy image data
                try (InputStream inputStream = connection.getInputStream();
                     OutputStream outputStream = response.getOutputStream()) {
                    byte[] buffer = new byte[8192]; // Larger buffer
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }

                // Success - exit retry loop
                log.debug("Successfully proxied image from: {}", imageUrl);
                return;

            } catch (java.net.SocketTimeoutException e) {
                retryCount++;
                log.warn("Timeout proxying image from {} (attempt {}/{}): {}",
                        imageUrl, retryCount, maxRetries + 1, e.getMessage());

                if (retryCount > maxRetries) {
                    log.error("Failed to proxy image after {} attempts: {}", maxRetries + 1, imageUrl);
                    response.setStatus(HttpStatus.GATEWAY_TIMEOUT.value());
                    return;
                }

                // Exponential backoff before retry
                try {
                    Thread.sleep(retryCount * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }

            } catch (Exception e) {
                log.error("Error proxying image from {}: {} - {}", imageUrl, e.getClass().getSimpleName(), e.getMessage());
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                break;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    private String getContentTypeFromUrl(String imageUrl) {
        if (imageUrl.toLowerCase().endsWith(".png")) return "image/png";
        if (imageUrl.toLowerCase().endsWith(".jpg") || imageUrl.toLowerCase().endsWith(".jpeg")) return "image/jpeg";
        if (imageUrl.toLowerCase().endsWith(".gif")) return "image/gif";
        if (imageUrl.toLowerCase().endsWith(".webp")) return "image/webp";
        if (imageUrl.toLowerCase().endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg"; // default fallback
    }
}