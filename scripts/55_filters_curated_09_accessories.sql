-- ============================================================================
-- 55_filters_curated_09_accessories.sql
--
-- Курация на каноничния филтърен слой, партида 9 — аксесоари:
--   40  Чанти за лаптопи       163 Калъфи за телефони      41 Калъфи за таблети
--   173 Геймърски падове       169 Геймърски столове        60 Уеб камери
--   14  Термо пасти и подложки 90  Наливни мастила за принтери
--
-- ИЗИСКВА: V38–V40, скриптове 55_..._01 („Цвят“), 55_..._03 („RGB подсветка“), 55_..._04 („Марка на
--          принтера“, „Оригинален / съвместим“ и правилата им по име за мастилата) и 55_..._06
--          („Микрофон“) и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- Размерите (лаптоп, таблет, пад), натоварването на стола, топлопроводимостта и обемът са в диапазони.
-- Падовете нямат параметър за размер — той идва от името: „XL“, „Large“ или „750 x 300 mm“.
-- Резолюцията на уеб камерите е „поддържана“: VALI дава по опция за всяка (720p, 1080p…).
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 9') и MANUAL групите на осемте
--   категории, после ги вмъква наново. Правилата по име за цвета и марката на мастилата се копират
--   от „Консумативи за мастиленоструйни устройства“ — пусни скрипта отново, ако 55_04 се промени.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF (SELECT count(*) FROM filter_attributes
        WHERE origin = 'MANUAL' AND slug IN ('colour', 'rgb-lighting', 'printer-brand', 'consumable-origin', 'microphone')) <> 5 THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_01, 03, 04 и 06.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((40, 'Чанти за лаптопи'), (163, 'Калъфи за телефони'), (41, 'Калъфи за таблети'),
                                (173, 'Геймърски падове'), (169, 'Геймърски столове'), (60, 'Уеб камери'),
                                (14, 'Термо пасти и подложки'), (90, 'Наливни мастила за принтери'))) <> 8 THEN
        RAISE EXCEPTION 'Някоя от категориите 40, 163, 41, 173, 169, 60, 14, 90 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 9';
