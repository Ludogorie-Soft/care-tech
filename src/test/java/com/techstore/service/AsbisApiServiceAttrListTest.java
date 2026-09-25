package com.techstore.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ASBIS repeats an attribute name inside one product with a different value each time (1,170 times
 * in 669 products of ProductList.xml); a plain map kept only the last one.
 */
class AsbisApiServiceAttrListTest {

    @Test
    @DisplayName("a repeated name keeps every distinct value, in feed order")
    void repeatedNames() throws Exception {
        Map<String, List<String>> attributes = AsbisApiService.extractAttrList(attrList("""
                <AttrList>
                  <element Name="LAN" Value="4 x 10Base-T/100Base-TX/1000Base-T"/>
                  <element Name="Височина" Value="16 мм"/>
                  <element Name="LAN" Value="4 (RJ-45)"/>
                  <element Name="Височина" Value="38 мм"/>
                  <element Name="Височина" Value="16 мм"/>
                  <element Name="Външен цвят" Value="Черен"/>
                  <element Name="" Value="x"/>
                  <element Name="Празно" Value=""/>
                </AttrList>"""));

        assertEquals(List.of("4 x 10Base-T/100Base-TX/1000Base-T", "4 (RJ-45)"), attributes.get("LAN"));
        assertEquals(List.of("16 мм", "38 мм"), attributes.get("Височина"));
        assertEquals(List.of("Черен"), attributes.get("Външен цвят"));
        assertEquals(List.of("LAN", "Височина", "Външен цвят"), List.copyOf(attributes.keySet()));
    }

    private static Element attrList(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .getDocumentElement();
    }
}
