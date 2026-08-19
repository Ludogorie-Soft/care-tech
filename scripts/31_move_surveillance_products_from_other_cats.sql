-- ============================================================
-- Script 31: Преместване на surveillance продукти от грешни категории
--            → правилните категории под Видеонаблюдение
-- Дата: 2026-08-19
-- СТАРТИРАЙ СЛЕД script 29 (новите категории трябва да съществуват)
--
-- DB колони: show_flag (не show), category_id, status, platform, name_bg, name_en
--
-- НАМЕРЕНИ ОТ ОДИТА (783 потенциални, ~350 реални):
--
--   TRUE POSITIVES за преместване:
--   [500] Смарт устройства        — 82 ASBIS IMOU (камери + smart home)
--   [153] Смарт часовници         — 34 ASBIS IMOU камери
--   [502] Външна смарт IP камера  —  4 ASBIS IMOU камери
--   [546] Сигурност-Видеорекордер — 16 ASBIS Dahua/Imou NVR/DVR
--   [362] Дисплеи                 — 17 ASBIS Dahua monitors
--   [366] Твърди дискове (ASBIS)  — 14 ASBIS SkyHawk/WD Purple
--   [16]  Хард дискове 3.5"       — 21 VALI  WD Purple/SkyHawk
--   [99]  Рутери и мрежово        — 12 ASBIS Imou/Ubiquiti PoE switches
--   [342] Мрежов хардуер          —  4 MOST  Tenda switch/NVR/камера
--   [298] Аксесоари               — 14 ASBIS Dahua junction boxes + LORGAR cameras
--   [590] Сигурност-Комплект      —  2 ASBIS IMOU kits
--   [295] Сигурност-Комплект      —  ? ASBIS IMOU kits
--   [548] Мрежово-Контрол достъп  —  2 ASBIS Ubiquiti AI camera
--   [593] Security Outdoor Camera —  1 ASBIS Ubiquiti 4K camera
--   [50]  Монитори (BenQ PVS7)    —  1 VALI  surveillance monitor
--   [517] Флаш памети             —  4 ASBIS IMOU/Dahua microSD
--   [57]  USB памети              —  1 ASBIS IMOU microSD
--   [127] Кабели                  —  2 ASBIS Ubiquiti PoE+ adapters
--   [545] Cloud Key Enterprise    —  1 ASBIS Ubiquiti Cloud Gateway
--
--   FALSE POSITIVES (не се местят):
--   [333] Dell switches, [60] Logitech webcams, [202/205/206/208/209] батерии,
--   [109/110] VALI general switches, [745] таблети, [325] conference cameras,
--   [40] ASUS ROG Ranger раници, [147/149] фото аксесоари,
--   [443] COUGAR Ranger столове, [678] смартфони
-- ============================================================

BEGIN;

-- ============================================================
-- ГРУПА 1: IMOU / Dahua IP камери → cat 100 "IP камери и смарт у-ва"
-- Засяга: [500, 153, 502, 548, 593, 99] (само камери — не smart home сензори)
-- ============================================================

UPDATE products
SET category_id = 100,
    updated_at  = NOW()
WHERE platform = 'ASBIS'
  AND category_id IN (500, 153, 502, 548, 593, 298, 99)
  AND (
      -- IMOU камери (bullet, ranger, cruiser, knight и т.н.)
      (name_en ILIKE '%imou%' AND (
          name_en ILIKE '%camera%'
       OR name_en ILIKE '% cam%'
       OR name_en ILIKE '%bullet%'
       OR name_en ILIKE '%turret%'
       OR name_en ILIKE '%ranger%'
       OR name_en ILIKE '%knight%'
       OR name_en ILIKE '%cruiser%'
       OR name_en ILIKE '%cell%'
       OR name_en ILIKE '%cue%'
       OR name_en ILIKE '%looc%'
       OR name_en ILIKE '%dome%'
       OR name_en ILIKE '%spot%'
       OR name_en ILIKE '%versa%'
       OR name_en ILIKE '%rapid%'
      ))
      -- Dahua IP камери
   OR (name_en ILIKE '%dahua%' AND name_en ILIKE '%camera%')
      -- Ubiquiti/UniFi камери
   OR name_en ILIKE '%G5 Turret%'
   OR name_en ILIKE '%G5 Pro%'
   OR name_en ILIKE '%AI 360%'
   OR (name_en ILIKE '%unifi%' AND name_en ILIKE '%camera%')
   OR (name_en ILIKE '%ubiquiti%' AND name_en ILIKE '%camera%')
      -- LORGAR streaming камери (в cat 298) — IP камери
   OR (name_en ILIKE '%lorgar%' AND name_en ILIKE '%camera%')
      -- Generic surveillance wifi/poe cameras
   OR (name_en ILIKE '%ip camera%' AND (name_en ILIKE '%wi-fi%' OR name_en ILIKE '%poe%'))
  );

