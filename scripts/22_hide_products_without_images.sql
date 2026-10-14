-- ============================================================
-- Script 22: Скриване на продукти без снимки
-- Продукти без primaryImageUrl не трябва да се показват на сайта.
-- ============================================================

UPDATE products
SET show_flag = false,
    status = 'NOT_AVAILABLE',
    updated_at = CURRENT_TIMESTAMP
WHERE active = true
  AND show_flag = true
  AND (image_url IS NULL OR image_url = '');

-- Проверка
SELECT COUNT(*) AS скрити_без_снимка
FROM products
WHERE show_flag = false
  AND (image_url IS NULL OR image_url = '');

SELECT COUNT(*) AS видими_със_снимка
FROM products
WHERE show_flag = true
  AND image_url IS NOT NULL
  AND image_url <> '';
