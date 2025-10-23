

-- Основен индекс за products таблицата
CREATE INDEX IF NOT EXISTS idx_products_active_show
    ON products(active, show_flag)
    WHERE active = true AND show_flag = true;

-- Индекс за цена филтриране
CREATE INDEX IF NOT EXISTS idx_products_final_price
    ON products(final_price);

-- Индекс за featured продукти
CREATE INDEX IF NOT EXISTS idx_products_featured
    ON products(featured)
    WHERE featured = true;

-- Индекс за продукти на промоция
CREATE INDEX IF NOT EXISTS idx_products_discount
    ON products(discount)
    WHERE discount > 0;

-- Индекс за категория и производител
CREATE INDEX IF NOT EXISTS idx_products_category_manufacturer
    ON products(category_id, manufacturer_id);


-- 2. Full-text search индекси
-- ----------------------------------------------------------------------------

-- GIN индекс за full-text search на български
CREATE INDEX IF NOT EXISTS idx_products_fts_bg
    ON products USING gin(
        to_tsvector('simple',
            coalesce(name_bg, '') || ' ' ||
            coalesce(description_bg, '') || ' ' ||
            coalesce(model, '') || ' ' ||
            coalesce(reference_number, '')
        )
    );

-- GIN индекс за full-text search на английски
CREATE INDEX IF NOT EXISTS idx_products_fts_en
    ON products USING gin(
        to_tsvector('simple',
            coalesce(name_en, '') || ' ' ||
            coalesce(description_en, '') || ' ' ||
            coalesce(model, '') || ' ' ||
            coalesce(reference_number, '')
        )
    );


-- 3. Индекси за параметри (КРИТИЧНИ за производителност!)
-- ----------------------------------------------------------------------------

-- Основен индекс за product_parameters по product_id
CREATE INDEX IF NOT EXISTS idx_product_parameters_product_id
    ON product_parameters(product_id);

-- Composite индекс за JOIN операции
CREATE INDEX IF NOT EXISTS idx_product_parameters_param_option
    ON product_parameters(parameter_id, parameter_option_id);

-- Composite индекс за оптимално филтриране (препокрива всички колони)
CREATE INDEX IF NOT EXISTS idx_product_params_lookup
    ON product_parameters(product_id, parameter_id, parameter_option_id);

-- Индекс за обратни lookup-и
CREATE INDEX IF NOT EXISTS idx_product_parameters_option_id
    ON product_parameters(parameter_option_id);


-- 4. Индекси за parameters и parameter_options
-- ----------------------------------------------------------------------------

-- Индекс за параметри по категория
CREATE INDEX IF NOT EXISTS idx_parameters_category
    ON parameters(category_id);

-- Индекс за търсене по име (case-insensitive)
CREATE INDEX IF NOT EXISTS idx_parameters_name_bg_lower
    ON parameters(LOWER(name_bg));

CREATE INDEX IF NOT EXISTS idx_parameters_name_en_lower
    ON parameters(LOWER(name_en));

-- Индекс за sort order
CREATE INDEX IF NOT EXISTS idx_parameters_sort_order
    ON parameters(sort_order);

-- Индекси за parameter_options
CREATE INDEX IF NOT EXISTS idx_parameter_options_parameter
    ON parameter_options(parameter_id);

CREATE INDEX IF NOT EXISTS idx_parameter_options_name_bg_lower
    ON parameter_options(LOWER(name_bg));

CREATE INDEX IF NOT EXISTS idx_parameter_options_name_en_lower
    ON parameter_options(LOWER(name_en));

CREATE INDEX IF NOT EXISTS idx_parameter_options_sort_order
    ON parameter_options(sort_order);


-- 5. Индекси за categories и manufacturers
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_categories_name_bg_lower
    ON categories(LOWER(name_bg));

CREATE INDEX IF NOT EXISTS idx_categories_name_en_lower
    ON categories(LOWER(name_en));

CREATE INDEX IF NOT EXISTS idx_manufacturers_name_lower
    ON manufacturers(LOWER(name));


-- 6. Индекси за autocomplete suggestions (опционално)
-- ----------------------------------------------------------------------------

-- Trigram индекси за fuzzy matching (изисква pg_trgm extension)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_products_name_bg_trgm
    ON products USING gin(name_bg gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_name_en_trgm
    ON products USING gin(name_en gin_trgm_ops);


-- =============================================================================
-- Статистики и анализ
-- =============================================================================

-- Обнови статистиките за оптимизатора
ANALYZE products;
ANALYZE product_parameters;
ANALYZE parameters;
ANALYZE parameter_options;
ANALYZE categories;
ANALYZE manufacturers;