package com.techstore.controller;

import com.techstore.config.SecurityConfig;
import com.techstore.entity.Category;
import com.techstore.entity.Manufacturer;
import com.techstore.entity.Product;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.seo.SpaShellService;
import com.techstore.util.JwtUtil;
import com.techstore.util.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
@DisplayName("OgMetaController — React страницата с нейните тагове и статус 200/301/404/503")
class OgMetaControllerTest {

    private static final String SITE = "https://www.caretech.bg";

    @Autowired MockMvc mockMvc;

    @MockBean ProductRepository productRepository;
    @MockBean CategoryRepository categoryRepository;
    @MockBean SpaShellService spaShell;
    @MockBean SecurityHelper securityHelper;
    @MockBean UserRepository userRepository;
    @MockBean JwtUtil jwtUtil;

    @BeforeEach
    void productionIndexHtml() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/spa/index.html")) {
            when(spaShell.indexHtml()).thenReturn(Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        }
    }

    private static Category category(long id, String slug) {
        Category c = new Category();
        c.setId(id);
        c.setSlug(slug);
        c.setNameBg("Категория " + id);
        c.setShow(true);
        return c;
    }

    private static Category hidden(Category c) {
        c.setShow(false);
        return c;
    }

    /** A product the site shows: AVAILABLE, active, shown, not deleted. */
    private static Product product(long id, String slug) {
        Product p = new Product();
        p.setId(id);
        p.setSlug(slug);
        p.setNameBg("Продукт " + id);
        p.setStatus(ProductStatus.AVAILABLE);
        p.setActive(true);
        p.setShow(true);
        p.setDeleted(false);
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

    // ── Category: 301 ──────────────────────────────────────────────────────────

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
    @DisplayName("Slug-ът вече го няма → 301 към текущия URL на ID-то")
    void unknownSlugFallsBackToId() throws Exception {
        noVisibleCategoryWithSlug();
        when(categoryRepository.findById(219L)).thenReturn(Optional.of(category(219, "timers")));
        when(categoryRepository.existsByParentIdAndShowTrue(219L)).thenReturn(false);

        mockMvc.perform(og("/api/og/category/219", "/category/old-name/219"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", SITE + "/category/timers/219"));
    }

    // ── Category: 200 / 404 ────────────────────────────────────────────────────

    @Test
    @DisplayName("Родител на каноничния си URL → React страницата с неговите тагове, без тези на началната")
    void parentCategoryPageHasItsOwnHead() throws Exception {
        when(categoryRepository.findFirstBySlugAndShowTrueOrderByIdAsc("laptops")).thenReturn(Optional.of(category(36, "laptops")));
        when(categoryRepository.existsByParentIdAndShowTrue(36L)).thenReturn(true);

        mockMvc.perform(og("/api/og/category/36", "/category/list/laptops/36/?utm_source=fb"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(content().string(containsString("<div id=\"root\">")))
                .andExpect(content().string(containsString("<title>Категория 36 | Care Tech</title>")))
                .andExpect(content().string(containsString("<link rel=\"canonical\" href=\"" + SITE + "/category/list/laptops/36\">")))
                .andExpect(content().string(containsString("<meta property=\"og:url\" content=\"" + SITE + "/category/list/laptops/36\">")))
                .andExpect(content().string(not(containsString("Care Tech: Компютри, Принтери, Видеонаблюдение"))));
    }

    @Test
    @DisplayName("Листо → заглавие като в Category.jsx")
    void leafCategoryTitleMatchesReact() throws Exception {
        when(categoryRepository.findFirstBySlugAndShowTrueOrderByIdAsc("timers")).thenReturn(Optional.of(category(219, "timers")));
        when(categoryRepository.existsByParentIdAndShowTrue(219L)).thenReturn(false);

        mockMvc.perform(og("/api/og/category/219", "/category/timers/219"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Категория 219 - Категория</title>")));
    }

    @Test
    @DisplayName("Скрита категория → 404 с noindex и React страницата")
    void hiddenCategoryIsNotFound() throws Exception {
        noVisibleCategoryWithSlug();
        when(categoryRepository.findById(213L)).thenReturn(Optional.of(hidden(category(213, "lunch-box"))));

        mockMvc.perform(og("/api/og/category/213", "/category/lunch-box/213"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<meta name=\"robots\" content=\"noindex\">")))
                .andExpect(content().string(containsString("<div id=\"root\">")))
                .andExpect(content().string(not(containsString("rel=\"canonical\""))));
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
    @DisplayName("Без X-Original-URI → страницата, без пренасочване")
    void withoutOriginalUriNothingRedirects() throws Exception {
        when(categoryRepository.findById(219L)).thenReturn(Optional.of(category(219, "timers")));
        when(categoryRepository.existsByParentIdAndShowTrue(219L)).thenReturn(false);

        mockMvc.perform(og("/api/og/category/219", null))
                .andExpect(status().isOk());
    }

    // ── Product ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Продукт със стар slug → 301 към текущия")
    void productWithOldSlugRedirects() throws Exception {
        when(productRepository.findWithManufacturerById(15245L)).thenReturn(Optional.of(product(15245, "canyon-cable")));

        mockMvc.perform(og("/api/og/product/15245", "/product/office-pc/15245"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", SITE + "/product/canyon-cable/15245"));
    }

    @Test
    @DisplayName("Продукт без slug в URL-а → 301 към URL-а със slug")
    void productWithoutSlugRedirects() throws Exception {
        when(productRepository.findWithManufacturerById(7911L)).thenReturn(Optional.of(product(7911, "keychron-b6")));

        mockMvc.perform(og("/api/og/product/7911", "/product/7911"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", SITE + "/product/keychron-b6/7911"));
    }

    @Test
    @DisplayName("Каноничен продуктов URL → React страницата: заглавие с марката, canonical, og тагове")
    void productPageHasItsOwnHead() throws Exception {
        Product p = product(7911, "keychron-b6");
        Manufacturer keychron = new Manufacturer();
        keychron.setName("Keychron");
        p.setManufacturer(keychron);
        p.setDescriptionBg("<p>Безжична клавиатура с \"тиха\" механика</p>");
        p.setPrimaryImageUrl("https://cdn.example.com/b6.jpg");
        when(productRepository.findWithManufacturerById(7911L)).thenReturn(Optional.of(p));

        mockMvc.perform(og("/api/og/product/7911", "/product/keychron-b6/7911/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Продукт 7911 | Keychron</title>")))
                .andExpect(content().string(containsString("<link rel=\"canonical\" href=\"" + SITE + "/product/keychron-b6/7911\">")))
                .andExpect(content().string(containsString("<meta property=\"og:type\" content=\"product\">")))
                .andExpect(content().string(containsString("<meta property=\"og:title\" content=\"Продукт 7911 | Care Tech\">")))
                .andExpect(content().string(containsString("<meta property=\"og:image\" content=\"https://cdn.example.com/b6.jpg\">")))
                .andExpect(content().string(containsString("content=\"Безжична клавиатура с &quot;тиха&quot; механика\"")))
                .andExpect(content().string(containsString("/static/js/main.")));
    }

    @Test
    @DisplayName("Скрит продукт (не е AVAILABLE) → 404 с noindex")
    void hiddenProductIsNotFound() throws Exception {
        Product p = product(2418, "alphacool-apex");
        p.setStatus(ProductStatus.NOT_AVAILABLE);
        when(productRepository.findWithManufacturerById(2418L)).thenReturn(Optional.of(p));

        mockMvc.perform(og("/api/og/product/2418", "/product/alphacool-apex/2418"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<meta name=\"robots\" content=\"noindex\">")))
                .andExpect(content().string(containsString("<div id=\"root\">")));
    }

    @Test
    @DisplayName("Несъществуващ продукт → 404")
    void unknownProductIsNotFound() throws Exception {
        when(productRepository.findWithManufacturerById(any())).thenReturn(Optional.empty());

        mockMvc.perform(og("/api/og/product/999", "/product/gone/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Заявка с Accept: */* (напр. Facebook) → пак страницата")
    void anyAcceptGetsThePage() throws Exception {
        when(productRepository.findWithManufacturerById(7911L)).thenReturn(Optional.of(product(7911, "keychron-b6")));

        mockMvc.perform(get("/api/og/product/7911").accept(MediaType.ALL).header("X-Original-URI", "/product/keychron-b6/7911"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("og:title")));
    }

    // ── Without index.html ─────────────────────────────────────────────────────

    @Test
    @DisplayName("index.html не е наличен → 503 (nginx показва обикновения React)")
    void withoutShellPageIsUnavailable() throws Exception {
        when(spaShell.indexHtml()).thenReturn(Optional.empty());
        when(productRepository.findWithManufacturerById(7911L)).thenReturn(Optional.of(product(7911, "keychron-b6")));

        mockMvc.perform(og("/api/og/product/7911", "/product/keychron-b6/7911"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("index.html не е наличен и продуктът го няма → пак 404")
    void withoutShellNotFoundIsStillNotFound() throws Exception {
        when(spaShell.indexHtml()).thenReturn(Optional.empty());
        when(productRepository.findWithManufacturerById(any())).thenReturn(Optional.empty());

        mockMvc.perform(og("/api/og/product/999", "/product/gone/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("http-equiv=\"refresh\"")));
    }
}
