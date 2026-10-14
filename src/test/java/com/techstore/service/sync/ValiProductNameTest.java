package com.techstore.service.sync;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VALI's own products came with its brand in the name ("Настолен компютър VALI OFFICE BASIC").
 * The shop sells them without it, and the VALI sync rewrites names every night, so the brand
 * is removed there — a one-off SQL fix alone would be undone by the next run.
 */
class ValiProductNameTest {

    @Test
    @DisplayName("Removes VALI from the middle of a name")
    void middleOfName() {
        assertEquals("Настолен компютър OFFICE BASIC",
                ValiSyncService.removeValiBrand("Настолен компютър VALI OFFICE BASIC"));
    }

    @Test
    @DisplayName("Collapses the double space VALI sends in some English names")
    void doubleSpace() {
        assertEquals("PC Desktop GAMING AMD RYZEN 5 7500F",
                ValiSyncService.removeValiBrand("PC Desktop  VALI GAMING AMD RYZEN 5 7500F"));
    }

    @Test
    @DisplayName("Leaves no stray space before a comma")
    void beforeComma() {
        assertEquals("Хартиени пликчета за CD 50 бр. комплект, Бял",
                ValiSyncService.removeValiBrand("Хартиени пликчета за CD 50 бр. комплект VALI, Бял"));
    }

    @Test
    @DisplayName("Removes VALI at the start of a name")
    void startOfName() {
        assertEquals("OFFICE GT", ValiSyncService.removeValiBrand("VALI OFFICE GT"));
    }

    @Test
    @DisplayName("Leaves words that only contain the letters alone")
    void wholeWordOnly() {
        assertEquals("Lavalier микрофон", ValiSyncService.removeValiBrand("Lavalier микрофон"));
        assertEquals("Validated, Certified", ValiSyncService.removeValiBrand("Validated, Certified"));
    }

    @Test
    @DisplayName("Leaves names without VALI untouched, double spaces included")
    void otherNamesUntouched() {
        assertEquals("Монитор  ACER 24\"", ValiSyncService.removeValiBrand("Монитор  ACER 24\""));
        assertNull(ValiSyncService.removeValiBrand(null));
    }

    @Test
    @DisplayName("Keeps a name that is nothing but VALI rather than blanking it")
    void neverBlank() {
        assertEquals("VALI", ValiSyncService.removeValiBrand("VALI"));
    }
}
