-- =============================================================================
-- Script 23: Alias subcategories на "Геймърска периферия" под "Компютърна периферия"
--
-- Какво прави:
--   За всяка видима подкатегория на "Геймърска периферия" създава нова категория
--   (alias) под "Компютърна периферия". Alias категорията няма собствени продукти —
--   бекендът прозрачно зарежда продуктите на оригинала чрез alias_of_id.
--
-- Изисквания:
--   - V32 Flyway миграцията трябва да е изпълнена преди този скрипт
--     (добавя alias_of_id колона)
--   - Категориите "Геймърска периферия" и "Компютърна периферия" трябва да съществуват
--
-- Безопасност:
--   - Скриптът е идемпотентен: alias категории, които вече съществуват
--     (открити по alias_of_id + parent_id), се пропускат
--   - Не пипа съществуващи категории или продукти
-- =============================================================================

DO $$
DECLARE
    gaming_id      BIGINT;
    computer_id    BIGINT;
    rec            RECORD;
    alias_slug     VARCHAR(200);
    alias_exists   BOOLEAN;
BEGIN
    -- Намираме ID-то на "Геймърска периферия"
    SELECT id INTO gaming_id
    FROM categories
    WHERE name_bg ILIKE '%Геймърска периферия%'
      AND show_flag = true
    LIMIT 1;

    IF gaming_id IS NULL THEN
        RAISE EXCEPTION 'Категория "Геймърска периферия" не е намерена. Прекратяване.';
    END IF;

    -- Намираме ID-то на "Компютърна периферия"
    SELECT id INTO computer_id
    FROM categories
    WHERE name_bg ILIKE '%Компютърна периферия%'
      AND show_flag = true
    LIMIT 1;

    IF computer_id IS NULL THEN
        RAISE EXCEPTION 'Категория "Компютърна периферия" не е намерена. Прекратяване.';
    END IF;

    RAISE NOTICE 'Геймърска периферия id=%', gaming_id;
    RAISE NOTICE 'Компютърна периферия id=%', computer_id;

    -- Обхождаме всички видими подкатегории на "Геймърска периферия"
    FOR rec IN
        SELECT id, name_bg, name_en, slug, sort_order
        FROM categories
        WHERE parent_id = gaming_id
          AND show_flag = true
        ORDER BY sort_order, id
    LOOP
        -- Уникален slug: префикс "kp-" пред оригиналния slug
        alias_slug := 'kp-' || rec.slug;

        -- Проверяваме дали alias вече съществува
        SELECT EXISTS (
            SELECT 1 FROM categories
            WHERE alias_of_id = rec.id
              AND parent_id = computer_id
        ) INTO alias_exists;

        IF alias_exists THEN
            RAISE NOTICE 'Alias вече съществува за "%", пропускаме.', rec.name_bg;
            CONTINUE;
        END IF;

        -- Вмъкваме alias категорията
        INSERT INTO categories (
            name_bg, name_en, slug,
            show_flag, sort_order,
            parent_id, alias_of_id,
            is_promo_active,
            created_at, updated_at,
            created_by, last_modified_by
        ) VALUES (
            rec.name_bg, rec.name_en, alias_slug,
            true, rec.sort_order,
            computer_id, rec.id,
            false,
            NOW(), NOW(),
            'script-23', 'script-23'
        );

        RAISE NOTICE 'Създаден alias "%" (slug: %) → оригинал id=%', rec.name_bg, alias_slug, rec.id;
    END LOOP;

    RAISE NOTICE 'Готово.';
END $$;

-- Верификация: показва всички alias категории под "Компютърна периферия"
SELECT
    a.id            AS alias_id,
    a.name_bg       AS alias_name,
    a.slug          AS alias_slug,
    o.id            AS original_id,
    o.name_bg       AS original_name,
    p.name_bg       AS original_parent
FROM categories a
JOIN categories o ON o.id = a.alias_of_id
JOIN categories p ON p.id = o.parent_id
WHERE a.parent_id = (
    SELECT id FROM categories WHERE name_bg ILIKE '%Компютърна периферия%' AND show_flag = true LIMIT 1
)
ORDER BY a.sort_order, a.id;
