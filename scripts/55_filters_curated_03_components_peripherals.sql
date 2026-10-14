-- ============================================================================
-- 55_filters_curated_03_components_peripherals.sql
--
-- Курация на каноничния филтърен слой, партида 3:
--   9  Захранвания        11 Кутии за компютри     4  Охладители за процесори
--   12 Вентилатори        62 Мишки                 174 Геймърски мишки
--   61 Клавиатури         172 Геймърски клавиатури
--
-- ИЗИСКВА: V38–V40, скриптове 55_..._01 и 55_..._02 (ползва „Цвят“, „Сокет“,
--          „RGB подсветка“) и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- ПРАВИЛА ПО ИМЕ НА ПРОДУКТА — само когато параметрите на продукта мълчат:
--   мощността и 80 PLUS нивото на захранването („850W 80+ Gold“), размерът на
--   вентилатора („120mm“), безжичната връзка и кирилизацията на клавиатурата
--   („… BG“), стъкленият панел на кутията („TG“), цветът („White Edition“).
--   Цветът по име има собствени, по-тесни правила (виж т. 5): „80+ Gold“ не е златист.
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 3') и MANUAL
--   групите на осемте категории, после ги вмъква наново.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'cpu-socket' AND origin = 'MANUAL') THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_02_pc_components.sql.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((9, 'Захранвания'), (11, 'Кутии за компютри'), (4, 'Охладители за процесори'),
                                (12, 'Вентилатори'), (62, 'Мишки'), (174, 'Геймърски мишки'),
                                (61, 'Клавиатури'), (172, 'Геймърски клавиатури'))) <> 8 THEN
        RAISE EXCEPTION 'Някоя от категориите 9, 11, 4, 12, 62, 174, 61, 172 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 3';
