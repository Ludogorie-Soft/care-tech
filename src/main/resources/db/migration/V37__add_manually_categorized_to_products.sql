-- Маркира продукти, чиято категория е определена ръчно (админ панел или скрипт 52).
--
-- ЗАЩО: MostSyncService:1018 и TekraSyncService:1124/1375 задават категорията
-- безусловно при всеки прогон, а AsbisSyncService:663 — винаги когато мапингът
-- от feed-а успее. Без този флаг всяка ръчна корекция се губи следващата нощ.
--
-- Огледален на `manually_hidden` (V36) и работи по същия начин: sync-ът чете флага
-- и прескача presetването на категория; админ панелът го вдига при смяна на
-- категория (ProductService.applyAdminCategory).
--
-- VALI не се нуждае от защита — ValiSyncService:990 задава категория само при
-- създаване на нов продукт.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS manually_categorized BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_products_manually_categorized
    ON products (manually_categorized)
    WHERE manually_categorized;
