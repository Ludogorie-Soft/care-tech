-- ============================================================================
-- 55_filters_curated_01_monitors_colour.sql
--
-- Курация на каноничния филтърен слой (V38), партида 1:
--   • общ „Цвят“ за целия магазин — всички доставчици, базови цветове;
--   • филтрите на категория 50 „Монитори“.
--
-- КАКВО ПРАВИ
--   Пипа САМО таблиците на каноничния слой (filter_*, category_filter*).
--   Не пипа parameters / parameter_options / product_parameters — те са на
--   sync-а и всяка ръчна промяна там се губи до сутринта.
--
-- КОГА СЕ ПУСКА
--   1) след деплой на V38/V39 и на кода с FilterIndexService;
--   2) след ПЪРВИЯ rebuild (POST /api/admin/filters/rebuild или нощния cron),
--      защото ползва автоматичните свойства „Яркост“ и „Приложение“;
--   3) след скрипта: POST /api/admin/filters/rebuild — едва тогава промяната
--      стига до клиентите (иначе в 01:00 при нощния прогон).
--
-- ИДЕМПОТЕНТНОСТ
--   Скриптът е авторитетен за своя обхват: трие собствените си правила
--   (note = '55 партида 1') и настройките на категория 50, после ги вмъква
--   наново. Може да се пуска многократно; поправка = редакция тук + ново пускане.
--
-- ПРЕГЛЕД ПРЕДИ ПРОД
--   POST /api/admin/filters/rebuild?dryRun=true показва ефекта без да го запише.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF to_regclass('public.filter_attributes') IS NULL THEN
        RAISE EXCEPTION 'Каноничният слой (V38) липсва — първо деплой.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM filter_rebuild_runs WHERE status = 'SUCCESS' AND NOT dry_run) THEN
        RAISE EXCEPTION 'Няма успешен rebuild — пусни POST /api/admin/filters/rebuild преди скрипта.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM categories WHERE id = 50 AND name_bg = 'Монитори') THEN
        RAISE EXCEPTION 'Категория 50 не е „Монитори“ — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане на тази партида ──────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 1';
