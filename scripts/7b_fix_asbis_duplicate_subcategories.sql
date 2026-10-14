-- =============================================================================
-- Скрипт 7b: Поправка на Asbis дублиращи подкатегории
-- Проблем: след скрипт 7 децата на Asbis roots се преместиха под Vali roots,
-- но вече съществуват Vali категории с едни и същи/сходни имена (мн.ч.).
-- Напр: "Видео карта" (Asbis) дублира "Видео карти" (Vali)
--       "Памет" (Asbis child)  дублира "Памети" (Vali)
--
-- Стратегия:
--   1. Препрати продуктите от Asbis дублика → Vali канонична
--   2. Скрий Asbis дублика
--
-- Изпълни СЛЕД: 7_reorganize_asbis_categories.sql
-- Може да се изпълнява многократно (идемпотентен благодарение на WHERE platform='ASBIS')
-- =============================================================================

BEGIN;

-- =============================================================================
-- ДИАГНОСТИКА: всички Asbis подкатегории под Компютърни компоненти
-- Прегледай списъка и добавяй нови merge редове ако има още дублики.
-- =============================================================================
SELECT
    c.id,
    c.name_bg,
    c.platform,
    c.show_flag,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id)                  AS total_products,
    (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id AND p.show_flag = true) AS visible_products
FROM categories c
WHERE c.parent_id = (SELECT id FROM categories WHERE external_id = 373)
ORDER BY c.show_flag DESC, c.platform, c.name_bg;

-- =============================================================================
-- HELPER ФУНКЦИЯ: merge Asbis дублик → Vali канонична
-- =============================================================================
CREATE OR REPLACE FUNCTION merge_asbis_duplicate(
    p_asbis_name   TEXT,      -- name_bg на Asbis дублика
    p_vali_name    TEXT,      -- name_bg на Vali каноничната
    p_parent_eid   BIGINT     -- external_id на общия родител
) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    v_parent_id  BIGINT;
    v_asbis_id   BIGINT;
    v_vali_id    BIGINT;
    v_moved      INT;
BEGIN
    SELECT id INTO v_parent_id FROM categories WHERE external_id = p_parent_eid;
    IF v_parent_id IS NULL THEN
        RAISE NOTICE '[SKIP] Parent external_id=% not found', p_parent_eid;
        RETURN;
    END IF;

    SELECT id INTO v_asbis_id
      FROM categories
     WHERE name_bg = p_asbis_name AND platform = 'ASBIS' AND parent_id = v_parent_id;

    IF v_asbis_id IS NULL THEN
        -- Опитай и без parent constraint (може да е директно дете на root преди migration)
        SELECT id INTO v_asbis_id
          FROM categories
         WHERE name_bg = p_asbis_name AND platform = 'ASBIS';
    END IF;

    IF v_asbis_id IS NULL THEN
        RAISE NOTICE '[SKIP] Asbis duplicate "%" not found', p_asbis_name;
        RETURN;
    END IF;

    SELECT id INTO v_vali_id
      FROM categories
     WHERE name_bg = p_vali_name AND parent_id = v_parent_id;

    IF v_vali_id IS NULL THEN
        RAISE NOTICE '[SKIP] Vali canonical "%" not found under parent %', p_vali_name, v_parent_id;
        RETURN;
    END IF;

    IF v_asbis_id = v_vali_id THEN
        RAISE NOTICE '[SKIP] Same category, nothing to merge for "%"', p_asbis_name;
        RETURN;
    END IF;

    -- Препрати продуктите
    UPDATE products SET category_id = v_vali_id, updated_at = NOW()
     WHERE category_id = v_asbis_id;
    GET DIAGNOSTICS v_moved = ROW_COUNT;

    -- Скрий Asbis дублика
    UPDATE categories SET show_flag = false, updated_at = NOW()
     WHERE id = v_asbis_id;

    RAISE NOTICE '[OK] "%" → "%": % продукта преместени', p_asbis_name, p_vali_name, v_moved;
END;
$$;

-- =============================================================================
-- КОМПЮТЪРНИ КОМПОНЕНТИ  (external_id = 373)
-- Asbis дублик                  →  Vali канонична
-- =============================================================================
SELECT merge_asbis_duplicate('Видео карта',        'Видео карти',             373);
SELECT merge_asbis_duplicate('Памет',               'Памети',                  373);
SELECT merge_asbis_duplicate('Памети',              'Памети',                  373);  -- ако е наречена в мн.ч.
SELECT merge_asbis_duplicate('Дънна платка',        'Дъни платки',             373);
SELECT merge_asbis_duplicate('Процесор',            'Процесори',               373);
SELECT merge_asbis_duplicate('Компютърна кутия',    'Кутии за компютри',       373);
SELECT merge_asbis_duplicate('Охладител',           'Охладители за процесори', 373);
SELECT merge_asbis_duplicate('Захранване',          'Захранвания',             373);
SELECT merge_asbis_duplicate('Звукова Карта',       'Звукови карти',           373);
SELECT merge_asbis_duplicate('Оптично устройство',  'Оптични устройства',      373);
SELECT merge_asbis_duplicate('Твърд диск',          'Хард дискове 3.5"',       373);

-- =============================================================================
-- ЛАПТОПИ  (external_id = 482)
-- =============================================================================
SELECT merge_asbis_duplicate('Лаптоп',              'Лаптопи',                 482);
SELECT merge_asbis_duplicate('Таблет',              'Таблети',                 482);

-- =============================================================================
-- КОМПЮТЪРНА ПЕРИФЕРИЯ  (external_id = 398)
-- =============================================================================
SELECT merge_asbis_duplicate('Слушалка',            'Слушалки',                398);
SELECT merge_asbis_duplicate('Мишка',               'Мишки',                   398);
SELECT merge_asbis_duplicate('Клавиатура',          'Клавиатури',              398);
SELECT merge_asbis_duplicate('Уеб камера',          'Уеб камери',              398);

-- =============================================================================
-- ИЗЧИСТИ HELPER ФУНКЦИЯТА
-- =============================================================================
DROP FUNCTION IF EXISTS merge_asbis_duplicate(TEXT, TEXT, BIGINT);

-- =============================================================================
-- ФИНАЛНА ДИАГНОСТИКА: всички ВИДИМИ Asbis категории, наредени по родител
-- Прегледай ръчно за оставащи дублики и добавяй нови merge_asbis_duplicate() редове.
-- =============================================================================
SELECT
    COALESCE(p.name_bg, '(root)') AS parent,
    c.name_bg,
    c.platform,
    (SELECT COUNT(*) FROM products pr WHERE pr.category_id = c.id AND pr.show_flag = true) AS visible_products
FROM categories c
LEFT JOIN categories p ON p.id = c.parent_id
WHERE c.platform = 'ASBIS'
  AND c.show_flag = true
ORDER BY p.name_bg NULLS FIRST, c.name_bg;

COMMIT;
