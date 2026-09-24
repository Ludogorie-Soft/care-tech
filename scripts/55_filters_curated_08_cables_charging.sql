-- ============================================================================
-- 55_filters_curated_08_cables_charging.sql
--
-- Курация на каноничния филтърен слой, партида 8 — кабели, адаптери и зареждане:
--   129 Кабели за мобилни устройства    131 Видео кабели     133 PC кабели     130 Аудио кабели
--   128 Адаптери, конвертори            161 Зарядни за телефони                160 Външни батерии
--
-- ИЗИСКВА: V38–V40, скриптове 55_..._01 (ползва „Цвят“) и 55_..._07 (ползва „Дължина“ и правилата ѝ
--          по име) и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- „Конектор 1“ и „Конектор 2“ дават едно свойство „Конектори“: кабел USB-C – Lightning излиза и при
--   USB-C, и при Lightning — купувачът търси „кабел с Lightning“, не „Lightning отляво“.
-- Мощността на зарядните и капацитетът на външните батерии са в диапазони.
-- Резолюцията е „поддържана“, не „максимална“: VALI дава по опция за всяка (4K и 8K), и 8K кабелът
--   правилно излиза и при търсене на 4K.
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 8') и MANUAL групите на седемте
--   категории, после ги вмъква наново.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'colour' AND origin = 'MANUAL')
       OR NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'cable-length' AND origin = 'MANUAL') THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_01 и 55_filters_curated_07.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((129, 'Кабели за мобилни устройства'), (131, 'Видео кабели'), (133, 'PC кабели'),
                                (130, 'Аудио кабели'), (128, 'Адаптери, конвертори'),
                                (161, 'Зарядни за телефони'), (160, 'Външни батерии'))) <> 7 THEN
        RAISE EXCEPTION 'Някоя от категориите 129, 131, 133, 130, 128, 161, 160 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 8';