DELETE FROM filter_value_rules WHERE note = '55 партида 9';
DELETE FROM filter_name_rules WHERE note = '55 партида 9';
DELETE FROM category_filters WHERE category_id IN (40, 163, 41, 173, 169, 60, 14, 90) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('bag-type',             'Вид',                     'Type',                 'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('laptop-size',          'За лаптоп до',            'Fits laptops up to',   'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('device-brand',         'За марка',                'For brand',            'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('case-type',            'Вид калъф',               'Case type',            'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('tablet-size',          'Размер на таблета',       'Tablet size',          'ENUM', NULL, NULL, FALSE, 15, 'MANUAL'),
    ('pad-size',             'Размер',                  'Size',                 'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('chair-type',           'Вид',                     'Type',                 'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('chair-material',       'Материал',                'Material',             'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('max-load',             'Макс. натоварване',       'Max load',             'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('rocking',              'Люлеещ механизъм',        'Rocking mechanism',    'ENUM', NULL, NULL, FALSE, 40, 'MANUAL'),
    ('webcam-resolution',    'Поддържана резолюция',    'Supported resolution', 'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('focus',                'Фокус',                   'Focus',                'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('tim-type',             'Вид',                     'Type',                 'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('thermal-conductivity', 'Топлопроводимост',        'Thermal conductivity', 'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('tim-amount',           'Количество',              'Amount',               'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('ink-volume',           'Обем',                    'Volume',               'ENUM', NULL, NULL, FALSE, 40, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('bag-type', 'laptop-size', 'device-brand', 'case-type', 'tablet-size', 'pad-size', 'chair-type',
                   'chair-material', 'max-load', 'rocking', 'webcam-resolution', 'focus', 'tim-type',
                   'thermal-conductivity', 'tim-amount', 'ink-volume')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 9'
FROM (VALUES
    ('bag-type',             'Тип аксесоар',                  40::BIGINT),
    ('bag-type',             'Тип',                           40),
    ('laptop-size',          'Максимален диагонал',           40),
    ('laptop-size',          'Съвместими модели',             40),
    ('device-brand',         'Съвместими модели',             163),
    ('device-brand',         'Съвместим бранд',               163),
    ('device-brand',         'Съвместими модели',             41),
    ('device-brand',         'Съвместим бранд',               41),
    ('case-type',            'Тип',                           163),
    ('case-type',            'Тип',                           41),
    ('tablet-size',          'Диагонал (inch)',               41),
    ('rgb-lighting',         'Характеристики',                173),
    ('chair-material',       'Материал',                      169),
    ('chair-material',       'Материал на корпуса',           169),
    ('max-load',             'Максимално тегло',              169),
    ('max-load',             'Максимално подходящо тегло',    169),
    ('rocking',              'Люлеещ механизъм',              169),
    ('webcam-resolution',    'Видео резолюция (пиксели)',     60),
    ('webcam-resolution',    'Макс. видео резолюция',         60),
    ('microphone',           'Микрофон',                      60),
    ('microphone',           'Вградени устройства',           60),
    ('focus',                'Фокус',                         60),
    ('thermal-conductivity', 'Топлопроводимост',              14),
    ('tim-amount',           'Количество',                    14),
    ('consumable-origin',    'Консуматив',                    90),
    ('ink-volume',           'Капацитет съхранение',          90)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('bag-type', 'Раница', 'Backpack', 1), ('bag-type', 'Чанта', 'Bag', 2), ('bag-type', 'Калъф (sleeve)', 'Sleeve', 3),
    ('laptop-size', 'До 14"', 'Up to 14"', 1), ('laptop-size', '15 – 16"', '15 – 16"', 2),
    ('laptop-size', '17 – 18"', '17 – 18"', 3),
    ('device-brand', 'Apple', 'Apple', 1), ('device-brand', 'Samsung', 'Samsung', 2),
    ('device-brand', 'Xiaomi', 'Xiaomi', 3), ('device-brand', 'Huawei', 'Huawei', 4), ('device-brand', 'Honor', 'Honor', 5),
    ('device-brand', 'Motorola', 'Motorola', 6), ('device-brand', 'Google', 'Google', 7),
    ('device-brand', 'Nokia / Lumia', 'Nokia / Lumia', 8), ('device-brand', 'ASUS', 'ASUS', 9),
    ('device-brand', 'Lenovo', 'Lenovo', 10), ('device-brand', 'Acer', 'Acer', 11),
    ('device-brand', 'Универсален', 'Universal', 20),
    ('case-type', 'Гръб', 'Back cover', 1), ('case-type', 'Бъмпер', 'Bumper', 2),
    ('case-type', 'Флип / тефтер', 'Flip / folio', 3), ('case-type', 'Калъф-джоб', 'Pouch / sleeve', 4),
    ('case-type', 'С клавиатура', 'With keyboard', 5),
    ('tablet-size', 'До 8"', 'Up to 8"', 1), ('tablet-size', '9 – 11"', '9 – 11"', 2),
    ('tablet-size', '12" и повече', '12" and larger', 3),
    ('pad-size', 'S', 'S', 1), ('pad-size', 'M', 'M', 2), ('pad-size', 'L', 'L', 3), ('pad-size', 'XL', 'XL', 4),
    ('pad-size', 'XXL / за цялото бюро', 'XXL / desk mat', 5),
    ('chair-type', 'Геймърски стол', 'Gaming chair', 1), ('chair-type', 'Кокпит (симулатор)', 'Racing cockpit', 2),
    ('chair-material', 'Изкуствена кожа', 'Faux leather', 1), ('chair-material', 'Естествена кожа', 'Real leather', 2),
    ('chair-material', 'Текстил', 'Fabric', 3), ('chair-material', 'Мрежа', 'Mesh', 4),
    ('max-load', 'До 120 кг', 'Up to 120 kg', 1), ('max-load', '121 – 150 кг', '121 – 150 kg', 2),
    ('max-load', 'Над 150 кг', 'Over 150 kg', 3),
    ('rocking', 'Да', 'Yes', 1), ('rocking', 'Не', 'No', 2),
    ('webcam-resolution', '720p (HD)', '720p (HD)', 1), ('webcam-resolution', '1080p (Full HD)', '1080p (Full HD)', 2),
    ('webcam-resolution', '1440p (2K)', '1440p (2K)', 3), ('webcam-resolution', '4K', '4K', 4),
    ('focus', 'Автоматичен', 'Auto', 1), ('focus', 'Фиксиран', 'Fixed', 2), ('focus', 'Ръчен', 'Manual', 3),
    ('tim-type', 'Термопаста', 'Thermal paste', 1), ('tim-type', 'Термо подложка', 'Thermal pad', 2),
    ('tim-type', 'Течен метал', 'Liquid metal', 3), ('tim-type', 'Аксесоари', 'Accessories', 4),
    ('thermal-conductivity', 'До 8 W/mK', 'Up to 8 W/mK', 1), ('thermal-conductivity', '8 – 12 W/mK', '8 – 12 W/mK', 2),
    ('thermal-conductivity', 'Над 12 W/mK', 'Over 12 W/mK', 3),
    ('tim-amount', 'До 2 g', 'Up to 2 g', 1), ('tim-amount', '3 – 5 g', '3 – 5 g', 2), ('tim-amount', 'Над 5 g', 'Over 5 g', 3),
    ('ink-volume', 'До 100 ml', 'Up to 100 ml', 1), ('ink-volume', '101 – 999 ml', '101 – 999 ml', 2),
    ('ink-volume', '1 л и повече', '1 l and more', 3)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
-- Размер в инчове: „15.6 inch“, „16``“, „15.6”“ или обичайните размери без знак („13.3“, „15.6“).
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 9'
FROM (VALUES
    ('bag-type', 'Раница',         '(раница|backpack)'),
    ('bag-type', 'Чанта',          '(чанта|(^|[^[:alpha:]])bag([^[:alpha:]]|$)|briefcase|messenger|sling|shoulder)'),
    ('bag-type', 'Калъф (sleeve)', '(калъф|sleeve|(^|[^[:alpha:]])case([^[:alpha:]]|$))'),
    ('laptop-size', 'До 14"',   '(^|[^0-9.])(1[0-4])([.,][0-9])? ?("|”|″|``|''''|inch|инч)'),
    ('laptop-size', '15 – 16"', '((^|[^0-9.])(1[56])([.,][0-9])? ?("|”|″|``|''''|inch|инч)|(^|[^0-9.])(15[.,]6|16[.,][12])([^0-9]|$))'),
    ('laptop-size', '17 – 18"', '((^|[^0-9.])(1[78])([.,][0-9])? ?("|”|″|``|''''|inch|инч)|(^|[^0-9.])17[.,]3([^0-9]|$))'),
    ('device-brand', 'Apple',         '(apple|iphone|ipad)'),
    ('device-brand', 'Samsung',       '(samsung|galaxy)'),
    ('device-brand', 'Xiaomi',        '(xiaomi|redmi|poco)'),
    ('device-brand', 'Huawei',        'huawei'),
    ('device-brand', 'Honor',         '(^|[^[:alnum:]])honor([^[:alnum:]]|$)'),
    ('device-brand', 'Motorola',      '(motorola|(^|[^[:alnum:]])moto ?[a-z0-9])'),
    ('device-brand', 'Google',        '(google|pixel)'),
    ('device-brand', 'Nokia / Lumia', '(nokia|lumia|hmd)'),
    ('device-brand', 'ASUS',          '(asus|zenfone|transformer|memo ?pad|(^|[^[:alnum:]])z[a-e][0-9]{3}[a-z]{1,3}([^[:alnum:]]|$))'),
    ('device-brand', 'Lenovo',        'lenovo'),
    ('device-brand', 'Acer',          '(acer|iconia)'),
    ('device-brand', 'Универсален',   '(универсал|universal)'),
    ('case-type', 'С клавиатура',  '(keyboard|клавиатур|wireless/bluetooth)'),
    ('case-type', 'Флип / тефтер', '(flip|folio|book|тефтер|wallet|tricover|travel cover|smart cover)'),
    ('case-type', 'Бъмпер',        'bumper'),
    ('case-type', 'Калъф-джоб',    '(pouch|pocket|sleeve|джоб|carrying)'),
    ('case-type', 'Гръб',          '(back ?cover|shell|clear case|hybrid|glam|гръб|(^|[^[:alpha:]])(tpu|silicone) case)'),
    ('tablet-size', 'До 8"',        '((^|[^0-9.])[78]([.,][0-9])? ?("|”|″|inch|инч)|^[78]([.,][0-9])?"?$|nexus ?7|hd ?7)'),
    ('tablet-size', '9 – 11"',      '((^|[^0-9.])(9|10|11)([.,][0-9])? ?("|”|″|inch|инч)|^(9|10|11)([.,][0-9])?"?$)'),
    ('tablet-size', '12" и повече', '((^|[^0-9.])1[2-5]([.,][0-9])? ?("|”|″|inch|инч)|^1[2-5]([.,][0-9])?"?$)'),
    ('rgb-lighting', 'Да', '(rgb|led)'),
    ('chair-material', 'Изкуствена кожа', '(изкуствена кожа|(^|[^[:alpha:]])(pu|pvc)([^[:alpha:]]|$)|faux|eco ?leather|екокожа)'),
    ('chair-material', 'Естествена кожа', '(естествена кожа|real leather|genuine)'),
    ('chair-material', 'Текстил',         '(текстил|плат|fabric)'),
    ('chair-material', 'Мрежа',           '(мрежа|mesh)'),
    ('max-load', 'До 120 кг',    '^(1[01][0-9]|120|[5-9][0-9]) ?(kg|кг)'),
    ('max-load', '121 – 150 кг', '^(12[1-9]|1[34][0-9]|150) ?(kg|кг)'),
    ('max-load', 'Над 150 кг',   '^(15[1-9]|1[6-9][0-9]|[2-9][0-9]{2}) ?(kg|кг)'),
    ('rocking', 'Да', '^(да|yes)$'),
    ('rocking', 'Не', '^(не|no)$'),
    ('webcam-resolution', '4K',              '(4k|2160|3840|uhd)'),
    ('webcam-resolution', '1440p (2K)',      '^(?!.*(4k|2160|3840|uhd)).*(1440|(^|[^[:alnum:]])2k|2560|qhd)'),
    ('webcam-resolution', '1080p (Full HD)', '^(?!.*(4k|2160|3840|uhd|1440|(^|[^[:alnum:]])2k|2560|qhd)).*(1080|full ?-?hd|1920|1980)'),
    ('webcam-resolution', '720p (HD)',       '^(?!.*(1080|1440|2160|4k|(^|[^[:alnum:]])2k|full ?-?hd|1920|1980)).*(720|(^|[^[:alnum:]])hd([^[:alnum:]]|$)|1280)'),
    ('microphone', 'Да', '(microphone|микрофон)'),
    ('focus', 'Автоматичен', '(auto|автоматич)'),
    ('focus', 'Фиксиран',    '(fixed|фиксиран)'),
    ('focus', 'Ръчен',       '(ръчен|manual)'),
    ('thermal-conductivity', 'До 8 W/mK',   '^[0-7]([.,][0-9]+)? ?w/ ?m ?k'),
    ('thermal-conductivity', '8 – 12 W/mK', '^(8|9|1[01])([.,][0-9]+)? ?w/ ?m ?k'),
    ('thermal-conductivity', 'Над 12 W/mK', '^(1[2-9]|[2-9][0-9])([.,][0-9]+)? ?w/ ?m ?k'),
    ('tim-amount', 'До 2 g',  '^(0[.,][0-9]+|1([.,][0-9]+)?|2) ?(g|гр)'),
    ('tim-amount', '3 – 5 g', '^[3-5]([.,][0-9]+)? ?(g|гр)'),
    ('tim-amount', 'Над 5 g', '^([6-9]|[1-9][0-9]+)([.,][0-9]+)? ?(g|гр)'),
    ('ink-volume', 'До 100 ml',    '(^|[^0-9.])([1-9][0-9]?|100)([.,][0-9]+)? ?ml([^[:alnum:]]|$)'),
    ('ink-volume', '101 – 999 ml', '(^|[^0-9.])(10[1-9]|1[1-9][0-9]|[2-9][0-9]{2}) ?ml([^[:alnum:]]|$)'),
    ('ink-volume', '1 л и повече', '((^|[^0-9.])[1-9][0-9]{3} ?ml([^[:alnum:]]|$)|(^|[^0-9.])[1-9] ?(l|л)([^[:alnum:]]|$))')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
-- Същите изрази като за параметрите, освен закотвените към цялата стойност.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, c.category_id, r.pattern, r.value_id, '55 партида 9'
FROM filter_value_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
JOIN (VALUES ('bag-type', 40::BIGINT), ('laptop-size', 40), ('device-brand', 163), ('device-brand', 41),
             ('case-type', 163), ('case-type', 41), ('tablet-size', 41), ('rgb-lighting', 173),
             ('chair-material', 169), ('webcam-resolution', 60), ('microphone', 60), ('ink-volume', 90))
     AS c(slug, category_id) ON c.slug = a.slug
WHERE r.note = '55 партида 9' AND r.pattern NOT LIKE '^(?!%' AND r.pattern NOT LIKE '^[%' AND r.pattern NOT LIKE '^(%';

-- Резолюцията на уеб камерата е изключваща („най-високата“) — изразите с ^(?!…) важат и за името.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, 60, r.pattern, r.value_id, '55 партида 9'
FROM filter_value_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.slug = 'webcam-resolution' AND a.origin = 'MANUAL'
WHERE r.note = '55 партида 9' AND r.pattern LIKE '^(?!%';

INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 9'
FROM (VALUES
    -- Падове: буквата на размера или ШИРИНАТА — първата мярка („750 x 300 x 3mm“ е 750, „1,200 x 550“ е 1200).
    ('pad-size', 173::BIGINT, 'XXL / за цялото бюро', '(xxl|3xl|extended|разширен|desk ?mat|(^|[^0-9])(?<![x×х*] )(?<![x×х*])(?<![0-9][,.])(9[0-9]{2}|1[,.]?[0-9]{3}) ?(mm)? ?[x×х*]|(^|[^0-9])(?<![x×х*] )(?<![x×х*])(?<![0-9][,.])(9[0-9]|1[0-2][0-9])( ?[x×х*] ?[0-9]{2}([.,][0-9])?)? ?(x ?[0-9.]+ ?)?(cm|см))'),
    ('pad-size', 173, 'XL', '((^|[^[:alnum:]x])xl([^[:alnum:]]|$)|x-?large|extra large|(^|[^0-9])(?<![x×х*] )(?<![x×х*])(?<![0-9][,.])[6-8][0-9]{2} ?(mm)? ?[x×х*]|(^|[^0-9])(?<![x×х*] )(?<![x×х*])(?<![0-9][,.])[6-8][0-9]( ?[x×х*] ?[0-9]{2}([.,][0-9])?)? ?(x ?[0-9.]+ ?)?(cm|см))'),
    ('pad-size', 173, 'L', '((^|[^[:alnum:]x-])l([^[:alnum:]]|$)|(?<!x-)(?<!extra )(?<!x)large|(^|[^0-9])(?<![x×х*] )(?<![x×х*])(?<![0-9][,.])[45][0-9]{2} ?(mm)? ?[x×х*]|(^|[^0-9])(?<![x×х*] )(?<![x×х*])(?<![0-9][,.])[45][0-9]( ?[x×х*] ?[0-9]{2}([.,][0-9])?)? ?(x ?[0-9.]+ ?)?(cm|см))'),
    ('pad-size', 173, 'M', '((^|[^[:alnum:]])m$|medium|(^|[^0-9])(?<![x×х*] )(?<![x×х*])(?<![0-9][,.])(2[5-9][0-9]|3[0-9]{2}) ?(mm)? ?[x×х*]|(^|[^0-9])(?<![x×х*] )(?<![x×х*])(?<![0-9][,.])(2[5-9]|3[0-9])( ?[x×х*] ?[0-9]{2}([.,][0-9])?)? ?(x ?[0-9.]+ ?)?(cm|см))'),
    ('pad-size', 173, 'S', '(small|(^|[^0-9])(?<![x×х*] )(?<![x×х*])(?<![0-9][,.])(1[0-9]{2}|2[0-4][0-9]) ?(mm)? ?[x×х*])'),
    ('chair-type', 169, 'Кокпит (симулатор)', '(cockpit|кокпит|playseat|racing seat|f-gt|simulator)'),
    ('chair-type', 169, 'Геймърски стол',     '^(?!.*(cockpit|кокпит|playseat|f-gt|simulator)).*(стол|chair)'),
    ('focus', 60, 'Автоматичен', '(autofocus|auto ?focus|автофокус)'),
    ('tim-type', 14, 'Термо подложка', '(подложк|thermal pad|(^|[^[:alnum:]])pad([^[:alnum:]]|$)|minus pad)'),
    ('tim-type', 14, 'Течен метал',    '(liquid metal|течен метал|conductonaut)'),
    ('tim-type', 14, 'Термопаста',     '(паста|paste|grease)'),
    ('tim-type', 14, 'Аксесоари',      '(рамка|frame|(^|[^[:alpha:]])лак([^[:alpha:]]|$)|shield|почиств|cleaner|шпатула|remover)'),
    ('thermal-conductivity', 14, 'До 8 W/mK',   '(^|[^0-9.])[0-7]([.,][0-9]+)? ?w/ ?m ?k'),
    ('thermal-conductivity', 14, '8 – 12 W/mK', '(^|[^0-9.])(8|9|1[01])([.,][0-9]+)? ?w/ ?m ?k'),
    ('thermal-conductivity', 14, 'Над 12 W/mK', '(^|[^0-9.])(1[2-9]|[2-9][0-9])([.,][0-9]+)? ?w/ ?m ?k'),
    ('tim-amount', 14, 'До 2 g',  '(^|[^0-9.])(0[.,][0-9]+|1([.,][0-9]+)?|2) ?(g|гр)([^[:alnum:]]|$)'),
    ('tim-amount', 14, '3 – 5 g', '(^|[^0-9.])[3-5]([.,][0-9]+)? ?(g|гр)([^[:alnum:]]|$)'),
    ('tim-amount', 14, 'Над 5 g', '(^|[^0-9.])([6-9]|[1-9][0-9]+)([.,][0-9]+)? ?(g|гр)([^[:alnum:]]|$)')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Наливни мастила: марката на принтера, оригинален/съвместим и цветът по името — както при касетите (55_04).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, 90, r.pattern, r.value_id, '55 партида 9'
FROM filter_name_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
                        AND a.slug IN ('printer-brand', 'consumable-origin', 'colour')
WHERE r.category_id = 87 AND r.note = '55 партида 4';

-- Цвят по името (MOST: „ASUS BUMPER CASE ZE500CL BLUE“, „LUMIA 532/435 SHELL GREEN“).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, c.category_id, '(^|[^[:alpha:]])(' || r.words || ')([^[:alpha:]]|$)', v.id, '55 партида 9'
FROM (VALUES
    ('Черен', 'black|черен|черн[аио]'), ('Бял', 'white|бял|бял[аоие]'), ('Сив', 'gr[ae]y|сив|сив[аои]'),
    ('Червен', 'red|червен|червен[аои]'), ('Син', 'blue|син|синя|синьо'), ('Жълт', 'yellow|жълт|жълт[аои]'),
    ('Зелен', 'green|зелен|зелен[аои]'), ('Розов', 'pink|розов|розов[аои]'), ('Лилав', 'purple|лилав|лилав[аои]'),
    ('Оранжев', 'orange|оранжев|оранжев[аои]'), ('Кафяв', 'brown|кафяв|кафяв[аои]'),
    ('Прозрачен', 'clear|transparent|прозрач[а-я]*')
) AS r(value, words)
JOIN filter_attributes a ON a.slug = 'colour' AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value)
CROSS JOIN (VALUES (40::BIGINT), (163), (41), (173), (169)) AS c(category_id);

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (40, 'MANUAL'), (163, 'MANUAL'), (41, 'MANUAL'), (173, 'MANUAL'), (169, 'MANUAL'), (60, 'MANUAL'),
       (14, 'MANUAL'), (90, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (40::BIGINT, 'bag-type', 10), (40, 'laptop-size', 20), (40, 'colour', 30),
    (163, 'device-brand', 10), (163, 'case-type', 20), (163, 'colour', 30),
    (41, 'device-brand', 10), (41, 'tablet-size', 15), (41, 'case-type', 20), (41, 'colour', 30),
    (173, 'pad-size', 10), (173, 'rgb-lighting', 20), (173, 'colour', 30),
    (169, 'chair-type', 10), (169, 'chair-material', 20), (169, 'max-load', 30), (169, 'rocking', 40),
    (169, 'colour', 50),
    (60, 'webcam-resolution', 10), (60, 'microphone', 20), (60, 'focus', 30),
    (14, 'tim-type', 10), (14, 'thermal-conductivity', 20), (14, 'tim-amount', 30),
    (90, 'printer-brand', 10), (90, 'colour', 20), (90, 'consumable-origin', 30), (90, 'ink-volume', 40)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

-- Невалиден регулярен израз спира целия rebuild (и нощния) — всеки израз се компилира тук, преди COMMIT.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT pattern FROM filter_value_rules WHERE note = '55 партида 9' AND pattern IS NOT NULL
             UNION ALL
             SELECT pattern FROM filter_name_rules WHERE note = '55 партида 9' AND pattern IS NOT NULL
    LOOP
        PERFORM '' ~* r.pattern;
    END LOOP;
END $$;

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (40, 163, 41, 173, 169, 60, 14, 90) AND origin = 'MANUAL';
    IF n <> 28 THEN
        RAISE EXCEPTION 'Очаквах 28 групи в осемте категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 9 записана: % групи в 8 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (40, 163, 41, 173, 169, 60, 14, 90) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
