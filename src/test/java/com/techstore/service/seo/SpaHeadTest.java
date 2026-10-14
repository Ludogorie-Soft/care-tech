package com.techstore.service.seo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SpaHead — таговете на страницата в index.html от продукция")
class SpaHeadTest {

    private static String indexHtml;

    private static final SpaHead.Tags PRODUCT = new SpaHead.Tags(
            "Keychron B6 | Keychron", "Keychron B6 | Care Tech", "Безжична клавиатура",
            "https://www.caretech.bg/product/keychron-b6/7911", "https://cdn.example.com/b6.jpg",
            "Keychron B6", "product", true);

    @BeforeAll
    static void load() throws IOException {
        try (InputStream in = SpaHeadTest.class.getResourceAsStream("/spa/index.html")) {
            indexHtml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int count(String html, String regex) {
        Matcher m = Pattern.compile(regex).matcher(html);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    private static String head(String html) {
        return html.substring(0, html.indexOf("</head>"));
    }

    @Test
    @DisplayName("Всеки таг на страницата е точно веднъж, с нейната стойност")
    void everyPageTagOnce() {
        String html = SpaHead.render(indexHtml, PRODUCT);

        assertThat(count(html, "<title>")).isEqualTo(1);
        assertThat(count(html, "name=\"description\"")).isEqualTo(1);
        assertThat(count(html, "name=\"robots\"")).isEqualTo(1);
        assertThat(count(html, "rel=\"canonical\"")).isEqualTo(1);
        assertThat(count(html, "property=\"og:title\"")).isEqualTo(1);
        assertThat(count(html, "property=\"og:url\"")).isEqualTo(1);
        assertThat(count(html, "property=\"og:image\"")).isEqualTo(1);
        assertThat(count(html, "name=\"twitter:title\"")).isEqualTo(1);
        assertThat(html).contains("<title>Keychron B6 | Keychron</title>",
                "<link rel=\"canonical\" href=\"https://www.caretech.bg/product/keychron-b6/7911\">",
                "<meta property=\"og:type\" content=\"product\">");
    }

    @Test
    @DisplayName("Нищо от началната страница не остава: заглавие, описание, ключови думи, og:url")
    void homepageDefaultsAreGone() {
        String html = SpaHead.render(indexHtml, PRODUCT);

        assertThat(html).doesNotContain("Care Tech – Продажба и ремонт на лаптопи",
                "Care Tech: Компютри, Принтери, Видеонаблюдение",
                "name=\"keywords\"",
                "content=\"https://www.caretech.bg/\"");
    }

    @Test
    @DisplayName("Останалото в index.html е непокътнато: JSON-LD на фирмата, gtag, React скриптът и тялото")
    void restOfIndexHtmlIsKept() {
        String html = SpaHead.render(indexHtml, PRODUCT);

        assertThat(html).contains("application/ld+json", "googletagmanager.com/gtag/js", "/static/js/main.",
                "name=\"viewport\"", "name=\"geo.region\"");
        assertThat(html.substring(html.indexOf("</head>"))).isEqualTo(indexHtml.substring(indexHtml.indexOf("</head>")));
    }

    @Test
    @DisplayName("Кавички и < > в имената се ескейпват")
    void valuesAreEscaped() {
        SpaHead.Tags tags = new SpaHead.Tags("Кабел \"USB-C\" <2m>", "x", "a & b", null, null, null, "website", true);

        String head = head(SpaHead.render(indexHtml, tags));

        assertThat(head).contains("<title>Кабел &quot;USB-C&quot; &lt;2m&gt;</title>",
                "<meta name=\"description\" content=\"a &amp; b\">");
    }

    @Test
    @DisplayName("Страница за 404: noindex, без canonical и og:url")
    void notFoundIsNotIndexable() {
        SpaHead.Tags tags = new SpaHead.Tags("Страницата не е намерена | Care Tech", "Care Tech", "…", null,
                "https://www.caretech.bg/logo.png", "Care Tech", "website", false);

        String head = head(SpaHead.render(indexHtml, tags));

        assertThat(head).contains("<meta name=\"robots\" content=\"noindex\">")
                .doesNotContain("rel=\"canonical\"", "og:url");
    }

    @Test
    @DisplayName("Без </head> → грешка, не счупена страница")
    void rejectsHtmlWithoutHead() {
        assertThatThrownBy(() -> SpaHead.render("<html><body></body></html>", PRODUCT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
