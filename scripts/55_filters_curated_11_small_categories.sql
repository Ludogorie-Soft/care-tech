-- ============================================================================
-- 55_filters_curated_11_small_categories.sql
--
-- Курация на каноничния филтърен слой, партида 11 — по-малки категории:
--   104 Безжични адаптери       156 Смарт часовници        43 Охлаждащи поставки за лаптопи
--   42  Зарядни за лаптопи      64  Падове за мишки        132 Захранващи кабели
--   192 Софтуер                 147 Стативи /Триподи/
--
-- ИЗИСКВА: V38–V40 и скриптове 55_..._01 („Цвят“), 55_..._07 (Wi-Fi, „Интерфейс към компютъра“,
--          „Дължина“), 55_..._08 (зарядни, „Конектори“, „Вид“) и 55_..._09 („За лаптоп до“, „Размер“ на
--          пад) и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- Правилата по име за Wi-Fi, размер на лаптоп, размер на пад и мощност на зарядно се копират от
-- категориите, където са написани (111, 40, 173, 161) — пусни скрипта отново, ако 55_07/08/09 се променят.
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 11') и MANUAL групите на осемте
--   категории, после ги вмъква наново.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF (SELECT count(*) FROM filter_attributes
        WHERE origin = 'MANUAL' AND slug IN ('colour', 'wifi-standard', 'wifi-bands', 'pc-interface', 'cable-length',
                                             'charger-type', 'charger-power', 'cable-connector', 'cable-product-type',
                                             'laptop-size', 'pad-size')) <> 11 THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_01, 07, 08 и 09.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((104, 'Безжични адаптери'), (156, 'Смарт часовници'),
                                (43, 'Охлаждащи поставки за лаптопи'), (42, 'Зарядни за лаптопи'),
                                (64, 'Падове за мишки'), (132, 'Захранващи кабели'), (192, 'Софтуер'),
                                (147, 'Стативи /Триподи/'))) <> 8 THEN
        RAISE EXCEPTION 'Някоя от категориите 104, 156, 43, 42, 64, 132, 192, 147 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 11';
