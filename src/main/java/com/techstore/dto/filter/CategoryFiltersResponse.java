package com.techstore.dto.filter;

import java.util.List;

/**
 * Filter panel of a category: every group with its values and counts for the current selection.
 * A count is what the shopper gets by ticking that value on top of everything selected in the other
 * groups. Selected values are always present, also with a count of 0, so they can be unticked.
 *
 * @param total products matching the whole selection
 */
public record CategoryFiltersResponse(Long categoryId,
                                      long total,
                                      List<Group> groups,
                                      List<Option> manufacturers) {

    /** @param visible FALSE only in the admin view, for a group switched off in this category */
    public record Group(Long attributeId, String slug, String name, String unit, String type,
                        boolean visible, List<Option> values) {
    }

    public record Option(Long id, String name, long count, boolean selected) {
    }
}
