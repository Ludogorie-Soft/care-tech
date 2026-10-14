CREATE TABLE blog_categories (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    slug             VARCHAR(100) NOT NULL UNIQUE,
    description      TEXT,
    parent_id        BIGINT REFERENCES blog_categories(id) ON DELETE SET NULL,
    sort_order       INT NOT NULL DEFAULT 0,
    created_by       VARCHAR(100),
    last_modified_by VARCHAR(100),
    created_at       TIMESTAMP DEFAULT now(),
    updated_at       TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_blog_categories_slug      ON blog_categories(slug);
CREATE INDEX idx_blog_categories_parent_id ON blog_categories(parent_id);
