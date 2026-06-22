CREATE TABLE blog_tags (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL UNIQUE,
    slug             VARCHAR(100) NOT NULL UNIQUE,
    created_by       VARCHAR(100),
    last_modified_by VARCHAR(100),
    created_at       TIMESTAMP DEFAULT now(),
    updated_at       TIMESTAMP DEFAULT now()
);

CREATE TABLE blog_post_tags (
    post_id BIGINT NOT NULL REFERENCES blog_posts(id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES blog_tags(id)  ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

CREATE INDEX idx_blog_post_tags_tag_id ON blog_post_tags(tag_id);