DELETE FROM filter_value_rules WHERE note = '55 партида 1';
DELETE FROM category_filters WHERE category_id = 50 AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
-- Slug-ът е идентичността на ръчното свойство. Ако вече е зает от AUTO свойство,
-- проверката по-долу спира скрипта, вместо тихо да преизползва чуждия ред.
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, split_pattern, auto_values, sort_order, origin)
VALUES
    ('colour',             'Цвят',                      'Colour',        'ENUM',    NULL,  NULL,            '\s*[/;,+&|]\s*|\s+(и|and|with|със|с)\s+', FALSE, 50,  'MANUAL'),
    ('screen-diagonal',    'Диагонал',                  'Diagonal',      'NUMERIC', '"',   'DIAGONAL_INCH', NULL, FALSE, 10, 'MANUAL'),
    ('screen-resolution',  'Резолюция',                 'Resolution',    'NUMERIC', NULL,  'RESOLUTION',    NULL, FALSE, 20, 'MANUAL'),
    ('refresh-rate',       'Честота на опресняване',    'Refresh rate',  'NUMERIC', 'Hz',  'REFRESH_HZ',    NULL, FALSE, 30, 'MANUAL'),
    ('panel-type',         'Тип матрица',               'Panel type',    'ENUM',    NULL,  NULL,            NULL, FALSE, 40, 'MANUAL'),
    ('response-time',      'Време за реакция',          'Response time', 'NUMERIC', 'ms',  'RESPONSE_MS',   NULL, FALSE, 50, 'MANUAL'),
    ('aspect-ratio',       'Съотношение',               'Aspect ratio',  'ENUM',    NULL,  NULL,            NULL, FALSE, 60, 'MANUAL'),
    ('built-in-speakers',  'Вградени говорители',       'Speakers',      'ENUM',    NULL,  NULL,            NULL, FALSE, 80, 'MANUAL'),
    ('height-adjustment',  'Регулиране на височината',  'Height adjust', 'ENUM',    NULL,  NULL,            NULL, FALSE, 90, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, split_pattern = EXCLUDED.split_pattern,
        auto_values = EXCLUDED.auto_values, sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('colour', 'screen-diagonal', 'screen-resolution', 'refresh-rate', 'panel-type',
                   'response-time', 'aspect-ratio', 'built-in-speakers', 'height-adjustment')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Кои сурови параметри хранят кое свойство ──────────────────────────────
-- Без категория = важи в целия магазин (името означава едно и също навсякъде).
-- С категория 50 = думата е двусмислена другаде („Резолюция“ при камерите е в MP).
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 1'
FROM (VALUES
    ('colour',            'Цвят',                                    NULL::BIGINT),
    ('colour',            'Външен цвят',                             NULL),
    ('colour',            'Цвят на корпуса',                         NULL),
    ('colour',            'Обикновен цвят',                          NULL),
    ('screen-diagonal',   'Размер на дисплея',                       NULL),
    ('screen-diagonal',   'Размер на екрана',                        NULL),
    ('screen-diagonal',   'Дължина на диагонала на екрана',          NULL),
    ('screen-diagonal',   'Диагонал',                                NULL),
    ('refresh-rate',      'Честота на опресняване',                  NULL),
    ('refresh-rate',      'Максимална скорост на видео-опресняване', NULL),
    ('response-time',     'Време за реакция',                        NULL),
    ('panel-type',        'Тип на матрицата',                        NULL),
    ('panel-type',        'Вид матрица',                             NULL),
    ('aspect-ratio',      'Формат на картината',                     NULL),
    ('aspect-ratio',      'Изображение - съотношение на размерите',  NULL),
    ('built-in-speakers', 'Вградени говорители',                     NULL),
    ('height-adjustment', 'Регулиране височина',                     NULL),
    ('screen-resolution', 'Резолюция',                               50),
    ('screen-resolution', 'Резолюция на екрана',                     50),
    ('screen-resolution', 'Максимална резолюция',                    50),
    ('panel-type',        'Технология',                              50),
    ('built-in-speakers', 'Формат на говорителите',                  50),
    ('height-adjustment', 'Ергономични функции',                     50),
    ('height-adjustment', 'Ергономични характеристики',              50)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности ─────────────────────────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('colour', 'Черен', 'Black', 1),      ('colour', 'Бял', 'White', 2),
    ('colour', 'Сив', 'Grey', 3),          ('colour', 'Сребрист', 'Silver', 4),
    ('colour', 'Син', 'Blue', 5),          ('colour', 'Червен', 'Red', 6),
    ('colour', 'Зелен', 'Green', 7),       ('colour', 'Жълт', 'Yellow', 8),
    ('colour', 'Оранжев', 'Orange', 9),    ('colour', 'Розов', 'Pink', 10),
    ('colour', 'Лилав', 'Purple', 11),     ('colour', 'Кафяв', 'Brown', 12),
    ('colour', 'Бежов', 'Beige', 13),      ('colour', 'Златист', 'Gold', 14),
    ('colour', 'Прозрачен', 'Transparent', 15), ('colour', 'Многоцветен', 'Multicolour', 16),
    ('colour', 'Магента', 'Magenta', 17),  ('colour', 'Циан', 'Cyan', 18),
    ('panel-type', 'IPS', 'IPS', 1),       ('panel-type', 'VA', 'VA', 2),
    ('panel-type', 'TN', 'TN', 3),         ('panel-type', 'OLED', 'OLED', 4),
    ('aspect-ratio', '16:9', '16:9', 1),   ('aspect-ratio', '16:10', '16:10', 2),
    ('aspect-ratio', '21:9', '21:9', 3),   ('aspect-ratio', '32:9', '32:9', 4),
    ('built-in-speakers', 'Да', 'Yes', 1), ('built-in-speakers', 'Не', 'No', 2),
    ('height-adjustment', 'Да', 'Yes', 1), ('height-adjustment', 'Не', 'No', 2)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Правила сурова стойност → стойност ────────────────────────────────────
-- REGEX правилата се прилагат върху filter_norm(текста) — малки букви. Всички
-- съвпадения важат: „Black with blue“ е и Черен, и Син. Граница на дума =
-- не-буква ([[:alpha:]] разпознава кирилица при en_US.utf8 — прод и локално).
-- Доставчиците смесват кирилица и латиница в една дума („чеpен“ с латинско p,
-- „cив“ с латинско c) — затова [рp], [сc], [еe], [мm].
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, '(^|[^[:alpha:]])' || r.words || '([^[:alpha:]]|$)', v.id, '55 партида 1'
FROM (VALUES
    ('colour', 'Черен',       '(ч[еe][рp][еe]н|ч[еe][рp]н[аои]|black|onyx|obsidian|midnight|jet|ebony)'),
    ('colour', 'Бял',         '(бял|бял[аоие]|бели|white|ivory)'),
    ('colour', 'Сив',         '([сc]ив|[сc]ив[аои]|светло[сc]ив[аои]?|антрацит|gr[ae]y|graphite|anthracite|charcoal|gunmetal|titan|titanium)'),
    ('colour', 'Сребрист',    '(сребрист|сребрист[аои]|сребърн[аои]|сребърен|silver|инокс|inox|platinum|metallic|metal|chrome|alumin(i)?um|stainless|steel)'),
    ('colour', 'Син',         '([сc]ин|[сc]ин[яьи]|[сc]иньо|пастелно[сc]ин|blue|navy|тюркоаз|turquoise|teal|aqua|cobalt)'),
    ('colour', 'Червен',      '(червен|червен[аои]|бордо|red|burgundy|crimson|scarlet)'),
    ('colour', 'Зелен',       '(зелен|зелен[аои]|green|lime|mint|olive|emerald)'),
    ('colour', 'Жълт',        '(жълт|жълт[аои]|y[еe]llow)'),
    ('colour', 'Оранжев',     '(оранжев|оранжев[аои]|orange)'),
    ('colour', 'Розов',       '(розов|розов[аои]|pink|rose)'),
    ('colour', 'Лилав',       '(лилав|лилав[аои]|виолетов[аои]?|purple|violet|lavender|lilac)'),
    ('colour', 'Кафяв',       '(кафяв|кафяв[аои]|brown|wood|walnut|oak|maple|chocolate|coffee|taupe)'),
    ('colour', 'Бежов',       '(бежов|бежов[аои]|натурал(ен|н[аои])|beige|sand|khaki|cream)'),
    ('colour', 'Златист',     '(златист|златист[аои]|златен|златн[аои]|gold|golden|brass|bronze)'),
    ('colour', 'Прозрачен',   '(прозрачен|прозрачн[аои]|transparent|clear|translucent)'),
    ('colour', 'Многоцветен', '([мm]ногоцветен|[мm]ногоцветн[аои]|цветн[аои]|rgb|multicolou?r|multi-colou?r|tri-colou?r|rainbow)'),
    ('colour', 'Магента',     '(магента|magenta|пурпурен|пурпурн[аои])'),
    ('colour', 'Циан',        '(циан|cyan)'),
    ('panel-type', 'IPS',     '(ips|ads|pls|ahva)'),
    ('panel-type', 'VA',      '(va|mva|pva)'),
    ('panel-type', 'TN',      '(tn)'),
    ('aspect-ratio', '16:9',  '(16 ?: ?9)'),
    ('aspect-ratio', '16:10', '(16 ?: ?10)'),
    ('aspect-ratio', '21:9',  '(21 ?: ?9)'),
    ('aspect-ratio', '32:9',  '(32 ?: ?9)'),
    ('built-in-speakers', 'Не', '(не|no|няма)'),
    ('built-in-speakers', 'Да', '(да|yes|вграден|вградени|[0-9]+(\.[0-9]+)? ?w)'),
    ('height-adjustment', 'Не', '(не|no)'),
    ('height-adjustment', 'Да', '(да|yes|[0-9]+ ?(mm|мм)|височина|височината|height)')
) AS r(slug, value, words)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- OLED без граница на дума: „WOLED“, „QD-OLED“.
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, 'oled', v.id, '55 партида 1'
FROM filter_attributes a JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = 'oled'
WHERE a.slug = 'panel-type' AND a.origin = 'MANUAL';

-- ASBIS „Ергономични функции“ = само наклон → няма регулиране на височината.
INSERT INTO filter_value_rules (attribute_id, raw_norm, value_id, note)
SELECT a.id, filter_norm(r.raw), v.id, '55 партида 1'
FROM (VALUES ('Наклон'), ('Tilt')) AS r(raw)
JOIN filter_attributes a ON a.slug = 'height-adjustment' AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = 'не';

-- ── 5. Филтрите на „Монитори“ ────────────────────────────────────────────────
-- MANUAL: rebuild-ът не добавя AUTO групи тук. „Яркост“ и „Приложение“ са
-- AUTO свойства (от имената на параметрите) — ползват се такива, каквито са.
INSERT INTO category_filter_settings (category_id, mode) VALUES (50, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT 50, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    ('slug', 'screen-diagonal',   10), ('slug', 'screen-resolution', 20), ('slug', 'refresh-rate', 30),
    ('slug', 'panel-type',        40), ('slug', 'response-time',     50),
    ('auto', 'яркост',            70), ('slug', 'built-in-speakers', 80), ('slug', 'height-adjustment', 90),
    ('slug', 'colour',           100), ('auto', 'приложение',       110)
) AS f(kind, ref, ord)
JOIN filter_attributes a ON (f.kind = 'slug' AND a.slug = f.ref AND a.origin = 'MANUAL')
                         OR (f.kind = 'auto' AND a.auto_key = f.ref)
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters WHERE category_id = 50 AND origin = 'MANUAL';
    IF n <> 10 THEN
        RAISE EXCEPTION 'Очаквах 10 групи за „Монитори“, получих % — липсва AUTO „Яркост“ или „Приложение“?', n;
    END IF;
    RAISE NOTICE 'Партида 1 записана: % групи за „Монитори“. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT cf.sort_order, a.name_bg, a.value_type, a.origin
FROM category_filters cf JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id = 50 ORDER BY cf.sort_order;
