-- =============================================================================
-- Скрипт 10: Поправка на sort_order след реорганизацията на Asbis категории
-- Проблем: след скрипт 7 Asbis подкатегориите се добавиха с sort_order=0,
-- което ги поставя ПРЕДИ наредените Vali подкатегории.
-- Решение: Asbis-derived подкатегории отиват накрая (sort_order = 100+),
-- а познатите Vali подкатегории се наредят коректно.
--
-- Изпълни СЛЕД: 9_hide_empty_categories.sql
-- Може да се изпълнява многократно (идемпотентен)
-- =============================================================================

BEGIN;

-- =============================================================================
-- СТЪПКА 1: Вкарай всички Asbis-derived подкатегории накрая
-- (sort_order = 100 за видимите — ще се презапишат в СТЪПКА 2 за познатите)
-- =============================================================================

DO $$
DECLARE cat_id BIGINT;
BEGIN
    -- Компютърни компоненти (external_id = 373)
    SELECT id INTO cat_id FROM categories WHERE external_id = 373;
    UPDATE categories
       SET sort_order  = 100,
           updated_at  = NOW()
     WHERE parent_id   = cat_id
       AND platform    = 'ASBIS'
       AND show_flag   = true
       AND sort_order  = 0;

    -- Компютри / Компютърни системи (external_id = 549)
    SELECT id INTO cat_id FROM categories WHERE external_id = 549;
    UPDATE categories
       SET sort_order  = 100,
           updated_at  = NOW()
     WHERE parent_id   = cat_id
       AND platform    = 'ASBIS'
       AND show_flag   = true
       AND sort_order  = 0;

    -- Рутери и мрежово оборудване (external_id = 412)
    SELECT id INTO cat_id FROM categories WHERE external_id = 412;
    UPDATE categories
       SET sort_order  = 100,
           updated_at  = NOW()
     WHERE parent_id   = cat_id
       AND platform    = 'ASBIS'
       AND show_flag   = true
       AND sort_order  = 0;

    -- Лаптопи (external_id = 482)
    SELECT id INTO cat_id FROM categories WHERE external_id = 482;
    UPDATE categories
       SET sort_order  = 100,
           updated_at  = NOW()
     WHERE parent_id   = cat_id
       AND platform    = 'ASBIS'
       AND show_flag   = true
       AND sort_order  = 0;

    -- Принтери
    SELECT id INTO cat_id FROM categories WHERE name_bg ILIKE 'Принтери%' AND parent_id IS NULL;
    IF cat_id IS NOT NULL THEN
        UPDATE categories
           SET sort_order  = 100,
               updated_at  = NOW()
         WHERE parent_id   = cat_id
           AND platform    = 'ASBIS'
           AND show_flag   = true
           AND sort_order  = 0;
    END IF;

    -- Сторидж
    SELECT id INTO cat_id FROM categories WHERE name_bg ILIKE 'Сторидж%' AND parent_id IS NULL;
    IF cat_id IS NOT NULL THEN
        UPDATE categories
           SET sort_order  = 100,
               updated_at  = NOW()
         WHERE parent_id   = cat_id
           AND platform    = 'ASBIS'
           AND show_flag   = true
           AND sort_order  = 0;
    END IF;
END $$;

-- =============================================================================
-- СТЪПКА 2: Пренареди КОМПЮТЪРНИ КОМПОНЕНТИ — познатите подкатегории
-- (същата логика като скрипт 4, но включва и Asbis-derived Памет, SSD и др.)
-- =============================================================================

