package com.techstore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Paging of the TEKRA product feed. Only page 1 used to be read, so no category went past 100
 * products ("IP системи" has 391). Page delay and retry backoff are 0 here: the @Value fields are
 * not injected when the service is built by hand.
 */
@ExtendWith(MockitoExtension.class)
class TekraApiServicePagingTest {

    @Mock
    private RestTemplate restTemplate;

    private TekraApiService service;

    @BeforeEach
    void setUp() {
        service = new TekraApiService(restTemplate);
        ReflectionTestUtils.setField(service, "tekraApiEnabled", true);
        ReflectionTestUtils.setField(service, "baseUrl", "https://tekra.test/shop/api");
        ReflectionTestUtils.setField(service, "accessToken", "t");
    }

    @Test
    @DisplayName("reads every page until a short one")
    void readsAllPages() {
        page(1, items(1, 100));
        page(2, items(101, 200));
        page(3, items(201, 291));

        List<Map<String, Object>> products = service.getProductsRaw("ip-sistemi");

        assertEquals(291, products.size());
        verifyPages(3);
    }

    @Test
    @DisplayName("stops when a full page brings nothing new (TEKRA ignoring page)")
    void stopsOnRepeatedPage() {
        String all = items(1, 150);
        page(1, all);
        page(2, all);

        assertEquals(150, service.getProductsRaw("videonablyudenie").size());
        verifyPages(2);
    }

    @Test
    @DisplayName("an exactly full last page ends at the following empty page")
    void emptyPageEnds() {
        page(1, items(1, 100));
        page(2, "<items/>");

        assertEquals(100, service.getProductsRaw("ndaa").size());
        verifyPages(2);
    }

    @Test
    @DisplayName("a small category is one request")
    void smallCategory() {
        page(1, items(1, 13));

        assertEquals(13, service.getProductsRaw("hdd").size());
        verifyPages(1);
    }

    @Test
    @DisplayName("a page that cannot be read fails the category instead of returning part of it")
    void failedPageThrows() {
        page(1, items(1, 100));
        when(restTemplate.getForObject(argThat((String url) -> url != null && url.contains("page=2&")), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        assertThrows(ResourceAccessException.class, () -> service.getProductsRaw("ip-sistemi"));
    }

    @Test
    @DisplayName("429 on every attempt propagates so the sync can cool down")
    void rateLimited() {
        when(restTemplate.getForObject(argThat((String url) -> url != null && url.contains("page=1&")), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too many", null, null, null));

        assertThrows(HttpClientErrorException.TooManyRequests.class, () -> service.getProductsRaw("ip-sistemi"));
        verify(restTemplate, times(3)).getForObject(argThat((String url) -> url != null && url.contains("page=1&")), eq(String.class));
        verify(restTemplate, never()).getForObject(argThat((String url) -> url != null && url.contains("page=2&")), eq(String.class));
    }

    @Test
    @DisplayName("unparseable XML is a failure, not an empty category")
    void brokenXml() {
        page(1, "<items><item><sku>1</sku>");

        assertThrows(IllegalStateException.class, () -> service.getProductsRaw("hdd"));
    }

    private void page(int number, String body) {
        String xml = body.startsWith("<items") ? "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + body : body;
        when(restTemplate.getForObject(argThat((String url) -> url != null && url.contains("page=" + number + "&")),
                eq(String.class))).thenReturn(xml);
    }

    private void verifyPages(int pages) {
        for (int p = 1; p <= pages; p++) {
            int number = p;
            verify(restTemplate).getForObject(argThat((String url) -> url != null && url.contains("page=" + number + "&")),
                    eq(String.class));
        }
        int next = pages + 1;
        verify(restTemplate, never()).getForObject(argThat((String url) -> url != null && url.contains("page=" + next + "&")),
                eq(String.class));
    }

    private static String items(int from, int to) {
        return IntStream.rangeClosed(from, to)
                .mapToObj(i -> "<item><sku>SKU-" + i + "</sku><name>Продукт " + i + "</name></item>")
                .collect(Collectors.joining("", "<items>", "</items>"));
    }
}