DELETE FROM filter_value_rules WHERE note = '55 партида 11';
DELETE FROM filter_name_rules WHERE note = '55 партида 11';
DELETE FROM category_filters WHERE category_id IN (104, 156, 43, 42, 64, 132, 192, 147) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('watch-type',     'Вид',                       'Type',             'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('display-shape',  'Форма на дисплея',          'Display shape',    'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('watch-features', 'Функции',                   'Features',         'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('stand-type',     'Вид',                       'Type',             'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('material',       'Материал',                  'Material',         'ENUM', NULL, NULL, FALSE, 40, 'MANUAL'),
    ('pad-material',   'Повърхност',                'Surface',          'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('power-plug',     'Щепсел / конектор',         'Plug / connector', 'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('software-type',  'Вид софтуер',               'Software type',    'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('license-term',   'Лиценз',                    'License term',     'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('tripod-type',    'Вид',                       'Type',             'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('tripod-height',  'Макс. височина',            'Max height',       'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('tripod-for',     'Предназначен за',           'Made for',         'ENUM', NULL, NULL, FALSE, 30, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('watch-type', 'display-shape', 'watch-features', 'stand-type', 'material', 'pad-material',
                   'power-plug', 'software-type', 'license-term', 'tripod-type', 'tripod-height', 'tripod-for')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 11'
FROM (VALUES
    ('wifi-bands',         'Честота (GHz)',                  104::BIGINT),
    ('wifi-bands',         'Frequency',                      104),
    ('pc-interface',       'Интерфейс',                      104),
    ('watch-type',         'Тип продукт',                    156),
    ('display-shape',      'Вид дисплей',                    156),
    ('watch-features',     'Сензори',                        156),
    ('stand-type',         'Тип аксесоар',                   43),
    ('laptop-size',        'Макс. диагонал',                 43),
    ('material',           'Материал',                       43),
    ('charger-type',       'Тип аксесоар',                   42),
    ('charger-power',      'Мощност',                        42),
    ('cable-connector',    'Тип конектор',                   42),
    ('pad-material',       'Покритие',                       64),
    ('power-plug',         'Тип конектор 1',                 132),
    ('power-plug',         'Тип конектор 2',                 132),
    ('power-plug',         'Интерфейс 1',                    132),
    ('cable-length',       'Дължина (м)',                    132),
    ('cable-product-type', 'Тип аксесоар',                   132),
    ('software-type',      'Продукт',                        192),
    ('license-term',       'Период на валидност на лиценза', 192),
    ('tripod-type',        'Тип',                            147),
    ('tripod-height',      'Макс. височина (cm)',            147),
    ('tripod-for',         'Предназначен за',                147),
    ('material',           'Материал',                       147)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('watch-type', 'Смарт часовник', 'Smart watch', 1), ('watch-type', 'Детски часовник', 'Kids watch', 2),
    ('watch-type', 'Фитнес гривна', 'Fitness band', 3),
    ('display-shape', 'Кръгъл', 'Round', 1), ('display-shape', 'Правоъгълен', 'Rectangular', 2),
    ('watch-features', 'GPS', 'GPS', 1), ('watch-features', 'Разговори', 'Calls', 2),
    ('watch-features', 'Пулс', 'Heart rate', 3), ('watch-features', 'Кислород в кръвта (SpO2)', 'Blood oxygen (SpO2)', 4),
    ('watch-features', '4G / SIM', '4G / SIM', 5), ('watch-features', 'Водоустойчив', 'Waterproof', 6),
    ('stand-type', 'Охладител (с вентилатор)', 'Cooler (with fan)', 1), ('stand-type', 'Стойка', 'Stand', 2),
    ('material', 'Алуминий', 'Aluminium', 1), ('material', 'Карбон', 'Carbon', 2), ('material', 'Пластмаса', 'Plastic', 3),
    ('pad-material', 'Текстил', 'Cloth', 1), ('pad-material', 'Пластмаса', 'Plastic', 2), ('pad-material', 'Гума', 'Rubber', 3),
    ('pad-material', 'Изкуствена кожа', 'Faux leather', 4),
    ('power-plug', 'Шуко', 'Schuko', 1), ('power-plug', 'Евро щепсел (C7/C8)', 'Euro plug (C7/C8)', 2),
    ('power-plug', 'IEC C13/C14 (компютър)', 'IEC C13/C14 (PC)', 3), ('power-plug', 'IEC C5 (лаптоп, „мики“)', 'IEC C5 (laptop, clover)', 4),
    ('power-plug', 'IEC C15', 'IEC C15', 5), ('power-plug', 'PC захранване (12VHPWR, ATX)', 'PC power (12VHPWR, ATX)', 6),
    ('cable-connector', 'DC жак (барел)', 'DC barrel', 6),
    ('software-type', 'Операционна система', 'Operating system', 1), ('software-type', 'Офис', 'Office', 2),
    ('software-type', 'Образователен', 'Education', 3), ('software-type', 'Антивирус', 'Antivirus', 4),
    ('software-type', 'Сървърен лиценз (CAL)', 'Server licence (CAL)', 5),
    ('license-term', 'Безсрочен', 'Perpetual', 1), ('license-term', 'Абонамент (1 година)', 'Subscription (1 year)', 2),
    ('tripod-type', 'Трипод', 'Tripod', 1), ('tripod-type', 'Мини трипод', 'Mini tripod', 2),
    ('tripod-type', 'Монопод', 'Monopod', 3), ('tripod-type', 'Селфи стик', 'Selfie stick', 4),
    ('tripod-height', 'До 50 cm', 'Up to 50 cm', 1), ('tripod-height', '51 – 120 cm', '51 – 120 cm', 2),
    ('tripod-height', '121 – 160 cm', '121 – 160 cm', 3), ('tripod-height', 'Над 160 cm', 'Over 160 cm', 4),
    ('tripod-for', 'Фотоапарат', 'Camera', 1), ('tripod-for', 'Смартфон', 'Smartphone', 2),
    ('tripod-for', 'Видеокамера', 'Camcorder', 3), ('tripod-for', 'Екшън камера (GoPro)', 'Action camera (GoPro)', 4)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 11'
FROM (VALUES
    ('watch-type', 'Детски часовник', '(kids|детск)'),
    ('watch-type', 'Фитнес гривна',   '(гривна|fitness band|fitness tracker|(^|[^[:alnum:]])band([^[:alnum:]]|$))'),
    ('watch-type', 'Смарт часовник',  '^(?!.*(kids|детск)).*(smart ?watch|смарт часовник)'),
    ('display-shape', 'Кръгъл',      '(кръгъл|round)'),
    ('display-shape', 'Правоъгълен', '(правоъгъл|square|квадрат|rectang)'),
    ('watch-features', 'GPS',                      'gps'),
    ('watch-features', 'Разговори',                '(разговор|calling|bluetooth call|функция за телефон)'),
    ('watch-features', 'Пулс',                     '(heart rate|пулс)'),
    ('watch-features', 'Кислород в кръвта (SpO2)', '(spo2|кислород|blood oxygen)'),
    ('watch-features', '4G / SIM',                 '((^|[^[:alnum:]])(4g|lte|sim)([^[:alnum:]]|$))'),
    ('watch-features', 'Водоустойчив',             '(водоустойчив|waterproof|(^|[^[:alnum:]])ip6[78]|[0-9] ?atm)'),
    ('stand-type', 'Охладител (с вентилатор)', '(охладител|cooler|cooling)'),
    ('stand-type', 'Стойка',                   '(стойка|stand|поставка)'),
    ('material', 'Алуминий',  '(алуминий|alumin)'),
    ('material', 'Карбон',    '(карбон|carbon)'),
    ('material', 'Пластмаса', '(пластмас|plastic|abs)'),
    ('pad-material', 'Текстил',         '(плат|текстил|cloth|fabric)'),
    ('pad-material', 'Пластмаса',       '((^|[^[:alpha:]])(pp|pvc|eva)([^[:alpha:]]|$)|фолио|пластмас)'),
    ('pad-material', 'Гума',            '(гума|rubber)'),
    ('pad-material', 'Изкуствена кожа', '(полиуретан|(^|[^[:alpha:]])pu([^[:alpha:]]|$)|кожа|leather)'),
    ('power-plug', 'Шуко',                         '(шуко|schuko|cee ?7)'),
    ('power-plug', 'Евро щепсел (C7/C8)',          '(euro[- ]?plug|евро|c7|c8|2-pin)'),
    ('power-plug', 'IEC C13/C14 (компютър)',       '(c13|c14)'),
    ('power-plug', 'IEC C5 (лаптоп, „мики“)',      '(c5|c6|3 ?pin\(iec c5\))'),
    ('power-plug', 'IEC C15',                      'c15'),
    ('power-plug', 'PC захранване (12VHPWR, ATX)', '(12vhpwr|12v-2x6|pci-?e [0-9]|20\+4|4\+4|atx)'),
    ('cable-connector', 'DC жак (барел)', '(стандартно|dc ?jack|барел|barrel|(^|[^[:alnum:]])dc([^[:alnum:]]|$))'),
    ('cable-product-type', 'Удължител', 'удължит'),
    ('software-type', 'Операционна система',   '(windows|win ?1[01]|linux|(^|[^[:alnum:]])os([^[:alnum:]]|$))'),
    ('software-type', 'Офис',                  '(office|365|word|excel)'),
    ('software-type', 'Образователен',         '(mozabook|atlas|mclass|mstudent|educational|образовател|teacher|student|whiteboard)'),
    ('software-type', 'Антивирус',             '(antivirus|антивирус|eset|kaspersky|bitdefender|norton|security)'),
    ('software-type', 'Сървърен лиценз (CAL)', '((^|[^[:alnum:]])cal([^[:alnum:]]|$)|server)'),
    ('license-term', 'Безсрочен',            '(no time limit|доживот|lifetime|perpetual|безсроч)'),
    ('license-term', 'Абонамент (1 година)', '(1 ?year|1 ?година|12 месеца|annual)'),
    ('tripod-type', 'Селфи стик',  '(селфи|selfie)'),
    ('tripod-type', 'Мини трипод', '(мини|mini|настолен|flex)'),
    ('tripod-type', 'Монопод',     '(монопод|monopod)'),
    ('tripod-type', 'Трипод',      '^(?!.*(мини|mini|селфи|selfie|монопод|monopod)).*(трипод|статив|tripod)'),
    ('tripod-height', 'До 50 cm',     '^([0-9]|[1-4][0-9]|50)([.,][0-9]+)? ?(cm|см)'),
    ('tripod-height', '51 – 120 cm',  '^(5[1-9]|[6-9][0-9]|1[01][0-9]|120)([.,][0-9]+)? ?(cm|см)'),
    ('tripod-height', '121 – 160 cm', '^(12[1-9]|1[3-5][0-9]|160)([.,][0-9]+)? ?(cm|см)'),
    ('tripod-height', 'Над 160 cm',   '^(16[1-9]|1[7-9][0-9]|[2-9][0-9]{2})([.,][0-9]+)? ?(cm|см)'),
    ('tripod-for', 'Фотоапарат',           '(фотоапарат|фото ?камер|(^|[^[:alpha:]])(dslr|slr|mirrorless|photo camera)([^[:alpha:]]|$))'),
    ('tripod-for', 'Смартфон',             '(смартфон|телефон|smartphone|phone)'),
    ('tripod-for', 'Видеокамера',          '(видео ?камер|camcorder)'),
    ('tripod-for', 'Екшън камера (GoPro)', '(gopro|екшън|action cam)')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
-- Правилата на тази партида важат и за имената, освен закотвените към цялата стойност (височина).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, c.category_id, r.pattern, r.value_id, '55 партида 11'
FROM filter_value_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
JOIN (VALUES ('watch-type', 156::BIGINT), ('display-shape', 156), ('watch-features', 156), ('stand-type', 43),
             ('material', 43), ('material', 147), ('pad-material', 64), ('power-plug', 132),
             ('cable-product-type', 132), ('software-type', 192), ('license-term', 192), ('tripod-type', 147),
             ('tripod-for', 147)) AS c(slug, category_id) ON c.slug = a.slug
WHERE r.note = '55 партида 11';

-- Готови правила по име от други категории: Wi-Fi (мрежови карти), размер на лаптоп (чанти), размер на
-- пад (геймърски падове), вид и мощност на зарядно (зарядни за телефони), дължина (мрежови кабели).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, use_parser, note)
SELECT r.attribute_id, c.target, r.pattern, r.value_id, r.use_parser, '55 партида 11'
FROM filter_name_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
JOIN (VALUES ('wifi-standard', 111::BIGINT, 104::BIGINT), ('wifi-bands', 111, 104), ('pc-interface', 111, 104),
             ('laptop-size', 40, 43), ('pad-size', 173, 64), ('charger-type', 161, 42), ('charger-power', 161, 42),
             ('cable-length', 135, 132)) AS c(slug, source, target)
  ON c.slug = a.slug AND r.category_id = c.source
WHERE r.note LIKE '55 партида %';

INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 11'
FROM (VALUES
    ('cable-connector', 42::BIGINT, 'USB-C', '(usb-?c|type-?c)'),
    -- Падове за мишки: буквата на размера между запетаи („"Business", M, Сив“).
    ('pad-size', 64, 'S',  ', ?s ?(,|$)'),
    ('pad-size', 64, 'M',  ', ?m ?(,|$)'),
    ('pad-size', 64, 'L',  ', ?l ?(,|$)'),
    ('pad-size', 64, 'XL', ', ?xl ?(,|$)'),
    ('tripod-height', 147, 'До 50 cm',     '(^|[^0-9.])(?<![-–] )(?<![-–])([1-9]|[1-4][0-9]|50) ?(cm|см)([^[:alnum:]]|$)'),
    ('tripod-height', 147, '51 – 120 cm',  '(^|[^0-9.])(?<![-–] )(?<![-–])(5[1-9]|[6-9][0-9]|1[01][0-9]|120) ?(cm|см)([^[:alnum:]]|$)'),
    ('tripod-height', 147, '121 – 160 cm', '(^|[^0-9.])(12[1-9]|1[3-5][0-9]|160) ?(cm|см)([^[:alnum:]]|$)'),
    ('tripod-height', 147, 'Над 160 cm',   '(^|[^0-9.])(16[1-9]|1[7-9][0-9]|[2-9][0-9]{2}) ?(cm|см)([^[:alnum:]]|$)')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Цвят по името.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, c.category_id, '(^|[^[:alpha:]])(' || r.words || ')([^[:alpha:]]|$)', v.id, '55 партида 11'
FROM (VALUES
    ('Черен', 'black|черен|черн[аио]'), ('Бял', 'white|бял|бял[аоие]'), ('Сив', 'gr[ae]y|сив|сив[аои]|graphite'),
    ('Сребрист', 'silver|сребрист|сребрист[аои]'), ('Червен', 'red|червен|червен[аои]'),
    ('Син', 'blue|син|синя|синьо'), ('Зелен', 'green|зелен|зелен[аои]'), ('Розов', 'pink|розов|розов[аои]')
) AS r(value, words)
JOIN filter_attributes a ON a.slug = 'colour' AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value)
CROSS JOIN (VALUES (156::BIGINT), (43), (42), (64), (132)) AS c(category_id);

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (104, 'MANUAL'), (156, 'MANUAL'), (43, 'MANUAL'), (42, 'MANUAL'), (64, 'MANUAL'), (132, 'MANUAL'),
       (192, 'MANUAL'), (147, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (104::BIGINT, 'wifi-standard', 10), (104, 'wifi-bands', 20), (104, 'pc-interface', 30),
    (156, 'watch-type', 10), (156, 'display-shape', 20), (156, 'watch-features', 30), (156, 'colour', 40),
    (43, 'stand-type', 10), (43, 'laptop-size', 20), (43, 'material', 30), (43, 'colour', 40),
    (42, 'charger-type', 10), (42, 'charger-power', 20), (42, 'cable-connector', 30), (42, 'colour', 40),
    (64, 'pad-size', 10), (64, 'pad-material', 20), (64, 'colour', 30),
    (132, 'power-plug', 10), (132, 'cable-product-type', 20), (132, 'cable-length', 30), (132, 'colour', 40),
    (192, 'software-type', 10), (192, 'license-term', 20),
    (147, 'tripod-type', 10), (147, 'tripod-height', 20), (147, 'tripod-for', 30), (147, 'material', 40)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

-- Невалиден регулярен израз спира целия rebuild (и нощния) — всеки израз се компилира тук, преди COMMIT.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT pattern FROM filter_value_rules WHERE note = '55 партида 11' AND pattern IS NOT NULL
             UNION ALL
             SELECT pattern FROM filter_name_rules WHERE note = '55 партида 11' AND pattern IS NOT NULL
    LOOP
        PERFORM '' ~* r.pattern;
    END LOOP;
END $$;

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (104, 156, 43, 42, 64, 132, 192, 147) AND origin = 'MANUAL';
    IF n <> 28 THEN
        RAISE EXCEPTION 'Очаквах 28 групи в осемте категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 11 записана: % групи в 8 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (104, 156, 43, 42, 64, 132, 192, 147) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
