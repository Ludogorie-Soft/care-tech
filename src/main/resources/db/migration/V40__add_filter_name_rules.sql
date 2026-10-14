-- V40: Filter values taken from the product name.
--
-- Some suppliers leave key attributes out of their parameters but put them in the product name:
-- MOST sends no socket for most CPUs ("AMD RYZEN 7 7800X3D BOX"), boards name their chipset
-- ("ASROCK B550M-HDV") and memory names its type ("16GB DDR5 6000"). A name rule gives such a product
-- a value for an attribute only when none of its parameters did — supplier data always wins.
--
-- Two kinds of rule:
--   pattern + value_id : filter_norm(products.name_bg) ~* pattern  -> that value
--   use_parser         : the attribute's NUMERIC parser reads the name ("1TB", "2x8GB")
-- category_id narrows a rule to one category; NULL applies everywhere the attribute is used.

CREATE TABLE filter_name_rules (
    id           BIGSERIAL PRIMARY KEY,
    attribute_id BIGINT       NOT NULL REFERENCES filter_attributes (id) ON DELETE CASCADE,
    category_id  BIGINT REFERENCES categories (id) ON DELETE CASCADE,
    pattern      VARCHAR(500),
    value_id     BIGINT REFERENCES filter_values (id) ON DELETE CASCADE,
    use_parser   BOOLEAN      NOT NULL DEFAULT FALSE,
    note         VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((use_parser AND pattern IS NULL AND value_id IS NULL)
        OR (NOT use_parser AND pattern IS NOT NULL AND value_id IS NOT NULL))
);
CREATE UNIQUE INDEX uq_filter_name_rules ON filter_name_rules
    (attribute_id, COALESCE(category_id, -1), COALESCE(pattern, ''), COALESCE(value_id, -1));
CREATE INDEX idx_filter_name_rules_category ON filter_name_rules (category_id);

-- Trademark signs split words ("Ryzen™ 7", "Intel® Core™ 5"), so rules could not see "ryzen 7".
-- They carry no meaning for filtering and are dropped by the one normalization everything uses.
CREATE OR REPLACE FUNCTION filter_norm(t text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$
SELECT NULLIF(btrim(regexp_replace(
           regexp_replace(translate(lower(t), '™®©', ''), '[[:space:]\u00a0\u200b-\u200f\ufeff]+', ' ', 'g'),
           '[[:space:].,:;]+$', ''), ' '), '')
$$;
