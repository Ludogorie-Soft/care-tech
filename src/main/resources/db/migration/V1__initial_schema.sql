-- V1__initial_schema.sql
-- Clean initial schema with Many-to-Many Parameter-Category relationship

-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE,
    email VARCHAR(200) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    last_login_at TIMESTAMP,
    is_correct BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    preferred_language VARCHAR(10) DEFAULT 'bg',
    phone VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system'
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- ============================================================
-- CATEGORIES TABLE
-- ============================================================
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    tekra_id VARCHAR(500),
    tekra_slug VARCHAR(500),
    asbis_id VARCHAR(500),
    asbis_code VARCHAR(100),
    external_id BIGINT UNIQUE,
    name_en VARCHAR(255),
    name_bg VARCHAR(255),
    slug VARCHAR(200),
    category_path VARCHAR(500),
    show_flag BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    parent_id BIGINT,
    platform VARCHAR(50),
    is_promo_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system',
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_tekra_slug ON categories(tekra_slug);
CREATE INDEX idx_categories_asbis_id ON categories(asbis_id);
CREATE INDEX idx_categories_external_id ON categories(external_id);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_show_flag ON categories(show_flag);
CREATE INDEX idx_categories_platform ON categories(platform);
CREATE INDEX idx_categories_path ON categories(category_path);
CREATE INDEX idx_categories_name_bg_lower ON categories(LOWER(name_bg));
CREATE INDEX idx_categories_name_en_lower ON categories(LOWER(name_en));

-- ============================================================
-- MANUFACTURERS TABLE
-- ============================================================
CREATE TABLE manufacturers (
    id BIGSERIAL PRIMARY KEY,
    external_id BIGINT UNIQUE,
    asbis_id VARCHAR(100),
    asbis_code VARCHAR(100),
    name VARCHAR(255) NOT NULL,
    information_name VARCHAR(255),
    information_email VARCHAR(255),
    information_address TEXT,
    eu_representative_name VARCHAR(255),
    eu_representative_email VARCHAR(255),
    eu_representative_address TEXT,
    platform VARCHAR(50),
    is_promo_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system'
);

CREATE INDEX idx_manufacturers_name ON manufacturers(name);
CREATE INDEX idx_manufacturers_external_id ON manufacturers(external_id);
CREATE INDEX idx_manufacturers_asbis_id ON manufacturers(asbis_id);
CREATE INDEX idx_manufacturers_platform ON manufacturers(platform);
CREATE INDEX idx_manufacturers_name_lower ON manufacturers(LOWER(name));

-- ============================================================
-- PRODUCTS TABLE
-- ============================================================
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    tekra_id VARCHAR(255),
    asbis_id VARCHAR(100),
    asbis_code VARCHAR(100),
    asbis_part_number VARCHAR(100),
    sku VARCHAR(255),
    name_bg TEXT,
    name_en TEXT,
    description_bg TEXT,
    description_en TEXT,
    external_id BIGINT UNIQUE,
    workflow_id BIGINT,
    reference_number VARCHAR(255) UNIQUE,
    model VARCHAR(500),
    barcode VARCHAR(255),
    manufacturer_id BIGINT,
    status VARCHAR(50),
    price_client DECIMAL(10, 2),
    price_partner DECIMAL(10, 2),
    price_promo DECIMAL(10, 2),
    price_client_promo DECIMAL(10, 2),
    markup_percentage DECIMAL(5, 2) DEFAULT 20.0,
    final_price DECIMAL(10, 2),
    show_flag BOOLEAN DEFAULT true,
    warranty INTEGER,
    discount DECIMAL(10, 2) DEFAULT 0,
    active BOOLEAN DEFAULT true,
    featured BOOLEAN DEFAULT false,
    image_url VARCHAR(1000),
    weight DECIMAL(8, 2),
    category_id BIGINT,
    platform VARCHAR(50),
    slug TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system',
    CONSTRAINT fk_product_manufacturer FOREIGN KEY (manufacturer_id) REFERENCES manufacturers(id) ON DELETE SET NULL,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_external_id ON products(external_id);
