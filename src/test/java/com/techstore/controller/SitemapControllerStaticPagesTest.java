package com.techstore.controller;

import com.techstore.repository.BlogPostRepository;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The frontend answers 200 for every path, so a sitemap entry for a page that does not exist is
 * not an error anywhere — Google just reports a soft 404. Until 2026-10-14 five entries pointed at
 * such paths (/contact, /why-us, /our-services, /our-clients, /brands).
 */
class SitemapControllerStaticPagesTest {

    /** Routes without parameters in care-tech-ui/src/App.js that are meant to be indexed. */
    private static final Set<String> FRONTEND_ROUTES = Set.of(
            "/", "/promotions", "/blog", "/contacts", "/services", "/about", "/why-caretech",
            "/certifications", "/clients", "/faq", "/delivery-and-payment", "/installment-purchase",
            "/device-insurance", "/return-policy", "/policy", "/cookies", "/site-map");

    @Test
    void everyStaticPageIsARealRoute() {
        assertThat(Arrays.stream(SitemapController.STATIC_PAGES).map(page -> page[0]))
                .allMatch(FRONTEND_ROUTES::contains)
                .doesNotHaveDuplicates()
                .contains("/faq", "/device-insurance");
    }

    @Test
    void sitemapListsTheInformationPagesAndNotTheOldWrongPaths() {
        SitemapController controller = new SitemapController(
                mock(ProductRepository.class), mock(CategoryRepository.class), mock(BlogPostRepository.class));
        ReflectionTestUtils.setField(controller, "appUrl", "https://www.caretech.bg");

        ResponseEntity<String> response = controller.sitemap();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .contains("<loc>https://www.caretech.bg/faq</loc>",
                        "<loc>https://www.caretech.bg/device-insurance</loc>",
                        "<loc>https://www.caretech.bg/contacts</loc>",
                        "<loc>https://www.caretech.bg/why-caretech</loc>")
                .doesNotContain("/contact<", "/why-us<", "/our-services<", "/our-clients<", "/brands<");
    }
}
