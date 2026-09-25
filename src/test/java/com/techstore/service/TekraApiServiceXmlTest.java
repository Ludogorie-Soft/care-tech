package com.techstore.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Parsing of the TEKRA product feed ({@code action=browse&feed=1}). */
class TekraApiServiceXmlTest {

    private final TekraApiService service = new TekraApiService(null);

    @Test
    @DisplayName("a repeated property tag keeps every value; other tags keep the last one")
    void repeatedTags() {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <items>
                  <item>
                    <sku>DS-2CD1043G2-I</sku>
                    <name>Камера</name>
                    <name>IP камера</name>
                    <prop_cvjat>Бял</prop_cvjat>
                    <prop_cvjat>Черен</prop_cvjat>
                    <prop_cvjat>Сив</prop_cvjat>
                    <prop_korpus>Булет</prop_korpus>
                    <prop_merna>бр.</prop_merna>
                  </item>
                </items>
                """;

        List<Map<String, Object>> products = service.parseProductsFromXML(xml);

        assertEquals(1, products.size());
        Map<String, Object> product = products.get(0);
        assertEquals(List.of("Бял", "Черен", "Сив"), product.get("prop_cvjat"));
        assertEquals("Булет", product.get("prop_korpus"));
        assertEquals("IP камера", product.get("name"));
        assertEquals("DS-2CD1043G2-I", product.get("sku"));
    }

    @Test
    @DisplayName("escaped <br/> inside a value stays text for the sync to split")
    void escapedLineBreak() {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <items><item><sku>1</sku><prop_stepen_na_zashtita>IP55&lt;br/&gt;IK07</prop_stepen_na_zashtita></item></items>
                """;

        assertEquals("IP55<br/>IK07", service.parseProductsFromXML(xml).get(0).get("prop_stepen_na_zashtita"));
    }
}
