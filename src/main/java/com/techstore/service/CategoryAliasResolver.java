package com.techstore.service;

import com.techstore.entity.Category;
import com.techstore.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves an alias category (categories.alias_of_id) to the category that holds its products.
 * An alias whose target is hidden resolves to itself: it has no products of its own, so the page is
 * empty rather than showing products of a category the shop has switched off.
 */
@Component
@RequiredArgsConstructor
public class CategoryAliasResolver {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Long resolve(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .map(c -> {
                    Category target = c.getAliasOf();
                    if (target == null || Boolean.FALSE.equals(target.getShow())) {
                        return c.getId();
                    }
                    return target.getId();
                })
                .orElse(categoryId);
    }
}
