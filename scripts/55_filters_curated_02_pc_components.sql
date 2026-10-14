-- ============================================================================
-- 55_filters_curated_02_pc_components.sql
--
-- Курация на каноничния филтърен слой, партида 2 — компоненти и лаптопи:
--   3  Процесори          2  Дънни платки         6  Памети
--   17 SSD                8  Видео карти          37 Лаптопи
--
-- ИЗИСКВА: V38–V40, скрипт 55_..._01 (ползва „Диагонал“, „Резолюция“, „Цвят“)
--          и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- ЗАЩО ИМА ПРАВИЛА ПО ИМЕ НА ПРОДУКТА (V40)
--   Доставчиците пропускат ключови свойства, които са в името: MOST не дава
--   сокет на повечето процесори („AMD RYZEN 7 7800X3D BOX“), дънните платки
--   казват чипсета си в името („ASROCK B550M-HDV“). Правило по име дава стойност
--   САМО ако параметрите на продукта не са дали — данните от доставчика печелят.
--
-- ЗАЩО ЧАСТ ОТ ИМЕНАТА СА ОГРАНИЧЕНИ ДО КАТЕГОРИЯ
--   „Интерфейс“, „Форм фактор“, „Тип памет“, „Чипсет“, „Frequency“ означават
--   различни неща в различни категории (чипсетът на видео карта е „nVIDIA“,
--   типът памет на видео карта е GDDR6). Затова те важат само тук.
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 2') и
--   MANUAL групите на шестте категории, после ги вмъква наново.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF to_regclass('public.filter_name_rules') IS NULL THEN
        RAISE EXCEPTION 'V40 (filter_name_rules) липсва — първо деплой.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'screen-resolution' AND origin = 'MANUAL') THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_01_monitors_colour.sql.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((2, 'Дънни платки'), (3, 'Процесори'), (6, 'Памети'),
                                (8, 'Видео карти'), (17, 'SSD (Solid State Drive)'), (37, 'Лаптопи'))) <> 6 THEN
        RAISE EXCEPTION 'Някоя от категориите 2, 3, 6, 8, 17, 37 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 2';
