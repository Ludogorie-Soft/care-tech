-- =============================================================================
-- Скрипт 9: Скриване на празни категории
-- Скрива категории без видими продукти (show=true) и без видими подкатегории.
-- Итеративен — всеки кръг скрива нови "паднали" категории.
--
-- Изпълни СЛЕД: 7_reorganize_asbis_categories.sql
-- НЕ е Flyway миграция — изпълнява се ръчно след sync + 8_asbis_filters.sql
-- =============================================================================

BEGIN;

-- Покажи какво ще бъде скрито (само диагностика преди промените)
SELECT
    c.id,
    c.name_bg,
    c.platform,
    p.name_bg AS parent_name,
    (SELECT COUNT(*) FROM products pr WHERE pr.category_id = c.id AND pr.show_flag = true) AS visible_products,
    (SELECT COUNT(*) FROM categories ch WHERE ch.parent_id = c.id AND ch.show_flag = true) AS visible_children
FROM categories c
LEFT JOIN categories p ON c.parent_id = p.id
WHERE c.show_flag = true
  AND NOT EXISTS (
      SELECT 1 FROM categories ch
      WHERE ch.parent_id = c.id AND ch.show_flag = true
  )
  AND NOT EXISTS (
      SELECT 1 FROM products pr
      WHERE pr.category_id = c.id AND pr.show_flag = true
  )
ORDER BY p.name_bg NULLS LAST, c.name_bg;

-- =============================================================================
-- Итеративно скриване: всеки pass скрива leaf категории без видимо съдържание.
-- Повтаря се докато няма повече промени (родителите се скриват след децата).
-- =============================================================================

DO $$
DECLARE
    changed INT := 1;
    total   INT := 0;
    pass    INT := 0;
BEGIN
    WHILE changed > 0 LOOP
        pass := pass + 1;

        UPDATE categories
        SET show_flag  = false,
            updated_at = NOW()
        WHERE show_flag = true
          AND NOT EXISTS (
              SELECT 1 FROM categories ch
              WHERE ch.parent_id = categories.id
                AND ch.show_flag = true
          )
          AND NOT EXISTS (
              SELECT 1 FROM products p
              WHERE p.category_id = categories.id
                AND p.show_flag = true
          );

        GET DIAGNOSTICS changed = ROW_COUNT;
        total := total + changed;
        RAISE NOTICE 'Pass %: скрити % категории', pass, changed;
    END LOOP;

    RAISE NOTICE 'Общо скрити категории: %', total;
END $$;

-- =============================================================================
-- Проверка на резултата
-- =============================================================================

-- Визуализация на скрите категории по родител
SELECT
    COALESCE(p.name_bg, '(root)') AS parent,
    COUNT(*) FILTER (WHERE c.show_flag = false) AS hidden,
    COUNT(*) FILTER (WHERE c.show_flag = true)  AS visible
FROM categories c
LEFT JOIN categories p ON c.parent_id = p.id
GROUP BY p.name_bg
HAVING COUNT(*) FILTER (WHERE c.show_flag = false) > 0
ORDER BY hidden DESC;

COMMIT;
