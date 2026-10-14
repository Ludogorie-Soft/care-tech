CREATE TABLE blog_posts (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    slug             VARCHAR(255) NOT NULL UNIQUE,
    excerpt          TEXT,
    content          TEXT NOT NULL,
    cover_image_url  VARCHAR(500),
    published        BOOLEAN NOT NULL DEFAULT false,
    published_at     TIMESTAMP,
    created_by       VARCHAR(255),
    last_modified_by VARCHAR(255),
    created_at       TIMESTAMP DEFAULT now(),
    updated_at       TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_blog_posts_slug ON blog_posts(slug);
CREATE INDEX idx_blog_posts_published ON blog_posts(published, published_at DESC);
