package com.techstore.service.sync;

import com.techstore.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The Most feed carries 222 distinct (category, subcategory) pairs against only 29
 * categories. Mapping on the category alone collapsed them into at most 29 buckets, which
 * is how 150 phone accessories ended up filed as phones and 147 power supplies as cases.
 */
class MostCategoryResolutionTest {

    @ParameterizedTest
    @CsvSource({
            "GSM,        Accessories,      Други мобилни аксесоари",
            "CASE,       PSU,              Захранвания",
            "NOTEBOOK,   NB Accessories,   Аксесоари за лаптопи/таблети",
            "M - MEDIA,  Projectors,       Проектори",
            "M - MEDIA,  Headset and mic,  Слушалки",
            "LAN,        LAN Cable,        Мрежови кабели",
            "MONITOR,    TV,               Телевизори",
            "UPS,        Power Bank,       Външни батерии"
    })
    @DisplayName("a mapped pair wins over the category")
    void pairBeatsCategory(String category, String subcategory, String expected) {
        assertEquals(expected, MostSyncService.resolveTargetCategoryName(category, subcategory));
    }

    @ParameterizedTest
    @CsvSource({
            "NOTEBOOK,  NB LENOVO,   Лаптопи",
            "NOTEBOOK,  NB ASUS,     Лаптопи",
            "MONITOR,   LENOVO,      Монитори",
            "GSM,       Smart,       Мобилни телефони",
            "GSM,       Feature,     Мобилни телефони",
            "CASE,      CASE without PSU, Кутии за компютри"
    })
    @DisplayName("an unmapped pair falls back to the category, as before")
    void unmappedPairFallsBackToCategory(String category, String subcategory, String expected) {
        assertEquals(expected, MostSyncService.resolveTargetCategoryName(category, subcategory));
    }

    @Test
    @DisplayName("a missing or blank subcategory falls back to the category")
    void blankSubcategoryFallsBack() {
        assertEquals("Лаптопи", MostSyncService.resolveTargetCategoryName("NOTEBOOK", null));
        assertEquals("Лаптопи", MostSyncService.resolveTargetCategoryName("NOTEBOOK", ""));
        assertEquals("Лаптопи", MostSyncService.resolveTargetCategoryName("NOTEBOOK", "   "));
    }

    @Test
    @DisplayName("an unknown category still maps to nothing")
    void unknownCategoryMapsToNothing() {
        assertNull(MostSyncService.resolveTargetCategoryName("SOFTWARE", "Anything"));
        assertNull(MostSyncService.resolveTargetCategoryName("NO SUCH CATEGORY", null));
        assertNull(MostSyncService.resolveTargetCategoryName(null, "Accessories"));
        assertNull(MostSyncService.resolveTargetCategoryName("", "Accessories"));
    }

    @Test
    @DisplayName("the subcategory alone never decides — 'Accessories' means different things per category")
    void subcategoryIsOnlyMeaningfulWithItsCategory() {
        // Both feeds say "Accessories", and they are not the same kind of accessory.
        assertEquals("Други мобилни аксесоари",
                MostSyncService.resolveTargetCategoryName("GSM", "Accessories"));
        // CASE|Accessories is not mapped, so it stays with computer cases rather than
        // drifting into the phone bucket.
        assertEquals("Кутии за компютри",
                MostSyncService.resolveTargetCategoryName("CASE", "Accessories"));
    }

    @Test
    @DisplayName("pairs held back on purpose keep their old destination")
    void deliberatelyUnmappedPairsUnchanged() {
        // A genuinely mixed bag — needs per-product classification, not a blanket move.
        assertEquals("Консумативи(тонери) за лазерни устройства",
                MostSyncService.resolveTargetCategoryName("HP", "PSG Accessories"));
    }

    // ── name overrides take precedence over the pair mapping ──────────────────

    @ParameterizedTest
    @CsvSource({
            "Аксесоари за лаптопи/таблети, HP NOTEBOOK ЧАНТА 15.6,        Чанти за лаптопи",
            "Аксесоари за лаптопи/таблети, LENOVO ЗАРЯДНО 65W,            Зарядни за лаптопи",
            "Лаптопи,                      ASUS BACKPACK 15-17,           Чанти за лаптопи"
    })
    @DisplayName("a name that says bag or charger beats the generic accessories bucket")
    void nameOverrideBeatsPairMapping(String target, String productName, String expected) {
        assertEquals(expected, MostSyncService.applyLaptopNameOverride(target, productName));
    }

    @Test
    @DisplayName("without a matching name the pair mapping stands")
    void keepsTargetWhenNoNameMatches() {
        assertEquals("Аксесоари за лаптопи/таблети",
                MostSyncService.applyLaptopNameOverride("Аксесоари за лаптопи/таблети", "SOME UNRECOGNISED THING"));
        assertEquals("Лаптопи",
                MostSyncService.applyLaptopNameOverride("Лаптопи", "LENOVO IP3 SLIM 15"));
    }

