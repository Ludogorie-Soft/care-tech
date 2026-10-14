-- ============================================================================
-- 55_filters_curated_06_audio.sql
--
-- Курация на каноничния филтърен слой, партида 6 — аудио:
--   159 Bluetooth слушалки     171 Геймърски слушалки     66  Слушалки      67 Слушалки (тапи)
--   59  Звукови системи и тонколони                        120 Преносими тонколони
--   65  Микрофони
--
-- ИЗИСКВА: V38–V40, скриптове 55_..._01 (ползва „Цвят“) и 55_..._03 (ползва „Свързване“ и
--          „RGB подсветка“) и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- „Bluetooth слушалки“ е категорията, с която започна одитът: 118 групи за 89 продукта в прод.
-- Времето на работа и мощността са в диапазони: доставчиците пишат „20 h“, „45 ч.“, „42W (RMS)“.
-- VALI пише името на параметъра „Mощност RMS“ с ЛАТИНСКО M — източникът по-долу е написан така умишлено.
-- „Преносими тонколони“ нямат „Вид“: доставчикът е записал всички като преносими, вкл. Echo Dot и Nest.
-- „Blue“ не е цвят по името: Blue Yeti е марка микрофони, BLUE VO!CE е технология в слушалките.
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 6') и MANUAL групите на седемте
--   категории, после ги вмъква наново.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'colour' AND origin = 'MANUAL')
       OR NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'connection' AND origin = 'MANUAL') THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_01 и 55_filters_curated_03.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((159, 'Bluetooth слушалки'), (171, 'Геймърски слушалки'), (66, 'Слушалки'),
                                (67, 'Слушалки (тапи)'), (59, 'Звукови системи и тонколони'),
                                (120, 'Преносими тонколони'), (65, 'Микрофони'))) <> 7 THEN
        RAISE EXCEPTION 'Някоя от категориите 159, 171, 66, 67, 59, 120, 65 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 6';
