package com.techstore.service;

import com.techstore.dto.request.CategoryRequestFromExternalDto;
import com.techstore.dto.request.DocumentRequestDto;
import com.techstore.dto.request.ManufacturerRequestDto;
import com.techstore.dto.request.ParameterRequestDto;
import com.techstore.dto.request.ProductRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValiApiService {

    @Qualifier("largeResponseWebClient")
    private final WebClient webClient;

    @Value("${vali.api.base-url}")
    private String baseUrl;

    @Value("${vali.api.token}")
    private String apiToken;

    @Value("${vali.api.timeout:120000}")
    private int timeout;

    @Value("${vali.api.retry-attempts:3}")
    private int retryAttempts;

    @Value("${vali.api.retry-delay:2000}")
    private long retryDelay;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "bg-BG,bg;q=0.9,en;q=0.8");
        headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
        headers.set(HttpHeaders.CONNECTION, "keep-alive");
        headers.set("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-platform", "\"Windows\"");
        headers.set("Sec-Fetch-Dest", "empty");
        headers.set("Sec-Fetch-Mode", "cors");
        headers.set("Sec-Fetch-Site", "same-origin");
        return headers;
    }

    public List<CategoryRequestFromExternalDto> getCategories() {
        log.info("Fetching categories from Vali API");
        log.info("Using base URL: {}", baseUrl);

        String fullUrl = baseUrl + "/categories";
        log.info("Full URL: {}", fullUrl);

        try {
            List<CategoryRequestFromExternalDto> categories = webClient.get()
                    .uri(fullUrl)
                    .headers(h -> h.addAll(createHeaders()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CategoryRequestFromExternalDto>>() {})
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay))
                            .filter(throwable ->
                                    throwable instanceof WebClientResponseException.TooManyRequests ||
                                            throwable instanceof WebClientResponseException.ServiceUnavailable))
                    .doOnError(WebClientResponseException.class, ex -> {
                        log.error("Error fetching categories - Status: {}, Response: {}",
                                ex.getStatusCode(), ex.getResponseBodyAsString());
                    })
                    .doOnError(throwable -> {
                        log.error("Generic error fetching categories: {}", throwable.getMessage());
                    })
                    .block();

            log.info("Successfully fetched {} categories", categories != null ? categories.size() : 0);
            return categories;

        } catch (Exception e) {
            log.error("Failed to fetch categories from Vali API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch categories from Vali API", e);
        }
    }

    public List<ManufacturerRequestDto> getManufacturers() {
        log.info("Fetching manufacturers from Vali API");
        String fullUrl = baseUrl + "/manufacturers";

        try {
            List<ManufacturerRequestDto> manufacturers = webClient.get()
                    .uri(fullUrl)
                    .headers(h -> h.addAll(createHeaders()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ManufacturerRequestDto>>() {})
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay)))
                    .doOnError(WebClientResponseException.class, ex ->
                            log.error("Error fetching manufacturers: {} - {}",
                                    ex.getStatusCode(), ex.getResponseBodyAsString()))
                    .block();

            log.info("Successfully fetched {} manufacturers", manufacturers != null ? manufacturers.size() : 0);
            return manufacturers;

        } catch (Exception e) {
            log.error("Failed to fetch manufacturers from Vali API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch manufacturers from Vali API", e);
        }
    }

    public List<ParameterRequestDto> getParametersByCategory(Long categoryId) {
        log.debug("Fetching parameters for category: {}", categoryId);
        String fullUrl = baseUrl + "/parameters/" + categoryId;

        try {
            List<ParameterRequestDto> parameters = webClient.get()
                    .uri(fullUrl)
                    .headers(h -> h.addAll(createHeaders()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ParameterRequestDto>>() {})
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay)))
                    .onErrorResume(DataBufferLimitException.class, ex -> {
                        log.error("Response too large for category {}: {}", categoryId, ex.getMessage());
                        return Mono.just(List.of());
                    })
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.warn("Error fetching parameters for category {}: {} - {}",
                                categoryId, ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.just(List.of());
                    })
                    .block();

            return parameters != null ? parameters : List.of();

        } catch (Exception e) {
            log.error("Unexpected error fetching parameters for category {}: {}", categoryId, e.getMessage());
            return List.of();
        }
    }

    public List<ProductRequestDto> getProductsByCategory(Long categoryId) {
        log.debug("Fetching products for category: {}", categoryId);
        String fullUrl = baseUrl + "/products/by_category/" + categoryId + "/full";

        try {
            List<ProductRequestDto> products = webClient.get()
                    .uri(fullUrl)
                    .headers(h -> h.addAll(createHeaders()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ProductRequestDto>>() {})
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay)))
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.warn("Error fetching products for category {}: {} - {}",
                                categoryId, ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.just(List.of());
                    })
                    .block();

            if (products == null) {
                log.warn("No products found for category {}", categoryId);
                return List.of();
            }

            log.debug("Found {} products for category {}", products.size(), categoryId);
            return products;
        } catch (Exception e) {
            log.error("Failed to fetch products for category {}: {}", categoryId, e.getMessage());
            return List.of();
        }
    }

    public List<DocumentRequestDto> getDocumentsByProduct(Long productId) {
        log.debug("Fetching documents for product: {}", productId);
        String fullUrl = baseUrl + "/products/" + productId + "/documents";

        try {
            List<DocumentRequestDto> documents = webClient.get()
                    .uri(fullUrl)
                    .headers(h -> h.addAll(createHeaders()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<DocumentRequestDto>>() {})
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay)))
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.warn("Error fetching documents for product {}: {} - {}",
                                productId, ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.just(List.of());
                    })
                    .block();

            if (documents == null) {
                log.debug("No documents found for product {}", productId);
                return List.of();
            }

            log.debug("Found {} documents for product {}", documents.size(), productId);
            return documents;

        } catch (Exception e) {
            log.error("Unexpected error fetching documents for product {}: {}", productId, e.getMessage());
            return List.of();
        }
    }

    public List<DocumentRequestDto> getAllDocuments() {
        log.debug("Fetching all documents from Vali API");
        String fullUrl = baseUrl + "/documents";

        try {
            List<DocumentRequestDto> documents = webClient.get()
                    .uri(fullUrl)
                    .headers(h -> h.addAll(createHeaders()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<DocumentRequestDto>>() {})
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay)))
                    .onErrorResume(DataBufferLimitException.class, ex -> {
                        log.error("Response too large for documents: {}", ex.getMessage());
                        return Mono.just(List.of());
                    })
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.warn("Error fetching all documents: {} - {}",
                                ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.just(List.of());
                    })
                    .block();

            if (documents == null) {
                log.debug("No documents found");
                return List.of();
            }

            log.debug("Found {} total documents", documents.size());
            return documents;

        } catch (Exception e) {
            log.error("Unexpected error fetching all documents: {}", e.getMessage());
            return List.of();
        }
    }
}