DELETE FROM filter_value_rules WHERE note = '55 партида 8';
DELETE FROM filter_name_rules WHERE note = '55 партида 8';
DELETE FROM category_filters WHERE category_id IN (129, 131, 133, 130, 128, 161, 160) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('cable-connector',    'Конектори',               'Connectors',        'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('cable-data-speed',   'Скорост на данните',      'Data speed',        'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('video-resolution',   'Поддържана резолюция',    'Supported resolution', 'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('cable-product-type', 'Вид',                     'Type',              'ENUM', NULL, NULL, FALSE, 5,  'MANUAL'),
    ('charger-type',       'Вид зарядно',             'Charger type',      'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('charger-power',      'Мощност',                 'Power',             'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('fast-charge',        'Бързо зареждане (PD/QC)', 'Fast charging',     'ENUM', NULL, NULL, FALSE, 40, 'MANUAL'),
    ('powerbank-capacity', 'Капацитет',               'Capacity',          'ENUM', NULL, NULL, FALSE, 10, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('cable-connector', 'cable-data-speed', 'video-resolution', 'cable-product-type', 'charger-type',
                   'charger-power', 'fast-charge', 'powerbank-capacity')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 8'
FROM (VALUES
    ('cable-connector',    'Конектор 1',                                129::BIGINT),
    ('cable-connector',    'Конектор 2',                                129),
    ('cable-connector',    'Ляв конектор',                              129),
    ('cable-connector',    'Десен конектор',                            129),
    ('cable-connector',    'Конектор 1',                                131),
    ('cable-connector',    'Конектор 2',                                131),
    ('cable-connector',    'Конектор 1',                                133),
    ('cable-connector',    'Конектор 2',                                133),
    ('cable-connector',    'Конектор 1',                                130),
    ('cable-connector',    'Конектор 2',                                130),
    ('cable-connector',    'Тип конектор 1',                            128),
    ('cable-connector',    'Тип конектор 2',                            128),
    ('cable-connector',    'Ляв конектор',                              128),
    ('cable-connector',    'Десен конектор',                            128),
    ('cable-connector',    'Свързаност',                                161),
    ('cable-connector',    'Тип конектори на изхода',                   161),
    ('cable-connector',    'Свързаност',                                160),
    ('cable-length',       'Дължина кабел',                             129),
    ('cable-length',       'Дължина на кабела',                         129),
    ('cable-length',       'Дължина кабел',                             131),
    ('cable-data-speed',   'Трансфер на данни',                         129),
    ('cable-data-speed',   'Съвместими стандарти за кабел',             129),
    ('video-resolution',   'Макс. резолюция',                           131),
    ('video-resolution',   'Максимална резолюция',                      128),
    ('cable-product-type', 'Тип продукт',                               130),
    ('cable-product-type', 'Тип продукт',                               128),
    ('charger-type',       'Тип аксесоар',                              161),
    ('charger-type',       'Тип захранване',                            161),
    ('charger-power',      'Изход',                                     161),
    ('charger-power',      'Максимално изходно захранване',             161),
    ('fast-charge',        'Захранващ блок и адаптер - характеристики', 161),
    ('fast-charge',        'Захранващ блок и адаптер - характеристики', 160),
    ('fast-charge',        'Специални характеристики',                  160),
    ('powerbank-capacity', 'Капацитет',                                 160),
    ('powerbank-capacity', 'Капацитет на батерията',                    160)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('cable-connector', 'USB-C', 'USB-C', 1), ('cable-connector', 'USB-A', 'USB-A', 2),
    ('cable-connector', 'Lightning', 'Lightning', 3), ('cable-connector', 'micro USB', 'micro USB', 4),
    ('cable-connector', 'mini USB', 'mini USB', 5), ('cable-connector', 'HDMI', 'HDMI', 10),
    ('cable-connector', 'mini HDMI', 'mini HDMI', 11), ('cable-connector', 'micro HDMI', 'micro HDMI', 12),
    ('cable-connector', 'DisplayPort', 'DisplayPort', 13), ('cable-connector', 'mini DisplayPort', 'mini DisplayPort', 14),
    ('cable-connector', 'DVI', 'DVI', 15), ('cable-connector', 'VGA', 'VGA', 16), ('cable-connector', 'SCART', 'SCART', 17),
    ('cable-connector', '3.5 mm жак', '3.5 mm jack', 20), ('cable-connector', '6.3 mm жак', '6.3 mm jack', 21),
    ('cable-connector', 'RCA (чинч)', 'RCA', 22), ('cable-connector', 'XLR', 'XLR', 23),
    ('cable-connector', 'S/PDIF (оптичен)', 'S/PDIF (optical)', 24),
    ('cable-connector', 'SATA', 'SATA', 30), ('cable-connector', 'Molex', 'Molex', 31),
    ('cable-connector', 'Сериен (RS-232)', 'Serial (RS-232)', 32),
    ('cable-data-speed', 'USB 2.0 (480 Mbps)', 'USB 2.0 (480 Mbps)', 1), ('cable-data-speed', '5 Gbps', '5 Gbps', 2),
    ('cable-data-speed', '10 Gbps', '10 Gbps', 3), ('cable-data-speed', '20 Gbps', '20 Gbps', 4),
    ('cable-data-speed', '40 Gbps и повече', '40 Gbps and more', 5),
    ('video-resolution', 'Full HD (1080p)', 'Full HD (1080p)', 1), ('video-resolution', '4K', '4K', 2),
    ('video-resolution', '8K и повече', '8K and more', 3),
    ('cable-product-type', 'Кабел', 'Cable', 1), ('cable-product-type', 'Адаптер', 'Adapter', 2),
    ('cable-product-type', 'Удължител', 'Extension', 3), ('cable-product-type', 'Сплитер / суич', 'Splitter / switch', 4),
    ('charger-type', 'Мрежово зарядно', 'Wall charger', 1), ('charger-type', 'За кола', 'Car charger', 2),
    ('charger-type', 'Безжично', 'Wireless', 3), ('charger-type', 'Зарядна станция', 'Charging station', 4),
    ('charger-power', 'До 20 W', 'Up to 20 W', 1), ('charger-power', '21 – 45 W', '21 – 45 W', 2),
    ('charger-power', '46 – 100 W', '46 – 100 W', 3), ('charger-power', 'Над 100 W', 'Over 100 W', 4),
    ('fast-charge', 'Да', 'Yes', 1),
    ('powerbank-capacity', 'До 5 000 mAh', 'Up to 5,000 mAh', 1),
    ('powerbank-capacity', '6 000 – 10 000 mAh', '6,000 – 10,000 mAh', 2),
    ('powerbank-capacity', '11 000 – 20 000 mAh', '11,000 – 20,000 mAh', 3),
    ('powerbank-capacity', 'Над 20 000 mAh', 'Over 20,000 mAh', 4)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
-- HDMI и DisplayPort без „mini“/„micro“ пред или след; „usb/usb-c“ е и USB-A, и USB-C.
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 8'
FROM (VALUES
    ('cable-connector', 'USB-C',       '(usb[- ]?c|type[- ]?c|тип c|thunderbolt|usb4)'),
    ('cable-connector', 'USB-A',       '(usb[- ]?[aа](?![[:alpha:]])|type[- ]?[aа](?![[:alpha:]])|usb тип [aа]|^usb$|(^|[^[:alnum:]-])usb/)'),
    ('cable-connector', 'Lightning',   'lightning'),
    ('cable-connector', 'micro USB',   '(micro ?-?usb|micro-?b)'),
    ('cable-connector', 'mini USB',    'mini ?-?usb'),
    ('cable-connector', 'HDMI',        '(?<!mini )(?<!micro )(?<!mini-)(?<!micro-)(?<!mini)(?<!micro)hdmi(?! ?(micro|mini))'),
    ('cable-connector', 'mini HDMI',   '(mini ?-?hdmi|hdmi mini)'),
    ('cable-connector', 'micro HDMI',  '(micro ?-?hdmi|hdmi micro|type-?d)'),
    ('cable-connector', 'DisplayPort', '(?<!mini )(?<!мини )(?<!mini-)(?<!mini)(display ?port|(^|[^[:alnum:]])dp([^[:alnum:]]|$))'),
    ('cable-connector', 'mini DisplayPort', '(mini|мини) ?-?(display ?port|dp)'),
    ('cable-connector', 'DVI',         'dvi'),
    ('cable-connector', 'VGA',         '(vga|d-?sub 15|db-?15)'),
    ('cable-connector', 'SCART',       'scart'),
    ('cable-connector', '3.5 mm жак',  '((?<![0-9.])3[.,]5 ?(mm|мм)|jack 3[.,]5|3[.,]5 ?(mm |мм )?жак)'),
    ('cable-connector', '6.3 mm жак',  '((?<![0-9.])6[.,][35] ?(mm|мм)|jack 6[.,][35])'),
    ('cable-connector', 'RCA (чинч)',  '(rca|чинч|cinch)'),
    ('cable-connector', 'XLR',         'xlr'),
    ('cable-connector', 'S/PDIF (оптичен)', '(s/pdif|spdif|toslink|оптичен)'),
    ('cable-connector', 'SATA',        '(^|[^e])sata'),
    ('cable-connector', 'Molex',       'molex'),
    ('cable-connector', 'Сериен (RS-232)', '(rs-?232|d-?sub 9|db-?9|serial)'),
    ('cable-length', 'До 1 м', '^[1-9][0-9]? ?(cm|см)$'),
    ('cable-data-speed', 'USB 2.0 (480 Mbps)', '(480 ?mb|usb ?2\.0)'),
    ('cable-data-speed', '5 Gbps',             '(^|[^0-9.])5 ?gb'),
    ('cable-data-speed', '10 Gbps',            '(^|[^0-9.])10 ?gb'),
    ('cable-data-speed', '20 Gbps',            '(^|[^0-9.])20 ?gb'),
    ('cable-data-speed', '40 Gbps и повече',   '((^|[^0-9.])(40|80|120) ?gb|usb4|thunderbolt ?[345])'),
    ('video-resolution', '8K и повече',     '(8k|10k|16k|7680)'),
    ('video-resolution', '4K',              '^(?!.*(8k|10k|16k|7680)).*(4k|3840|4096|2160|uhd|ultra[- ]?hd)'),
    ('video-resolution', 'Full HD (1080p)', '^(?!.*(4k|8k|10k|16k|2160|3840|4096|uhd|ultra[- ]?hd)).*(1080|full[- ]?hd|1920)'),
    ('cable-product-type', 'Кабел',          '(кабел|cable)'),
    ('cable-product-type', 'Адаптер',        '(адаптер|adapter|преходник|конвертор|converter)'),
    ('cable-product-type', 'Сплитер / суич', '(сплитер|splitter|суич|switch)'),
    ('charger-type', 'За кола',         '(за кола|car|запалка|автомоб)'),
    ('charger-type', 'Безжично',        '(безжичн|wireless)'),
    ('charger-type', 'Мрежово зарядно', '(мрежово|захранващ адаптер|стенно)'),
    ('charger-type', 'Зарядна станция', '(станция|station)'),
    ('charger-power', 'До 20 W',    '^([1-9]|1[0-9]|20)([.,][0-9]+)? ?w'),
    ('charger-power', '21 – 45 W',  '^(2[1-9]|3[0-9]|4[0-5])([.,][0-9]+)? ?w'),
    ('charger-power', '46 – 100 W', '^(4[6-9]|[5-9][0-9]|100)([.,][0-9]+)? ?w'),
    ('charger-power', 'Над 100 W',  '^(10[1-9]|1[1-9][0-9]|[2-9][0-9]{2})([.,][0-9]+)? ?w'),
    ('fast-charge', 'Да', '((^|[^[:alnum:]])(pd|qc|pps)([^[:alnum:]]|[0-9]|$)|power delivery|quick charge|бързо|fast charg)'),
    ('powerbank-capacity', 'До 5 000 mAh',        '(^|[^0-9])[1-5][0-9]{3} ?mah'),
    ('powerbank-capacity', '6 000 – 10 000 mAh',  '(^|[^0-9])([6-9][0-9]{3}|10[0-9]{3}) ?mah'),
    ('powerbank-capacity', '11 000 – 20 000 mAh', '(^|[^0-9])(1[1-9][0-9]{3}|20[0-9]{3}) ?mah'),
    ('powerbank-capacity', 'Над 20 000 mAh',      '(^|[^0-9])(2[1-9][0-9]{3}|[3-9][0-9]{4}) ?mah')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
-- Конектори, скорост, резолюция, вид, капацитет: същите изрази като за параметрите.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, c.category_id, r.pattern, r.value_id, '55 партида 8'
FROM filter_value_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
JOIN (VALUES ('cable-connector', 129::BIGINT), ('cable-connector', 131), ('cable-connector', 133),
             ('cable-connector', 130), ('cable-connector', 128), ('cable-connector', 161), ('cable-connector', 160),
             ('cable-data-speed', 129), ('video-resolution', 131), ('video-resolution', 128),
             ('cable-product-type', 130), ('cable-product-type', 128), ('fast-charge', 161), ('fast-charge', 160),
             ('powerbank-capacity', 160), ('charger-type', 161)) AS c(slug, category_id) ON c.slug = a.slug
WHERE r.note = '55 партида 8';

-- Дължина по името: същите правила като при мрежовите кабели (55_07) + сантиметри („40cm“).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, c.category_id, r.pattern, r.value_id, '55 партида 8'
FROM filter_name_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.slug = 'cable-length' AND a.origin = 'MANUAL'
CROSS JOIN (VALUES (129::BIGINT), (131), (133), (130)) AS c(category_id)
WHERE r.category_id = 135 AND r.note = '55 партида 7';

INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 8'
FROM (VALUES
    ('cable-length', 129::BIGINT, 'До 1 м', '(^|[^0-9.,])[1-9][0-9]? ?(cm|см)([^[:alnum:]]|$)'),
    ('cable-length', 131, 'До 1 м', '(^|[^0-9.,])[1-9][0-9]? ?(cm|см)([^[:alnum:]]|$)'),
    ('cable-length', 133, 'До 1 м', '(^|[^0-9.,])[1-9][0-9]? ?(cm|см)([^[:alnum:]]|$)'),
    ('cable-length', 130, 'До 1 м', '(^|[^0-9.,])[1-9][0-9]? ?(cm|см)([^[:alnum:]]|$)'),
    ('cable-product-type', 130, 'Удължител', 'удължител'),
    ('cable-product-type', 128, 'Удължител', 'удължител'),
    ('charger-power', 161, 'До 20 W',    '(^|[^0-9.])([1-9]|1[0-9]|20)([.,][0-9]+)? ?w([^[:alnum:]]|$)'),
    ('charger-power', 161, '21 – 45 W',  '(^|[^0-9.])(2[1-9]|3[0-9]|4[0-5])([.,][0-9]+)? ?w([^[:alnum:]]|$)'),
    ('charger-power', 161, '46 – 100 W', '(^|[^0-9.])(4[6-9]|[5-9][0-9]|100)([.,][0-9]+)? ?w([^[:alnum:]]|$)'),
    ('charger-power', 161, 'Над 100 W',  '(^|[^0-9.])(10[1-9]|1[1-9][0-9]|[2-9][0-9]{2})([.,][0-9]+)? ?w([^[:alnum:]]|$)'),
    ('charger-type', 161, 'Мрежово зарядно', '(220 ?v|мрежово|wall|eu)')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Цвят по името („… Nylon, зелен“, „… Черно“).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, c.category_id, '(^|[^[:alpha:]])(' || r.words || ')([^[:alpha:]]|$)', v.id, '55 партида 8'
FROM (VALUES
    ('Черен', 'black|черен|черн[аио]'), ('Бял', 'white|бял|бял[аоие]'), ('Сив', 'gr[ae]y|сив|сив[аои]'),
    ('Червен', 'red|червен|червен[аои]'), ('Син', 'blue|син|синя|синьо'), ('Жълт', 'yellow|жълт|жълт[аои]'),
    ('Зелен', 'green|зелен|зелен[аои]'), ('Розов', 'pink|розов|розов[аои]'), ('Лилав', 'purple|лилав|лилав[аои]'),
    ('Сребрист', 'silver|сребрист|сребрист[аои]')
) AS r(value, words)
JOIN filter_attributes a ON a.slug = 'colour' AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value)
CROSS JOIN (VALUES (129::BIGINT), (131), (128), (161), (160)) AS c(category_id);

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (129, 'MANUAL'), (131, 'MANUAL'), (133, 'MANUAL'), (130, 'MANUAL'), (128, 'MANUAL'), (161, 'MANUAL'),
       (160, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (129::BIGINT, 'cable-connector', 10), (129, 'cable-length', 20), (129, 'cable-data-speed', 30), (129, 'colour', 40),
    (131, 'cable-connector', 10), (131, 'video-resolution', 20), (131, 'cable-length', 30), (131, 'colour', 40),
    (133, 'cable-connector', 10), (133, 'cable-length', 20),
    (130, 'cable-product-type', 5), (130, 'cable-connector', 10), (130, 'cable-length', 20),
    (128, 'cable-product-type', 5), (128, 'cable-connector', 10), (128, 'video-resolution', 20), (128, 'colour', 30),
    (161, 'charger-type', 10), (161, 'charger-power', 20), (161, 'cable-connector', 30), (161, 'fast-charge', 40),
    (161, 'colour', 50),
    (160, 'powerbank-capacity', 10), (160, 'cable-connector', 20), (160, 'fast-charge', 30), (160, 'colour', 40)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

-- Невалиден регулярен израз спира целия rebuild (и нощния) — всеки израз се компилира тук, преди COMMIT.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT pattern FROM filter_value_rules WHERE note = '55 партида 8' AND pattern IS NOT NULL
             UNION ALL
             SELECT pattern FROM filter_name_rules WHERE note = '55 партида 8' AND pattern IS NOT NULL
    LOOP
        PERFORM '' ~* r.pattern;
    END LOOP;
END $$;

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (129, 131, 133, 130, 128, 161, 160) AND origin = 'MANUAL';
    IF n <> 26 THEN
        RAISE EXCEPTION 'Очаквах 26 групи в седемте категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 8 записана: % групи в 7 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (129, 131, 133, 130, 128, 161, 160) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