DO $$
DECLARE parent_id_val BIGINT;
BEGIN
  SELECT id INTO parent_id_val FROM categories WHERE external_id = 373;

  -- Vali subcategories (наредени 1-21 от скрипт 4)
  UPDATE categories SET sort_order = 1,  updated_at = NOW() WHERE name_bg = 'Дъни платки'                           AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 2,  updated_at = NOW() WHERE name_bg = 'Процесори'                             AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 3,  updated_at = NOW() WHERE name_bg = 'Охладители за процесори'               AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 4,  updated_at = NOW() WHERE name_bg = 'Памети'                                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 5,  updated_at = NOW() WHERE name_bg = 'Памети за лаптоп'                      AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 6,  updated_at = NOW() WHERE name_bg = 'Видео карти'                           AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 7,  updated_at = NOW() WHERE name_bg = 'Охладители за видео карти'             AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 8,  updated_at = NOW() WHERE name_bg = 'Захранвания'                           AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 9,  updated_at = NOW() WHERE name_bg = 'Кутии за компютри'                     AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 10, updated_at = NOW() WHERE name_bg = 'Оптични устройства'                    AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 11, updated_at = NOW() WHERE name_bg = 'Звукови карти'                         AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 12, updated_at = NOW() WHERE external_id = 377;  -- SSD (Solid State Drive)
  UPDATE categories SET sort_order = 13, updated_at = NOW() WHERE name_bg ILIKE 'Хард дискове%3.5%'                 AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 14, updated_at = NOW() WHERE name_bg ILIKE 'Хард дискове%2.5%'                 AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 15, updated_at = NOW() WHERE name_bg = 'Вентилатори'                           AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 16, updated_at = NOW() WHERE name_bg = 'Контролери за вентилатори'             AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 17, updated_at = NOW() WHERE name_bg ILIKE 'Термо пасти%'                      AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 18, updated_at = NOW() WHERE name_bg ILIKE 'Контролери%RAID%'                  AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 19, updated_at = NOW() WHERE name_bg ILIKE 'Чекмеджета%'                       AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 20, updated_at = NOW() WHERE name_bg ILIKE 'TV Тунери%'                        AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 21, updated_at = NOW() WHERE name_bg ILIKE 'Входно-изходни%'                   AND parent_id = parent_id_val;

  -- Asbis-derived Памет подкатегории — след Vali-те (sort_order 101+)
  UPDATE categories SET sort_order = 101, updated_at = NOW() WHERE name_bg ILIKE 'DDR4%'       AND parent_id = parent_id_val AND platform = 'ASBIS';
  UPDATE categories SET sort_order = 102, updated_at = NOW() WHERE name_bg ILIKE 'DDR5%'       AND parent_id = parent_id_val AND platform = 'ASBIS';
  UPDATE categories SET sort_order = 103, updated_at = NOW() WHERE name_bg ILIKE 'DDR3%'       AND parent_id = parent_id_val AND platform = 'ASBIS';
  UPDATE categories SET sort_order = 104, updated_at = NOW() WHERE name_bg ILIKE '%SODIMM%'    AND parent_id = parent_id_val AND platform = 'ASBIS';
  UPDATE categories SET sort_order = 105, updated_at = NOW() WHERE name_bg ILIKE '%SO-DIMM%'   AND parent_id = parent_id_val AND platform = 'ASBIS';
END $$;

-- =============================================================================
-- СТЪПКА 3: Re-run на top-level sort_order (от скрипт 3, идемпотентен)
-- =============================================================================

