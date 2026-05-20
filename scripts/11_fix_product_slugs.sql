-- =============================================================================
-- Скрипт 11: Поправка на продукти без slug
-- Продукти с empty/null slug показват числов ID в URL-а вместо slug.
-- Тук генерираме slug от name_en или name_bg директно в PostgreSQL.
--
-- Забележка: Java transliterate() прави Кирилица→Латиница и lowercase.
-- Тук правим опростена версия (само за продукти с латинско name_en).
-- За Кирилски имена — след restart на приложението SlugRegenerationRunner
-- ще довърши работата чрез Java transliterate().
--
-- Изпълни по всяко време — WHERE condition е безопасен.
-- =============================================================================

BEGIN;

-- Диагностика: колко продукти нямат slug
SELECT
    platform,
    COUNT(*) AS no_slug_count
FROM products
WHERE (slug IS NULL OR slug = '')
  AND (name_en IS NOT NULL AND name_en != '' OR name_bg IS NOT NULL AND name_bg != '')
GROUP BY platform
ORDER BY no_slug_count DESC;

-- =============================================================================
-- Генерирай slug от name_en (само Latin/ASCII имена — без транслитерация)
-- =============================================================================
UPDATE products
SET slug       = lower(
                     regexp_replace(
                         regexp_replace(
                             regexp_replace(
                                 trim(coalesce(name_en, name_bg, '')),
                                 '[^\w\s\-]', '', 'g'     -- премахни не-word символи
                             ),
                             '\s+', '-', 'g'              -- whitespace → тире
                         ),
                         '-{2,}', '-', 'g'                -- множество тирета → едно
                     )
                 ),
    updated_at = NOW()
WHERE (slug IS NULL OR slug = '')
  AND (
      (name_en IS NOT NULL AND name_en != '' AND name_en NOT SIMILAR TO '%[а-яёА-ЯЁ]%')
      OR
      (name_en IS NULL OR name_en = '')
      AND (name_bg IS NOT NULL AND name_bg != '' AND name_bg NOT SIMILAR TO '%[а-яёА-ЯЁ]%')
  );

-- =============================================================================
-- Продукти с Кирилски имена без slug → задай временен slug от id
-- (SlugRegenerationRunner ще ги подмине защото slug ≠ empty след това)
-- КОМЕНТИРАЙ ако предпочиташ да изчакаш Java да транслитерира правилно.
-- =============================================================================
UPDATE products
SET slug       = 'product-' || id::text,
    updated_at = NOW()
WHERE (slug IS NULL OR slug = '')
  AND (name_en IS NOT NULL AND name_en != '' OR name_bg IS NOT NULL AND name_bg != '');

-- =============================================================================
-- Продукти без никакво има → оставяме за SlugRegenerationRunner
-- =============================================================================

-- Проверка на резултата
SELECT
    platform,
    COUNT(*) FILTER (WHERE slug IS NULL OR slug = '') AS still_empty,
    COUNT(*) FILTER (WHERE slug IS NOT NULL AND slug != '') AS has_slug
FROM products
GROUP BY platform
ORDER BY platform;

COMMIT;
