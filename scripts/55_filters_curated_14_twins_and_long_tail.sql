-- ============================================================================
-- 55_filters_curated_14_twins_and_long_tail.sql
--
-- Курация на каноничния филтърен слой, партида 14.
--
--   1. CCTV двойници — отделни клонове в „Видеонаблюдение“, не дубликати. Получават курацията на
--      двойника си (източници, правила по име, групи):
--        964 NDAA › IP камери  ← 100 IP системи › IP камери
--        249 Видеонаблюдение › Монитори ← 50 Монитори и дисплеи › Монитори
--        240 NDAA › NVR        ← 962 IP системи › NVR
--   2. Дълга опашка с полза:
--        136 Антенни кабели    — „Конектор 1/2“ стават една група „Конектори“ (нови стойности F, RF)
--        77  Инвертори         — „Power“ става „Мощност“ (число във ватове)
--        83  Скенери           — 6 групи вместо 12; „Двустранно сканиране“ Да/Не вместо Automatic/Manual
--        246 Стойки за камери  — „Вид монтаж“ от името (кутия, стена, стълб, окачване)
--        99  Мрежово оборудване (модули, адаптери, планки) — „Вид“ от името
--        122 Мултимедийни плейъри — 4 групи; резолюцията през курираното свойство на телевизорите
--        178 Компютърни и геймърски очила — 5 групи вместо 9
--
-- ИЗИСКВА: V38–V41, скриптове 55_01…55_13 и поне един успешен rebuild (V42 не е нужна).
-- СЛЕД СКРИПТА: Админ → „Филтри“ → Rebuild.
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си редове (note = '55 партида 14') и MANUAL групите на своите
--   десет категории, после ги вмъква наново. Повторно пускане трие и админ промените по групите на
--   тези категории — след първото пускане собственик е админ панелът.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((964, 'IP камери'), (249, 'Монитори'), (240, 'NVR'), (136, 'Антенни кабели'),
                                (77, 'Инвертори'), (83, 'Скенери'), (246, 'Стойки и основи за камери'),
                                (99, 'Рутери и мрежово оборудване'), (122, 'Мултимедийни плейъри'),
                                (178, 'Компютърни и геймърски очила'))) <> 10 THEN
        RAISE EXCEPTION 'Някоя от категориите на партида 14 не е тази, която очаквам — спирам.';
    END IF;
    IF (SELECT count(*) FROM category_filter_settings WHERE category_id IN (100, 50, 962) AND mode = 'MANUAL') <> 3 THEN
        RAISE EXCEPTION 'Двойниците 100, 50 и 962 трябва да са курирани — първо пусни 55_01, 04 и 10.';
    END IF;
    IF (SELECT count(*) FROM filter_attributes
        WHERE origin = 'MANUAL' AND slug IN ('cable-connector', 'psu-power', 'paper-size', 'tv-resolution', 'colour',
                                             'screen-resolution')) <> 6 THEN
        RAISE EXCEPTION 'Липсва курирано свойство — първо пусни 55_01…55_12.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 14';
DELETE FROM filter_value_rules WHERE note = '55 партида 14';
DELETE FROM filter_name_rules WHERE note = '55 партида 14';
DELETE FROM category_filters WHERE category_id IN (964, 249, 240, 136, 77, 83, 246, 99, 122, 178) AND origin = 'MANUAL';

-- ── 1. CCTV двойници: курацията на двойника ──────────────────────────────────
-- Източниците и правилата по име са вързани с категория, затова се копират, не само групите.
INSERT INTO filter_attribute_sources (action, attribute_id, parameter_id, name_norm, platform, category_id, origin, note)
SELECT s.action, s.attribute_id, s.parameter_id, s.name_norm, s.platform, t.target, 'MANUAL', '55 партида 14'
FROM filter_attribute_sources s
JOIN (VALUES (100::BIGINT, 964::BIGINT), (50, 249), (962, 240)) AS t(source, target) ON s.category_id = t.source
ON CONFLICT DO NOTHING;

INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, use_parser, note)
SELECT r.attribute_id, t.target, r.pattern, r.value_id, r.use_parser, '55 партида 14'
FROM filter_name_rules r
JOIN (VALUES (100::BIGINT, 964::BIGINT), (50, 249), (962, 240)) AS t(source, target) ON r.category_id = t.source
ON CONFLICT DO NOTHING;

