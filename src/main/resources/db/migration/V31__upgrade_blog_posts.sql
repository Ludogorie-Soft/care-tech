-- Add new columns
ALTER TABLE blog_posts
    ADD COLUMN status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN featured         BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN meta_title       VARCHAR(255),
    ADD COLUMN meta_description VARCHAR(500),
    ADD COLUMN view_count       BIGINT      NOT NULL DEFAULT 0,
    ADD COLUMN category_id      BIGINT      REFERENCES blog_categories(id) ON DELETE SET NULL;

-- Migrate existing published boolean → status string
UPDATE blog_posts SET status = 'PUBLISHED' WHERE published = true;
UPDATE blog_posts SET status = 'DRAFT'     WHERE published = false;

-- Drop old boolean column
ALTER TABLE blog_posts DROP COLUMN published;

-- Update indexes
DROP INDEX IF EXISTS idx_blog_posts_published;
CREATE INDEX idx_blog_posts_status      ON blog_posts(status, published_at DESC);
CREATE INDEX idx_blog_posts_category_id ON blog_posts(category_id);
CREATE INDEX idx_blog_posts_featured    ON blog_posts(featured) WHERE featured = true;