DELETE FROM filter_value_rules WHERE note = '55 партида 3';
DELETE FROM filter_name_rules WHERE note = '55 партида 3';
DELETE FROM category_filters WHERE category_id IN (9, 11, 4, 12, 62, 174, 61, 172) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('psu-power',           'Мощност',                        'Power',               'NUMERIC', 'W',  'POWER_W',   FALSE, 10, 'MANUAL'),
    ('psu-80plus',          '80 PLUS сертификат',             '80 PLUS',             'ENUM',    NULL, NULL,        FALSE, 20, 'MANUAL'),
    ('psu-modular',         'Модулност',                      'Modular',             'ENUM',    NULL, NULL,        FALSE, 30, 'MANUAL'),
    ('psu-form-factor',     'Форм фактор',                    'Form factor',         'ENUM',    NULL, NULL,        FALSE, 40, 'MANUAL'),
    ('psu-pcie5-connector', '12V-2x6 (PCIe 5.0) конектор',    '12V-2x6 connector',   'ENUM',    NULL, NULL,        FALSE, 50, 'MANUAL'),
    ('case-board-support',  'Поддържани дънни платки',        'Motherboard support', 'ENUM',    NULL, NULL,        FALSE, 10, 'MANUAL'),
    ('case-size',           'Размер на кутията',              'Case size',           'ENUM',    NULL, NULL,        FALSE, 20, 'MANUAL'),
    ('case-gpu-length',     'Макс. дължина на видео карта',   'Max GPU length',      'NUMERIC', 'mm', 'LENGTH_MM', FALSE, 30, 'MANUAL'),
    ('case-cooler-height',  'Макс. височина на охладител',    'Max cooler height',   'NUMERIC', 'mm', 'LENGTH_MM', FALSE, 40, 'MANUAL'),
    ('case-psu-included',   'Включено захранване',            'PSU included',        'ENUM',    NULL, NULL,        FALSE, 50, 'MANUAL'),
    ('glass-panel',         'Стъклен панел',                  'Glass panel',         'ENUM',    NULL, NULL,        FALSE, 60, 'MANUAL'),
    ('cooling-type',        'Тип охлаждане',                  'Cooling type',        'ENUM',    NULL, NULL,        FALSE, 10, 'MANUAL'),
    ('fan-size',            'Размер на вентилатора',          'Fan size',            'NUMERIC', 'mm', 'FAN_MM',    FALSE, 30, 'MANUAL'),
    ('connection',          'Свързване',                      'Connection',          'ENUM',    NULL, NULL,        FALSE, 10, 'MANUAL'),
    ('mouse-dpi',           'Макс. DPI',                      'Max DPI',             'NUMERIC', NULL, 'DPI_MAX',   FALSE, 20, 'MANUAL'),
    ('mouse-buttons',       'Брой бутони',                    'Buttons',             'NUMERIC', NULL, 'COUNT',     FALSE, 30, 'MANUAL'),
    ('mouse-sensor',        'Сензор',                         'Sensor',              'ENUM',    NULL, NULL,        FALSE, 40, 'MANUAL'),
    ('key-type',            'Тип клавиши',                    'Key type',            'ENUM',    NULL, NULL,        FALSE, 20, 'MANUAL'),
    ('keyboard-size',       'Формат',                         'Layout size',         'ENUM',    NULL, NULL,        FALSE, 30, 'MANUAL'),
    ('keyboard-backlight',  'Подсветка',                      'Backlight',           'ENUM',    NULL, NULL,        FALSE, 40, 'MANUAL'),
    ('cyrillic-layout',     'Кирилизация (БДС)',              'Bulgarian layout',    'ENUM',    NULL, NULL,        FALSE, 20, 'MANUAL'),
    ('mouse-included',      'Комплект с мишка',               'Mouse included',      'ENUM',    NULL, NULL,        FALSE, 50, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('psu-power', 'psu-80plus', 'psu-modular', 'psu-form-factor', 'psu-pcie5-connector',
                   'case-board-support', 'case-size', 'case-gpu-length', 'case-cooler-height',
                   'case-psu-included', 'glass-panel', 'cooling-type', 'fan-size', 'connection', 'mouse-dpi',
                   'mouse-buttons', 'mouse-sensor', 'key-type', 'keyboard-size', 'keyboard-backlight',
                   'cyrillic-layout', 'mouse-included')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
-- „Сертификати“ е IGNORE в целия магазин (V39); при захранванията ограниченото
-- до категория правило го надделява и чете 80 PLUS нивото.
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 3'
FROM (VALUES
    ('psu-power',           'Мощност',                                    9::BIGINT),
    ('psu-power',           'Мощност [вати]',                             NULL),
    ('psu-power',           'Максимално изходно захранване',              NULL),
    ('psu-80plus',          '80 PLUS certification',                      NULL),
    ('psu-80plus',          'Сертификати',                                9),
    ('psu-80plus',          'Безопасност и екология',                     9),
    ('psu-modular',         'Модулен захранващ блок',                     NULL),
    ('psu-modular',         'Модулно захранване',                         NULL),
    ('psu-form-factor',     'Форм фактор',                                9),
    ('psu-pcie5-connector', '16-pin PCI Express 5.0 (12VHPWR) connector', NULL),
    ('psu-pcie5-connector', 'Конектори',                                  9),
    ('psu-pcie5-connector', 'Информация за кабела',                       9),
    ('case-board-support',  'Формат дънна платка',                        11),
    ('case-board-support',  'Поддържан форм фактор на дънна платка',      NULL),
    ('case-board-support',  'Дънна платка - форм фактор',                 11),
    ('case-board-support',  'Форм фактор',                                11),
    ('case-size',           'Вид кутия',                                  NULL),
    ('case-size',           'Кутия/шкаф форм фактор',                     NULL),
    ('case-gpu-length',     'Дължина GPU',                                NULL),
    ('case-gpu-length',     'Expansion card max length',                  NULL),
    ('case-cooler-height',  'Височина CPU Охладител',                     NULL),
    ('case-cooler-height',  'CPU cooler maximum height',                  NULL),
    ('case-psu-included',   'Захранващ блок',                             11),
    ('glass-panel',         'Материал на корпуса',                        11),
    ('glass-panel',         'Материали',                                  11),
    ('cooling-type',        'Тип охлаждане',                              NULL),
    ('cooling-type',        'Охладителна система - тип',                  NULL),
    ('fan-size',            'Размери вентилатор',                         NULL),
    ('fan-size',            'Размери на вентилатора',                     NULL),
    ('fan-size',            'Size [mm]',                                  NULL),
    ('rgb-lighting',        'Конектор',                                   4),
    ('rgb-lighting',        'Конектор',                                   12),
    ('rgb-lighting',        'Характеристики на мишка',                    174),
    ('connection',          'Технология',                                 62),
    ('connection',          'Тип',                                        62),
    ('connection',          'Тип',                                        61),
    ('connection',          'Технология на свързване',                    NULL),
    ('connection',          'Интерфейс клавиатура',                       NULL),
    ('connection',          'Безжична технология',                        61),
    ('connection',          'Безжична технология',                        174),
    ('connection',          'Свързване',                                  62),
    ('connection',          'Свързване',                                  174),
    ('connection',          'Свързване',                                  61),
    ('connection',          'Свързване',                                  172),
    ('connection',          'Интерфейс',                                  62),
    ('connection',          'Интерфейс',                                  174),
    ('connection',          'Интерфейс',                                  61),
    ('connection',          'Интерфейс',                                  172),
    ('mouse-dpi',           'Резолюция (dpi)',                            NULL),
    ('mouse-dpi',           'Чувствителност при движение',                NULL),
    ('mouse-dpi',           'Резолюция на движение',                      NULL),
    ('mouse-dpi',           'Чувствителност',                             174),
    ('mouse-buttons',       'Бутони/скрол',                               NULL),
    ('mouse-buttons',       'Брой бутони',                                62),
    ('mouse-buttons',       'Брой бутони',                                174),
    ('mouse-sensor',        'Сензор',                                     62),
    ('mouse-sensor',        'Сензор',                                     174),
    ('mouse-sensor',        'Технология на устройство',                   NULL),
    ('mouse-sensor',        'Сензор за мишка',                            NULL),
    ('mouse-sensor',        'Sensors',                                    62),
    ('mouse-sensor',        'Sensors',                                    174),
    ('key-type',            'Тип клавиатура',                             NULL),
    ('key-type',            'Тип клавиши',                                NULL),
    ('key-type',            'Суичове',                                    NULL),
    ('keyboard-size',       'Брой клавиши',                               NULL),
    ('keyboard-backlight',  'Подсветка',                                  61),
    ('keyboard-backlight',  'Подсветка',                                  172),
    ('keyboard-backlight',  'LED подсветка',                              61),
    ('keyboard-backlight',  'LED подсветка',                              172),
    ('cyrillic-layout',     'БДС кирилизация',                            NULL),
    ('cyrillic-layout',     'Локализация',                                61),
    ('cyrillic-layout',     'Локализация',                                172),
    ('cyrillic-layout',     'Локализация на клавиатурата',                NULL),
    ('mouse-included',      'Комплект',                                   61),
    ('mouse-included',      'Включена мишка',                             NULL)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('psu-80plus', 'White', 'White', 1), ('psu-80plus', 'Bronze', 'Bronze', 2),
    ('psu-80plus', 'Silver', 'Silver', 3), ('psu-80plus', 'Gold', 'Gold', 4),
    ('psu-80plus', 'Platinum', 'Platinum', 5), ('psu-80plus', 'Titanium', 'Titanium', 6),
    ('psu-modular', 'Модулно', 'Modular', 1), ('psu-modular', 'Полумодулно', 'Semi-modular', 2),
    ('psu-modular', 'Немодулно', 'Non-modular', 3),
    ('psu-form-factor', 'ATX', 'ATX', 1), ('psu-form-factor', 'SFX', 'SFX', 2),
    ('psu-form-factor', 'SFX-L', 'SFX-L', 3), ('psu-form-factor', 'TFX', 'TFX', 4),
    ('psu-form-factor', 'Flex ATX', 'Flex ATX', 5),
    ('psu-pcie5-connector', 'Да', 'Yes', 1),
    ('case-board-support', 'E-ATX', 'E-ATX', 1), ('case-board-support', 'ATX', 'ATX', 2),
    ('case-board-support', 'Micro-ATX', 'Micro-ATX', 3), ('case-board-support', 'Mini-ITX', 'Mini-ITX', 4),
    ('case-size', 'Full Tower', 'Full Tower', 1), ('case-size', 'Mid Tower', 'Mid Tower', 2),
    ('case-size', 'Mini Tower', 'Mini Tower', 3), ('case-size', 'Компактна (ITX/SFF)', 'Compact (ITX/SFF)', 4),
    ('case-psu-included', 'Да', 'Yes', 1), ('case-psu-included', 'Не', 'No', 2),
    ('glass-panel', 'Да', 'Yes', 1), ('glass-panel', 'Не', 'No', 2),
    ('cooling-type', 'Въздушно', 'Air', 1), ('cooling-type', 'Водно', 'Liquid', 2),
    ('connection', 'Кабелна', 'Wired', 1), ('connection', 'Безжична', 'Wireless', 2),
    ('connection', 'Bluetooth', 'Bluetooth', 3),
    ('mouse-sensor', 'Оптичен', 'Optical', 1), ('mouse-sensor', 'Лазерен', 'Laser', 2),
    ('key-type', 'Механична', 'Mechanical', 1), ('key-type', 'Оптична', 'Optical', 2),
    ('key-type', 'Аналогова (магнитна)', 'Analog (magnetic)', 3), ('key-type', 'Мембранна', 'Membrane', 4),
    ('key-type', 'Ножична', 'Scissor', 5),
    ('keyboard-size', 'Пълен размер', 'Full size', 1), ('keyboard-size', 'TKL', 'TKL', 2),
    ('keyboard-size', '75%', '75%', 3), ('keyboard-size', '65%', '65%', 4), ('keyboard-size', '60%', '60%', 5),
    ('keyboard-backlight', 'RGB', 'RGB', 1), ('keyboard-backlight', 'Да', 'Yes', 2),
    ('keyboard-backlight', 'Не', 'No', 3),
    ('cyrillic-layout', 'Да', 'Yes', 1), ('cyrillic-layout', 'Не', 'No', 2),
    ('mouse-included', 'Да', 'Yes', 1), ('mouse-included', 'Не', 'No', 2)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
-- „Интерфейс: USB“ и „USB-A Plug“ не значат кабелна: безжичните мишки и клавиатури описват така приемника.
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 3'
FROM (VALUES
    ('psu-80plus', 'White',    '(^|[^[:alnum:]])(white|standard)([^[:alnum:]]|$)'),
    ('psu-80plus', 'Bronze',   '(^|[^[:alnum:]])bronze([^[:alnum:]]|$)'),
    ('psu-80plus', 'Silver',   '(^|[^[:alnum:]])silver([^[:alnum:]]|$)'),
    ('psu-80plus', 'Gold',     '(^|[^[:alnum:]])gold([^[:alnum:]]|$)'),
    ('psu-80plus', 'Platinum', '(^|[^[:alnum:]])platinum([^[:alnum:]]|$)'),
    ('psu-80plus', 'Titanium', '(^|[^[:alnum:]])titanium([^[:alnum:]]|$)'),
    ('psu-modular', 'Полумодулно', '(semi|полу)'),
    ('psu-modular', 'Модулно',     '(^(да|yes)$|(?<!semi[- ])(?<!non[- ])(?<![[:alnum:]])(fully )?modular|напълно модулн)'),
    ('psu-modular', 'Немодулно',   '(^(не|no)$|non[- ]?modular|немодулн)'),
    ('psu-form-factor', 'SFX-L',    'sfx-?l([^[:alnum:]]|$)'),
    ('psu-form-factor', 'SFX',      'sfx(?!-?l)'),
    ('psu-form-factor', 'TFX',      'tfx'),
    ('psu-form-factor', 'Flex ATX', 'flex'),
    ('psu-form-factor', 'ATX',      '(?<!flex[ -])(?<![[:alnum:]])atx'),
    ('psu-pcie5-connector', 'Да', '(12vhpwr|12v-2x6|16[- ]?pin|pcie ?5|^[1-9]$)'),
    ('case-board-support', 'E-ATX',     '(e-?atx|extended atx)'),
    ('case-board-support', 'Micro-ATX', '(micro ?-?atx|(^|[^[:alnum:]])m-?atx|matx|µatx|μatx|uatx)'),
    ('case-board-support', 'Mini-ITX',  '(mini ?-?itx|(^|[^[:alnum:]])m?itx([^[:alnum:]]|$))'),
    ('case-board-support', 'ATX',       '(?<!micro[ -])(?<!e-)(?<!extended )(?<![[:alnum:]])atx([^[:alnum:]]|$)'),
    ('case-size', 'Full Tower',          '(full|ultra[ -]?tower|super[ -]?tower)'),
    ('case-size', 'Mid Tower',           '(mid|middle|midi)'),
    ('case-size', 'Mini Tower',          '(mini[ -]?tower|micro[ -]?tower|micro[- ]?atx tower)'),
    ('case-size', 'Компактна (ITX/SFF)', '(cube|slim|(^|[^[:alnum:]])itx|sff|small form)'),
    ('case-psu-included', 'Не', '(^0 ?w$|без|no psu|w/?o psu|^(не|no|-)$)'),
    ('case-psu-included', 'Да', '[1-9][0-9]{2,3} ?w'),
    ('glass-panel', 'Да', '(glass|стъкл|(^|[^[:alnum:]])tg([^[:alnum:]]|$)|tempered)'),
    ('glass-panel', 'Не', '^(?!.*(glass|стъкл|tempered)).*(steel|mesh|plastic|стомана|пластмас|метал|metal|spcc|sgcc|алумин|alumin)'),
    ('cooling-type', 'Водно',    '(течност|liquid|water|водн|aio)'),
    ('cooling-type', 'Въздушно', '(въздух|^cpu cooler$|(^|[^[:alnum:]])air([^[:alnum:]]|$))'),
    ('connection', 'Кабелна',   '((?<!без)кабел|(?<![[:alpha:]])wired|(?<!без)жичн|cable)'),
    ('connection', 'Безжична',  '(безжич|wireless|(^|[^[:alnum:]])rf([^[:alnum:]]|$)|2\.4 ?g|nano|dongle|receiver|ресийвър|приемник|wi-?fi)'),
    ('connection', 'Bluetooth', 'bluetooth'),
    ('mouse-sensor', 'Лазерен', '(лазер|laser)'),
    ('mouse-sensor', 'Оптичен', '(оптич|optical|pixart|pmw|paw|hero|focus|avago|adns|bc3332|instant|ka8|truemove)'),
    ('key-type', 'Мембранна',            '(мембран|membrane)'),
    ('key-type', 'Механична',            '(механич|mechanical|(^|[^[:alnum:]])(gateron|cherry|kailh|outemu|g3ms|romer|huano|ttc)([^[:alnum:]]|$)|(red|brown|blue|yellow|silver|black|green) switch|(^|[^[:alpha:]])(red|brown|blue|linear|tactile|clicky|червени|кафяви|сини)([^[:alpha:]]|$))'),
    ('key-type', 'Оптична',              '(оптич|optical)'),
    ('key-type', 'Аналогова (магнитна)', '(аналог|analog|magnetic|hall)'),
    ('key-type', 'Ножична',              '(scissor|ножич)'),
    ('keyboard-size', 'Пълен размер', '^(10[4-9]|11[0-9])$'),
    ('keyboard-size', 'TKL',          '^(8[5-8])$'),
    ('keyboard-size', '75%',          '^(8[0-4])$'),
    ('keyboard-size', '65%',          '^(6[6-9])$'),
    ('keyboard-size', '60%',          '^(6[0-3])$'),
    ('keyboard-backlight', 'RGB', '(a?rgb|16[.,]8m|per-key)'),
    ('keyboard-backlight', 'Да',  '^(?!.*rgb).*(^да$|^yes$|бял|white|backlit|(^|[^[:alnum:]])led([^[:alnum:]]|$))'),
    ('keyboard-backlight', 'Не',  '^(не|no|none|без)$'),
    ('cyrillic-layout', 'Да', '(^да$|^yes$|бълг|bulgarian|бдс|кирил|(^|[^[:alnum:]])bg([^[:alnum:]]|$))'),
    ('cyrillic-layout', 'Не', '^(?!.*(бълг|bulgarian|бдс|кирил)).*(^не$|^no$|us english|английск|international|международ|qwertz|turkish|hebrew)'),
    ('mouse-included', 'Да', '^(да|yes)$'),
    ('mouse-included', 'Не', '^(не|no)$')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 3'
FROM (VALUES
    ('psu-80plus', 9::BIGINT, 'Bronze',   '(^|[^[:alnum:]])bronze([^[:alnum:]]|$)'),
    ('psu-80plus', 9, 'Silver',   '(^|[^[:alnum:]])silver([^[:alnum:]]|$)'),
    ('psu-80plus', 9, 'Gold',     '(^|[^[:alnum:]])gold([^[:alnum:]]|$)'),
    ('psu-80plus', 9, 'Platinum', '(^|[^[:alnum:]])platinum([^[:alnum:]]|$)'),
    ('psu-80plus', 9, 'Titanium', '(^|[^[:alnum:]])titanium([^[:alnum:]]|$)'),
    ('psu-80plus', 9, 'White',    '80 ?(\+|plus) ?(white|standard)'),
    ('psu-modular', 9, 'Полумодулно', '(semi[- ]?modular|полумодул)'),
    ('psu-modular', 9, 'Модулно',     '(?<!semi[- ])(?<!non[- ])(?<![[:alnum:]])(fully )?modular'),
    ('psu-form-factor', 9, 'SFX-L', 'sfx-?l([^[:alnum:]]|$)'),
    ('psu-form-factor', 9, 'SFX',   'sfx(?!-?l)'),
    ('psu-form-factor', 9, 'TFX',   'tfx'),
    ('psu-pcie5-connector', 9, 'Да', '(atx ?3\.[01]|12vhpwr|12v-2x6|pcie ?5)'),
    ('case-size', 11, 'Full Tower',          'full[- ]?tower'),
    ('case-size', 11, 'Mid Tower',           '(mid|midi|middle)[- ]?tower'),
    ('case-size', 11, 'Mini Tower',          'mini[- ]?tower'),
    ('case-size', 11, 'Компактна (ITX/SFF)', '(cube|sff|mini[- ]?itx)'),
    ('case-psu-included', 11, 'Не', '(без захранване|w/?o psu|no psu)'),
    ('case-psu-included', 11, 'Да', '(^|[^0-9])[3-9][0-9]{2} ?w([^[:alnum:]]|$)'),
    ('glass-panel', 11, 'Да', '((^|[^[:alnum:]])tg([^[:alnum:]]|$)|tempered|glass|стъкл)'),
    ('cooling-type', 4, 'Водно', '(aio|liquid|water|водн)'),
    ('connection', 62,  'Безжична',  '(wireless|безжичн|2\.4 ?g)'),
    ('connection', 174, 'Безжична',  '(wireless|безжичн|2\.4 ?g)'),
    ('connection', 61,  'Безжична',  '(wireless|безжичн|2\.4 ?g)'),
    ('connection', 172, 'Безжична',  '(wireless|безжичн|2\.4 ?g)'),
    ('connection', 62,  'Bluetooth', 'bluetooth'),
    ('connection', 174, 'Bluetooth', 'bluetooth'),
    ('connection', 61,  'Bluetooth', 'bluetooth'),
    ('connection', 172, 'Bluetooth', 'bluetooth'),
    ('connection', 62,  'Кабелна',   '((^|[^[:alnum:]])wired|кабелн|с кабел)'),
    ('connection', 174, 'Кабелна',   '((^|[^[:alnum:]])wired|кабелн|с кабел)'),
    ('connection', 61,  'Кабелна',   '((^|[^[:alnum:]])wired|кабелн|с кабел)'),
    ('connection', 172, 'Кабелна',   '((^|[^[:alnum:]])wired|кабелн|с кабел)'),
    ('connection', 62,  'Кабелна',   '^(?!.*(wireless|безжич|bluetooth|2\.4 ?g|rf)).*(^|[^[:alnum:]])usb([^[:alnum:]]|$)'),
    ('connection', 61,  'Кабелна',   '^(?!.*(wireless|безжич|bluetooth|2\.4 ?g|rf)).*(^|[^[:alnum:]])usb([^[:alnum:]]|$)'),
    ('key-type', 61,  'Механична',            '(mechanical|механич)'),
    ('key-type', 172, 'Механична',            '(mechanical|механич)'),
    ('key-type', 61,  'Мембранна',            '(membrane|мембран)'),
    ('key-type', 172, 'Мембранна',            '(membrane|мембран)'),
    ('key-type', 172, 'Оптична',              '(optical|оптич)'),
    ('key-type', 172, 'Аналогова (магнитна)', '(magnetic|hall effect|analog)'),
    ('keyboard-size', 172, 'TKL', '(tkl|tenkeyless|80 ?%)'),
    ('keyboard-size', 172, '75%', '75 ?%'),
    ('keyboard-size', 172, '65%', '65 ?%'),
    ('keyboard-size', 172, '60%', '(60 ?%|(^|[^[:alnum:]])mini([^[:alnum:]]|$))'),
    ('keyboard-size', 172, 'Пълен размер', '(full[- ]?size|100 ?%)'),
    ('keyboard-backlight', 61,  'RGB', 'rgb'),
    ('keyboard-backlight', 172, 'RGB', 'rgb'),
    ('cyrillic-layout', 61,  'Да', '((^|[^[:alnum:]])bg([^[:alnum:]]|$)|бдс|кирил|bulgarian)'),
    ('cyrillic-layout', 172, 'Да', '((^|[^[:alnum:]])bg([^[:alnum:]]|$)|бдс|кирил|bulgarian)'),
    ('cyrillic-layout', 61,  'Не', '(^|[^[:alnum:]])us([^[:alnum:]]|$)'),
    ('cyrillic-layout', 172, 'Не', '(^|[^[:alnum:]])us([^[:alnum:]]|$)'),
    ('mouse-included', 61, 'Да', '(combo|комплект|desktop set|(^|[^[:alnum:]])set([^[:alnum:]]|$)|mouse|мишка)')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- RGB по името (охладители, вентилатори, кутии, геймърски мишки).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, c.category_id, 'rgb', v.id, '55 партида 3'
FROM filter_attributes a
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = 'да'
CROSS JOIN (VALUES (4::BIGINT), (12), (174)) AS c(category_id)
WHERE a.slug = 'rgb-lighting' AND a.origin = 'MANUAL';

-- Цвят по името. Не се копират правилата за параметрите: там „gold“, „platinum“ и „rgb“ са цвят, а в
-- името са 80 PLUS ниво („80+ Gold“) и подсветка („A-RGB“). Цвят на суич („Red Switch“, „HX Brown“)
-- също не е цвят на клавиатурата, затова при клавиатурите се четат само черно, бяло, сиво и розово.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, c.category_id,
       '(^|[^[:alpha:]])' || r.before || '(' || r.words || ')([^[:alpha:]]|$)(?![ -]?(switch|суич))',
       v.id, '55 партида 3'
FROM (VALUES
    ('Черен',    'PC KB PSU', '',                        'черен|черн[аио]|black'),
    ('Бял',      'PC KB PSU', '(?<!80\+ )(?<!80 plus )', 'бял|бял[аоие]|бели|white'),
    ('Сив',      'PC KB',     '',                        'сив|сив[аоия]|grey|gray|графит[ен]*|graphite'),
    ('Розов',    'PC KB',     '',                        'розов|розов[аои]|pink'),
    ('Сребрист', 'PC',        '',                        'сребрист|сребрист[аои]|silver'),
    ('Червен',   'PC',        '',                        'червен|червен[аои]|red'),
    ('Син',      'PC',        '',                        'син|синя|синьо|сини|blue'),
    ('Зелен',    'PC',        '',                        'зелен|зелен[аои]|green'),
    ('Жълт',     'PC',        '',                        'жълт|жълт[аои]|yellow'),
    ('Лилав',    'PC',        '',                        'лилав|лилав[аои]|purple|violet'),
    ('Оранжев',  'PC',        '',                        'оранжев|оранжев[аои]|orange')
) AS r(value, grps, before, words)
JOIN filter_attributes a ON a.slug = 'colour' AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value)
JOIN (VALUES (4::BIGINT, 'PC'), (12, 'PC'), (11, 'PC'), (62, 'PC'), (174, 'PC'),
             (61, 'KB'), (172, 'KB'), (9, 'PSU')) AS c(category_id, grp)
  ON c.grp = ANY (string_to_array(r.grps, ' '));

-- Числови стойности от името: „850W“, „120mm“.
INSERT INTO filter_name_rules (attribute_id, category_id, use_parser, note)
SELECT a.id, r.category_id, TRUE, '55 партида 3'
FROM (VALUES ('psu-power', 9::BIGINT), ('fan-size', 12), ('fan-size', 4)) AS r(slug, category_id)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL';

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (9, 'MANUAL'), (11, 'MANUAL'), (4, 'MANUAL'), (12, 'MANUAL'),
       (62, 'MANUAL'), (174, 'MANUAL'), (61, 'MANUAL'), (172, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (9::BIGINT, 'psu-power', 10), (9, 'psu-80plus', 20), (9, 'psu-modular', 30), (9, 'psu-form-factor', 40),
    (9, 'psu-pcie5-connector', 50), (9, 'colour', 60),
    (11, 'case-board-support', 10), (11, 'case-size', 20), (11, 'case-gpu-length', 30),
    (11, 'case-cooler-height', 40), (11, 'case-psu-included', 50), (11, 'glass-panel', 60), (11, 'colour', 70),
    (4, 'cooling-type', 10), (4, 'cpu-socket', 20), (4, 'fan-size', 30), (4, 'rgb-lighting', 40), (4, 'colour', 50),
    (12, 'fan-size', 10), (12, 'rgb-lighting', 20), (12, 'colour', 30),
    (62, 'connection', 10), (62, 'mouse-dpi', 20), (62, 'mouse-buttons', 30), (62, 'mouse-sensor', 40),
    (62, 'colour', 50),
    (174, 'connection', 10), (174, 'mouse-dpi', 20), (174, 'mouse-buttons', 30), (174, 'rgb-lighting', 40),
    (174, 'colour', 50),
    (61, 'connection', 10), (61, 'cyrillic-layout', 20), (61, 'key-type', 30), (61, 'keyboard-backlight', 40),
    (61, 'mouse-included', 50), (61, 'colour', 60),
    (172, 'connection', 10), (172, 'key-type', 20), (172, 'keyboard-size', 30), (172, 'keyboard-backlight', 40),
    (172, 'cyrillic-layout', 50), (172, 'colour', 60)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (9, 11, 4, 12, 62, 174, 61, 172) AND origin = 'MANUAL';
    IF n <> 43 THEN
        RAISE EXCEPTION 'Очаквах 43 групи в осемте категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 3 записана: % групи в 8 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (9, 11, 4, 12, 62, 174, 61, 172) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