-- Мониторите на TEKRA носят резолюцията като „4K“ / „FULL HD 1080p“ в „Резолюция“.
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm('Резолюция'), 249, 'MANUAL', '55 партида 14'
FROM filter_attributes a WHERE a.slug = 'screen-resolution' AND a.origin = 'MANUAL'
ON CONFLICT DO NOTHING;

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT t.target, cf.attribute_id, cf.sort_order, TRUE, 'MANUAL'
FROM category_filters cf
JOIN (VALUES (100::BIGINT, 964::BIGINT), (50, 249), (962, 240)) AS t(source, target) ON cf.category_id = t.source
WHERE cf.visible AND cf.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

-- „Яркост“ и „Приложение“ на Монитори са автоматични свойства — копират се отделно.
INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT 249, cf.attribute_id, cf.sort_order, TRUE, 'MANUAL'
FROM category_filters cf JOIN filter_attributes a ON a.id = cf.attribute_id AND a.origin = 'AUTO'
WHERE cf.category_id = 50 AND cf.visible
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

-- ── 2. Нови свойства и стойности ─────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('scan-duplex',            'Двустранно сканиране', 'Duplex scanning', 'ENUM', NULL, NULL, FALSE, 40, 'MANUAL'),
    ('camera-mount-type',      'Вид монтаж',           'Mount type',      'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('network-accessory-type', 'Вид',                  'Type',            'ENUM', NULL, NULL, FALSE, 10, 'MANUAL')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.value_bg, v.value_en, filter_norm(v.value_bg), v.ord, 'MANUAL'
FROM (VALUES
    ('cable-connector',        'F-конектор',              'F connector',         33),
    ('cable-connector',        'RF (IEC)',                'RF (IEC)',            34),
    ('scan-duplex',            'Да',                      'Yes',                 1),
    ('scan-duplex',            'Не',                      'No',                  2),
    ('camera-mount-type',      'Монтажна кутия',          'Junction box',        1),
    ('camera-mount-type',      'Стойка за стена',         'Wall bracket',        2),
    ('camera-mount-type',      'За стълб',                'Pole mount',          3),
    ('camera-mount-type',      'Окачване / таван',        'Pendant / ceiling',   4),
    ('camera-mount-type',      'Адаптер / аксесоар',      'Adapter / accessory', 5),
    ('network-accessory-type', 'SFP / модул',             'SFP / module',        1),
    ('network-accessory-type', 'Оптичен терминал (ONU)',  'Optical terminal',    2),
    ('network-accessory-type', 'Захранване / адаптер',    'Power adapter',       3),
    ('network-accessory-type', 'Монтаж и аксесоари',      'Mounting',            4)
) AS v(slug, value_bg, value_en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO NOTHING;

-- ── 3. Правила за стойности ──────────────────────────────────────────────────
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 14'
FROM (VALUES
    ('cable-connector', 'F-конектор', '^f (женско|мъжко|female|male)$'),
    ('cable-connector', 'RF (IEC)',   '^(rf|iec) (женско|мъжко|female|male)$'),
    ('scan-duplex', 'Да', '^(automatic|автоматично|да|yes)$'),
    ('scan-duplex', 'Не', '^(manual|ръчно|не|no)$')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 4. Източници ─────────────────────────────────────────────────────────────
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 14'
FROM (VALUES
    ('cable-connector', 'Конектор 1',                                    136::BIGINT),
    ('cable-connector', 'Конектор 2',                                    136),
    ('psu-power',       'Power',                                         77),
    ('scan-duplex',     'Автоматично двустранно сканиране (DADF/RADF)',  83),
    ('paper-size',      'Основен формат',                                83),
    ('tv-resolution',   'Резолюция',                                     122),
    ('tv-resolution',   'Функции',                                       122)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL'
ON CONFLICT DO NOTHING;

-- ── 5. Стойности от името на продукта ────────────────────────────────────────
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 14'
FROM (VALUES
    ('camera-mount-type', 246::BIGINT, 'Монтажна кутия',     '(кутия|junction ?box|(^|[^[:alnum:]])box([^[:alnum:]]|$))'),
    ('camera-mount-type', 246, 'За стълб',           '(стълб|(^|[^[:alnum:]])pole)'),
    ('camera-mount-type', 246, 'Окачване / таван',   '(окачен|таван|pendant|ceiling)'),
    ('camera-mount-type', 246, 'Стойка за стена',    '^(?!.*(кутия|box|стълб|pole|окачен|таван|pendant|ceiling)).*(стойка|wall mount|скоба|bracket|основа)'),
    ('camera-mount-type', 246, 'Адаптер / аксесоар', '(адаптер|adapter|joint)'),
    ('network-accessory-type', 99, 'SFP / модул',            '(sfp|gbic|transceiver|трансивър|модул|module|1000sx|1000tx|100fx|bidi)'),
    ('network-accessory-type', 99, 'Оптичен терминал (ONU)', '(epon|gpon|(^|[^[:alnum:]])onu([^[:alnum:]]|$))'),
    ('network-accessory-type', 99, 'Захранване / адаптер',   '(power adapter|захранващ адаптер|(^|[^[:alnum:]])adapter-ac)'),
    ('network-accessory-type', 99, 'Монтаж и аксесоари',     '(планк|rack|holder|tray|стойк)')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Мониторите на TEKRA нямат параметър за диагонал, но всяко име го носи („Монитор 27\" FHD“).
INSERT INTO filter_name_rules (attribute_id, category_id, use_parser, note)
SELECT a.id, 249, TRUE, '55 партида 14'
FROM filter_attributes a WHERE a.slug = 'screen-diagonal' AND a.origin = 'MANUAL'
ON CONFLICT DO NOTHING;

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (964, 'MANUAL'), (249, 'MANUAL'), (240, 'MANUAL'), (136, 'MANUAL'), (77, 'MANUAL'), (83, 'MANUAL'),
       (246, 'MANUAL'), (99, 'MANUAL'), (122, 'MANUAL'), (178, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

-- Курирани свойства.
INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (136::BIGINT, 'cable-connector', 20),
    (77, 'psu-power', 10),
    (83, 'paper-size', 20), (83, 'scan-duplex', 40), (83, 'colour', 60),
    (246, 'camera-mount-type', 10), (246, 'colour', 20),
    (99, 'network-accessory-type', 10),
    (122, 'tv-resolution', 20), (122, 'colour', 40)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

-- Автоматични свойства (по името на параметъра), които вече вършат работа.
INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (136::BIGINT, 'Тип продукт', 10), (136, 'Екраниран', 30),
    (77, 'Входно напрежение', 20), (77, 'Форма на изходното напрежение', 30),
    (83, 'Вид', 10), (83, 'Автоматично подаващо устройство (ADF)', 30), (83, 'Захранване', 50),
    (122, 'Тип продукт', 10), (122, 'Операционна система', 30),
    (178, 'Тип аксесоар', 10), (178, 'Защита', 20), (178, 'За', 30), (178, 'Материал рамка', 40), (178, 'Стил рамка', 50)
) AS f(category_id, name, ord)
JOIN filter_attributes a ON a.auto_key = filter_norm(f.name)
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

-- Невалиден регулярен израз спира целия rebuild (и нощния) — всеки израз се компилира тук, преди COMMIT.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT pattern FROM filter_value_rules WHERE note = '55 партида 14' AND pattern IS NOT NULL
             UNION ALL
             SELECT pattern FROM filter_name_rules WHERE note = '55 партида 14' AND pattern IS NOT NULL
    LOOP
        PERFORM '' ~* r.pattern;
    END LOOP;
END $$;

DO $$
DECLARE twins INT; tail INT;
BEGIN
    SELECT count(*) INTO twins FROM category_filters WHERE category_id IN (964, 249, 240) AND origin = 'MANUAL';
    SELECT count(*) INTO tail FROM category_filters
    WHERE category_id IN (136, 77, 83, 246, 99, 122, 178) AND origin = 'MANUAL';
    IF tail <> 24 THEN
        RAISE EXCEPTION 'Очаквах 24 групи в дългата опашка, вмъкнати са % — липсва свойство.', tail;
    END IF;
    IF twins < 20 THEN
        RAISE EXCEPTION 'Двойниците получиха само % групи — провери 100, 50 и 962.', twins;
    END IF;
    RAISE NOTICE 'Партида 14: % групи при CCTV двойниците, % в дългата опашка.', twins, tail;
END $$;

COMMIT;

-- Контрола (след COMMIT, само чете): групите по категория.
SELECT cf.category_id, count(*) AS groups, string_agg(a.name_bg, ', ' ORDER BY cf.sort_order) AS names
FROM category_filters cf JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (964, 249, 240, 136, 77, 83, 246, 99, 122, 178) AND cf.origin = 'MANUAL'
GROUP BY cf.category_id ORDER BY cf.category_id;