-- MOST IMOU камери в cat 153
UPDATE products
SET category_id = 100,
    updated_at  = NOW()
WHERE platform  = 'MOST'
  AND category_id IN (153)
  AND (name_en ILIKE '%camera%' OR name_bg ILIKE '%камера%');

-- ============================================================
-- ГРУПА 2: IMOU / Dahua CCTV комплекти → cat 242 "Комплект" (NDAA)
-- Засяга: [500, 153, 590, 295]
-- ============================================================

UPDATE products
SET category_id = 242,
    updated_at  = NOW()
WHERE platform = 'ASBIS'
  AND category_id IN (500, 153, 590, 295)
  AND (
      (name_en ILIKE '%imou%' OR name_en ILIKE '%dahua%')
   AND (
       name_en ILIKE '%kit%'
    OR name_en ILIKE '%cctv kit%'
    OR name_en ILIKE '%nvr kit%'
    OR name_en ILIKE '%wireless cctv%'
   )
  );

-- ============================================================
-- ГРУПА 3: Dahua / Imou NVR → "Записващи устройства" под IP системи
--           Dahua DVR → cat 238 "Записващи устройства - DVR"
-- Засяга: [546, 259, 265, 342]
-- ============================================================

-- NVR → Записващи устройства (IP системи)
UPDATE products
SET category_id = (SELECT id FROM categories WHERE slug = 'ip-sistemi-zapisvashti-ustrojstva'),
    updated_at  = NOW()
WHERE category_id IN (546, 259, 265, 342)
  AND (
      name_en ILIKE '%NVR%'
   OR name_bg ILIKE '%NVR%'
   OR name_en ILIKE '%network video recorder%'
   OR name_en ILIKE '%N3L%'       -- Tenda N3L серия NVR
  );

-- DVR → cat 238 "Записващи устройства - DVR"
UPDATE products
SET category_id = 238,
    updated_at  = NOW()
WHERE category_id IN (546, 259, 265, 342)
  AND (
      name_en ILIKE '%DVR%'
   OR name_bg ILIKE '%DVR%'
   OR name_en ILIKE '%pentabrid%'
   OR name_en ILIKE '%penta-brid%'
  );

-- ============================================================
-- ГРУПА 4: Dahua surveillance монитори → cat 249 "Монитори" (Видеонаблюдение)
-- Засяга: [362] Дисплеи (ASBIS), [50] Монитори (VALI BenQ PVS7)
-- ============================================================

UPDATE products
SET category_id = 249,
    updated_at  = NOW()
WHERE category_id IN (362, 50)
  AND (
      -- Dahua LM серия surveillance монитори
      name_en ILIKE '%dahua%'
      -- BenQ PVS7 — surveillance monitor (video camera monitor)
   OR name_bg ILIKE '%BenQ PVS%'
   OR name_en ILIKE '%BenQ PVS%'
   OR (name_bg ILIKE '%монитор%' AND name_bg ILIKE '%video%')
  );

-- ============================================================
-- ГРУПА 5: WD Purple / Seagate SkyHawk → cat 235 "Твърди дискове"
-- Засяга: [16] HDD 3.5" (VALI+ASBIS), [366] Твърди дискове (ASBIS)
-- ============================================================

UPDATE products
SET category_id = 235,
    updated_at  = NOW()
