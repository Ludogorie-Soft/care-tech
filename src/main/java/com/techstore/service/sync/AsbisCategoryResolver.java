package com.techstore.service.sync;

import com.techstore.entity.Category;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Finds the shop category for an ASBIS product from its feed category (L1, "ProductCategory") and type
 * (L2, "ProductType"): the L2 child under the L1 root first, then L2 as a root, then the L1 root.
 *
 * <p>Only categories visible all the way up to their root are used, so a product never lands where the
 * shop does not show it. A hidden category that is an alias ({@code categories.alias_of_id}) of a visible
 * one sends its products to that target. That is how an ASBIS subcategory the shop does not show on its
 * own ("Смарт устройства › Смарт часовник") still reaches the right shelf ("Смарт часовници") instead of
 * falling back to its root.
 *
 * <p>Everything is worked out from ids when it is built: the product sync clears the persistence context
 * every 100 products, after which lazy parent and alias references can no longer be read.
 */
final class AsbisCategoryResolver {

    private static final int MAX_DEPTH = 20;

    private final Map<String, Category> rootsByName = new HashMap<>();
    private final Map<String, Category> childrenByParentAndName = new HashMap<>();
    /** Category id → the category its products go to, or no entry when it cannot hold products. */
    private final Map<Long, Category> destinationById = new HashMap<>();

    AsbisCategoryResolver(Collection<Category> categories) {
        Map<Long, Category> byId = new HashMap<>();
        Map<Long, Long> parentIdById = new HashMap<>();
        Map<Long, Long> aliasIdById = new HashMap<>();
        for (Category c : categories) {
            if (c.getId() == null) continue;
            byId.put(c.getId(), c);
            // getId() on a lazy reference does not load it
            if (c.getParent() != null) parentIdById.put(c.getId(), c.getParent().getId());
            if (c.getAliasOf() != null) aliasIdById.put(c.getId(), c.getAliasOf().getId());
        }

        for (Category c : byId.values()) {
            if (visibleUpToRoot(c.getId(), byId, parentIdById)) {
                destinationById.put(c.getId(), c);
            } else {
                Long aliasId = aliasIdById.get(c.getId());
                if (aliasId != null && visibleUpToRoot(aliasId, byId, parentIdById)) {
                    destinationById.put(c.getId(), byId.get(aliasId));
                }
            }

            String name = key(c.getNameBg());
            if (name == null) continue;
            Long parentId = parentIdById.get(c.getId());
            if (parentId == null) {
                rootsByName.merge(name, c, AsbisCategoryResolver::preferVisibleThenLowestId);
            } else {
                childrenByParentAndName.merge(parentId + ":::" + name, c, AsbisCategoryResolver::preferVisibleThenLowestId);
            }
        }
    }

    /** The category for a product, or {@code null} when neither name leads to a category the shop shows. */
    Category resolve(String productCategory, String productType) {
        String l1 = key(productCategory);
        String l2 = key(productType);

        if (l1 != null && l2 != null) {
            Category root = rootsByName.get(l1);
            if (root != null) {
                Category found = destination(childrenByParentAndName.get(root.getId() + ":::" + l2));
                if (found != null) return found;
            }
        }
        if (l2 != null) {
            Category found = destination(rootsByName.get(l2));
            if (found != null) return found;
        }
        return l1 != null ? destination(rootsByName.get(l1)) : null;
    }

    private Category destination(Category category) {
        return category == null ? null : destinationById.get(category.getId());
    }

    private static boolean visibleUpToRoot(Long id, Map<Long, Category> byId, Map<Long, Long> parentIdById) {
        Long current = id;
        for (int depth = 0; current != null && depth < MAX_DEPTH; depth++) {
            Category c = byId.get(current);
            if (c == null || !Boolean.TRUE.equals(c.getShow())) return false;
            current = parentIdById.get(current);
        }
        return current == null;
    }

    private static String key(String name) {
        if (name == null || name.isBlank()) return null;
        return name.toLowerCase(Locale.ROOT).trim();
    }

    /** Two categories sharing a name: a visible one wins, otherwise the lower id. */
    private static Category preferVisibleThenLowestId(Category a, Category b) {
        boolean aVisible = Boolean.TRUE.equals(a.getShow());
        boolean bVisible = Boolean.TRUE.equals(b.getShow());
        if (aVisible != bVisible) {
            return aVisible ? a : b;
        }
        return a.getId() <= b.getId() ? a : b;
    }
}
