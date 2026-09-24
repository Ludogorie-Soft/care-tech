-- ============================================================================
-- 55_filters_curated_12_devices.sql
--
-- Курация на каноничния филтърен слой, партида 12 — устройства с ясни параметри:
--   139 Телевизори              154 Мобилни телефони       744 Таблети            124 eBook четци
--   73  Управляеми UPS-и        74  Неуправляеми UPS-и     80  Мултифункционални устройства
--   177 Геймпадове              175 Волани и педали        18  Чекмеджета за дискове
--   63  Четци за карти          95  Консумативи за 3D принтери
--
-- ИЗИСКВА: V38–V40 и скриптове 55_..._01 („Цвят“, „Диагонал“, „Честота на опресняване“), 55_..._03
--          („Свързване“), 55_..._05 („USB стандарт“, „Размер на диска“), 55_..._06 („Платформа“),
--          55_..._07 („4G/5G“), 55_..._08 („Конектори“), 55_..._09 („Размер на таблета“), 55_..._11
--          („Функции“) и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- Правилата по име за платформа, USB стандарт, конектори и размер на таблета се копират от
-- категориите, където са написани (171, 55, 129, 41) — пусни скрипта отново, ако те се променят.
-- MOST пише някои имена на параметри с точка накрая („Тип принтиране.“) — filter_norm я маха.
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 12') и MANUAL групите на
--   дванадесетте категории, после ги вмъква наново.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF (SELECT count(*) FROM filter_attributes
        WHERE origin = 'MANUAL' AND slug IN ('colour', 'screen-diagonal', 'refresh-rate', 'connection', 'usb-standard',
                                             'drive-form-factor', 'gaming-platform', 'cellular', 'cable-connector',
                                             'tablet-size', 'watch-features')) <> 11 THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_01, 03, 05, 06, 07, 08, 09 и 11.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((139, 'Телевизори'), (154, 'Мобилни телефони'), (744, 'Таблети'), (124, 'eBook четци'),
                                (73, 'Управляеми UPS-и'), (74, 'Неуправляеми UPS-и'), (80, 'Мултифункционални устройства'),
                                (177, 'Геймпадове'), (175, 'Волани и педали'), (18, 'Чекмеджета за дискове'),
                                (63, 'Четци за карти'), (95, 'Консумативи за 3D принтери'))) <> 12 THEN
        RAISE EXCEPTION 'Някоя от категориите на партида 12 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 12';