WHERE category_id IN (16, 366)
  AND (
      -- WD Purple серия (surveillance HDD)
      name_bg ILIKE '%wd purple%'
   OR name_en ILIKE '%wd purple%'
   OR name_en ILIKE '%purple pro%'
      -- Seagate SkyHawk серия (surveillance HDD)
   OR name_bg ILIKE '%skyhawk%'
   OR name_en ILIKE '%skyhawk%'
   OR name_en ILIKE '%surveillance%'
   OR name_bg ILIKE '%surveillance%'
      -- Seagate SkyHawk VX модели
   OR name_en ILIKE '%ST1000VX%'
   OR name_en ILIKE '%ST2000VX%'
   OR name_en ILIKE '%ST3000VX%'
   OR name_en ILIKE '%ST4000VX%'
   OR name_en ILIKE '%ST6000VX%'
   OR name_en ILIKE '%ST8000VX%'
      -- Toshiba S300 Surveillance
   OR (name_en ILIKE '%toshiba%' AND name_en ILIKE '%S300%')
   OR (name_bg ILIKE '%toshiba%' AND name_bg ILIKE '%S300%')
      -- Ubiquiti Enterprise HDD за NVR
   OR (name_en ILIKE '%ubiquiti%' AND name_en ILIKE '%HDD%')
   OR (name_en ILIKE '%ubiquiti%' AND name_en ILIKE '%hard%')
  );

-- ============================================================
-- ГРУПА 6: PoE суичове за видеонаблюдение (ASBIS/MOST)
--          → "Мрежови устройства" под IP системи
-- Засяга: [99] Рутери (ASBIS Imou/Ubiquiti), [342] Мрежов хардуер (MOST Tenda)
-- НЕ засяга: [109/110] VALI general networking switches
-- ============================================================

-- Imou PoE switches
UPDATE products
SET category_id = (SELECT id FROM categories WHERE slug = 'ip-sistemi-mrezhovi-ustrojstva'),
    updated_at  = NOW()
WHERE platform = 'ASBIS'
  AND category_id IN (99, 333)
  AND name_en ILIKE '%imou%'
  AND (name_en ILIKE '%switch%' OR name_en ILIKE '%poe%');

-- Tenda PoE switch (MOST)
UPDATE products
SET category_id = (SELECT id FROM categories WHERE slug = 'ip-sistemi-mrezhovi-ustrojstva'),
    updated_at  = NOW()
WHERE platform = 'MOST'
  AND category_id IN (342)
  AND name_en ILIKE '%tenda%'
  AND (name_en ILIKE '%poe%' OR name_en ILIKE '%switch%' OR name_en ILIKE '%TEF%');

-- Ubiquiti UniFi PoE switches → Мрежови устройства
UPDATE products
SET category_id = (SELECT id FROM categories WHERE slug = 'ip-sistemi-mrezhovi-ustrojstva'),
    updated_at  = NOW()
WHERE platform = 'ASBIS'
  AND category_id IN (99, 127, 128, 380)
  AND (name_en ILIKE '%ubiquiti%' OR name_en ILIKE '%unifi%')
  AND (name_en ILIKE '%switch%' OR name_en ILIKE '%poe%');

-- ============================================================
-- ГРУПА 7: Ubiquiti Cloud Gateway → cat 239 "Сървъри" (IP системи)
-- ============================================================

UPDATE products
SET category_id = 239,
    updated_at  = NOW()
WHERE category_id IN (99, 545)
  AND platform = 'ASBIS'
  AND (name_en ILIKE '%cloud gateway%' OR name_en ILIKE '%cloud key%');

-- ============================================================
-- ГРУПА 8: Dahua junction boxes → cat 246 "Стойки и основи за камери"
-- Засяга: [298] Аксесоари — Dahua waterproof junction box
-- ============================================================

UPDATE products
SET category_id = 246,
    updated_at  = NOW()
WHERE category_id = 298
  AND platform = 'ASBIS'
  AND name_en ILIKE '%dahua%'
  AND (
      name_en ILIKE '%junction box%'
   OR name_en ILIKE '%mount%'
   OR name_en ILIKE '%bracket%'
   OR name_en ILIKE '%waterproof%'
  );

-- ============================================================
-- ГРУПА 9: IMOU / Dahua microSD карти → cat 235 "Твърди дискове"
-- Засяга: [517] Флаш памети, [57] USB памети
-- ============================================================

