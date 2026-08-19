-- ============================================================
-- Script 30: Поправка на MOST/VALI/TEKRA продукти грешно поставени
--            ВЪТРЕ в Видеонаблюдение дървото
-- Дата: 2026-08-19
-- СТАРТИРАЙ СЛЕД script 29
--
-- DB колони: show_flag (не show), category_id, status, platform
--
-- ПРОБЛЕМИ:
--   А) cat 235 "Твърди дискове":
--      - 2 TEKRA адаптера (DAHUA PFM300, PFM320D) → Адаптери (консумативи-adapteri)
--      - 1 TEKRA конвертор (TEKRA RX-T36S1R5) → Захранващи блокове
--      - 6 MOST Seagate SkyHawk → ОСТАВАТ в 235 (правилно)
--      - 151 MOST SSD → cat 17 (SSD)
--      - 30 MOST general HDD → cat 16 (HDD 3.5")
--      - 33 MOST external drives → cat 16
--
--   Б) cat 236 "Монитори и аксесоари" (parent директно):
--      - 2 TEKRA захранвания → Захранващи блокове
--
--   В) cat 249 "Монитори":
--      - 298 MOST gaming/office монитора → cat 50 (общи монитори)
--      - 13 TEKRA Dahua/Comelit surveillance монитора → ОСТАВАТ
--
--   Г) cat 100 "IP камери и смарт у-ва":
--      - 43 VALI consumer камери (NOT_AVAILABLE) → show_flag = false
-- ============================================================

BEGIN;

-- ============================================================
-- А: cat 235 (Твърди дискове) — изчисти грешните TEKRA продукти
-- ============================================================

-- А1: TEKRA адаптери за монтаж → Адаптери (под Консумативи)
UPDATE products
SET category_id    = (SELECT id FROM categories WHERE slug = 'konsumativy-adapteri'),
    updated_at     = NOW()
WHERE category_id = 235
  AND platform    = 'TEKRA'
  AND (name_bg ILIKE '%адаптер%' OR name_en ILIKE '%adapt%');

-- А2: TEKRA конвертор на захранване → Захранващи блокове
UPDATE products
SET category_id    = (SELECT id FROM categories WHERE slug = 'konsumativy-zahranvashti-blokove'),
    updated_at     = NOW()
WHERE category_id = 235
  AND platform    = 'TEKRA'
  AND (name_bg ILIKE '%конвертор%' OR name_en ILIKE '%converter%');

-- А3: MOST SSDs → cat 17 (SSD Solid State Drive)
UPDATE products
SET category_id = 17,
    updated_at  = NOW()
WHERE category_id = 235
  AND platform   = 'MOST'
  AND (
      name_en ILIKE '%SSD%'
   OR name_en ILIKE '%SPATIUM%'
   OR name_en ILIKE '%RENEGADE%'
   OR name_en ILIKE '%SNV3%'
   OR name_en ILIKE '%SKC3%'
   OR name_en ILIKE '%SFY%'
   OR name_en ILIKE '%SPECTRIX%'
   OR name_en ILIKE '%NV5000%'
   OR name_en ILIKE '%MS30%'
   OR name_en ILIKE '%M371%'
   OR name_en ILIKE '%SU650%'
   OR name_en ILIKE '%SA400%'
   OR name_en ILIKE '%SC610%'
   OR name_en ILIKE '%SKC600%'
   OR (name_en ILIKE '%PCIE%' AND name_en ILIKE '%M2%')
  );

-- А4: MOST External drives → cat 16 (HDD 3.5" — обща storage)
UPDATE products
SET category_id = 16,
    updated_at  = NOW()
WHERE category_id = 235
  AND platform   = 'MOST'
  AND (
      name_en ILIKE '%EXT %'
   OR name_en ILIKE '%HD710%'
   OR name_en ILIKE '%HV620%'
   OR name_en ILIKE '%HD720%'
   OR name_en ILIKE '%710M%'
   OR name_en ILIKE '%SXS2000%'
  );

-- А5: MOST General HDDs (non-surveillance) → cat 16
UPDATE products
SET category_id = 16,
    updated_at  = NOW()
WHERE category_id = 235
  AND platform   = 'MOST'
  AND (
      name_en ILIKE '%ST2000DM%'   -- Seagate Barracuda
   OR name_en ILIKE '%ST2000VN%'   -- Seagate IronWolf NAS
   OR name_en ILIKE '%ST4000DM%'
   OR name_en ILIKE '%ST4000VN%'
   OR name_en ILIKE '%ST6000DM%'
   OR name_en ILIKE '%IRONWOLF%'
   OR name_en ILIKE '%BARRACUDA%'
  );

-- Провери: в cat 235 трябва само MOST SkyHawk (6 бр.)
SELECT platform, COUNT(*) AS remaining,
       STRING_AGG(LEFT(COALESCE(name_en, name_bg), 50), ' | ' ORDER BY id) AS samples
FROM products
WHERE category_id = 235
GROUP BY platform;

-- ============================================================
-- Б: cat 236 "Монитори и аксесоари" (parent) — захранвания → правилна cat
-- ============================================================

UPDATE products
SET category_id    = (SELECT id FROM categories WHERE slug = 'konsumativy-zahranvashti-blokove'),
    updated_at     = NOW()
WHERE category_id = 236
  AND platform    = 'TEKRA'
  AND (name_bg ILIKE '%захранва%' OR name_bg ILIKE '%захранващ%');

-- ============================================================
-- В: cat 249 "Монитори" — 298 MOST gaming монитора → cat 50 (Монитори и дисплеи)
-- ============================================================

UPDATE products
SET category_id = 50,
    updated_at  = NOW()
WHERE category_id = 249
  AND platform   = 'MOST';

-- Провери: в cat 249 трябва само TEKRA (13 surveillance монитора)
SELECT platform, COUNT(*) AS remaining
FROM products
WHERE category_id = 249
GROUP BY platform;

-- ============================================================
-- Г: cat 100 "IP камери" — скрий VALI NOT_AVAILABLE consumer камери
-- ============================================================

UPDATE products
SET show_flag  = false,
    updated_at = NOW()
WHERE category_id = 100
  AND platform   = 'VALI'
  AND status    != 'AVAILABLE';

-- Покажи VALI/platform='' продукти с AVAILABLE в cat 100:
SELECT id, name_bg, status, show_flag, platform
FROM products
WHERE category_id = 100
  AND platform IN ('VALI', '')
  AND status = 'AVAILABLE';

COMMIT;

-- Финална проверка на ключовите категории
SELECT c.id, c.name_bg,
    COUNT(p.id)                                              AS total,
    COUNT(p.id) FILTER (WHERE p.status = 'AVAILABLE')       AS available,
    COUNT(p.id) FILTER (WHERE p.platform = 'TEKRA')         AS tekra,
    COUNT(p.id) FILTER (WHERE p.platform = 'MOST')          AS most,
    COUNT(p.id) FILTER (WHERE p.platform = 'VALI')          AS vali,
    COUNT(p.id) FILTER (WHERE p.platform = 'ASBIS')         AS asbis
FROM categories c
LEFT JOIN products p ON p.category_id = c.id
WHERE c.id IN (100, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 249)
  OR c.slug IN ('konsumativy-adapteri','konsumativy-zahranvashti-blokove')
GROUP BY c.id, c.name_bg
ORDER BY c.id;
