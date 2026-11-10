-- Marks products whose status (Наличен / Неналичен ...) was set by an admin.
--
-- WHY: every supplier sync (MOST, VALI, ASBIS, TEKRA) assigned the status from its
-- feed on every run, and the "missing from the feed" passes marked products
-- NOT_AVAILABLE in bulk. An admin's choice therefore lasted only until the next run.
--
-- Same pattern as `manually_hidden` (V36) and `manually_categorized` (V37): the syncs
-- read the flag and leave the status alone, and derive visibility from it. The admin
-- panel raises the flag when the status is changed (ProductService.applyAdminStatus)
-- and clears it when the admin hands the status back to the supplier.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS manual_status BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_products_manual_status
    ON products (manual_status)
    WHERE manual_status;
