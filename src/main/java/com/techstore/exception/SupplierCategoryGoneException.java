package com.techstore.exception;

/**
 * The supplier no longer has a category we still carry — its API rejects the id outright. Not a failed
 * fetch: asking again cannot succeed, and nothing we held for the category is wrong because of it.
 */
public class SupplierCategoryGoneException extends RuntimeException {

    private final Long categoryId;

    public SupplierCategoryGoneException(Long categoryId, String message) {
        super(message);
        this.categoryId = categoryId;
    }

    public Long getCategoryId() {
        return categoryId;
    }
}
