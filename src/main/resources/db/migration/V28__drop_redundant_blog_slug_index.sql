-- The UNIQUE constraint on blog_posts.slug already creates an implicit B-tree index.
-- The explicit idx_blog_posts_slug is redundant and doubles write overhead on every INSERT/UPDATE.
DROP INDEX IF EXISTS idx_blog_posts_slug;
