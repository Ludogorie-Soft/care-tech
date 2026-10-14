-- ============================================================================
-- 51_migrate_products_from_hidden_categories.sql
--
-- Стъпка 2б: изважда продукти от скритите ASBIS категории във видимото дърво.
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
-- ЗАЩО ЕДНОКРАТЕН СКРИПТ СТИГА (за разлика от MOST мапинга):
--   ASBIS продуктовият sync зарежда `findByShowTrue()` — само видими категории.
--   Значи нов ASBIS продукт вече НЕ МОЖЕ да попадне в скрита категория. Тези
--   продукти са историческа утайка от времето, когато категориите са били видими.
--   Sync-ът ги обновява всеки ден (1397 днес), но запазва категорията им, защото
--   мапингът от feed-а се проваля (AsbisSyncService:601-613). Същата логика ще
--   запази и НОВАТА категория след преместването.
--
-- ДЪРВОТО НЕ СЕ ПИПА: не се създават, трият или местят категории. Само продукти.
--
-- ЦЕЛИТЕ СА ПО id, НЕ ПО ИМЕ — „Монитори" например съществува два пъти видимо
--   (id 50 компютърни, id 249 за видеонаблюдение), а „Слушалки" и „Мрежови кабели"
--   имат по три копия. Търсене по име тук би било лотария.
--
-- ОБРАТИМОСТ: оригиналната категория се пази в `category_id_pre_phase5`
--   (колоната от скрипт 48). Откат в края на файла.
--
-- Дата: 2026-09-18
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';
SET statement_timeout = '300s';


-- ─── СЕКЦИЯ A: отчет ПРЕДИ ──────────────────────────────────────────────────

SELECT c.id, left(c.name_bg, 26) AS skrita_kategoriya,
       count(*) AS produkti,
       count(*) FILTER (WHERE p.active AND p.show_flag AND p.status = 'AVAILABLE'
                          AND p.deleted = false AND p.image_url <> '') AS vidimi
FROM products p JOIN categories c ON c.id = p.category_id
WHERE p.category_id IN (443, 406, 348, 333, 436, 517, 489, 362, 398, 513, 366, 584, 415, 678, 392, 325, 500)
GROUP BY 1, 2 ORDER BY 4 DESC;


BEGIN;

-- ─── СЕКЦИЯ B: категории с еднозначна цел ───────────────────────────────────
-- Съдържанието им е проверено с извадки; всяка отива цяла в една категория.

UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 62,  updated_at = NOW() WHERE category_id = 406;  -- Периферия → Мишки
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 192, updated_at = NOW() WHERE category_id = 348;  -- Софтуеър → Софтуер
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 58,  updated_at = NOW() WHERE category_id = 333;  -- Мрежови продукти → USB хъбове
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 131, updated_at = NOW() WHERE category_id = 436;  -- Видео адаптери → Видео кабели
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 321, updated_at = NOW() WHERE category_id = 517;  -- Флаш памети → Флаш памети
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 66,  updated_at = NOW() WHERE category_id = 489;  -- Аидио устройства → Слушалки
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 50,  updated_at = NOW() WHERE category_id = 362;  -- Дисплеи → Монитори
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 966, updated_at = NOW() WHERE category_id = 398;  -- Адаптери → Адаптери
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 56,  updated_at = NOW() WHERE category_id = 513;  -- SSD → Външни SSD
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 15,  updated_at = NOW() WHERE category_id = 366;  -- Твърди дискове → Хард дискове 2.5"
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 211, updated_at = NOW() WHERE category_id = 584;  -- Tools → Инструменти и аксесоари
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 6,   updated_at = NOW() WHERE category_id = 415;  -- Памет → Памети
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 154, updated_at = NOW() WHERE category_id = 678;  -- Смартфони → Мобилни телефони
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 18,  updated_at = NOW() WHERE category_id = 392;  -- Компютърни кутии → Чекмеджета за дискове


-- ─── СЕКЦИЯ C: 443 „Игри и мултимедия" — разделя се по вид ──────────────────
-- Цялото съдържание е геймърска стока и съвпада точно с подкатегориите на
-- „Геймърска периферия": 18 мишки, 17 клавиатури, 10 слушалки, 7 подложки.
-- РЕДЪТ Е ВАЖЕН: подложките първи, защото „mouse pad" съдържа и „mouse".

UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 173, updated_at = NOW()
 WHERE category_id = 443 AND coalesce(name_bg, name_en) ~* '(mouse ?pad|padded|gigantus|goliathus)';

UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 174, updated_at = NOW()
 WHERE category_id = 443 AND coalesce(name_bg, name_en) ~* 'mouse';

UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 172, updated_at = NOW()
 WHERE category_id = 443 AND coalesce(name_bg, name_en) ~* 'keyboard';

UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 171, updated_at = NOW()
 WHERE category_id = 443 AND coalesce(name_bg, name_en) ~* 'headset|headphone';

-- Остатъкът е също геймърски — стойки, RGB ленти, стрийминг микрофони
UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 179, updated_at = NOW()
 WHERE category_id = 443;


-- ─── СЕКЦИЯ D: 325 „Мултимедиен хардуер" — само каквото е ясно ──────────────
-- Смесена: ~46 тонколони, слушалки, микрофони, но и таванни стойки за проектори
-- и стрийминг устройства. Местят се САМО двете еднозначни групи; останалото
-- остава в 325 за отделен преглед. Съзнателно консервативно.
-- Бележка: „eaphon" не е печатна грешка тук — така са изписани REALME артикулите.

UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 66, updated_at = NOW()
 WHERE category_id = 325 AND coalesce(name_bg, name_en) ~* 'headphone|headset|earbud|earphone|eaphon|слушалк';

UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 59, updated_at = NOW()
 WHERE category_id = 325 AND coalesce(name_bg, name_en) ~* 'speaker|тонколон|audio system|soundbar|sound bar';


-- ─── СЕКЦИЯ E: 500 „Смарт устройства" — само детските часовници ─────────────
-- Останалото е Aqara смарт дом и остава за отделно решение.

UPDATE products SET category_id_pre_phase5 = coalesce(category_id_pre_phase5, category_id),
                    category_id = 156, updated_at = NOW()
 WHERE category_id = 500 AND coalesce(name_bg, name_en) ~* 'watch|часовник';


-- ─── СЕКЦИЯ F: предпазител ──────────────────────────────────────────────────

DO $$
DECLARE
    v_ostanali_443 int;
    v_v_skriti     int;
    v_premesteni   int;
    v_v_nevidimi   int;
BEGIN
    -- 443 трябва да е изпразнена докрай — последното правило няма условие по име
    SELECT count(*) INTO v_ostanali_443 FROM products WHERE category_id = 443;
    IF v_ostanali_443 > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % продукта още са в 443', v_ostanali_443;
    END IF;

    -- Нито един продукт да не е попаднал в СКРИТА категория
    SELECT count(*) INTO v_v_skriti
      FROM products p JOIN categories c ON c.id = p.category_id
     WHERE p.category_id_pre_phase5 IS NOT NULL AND NOT c.show_flag
       AND p.category_id NOT IN (325, 500);   -- тези две умишлено остават частично пълни
    IF v_v_skriti > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % преместени продукта са в скрита категория', v_v_skriti;
    END IF;

    SELECT count(*) INTO v_premesteni FROM products WHERE category_id_pre_phase5 IS NOT NULL;
    SELECT count(*) INTO v_v_nevidimi
      FROM products p JOIN categories c ON c.id = p.category_id
     WHERE NOT c.show_flag AND p.active AND p.show_flag AND p.status = 'AVAILABLE'
       AND p.deleted = false AND p.image_url <> '';

    RAISE NOTICE '✓ Проверките минаха. Общо преместени продукта (вкл. от предишни скриптове): %. Видими продукти в невидими категории: %.',
                 v_premesteni, v_v_nevidimi;
END $$;

COMMIT;


-- ─── СЕКЦИЯ G: отчет СЛЕД ───────────────────────────────────────────────────

SELECT c.id, left(c.name_bg, 28) AS kategoriya, c.show_flag AS vidima,
       (SELECT count(*) FROM products p WHERE p.category_id = c.id) AS produkti
FROM categories c
WHERE c.id IN (62, 192, 58, 131, 321, 66, 50, 966, 56, 15, 211, 6, 154, 18,
               173, 174, 172, 171, 179, 59, 156, 325, 500, 443)
ORDER BY c.show_flag DESC, c.name_bg;

SELECT count(*) AS vidimi_produkti_v_nevidimi_kategorii
FROM products p JOIN categories c ON c.id = p.category_id
WHERE NOT c.show_flag AND p.active AND p.show_flag AND p.status = 'AVAILABLE'
  AND p.deleted = false AND p.image_url <> '';


-- ============================================================================
-- ОТКАТ (връща и скрипт 48/50, ако е нужно — виж коя стойност е записана)
--   BEGIN;
--   UPDATE products
--   SET category_id = category_id_pre_phase5, category_id_pre_phase5 = NULL
--   WHERE category_id_pre_phase5 IS NOT NULL;
--   COMMIT;
-- ============================================================================
