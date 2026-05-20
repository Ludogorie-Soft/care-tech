-- =============================================================================
-- Скрипт за пренареждане на подкатегории (sort_order)
-- Изпълни СЛЕД rename_categories.sql
-- Изпълни ръчно: psql -U <user> -d <db> -f reorder_subcategories.sql
-- НЕ е Flyway миграция — може да се изпълнява многократно (UPDATE е идемпотентен)
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. КОМПЮТЪРНИ КОМПОНЕНТИ
-- =============================================================================

DO $$
DECLARE parent_id_val BIGINT;
BEGIN
  SELECT id INTO parent_id_val FROM categories WHERE name_bg = 'Компютърни компоненти' AND parent_id IS NULL;

  UPDATE categories SET sort_order = 1,  updated_at = NOW() WHERE name_bg = 'Дъни платки'                          AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 2,  updated_at = NOW() WHERE name_bg = 'Процесори'                            AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 3,  updated_at = NOW() WHERE name_bg = 'Охладители за процесори'              AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 4,  updated_at = NOW() WHERE name_bg = 'Памети'                               AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 5,  updated_at = NOW() WHERE name_bg = 'Памети за лаптоп'                     AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 6,  updated_at = NOW() WHERE name_bg = 'Видео карти'                          AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 7,  updated_at = NOW() WHERE name_bg = 'Охладители за видео карти'            AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 8,  updated_at = NOW() WHERE name_bg = 'Захранвания'                          AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 9,  updated_at = NOW() WHERE name_bg = 'Кутии за компютри'                    AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 10, updated_at = NOW() WHERE name_bg = 'Оптични устройства'                   AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 11, updated_at = NOW() WHERE name_bg = 'Звукови карти'                        AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 12, updated_at = NOW() WHERE external_id = 377;  -- SSD (Solid State Drive)
  UPDATE categories SET sort_order = 13, updated_at = NOW() WHERE name_bg ILIKE 'Хард дискове%3.5%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 14, updated_at = NOW() WHERE name_bg ILIKE 'Хард дискове%2.5%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 15, updated_at = NOW() WHERE name_bg = 'Вентилатори'                          AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 16, updated_at = NOW() WHERE name_bg = 'Контролери за вентилатори'            AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 17, updated_at = NOW() WHERE name_bg ILIKE 'Термо пасти%'                     AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 18, updated_at = NOW() WHERE name_bg ILIKE 'Контролери%RAID%'                 AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 19, updated_at = NOW() WHERE name_bg ILIKE 'Чекмеджета%'                      AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 20, updated_at = NOW() WHERE name_bg ILIKE 'TV Тунери%'                       AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 21, updated_at = NOW() WHERE name_bg ILIKE 'Входно-изходни%'                  AND parent_id = parent_id_val;
END $$;

-- =============================================================================
-- 2. ЛАПТОПИ, ТАБЛЕТИ И АКСЕСОАРИ
-- =============================================================================

DO $$
DECLARE parent_id_val BIGINT;
BEGIN
  SELECT id INTO parent_id_val FROM categories WHERE name_bg ILIKE 'Лаптопи%' AND parent_id IS NULL;

  UPDATE categories SET sort_order = 1,  updated_at = NOW() WHERE name_bg = 'Лаптопи'                             AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 2,  updated_at = NOW() WHERE name_bg ILIKE 'Чанти за лаптопи%'               AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 3,  updated_at = NOW() WHERE external_id = 536;  -- Зарядни за лаптопи
  UPDATE categories SET sort_order = 4,  updated_at = NOW() WHERE name_bg ILIKE 'Охлаждащи поставки%'             AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 5,  updated_at = NOW() WHERE name_bg ILIKE 'Аксесоари за лаптопи%'           AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 6,  updated_at = NOW() WHERE name_bg ILIKE 'Заключващи устройства%'          AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 7,  updated_at = NOW() WHERE name_bg = 'Таблети'                             AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 8,  updated_at = NOW() WHERE name_bg = 'Графични таблети'                    AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 9,  updated_at = NOW() WHERE name_bg ILIKE 'Калъфи за таблети%'              AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 10, updated_at = NOW() WHERE name_bg ILIKE 'Поставки за таблет%'             AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 11, updated_at = NOW() WHERE name_bg ILIKE 'Аксесоари за графични таблети%'  AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 12, updated_at = NOW() WHERE name_bg ILIKE 'Колички и шкафове%'              AND parent_id = parent_id_val;
