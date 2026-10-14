-- ============================================================
-- Script 36: Поправка на категориите на TEKRA продукти
-- Дата: 2026-08-19
--
-- ПРОБЛЕМ 1: 42 NVR продукта са в cat 238 (DVR) вместо cat 962 (NVR под IP системи)
--   Причина: script 33 Д2 е преместил NVR от cat 231, но NVR-ите вече в cat 238
--   не са засегнати.
--   Засегнати: Dahua NVR, Ajax NVR, Raysharp NVR, Carrier NVR
--
-- ПРОБЛЕМ 2: 42 Comelit IP камери са в cat 100 (IP камери под IP системи)
--   вместо cat 964 (IP камери под NDAA).
--   Причина: при синка findCategoryWithTypeDiscrimination ги е пратил в cat 100,
--   без да взема предвид NDAA принадлежността им.
--
-- РЕЗУЛТАТ СЛЕД СКРИПТА:
--   cat 238 (DVR):            18 продукта (само DVR/XVR)
--   cat 962 (NVR под IP):     43 продукта (+42)
--   cat 100 (IP камери):     209 продукта (-42 Comelit)
--   cat 964 (IP камери NDAA): 42 продукта (+42 Comelit)
-- ============================================================

BEGIN;

-- ============================================================
-- А: NVR продукти от cat 238 (DVR) → cat 962 (NVR под IP системи)
-- ============================================================
UPDATE products
SET category_id = 962,
    updated_at  = NOW()
WHERE platform    = 'TEKRA'
  AND category_id = 238
  AND (name_bg ILIKE '%NVR%' OR name_en ILIKE '%NVR%');

-- ============================================================
-- Б: Comelit IP камери от cat 100 → cat 964 (IP камери NDAA)
-- Comelit = NDAA-сертифициран производител в Tekra структурата
-- ============================================================
UPDATE products
SET category_id = 964,
    updated_at  = NOW()
WHERE platform    = 'TEKRA'
  AND category_id = 100
  AND (name_bg ILIKE '%Comelit%' OR name_en ILIKE '%Comelit%');

COMMIT;

-- ============================================================
-- ПРОВЕРКА
-- ============================================================
SELECT
    c.id,
    c.name_bg,
    COUNT(p.id) FILTER (WHERE p.platform = 'TEKRA') AS tekra_count
FROM categories c
LEFT JOIN products p ON p.category_id = c.id
WHERE c.id IN (100, 238, 962, 964)
GROUP BY c.id, c.name_bg
ORDER BY c.id;