UPDATE categories SET sort_order = 1,  updated_at = NOW() WHERE external_id = 549;
UPDATE categories SET sort_order = 2,  updated_at = NOW() WHERE name_bg = 'Компютърни компоненти' AND parent_id IS NULL;
UPDATE categories SET sort_order = 3,  updated_at = NOW() WHERE name_bg ILIKE 'Лаптопи%'          AND parent_id IS NULL;
UPDATE categories SET sort_order = 4,  updated_at = NOW() WHERE external_id = 394;
UPDATE categories SET sort_order = 5,  updated_at = NOW() WHERE name_bg ILIKE 'Компютърна периферия%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 6,  updated_at = NOW() WHERE name_bg ILIKE 'Геймърска периферия%'  AND parent_id IS NULL;
UPDATE categories SET sort_order = 7,  updated_at = NOW() WHERE name_bg ILIKE 'Принтери%'             AND parent_id IS NULL;
UPDATE categories SET sort_order = 8,  updated_at = NOW() WHERE external_id = 412;
UPDATE categories SET sort_order = 9,  updated_at = NOW() WHERE name_bg ILIKE 'Видеонаблюдение%'      AND parent_id IS NULL;
UPDATE categories SET sort_order = 10, updated_at = NOW() WHERE name_bg ILIKE 'Непрекъсваеми%'         AND parent_id IS NULL;
UPDATE categories SET sort_order = 11, updated_at = NOW() WHERE name_bg ILIKE 'Сторидж%'              AND parent_id IS NULL;
UPDATE categories SET sort_order = 12, updated_at = NOW() WHERE name_bg ILIKE 'Батерии%'              AND parent_id IS NULL;
UPDATE categories SET sort_order = 13, updated_at = NOW() WHERE name_bg = 'Кабели'                    AND parent_id IS NULL;
UPDATE categories SET sort_order = 14, updated_at = NOW() WHERE name_bg ILIKE 'Фото%'                 AND parent_id IS NULL;
UPDATE categories SET sort_order = 15, updated_at = NOW() WHERE name_bg ILIKE 'TV%'                   AND parent_id IS NULL;
UPDATE categories SET sort_order = 16, updated_at = NOW() WHERE name_bg ILIKE 'Електроника%'           AND parent_id IS NULL;
UPDATE categories SET sort_order = 17, updated_at = NOW() WHERE external_id = 469;
UPDATE categories SET sort_order = 18, updated_at = NOW() WHERE external_id = 459;
UPDATE categories SET sort_order = 19, updated_at = NOW() WHERE name_bg ILIKE 'Проектори%'            AND parent_id IS NULL;
UPDATE categories SET sort_order = 20, updated_at = NOW() WHERE name_bg ILIKE 'Софтуер%'              AND parent_id IS NULL;
UPDATE categories SET sort_order = 21, updated_at = NOW() WHERE name_bg ILIKE 'Водно охлаждане%'       AND parent_id IS NULL;
UPDATE categories SET sort_order = 22, updated_at = NOW() WHERE name_bg ILIKE 'VR%'                   AND parent_id IS NULL;
UPDATE categories SET sort_order = 23, updated_at = NOW() WHERE name_bg ILIKE 'Офис продукти%'        AND parent_id IS NULL;
UPDATE categories SET sort_order = 24, updated_at = NOW() WHERE external_id = 491;
UPDATE categories SET sort_order = 25, updated_at = NOW() WHERE name_bg = 'Дребни домакински уреди'   AND parent_id IS NULL;
UPDATE categories SET sort_order = 26, updated_at = NOW() WHERE name_bg = 'Дигитална сигнализация IDS' AND parent_id IS NULL;
UPDATE categories SET sort_order = 27, updated_at = NOW() WHERE name_bg = 'Роботизирани решения'       AND parent_id IS NULL;
UPDATE categories SET sort_order = 28, updated_at = NOW() WHERE name_bg = 'STEM'                       AND parent_id IS NULL;
UPDATE categories SET sort_order = 29, updated_at = NOW() WHERE external_id = 708;

-- =============================================================================
-- Проверка
-- =============================================================================

-- Top-level categories
SELECT id, external_id, name_bg, sort_order, show_flag
FROM categories
WHERE parent_id IS NULL
ORDER BY sort_order, name_bg;

-- Компютърни компоненти подкатегории
SELECT c.sort_order, c.name_bg, c.platform, c.show_flag,
       (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.show_flag = true) AS visible_products
FROM categories c
WHERE c.parent_id = (SELECT id FROM categories WHERE external_id = 373)
ORDER BY c.sort_order, c.name_bg;

COMMIT;