CREATE INDEX idx_products_reference_number ON products(reference_number);
CREATE INDEX idx_products_manufacturer_id ON products(manufacturer_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_show_flag ON products(show_flag);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_featured ON products(featured);
CREATE INDEX idx_products_platform ON products(platform);
CREATE INDEX idx_products_asbis_id ON products(asbis_id);
CREATE INDEX idx_products_asbis_part_number ON products(asbis_part_number);
CREATE INDEX idx_products_slug ON products(slug);
CREATE INDEX idx_products_active_show ON products(active, show_flag) WHERE active = true AND show_flag = true;
CREATE INDEX idx_products_final_price ON products(final_price);
CREATE INDEX idx_products_category_manufacturer ON products(category_id, manufacturer_id);

-- Full-text search indexes
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_products_fts_bg ON products USING gin(
    to_tsvector('simple',
        coalesce(name_bg, '') || ' ' ||
        coalesce(description_bg, '') || ' ' ||
        coalesce(model, '') || ' ' ||
        coalesce(reference_number, '')
    )
);

CREATE INDEX idx_products_fts_en ON products USING gin(
    to_tsvector('simple',
        coalesce(name_en, '') || ' ' ||
        coalesce(description_en, '') || ' ' ||
        coalesce(model, '') || ' ' ||
        coalesce(reference_number, '')
    )
);

CREATE INDEX idx_products_name_bg_trgm ON products USING gin(name_bg gin_trgm_ops);
CREATE INDEX idx_products_name_en_trgm ON products USING gin(name_en gin_trgm_ops);

-- ============================================================
-- ADDITIONAL IMAGES TABLE
-- ============================================================
CREATE TABLE additional_images (
    product_id BIGINT NOT NULL,
    additional_urls VARCHAR(1000),
    CONSTRAINT fk_additional_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_additional_images_product_id ON additional_images(product_id);

-- ============================================================
-- PARAMETERS TABLE (NO category_id - Many-to-Many)
-- ============================================================
CREATE TABLE parameters (
    id BIGSERIAL PRIMARY KEY,
    external_id BIGINT,
    tekra_key VARCHAR(500),
    asbis_key VARCHAR(500),
    name_bg VARCHAR(500),
    name_en VARCHAR(500),
    sort_order INTEGER,
    platform VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system'
);

CREATE INDEX idx_parameters_external_id ON parameters(external_id);
CREATE INDEX idx_parameters_tekra_key ON parameters(tekra_key);
CREATE INDEX idx_parameters_asbis_key ON parameters(asbis_key);
CREATE INDEX idx_parameters_platform ON parameters(platform);
CREATE INDEX idx_parameters_name_bg_lower ON parameters(LOWER(name_bg));
CREATE INDEX idx_parameters_name_en_lower ON parameters(LOWER(name_en));
CREATE INDEX idx_parameters_sort_order ON parameters(sort_order);

-- ============================================================
-- CATEGORY_PARAMETERS (Many-to-Many junction table)
-- ============================================================
CREATE TABLE category_parameters (
    category_id BIGINT NOT NULL,
    parameter_id BIGINT NOT NULL,
    PRIMARY KEY (category_id, parameter_id),
    CONSTRAINT fk_category_parameters_category
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT fk_category_parameters_parameter
        FOREIGN KEY (parameter_id) REFERENCES parameters(id) ON DELETE CASCADE
);

CREATE INDEX idx_category_parameters_category ON category_parameters(category_id);
CREATE INDEX idx_category_parameters_parameter ON category_parameters(parameter_id);

-- ============================================================
-- PARAMETER OPTIONS TABLE
-- ============================================================
CREATE TABLE parameter_options (
    id BIGSERIAL PRIMARY KEY,
    external_id BIGINT,
    parameter_id BIGINT NOT NULL,
    name_bg TEXT,
    name_en TEXT,
    sort_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system',
    CONSTRAINT fk_parameter_option_parameter FOREIGN KEY (parameter_id) REFERENCES parameters(id) ON DELETE CASCADE
);

CREATE INDEX idx_parameter_options_parameter_id ON parameter_options(parameter_id);
CREATE INDEX idx_parameter_options_external_id ON parameter_options(external_id);
CREATE INDEX idx_parameter_options_name_bg_lower ON parameter_options(LOWER(name_bg));
CREATE INDEX idx_parameter_options_name_en_lower ON parameter_options(LOWER(name_en));
CREATE INDEX idx_parameter_options_sort_order ON parameter_options(sort_order);

-- ============================================================
-- PRODUCT PARAMETERS TABLE
-- ============================================================
CREATE TABLE product_parameters (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    parameter_id BIGINT NOT NULL,
    parameter_option_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system',
    CONSTRAINT fk_product_parameter_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_parameter_parameter FOREIGN KEY (parameter_id) REFERENCES parameters(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_parameter_option FOREIGN KEY (parameter_option_id) REFERENCES parameter_options(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_parameters_product_id ON product_parameters(product_id);
CREATE INDEX idx_product_parameters_parameter_id ON product_parameters(parameter_id);
CREATE INDEX idx_product_parameters_option_id ON product_parameters(parameter_option_id);
CREATE INDEX idx_product_parameters_param_option ON product_parameters(parameter_id, parameter_option_id);
CREATE INDEX idx_product_params_lookup ON product_parameters(product_id, parameter_id, parameter_option_id);

-- ============================================================
-- PRODUCT FLAGS TABLE
-- ============================================================
CREATE TABLE product_flags (
    id BIGSERIAL PRIMARY KEY,
    external_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    image_url TEXT,
    name_bg VARCHAR(255),
    name_en VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system',
    CONSTRAINT fk_product_flag_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_flags_product_id ON product_flags(product_id);
CREATE INDEX idx_product_flags_external_id ON product_flags(external_id);

-- ============================================================
-- CART ITEMS TABLE
-- ============================================================
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system',
    CONSTRAINT fk_cart_item_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_cart_items_user_id ON cart_items(user_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);

-- ============================================================
-- USER FAVORITES TABLE
-- ============================================================
CREATE TABLE user_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system',
    CONSTRAINT fk_user_favorite_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_favorite_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_favorite UNIQUE (user_id, product_id)
);

CREATE INDEX idx_user_favorites_user_id ON user_favorites(user_id);
CREATE INDEX idx_user_favorites_product_id ON user_favorites(product_id);

-- ============================================================
-- SUBSCRIPTION TABLE
-- ============================================================
CREATE TABLE subscription (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE INDEX idx_subscription_email ON subscription(email);

-- ============================================================
-- SYNC LOGS TABLE
-- ============================================================
CREATE TABLE sync_logs (
    id BIGSERIAL PRIMARY KEY,
    sync_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    records_processed BIGINT,
    records_created BIGINT,
    records_updated BIGINT,
    error_message TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system'
);

CREATE INDEX idx_sync_logs_sync_type ON sync_logs(sync_type);
CREATE INDEX idx_sync_logs_status ON sync_logs(status);
CREATE INDEX idx_sync_logs_created_at ON sync_logs(created_at);

-- ============================================================
-- ORDERS TABLE
-- ============================================================
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(30),
    subtotal DECIMAL(10, 2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    shipping_cost DECIMAL(10, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(10, 2) DEFAULT 0,
    total DECIMAL(10, 2) NOT NULL DEFAULT 0,
    customer_first_name VARCHAR(100) NOT NULL,
    customer_last_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(200) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_company VARCHAR(200),
    customer_vat_number VARCHAR(50),
    customer_vat_registered BOOLEAN DEFAULT false,
    shipping_address TEXT NOT NULL,
    shipping_city VARCHAR(100) NOT NULL,
    shipping_postal_code VARCHAR(20),
    shipping_country VARCHAR(100) NOT NULL DEFAULT 'Bulgaria',
    billing_address TEXT,
    billing_city VARCHAR(100),
    billing_postal_code VARCHAR(20),
    billing_country VARCHAR(100) DEFAULT 'Bulgaria',
    customer_notes TEXT,
    admin_notes TEXT,
    tracking_number VARCHAR(100),
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    invoice_number VARCHAR(50),
    invoice_date TIMESTAMP,
    fiscal_receipt_number VARCHAR(100),
    shipping_method VARCHAR(30),
    shipping_speedy_site_id BIGINT,
    shipping_speedy_office_id BIGINT,
    shipping_speedy_site_name VARCHAR(255),
    shipping_speedy_office_name VARCHAR(255),
    is_to_speedy_office BOOLEAN NOT NULL DEFAULT TRUE,
    insurance_offer BOOLEAN NOT NULL DEFAULT FALSE,
    installment_offer BOOLEAN NOT NULL DEFAULT FALSE,
    terms_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system',
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_payment_status ON orders(payment_status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_customer_email ON orders(customer_email);
CREATE INDEX idx_orders_shipping_method ON orders(shipping_method);
CREATE INDEX idx_orders_speedy_site_id ON orders(shipping_speedy_site_id);
CREATE INDEX idx_orders_speedy_office_id ON orders(shipping_speedy_office_id);

-- ============================================================
-- ORDER ITEMS TABLE
-- ============================================================
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(500) NOT NULL,
    product_sku VARCHAR(100),
    product_model VARCHAR(255),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    tax_rate DECIMAL(5, 2) NOT NULL DEFAULT 20.00,
    line_total DECIMAL(10, 2) NOT NULL,
    line_tax DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system',
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- ============================================================
-- CATEGORY PATH GENERATION FUNCTION
-- ============================================================
CREATE OR REPLACE FUNCTION generate_category_path(cat_id BIGINT)
RETURNS TEXT AS $$
DECLARE
    path_parts TEXT[] := '{}';
    current_id BIGINT;
    current_slug TEXT;
    current_parent_id BIGINT;
    iteration_count INTEGER := 0;
    max_iterations INTEGER := 10;
BEGIN
    current_id := cat_id;

    WHILE current_id IS NOT NULL AND iteration_count < max_iterations LOOP
        SELECT
            COALESCE(tekra_slug, slug, CONCAT('cat-', id)),
            parent_id
        INTO current_slug, current_parent_id
        FROM categories
        WHERE id = current_id;

        IF NOT FOUND THEN
            EXIT;
        END IF;

        IF current_slug IS NOT NULL THEN
            path_parts := array_prepend(current_slug, path_parts);
        END IF;

        current_id := current_parent_id;
        iteration_count := iteration_count + 1;
    END LOOP;

    IF array_length(path_parts, 1) > 0 THEN
        RETURN array_to_string(path_parts, '/');
    ELSE
        RETURN NULL;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- ANALYZE TABLES
-- ============================================================
ANALYZE products;
ANALYZE product_parameters;
ANALYZE parameters;
ANALYZE parameter_options;
ANALYZE categories;
ANALYZE manufacturers;