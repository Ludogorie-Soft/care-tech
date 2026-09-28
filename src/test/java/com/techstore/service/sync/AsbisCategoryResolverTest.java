package com.techstore.service.sync;

import com.techstore.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The ASBIS product sync matched only visible categories, so a product whose ASBIS subcategory the shop
 * hides fell back to the root: after "Смарт устройства" (500) became visible, 225 products piled up there,
 * smartwatches and IP cameras among them. A hidden subcategory that is an alias of a visible category now
 * sends its products to that category.
 */
class AsbisCategoryResolverTest {

    // Shop tree (ids as in production where it matters)
    private final Category smartDevices = category(500, "Смарт устройства", null, true);
    private final Category smartWatchAsbis = category(762, "Смарт часовник", smartDevices, false);
    private final Category smartSensors = category(768, "Смарт сензори за дома", smartDevices, false);
    private final Category smartLighting = category(763, "Смарт LED осветление", smartDevices, true);

    private final Category watchesParent = category(153, "Смарт часовници, телефони и аксесоари", null, true);
    private final Category smartWatches = category(156, "Смарт часовници", watchesParent, true);
    private final Category fitnessBands = category(157, "Фитнес гривни", watchesParent, false);

    private final Category cctv = category(230, "Видеонаблюдение", null, true);
    private final Category hiddenCctvBranch = category(233, "NDAA, NIS2", cctv, false);
    private final Category ndaaCameras = category(964, "IP камери NDAA", hiddenCctvBranch, true);

    private final Category displaysHidden = category(362, "Дисплеи", null, false);
    private final Category ledMonitorAsbis = category(363, "LED Монитор", displaysHidden, false);
    private final Category monitors = category(50, "Монитори", null, true);

    private final Category components = category(1, "Компютърни компоненти", null, true);
    private final Category componentsHiddenTwin = category(392, "Компютърни компоненти", null, false);

    private AsbisCategoryResolver resolver(Category... extra) {
        List<Category> all = new java.util.ArrayList<>(List.of(smartDevices, smartWatchAsbis, smartSensors,
                smartLighting, watchesParent, smartWatches, fitnessBands, cctv, hiddenCctvBranch, ndaaCameras,
                displaysHidden, ledMonitorAsbis, monitors, components, componentsHiddenTwin));
        all.addAll(List.of(extra));
        return new AsbisCategoryResolver(all);
    }

    private static Category category(long id, String name, Category parent, boolean show) {
        Category c = new Category();
        c.setId(id);
        c.setNameBg(name);
        c.setParent(parent);
        c.setShow(show);
        return c;
    }

    @Test
    @DisplayName("visible subcategory under a visible root is used as is")
    void visibleChild() {
        assertEquals(763L, resolver().resolve("Смарт устройства", "Смарт LED осветление").getId());
    }

    @Test
    @DisplayName("hidden subcategory without an alias falls back to the root, as before")
    void hiddenChildWithoutAliasFallsBackToRoot() {
        assertEquals(500L, resolver().resolve("Смарт устройства", "Смарт сензори за дома").getId());
        assertEquals(500L, resolver().resolve("Смарт устройства", "Смарт часовник").getId());
    }

    @Test
    @DisplayName("hidden subcategory aliased to a visible category sends the product there")
    void hiddenChildAliasedToVisibleTarget() {
        smartWatchAsbis.setAliasOf(smartWatches);

        assertEquals(156L, resolver().resolve("Смарт устройства", "Смарт часовник").getId());
    }

    @Test
    @DisplayName("an alias to a hidden category, or to one under a hidden parent, is not followed")
    void aliasToInvisibleTargetIsIgnored() {
        smartWatchAsbis.setAliasOf(fitnessBands);
        assertEquals(500L, resolver().resolve("Смарт устройства", "Смарт часовник").getId());

        smartWatchAsbis.setAliasOf(ndaaCameras); // visible itself, parent 233 is hidden
        assertEquals(500L, resolver().resolve("Смарт устройства", "Смарт часовник").getId());
    }

    @Test
    @DisplayName("a visible category under a hidden parent is never used")
    void visibleCategoryUnderHiddenParent() {
        assertNull(resolver().resolve("NDAA, NIS2", "IP камери NDAA"));
    }

    @Test
    @DisplayName("hidden root: skipped as before, unless its subcategory is an alias")
    void hiddenRoot() {
        assertNull(resolver().resolve("Дисплеи", "LED Монитор"));

        ledMonitorAsbis.setAliasOf(monitors);
        assertEquals(50L, resolver().resolve("Дисплеи", "LED Монитор").getId());
    }

    @Test
    @DisplayName("the type alone can name a visible root")
    void typeAsRoot() {
        assertEquals(50L, resolver().resolve("Other", "Монитори").getId());
    }

    @Test
    @DisplayName("of two roots with the same name the visible one is taken")
    void duplicateRootNamesPreferVisible() {
        Category motherboardAsbis = category(414, "Дънна платка настолна", components, false);
        Category motherboards = category(2, "Дънни платки", components, true);
        motherboardAsbis.setAliasOf(motherboards);

        assertEquals(2L, resolver(motherboardAsbis, motherboards)
                .resolve("Компютърни компоненти", "Дънна платка настолна").getId());
    }

    @Test
    @DisplayName("names match regardless of case and surrounding spaces")
    void caseAndWhitespace() {
        assertEquals(763L, resolver().resolve("  смарт устройства ", "СМАРТ LED ОСВЕТЛЕНИЕ").getId());
    }

    @Test
    @DisplayName("missing or unknown names give no category")
    void unknownNames() {
        AsbisCategoryResolver resolver = resolver();
        assertNull(resolver.resolve(null, null));
        assertNull(resolver.resolve(" ", ""));
        assertNull(resolver.resolve("Other", "Accessories for Staged"));
        assertEquals(500L, resolver.resolve("Смарт устройства", null).getId());
    }
}
