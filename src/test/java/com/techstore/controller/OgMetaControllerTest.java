package com.techstore.controller;

import com.techstore.config.SecurityConfig;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.UserRepository;
import com.techstore.util.JwtUtil;
import com.techstore.util.SecurityHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OgMetaController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cors.allowedOrigins=http://localhost:3000",
        "app.url=https://www.caretech.bg"
})
@DisplayName("OgMetaController — 301 към каноничния URL, 404 за непознато ID")
class OgMetaControllerTest {

    private static final String SITE = "https://www.caretech.bg";

    @Autowired MockMvc mockMvc;

    @MockBean ProductRepository productRepository;
    @MockBean CategoryRepository categoryRepository;
    @MockBean SecurityHelper securityHelper;
    @MockBean UserRepository userRepository;
    @MockBean JwtUtil jwtUtil;

    private static Category category(long id, String slug) {
        Category c = new Category();
        c.setId(id);
        c.setSlug(slug);
        c.setNameBg("Категория " + id);
        return c;
    }

    private static Product product(long id, String slug) {
        Product p = new Product();
        p.setId(id);
        p.setSlug(slug);
        p.setNameBg("Продукт " + id);
        return p;
    }

    /** What nginx sends after rewriting the visitor's URL to /api/og/... */
    private static MockHttpServletRequestBuilder og(String apiPath, String originalUri) {
        MockHttpServletRequestBuilder req = get(apiPath).accept(MediaType.TEXT_HTML);
        return originalUri == null ? req : req.header("X-Original-URI", originalUri);
    }

    private void noVisibleCategoryWithSlug() {
        when(categoryRepository.findFirstBySlugAndShowTrueOrderByIdAsc(anyString())).thenReturn(Optional.empty());
    }

    // ── Category ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Изместено ID (torches/213, а „Фенери“ е 214) → 301 към slug-а, не към ID-то")
    void staleCategoryIdRedirectsBySlug() throws Exception {
        when(categoryRepository.findFirstBySlugAndShowTrueOrderByIdAsc("torches")).thenReturn(Optional.of(category(214, "torches")));
        when(categoryRepository.findById(213L)).thenReturn(Optional.of(category(213, "lunch-box")));
        when(categoryRepository.existsByParentIdAndShowTrue(anyLong())).thenReturn(false);

        mockMvc.perform(og("/api/og/category/213", "/category/torches/213"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", SITE + "/category/torches/214"));
    }

    @Test
    @DisplayName("Родител без /list/ → 301 към /category/list/...")
    void parentWithoutListRedirects() throws Exception {
        when(categoryRepository.findFirstBySlugAndShowTrueOrderByIdAsc("laptops")).thenReturn(Optional.of(category(36, "laptops")));
        when(categoryRepository.existsByParentIdAndShowTrue(36L)).thenReturn(true);

        mockMvc.perform(og("/api/og/category/36", "/category/laptops/36"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", SITE + "/category/list/laptops/36"));
    }

    @Test
    @DisplayName("Листо с /list/ → 301 без /list/")
    void leafWithListRedirects() throws Exception {
        when(categoryRepository.findFirstBySlugAndShowTrueOrderByIdAsc("timers")).thenReturn(Optional.of(category(219, "timers")));
        when(categoryRepository.existsByParentIdAndShowTrue(219L)).thenReturn(false);

        mockMvc.perform(og("/api/og/category/219", "/category/list/timers/219"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", SITE + "/category/timers/219"));
    }

    @Test
    @DisplayName("Каноничен URL (с query и наклонена черта накрая) → 200 и canonical към себе си")
    void canonicalCategoryUrlIsServed() throws Exception {
        when(categoryRepository.findFirstBySlugAndShowTrueOrderByIdAsc("laptops")).thenReturn(Optional.of(category(36, "laptops")));
        when(categoryRepository.existsByParentIdAndShowTrue(36L)).thenReturn(true);

        mockMvc.perform(og("/api/og/category/36", "/category/list/laptops/36/?utm_source=fb"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<link rel=\"canonical\" href=\"" + SITE + "/category/list/laptops/36\">")));
    }

    @Test
    @DisplayName("Slug-ът вече го няма → 301 към текущия URL на ID-то")
    void unknownSlugFallsBackToId() throws Exception {
        noVisibleCategoryWithSlug();
        when(categoryRepository.findById(219L)).thenReturn(Optional.of(category(219, "timers")));
        when(categoryRepository.existsByParentIdAndShowTrue(219L)).thenReturn(false);

        mockMvc.perform(og("/api/og/category/219", "/category/old-name/219"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", SITE + "/category/timers/219"));
    }

    @Test
    @DisplayName("Скрита категория на собствения си URL → 200, без пренасочване")
    void hiddenCategoryOnItsOwnUrlIsServed() throws Exception {
        noVisibleCategoryWithSlug();
        when(categoryRepository.findById(213L)).thenReturn(Optional.of(category(213, "lunch-box")));
        when(categoryRepository.existsByParentIdAndShowTrue(213L)).thenReturn(false);

        mockMvc.perform(og("/api/og/category/213", "/category/lunch-box/213"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Непознато ID и slug → 404")
    void unknownCategoryIsNotFound() throws Exception {
        noVisibleCategoryWithSlug();
        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(og("/api/og/category/999", "/category/nothing/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Без X-Original-URI (стар nginx) → 200 както досега, без пренасочване")
    void withoutOriginalUriNothingRedirects() throws Exception {
        when(categoryRepository.findById(213L)).thenReturn(Optional.of(category(213, "lunch-box")));
        when(categoryRepository.existsByParentIdAndShowTrue(213L)).thenReturn(false);

        mockMvc.perform(og("/api/og/category/213", null))
                .andExpect(status().isOk());
    }

    // ── Product ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Продукт със стар slug → 301 към текущия")
    void productWithOldSlugRedirects() throws Exception {
        when(productRepository.findById(15245L)).thenReturn(Optional.of(product(15245, "canyon-cable")));

        mockMvc.perform(og("/api/og/product/15245", "/product/office-pc/15245"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", SITE + "/product/canyon-cable/15245"));
    }

    @Test
    @DisplayName("Продукт без slug в URL-а → 301 към URL-а със slug")
    void productWithoutSlugRedirects() throws Exception {
        when(productRepository.findById(7911L)).thenReturn(Optional.of(product(7911, "keychron-b6")));

        mockMvc.perform(og("/api/og/product/7911", "/product/7911"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", SITE + "/product/keychron-b6/7911"));
    }

    @Test
    @DisplayName("Каноничен продуктов URL → 200")
    void canonicalProductUrlIsServed() throws Exception {
        when(productRepository.findById(7911L)).thenReturn(Optional.of(product(7911, "keychron-b6")));

        mockMvc.perform(og("/api/og/product/7911", "/product/keychron-b6/7911/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<link rel=\"canonical\" href=\"" + SITE + "/product/keychron-b6/7911\">")));
    }

    @Test
    @DisplayName("Несъществуващ продукт → 404 (досега 200 с пренасочване към началната)")
    void unknownProductIsNotFound() throws Exception {
        when(productRepository.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(og("/api/og/product/999", "/product/gone/999"))
                .andExpect(status().isNotFound());
    }
}
