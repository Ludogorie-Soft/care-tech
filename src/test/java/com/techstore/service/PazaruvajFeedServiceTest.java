package com.techstore.service;

import com.techstore.config.ShippingConfig;
import com.techstore.dto.pazaruvaj.PazaruvajProductProjection;
import com.techstore.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Pazaruvaj showed every offer with "6 EUR, 3 дни" — the feed wrote one fixed value from
 * {@code pazaruvaj.delivery.*} that matched nothing on the site. Pazaruvaj accepts these
 * fields only when they agree with the product page, so they now come from
 * {@link ShippingConfig}: a single number of working days, and the cost of an order holding
 * just that product.
 */
@ExtendWith(MockitoExtension.class)
class PazaruvajFeedServiceTest {

    @Mock
    private ProductRepository productRepository;

    private PazaruvajFeedService service;

    @BeforeEach
    void setUp() {
        ShippingConfig shippingConfig = new ShippingConfig();
        ReflectionTestUtils.setField(shippingConfig, "defaultShippingCost", new BigDecimal("3.50"));
        ReflectionTestUtils.setField(shippingConfig, "freeShippingThreshold", new BigDecimal("170.00"));
        ReflectionTestUtils.setField(shippingConfig, "deliveryDays", 2);

        service = new PazaruvajFeedService(productRepository, shippingConfig);
        ReflectionTestUtils.setField(service, "appUrl", "https://www.caretech.bg");
    }

    @Test
    @DisplayName("Product under 170 € with VAT pays the office rate")
    void cheapProductPaysOfficeRate() {
        String product = feedFor(product(1L, "140.00")); // 168.00 € with VAT

        assertEquals("2 работни дни", tag(product, "DeliveryTime"));
        assertEquals("3.50 EUR", tag(product, "DeliveryCost"));
    }

    @Test
    @DisplayName("Product from 170 € with VAT ships free")
    void expensiveProductShipsFree() {
        String product = feedFor(product(2L, "145.00")); // 174.00 € with VAT

        assertEquals("2 работни дни", tag(product, "DeliveryTime"));
        assertEquals("безплатно", tag(product, "DeliveryCost"));
    }

    @Test
    @DisplayName("Threshold is applied to the price with VAT, not the net price")
    void thresholdUsesPriceWithVat() {
        // 141.67 net → 170.00 with VAT: free, although the net price is under 170
        assertEquals("безплатно", tag(feedFor(product(3L, "141.67")), "DeliveryCost"));
        // 141.66 net → 169.99 with VAT: paid
        assertEquals("3.50 EUR", tag(feedFor(product(4L, "141.66")), "DeliveryCost"));
    }

    private String feedFor(PazaruvajProductProjection product) {
        when(productRepository.findAllForPazaruvajFeed()).thenReturn(List.of(product));
        when(productRepository.findAttributesForPazaruvajFeed(anyList())).thenReturn(List.of());

        service.refreshFeed();

        String feed = service.getCachedFeed();
        assertNotNull(feed, "feed was not generated");
        return feed;
    }

    private static String tag(String xml, String name) {
        Matcher m = Pattern.compile("<" + name + ">(.*?)</" + name + ">").matcher(xml);
        assertTrue(m.find(), "no <" + name + "> in feed");
        return m.group(1);
    }

    private static PazaruvajProductProjection product(Long id, String netPrice) {
        PazaruvajProductProjection p = mock(PazaruvajProductProjection.class);
        lenient().when(p.getId()).thenReturn(id);
        lenient().when(p.getProductName()).thenReturn("Product " + id);
        lenient().when(p.getSlug()).thenReturn("product-" + id);
        lenient().when(p.getFinalPrice()).thenReturn(new BigDecimal(netPrice));
        lenient().when(p.getCategoryName()).thenReturn("Монитори");
        return p;
    }
}
