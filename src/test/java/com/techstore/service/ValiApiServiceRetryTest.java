package com.techstore.service;

import com.techstore.dto.request.ParameterRequestDto;
import com.techstore.dto.request.ProductRequestDto;
import com.techstore.exception.SupplierCategoryGoneException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * From 2026-09-26 Vali answers the ids of its removed STEM section with HTTP 400 {"error":"Invalid category id"}.
 * The parameters sync counted them as failed categories and marked every night FAILED, and both Vali syncs
 * repeated each 400 five times with backoff (~155 s per category, ~20 min per sync). These tests pin that a
 * client error is asked once, that a removed category is reported as such, and that server errors still retry.
 */
class ValiApiServiceRetryTest {

    private static final String GONE_BODY = "{\"error\":\"Invalid category id\"}";
    private static final String ONE_PARAMETER = "[{\"id\": 11, \"category_id\": 701, \"name\": [], \"options\": [], \"order\": 1}]";

    private final AtomicInteger requests = new AtomicInteger();

    /** A service whose WebClient answers from the given queue; the last answer repeats once the queue is drained. */
    private ValiApiService serviceAnswering(HttpStatus... statuses) {
        return serviceAnswering(List.of(statuses), null);
    }

    private ValiApiService serviceAnswering(List<HttpStatus> statuses, String okBody) {
        Deque<HttpStatus> queue = new ArrayDeque<>(statuses);
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    requests.incrementAndGet();
                    HttpStatus status = queue.size() > 1 ? queue.poll() : queue.peek();
                    String body = status == HttpStatus.OK ? okBody
                            : status == HttpStatus.BAD_REQUEST ? GONE_BODY
                            : "{\"error\":\"" + status.getReasonPhrase() + "\"}";
                    return Mono.just(ClientResponse.create(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(body)
                            .build());
                })
                .build();

        ValiApiService service = new ValiApiService(webClient);
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.invalid/api/v1");
        ReflectionTestUtils.setField(service, "apiToken", "test");
        ReflectionTestUtils.setField(service, "timeout", 5_000);
        ReflectionTestUtils.setField(service, "retryAttempts", 3);
        ReflectionTestUtils.setField(service, "retryDelay", 1L); // keep the suite fast
        return service;
    }

    @Test
    @DisplayName("parameters of a category Vali removed: asked once, reported as gone")
    void removedCategoryIsGoneNotRetried() {
        ValiApiService service = serviceAnswering(HttpStatus.BAD_REQUEST);

        SupplierCategoryGoneException gone = assertThrows(SupplierCategoryGoneException.class,
                () -> service.getParametersByCategory(701L));

        assertEquals(701L, gone.getCategoryId());
        assertEquals(1, requests.get());
    }

    @Test
    @DisplayName("parameters answered 404: asked once, empty list")
    void notFoundIsEmptyNotRetried() {
        ValiApiService service = serviceAnswering(HttpStatus.NOT_FOUND);

        assertTrue(service.getParametersByCategory(373L).isEmpty());
        assertEquals(1, requests.get());
    }

    @Test
    @DisplayName("parameters: a server error is retried and the next answer is used")
    void serverErrorIsRetried() {
        ValiApiService service = serviceAnswering(List.of(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.OK), ONE_PARAMETER);

        List<ParameterRequestDto> parameters = service.getParametersByCategory(500L);

        assertEquals(1, parameters.size());
        assertEquals(11L, parameters.get(0).getExternalId());
        assertEquals(2, requests.get());
    }

    @Test
    @DisplayName("parameters: a persistent server error still fails the fetch after the retries")
    void persistentServerErrorThrows() {
        ValiApiService service = serviceAnswering(HttpStatus.INTERNAL_SERVER_ERROR);

        Exception e = assertThrows(Exception.class, () -> service.getParametersByCategory(500L));

        assertFalse(e instanceof SupplierCategoryGoneException);
        assertEquals(4, requests.get()); // first try + 3 retries
    }

    @Test
    @DisplayName("parameters: another client error fails the fetch without retrying")
    void otherClientErrorThrowsOnce() {
        ValiApiService service = serviceAnswering(HttpStatus.UNAUTHORIZED);

        Exception e = assertThrows(Exception.class, () -> service.getParametersByCategory(500L));

        assertFalse(e instanceof SupplierCategoryGoneException);
        assertEquals(1, requests.get());
    }

    @Test
    @DisplayName("products of a category Vali removed: asked once, empty list")
    void productsOfRemovedCategoryAreEmpty() {
        ValiApiService service = serviceAnswering(HttpStatus.BAD_REQUEST);

        List<ProductRequestDto> products = service.getProductsByCategory(701L);

        assertTrue(products.isEmpty());
        assertEquals(1, requests.get());
    }

    @Test
    @DisplayName("products: a server error is retried")
    void productsServerErrorIsRetried() {
        ValiApiService service = serviceAnswering(List.of(HttpStatus.BAD_GATEWAY, HttpStatus.OK), "[]");

        assertTrue(service.getProductsByCategory(500L).isEmpty());
        assertEquals(2, requests.get());
    }
}