DELETE FROM filter_value_rules WHERE note = '55 партида 12';
DELETE FROM filter_name_rules WHERE note = '55 партида 12';
DELETE FROM category_filters WHERE category_id IN (139, 154, 744, 124, 73, 74, 80, 177, 175, 18, 63, 95) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('tv-resolution',   'Резолюция',              'Resolution',       'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('tv-panel',        'Технология на екрана',   'Panel technology', 'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('smart-tv',        'Смарт ТВ',               'Smart TV',         'ENUM', NULL, NULL, FALSE, 50, 'MANUAL'),
    ('phone-type',      'Вид телефон',            'Phone type',       'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('network-gen',     'Мрежа',                  'Network',          'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('dual-sim',        'Dual SIM',               'Dual SIM',         'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('tablet-storage',  'Вградена памет',         'Storage',          'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('for-kids',        'За деца',                'For kids',         'ENUM', NULL, NULL, FALSE, 40, 'MANUAL'),
    ('ups-power',       'Мощност (VA)',           'Power (VA)',       'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('ups-topology',    'Технология',             'Topology',         'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('print-tech',      'Технология на печат',    'Print technology', 'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('print-colour',    'Цветен печат',           'Colour printing',  'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('mfp-functions',   'Функции',                'Functions',        'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('paper-size',      'Макс. формат',           'Max paper size',   'ENUM', NULL, NULL, FALSE, 40, 'MANUAL'),
    ('vibration',       'Вибрация',               'Vibration',        'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('wheel-type',      'Вид',                    'Type',             'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('supported-cards', 'Поддържани карти',       'Supported cards',  'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('filament-type',   'Материал',               'Material',         'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('filament-for',    'Предназначен за',        'Made for',         'ENUM', NULL, NULL, FALSE, 20, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('tv-resolution', 'tv-panel', 'smart-tv', 'phone-type', 'network-gen', 'dual-sim', 'tablet-storage',
                   'for-kids', 'ups-power', 'ups-topology', 'print-tech', 'print-colour', 'mfp-functions', 'paper-size',
                   'vibration', 'wheel-type', 'supported-cards', 'filament-type', 'filament-for')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 12'
FROM (VALUES
    ('screen-diagonal',   'Диагонал дисплей (inch)',      139::BIGINT),
    ('tv-resolution',     'Резолюция',                    139),
    ('tv-resolution',     'Качество на изображението',    139),
    ('tv-panel',          'Технология екран',             139),
    ('smart-tv',          'Вид телевизор',                139),
    ('phone-type',        'ОС',                           154),
    ('network-gen',       'Честотна лента на мрежата',    154),
    ('dual-sim',          'SIM',                          154),
    ('tablet-size',       'Размер на дисплея',            744),
    ('cellular',          'Свързаност',                   744),
    ('tablet-storage',    'Вътрешна памет',               124),
    ('watch-features',    'Функции',                      124),
    ('ups-power',         'Изходяща мощност',             73),
    ('ups-power',         'Изходяща мощност',             74),
    ('ups-power',         'Капацитет съхранение',         74),
    ('ups-topology',      'Технология',                   73),
    ('ups-topology',      'Технология',                   74),
    ('print-tech',        'Тип касета',                   80),
    ('print-colour',      'Тип принтиране',               80),
    ('mfp-functions',     'Налични функции',              80),
    ('mfp-functions',     'Функции',                      80),
    ('mfp-functions',     'ADF',                          80),
    ('paper-size',        'Формат на принтиране',         80),
    ('paper-size',        'Формат хартия',                80),
    ('gaming-platform',   'Съвместимост',                 177),
    ('connection',        'Технология',                   177),
    ('vibration',         'Вибрации',                     177),
    ('wheel-type',        'Тип продукт',                  175),
    ('gaming-platform',   'Съвместимост',                 175),
    ('gaming-platform',   'Платформа',                    175),
    ('drive-form-factor', 'Предлага размери на гнездото на вътрешното устройство', 18),
    ('usb-standard',      'Канал за данни',               18),
    ('cable-connector',   'Интерфейс PC',                 18),
    ('supported-cards',   'Поддържани карти',             63),
    ('cable-connector',   'Интерфейс',                    63),
    ('filament-type',     'Съвместимо влакно',            95),
    ('filament-for',      'Предназначен за',              95)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('tv-resolution', 'HD Ready (720p)', 'HD Ready (720p)', 1), ('tv-resolution', 'Full HD (1080p)', 'Full HD (1080p)', 2),
    ('tv-resolution', '4K Ultra HD', '4K Ultra HD', 3), ('tv-resolution', '8K', '8K', 4),
    ('tv-panel', 'LED', 'LED', 1), ('tv-panel', 'QLED', 'QLED', 2), ('tv-panel', 'Mini LED', 'Mini LED', 3),
    ('tv-panel', 'OLED', 'OLED', 4),
    ('smart-tv', 'Да', 'Yes', 1), ('smart-tv', 'Не', 'No', 2),
    ('phone-type', 'Смартфон', 'Smartphone', 1), ('phone-type', 'Телефон с копчета', 'Feature phone', 2),
    ('phone-type', 'Флип (сгъваем)', 'Flip', 3), ('phone-type', 'За възрастни (големи бутони)', 'Senior (big buttons)', 4),
    ('network-gen', '5G', '5G', 1), ('network-gen', '4G (LTE)', '4G (LTE)', 2), ('network-gen', '2G / 3G', '2G / 3G', 3),
    ('dual-sim', 'Да', 'Yes', 1),
    ('tablet-storage', '16 GB', '16 GB', 1), ('tablet-storage', '32 GB', '32 GB', 2), ('tablet-storage', '64 GB', '64 GB', 3),
    ('tablet-storage', '128 GB', '128 GB', 4), ('tablet-storage', '256 GB', '256 GB', 5),
    ('for-kids', 'Да', 'Yes', 1),
    ('watch-features', 'Безжично зареждане', 'Wireless charging', 7), ('watch-features', 'Подсветка', 'Backlight', 8),
    ('watch-features', 'Писалка (стилус)', 'Stylus', 9),
    ('ups-power', 'До 800 VA', 'Up to 800 VA', 1), ('ups-power', '801 – 1500 VA', '801 – 1500 VA', 2),
    ('ups-power', '1501 – 3000 VA', '1501 – 3000 VA', 3), ('ups-power', 'Над 3000 VA', 'Over 3000 VA', 4),
    ('ups-topology', 'Off-line', 'Off-line', 1), ('ups-topology', 'Line-interactive', 'Line-interactive', 2),
    ('ups-topology', 'On-line', 'On-line', 3),
    ('print-tech', 'Лазерно', 'Laser', 1), ('print-tech', 'Мастилено', 'Inkjet', 2),
    ('print-tech', 'Мастилено с резервоари', 'Ink tank', 3),
    ('print-colour', 'Цветен', 'Colour', 1), ('print-colour', 'Черно-бял', 'Monochrome', 2),
    ('mfp-functions', 'Факс', 'Fax', 1), ('mfp-functions', 'Wi-Fi', 'Wi-Fi', 2),
    ('mfp-functions', 'ADF (автоподаване)', 'ADF', 3), ('mfp-functions', 'Двустранен печат/сканиране', 'Duplex', 4),
    ('paper-size', 'A4', 'A4', 1), ('paper-size', 'A3', 'A3', 2),
    ('vibration', 'Да', 'Yes', 1), ('vibration', 'Не', 'No', 2),
    ('wheel-type', 'Волан', 'Wheel', 1), ('wheel-type', 'Педали', 'Pedals', 2), ('wheel-type', 'Основа (direct drive)', 'Wheel base', 3),
    ('drive-form-factor', 'M.2', 'M.2', 3),
    ('supported-cards', 'SD', 'SD', 1), ('supported-cards', 'microSD', 'microSD', 2),
    ('supported-cards', 'CompactFlash', 'CompactFlash', 3), ('supported-cards', 'Memory Stick', 'Memory Stick', 4),
    ('supported-cards', 'xD', 'xD', 5), ('supported-cards', 'Смарт карта (eID, SIM)', 'Smart card (eID, SIM)', 6),
    ('filament-type', 'PLA', 'PLA', 1), ('filament-type', 'ABS', 'ABS', 2), ('filament-type', 'PETG', 'PETG', 3),
    ('filament-type', 'TPU', 'TPU', 4),
    ('filament-for', '3D принтер', '3D printer', 1), ('filament-for', '3D писалка', '3D pen', 2)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 12'
FROM (VALUES
    ('tv-resolution', '8K',              '(8k|7680)'),
    ('tv-resolution', '4K Ultra HD',     '^(?!.*(8k|7680)).*(4k|uhd|3840|2160)'),
    ('tv-resolution', 'Full HD (1080p)', '^(?!.*(4k|uhd|3840|2160|8k)).*(full ?-?hd|fhd|1920|1080)'),
    ('tv-resolution', 'HD Ready (720p)', '^(?!.*(full|fhd|1920|1080|4k|uhd|3840|2160|8k)).*((^|[^[:alnum:]])hd([^[:alnum:]]|$)|hd ready|1366|1280|720)'),
    ('tv-panel', 'OLED',     '(^|[^[:alnum:]q])oled'),
    ('tv-panel', 'Mini LED', 'mini ?-?led'),
    ('tv-panel', 'QLED',     '^(?!.*mini ?-?led).*(qled|quantum)'),
    ('tv-panel', 'LED',      '^(?!.*(qled|oled|mini ?-?led|quantum)).*led'),
    ('smart-tv', 'Не', 'non ?-?smart'),
    ('smart-tv', 'Да', '^(?!.*non ?-?smart).*(smart|google tv|android tv|webos|tizen|vidaa)'),
    ('phone-type', 'Смартфон',          '(android|ios|harmony|smartphone|смартфон)'),
    ('phone-type', 'Телефон с копчета', '(s30|kaios|feature phone)'),
    ('network-gen', '5G',       '(^|[^[:alnum:]])5g([^[:alnum:]]|$)'),
    ('network-gen', '4G (LTE)', '((^|[^[:alnum:]])4g([^[:alnum:]]|$)|lte|volte)'),
    ('network-gen', '2G / 3G',  '^(?!.*(4g|lte|5g)).*(gsm|wcdma|3g|2g)'),
    ('dual-sim', 'Да', '(dual|4ff ?\+ ?4ff)'),
    ('tablet-storage', '16 GB',  '(^|[^0-9])16 ?gb?([^[:alnum:]]|$)'),
    ('tablet-storage', '32 GB',  '(^|[^0-9])32 ?gb?([^[:alnum:]]|$)'),
    ('tablet-storage', '64 GB',  '(^|[^0-9])64 ?gb?([^[:alnum:]]|$)'),
    ('tablet-storage', '128 GB', '(^|[^0-9])128 ?gb?([^[:alnum:]]|$)'),
    ('tablet-storage', '256 GB', '(^|[^0-9])256 ?gb?([^[:alnum:]]|$)'),
    ('cellular', 'Да', '(cellular|(^|[^[:alnum:]])(4g|lte)([^[:alnum:]]|$))'),
    ('cellular', 'Не', '^wi-?fi$'),
    ('watch-features', 'Безжично зареждане', '(безжично зареждане|wireless charg)'),
    ('watch-features', 'Подсветка',          '(подсветка|backlight|frontlight)'),
    ('watch-features', 'Писалка (стилус)',   '(стилус|stylus|писалка|(^|[^[:alnum:]])pen([^[:alnum:]]|$)|marker)'),
    ('ups-power', 'До 800 VA',      '((^|[^0-9.])([1-7][0-9]{2}|800) ?va|(^|[^0-9.])0[.,][5-8] ?kva)'),
    ('ups-power', '801 – 1500 VA',  '((^|[^0-9.])(80[1-9]|8[1-9][0-9]|9[0-9]{2}|1[0-4][0-9]{2}|1500) ?va|(^|[^0-9.])1([.,][0-5])? ?k(va)?([^[:alnum:]]|$))'),
    ('ups-power', '1501 – 3000 VA', '((^|[^0-9.])(150[1-9]|15[1-9][0-9]|1[6-9][0-9]{2}|2[0-9]{3}|3000) ?va|(^|[^0-9.])[23]([.,][0-9])? ?k(va)?([^[:alnum:]]|$))'),
    ('ups-power', 'Над 3000 VA',    '((^|[^0-9.])(300[1-9]|30[1-9][0-9]|3[1-9][0-9]{2}|[4-9][0-9]{3}|[1-9][0-9]{4}) ?va|(^|[^0-9.])([4-9]|[1-9][0-9])([.,][0-9])? ?k(va)?([^[:alnum:]]|$))'),
    ('ups-topology', 'On-line',          '(^|[^[:alpha:]-])on-?line'),
    ('ups-topology', 'Line-interactive', 'line[- ]?interactive'),
    ('ups-topology', 'Off-line',         '(off-?line|standby)'),
    ('print-tech', 'Мастилено с резервоари', '(ecotank|smart tank|megatank|резервоар|pixma g[0-9]|maxify gx|(^|[^[:alnum:]])gi-[0-9])'),
    ('print-tech', 'Лазерно',                '(laser|лазер|toner|тонер|(^|[^[:alnum:]])mf[- ]?[0-9]|i-sensys|crg|drum|ricoh|kyocera|bizhub|xerox|lexmark)'),
    ('print-tech', 'Мастилено',              '^(?!.*(ecotank|smart tank|megatank|резервоар|pixma g[0-9]|maxify gx|gi-[0-9])).*(pixma|deskjet|inkjet|мастил|officejet|pgi|cli-|(^|[^[:alpha:]])ink([^[:alpha:]]|$))'),
    ('print-colour', 'Черно-бял', '(монохром|mono|черно-бял)'),
    ('print-colour', 'Цветен',    '^(?!.*(монохром|mono|черно-бял)).*(цветн|colou?r|pixma|ecotank|deskjet|(^|[^[:alnum:]])mf[- ]?[0-9]{3}c|[0-9]c(dn|dw|w|x|nw)([^[:alnum:]]|$))'),
    ('mfp-functions', 'Факс',                       '(fax|факс)'),
    ('mfp-functions', 'Wi-Fi',                      '(wi-?fi|wireless)'),
    ('mfp-functions', 'ADF (автоподаване)',         '(adf|ardf|spdf|автоподав)'),
    ('mfp-functions', 'Двустранен печат/сканиране', '(двустран|duplex|ardf|spdf)'),
    ('paper-size', 'A3', '(^|[^[:alnum:]])a3([^[:alnum:]]|$)'),
    ('paper-size', 'A4', '^(?!.*(^|[^[:alnum:]])a3([^[:alnum:]]|$)).*(^|[^[:alnum:]])a4([^[:alnum:]]|$)'),
    ('vibration', 'Да', '^(да|yes)$'),
    ('vibration', 'Не', '^(не|no)$'),
    ('wheel-type', 'Педали',                '(педал|pedal)'),
    ('wheel-type', 'Основа (direct drive)', '(основа|wheel ?base|direct drive)'),
    ('wheel-type', 'Волан',                 '^(?!.*(педал|pedal)).*(волан|wheel)'),
    ('drive-form-factor', 'M.2', '(m\.2|nvme)'),
    ('supported-cards', 'microSD',                '(micro ?-?sd|(^|[^[:alnum:]])tf([^[:alnum:]]|$))'),
    ('supported-cards', 'SD',                     '(^|[^[:alnum:]o])(sd(hc|xc)?|mmc)([^[:alnum:]]|$)'),
    ('supported-cards', 'CompactFlash',           '(compact ?flash|(^|[^[:alnum:]])cf([^[:alnum:]]|$))'),
    ('supported-cards', 'Memory Stick',           '(memory stick|(^|[^[:alnum:]])ms([^[:alnum:]]|$))'),
    ('supported-cards', 'xD',                     '(^|[^[:alnum:]])xd'),
    ('supported-cards', 'Смарт карта (eID, SIM)', '(smart card|eid|sim card|смарт карт)'),
    ('filament-type', 'PLA',  '(^|[^[:alnum:]])(tough )?pla([^[:alnum:]]|$)'),
    ('filament-type', 'ABS',  '(^|[^[:alnum:]])abs([^[:alnum:]]|$)'),
    ('filament-type', 'PETG', 'petg'),
    ('filament-type', 'TPU',  '(^|[^[:alnum:]])tpu([^[:alnum:]]|$)'),
    ('filament-for', '3D писалка', '(писалка|(^|[^[:alnum:]])pen([^[:alnum:]]|$))'),
    ('filament-for', '3D принтер', '(принтер|printer)')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
-- Правилата на тази партида важат и за имената (без „Вибрация“ и „Не“ към цяла стойност).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, c.category_id, r.pattern, r.value_id, '55 партида 12'
FROM filter_value_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
JOIN (VALUES ('tv-resolution', 139::BIGINT), ('tv-panel', 139), ('smart-tv', 139), ('phone-type', 154),
             ('network-gen', 154), ('dual-sim', 154), ('tablet-storage', 744), ('tablet-storage', 124),
             ('ups-power', 73), ('ups-power', 74), ('ups-topology', 73), ('ups-topology', 74), ('print-tech', 80),
             ('print-colour', 80), ('mfp-functions', 80), ('paper-size', 80), ('wheel-type', 175),
             ('drive-form-factor', 18), ('supported-cards', 63), ('filament-type', 95), ('filament-for', 95))
     AS c(slug, category_id) ON c.slug = a.slug
WHERE r.note = '55 партида 12';

-- Готови правила по име от други категории: платформа (геймърски слушалки), USB стандарт (външни
-- дискове), конектори (кабели за мобилни), размер на таблета (калъфи за таблети), размер на диска (външни дискове).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, use_parser, note)
SELECT r.attribute_id, c.target, r.pattern, r.value_id, r.use_parser, '55 партида 12'
FROM filter_name_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
JOIN (VALUES ('gaming-platform', 171::BIGINT, 177::BIGINT), ('gaming-platform', 171, 175),
             ('usb-standard', 55, 18), ('drive-form-factor', 55, 18), ('cable-connector', 129, 18),
             ('cable-connector', 129, 63), ('tablet-size', 41, 744)) AS c(slug, source, target)
  ON c.slug = a.slug AND r.category_id = c.source
WHERE r.note LIKE '55 партида %';

INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 12'
FROM (VALUES
    ('phone-type', 154::BIGINT, 'Флип (сгъваем)',               '(flip|сгъваем)'),
    ('phone-type', 154, 'За възрастни (големи бутони)', '(big buttons|големи бутони|(^|[^[:alnum:]])sos([^[:alnum:]]|$)|senior)'),
    ('phone-type', 154, 'Смартфон',                     '(zenfone|galaxy|redmi|iphone|(^|[^[:alnum:]])5g([^[:alnum:]]|$))'),
    ('phone-type', 154, 'Телефон с копчета',            '((^|[^[:alnum:]])(105|110|130|150|220|225|2660|3310|8210|6310)([^[:alnum:]]|$)|music)'),
    ('dual-sim', 154, 'Да', '((^|[^[:alnum:]])ds([^[:alnum:]]|$)|dual ?sim)'),
    ('cellular', 744, 'Да', '((^|[^[:alnum:]])(4g|lte)([^[:alnum:]]|$)|cellular)'),
    ('for-kids', 744, 'Да', '(kids|детск)'),
    ('watch-features', 124, 'Водоустойчив',     '(ipx[78]|водоустойчив|waterproof)'),
    ('watch-features', 124, 'Писалка (стилус)', '(стилус|stylus|(^|[^[:alnum:]])pen([^[:alnum:]]|$)|marker)'),
    ('mfp-functions', 80, 'Wi-Fi',                      '([0-9]c?dw|[0-9]c?w([^[:alnum:]]|$)|wi-?fi|wireless)'),
    ('mfp-functions', 80, 'Двустранен печат/сканиране', '[0-9]c?d(w|n)([^[:alnum:]]|$)'),
    ('gaming-platform', 177, 'PC', '(^|[^[:alnum:]])pc([^[:alnum:]]|$)'),
    ('gaming-platform', 175, 'PC', '(^|[^[:alnum:]])pc([^[:alnum:]]|$)'),
    ('connection', 177, 'Безжична', '(безжич|wireless|2\.4 ?g)'),
    ('connection', 177, 'Кабелна',  '((?<!без)жичен|(^|[^[:alpha:]])wired|кабел)'),
    ('connection', 177, 'Bluetooth', 'bluetooth')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Числа от името: диагонал („50"“, „6.8"“), опресняване („120Hz“).
INSERT INTO filter_name_rules (attribute_id, category_id, use_parser, note)
SELECT a.id, r.category_id, TRUE, '55 партида 12'
FROM (VALUES ('screen-diagonal', 139::BIGINT), ('screen-diagonal', 124), ('refresh-rate', 139)) AS r(slug, category_id)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL';

-- Цвят по името.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, c.category_id, '(^|[^[:alpha:]])(' || r.words || ')([^[:alpha:]]|$)', v.id, '55 партида 12'
FROM (VALUES
    ('Черен', 'black|черен|черн[аио]'), ('Бял', 'white|бял|бял[аоие]'), ('Сив', 'gr[ae]y|сив|сив[аои]|graphite'),
    ('Сребрист', 'silver|сребрист|сребрист[аои]'), ('Червен', 'red|червен|червен[аои]'),
    ('Син', 'blue|син|синя|синьо'), ('Зелен', 'green|зелен|зелен[аои]'), ('Розов', 'pink|розов|розов[аои]'),
    ('Лилав', 'purple|violet|лилав|лилав[аои]'), ('Оранжев', 'orange|оранжев|оранжев[аои]')
) AS r(value, words)
JOIN filter_attributes a ON a.slug = 'colour' AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value)
CROSS JOIN (VALUES (154::BIGINT), (744), (124), (177), (95)) AS c(category_id);

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (139, 'MANUAL'), (154, 'MANUAL'), (744, 'MANUAL'), (124, 'MANUAL'), (73, 'MANUAL'), (74, 'MANUAL'),
       (80, 'MANUAL'), (177, 'MANUAL'), (175, 'MANUAL'), (18, 'MANUAL'), (63, 'MANUAL'), (95, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (139::BIGINT, 'screen-diagonal', 10), (139, 'tv-resolution', 20), (139, 'tv-panel', 30), (139, 'refresh-rate', 40),
    (139, 'smart-tv', 50),
    (154, 'phone-type', 10), (154, 'network-gen', 20), (154, 'dual-sim', 30), (154, 'colour', 40),
    (744, 'tablet-size', 10), (744, 'tablet-storage', 20), (744, 'cellular', 30), (744, 'for-kids', 40), (744, 'colour', 50),
    (124, 'screen-diagonal', 10), (124, 'tablet-storage', 20), (124, 'watch-features', 30), (124, 'colour', 40),
    (73, 'ups-power', 10), (73, 'ups-topology', 20),
    (74, 'ups-power', 10), (74, 'ups-topology', 20),
    (80, 'print-tech', 10), (80, 'print-colour', 20), (80, 'mfp-functions', 30), (80, 'paper-size', 40),
    (177, 'gaming-platform', 10), (177, 'connection', 20), (177, 'vibration', 30), (177, 'colour', 40),
    (175, 'wheel-type', 10), (175, 'gaming-platform', 20),
    (18, 'drive-form-factor', 10), (18, 'usb-standard', 20), (18, 'cable-connector', 30),
    (63, 'supported-cards', 10), (63, 'cable-connector', 20),
    (95, 'filament-type', 10), (95, 'filament-for', 20), (95, 'colour', 30)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

-- Невалиден регулярен израз спира целия rebuild (и нощния) — всеки израз се компилира тук, преди COMMIT.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT pattern FROM filter_value_rules WHERE note = '55 партида 12' AND pattern IS NOT NULL
             UNION ALL
             SELECT pattern FROM filter_name_rules WHERE note = '55 партида 12' AND pattern IS NOT NULL
    LOOP
        PERFORM '' ~* r.pattern;
    END LOOP;
END $$;

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (139, 154, 744, 124, 73, 74, 80, 177, 175, 18, 63, 95) AND origin = 'MANUAL';
    IF n <> 40 THEN
        RAISE EXCEPTION 'Очаквах 40 групи в дванадесетте категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 12 записана: % групи в 12 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (139, 154, 744, 124, 73, 74, 80, 177, 175, 18, 63, 95) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
