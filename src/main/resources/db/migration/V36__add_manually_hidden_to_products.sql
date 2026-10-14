-- Separates "an admin deliberately hid this product" from "the sync hid it".
--
-- WHY (SEARCH_AUDIT_PLAN.md phase 1, bug-460):
-- MostSyncService could only ever hide an existing product, never show it again,
-- because show_flag was doing double duty: it carried both the sync's stock/price
-- verdict and any manual decision by an admin. To avoid re-showing something an
-- admin had hidden on purpose, the sync gave up re-showing anything at all. The
-- result was 273 products that are in stock, priced, with an image and no visible
-- duplicate, yet permanently invisible — including the most expensive laptops in
-- the catalog.
--
-- With manual intent stored separately, the sync can recompute show_flag freely
-- and simply skip rows where manually_hidden = true.
--
-- NO BACKFILL — deliberately. Checked against production before writing this:
-- every one of the 273 eligible-but-hidden products had last_modified_by = 'system',
-- i.e. all were hidden by the sync, none by an admin. The 8500 hidden products
-- attributed to an admin account are all hidden for legitimate reasons (not
-- AVAILABLE, no image, no price) and stay hidden on those grounds regardless of
-- this flag. So defaulting every row to false loses no admin intent.
--
-- ADD COLUMN with a constant DEFAULT is metadata-only on PostgreSQL 11+, so this
-- does not rewrite the 26k-row table. It still takes a brief ACCESS EXCLUSIVE lock;
-- Flyway runs it at application startup rather than from a SQL client, which is
-- the safer path (see bug-475 — a manual ALTER left uncommitted took the site down).

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS manually_hidden BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN products.manually_hidden IS
    'True when an admin hid this product from the admin panel. The sync never '
    're-shows a product with this flag set. Sync-driven hiding uses show_flag only.';

-- Partial index: the sync and the dedup both filter on "not manually hidden",
-- and the flag is true for only a handful of rows.
CREATE INDEX IF NOT EXISTS idx_products_manually_hidden
    ON products(id) WHERE manually_hidden = true;