UPDATE products
SET category_id = 235,
    updated_at  = NOW()
WHERE platform = 'ASBIS'
  AND category_id IN (517, 57)
  AND (name_en ILIKE '%imou%' OR name_en ILIKE '%dahua%')
  AND (
      name_en ILIKE '%micro sd%'
   OR name_en ILIKE '%microsd%'
   OR name_en ILIKE '%sdhc%'
   OR name_en ILIKE '%sdxc%'
  );

-- ============================================================
-- ГРУПА 10: Tenda IP камера (MOST) → cat 100
-- Засяга: [342] Мрежов хардуер — Tenda IC7-PRS PoE IP camera
-- ============================================================

UPDATE products
SET category_id = 100,
    updated_at  = NOW()
WHERE platform = 'MOST'
  AND category_id = 342
  AND name_en ILIKE '%tenda%'
  AND name_en ILIKE '%camera%';

-- ============================================================
-- ГРУПА 11: Ubiquiti AI Port / UniFi Protect (cat 548) → cat 100
-- ============================================================

UPDATE products
SET category_id = 100,
    updated_at  = NOW()
WHERE category_id = 548
  AND platform = 'ASBIS'
  AND (
      name_en ILIKE '%camera%'
   OR name_en ILIKE '%unifi protect%'
   OR name_en ILIKE '%ai port%'
  );

-- ============================================================
-- ГРУПА 12: Ubiquiti outdoor 4K camera (cat 593) → cat 100
-- ============================================================

UPDATE products
SET category_id = 100,
    updated_at  = NOW()
WHERE category_id = 593
  AND platform = 'ASBIS';

-- ============================================================
-- ГРУПА 13: VALI Interactive displays в cat 52 (вече под cat 236)
--           Скрий VALI NOT_AVAILABLE продукти (образователни, не surveillance)
-- ============================================================

UPDATE products
SET show_flag  = false,
    updated_at = NOW()
WHERE category_id = 52
  AND platform   = 'VALI'
  AND status    != 'AVAILABLE';

COMMIT;

-- ============================================================
-- ФИНАЛНА ПРОВЕРКА — Surveillance категории след всички промени
-- ============================================================
WITH RECURSIVE tree AS (
    SELECT id, name_bg, parent_id, show_flag, sort_order,
           0 AS depth, LPAD('', 0) || name_bg AS path_sort
    FROM categories WHERE id = 230
    UNION ALL
    SELECT c.id, c.name_bg, c.parent_id, c.show_flag, c.sort_order,
           t.depth + 1,
           t.path_sort || '/' || LPAD(c.sort_order::TEXT,3,'0') || c.name_bg
    FROM categories c JOIN tree t ON t.id = c.parent_id
)
SELECT
    REPEAT('  ', depth) || t.name_bg AS tree,
    t.id,
    t.show_flag,
    COUNT(p.id)                                           AS total,
    COUNT(p.id) FILTER (WHERE p.status = 'AVAILABLE')    AS available,
    COUNT(p.id) FILTER (WHERE p.platform = 'TEKRA')      AS tekra,
    COUNT(p.id) FILTER (WHERE p.platform = 'ASBIS')      AS asbis,
    COUNT(p.id) FILTER (WHERE p.platform = 'MOST')       AS most,
    COUNT(p.id) FILTER (WHERE p.platform = 'VALI')       AS vali
FROM tree t
LEFT JOIN products p ON p.category_id = t.id
GROUP BY t.id, t.name_bg, t.show_flag, t.depth, t.sort_order, t.path_sort
ORDER BY t.path_sort;

-- Провери дали са останали IMOU/Dahua продукти в грешни категории
SELECT c.id, c.name_bg, COUNT(*) AS remaining
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE p.category_id IN (500, 153, 502, 546, 362, 342, 590, 295)
  AND p.platform = 'ASBIS'
  AND (
      p.name_en ILIKE '%imou%'
   OR p.name_en ILIKE '%dahua%'
   OR p.name_en ILIKE '%camera%'
   OR p.name_en ILIKE '%nvr%'
  )
GROUP BY c.id, c.name_bg
HAVING COUNT(*) > 0
ORDER BY c.id;
