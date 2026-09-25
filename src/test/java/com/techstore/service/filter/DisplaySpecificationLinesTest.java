package com.techstore.service.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ASBIS sends feature lists joined with {@code <br/>} and wrapped in {@code <b>}; the product page
 * shows values as text, so 606 visible products showed the tags literally.
 */
class DisplaySpecificationLinesTest {

    @Test
    @DisplayName("a <br/>-joined list becomes one line per item")
    void splitsLineBreaks() {
        assertEquals(List.of("Socket AM4", "Socket 1700", "Socket AM5"),
                DisplaySpecificationService.displayLines("Socket AM4 <br/>Socket 1700<BR>Socket AM5 <br />"));
    }

    @Test
    @DisplayName("formatting tags go, their text stays")
    void dropsFormattingTags() {
        assertEquals(List.of("Storage Controller:", "Supports: Ниво 0, Ниво 1"),
                DisplaySpecificationService.displayLines("<b>Storage Controller:</b><br/>Supports: Ниво 0, Ниво 1"));
        assertEquals(List.of("up to 1 Gbps RJ-45"),
                DisplaySpecificationService.displayLines("up to 1 Gbps</b> RJ-45"));
        assertEquals(List.of("2155451"), DisplaySpecificationService.displayLines(
                "<a href=\"https://eprel.ec.europa.eu/screen/product/electronicdisplays/2155451\">2155451</a>"));
        assertEquals(List.of("Standard warranty"), DisplaySpecificationService.displayLines(
                "<img src=\"http://abonline.pl/5years.jpg\" border=\"0\" />Standard warranty"));
    }

    @Test
    @DisplayName("the malformed </br> separates lines too")
    void closingLineBreak() {
        assertEquals(List.of("Multi-Protection Safety System", "3 intensity levels"),
                DisplaySpecificationService.displayLines("Multi-Protection Safety System</br>3 intensity levels"));
    }

    @Test
    @DisplayName("comparison signs in plain text are not mistaken for tags")
    void keepsComparisons() {
        assertEquals(List.of("<5 ms и >1 W"), DisplaySpecificationService.displayLines("<5 ms и >1 W"));
    }

    @Test
    @DisplayName("a plain value is one line")
    void plainValue() {
        assertEquals(List.of("16 GB"), DisplaySpecificationService.displayLines("  16 GB "));
        assertEquals(List.of(), DisplaySpecificationService.displayLines("<br/>"));
        assertEquals(List.of(), DisplaySpecificationService.displayLines(null));
    }
}