END $$;

-- =============================================================================
-- 3. КОМПЮТЪРНА ПЕРИФЕРИЯ
-- =============================================================================

DO $$
DECLARE parent_id_val BIGINT;
BEGIN
  SELECT id INTO parent_id_val FROM categories WHERE name_bg ILIKE 'Компютърна периферия%' AND parent_id IS NULL;

  UPDATE categories SET sort_order = 1,  updated_at = NOW() WHERE name_bg = 'Мишки'                               AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 2,  updated_at = NOW() WHERE name_bg ILIKE 'Падове за мишки%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 3,  updated_at = NOW() WHERE name_bg = 'Клавиатури'                          AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 4,  updated_at = NOW() WHERE name_bg = 'Слушалки'                            AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 5,  updated_at = NOW() WHERE name_bg ILIKE 'Слушалки (тапи)%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 6,  updated_at = NOW() WHERE name_bg = 'Микрофони'                           AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 7,  updated_at = NOW() WHERE name_bg ILIKE 'Звукови системи%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 8,  updated_at = NOW() WHERE name_bg ILIKE 'USB памети%'                     AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 9,  updated_at = NOW() WHERE name_bg ILIKE 'Външни дискове%'                 AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 10, updated_at = NOW() WHERE name_bg ILIKE 'Външни SSD%'                     AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 11, updated_at = NOW() WHERE name_bg ILIKE 'USB хъбове%'                     AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 12, updated_at = NOW() WHERE name_bg ILIKE 'Уеб камери%'                     AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 13, updated_at = NOW() WHERE name_bg ILIKE 'Четци за карти%'                 AND parent_id = parent_id_val;
END $$;

-- =============================================================================
-- 4. РУТЕРИ И МРЕЖОВО ОБОРУДВАНЕ (external_id = 412)
-- =============================================================================

DO $$
DECLARE parent_id_val BIGINT;
BEGIN
  SELECT id INTO parent_id_val FROM categories WHERE external_id = 412;

  UPDATE categories SET sort_order = 1,  updated_at = NOW() WHERE name_bg = 'Рутери'                              AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 2,  updated_at = NOW() WHERE name_bg ILIKE 'Безжични рутери%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 3,  updated_at = NOW() WHERE external_id = 420;  -- Суичове - неуправляеми
  UPDATE categories SET sort_order = 4,  updated_at = NOW() WHERE external_id = 523;  -- Суичове - управляеми
  UPDATE categories SET sort_order = 5,  updated_at = NOW() WHERE name_bg = 'Access Point'                        AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 6,  updated_at = NOW() WHERE name_bg ILIKE 'Безжични адаптери%'              AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 7,  updated_at = NOW() WHERE name_bg ILIKE 'Блутут адаптери%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 8,  updated_at = NOW() WHERE name_bg ILIKE 'Мрежови карти%'                  AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 9,  updated_at = NOW() WHERE name_bg ILIKE 'Защитни стени%'                  AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 10, updated_at = NOW() WHERE name_bg ILIKE 'KVM%'                            AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 11, updated_at = NOW() WHERE name_bg ILIKE 'Powerline%'                      AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 12, updated_at = NOW() WHERE external_id = 413;  -- IP камери и смарт у-ва
  UPDATE categories SET sort_order = 13, updated_at = NOW() WHERE name_bg ILIKE 'Кабелни канали%'                 AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 14, updated_at = NOW() WHERE external_id = 418;  -- Комуникационни шкафове - RACK
END $$;

-- =============================================================================
-- 5. КАБЕЛИ
-- =============================================================================

DO $$
DECLARE parent_id_val BIGINT;
BEGIN
  SELECT id INTO parent_id_val FROM categories WHERE name_bg = 'Кабели' AND parent_id IS NULL;

  UPDATE categories SET sort_order = 1,  updated_at = NOW() WHERE name_bg ILIKE 'Видео кабели%'                   AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 2,  updated_at = NOW() WHERE name_bg ILIKE 'РС кабели%'                      AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 3,  updated_at = NOW() WHERE name_bg ILIKE 'Мрежови кабели%'                 AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 4,  updated_at = NOW() WHERE name_bg ILIKE 'Аудио кабели%'                   AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 5,  updated_at = NOW() WHERE name_bg ILIKE 'Кабели за принтери%'             AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 6,  updated_at = NOW() WHERE name_bg ILIKE 'Кабели за мобилни%'              AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 7,  updated_at = NOW() WHERE name_bg ILIKE 'Захранващи кабели%'              AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 8,  updated_at = NOW() WHERE name_bg ILIKE 'Оптични кабели%'                 AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 9,  updated_at = NOW() WHERE name_bg ILIKE 'Антенни кабели%'                 AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 10, updated_at = NOW() WHERE name_bg ILIKE 'Адаптери, конвертори%'           AND parent_id = parent_id_val;