    @Test
    @DisplayName("overrides only apply to laptop destinations, never to unrelated ones")
    void overridesDoNotLeakIntoOtherCategories() {
        // "чанта" in the name must not drag a phone accessory out of its own category
        assertEquals("Други мобилни аксесоари",
                MostSyncService.applyLaptopNameOverride("Други мобилни аксесоари", "ЧАНТА ЗА ТЕЛЕФОН"));
        assertEquals("Захранвания",
                MostSyncService.applyLaptopNameOverride("Захранвания", "PSU 750W"));
    }

    @Test
    @DisplayName("a null product name is tolerated")
    void toleratesNullName() {
        assertEquals("Лаптопи", MostSyncService.applyLaptopNameOverride("Лаптопи", null));
    }

    @ParameterizedTest
    @CsvSource({
            "ACER AG PROTECT FILM B1-71X,      Защитни фолиа и стъкла",
            "ACER AGLR PROTECT FILM A1-830,    Защитни фолиа и стъкла",
            "NACON SCREEN PROTECTOR SWITCH,    Защитни фолиа и стъкла",
            "ACER PORTFOLIO CASE W3-810 GRY,   Аксесоари за лаптопи/таблети",
            "ASUS TRICOVER ME180A BLACK,       Аксесоари за лаптопи/таблети"
    })
    @DisplayName("a screen protector leaves the accessories bucket; a case stays in it")
    void screenProtectorsGoToTheirOwnCategory(String productName, String expected) {
        // Тези два override-а бяха причината 6-те фолиа ACER да стоят в кат. 47
        // вместо в „Защитни фолиа и стъкла" (165). Низът трябва да съвпада с
        // categories.name_bg — резолвва се по име (MostSyncService:986),
        // затова скрипт 53 и този низ вървят заедно.
        assertEquals(expected,
                MostSyncService.applyLaptopNameOverride("Аксесоари за лаптопи/таблети", productName));
    }

    // ── step 2a: LAN and FAN out of the hidden categories ─────────────────────

    @ParameterizedTest
    @CsvSource({
            "LAN,  WL Router,                Безжични рутери",
            "LAN,  LAN Switch,               Суичове - неуправляеми",
            "LAN,  WL Card / USB / Device,   Безжични адаптери",
            "LAN,  LAN Card,                 Мрежови карти",
            "LAN,  Bluetooth,                Блутут адаптери",
            "LAN,  LAN Accessories,          Рутери и мрежово оборудване",
            "LAN,  Other,                    Рутери и мрежово оборудване",
            "FAN,  CASE fan,                 Вентилатори",
            "FAN,  CPU Cooler,               Охладители за процесори",
            "FAN,  Water Cooler,             Водно охлаждане",
            "FAN,  Others,                   Вентилатори"
    })
    @DisplayName("LAN and FAN pairs reach visible categories instead of the hidden ones")
    void lanAndFanPairsReachVisibleCategories(String category, String subcategory, String expected) {
        assertEquals(expected, MostSyncService.resolveTargetCategoryName(category, subcategory));
    }

    @Test
    @DisplayName("cooling pairs reach the specific categories, not the generic fan one")
    void coolingPairsReachSpecificCategories() {
        // Both of these were missed on the first pass because the search for candidate
        // categories used '%охлажд%', which does not match "Охладители за процесори".
        assertEquals("Охладители за процесори", MostSyncService.resolveTargetCategoryName("FAN", "CPU Cooler"));
        assertEquals("Термо пасти и подложки", MostSyncService.resolveTargetCategoryName("FAN", "Thermal Grease"));
    }

    @Test
    @DisplayName("tablets go to Таблети now that script 48 made it visible")
    void tabletsReachTheirOwnCategory() {
        assertEquals("Таблети", MostSyncService.resolveTargetCategoryName("NOTEBOOK", "Tablet LENOVO"));
    }

    @Test
    @DisplayName("an unmapped LAN subcategory still falls back to the old destination")
    void unmappedLanStillFallsBack() {
        assertEquals("Мрежов хардуер", MostSyncService.resolveTargetCategoryName("LAN", "Something New"));
    }

    @Test
    @DisplayName("a non-unique category name resolves to the visible copy, then the lowest id")
    void nonUniqueCategoryNameResolvesToVisibleThenLowestId() {
        // Parameter sync used findByNameBg, which throws on a non-unique name — 15 of the Most
        // targets ("Монитори", "Процесори"...) are, so those products' parameters were skipped.
        Map<String, Category> index = MostSyncService.indexCategoriesByName(List.of(
                category(517L, "Монитори", false),
                category(50L, "Монитори", true),
                category(964L, "Монитори", true),
                category(12L, "Процесори", false),
                category(7L, "Процесори", false)));

        assertEquals(50L, index.get("монитори").getId());
        assertEquals(7L, index.get("процесори").getId());
    }

    private static Category category(Long id, String nameBg, boolean visible) {
        Category c = new Category();
        c.setId(id);
        c.setNameBg(nameBg);
        c.setShow(visible);
        return c;
    }
}