DELETE FROM filter_value_rules WHERE note = '55 партида 6';
DELETE FROM filter_name_rules WHERE note = '55 партида 6';
DELETE FROM category_filters WHERE category_id IN (159, 171, 66, 67, 59, 120, 65) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('headphone-type',  'Тип слушалки',                  'Headphone type',     'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('anc',             'Активно шумопотискане (ANC)',   'Active noise cancelling', 'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('battery-life',    'Време на работа',               'Battery life',       'ENUM', NULL, NULL, FALSE, 40, 'MANUAL'),
    ('audio-connector', 'Жак / конектор',                'Connector',          'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('microphone',      'Микрофон',                      'Microphone',         'ENUM', NULL, NULL, FALSE, 25, 'MANUAL'),
    ('gaming-platform', 'Платформа',                     'Platform',           'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('surround-sound',  'Съраунд звук (7.1 / spatial)',  'Surround sound',     'ENUM', NULL, NULL, FALSE, 40, 'MANUAL'),
    ('speaker-system',  'Система',                       'System',             'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('speaker-type',    'Вид тонколони',                 'Speaker type',       'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('output-power',    'Мощност (RMS)',                 'Output power (RMS)', 'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('bluetooth',       'Bluetooth',                     'Bluetooth',          'ENUM', NULL, NULL, FALSE, 40, 'MANUAL'),
    ('water-resistant', 'Водоустойчивост',               'Water resistant',    'ENUM', NULL, NULL, FALSE, 50, 'MANUAL'),
    ('mic-type',        'Тип микрофон',                  'Microphone type',    'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('mic-form',        'Вид микрофон',                  'Microphone form',    'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('polar-pattern',   'Насоченост',                    'Polar pattern',      'ENUM', NULL, NULL, FALSE, 30, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('headphone-type', 'anc', 'battery-life', 'audio-connector', 'microphone', 'gaming-platform',
                   'surround-sound', 'speaker-system', 'speaker-type', 'output-power', 'bluetooth',
                   'water-resistant', 'mic-type', 'mic-form', 'polar-pattern')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 6'
FROM (VALUES
    ('headphone-type',  'Тип слушалки',                         159::BIGINT),
    ('headphone-type',  'Форм фактор',                          159),
    ('headphone-type',  'Форм фактор',                          66),
    ('headphone-type',  'Тип',                                  66),
    ('anc',             'Активно шумопотискане (ANC)',          NULL),
    ('anc',             'Активно шумопотискане',                159),
    ('anc',             'Активно шумопотискане',                171),
    ('anc',             'Активно шумопотискане',                66),
    ('anc',             'Характеристики',                       159),
    ('anc',             'Характеристики',                       66),
    ('battery-life',    'Време на работа',                      159),
    ('battery-life',    'Средно време за работа на батерия',    159),
    ('battery-life',    'Автономия на батерията',               120),
    ('audio-connector', 'Свързаност',                           66),
    ('audio-connector', 'Свързаност',                           67),
    ('audio-connector', 'Аудио интерфейс',                      66),
    ('audio-connector', 'Интерфейс',                            171),
    ('audio-connector', 'Интерфейс',                            65),
    ('connection',      'Технология',                           171),
    ('connection',      'Технология',                           66),
    ('connection',      'Безжична технология',                  171),
    ('connection',      'Свързване',                            65),
    ('microphone',      'Микрофон',                             66),
    ('microphone',      'Микрофон',                             67),
    ('microphone',      'Микрофон - форм фактор',               66),
    ('gaming-platform', 'Съвместимост',                         171),
    ('gaming-platform', 'Направление',                          171),
    ('surround-sound',  'Surround 7.1 Dolby',                   NULL),
    ('surround-sound',  'Тип аудио изход',                      171),
    ('speaker-system',  'Система',                              59),
    ('speaker-system',  'Компоненти',                           59),
    ('speaker-type',    'Тип система',                          59),
    ('speaker-type',    'Тип тонколонка',                       59),
    ('output-power',    'Изходна мощност',                      59),
    ('output-power',    'Mощност RMS',                          59),
    ('output-power',    'Мощност RMS (W)',                      120),
    ('bluetooth',       'Bluetooth',                            59),
    ('bluetooth',       'Интерфейс',                            59),
    ('mic-type',        'Тип микрофон',                         65),
    ('mic-form',        'Употреба',                             65),
    ('polar-pattern',   'Насоченост',                           65)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('headphone-type', 'Тапи (in-ear)', 'In-ear', 1), ('headphone-type', 'Върху ухото (on-ear)', 'On-ear', 2),
    ('headphone-type', 'Над ухото (over-ear)', 'Over-ear', 3),
    ('headphone-type', 'Отворени / с клипс', 'Open-ear / clip', 4),
    ('anc', 'Да', 'Yes', 1), ('anc', 'Не', 'No', 2),
    ('battery-life', 'До 10 ч', 'Up to 10 h', 1), ('battery-life', '11 – 20 ч', '11 – 20 h', 2),
    ('battery-life', '21 – 40 ч', '21 – 40 h', 3), ('battery-life', 'Над 40 ч', 'Over 40 h', 4),
    ('audio-connector', '3.5 mm жак', '3.5 mm jack', 1), ('audio-connector', 'USB-C', 'USB-C', 2),
    ('audio-connector', 'USB-A', 'USB-A', 3), ('audio-connector', 'Lightning', 'Lightning', 4),
    ('audio-connector', 'XLR', 'XLR', 5),
    ('microphone', 'Да', 'Yes', 1), ('microphone', 'Не', 'No', 2),
    ('gaming-platform', 'PC', 'PC', 1), ('gaming-platform', 'PlayStation', 'PlayStation', 2),
    ('gaming-platform', 'Xbox', 'Xbox', 3), ('gaming-platform', 'Nintendo Switch', 'Nintendo Switch', 4),
    ('gaming-platform', 'Mac', 'Mac', 5), ('gaming-platform', 'Мобилни устройства', 'Mobile', 6),
    ('surround-sound', 'Да', 'Yes', 1), ('surround-sound', 'Не', 'No', 2),
    ('speaker-system', '1.0 (единична)', '1.0 (single)', 1), ('speaker-system', '2.0', '2.0', 2),
    ('speaker-system', '2.1', '2.1', 3), ('speaker-system', 'Саундбар', 'Soundbar', 4),
    ('speaker-type', 'Компютърни', 'PC speakers', 1), ('speaker-type', 'Домашно аудио', 'Home audio', 2),
    ('speaker-type', 'Преносима', 'Portable', 3), ('speaker-type', 'Смарт', 'Smart', 4),
    ('output-power', 'До 10 W', 'Up to 10 W', 1), ('output-power', '11 – 30 W', '11 – 30 W', 2),
    ('output-power', '31 – 60 W', '31 – 60 W', 3), ('output-power', 'Над 60 W', 'Over 60 W', 4),
    ('bluetooth', 'Да', 'Yes', 1),
    ('water-resistant', 'Да', 'Yes', 1),
    ('mic-type', 'Динамичен', 'Dynamic', 1), ('mic-type', 'Кондензаторен', 'Condenser', 2),
    ('mic-form', 'Настолен', 'Desktop', 1), ('mic-form', 'Петличен', 'Lavalier', 2),
    ('mic-form', 'За камера', 'On-camera', 3),
    ('polar-pattern', 'Кардиоиден', 'Cardioid', 1), ('polar-pattern', 'Суперкардиоиден', 'Supercardioid', 2),
    ('polar-pattern', 'Всепосочен', 'Omnidirectional', 3), ('polar-pattern', 'Стерео', 'Stereo', 4)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
-- „ENC“ е шумопотискане на разговора (микрофона), не ANC — не се брои.
-- VALI „Behind the ear“ не се чете: дава го и на наушници върху ухото (Hama Freedom Lit), и на тапи
-- (Maxell B13-EB2); тогава типът идва от името („On-Ear“, „тапи“, „TWS“).
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 6'
FROM (VALUES
    ('headphone-type', 'Тапи (in-ear)',        '(in[- ]?ear|в ухо|тапи|earbud)'),
    ('headphone-type', 'Върху ухото (on-ear)', '(on[- ]?ear|на ухото|върху ухо)'),
    ('headphone-type', 'Над ухото (over-ear)', '(over[- ]?ear|над ухото|пълен размер|around[- ]the[- ]ear)'),
    ('headphone-type', 'Отворени / с клипс',   '(clip|open[- ]?ear|(^|[^[:alnum:]])ows([^[:alnum:]]|$)|bone|костна)'),
    ('anc', 'Да', '(^(yes|да)$|active noise cancel|(^|[^[:alnum:]])anc([^[:alnum:]]|$)|активно шумопот)'),
    ('anc', 'Не', '^(no|не)$'),
    ('battery-life', 'До 10 ч',   '^([1-9]|10)([.,][0-9]+)? ?(h|ч|час)'),
    ('battery-life', '11 – 20 ч', '^(1[1-9]|20)([.,][0-9]+)? ?(h|ч|час)'),
    ('battery-life', '21 – 40 ч', '^(2[1-9]|3[0-9]|40)([.,][0-9]+)? ?(h|ч|час)'),
    ('battery-life', 'Над 40 ч',  '^(4[1-9]|[5-9][0-9]|[1-9][0-9]{2,})([.,][0-9]+)? ?(h|ч|час)'),
    ('audio-connector', '3.5 mm жак', '((?<![0-9.])3[.,]5 ?(mm|мм|-mm)|jack|жак|(^|[^[:alnum:]])aux([^[:alnum:]]|$))'),
    ('audio-connector', 'USB-C',      '(usb[- ]?c|type[- ]?c|тип c)'),
    ('audio-connector', 'USB-A',      '(usb[- ]?[aа](?![[:alpha:]])|usb тип [aа]|^usb( 2\.0)?$)'),
    ('audio-connector', 'Lightning',  'lightning'),
    ('audio-connector', 'XLR',        'xlr'),
    ('microphone', 'Да', '(^(да|yes)$|вграден|flexible|външен|ротацион|detachable|прибиращ)'),
    ('microphone', 'Не', '^(не|no)$'),
    ('gaming-platform', 'PC',                 '((^|[^[:alnum:]])pc([^[:alnum:]]|$)|компютър|windows)'),
    ('gaming-platform', 'PlayStation',        '(playstation|(^|[^[:alnum:]])ps ?[45]([^[:alnum:]]|$))'),
    ('gaming-platform', 'Xbox',               'xbox'),
    ('gaming-platform', 'Nintendo Switch',    '(nintendo|(^|[^[:alnum:]])switch([^[:alnum:]]|$))'),
    ('gaming-platform', 'Mac',                '(^|[^[:alnum:]])mac(os)?([^[:alnum:]]|$)'),
    ('gaming-platform', 'Мобилни устройства', '(mobile|мобил|android|ios|phone|телефон)'),
    ('surround-sound', 'Да', '(7\.1|surround|dts|dolby|spatial|съраунд)'),
    ('surround-sound', 'Не', '^(не|no|стерео|stereo)$'),
    ('speaker-system', '2.1',            '(2\.1|суб ?буфер|subwoofer)'),
    ('speaker-system', '2.0',            '^(?!.*(суб|sub|2\.1)).*(2\.0|2 колонки)'),
    ('speaker-system', '1.0 (единична)', '(1 говорител|^моно$|1\.0)'),
    ('speaker-system', 'Саундбар',       '(soundbar|саундбар|sound bar)'),
    ('speaker-type', 'Компютърни',    '(pc speakers|компютър|мултимедийни)'),
    ('speaker-type', 'Домашно аудио', '(home audio|bookshelf)'),
    ('speaker-type', 'Преносима',     '(преносим|портативн|мобилна|portable|безжична/блутут)'),
    ('speaker-type', 'Смарт',         '(smart|смарт)'),
    ('output-power', 'До 10 W',   '^(0[.,][1-9][0-9]*|[1-9]([.,][0-9]+)?|10) ?w'),
    ('output-power', '11 – 30 W', '^(1[1-9]|2[0-9]|30)([.,][0-9]+)? ?w'),
    ('output-power', '31 – 60 W', '^(3[1-9]|[45][0-9]|60)([.,][0-9]+)? ?w'),
    ('output-power', 'Над 60 W',  '^(6[1-9]|[7-9][0-9]|[1-9][0-9]{2,})([.,][0-9]+)? ?w'),
    ('bluetooth', 'Да', '(bluetooth|блутут|^да$|^1$)'),
    ('mic-type', 'Динамичен',     '(динамич|dynamic)'),
    ('mic-type', 'Кондензаторен', '(кондензатор|condenser|електрет)'),
    ('mic-form', 'За камера', 'камера'),
    ('polar-pattern', 'Суперкардиоиден', '(супер|super|hyper)'),
    ('polar-pattern', 'Кардиоиден',      '^(?!.*(супер|super|hyper)).*(кардио|cardioid|еднопосоч|unidirect)'),
    ('polar-pattern', 'Всепосочен',      '(омни|omni|всепосоч)'),
    ('polar-pattern', 'Стерео',          '(стерео|stereo)'),
    -- VALI пише Bluetooth и на кирилица („Технология: Блутут“).
    ('connection', 'Bluetooth', 'блутут')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 6'
FROM (VALUES
    ('headphone-type', 159::BIGINT, 'Тапи (in-ear)',        '((^|[^[:alnum:]])tws([^[:alnum:]]|$)|true wireless|earbuds|in-?ear|тапи)'),
    ('headphone-type', 66,  'Тапи (in-ear)',        '(in-?ear|тапи|earphones)'),
    ('headphone-type', 159, 'Над ухото (over-ear)', 'over-?ear'),
    ('headphone-type', 66,  'Над ухото (over-ear)', 'over-?ear'),
    ('headphone-type', 159, 'Върху ухото (on-ear)', 'on-?ear'),
    ('headphone-type', 66,  'Върху ухото (on-ear)', 'on-?ear'),
    ('headphone-type', 159, 'Отворени / с клипс',   '((^|[^[:alnum:]])ows([^[:alnum:]]|$)|ear ?clips?|open[- ]?ear|bone conduction)'),
    ('anc', 159, 'Да', '((^|[^[:alnum:]])anc([^[:alnum:]]|$)|noise cancell?ing|шумопотискане)'),
    ('anc', 66,  'Да', '((^|[^[:alnum:]])anc([^[:alnum:]]|$)|noise cancell?ing|шумопотискане)'),
    ('anc', 171, 'Да', '((^|[^[:alnum:]])anc([^[:alnum:]]|$)|noise cancell?ing|шумопотискане)'),
    ('connection', 171, 'Безжична', '(wireless|безжичн|lightspeed|(^|[^[:alnum:]])wl([^[:alnum:]]|$)|2\.4 ?g)'),
    ('connection', 171, 'Bluetooth', '(bluetooth|блутут)'),
    ('connection', 171, 'Кабелна',  '((^|[^[:alnum:]])wired|кабелн|с кабел)'),
    ('connection', 66,  'Bluetooth', '(bluetooth|блутут)'),
    ('connection', 65,  'Безжична', '(wireless|безжичн)'),
    ('audio-connector', 66, 'USB-C',      '(usb-?c|type-?c)'),
    ('audio-connector', 67, 'USB-C',      '(usb-?c|type-?c)'),
    ('audio-connector', 66, '3.5 mm жак', '(3[.,]5 ?(mm|мм)|жак|jack)'),
    ('audio-connector', 67, '3.5 mm жак', '(3[.,]5 ?(mm|мм)|жак|jack)'),
    ('audio-connector', 65, 'USB-C',      '(usb-?c|type-?c)'),
    ('audio-connector', 65, 'XLR',        'xlr'),
    ('microphone', 66, 'Да', '(микрофон|(^|[^[:alnum:]])mic([^[:alnum:]]|$)|headset)'),
    ('microphone', 67, 'Да', '(микрофон|(^|[^[:alnum:]])mic([^[:alnum:]]|$)|headset)'),
    ('gaming-platform', 171, 'PlayStation', '(playstation|(^|[^[:alnum:]])ps ?[45]([^[:alnum:]]|$))'),
    ('gaming-platform', 171, 'Xbox',        'xbox'),
    ('gaming-platform', 171, 'Nintendo Switch', 'nintendo'),
    ('surround-sound', 171, 'Да', '(7\.1|surround|spatial|dts|dolby)'),
    ('rgb-lighting', 171, 'Да', '(rgb|chroma|lightsync)'),
    ('speaker-system', 59,  '2.1',      '(^|[^0-9.])2\.1([^0-9]|$)'),
    ('speaker-system', 59,  '2.0',      '(^|[^0-9.])(?<!usb )(?<!usb)2\.0([^0-9]|$)'),
    ('speaker-system', 59,  'Саундбар', '(soundbar|саундбар|sound bar)'),
    ('speaker-system', 120, 'Саундбар', '(soundbar|саундбар|sound bar)'),
    ('speaker-type', 59,  'Домашно аудио', '(bookshelf|studio monitor|активни тонколони)'),
    ('speaker-type', 59,  'Смарт',         '(smart|смарт|alexa|google nest|echo)'),
    ('bluetooth', 59, 'Да', '(bluetooth|блутут|(^|[^[:alnum:]])bt ?v?[0-9]|(^|[^[:alnum:]])bt([^[:alnum:]]|$))'),
    ('water-resistant', 120, 'Да', '((^|[^[:alnum:]])ipx?[4-8]|ip6[4-8]|водоустойчив|защита от вода|waterproof)'),
    ('mic-type', 65, 'Кондензаторен', '(condenser|кондензатор)'),
    ('mic-type', 65, 'Динамичен',     '(dynamic|динамичен)'),
    ('mic-form', 65, 'Настолен',  '(настолен|desktop|streaming microphone|usb microphone)'),
    ('mic-form', 65, 'Петличен',  '(брошка|петлич|lavalier|(^|[^[:alnum:]])lav([^[:alnum:]]|$))'),
    ('mic-form', 65, 'За камера', '(за камера|on-camera|shotgun)')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Цвят по името: основните думи, без „blue“ (Blue Yeti, BLUE VO!CE) и без злато/RGB.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, c.category_id, '(^|[^[:alpha:]])(' || r.words || ')([^[:alpha:]]|$)', v.id, '55 партида 6'
FROM (VALUES
    ('Черен',    'black|черен|черн[аио]'),
    ('Бял',      'white|бял|бял[аоие]|бели'),
    ('Сив',      'gr[ae]y|сив|сив[аои]|graphite|графит'),
    ('Сребрист', 'silver|сребрист|сребрист[аои]'),
    ('Червен',   'red|червен|червен[аои]'),
    ('Син',      'син|синя|синьо|сини'),
    ('Жълт',     'yellow|жълт|жълт[аои]'),
    ('Зелен',    'green|зелен|зелен[аои]'),
    ('Розов',    'pink|розов|розов[аои]'),
    ('Лилав',    'purple|violet|лилав|лилав[аои]'),
    ('Оранжев',  'orange|оранжев|оранжев[аои]')
) AS r(value, words)
JOIN filter_attributes a ON a.slug = 'colour' AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value)
CROSS JOIN (VALUES (159::BIGINT), (171), (66), (67), (59), (120), (65)) AS c(category_id);

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (159, 'MANUAL'), (171, 'MANUAL'), (66, 'MANUAL'), (67, 'MANUAL'), (59, 'MANUAL'), (120, 'MANUAL'),
       (65, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (159::BIGINT, 'headphone-type', 10), (159, 'anc', 20), (159, 'battery-life', 30), (159, 'colour', 40),
    (171, 'connection', 10), (171, 'gaming-platform', 20), (171, 'surround-sound', 30), (171, 'audio-connector', 40),
    (171, 'rgb-lighting', 50), (171, 'colour', 60),
    (66, 'headphone-type', 10), (66, 'connection', 20), (66, 'audio-connector', 30), (66, 'microphone', 40),
    (66, 'anc', 50), (66, 'colour', 60),
    (67, 'audio-connector', 10), (67, 'microphone', 20), (67, 'colour', 30),
    (59, 'speaker-system', 10), (59, 'speaker-type', 20), (59, 'output-power', 30), (59, 'bluetooth', 40),
    (59, 'colour', 50),
    (120, 'output-power', 20), (120, 'battery-life', 30), (120, 'water-resistant', 40),
    (120, 'colour', 50),
    (65, 'mic-type', 10), (65, 'mic-form', 20), (65, 'polar-pattern', 30), (65, 'connection', 40),
    (65, 'audio-connector', 50), (65, 'colour', 60)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (159, 171, 66, 67, 59, 120, 65) AND origin = 'MANUAL';
    IF n <> 34 THEN
        RAISE EXCEPTION 'Очаквах 34 групи в седемте категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 6 записана: % групи в 7 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (159, 171, 66, 67, 59, 120, 65) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