DELETE FROM filter_value_rules WHERE note = '55 партида 2';
DELETE FROM filter_name_rules WHERE note = '55 партида 2';
DELETE FROM category_filters WHERE category_id IN (2, 3, 6, 8, 17, 37) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('cpu-series',          'Серия процесор',        'CPU series',       'ENUM',    NULL,  NULL,          FALSE, 10, 'MANUAL'),
    ('cpu-socket',          'Сокет',                 'Socket',           'ENUM',    NULL,  NULL,          FALSE, 20, 'MANUAL'),
    ('cpu-cores',           'Брой ядра',             'Cores',            'NUMERIC', NULL,  'COUNT',       FALSE, 30, 'MANUAL'),
    ('cpu-max-clock',       'Макс. честота',         'Max clock',        'NUMERIC', 'GHz', 'FREQ_GHZ',    FALSE, 40, 'MANUAL'),
    ('integrated-graphics', 'Вградена графика',      'Integrated GPU',   'ENUM',    NULL,  NULL,          FALSE, 50, 'MANUAL'),
    ('cooler-included',     'Охладител в комплекта', 'Cooler included',  'ENUM',    NULL,  NULL,          FALSE, 60, 'MANUAL'),
    ('memory-type',         'Тип памет',             'Memory type',      'ENUM',    NULL,  NULL,          FALSE, 70, 'MANUAL'),
    ('chipset',             'Чипсет',                'Chipset',          'ENUM',    NULL,  NULL,          FALSE, 20, 'MANUAL'),
    ('board-form-factor',   'Форм фактор',           'Form factor',      'ENUM',    NULL,  NULL,          FALSE, 30, 'MANUAL'),
    ('memory-slots',        'Слотове за памет',      'Memory slots',     'NUMERIC', NULL,  'COUNT',       FALSE, 50, 'MANUAL'),
    ('wifi',                'Wi-Fi',                 'Wi-Fi',            'ENUM',    NULL,  NULL,          FALSE, 60, 'MANUAL'),
    ('capacity',            'Капацитет',             'Capacity',         'NUMERIC', 'GB',  'CAPACITY_GB', FALSE, 20, 'MANUAL'),
    ('memory-speed',        'Честота',               'Speed',            'NUMERIC', 'MHz', 'MEM_MHZ',     FALSE, 30, 'MANUAL'),
    ('cas-latency',         'Латентност',            'CAS latency',      'NUMERIC', NULL,  'CAS_CL',      FALSE, 40, 'MANUAL'),
    ('rgb-lighting',        'RGB подсветка',         'RGB lighting',     'ENUM',    NULL,  NULL,          FALSE, 50, 'MANUAL'),
    ('ssd-interface',       'Интерфейс',             'Interface',        'ENUM',    NULL,  NULL,          FALSE, 20, 'MANUAL'),
    ('ssd-form-factor',     'Форм фактор',           'Form factor',      'ENUM',    NULL,  NULL,          FALSE, 30, 'MANUAL'),
    ('gpu-vendor',          'Производител на чипа',  'GPU vendor',       'ENUM',    NULL,  NULL,          FALSE, 10, 'MANUAL'),
    ('gpu-model',           'Графичен процесор',     'GPU',              'NUMERIC', NULL,  'GPU_MODEL',   FALSE, 20, 'MANUAL'),
    ('vram',                'Видео памет',           'Video memory',     'NUMERIC', 'GB',  'CAPACITY_GB', FALSE, 30, 'MANUAL'),
    ('vram-type',           'Тип видео памет',       'Video memory type','ENUM',    NULL,  NULL,          FALSE, 40, 'MANUAL'),
    ('ram-size',            'Оперативна памет',      'RAM',              'NUMERIC', 'GB',  'CAPACITY_GB', FALSE, 20, 'MANUAL'),
    ('disk-capacity',       'Капацитет на диска',    'Storage',          'NUMERIC', 'GB',  'CAPACITY_GB', FALSE, 30, 'MANUAL'),
    ('operating-system',    'Операционна система',   'Operating system', 'ENUM',    NULL,  NULL,          FALSE, 70, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('cpu-series', 'cpu-socket', 'cpu-cores', 'cpu-max-clock', 'integrated-graphics',
                   'cooler-included', 'memory-type', 'chipset', 'board-form-factor', 'memory-slots', 'wifi',
                   'capacity', 'memory-speed', 'cas-latency', 'rgb-lighting', 'ssd-interface',
                   'ssd-form-factor', 'gpu-vendor', 'gpu-model', 'vram', 'vram-type', 'ram-size',
                   'disk-capacity', 'operating-system')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 2'
FROM (VALUES
    -- Сокет: едно и също навсякъде (охладителите изброяват няколко — това е желано).
    ('cpu-socket',          'Сокет',                                NULL::BIGINT),
    ('cpu-socket',          'Цокъл на процесора',                   NULL),
    ('cpu-socket',          'Socket',                               NULL),
    ('cpu-socket',          'Съвместими сокети',                    NULL),
    ('cpu-socket',          'CPU socket support',                   NULL),
    ('cpu-socket',          'Поддържани процесорни цокли',          NULL),
    ('cpu-cores',           'Физически ядра',                       NULL),
    ('cpu-cores',           'Брой ядра',                            NULL),
    ('cpu-cores',           'Number of cores',                      NULL),
    ('cpu-max-clock',       'Turbo boost честота',                  NULL),
    ('cpu-max-clock',       'Turbo тактова честота',                NULL),
    ('cpu-series',          'Серия процесори',                      3),
    ('cpu-series',          'Процесор',                             3),
    ('cpu-series',          'Модел процесор',                       37),
    ('cpu-max-clock',       'Frequency',                            3),
    ('integrated-graphics', 'Графичен ускорител',                   3),
    ('integrated-graphics', 'Графика',                              3),
    ('cooler-included',     'Охладител',                            3),
    ('cooler-included',     'Fan',                                  3),
    ('cooler-included',     'Метод на охлаждане',                   3),
    ('memory-type',         'Видове памет',                         3),
    ('memory-type',         'Memory specification',                 3),
    ('memory-type',         'Поддържана памет',                     2),
    ('memory-type',         'Тип паметта',                          2),
    ('memory-type',         'Memory type',                          2),
    ('memory-type',         'Тип памет',                            6),
    ('memory-type',         'Тип',                                  6),
    ('memory-type',         'Тип памет',                            37),
    ('chipset',             'Чипсет',                               2),
    ('chipset',             'Чипсет на дънната платка',             NULL),
    ('board-form-factor',   'Форм фактор',                          2),
    ('memory-slots',        'Брой слотове памет',                   NULL),
    ('memory-slots',        'Общ брой слотове за памет',            NULL),
    ('wifi',                'Вграден Wi-Fi и Bluetooth',            2),
    ('wifi',                'Wi-Fi',                                2),
    ('wifi',                'Wireless',                             2),
    ('capacity',            'Капацитет',                            6),
    ('capacity',            'Капацитет съхранение',                 6),
    ('capacity',            'Капацитет',                            17),
    ('capacity',            'Капацитет съхранение',                 17),
    ('memory-speed',        'Честота (MHz)',                        NULL),
    ('memory-speed',        'Speed',                                6),
    ('cas-latency',         'CAS латентност',                       NULL),
    ('cas-latency',         'Латентност',                           6),
    ('rgb-lighting',        'LED подсветка',                        6),
    ('ssd-interface',       'Интерфейс',                            17),
    ('ssd-form-factor',     'Форм фактор',                          17),
    ('gpu-model',           'Графичен процесор',                    NULL),
    ('gpu-model',           'Чипсет на видео карта',                NULL),
    ('gpu-model',           'Графика',                              37),
    ('gpu-vendor',          'Чипсет',                               8),
    ('vram',                'Размер на паметта',                    8),
    ('vram',                'Капацитет на паметта',                 8),
    ('vram',                'Инсталирана видео памет',              NULL),
    ('vram-type',           'Тип памет',                            8),
    ('vram-type',           'Тип на паметта',                       8),
    ('vram-type',           'Тип паметта',                          8),
    ('ram-size',            'Размер на паметта',                    37),
    ('ram-size',            'RAM памет',                            37),
    ('disk-capacity',       'SSD',                                  37),
    ('disk-capacity',       'Капацитет за съхраненние',             37),
    ('disk-capacity',       'Капацитет за съхранение',              37),
    ('operating-system',    'ОС',                                   37),
    ('operating-system',    'Операционна система',                  37),
    ('screen-resolution',   'Резолюция на екрана',                  37)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- Английските имена, с които MOST ще подава новите си свойства след поправката
-- в MostSyncService — само спецификация, никога филтър.
INSERT INTO filter_attribute_sources (action, name_norm, origin, note)
SELECT s.action, filter_norm(s.name), 'MANUAL', '55 партида 2'
FROM (VALUES
    ('IGNORE', 'MTBF'), ('IGNORE', 'BIOS'), ('IGNORE', 'Rear panel I/O'), ('IGNORE', 'CPU''s supported'),
    ('IGNORE', 'Total bytes written (TBW)'), ('IGNORE', 'Compliance and Standarts'), ('IGNORE', 'Package'),
    ('IGNORE', 'Expansion slots'), ('IGNORE', 'Security'), ('IGNORE', 'Power adapter'),
    ('IGNORE', 'Max. PCI Express Lanes'), ('IGNORE', 'Color saturation'), ('IGNORE', 'Pixel pitch'),
    ('IGNORE', 'Tray'), ('HIDE', 'Mnfr ID')
) AS s(action, name)
ON CONFLICT DO NOTHING;

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.bg, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('cpu-series', 'Ryzen 3', 1), ('cpu-series', 'Ryzen 5', 2), ('cpu-series', 'Ryzen 7', 3),
    ('cpu-series', 'Ryzen 9', 4), ('cpu-series', 'Athlon', 5),
    ('cpu-series', 'Core i3', 10), ('cpu-series', 'Core i5', 11), ('cpu-series', 'Core i7', 12),
    ('cpu-series', 'Core i9', 13), ('cpu-series', 'Core 3', 14), ('cpu-series', 'Core 5', 15),
    ('cpu-series', 'Core 7', 16), ('cpu-series', 'Core Ultra 5', 17), ('cpu-series', 'Core Ultra 7', 18),
    ('cpu-series', 'Core Ultra 9', 19), ('cpu-series', 'Celeron / Pentium / N', 20),
    ('cpu-series', 'Snapdragon', 30),
    ('cpu-socket', 'AM4', 1), ('cpu-socket', 'AM5', 2), ('cpu-socket', 'LGA1851', 10),
    ('cpu-socket', 'LGA1700', 11), ('cpu-socket', 'LGA1200', 12), ('cpu-socket', 'LGA1151', 13),
    ('cpu-socket', 'LGA1150', 14), ('cpu-socket', 'LGA1155', 15),
    ('integrated-graphics', 'Да', 1), ('integrated-graphics', 'Не', 2),
    ('cooler-included', 'Да', 1), ('cooler-included', 'Не', 2),
    ('memory-type', 'DDR5', 1), ('memory-type', 'DDR4', 2), ('memory-type', 'DDR3', 3),
    ('memory-type', 'LPDDR5', 4), ('memory-type', 'LPDDR4', 5),
    ('board-form-factor', 'E-ATX', 1), ('board-form-factor', 'ATX', 2),
    ('board-form-factor', 'Micro-ATX', 3), ('board-form-factor', 'Mini-ITX', 4),
    ('wifi', 'Да', 1), ('wifi', 'Не', 2),
    ('rgb-lighting', 'Да', 1), ('rgb-lighting', 'Не', 2),
    ('ssd-interface', 'PCIe 5.0 NVMe', 1), ('ssd-interface', 'PCIe 4.0 NVMe', 2),
    ('ssd-interface', 'PCIe 3.0 NVMe', 3), ('ssd-interface', 'SATA', 4),
    ('ssd-form-factor', 'M.2 2280', 1), ('ssd-form-factor', 'M.2 2242', 2),
    ('ssd-form-factor', 'M.2 2230', 3), ('ssd-form-factor', '2.5"', 4),
    ('gpu-vendor', 'NVIDIA', 1), ('gpu-vendor', 'AMD', 2), ('gpu-vendor', 'Intel', 3),
    ('vram-type', 'GDDR7', 1), ('vram-type', 'GDDR6X', 2), ('vram-type', 'GDDR6', 3),
    ('vram-type', 'GDDR5', 4), ('vram-type', 'DDR4', 5),
    ('operating-system', 'Windows', 1), ('operating-system', 'Без ОС', 2),
    ('operating-system', 'Linux', 3), ('operating-system', 'macOS', 4), ('operating-system', 'ChromeOS', 5)
) AS v(slug, bg, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- Чипсетите: кодът е и етикетът. AMD първо, после Intel, по поколение.
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, c.code, c.code, filter_norm(c.code), c.ord, 'MANUAL'
FROM (VALUES
    ('A320', 1), ('B350', 2), ('X370', 3), ('B450', 4), ('X470', 5), ('A520', 6), ('B550', 7), ('X570', 8),
    ('A620', 10), ('B650', 11), ('B650E', 12), ('X670', 13), ('X670E', 14), ('B840', 15), ('B850', 16),
    ('X870', 17), ('X870E', 18),
    ('H410', 30), ('B460', 31), ('H470', 32), ('Z490', 33), ('H510', 34), ('B560', 35), ('H570', 36),
    ('Z590', 37), ('H610', 40), ('B660', 41), ('H670', 42), ('Z690', 43), ('B760', 44), ('H770', 45),
    ('Z790', 46), ('H810', 50), ('B860', 51), ('Z890', 52), ('Q870', 53), ('W680', 54), ('W880', 55),
    ('H61', 20), ('H81', 21), ('H110', 22), ('B250', 23), ('H310', 24), ('B360', 25), ('B365', 26),
    ('H370', 27), ('Z370', 28), ('Z390', 29), ('X299', 38), ('TRX50', 19), ('WRX90', 19)
) AS c(code, ord)
JOIN filter_attributes a ON a.slug = 'chipset' AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE SET sort_order = EXCLUDED.sort_order, origin = 'MANUAL';

-- Стойности на числови свойства, които текстът не дава с мерна единица.
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, numeric_value, origin)
SELECT a.id, '512 GB', '512 GB', '512', 512, 'MANUAL'
FROM filter_attributes a WHERE a.slug = 'disk-capacity' AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO NOTHING;

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
-- B = граница на дума: не-буква и не-цифра ([[:alnum:]] разпознава кирилица).
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 2'
FROM (VALUES
    ('cpu-series', 'Ryzen 3', '(^|[^[:alnum:]])(ryzen (a[il] )?(pro )?3([^[:alnum:]]|$)|r3 ?[0-9]{4})'),
    ('cpu-series', 'Ryzen 5', '(^|[^[:alnum:]])(ryzen (a[il] )?(pro )?5([^[:alnum:]]|$)|r5 ?[0-9]{4})'),
    ('cpu-series', 'Ryzen 7', '(^|[^[:alnum:]])(ryzen (a[il] )?(pro )?7([^[:alnum:]]|$)|r7 ?[0-9]{4})'),
    ('cpu-series', 'Ryzen 9', '(^|[^[:alnum:]])(ryzen (a[il] )?(pro )?(max\+? )?9([^[:alnum:]]|$)|r9 ?[0-9]{4})'),
    ('cpu-series', 'Athlon',  'athlon'),
    ('cpu-series', 'Core i3', '(^|[^[:alnum:]])i3([^[:alnum:]]|$)'),
    ('cpu-series', 'Core i5', '(^|[^[:alnum:]])i5([^[:alnum:]]|$)'),
    ('cpu-series', 'Core i7', '(^|[^[:alnum:]])i7([^[:alnum:]]|$)'),
    ('cpu-series', 'Core i9', '(^|[^[:alnum:]])i9([^[:alnum:]]|$)'),
    ('cpu-series', 'Core 3',  '(^|[^[:alnum:]])core 3 (processor )?[0-9]{3}'),
    ('cpu-series', 'Core 5',  '(^|[^[:alnum:]])core 5 (processor )?[0-9]{3}'),
    ('cpu-series', 'Core 7',  '(^|[^[:alnum:]])core 7 (processor )?[0-9]{3}'),
    ('cpu-series', 'Core Ultra 5', '(^|[^[:alnum:]])ultra ?5([^[:alnum:]]|$)'),
    ('cpu-series', 'Core Ultra 7', '(^|[^[:alnum:]])ultra ?7([^[:alnum:]]|$)'),
    ('cpu-series', 'Core Ultra 9', '(^|[^[:alnum:]])ultra ?9([^[:alnum:]]|$)'),
    ('cpu-series', 'Celeron / Pentium / N', '(celeron|pentium|(^|[^[:alnum:]])n[0-9]{3}([^[:alnum:]]|$))'),
    ('cpu-series', 'Snapdragon', 'snapdragon'),
    ('cpu-socket', 'AM4',     '(^|[^[:alnum:]])[aа][mм]4([^[:alnum:]]|$)'),
    ('cpu-socket', 'AM5',     '(^|[^[:alnum:]])[aа][mм]5([^[:alnum:]]|$)'),
    ('cpu-socket', 'LGA1851', '(^|[^0-9])1851([^0-9]|$)'),
    ('cpu-socket', 'LGA1700', '(^|[^0-9])1700([^0-9]|$)'),
    ('cpu-socket', 'LGA1200', '(^|[^0-9])1200([^0-9]|$)'),
    -- „115x“ (охладителите) = и трите сокета от поколението.
    ('cpu-socket', 'LGA1151', '(^|[^0-9])(1151|115x)([^0-9]|$)'),
    ('cpu-socket', 'LGA1150', '(^|[^0-9])(1150|115x)([^0-9]|$)'),
    ('cpu-socket', 'LGA1155', '(^|[^0-9])(1155|115x)([^0-9]|$)'),
    ('integrated-graphics', 'Не', '^(n/?a|no|none|няма|не|-|not included)$'),
    ('integrated-graphics', 'Да', '(radeon|uhd|iris|graphics|arc|vega|графика)'),
    ('cooler-included', 'Не', '^(не|no|none|няма|not included|-)$'),
    ('cooler-included', 'Да', '(^(да|yes)$|wraith|cooling fan|cooler|laminar)'),
    ('memory-type', 'DDR5',   '(^|[^[:alnum:]])ddr5([^[:alnum:]]|$)'),
    ('memory-type', 'DDR4',   '(^|[^[:alnum:]])ddr4([^[:alnum:]]|$)'),
    ('memory-type', 'DDR3',   '(^|[^[:alnum:]])ddr3l?([^[:alnum:]]|$)'),
    ('memory-type', 'LPDDR5', 'lpddr5'),
    ('memory-type', 'LPDDR4', 'lpddr4'),
    ('board-form-factor', 'E-ATX',     '(e-?atx|extended atx)'),
    ('board-form-factor', 'Micro-ATX', '(micro ?-?atx|(^|[^[:alnum:]])m-?atx|matx|µatx|uatx)'),
    ('board-form-factor', 'Mini-ITX',  '(mini ?-?itx|(^|[^[:alnum:]])itx([^[:alnum:]]|$))'),
    -- „ATX“, но не „Micro ATX“, „E-ATX“ или „Extended ATX“ (lookbehind — PostgreSQL 15).
    ('board-form-factor', 'ATX',       '(?<!micro[ -])(?<!e-)(?<!extended )(?<![[:alnum:]])atx([^[:alnum:]]|$)'),
    ('wifi', 'Не', '^(не|no|none|няма|-)$'),
    ('wifi', 'Да', '(wi-?fi|802\.11|(^|[^[:alnum:]])(ax|be)[0-9]{3}|^(да|yes)$|[0-9] x [0-9] wi)'),
    ('rgb-lighting', 'Не', '^(не|no|none|няма)$'),
    ('rgb-lighting', 'Да', '(rgb|^(да|yes)$)'),
    ('ssd-interface', 'PCIe 5.0 NVMe', '(gen ?5|pcie ?5(\.0)?|(^|[^0-9.])5\.0 ?x ?4)'),
    ('ssd-interface', 'PCIe 4.0 NVMe', '(gen ?4|pcie ?4(\.0)?|(^|[^0-9.])4\.0 ?x ?4)'),
    ('ssd-interface', 'PCIe 3.0 NVMe', '(gen ?3|pcie ?3(\.0)?|(^|[^0-9.])3\.0 ?x ?4)'),
    ('ssd-interface', 'SATA', 'sata'),
    ('ssd-form-factor', 'M.2 2280', '(2280|22 ?x ?80)'),
    ('ssd-form-factor', 'M.2 2242', '(2242|22 ?x ?42)'),
    ('ssd-form-factor', 'M.2 2230', '2230'),
    ('ssd-form-factor', '2.5"', '(^|[^0-9.])2[.,]5([^0-9]|$)'),
    ('gpu-vendor', 'NVIDIA', 'nvidia'),
    ('gpu-vendor', 'AMD', '(^|[^[:alnum:]])(amd|ati)([^[:alnum:]]|$)'),
    ('gpu-vendor', 'Intel', 'intel'),
    ('vram-type', 'GDDR7',  'gddr7'),
    ('vram-type', 'GDDR6X', 'gddr6x'),
    ('vram-type', 'GDDR6',  'gddr6([^x[:alnum:]]|$)'),
    ('vram-type', 'GDDR5',  'gddr5'),
    ('vram-type', 'DDR4',   '(^|[^[:alnum:]])ddr4'),
    ('operating-system', 'Windows', '(windows|(^|[^[:alnum:]])win ?1[01]|(^|[^[:alnum:]])w1[01])'),
    ('operating-system', 'Без ОС', '(free ?dos|(^|[^[:alnum:]])dos([^[:alnum:]]|$)|no os|no preinstalled|без ос|без операционна|^(none|n/a|free)$)'),
    ('operating-system', 'Linux', '(linux|ubuntu)'),
    ('operating-system', 'macOS', 'mac ?os'),
    ('operating-system', 'ChromeOS', 'chrome')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Чипсетите: „B650“ не бива да хване „B650E“, затова след кода следва не-буква
-- (или „M“: „Intel H510M Chipset“).
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id,
       '(^|[^[:alnum:]])' || lower(v.value_bg) || CASE WHEN v.value_bg = 'A620' THEN 'a?' ELSE '' END
           || 'm?([^[:alnum:]]|$)',
       v.id, '55 партида 2'
FROM filter_attributes a JOIN filter_values v ON v.attribute_id = a.id
WHERE a.slug = 'chipset' AND a.origin = 'MANUAL';

-- MOST дава диска на лаптопа понякога без мерна единица: „512“.
INSERT INTO filter_value_rules (attribute_id, raw_norm, value_id, note)
SELECT a.id, '512', v.id, '55 партида 2'
FROM filter_attributes a JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = '512'
WHERE a.slug = 'disk-capacity' AND a.origin = 'MANUAL';

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 2'
FROM (VALUES
    -- Процесори: сокетът по поколение на модела.
    ('cpu-socket', 3::BIGINT, 'AM4',     'ryzen [3579] (pro )?[345][0-9]{3}'),
    ('cpu-socket', 3,         'AM5',     'ryzen [3579] (pro )?[789][0-9]{3}'),
    ('cpu-socket', 3,         'LGA1700', '(^|[^[:alnum:]])i[3579][- ]?1[234][0-9]{3}'),
    ('cpu-socket', 3,         'LGA1200', '(^|[^[:alnum:]])i[3579][- ]?1[01][0-9]{3}'),
    ('cpu-socket', 3,         'LGA1851', 'ultra [579] 2[0-9]{2}'),
    ('cpu-socket', 3,         'AM4',     '(^|[^[:alnum:]])am4([^[:alnum:]]|$)'),
    ('cpu-socket', 3,         'AM5',     '(^|[^[:alnum:]])am5([^[:alnum:]]|$)'),
    ('cpu-socket', 3,         'LGA1700', '(^|[^0-9])1700([^0-9]|$)'),
    ('cpu-socket', 3,         'LGA1851', '(^|[^0-9])1851([^0-9]|$)'),
    ('cooler-included', 3,    'Не',      '(^|[^[:alnum:]])(tray|mpk)([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Ryzen 3', '(^|[^[:alnum:]])ryzen 3([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Ryzen 5', '(^|[^[:alnum:]])ryzen 5([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Ryzen 7', '(^|[^[:alnum:]])ryzen 7([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Ryzen 9', '(^|[^[:alnum:]])ryzen 9([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Core i3', '(^|[^[:alnum:]])i3([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Core i5', '(^|[^[:alnum:]])i5([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Core i7', '(^|[^[:alnum:]])i7([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Core i9', '(^|[^[:alnum:]])i9([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Core Ultra 5', '(^|[^[:alnum:]])ultra ?5([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Core Ultra 7', '(^|[^[:alnum:]])ultra ?7([^[:alnum:]]|$)'),
    ('cpu-series', 3, 'Core Ultra 9', '(^|[^[:alnum:]])ultra ?9([^[:alnum:]]|$)'),
    -- Дънни платки: сокетът и форм факторът по чипсета в името (B550M → AM4 + Micro-ATX).
    ('cpu-socket', 2, 'AM4',     '(^|[^[:alnum:]])(a320|b350|x370|b450|x470|a520|b550|x570)'),
    ('cpu-socket', 2, 'AM5',     '(^|[^[:alnum:]])(a620|b650|x670|b840|b850|x870)'),
    ('cpu-socket', 2, 'LGA1700', '(^|[^[:alnum:]])(h610|b660|h670|z690|b760|h770|z790)'),
    ('cpu-socket', 2, 'LGA1851', '(^|[^[:alnum:]])(h810|b860|z890)'),
    ('cpu-socket', 2, 'LGA1200', '(^|[^[:alnum:]])(h410|b460|h470|z490|h510|b560|h570|z590)'),
    ('board-form-factor', 2, 'Micro-ATX', '((^|[^[:alnum:]])[abhxz][0-9]{3}e?m([^[:alnum:]]|$)|m-?atx|micro)'),
    ('board-form-factor', 2, 'Mini-ITX',  '((^|[^[:alnum:]])[abhxz][0-9]{3}e?i([^[:alnum:]]|$)|itx)'),
    ('wifi', 2, 'Да', '(wi-?fi|(^|[^[:alnum:]])ax([^[:alnum:]]|$))'),
    ('memory-type', 2, 'DDR5', '(^|[^[:alnum:]])ddr5'),
    ('memory-type', 2, 'DDR4', '(^|[^[:alnum:]])ddr4'),
    -- Памети.
    ('memory-type', 6, 'DDR5', '(^|[^[:alnum:]])ddr5'),
    ('memory-type', 6, 'DDR4', '(^|[^[:alnum:]])ddr4'),
    ('memory-type', 6, 'DDR3', '(^|[^[:alnum:]])ddr3'),
    ('rgb-lighting', 6, 'Да', 'rgb'),
    -- SSD.
    ('ssd-interface', 17, 'PCIe 5.0 NVMe', '(gen ?5|pcie ?5)'),
    ('ssd-interface', 17, 'PCIe 4.0 NVMe', '(gen ?4|pcie ?4)'),
    ('ssd-interface', 17, 'PCIe 3.0 NVMe', '(gen ?3|pcie ?3)'),
    ('ssd-interface', 17, 'SATA', 'sata'),
    ('ssd-form-factor', 17, 'M.2 2280', '2280'),
    ('ssd-form-factor', 17, '2.5"', '(^|[^0-9.])2[.,]5 ?("|инча|inch)'),
    -- Видео карти.
    ('gpu-vendor', 8, 'NVIDIA', '(rtx|gtx|geforce|nvidia|quadro|(^|[^[:alnum:]])gt ?[0-9]{3,4})'),
    ('gpu-vendor', 8, 'AMD',    '(radeon|(^|[^[:alnum:]])rx ?[0-9]{3,4})'),
    ('gpu-vendor', 8, 'Intel',  '(^|[^[:alnum:]])arc ?[ab][0-9]{3}'),
    ('vram-type', 8, 'GDDR7',  'gddr7'),
    ('vram-type', 8, 'GDDR6X', 'gddr6x'),
    ('vram-type', 8, 'GDDR6',  'gddr6([^x[:alnum:]]|$)'),
    -- Лаптопи: серията процесор по модела в името.
    ('cpu-series', 37, 'Ryzen 5', '(^|[^[:alnum:]])(ryzen (a[il] )?(pro )?5([^[:alnum:]]|$)|r5 ?[0-9]{4})'),
    ('cpu-series', 37, 'Ryzen 7', '(^|[^[:alnum:]])(ryzen (a[il] )?(pro )?7([^[:alnum:]]|$)|r7 ?[0-9]{4})'),
    ('cpu-series', 37, 'Ryzen 9', '(^|[^[:alnum:]])(ryzen (a[il] )?(pro )?9([^[:alnum:]]|$)|r9 ?[0-9]{4})'),
    ('cpu-series', 37, 'Core i5', '(^|[^[:alnum:]])i5([^[:alnum:]]|$)'),
    ('cpu-series', 37, 'Core i7', '(^|[^[:alnum:]])i7([^[:alnum:]]|$)'),
    ('cpu-series', 37, 'Core Ultra 5', '(^|[^[:alnum:]])(ultra ?5|u5)([^[:alnum:]]|$)'),
    ('cpu-series', 37, 'Core Ultra 7', '(^|[^[:alnum:]])(ultra ?7|u7)([^[:alnum:]]|$)'),
    ('cpu-series', 37, 'Core 3', '(^|[^[:alnum:]])core 3 (processor )?[0-9]{3}'),
    ('cpu-series', 37, 'Core 5', '(^|[^[:alnum:]])core 5 (processor )?[0-9]{3}'),
    ('cpu-series', 37, 'Core 7', '(^|[^[:alnum:]])core 7 (processor )?[0-9]{3}')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Числови стойности от името: парсерът на свойството чете името („16GB DDR5 6000 CL30“).
INSERT INTO filter_name_rules (attribute_id, category_id, use_parser, note)
SELECT a.id, r.category_id, TRUE, '55 партида 2'
FROM (VALUES
    ('cpu-max-clock', 3::BIGINT), ('capacity', 6), ('memory-speed', 6), ('cas-latency', 6),
    ('capacity', 17), ('gpu-model', 8), ('gpu-model', 37)
) AS r(slug, category_id)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL';

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (2, 'MANUAL'), (3, 'MANUAL'), (6, 'MANUAL'), (8, 'MANUAL'), (17, 'MANUAL'), (37, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (3::BIGINT, 'cpu-series', 10), (3, 'cpu-socket', 20), (3, 'cpu-cores', 30), (3, 'cpu-max-clock', 40),
    (3, 'integrated-graphics', 50), (3, 'cooler-included', 60), (3, 'memory-type', 70),
    (2, 'cpu-socket', 10), (2, 'chipset', 20), (2, 'board-form-factor', 30), (2, 'memory-type', 40),
    (2, 'memory-slots', 50), (2, 'wifi', 60),
    (6, 'memory-type', 10), (6, 'capacity', 20), (6, 'memory-speed', 30), (6, 'cas-latency', 40),
    (6, 'rgb-lighting', 50),
    (17, 'capacity', 10), (17, 'ssd-interface', 20), (17, 'ssd-form-factor', 30),
    (8, 'gpu-vendor', 10), (8, 'gpu-model', 20), (8, 'vram', 30), (8, 'vram-type', 40),
    (37, 'cpu-series', 10), (37, 'ram-size', 20), (37, 'disk-capacity', 30), (37, 'screen-diagonal', 40),
    (37, 'screen-resolution', 50), (37, 'gpu-model', 60), (37, 'operating-system', 70), (37, 'colour', 80)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (2, 3, 6, 8, 17, 37) AND origin = 'MANUAL';
    IF n <> 33 THEN
        RAISE EXCEPTION 'Очаквах 33 групи в шестте категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 2 записана: % групи в 6 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (2, 3, 6, 8, 17, 37) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
