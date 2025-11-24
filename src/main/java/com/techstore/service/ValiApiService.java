package com.techstore.service;

import com.techstore.dto.external.*;
import com.techstore.dto.request.*;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Value("${vali.api.timeout:300000}")
    private int timeout;

    @Value("${vali.api.retry-attempts:5}")
    private int retryAttempts;

    @Value("${vali.api.retry-delay:5000}")
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

    /**
     * Get categories (no pagination available)
     */
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
                                            throwable instanceof WebClientResponseException.ServiceUnavailable ||
                                            (throwable.getMessage() != null && (
                                                    throwable.getMessage().contains("Connection") ||
                                                            throwable.getMessage().contains("timeout") ||
                                                            throwable.getMessage().contains("prematurely closed")
                                            )))
                            .doBeforeRetry(retrySignal ->
                                    log.info("⟳ Retrying categories (attempt {}/{})",
                                            retrySignal.totalRetries() + 1, retryAttempts)))
                    .doOnError(WebClientResponseException.class, ex -> {
                        log.error("Error fetching categories - Status: {}, Response: {}",
                                ex.getStatusCode(), ex.getResponseBodyAsString());
                    })
                    .doOnError(throwable -> {
                        log.error("Generic error fetching categories: {}", throwable.getMessage());
                    })
                    .block();

            log.info("✓ Successfully fetched {} categories", categories != null ? categories.size() : 0);
            return categories;

        } catch (Exception e) {
            log.error("✗ Failed to fetch categories from Vali API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch categories from Vali API", e);
        }
    }

    /**
     * Get manufacturers (no pagination available)
     * Uses increased timeout and retry for large response
     */
    public List<ManufacturerRequestDto> getManufacturers() {
        log.info("Fetching manufacturers from Vali API (no pagination available)");
        String fullUrl = baseUrl + "/manufacturers";

        try {
            List<ManufacturerRequestDto> manufacturers = webClient.get()
                    .uri(fullUrl)
                    .headers(h -> h.addAll(createHeaders()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ManufacturerRequestDto>>() {})
                    .timeout(Duration.ofMillis(timeout))
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay))
                            .filter(throwable -> {
                                String message = throwable.getMessage();
                                boolean shouldRetry = throwable instanceof WebClientResponseException ||
                                        (message != null && (
                                                message.contains("Connection") ||
                                                        message.contains("timeout") ||
                                                        message.contains("prematurely closed") ||
                                                        message.contains("reset by peer")
                                        ));

                                if (shouldRetry) {
                                    log.warn("Retry manufacturers: {}", message);
                                }
                                return shouldRetry;
                            })
                            .doBeforeRetry(retrySignal ->
                                    log.info("⟳ Retrying manufacturers (attempt {}/{})",
                                            retrySignal.totalRetries() + 1, retryAttempts)))
                    .doOnError(WebClientResponseException.class, ex ->
                            log.error("Error fetching manufacturers: {} - {}",
                                    ex.getStatusCode(), ex.getResponseBodyAsString()))
                    .onErrorResume(throwable -> {
                        log.error("Critical error fetching manufacturers: {}", throwable.getMessage());
                        return Mono.just(List.of());
                    })
                    .block();

            log.info("✓ Successfully fetched {} manufacturers", manufacturers != null ? manufacturers.size() : 0);
            return manufacturers != null ? manufacturers : List.of();

        } catch (Exception e) {
            log.error("✗ Failed to fetch manufacturers from Vali API: {}", e.getMessage(), e);
            return List.of(); // Return empty list instead of throwing
        }
    }

    /**
     * Get parameters by category (no pagination available)
     */
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

    /**
     * Get products by category (no pagination available in this endpoint)
     * For paginated approach, use getProductsByCategoryPaginated()
     */
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

    /**
     * Get products by category with PAGINATION support
     * This is the RECOMMENDED way to fetch products for large categories
     *
     * API: GET /api/v1/products?page={page}&per_page={perPage}
     * per_page range: 100-1000
     */
    public List<ProductRequestDto> getProductsByCategoryPaginated(Long categoryId, int perPage) {
        if (perPage < 100 || perPage > 1000) {
            log.warn("per_page must be 100-1000, adjusting to 500");
            perPage = 500;
        }

        log.info("Fetching products for category {} with pagination (per_page: {})", categoryId, perPage);

        List<ProductRequestDto> allProducts = new ArrayList<>();
        int page = 1;
        boolean hasMore = true;

        try {
            while (hasMore) {
                try {
                    log.info("  → Fetching page {} (per_page: {})", page, perPage);

                    String fullUrl = baseUrl + "/products?page=" + page + "&per_page=" + perPage;

                    int finalPage = page;
                    Map<String, Object> response = webClient.get()
                            .uri(fullUrl)
                            .headers(h -> h.addAll(createHeaders()))
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                            .timeout(Duration.ofMillis(timeout))
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(retryDelay))
                                    .filter(throwable -> {
                                        log.warn("Retry page {}: {}", finalPage, throwable.getMessage());
                                        return true;
                                    }))
                            .block();

                    if (response == null) {
                        log.warn("No response for page {}", page);
                        break;
                    }

                    // Parse paginated response
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

                    Integer totalItems = (Integer) response.get("total_items");
                    Integer currentPage = (Integer) response.get("current_page");
                    Integer lastPage = (Integer) response.get("last_page");

                    log.info("  ✓ Page {}/{}: {} products (total: {})",
                            currentPage, lastPage, items.size(), totalItems);

                    if (items == null || items.isEmpty()) {
                        log.info("  → No more products");
                        break;
                    }

                    // Convert to DTOs
                    List<ProductRequestDto> pageProducts = items.stream()
                            .map(this::mapToProductDto)
                            .toList();

                    allProducts.addAll(pageProducts);
                    log.info("  → Total collected so far: {}", allProducts.size());

                    hasMore = currentPage < lastPage;
                    page++;

                    // Small delay between pages to avoid overwhelming the server
                    if (hasMore) {
                        Thread.sleep(300);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while fetching products");
                    break;
                } catch (Exception e) {
                    log.error("Error fetching products page {}: {}", page, e.getMessage());
                    break;
                }
            }

            log.info("✓ Successfully fetched total of {} products for category {}",
                    allProducts.size(), categoryId);
            return allProducts;

        } catch (Exception e) {
            log.error("Failed to fetch paginated products for category {}: {}", categoryId, e.getMessage());
            return allProducts; // Return what we have collected so far
        }
    }

    /**
     * Get ALL products with PAGINATION (recommended for full sync)
     *
     * API: GET /api/v1/products?page={page}&per_page={perPage}
     * per_page range: 100-1000
     */
    public List<ProductRequestDto> getAllProductsPaginated(int perPage) {
        if (perPage < 100 || perPage > 1000) {
            log.warn("per_page must be 100-1000, adjusting to 500");
            perPage = 500;
        }

        log.info("Fetching ALL products with pagination (per_page: {})", perPage);

        List<ProductRequestDto> allProducts = new ArrayList<>();
        int page = 1;
        boolean hasMore = true;

        try {
            while (hasMore) {
                try {
                    log.info("  → Fetching page {}", page);

                    String fullUrl = baseUrl + "/products?page=" + page + "&per_page=" + perPage;

                    int finalPage = page;
                    Map<String, Object> response = webClient.get()
                            .uri(fullUrl)
                            .headers(h -> h.addAll(createHeaders()))
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                            .timeout(Duration.ofMillis(timeout))
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(retryDelay))
                                    .filter(throwable -> {
                                        log.warn("Retry page {}: {}", finalPage, throwable.getMessage());
                                        return true;
                                    }))
                            .block();

                    if (response == null) {
                        log.warn("No response for page {}", page);
                        break;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

                    Integer totalItems = (Integer) response.get("total_items");
                    Integer currentPage = (Integer) response.get("current_page");
                    Integer lastPage = (Integer) response.get("last_page");

                    log.info("  ✓ Page {}/{}: {} products (total in catalog: {})",
                            currentPage, lastPage, items != null ? items.size() : 0, totalItems);

                    if (items == null || items.isEmpty()) {
                        break;
                    }

                    List<ProductRequestDto> pageProducts = items.stream()
                            .map(this::mapToProductDto)
                            .toList();

                    allProducts.addAll(pageProducts);
                    log.info("  → Total collected: {}/{}", allProducts.size(), totalItems);

                    hasMore = currentPage < lastPage;
                    page++;

                    // Delay between pages
                    if (hasMore) {
                        Thread.sleep(500);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted");
                    break;
                } catch (Exception e) {
                    log.error("Error fetching page {}: {}", page, e.getMessage());

                    // Continue if we have some products already
                    if (!allProducts.isEmpty()) {
                        log.warn("Returning {} products collected so far", allProducts.size());
                        break;
                    }
                    throw e;
                }
            }

            log.info("✓ Successfully fetched total of {} products", allProducts.size());
            return allProducts;

        } catch (Exception e) {
            log.error("Failed to fetch all products: {}", e.getMessage());
            return allProducts;
        }
    }

    private ProductRequestDto mapToProductDto(Map<String, Object> data) {
        ProductRequestDto dto = new ProductRequestDto();

        try {
            // ============================================
            // BASIC FIELDS
            // ============================================
            if (data.get("id") != null) {
                dto.setId(((Number) data.get("id")).longValue());
            }
            if (data.get("idWF") != null) {
                dto.setIdWF(((Number) data.get("idWF")).longValue());
            }
            if (data.get("reference_number") != null) {
                dto.setReferenceNumber((String) data.get("reference_number"));
            }
            if (data.get("model") != null) {
                dto.setModel((String) data.get("model"));
            }
            if (data.get("barcode") != null) {
                dto.setBarcode((String) data.get("barcode"));
            }

            // ============================================
            // MANUFACTURER
            // ============================================
            if (data.get("manufacturer_id") != null) {
                dto.setManufacturerId(((Number) data.get("manufacturer_id")).longValue());
            }
            if (data.get("manufacturer") != null) {
                dto.setManufacturer((String) data.get("manufacturer"));
            }

            // ============================================
            // STATUS AND PRICING (using BigDecimal)
            // ============================================
            if (data.get("status") != null) {
                dto.setStatus(((Number) data.get("status")).intValue());
            }
            if (data.get("price_client") != null) {
                dto.setPriceClient(new BigDecimal(data.get("price_client").toString()));
            }
            if (data.get("price_partner") != null) {
                dto.setPricePartner(new BigDecimal(data.get("price_partner").toString()));
            }

            // Handle nullable promo prices
            Object pricePromo = data.get("price_promo");
            if (pricePromo != null && !"null".equals(pricePromo.toString())) {
                dto.setPricePromo(new BigDecimal(pricePromo.toString()));
            }

            Object priceClientPromo = data.get("price_client_promo");
            if (priceClientPromo != null && !"null".equals(priceClientPromo.toString())) {
                dto.setPriceClientPromo(new BigDecimal(priceClientPromo.toString()));
            }

            if (data.get("show") != null) {
                dto.setShow((Boolean) data.get("show"));
            }

            // ============================================
            // PRODUCT DETAILS
            // ============================================
            if (data.get("warranty") != null) {
                dto.setWarranty(((Number) data.get("warranty")).intValue());
            }
            if (data.get("weight") != null) {
                dto.setWeight(new BigDecimal(data.get("weight").toString()));
            }

            // ============================================
            // CATEGORIES - List<CategoryIdDto>
            // API: [{"id": 422}, {"id": 423}]
            // ============================================
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categories = (List<Map<String, Object>>) data.get("categories");
            if (categories != null && !categories.isEmpty()) {
                List<CategoryIdDto> categoryDtos = new ArrayList<>();
                for (Map<String, Object> category : categories) {
                    if (category.get("id") != null) {
                        CategoryIdDto categoryDto = new CategoryIdDto();
                        categoryDto.setId(((Number) category.get("id")).longValue());
                        categoryDtos.add(categoryDto);
                    }
                }
                dto.setCategories(categoryDtos);
            }

            // ============================================
            // NAME - List<NameDto>
            // API: [{"language_code": "bg", "text": "..."}, {"language_code": "en", "text": "..."}]
            // ============================================
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> names = (List<Map<String, Object>>) data.get("name");
            if (names != null) {
                List<NameDto> nameDtos = new ArrayList<>();
                for (Map<String, Object> name : names) {
                    NameDto nameDto = new NameDto();
                    nameDto.setLanguageCode((String) name.get("language_code"));
                    nameDto.setText((String) name.get("text"));
                    nameDtos.add(nameDto);
                }
                dto.setName(nameDtos);
            }

            // ============================================
            // DESCRIPTION - List<DescriptionDto>
            // API: [{"language_code": "bg", "text": "<p>...</p>"}, ...]
            // ============================================
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> descriptions = (List<Map<String, Object>>) data.get("description");
            if (descriptions != null) {
                List<DescriptionDto> descriptionDtos = new ArrayList<>();
                for (Map<String, Object> description : descriptions) {
                    DescriptionDto descriptionDto = new DescriptionDto();
                    descriptionDto.setLanguageCode((String) description.get("language_code"));
                    descriptionDto.setText((String) description.get("text"));
                    descriptionDtos.add(descriptionDto);
                }
                dto.setDescription(descriptionDtos);
            }

            // ============================================
            // IMAGES - List<ImageDto>
            // API: [{"href": "http://..."}, {"href": "http://..."}]
            // ============================================
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> images = (List<Map<String, Object>>) data.get("images");
            if (images != null) {
                List<ImageDto> imageDtos = new ArrayList<>();
                for (Map<String, Object> image : images) {
                    ImageDto imageDto = new ImageDto();
                    imageDto.setHref((String) image.get("href"));
                    imageDtos.add(imageDto);
                }
                dto.setImages(imageDtos);
            }

            // ============================================
            // DOCUMENTS - List<DocumentDto>
            // API: [{"href": "http://...", "comment": [{"language_code": "bg", "text": "..."}]}]
            // ============================================
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) data.get("documents");
            if (documents != null) {
                List<DocumentDto> documentDtos = new ArrayList<>();
                for (Map<String, Object> document : documents) {
                    DocumentDto documentDto = new DocumentDto();
                    documentDto.setHref((String) document.get("href"));

                    // Document comments (multi-language)
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> comments = (List<Map<String, Object>>) document.get("comment");
                    if (comments != null) {
                        List<NameDto> commentDtos = new ArrayList<>();
                        for (Map<String, Object> comment : comments) {
                            NameDto commentDto = new NameDto();
                            commentDto.setLanguageCode((String) comment.get("language_code"));
                            commentDto.setText((String) comment.get("text"));
                            commentDtos.add(commentDto);
                        }
                        documentDto.setComment(commentDtos);
                    }

                    documentDtos.add(documentDto);
                }
                dto.setDocuments(documentDtos);
            }

            // ============================================
            // PARAMETERS - List<ParameterValueRequestDto>
            // API: [{
            //   "parameter_id": 1323,
            //   "parameter_name": [{"language_code": "bg", "text": "Интерфейс"}],
            //   "option_id": 9078,
            //   "option_name": [{"language_code": "bg", "text": "PCI-ex"}]
            // }]
            // ============================================
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) data.get("parameters");
            if (parameters != null) {
                List<ParameterValueRequestDto> parameterDtos = new ArrayList<>();

                for (Map<String, Object> param : parameters) {
                    ParameterValueRequestDto parameterDto = new ParameterValueRequestDto();

                    if (param.get("parameter_id") != null) {
                        parameterDto.setParameterId(((Number) param.get("parameter_id")).longValue());
                    }

                    if (param.get("option_id") != null) {
                        parameterDto.setOptionId(((Number) param.get("option_id")).longValue());
                    }

                    parameterDtos.add(parameterDto);
                }

                dto.setParameters(parameterDtos);
            }

            // ============================================
            // FLAGS - List<FlagDto>
            // API: [{"id": 1, "image": "https://...", "name": [{"language_code": "bg", "text": "..."}]}]
            // ============================================
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> flags = (List<Map<String, Object>>) data.get("flags");
            if (flags != null) {
                List<FlagDto> flagDtos = new ArrayList<>();

                for (Map<String, Object> flag : flags) {
                    FlagDto flagDto = new FlagDto();

                    if (flag.get("id") != null) {
                        flagDto.setId(((Number) flag.get("id")).longValue());
                    }

                    if (flag.get("image") != null) {
                        flagDto.setImage((String) flag.get("image"));
                    }

                    // Flag name (multi-language)
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> flagNames = (List<Map<String, Object>>) flag.get("name");
                    if (flagNames != null) {
                        List<NameDto> nameDtos = new ArrayList<>();
                        for (Map<String, Object> name : flagNames) {
                            NameDto nameDto = new NameDto();
                            nameDto.setLanguageCode((String) name.get("language_code"));
                            nameDto.setText((String) name.get("text"));
                            nameDtos.add(nameDto);
                        }
                        flagDto.setName(nameDtos);
                    }

                    flagDtos.add(flagDto);
                }

                dto.setFlags(flagDtos);
            }

        } catch (Exception e) {
            log.error("Error mapping product data for id {}: {}",
                    data.get("id"), e.getMessage(), e);
            // Return partially mapped DTO - better than losing the whole product
        }

        return dto;
    }

    /**
     * Get documents by product
     */
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

    /**
     * Get all documents
     */
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