-- ============================================================
-- Script 32: Пълен одит — поправка на грешно наредени продукти
-- Дата: 2026-08-19
-- СТАРТИРАЙ СЛЕД скриптове 29, 30, 31
--
-- ГРУПИ:
--   А  — MOST ADATA/XPG SSDs в cat 235 → cat 17 (SSD)
--   Б  — MOST NAS/Enterprise/Desktop HDDs в cat 235 → cat 16 (HDD 3.5")
--   В  — MOST QNAP NAS устройства в cat 235 → cat 361 (NAS у-ва)
--   Г  — Аксесоари в cat 235 → правилни категории
--   Д  — Hama аналогови часовници в Смарт часовници (cat 156) → cat 220
--   Е  — Imou/Tenda non-PoE суичове в cat 963 → общо мрежово (109/110)
-- ============================================================

BEGIN;

-- ============================================================
-- СТЪПКА 0: Покажи скрити категории, в които ще местим продукти
-- ============================================================

-- cat 220 (Часовници) — скрита, ще я показваме
UPDATE categories
SET show_flag  = true,
    updated_at = NOW()
WHERE id = 220;

-- cat 361 (Мрежово устройство за съхранение — NAS) — скрита
UPDATE categories
SET show_flag  = true,
    updated_at = NOW()
WHERE id = 361;

-- ============================================================
-- ГРУПА А: MOST ADATA/XPG/KingSpec SSDs → cat 17 (SSD)
-- Тези модели не съдържат "SSD" в имената и не бяха уловени от script 30
-- ============================================================

UPDATE products
SET category_id = 17,
    updated_at  = NOW()
WHERE id IN (
    30635, -- ADATA SX8200P 1T M2 2280 XPG
    30634, -- ADATA SX8200P 512G M2 2280 XPG
    30636, -- ADATA SX8200P 2T M2 2280 XPG
    30637, -- ADATA LEGEND 800 500GB M2 2280
    30638, -- ADATA LEGEND 800 1TB M2 2280
    30639, -- ADATA LEGEND 800 2TB M2 2280
    27031, -- ADATA LEGEND 860 500G M2 2280
    27032, -- ADATA LEGEND 860 1TB M2 2280
    27033, -- ADATA LEGEND 860 2TB M2 2280
    26996, -- 512G XPG S70 BLADE M2 PCI GEN4
    27002, -- 2T XPG S70 BLADE M2 PCI GEN4
    26998, -- 1T XPG S70 BLADE M2 PCI GEN4
    26925, -- 2TB XPG S60 M2 PCI GEN4
    30629, -- 1TB XPG S20G M2 PCI GEN3 RGB
    26401  -- KINGSPEC P4-480 480GB 2.5" SSD
);

-- ============================================================
-- ГРУПА Б: MOST NAS / Enterprise / Desktop HDDs → cat 16 (HDD 3.5")
-- IronWolf (VN), Exos (NM/NT), BarraCuda (DM) — не са surveillance дискове
-- ============================================================

UPDATE products
SET category_id = 16,
    updated_at  = NOW()
WHERE id IN (
    25981, -- 1T SG ST1000VN008 256MB NAS (IronWolf)
    35524, -- 8T SG ST8000VN002 NAS (IronWolf)
    26189, -- 6T SG ST6000VN006 NAS (IronWolf)
    26136, -- 3T SG ST3000VN006 NAS (IronWolf)
    26198, -- 8T SG ST8000VN004 NAS (IronWolf)
    30624, -- 24TB ST24000NT002 NAS (Exos)
    26220, -- 24TB EXOS X24 ST24000NM002H
    26219, -- 20TB EXOS X24 ST20000NM002H
    26221, -- 30TB EXOS X24 ST30000NM004K
    26201, -- 8T SG ST8000DM004 256MB/5400 (BarraCuda)
    25977, -- 1T SG SATA 6G/7200 ST1000DM014 (BarraCuda)
    26131  -- 3T SG SATA ST3000DM007 (BarraCuda)
);

-- ============================================================
-- ГРУПА В: MOST QNAP NAS устройства → cat 361 (NAS у-ва, вече показана)
-- ============================================================

UPDATE products
SET category_id = 361,
    updated_at  = NOW()
WHERE id IN (
    27772, -- QNAP TS-233-EU NAS 2-BAY
    27774  -- QNAP TS-433-4G-EU NAS
);

-- ============================================================
-- ГРУПА Г: Аксесоари грешно в cat 235
-- ============================================================

-- Г1: SM drive cage → cat 18 (Чекмеджета за дискове)
UPDATE products
SET category_id = 18,
    updated_at  = NOW()
WHERE id = 26375; -- SM MCP-220-73201-0N 2.5 CAGE

-- Г2: TEKRA Dahua camera mount adapters → konsumativy-adapteri
UPDATE products
SET category_id = (SELECT id FROM categories WHERE slug = 'konsumativy-adapteri'),
    updated_at  = NOW()
WHERE id IN (
    10686, -- Адаптер за открит монтаж на стена - DAHUA PFM300
    10701  -- Адаптер за монтаж в кутия или на стена - DAHUA PFM320D-015
);

-- ============================================================
-- ГРУПА Д: Hama аналогови/кварц/DCF часовници → cat 220 (Часовници)
-- Грешно в cat 156 (Смарт часовници) — не са смарт устройства
-- ============================================================

UPDATE products
SET category_id = 220,
    updated_at  = NOW()
WHERE id IN (
    10590, -- Children's wall clock "Koala"
    10591, -- Children's wall clock "Happy Dino"
    10592, -- "Black Digits" Wall Clock
    10593, -- "Nostalgia" Alarm Clock
    10594, -- Hama "Magical Unicorn" Children's Alarm Clock
    10595, -- Hama "Mauritius" Bathroom/Wall Clock with Thermometer
    10596, -- Hama "Salina" Wall Clock Ø22cm white
    10597, -- Hama "Santorini" Wall Clock
    10598, -- Hama "Elegance" Wall Clock Ø30cm
    10599, -- Hama "Tenerife" DCF Radio Wall Clock
    10600, -- Hama "Salina" Wall Clock (black)
    10601, -- Hama "Formentera" Wall Clock Ø35cm
    10602, -- Hama "Cebu" Children's Wall Clock
    10603, -- Hama "Malta" Wall Clock
    10604, -- Hama "Corsica" Wall Clock Ø30cm
    10605, -- Hama "Corfu" Wall Clock Ø30cm Quartz
    10606  -- Hama "Ischia" DCF Digital Table/Wall Clock
);

-- ============================================================
-- ГРУПА Е: Imou/Tenda non-PoE суичове → общо мрежово
-- Тези нямат PoE функционалност и не са surveillance мрежово оборудване
-- ============================================================

-- Imou non-PoE Gigabit switches → cat 109 (Суичове - неуправляеми)
UPDATE products
SET category_id = 109,
    updated_at  = NOW()
WHERE id IN (
    14277, -- Imou 5-port Gigabit Switch
    14332  -- Imou 8-port Gigabit Switch
);

-- Tenda SG108M (управляем, без PoE) → cat 110 (Суичове - управляеми)
UPDATE products
SET category_id = 110,
    updated_at  = NOW()
WHERE id = 25479; -- SWITCH TENDA SG108M 8P

COMMIT;

-- ============================================================
-- ФИНАЛНА ПРОВЕРКА
-- ============================================================

-- Провери cat 235 — трябва само surveillance HDD
SELECT platform, COUNT(*) AS total,
       STRING_AGG(LEFT(COALESCE(name_en, name_bg), 50), ' | ' ORDER BY id) AS samples
FROM products
WHERE category_id = 235
GROUP BY platform
ORDER BY platform;

-- Провери cat 17 — новите SSDs
SELECT COUNT(*) AS ssd_total, platform
FROM products
WHERE category_id = 17
GROUP BY platform;

-- Провери cat 16 — новите HDDs
SELECT COUNT(*) AS hdd_total, platform
FROM products
WHERE category_id = 16
GROUP BY platform;

-- Провери cat 220 — часовниците
SELECT COUNT(*) AS clock_total, status
FROM products
WHERE category_id = 220
GROUP BY status;

-- Провери cat 156 — само смарт часовници трябва да останат
SELECT COUNT(*) AS smart_total, platform
FROM products
WHERE category_id = 156
GROUP BY platform;

-- Провери cat 361 — QNAP NAS
SELECT id, name_en, status FROM products WHERE category_id = 361;

-- Провери cat 963 — само PoE surveillance мрежово
SELECT p.id, p.name_en, p.platform
FROM products p
WHERE p.category_id = 963
ORDER BY p.platform, p.name_en;
