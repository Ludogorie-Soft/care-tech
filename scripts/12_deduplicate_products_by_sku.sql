-- =============================================================================
-- Скрипт 12: Дедупликация на продукти по SKU
-- Проблем: един физически продукт (напр. Kingston KVR32N22D8-16) се внася
-- едновременно от Vali, Tekra и Asbis — три отделни реда.
--
-- Стратегия: за всяка група с еднакъв SKU → запази най-добрия (Vali > Tekra > Asbis,
-- при равни платформи — най-ниска цена), останалите скрий.
--
-- Изпълни ръчно след sync. Може да се изпълнява многократно (идемпотентен).
-- =============================================================================

BEGIN;

-- =============================================================================
-- ДИАГНОСТИКА: брой дублики по SKU
-- =============================================================================
SELECT
    p.sku,
    COUNT(*)                                             AS copies,
    array_agg(p.platform ORDER BY p.platform)           AS platforms,
    array_agg(p.final_price ORDER BY p.final_price)     AS prices,
    MIN(p.final_price)                                   AS best_price
FROM products p
WHERE p.sku IS NOT NULL
  AND p.show_flag = true
GROUP BY p.sku
HAVING COUNT(DISTINCT p.platform) > 1
ORDER BY copies DESC
LIMIT 50;

-- =============================================================================
-- ДЕДУПЛИКАЦИЯ
-- =============================================================================

WITH ranked AS (
    SELECT
        p.id,
        p.sku,
        p.platform,
        p.final_price,
        p.show_flag,
        ROW_NUMBER() OVER (
            PARTITION BY p.sku
            ORDER BY
                -- Основен приоритет: по-ниска цена напред
                p.final_price ASC NULLS LAST,
                -- При равна цена: предпочитай по-добрата платформа (Vali > Tekra > Asbis)
                CASE p.platform
                    WHEN 'VALI'   THEN 1
                    WHEN 'TEKRA'  THEN 2
                    WHEN 'ASBIS'  THEN 3
                    ELSE 4
                END,
                -- Накрая по id за детерминизъм
                p.id ASC
        ) AS rn
    FROM products p
    WHERE p.sku IS NOT NULL
      AND p.show_flag = true
),
skus_with_dupes AS (
    SELECT sku FROM ranked GROUP BY sku HAVING MAX(rn) > 1
)
UPDATE products
SET show_flag  = false,
    updated_at = NOW()
WHERE id IN (
    SELECT r.id
    FROM ranked r
    WHERE r.rn > 1
      AND r.sku IN (SELECT sku FROM skus_with_dupes)
);

-- =============================================================================
-- РЕЗУЛТАТ
-- =============================================================================
SELECT
    p.sku,
    p.platform,
    p.show_flag,
    p.final_price,
    p.name_bg
FROM products p
WHERE p.sku IN (
    SELECT sku FROM products WHERE sku IS NOT NULL GROUP BY sku HAVING COUNT(*) > 1
)
ORDER BY p.sku, p.show_flag DESC, p.platform;

COMMIT;
