package com.techstore.service;

import com.techstore.exception.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * On 2026-09-23 the nightly Most sync recorded SUCCESS with 0 products. The feed was
 * healthy — a single fetch failed at 01:50 (592 ms, no bytes) and every failure path
 * returned an empty list, which the sync service could not tell apart from a supplier
 * with no stock. These tests pin the two behaviours that were missing: a failed fetch
 * throws, and a transient failure is retried instead of losing the whole night.
 */
@ExtendWith(MockitoExtension.class)
class MostApiServiceTest {

    private static final String VALID_XML = """
            <?xml version="1.0" encoding="utf-8"?>
            <data>
              <productList>
                <product id="51530">
                  <name>C1806A DESIGNJET BLACK INK</name>
                  <price>78.43</price>
                  <currency>EUR</currency>
                  <category id="23">HP_</category>
                  <subcategory id="11">HP Expired Cartr</subcategory>
                </product>
                <product id="51531">
                  <name>ACER AG PROTECT FILM B1-71X</name>
                  <price>9.90</price>
                  <currency>EUR</currency>
                  <category id="4">NOTEBOOK</category>
                  <subcategory id="7">NB Accessories</subcategory>
                </product>
              </productList>
            </data>
            """;

    @Mock
    private RestTemplate restTemplate;

    private MostApiService service;

    @BeforeEach
    void setUp() {
        service = new MostApiService(restTemplate);
        ReflectionTestUtils.setField(service, "apiUrl", "https://example.invalid/feed.xml");
        ReflectionTestUtils.setField(service, "mostApiEnabled", true);
        ReflectionTestUtils.setField(service, "retryAttempts", 3);
        ReflectionTestUtils.setField(service, "retryDelay", 0L); // keep the suite fast
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("a healthy feed is parsed into products")
    void parsesHealthyFeed() {
        when(restTemplate.getForObject(anyString(), eq(byte[].class))).thenReturn(bytes(VALID_XML));

        List<Map<String, Object>> products = service.getAllProducts();

        assertEquals(2, products.size());
        verify(restTemplate, times(1)).getForObject(anyString(), eq(byte[].class));
    }

    @Test
    @DisplayName("an empty body is a failure, not an empty catalogue")
    void emptyBodyThrowsInsteadOfReturningEmptyList() {
        when(restTemplate.getForObject(anyString(), eq(byte[].class))).thenReturn(new byte[0]);

        assertThrows(ExternalApiException.class, () -> service.getAllProducts());
    }

    @Test
    @DisplayName("a null body is a failure too")
    void nullBodyThrows() {
        when(restTemplate.getForObject(anyString(), eq(byte[].class))).thenReturn(null);

        assertThrows(ExternalApiException.class, () -> service.getAllProducts());
    }

    @Test
    @DisplayName("a connection error throws rather than reporting an empty feed")
    void connectionErrorThrows() {
        // This is the shape of the 2026-09-23 incident: fails fast, no bytes received.
        when(restTemplate.getForObject(anyString(), eq(byte[].class)))
                .thenThrow(new ResourceAccessException("Connection reset"));

        ExternalApiException ex = assertThrows(ExternalApiException.class, () -> service.getAllProducts());
        assertTrue(ex.getMessage().contains("after 3 attempts"));
    }

    @Test
    @DisplayName("every attempt is used before giving up")
    void retriesUpToTheConfiguredLimit() {
        when(restTemplate.getForObject(anyString(), eq(byte[].class)))
                .thenThrow(new ResourceAccessException("Connection reset"));

        assertThrows(ExternalApiException.class, () -> service.getAllProducts());

        verify(restTemplate, times(3)).getForObject(anyString(), eq(byte[].class));
    }

    @Test
    @DisplayName("a transient failure is recovered on a later attempt — the night is not lost")
    void recoversFromATransientFailure() {
        when(restTemplate.getForObject(anyString(), eq(byte[].class)))
                .thenThrow(new ResourceAccessException("Connection reset"))
                .thenReturn(bytes(VALID_XML));

        List<Map<String, Object>> products = service.getAllProducts();

        assertEquals(2, products.size());
        verify(restTemplate, times(2)).getForObject(anyString(), eq(byte[].class));
    }

    @Test
    @DisplayName("an HTTP error carries its status code out to the caller")
    void httpErrorKeepsStatusCode() {
        when(restTemplate.getForObject(anyString(), eq(byte[].class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        ExternalApiException ex = assertThrows(ExternalApiException.class, () -> service.getAllProducts());
        assertEquals(502, ex.getStatusCode());
    }

    @Test
    @DisplayName("malformed XML throws instead of silently yielding nothing")
    void malformedXmlThrows() {
        when(restTemplate.getForObject(anyString(), eq(byte[].class)))
                .thenReturn(bytes("<data><productList><product id=\"1\"><name>truncated"));

        assertThrows(ExternalApiException.class, () -> service.getAllProducts());
    }

    @Test
    @DisplayName("a disabled feed stays quiet and never calls out")
    void disabledFeedReturnsEmptyWithoutCalling() {
        ReflectionTestUtils.setField(service, "mostApiEnabled", false);

        assertTrue(service.getAllProducts().isEmpty());
        verify(restTemplate, never()).getForObject(anyString(), any(Class.class));
    }

    @Test
    @DisplayName("a successful fetch is cached — the three nightly syncs pull once")
    void cachesSuccessfulFetch() {
        when(restTemplate.getForObject(anyString(), eq(byte[].class))).thenReturn(bytes(VALID_XML));

        service.getAllProducts();
        service.getAllProducts();
        service.getAllProducts();

        verify(restTemplate, times(1)).getForObject(anyString(), eq(byte[].class));
    }

    @Test
    @DisplayName("a failed fetch is not cached — the next sync tries again")
    void doesNotCacheFailures() {
        when(restTemplate.getForObject(anyString(), eq(byte[].class)))
                .thenThrow(new ResourceAccessException("Connection reset"));

        assertThrows(ExternalApiException.class, () -> service.getAllProducts());
        assertThrows(ExternalApiException.class, () -> service.getAllProducts());

        verify(restTemplate, times(6)).getForObject(anyString(), eq(byte[].class));
    }
}