END $$;

-- =============================================================================
-- 6. ФОТО И ВИДЕО АКСЕСОАРИ
-- =============================================================================

DO $$
DECLARE parent_id_val BIGINT;
BEGIN
  SELECT id INTO parent_id_val FROM categories WHERE name_bg ILIKE 'Фото%' AND parent_id IS NULL;

  UPDATE categories SET sort_order = 1,  updated_at = NOW() WHERE name_bg ILIKE 'Карти памет%'                    AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 2,  updated_at = NOW() WHERE name_bg ILIKE 'Фотоалбуми%'                     AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 3,  updated_at = NOW() WHERE name_bg ILIKE 'Стативи%'                        AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 4,  updated_at = NOW() WHERE name_bg ILIKE 'Чанти за камери%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 5,  updated_at = NOW() WHERE name_bg ILIKE 'Фото рамки%'                     AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 6,  updated_at = NOW() WHERE name_bg ILIKE 'Селфи%'                          AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 7,  updated_at = NOW() WHERE name_bg ILIKE 'Аксесоари за обективи%'          AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 8,  updated_at = NOW() WHERE name_bg ILIKE 'Други фото%'                     AND parent_id = parent_id_val;
END $$;

-- =============================================================================
-- 7. ГЕЙМЪРСКА ПЕРИФЕРИЯ
-- =============================================================================

DO $$
DECLARE parent_id_val BIGINT;
BEGIN
  SELECT id INTO parent_id_val FROM categories WHERE name_bg ILIKE 'Геймърска периферия%' AND parent_id IS NULL;

  UPDATE categories SET sort_order = 1,  updated_at = NOW() WHERE name_bg ILIKE 'Геймърски мишки%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 2,  updated_at = NOW() WHERE name_bg ILIKE 'Геймърски клавиатури%'           AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 3,  updated_at = NOW() WHERE name_bg ILIKE 'Геймърски падове%'               AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 4,  updated_at = NOW() WHERE name_bg ILIKE 'Геймърски слушалки%'             AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 5,  updated_at = NOW() WHERE name_bg ILIKE 'Геймърски бюра%'                 AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 6,  updated_at = NOW() WHERE name_bg ILIKE 'Геймърски столове%'              AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 7,  updated_at = NOW() WHERE name_bg ILIKE 'Компютърни и геймърски очила%'   AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 8,  updated_at = NOW() WHERE name_bg ILIKE 'Гейминг конзоли%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 9,  updated_at = NOW() WHERE name_bg ILIKE 'Волани и педали%'                AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 10, updated_at = NOW() WHERE name_bg ILIKE 'Геймпадове%'                     AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 11, updated_at = NOW() WHERE name_bg ILIKE 'Аксесоари за Волани%'            AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 12, updated_at = NOW() WHERE name_bg ILIKE 'Геймърски аксесоари%'            AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 13, updated_at = NOW() WHERE name_bg ILIKE 'Фигурки%'                        AND parent_id = parent_id_val;
  UPDATE categories SET sort_order = 14, updated_at = NOW() WHERE name_bg ILIKE 'Дрехи и аксесоари%'             AND parent_id = parent_id_val;
END $$;

-- =============================================================================
-- Проверка на резултата
-- =============================================================================
SELECT
  p.name_bg  AS parent,
  c.sort_order,
  c.name_bg  AS subcategory
FROM categories c
JOIN categories p ON c.parent_id = p.id
WHERE p.name_bg IN (
  'Компютърни компоненти',
  'Компютърна периферия',
  'Геймърска периферия',
  'Кабели'
)
   OR p.external_id IN (412)
   OR p.name_bg ILIKE 'Лаптопи%'
   OR p.name_bg ILIKE 'Фото%'
ORDER BY p.name_bg, c.sort_order;

COMMIT;
