-- V32: Add alias_of_id to categories to support "virtual" categories that
-- transparently proxy products from another category.
-- A category with alias_of_id set has no products of its own; the backend
-- resolves the alias before querying products.

ALTER TABLE categories
    ADD COLUMN alias_of_id BIGINT,
    ADD CONSTRAINT fk_category_alias_of
        FOREIGN KEY (alias_of_id) REFERENCES categories(id) ON DELETE SET NULL;

CREATE INDEX idx_categories_alias_of_id ON categories(alias_of_id);
