-- V38: Canonical filter layer.
--
-- Supplier data (parameters, parameter_options, product_parameters) stays the raw layer owned by the
-- sync services. The tables below are owned by the shop and no sync ever writes them, so nothing a
-- nightly run does can undo a mapping or a curated filter. Shopper filters are served from
-- product_filter_values, which FilterIndexService rebuilds from the raw layer after every sync.
--
-- Schema only. Mapping rules are data and live in scripts/ (V39 seeds the static junk rules).

-- In production since 2026-05 but never in a migration (bug-021, bug-364). No-op where it exists.
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_parameters_unique
    ON product_parameters (product_id, parameter_id, parameter_option_id);

-- The one text normalization used by the filter layer: lower case, any run of whitespace (including
-- non-breaking and zero-width characters suppliers send) collapsed to one space, trailing
-- punctuation dropped. Rules store normalized text, so "Цвят", "цвят " and "Цвят." all match.
CREATE OR REPLACE FUNCTION filter_norm(t text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$
SELECT NULLIF(btrim(regexp_replace(
           regexp_replace(lower(t), '[[:space:]\u00a0\u200b-\u200f\ufeff]+', ' ', 'g'),
           '[[:space:].,:;]+$', ''), ' '), '')
$$;

-- Key of an automatic value: filter_norm without any whitespace and with superscripts folded, so
-- "178°/178°" and "178° / 178°", "2 x HDMI" and "2x HDMI", "cd/m²" and "cd/m^2" are one value.
CREATE OR REPLACE FUNCTION filter_value_key(t text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$
SELECT NULLIF(regexp_replace(translate(filter_norm(t), '²³', '23'), '[[:space:]^]+', '', 'g'), '')
$$;

-- ── Shop-owned configuration ─────────────────────────────────────────────────────────────────────

CREATE TABLE filter_attributes (
    id            BIGSERIAL PRIMARY KEY,
    slug          VARCHAR(120) NOT NULL UNIQUE,
    name_bg       VARCHAR(255) NOT NULL,
    name_en       VARCHAR(255),
    -- ENUM: values come from the raw option text. NUMERIC: values come from a Java parser.
    value_type    VARCHAR(10)  NOT NULL DEFAULT 'ENUM' CHECK (value_type IN ('ENUM', 'NUMERIC')),
    unit          VARCHAR(20),
    parser        VARCHAR(40),
    -- Regex that splits one raw option into several values ("Черен/Сив" -> Черен, Сив).
    split_pattern VARCHAR(100),
    -- FALSE: only values produced by rules or the parser exist; the rest go to the unmapped report.
    auto_values   BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order    INTEGER      NOT NULL DEFAULT 100,
    -- AUTO: created by the rebuild from a parameter name. MANUAL: curated.
    origin        VARCHAR(10)  NOT NULL DEFAULT 'AUTO' CHECK (origin IN ('AUTO', 'MANUAL')),
    -- filter_norm(name) the AUTO attribute was created for; NULL for MANUAL attributes.
    auto_key      VARCHAR(500) UNIQUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (value_type <> 'NUMERIC' OR parser IS NOT NULL)
);

CREATE TABLE filter_values (
    id            BIGSERIAL PRIMARY KEY,
    attribute_id  BIGINT       NOT NULL REFERENCES filter_attributes (id) ON DELETE CASCADE,
    value_bg      VARCHAR(500) NOT NULL,
    value_en      VARCHAR(500),
    -- filter_value_key(text) for automatic values, filter_norm(label) for curated ones, the parser's
    -- key ("27", "144") for NUMERIC ones.
    norm_key      VARCHAR(500) NOT NULL,
    numeric_value NUMERIC(14, 4),
    sort_order    INTEGER,
    origin        VARCHAR(10)  NOT NULL DEFAULT 'AUTO' CHECK (origin IN ('AUTO', 'MANUAL')),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Value ids are stable across rebuilds, so filter URLs keep working.
    UNIQUE (attribute_id, norm_key)
);

-- Which raw parameters feed which attribute. A rule matches by parameters.id or by normalized
-- parameter name (Vali issues a separate parameter per category — "Цвят" has 93 ids — so name rules
-- are the ones that keep working when a supplier adds a category). Optional platform and category
-- narrow a rule. IGNORE: not a filter, still shown in specifications. HIDE: shown nowhere.
CREATE TABLE filter_attribute_sources (
    id           BIGSERIAL PRIMARY KEY,
    action       VARCHAR(10)  NOT NULL CHECK (action IN ('MAP', 'IGNORE', 'HIDE')),
    attribute_id BIGINT REFERENCES filter_attributes (id) ON DELETE CASCADE,
    -- Deliberately no FK: a supplier re-creating a parameter must not silently delete a curated rule.
    -- Rules whose parameter is gone are listed by the admin report.
    parameter_id BIGINT,
    name_norm    VARCHAR(500),
    -- Matches products.platform, NULL = any supplier.
    platform     VARCHAR(50),
    category_id  BIGINT REFERENCES categories (id) ON DELETE CASCADE,
    origin       VARCHAR(10)  NOT NULL DEFAULT 'MANUAL' CHECK (origin IN ('AUTO', 'MANUAL')),
    note         VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((action = 'MAP') = (attribute_id IS NOT NULL)),
    CHECK ((parameter_id IS NULL) <> (name_norm IS NULL)),
    CHECK (name_norm IS NULL OR name_norm = filter_norm(name_norm))
);
-- origin is part of the key so a curated rule can sit next to the AUTO rule for the same name and
-- win over it, instead of having to edit or delete a row the rebuild created.
CREATE UNIQUE INDEX uq_filter_attribute_sources ON filter_attribute_sources
    (COALESCE(parameter_id, -1), COALESCE(name_norm, ''), COALESCE(platform, ''), COALESCE(category_id, -1), origin);
CREATE INDEX idx_filter_attribute_sources_parameter ON filter_attribute_sources (parameter_id);
CREATE INDEX idx_filter_attribute_sources_name ON filter_attribute_sources (name_norm);

-- Raw option text -> canonical value. EXACT rules match filter_norm(text) and win over REGEX rules,
-- which match the normalized text case-insensitively and may yield several values. A NULL value_id
-- drops the raw text. Rules apply to the whole option first, then to each split part.
CREATE TABLE filter_value_rules (
    id           BIGSERIAL PRIMARY KEY,
    attribute_id BIGINT       NOT NULL REFERENCES filter_attributes (id) ON DELETE CASCADE,
    raw_norm     VARCHAR(500),
    pattern      VARCHAR(500),
    value_id     BIGINT REFERENCES filter_values (id) ON DELETE CASCADE,
    note         VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((raw_norm IS NULL) <> (pattern IS NULL)),
    CHECK (raw_norm IS NULL OR raw_norm = filter_norm(raw_norm))
);
CREATE UNIQUE INDEX uq_filter_value_rules ON filter_value_rules
    (attribute_id, COALESCE(raw_norm, ''), COALESCE(pattern, ''), COALESCE(value_id, -1));

-- AUTO: the rebuild fills free slots with the best-covered attributes. MANUAL: only curated rows.
CREATE TABLE category_filter_settings (
    category_id  BIGINT PRIMARY KEY REFERENCES categories (id) ON DELETE CASCADE,
    mode         VARCHAR(10) NOT NULL DEFAULT 'AUTO' CHECK (mode IN ('AUTO', 'MANUAL')),
    max_groups   INTEGER,
    min_coverage NUMERIC(4, 3)
);

-- The filter groups of a category. The rebuild rewrites AUTO rows only; MANUAL rows are human
-- intent (a MANUAL row with visible = FALSE keeps an attribute out of the category for good).
CREATE TABLE category_filters (
    category_id  BIGINT      NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    attribute_id BIGINT      NOT NULL REFERENCES filter_attributes (id) ON DELETE CASCADE,
    sort_order   INTEGER     NOT NULL DEFAULT 0,
    visible      BOOLEAN     NOT NULL DEFAULT TRUE,
    origin       VARCHAR(10) NOT NULL DEFAULT 'AUTO' CHECK (origin IN ('AUTO', 'MANUAL')),
    coverage     NUMERIC(5, 4),
    PRIMARY KEY (category_id, attribute_id)
);
CREATE INDEX idx_category_filters_attribute ON category_filters (attribute_id);

-- ── Derived by FilterIndexService (never edited by hand) ─────────────────────────────────────────

-- Resolved rule per raw parameter as used by products of one category and supplier.
-- platform is '' for products without a supplier (created in the admin panel).
CREATE TABLE filter_param_map (
    parameter_id BIGINT      NOT NULL REFERENCES parameters (id) ON DELETE CASCADE,
    category_id  BIGINT      NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    platform     VARCHAR(50) NOT NULL,
    action       VARCHAR(10) NOT NULL,
    attribute_id BIGINT REFERENCES filter_attributes (id) ON DELETE CASCADE,
    PRIMARY KEY (parameter_id, category_id, platform)
);

CREATE TABLE filter_option_map (
    parameter_option_id BIGINT NOT NULL REFERENCES parameter_options (id) ON DELETE CASCADE,
    attribute_id        BIGINT NOT NULL REFERENCES filter_attributes (id) ON DELETE CASCADE,
    value_id            BIGINT NOT NULL REFERENCES filter_values (id) ON DELETE CASCADE,
    PRIMARY KEY (parameter_option_id, attribute_id, value_id)
);
CREATE INDEX idx_filter_option_map_value ON filter_option_map (value_id);

CREATE TABLE product_filter_values (
    product_id   BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    attribute_id BIGINT NOT NULL REFERENCES filter_attributes (id) ON DELETE CASCADE,
    value_id     BIGINT NOT NULL REFERENCES filter_values (id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, attribute_id, value_id)
);
CREATE INDEX idx_product_filter_values_value ON product_filter_values (value_id, product_id);
CREATE INDEX idx_product_filter_values_attribute ON product_filter_values (attribute_id);

-- Raw values of attributes with auto_values = FALSE that no rule or parser could place.
CREATE TABLE filter_unmapped_values (
    attribute_id BIGINT       NOT NULL REFERENCES filter_attributes (id) ON DELETE CASCADE,
    raw_norm     VARCHAR(500) NOT NULL,
    sample_text  TEXT,
    option_count INTEGER      NOT NULL,
    PRIMARY KEY (attribute_id, raw_norm)
);

CREATE TABLE filter_rebuild_runs (
    id          BIGSERIAL PRIMARY KEY,
    started_at  TIMESTAMP   NOT NULL,
    finished_at TIMESTAMP,
    status      VARCHAR(20) NOT NULL,
    trigger     VARCHAR(30),
    dry_run     BOOLEAN     NOT NULL DEFAULT FALSE,
    stats       JSONB,
    error       TEXT
);
