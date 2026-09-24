-- ============================================================================
-- 55_filters_curated_07_networking.sql
--
-- Курация на каноничния филтърен слой, партида 7 — мрежово оборудване:
--   107 Безжични рутери        102 Access Point           109 Суичове - неуправляеми
--   110 Суичове - управляеми    111 Мрежови карти          135 Мрежови кабели
--
-- ИЗИСКВА: V38–V40, скрипт 55_..._01 (ползва „Цвят“) и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- Wi-Fi стандартът е едно поколение (парсер WIFI_GEN): „802.11 a/b/g/n/ac/ax“ е Wi-Fi 6, не четири
--   стойности; от имената се чете класът („AX3000“, „BE3600“).
-- Честотните ленти са отделни стойности (2.4 / 5 / 6 GHz), не „двубандов“: VALI записва всяка лента
--   като отделна опция, затова броят им не се вижда от една опция.
-- Броят портове на суич е отделен парсер (PORT_COUNT): COUNT би прочел „1100“ от „GS1100-16“.
-- Суровият параметър захранва само едно свойство в категория: „Стандарти“ на суичовете дава скоростта
--   на портовете; PoE идва от „PoE“, „Мрежови характеристики“ и от името.
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 7') и MANUAL групите на шестте
--   категории, после ги вмъква наново.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'colour' AND origin = 'MANUAL') THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_01.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((107, 'Безжични рутери'), (102, 'Access Point'), (109, 'Суичове - неуправляеми'),
                                (110, 'Суичове - управляеми'), (111, 'Мрежови карти'),
                                (135, 'Мрежови кабели'))) <> 6 THEN
        RAISE EXCEPTION 'Някоя от категориите 107, 102, 109, 110, 111, 135 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 7';
DELETE FROM filter_value_rules WHERE note = '55 партида 7';
DELETE FROM filter_name_rules WHERE note = '55 партида 7';
DELETE FROM category_filters WHERE category_id IN (107, 102, 109, 110, 111, 135) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('network-device-type', 'Вид устройство',           'Device type',        'ENUM',    NULL, NULL,         FALSE, 10, 'MANUAL'),
    ('wifi-standard',       'Wi-Fi стандарт',           'Wi-Fi standard',     'NUMERIC', NULL, 'WIFI_GEN',   FALSE, 20, 'MANUAL'),
    ('wifi-bands',          'Честотни ленти',           'Frequency bands',    'ENUM',    NULL, NULL,         FALSE, 30, 'MANUAL'),
    ('port-speed',          'Скорост на портовете',     'Port speed',         'ENUM',    NULL, NULL,         FALSE, 40, 'MANUAL'),
    ('cellular',            '4G/5G (SIM карта)',        '4G/5G (SIM card)',   'ENUM',    NULL, NULL,         FALSE, 50, 'MANUAL'),
    ('poe',                 'PoE',                      'PoE',                'ENUM',    NULL, NULL,         FALSE, 50, 'MANUAL'),
    ('outdoor',             'За външен монтаж',         'Outdoor',            'ENUM',    NULL, NULL,         FALSE, 60, 'MANUAL'),
    ('port-count',          'Брой портове',             'Number of ports',    'NUMERIC', NULL, 'PORT_COUNT', FALSE, 10, 'MANUAL'),
    ('rack-mount',          'Монтаж в шкаф (rack)',     'Rack mount',         'ENUM',    NULL, NULL,         FALSE, 60, 'MANUAL'),
    ('sfp',                 'SFP порт (оптика)',        'SFP port',           'ENUM',    NULL, NULL,         FALSE, 70, 'MANUAL'),
    ('nic-type',            'Вид връзка',               'Connection type',    'ENUM',    NULL, NULL,         FALSE, 10, 'MANUAL'),
    ('pc-interface',        'Интерфейс към компютъра',  'Host interface',     'ENUM',    NULL, NULL,         FALSE, 20, 'MANUAL'),
    ('cable-category',      'Категория',                'Category',           'ENUM',    NULL, NULL,         FALSE, 10, 'MANUAL'),
    ('cable-shielding',     'Екраниране',               'Shielding',          'ENUM',    NULL, NULL,         FALSE, 20, 'MANUAL'),
    ('cable-length',        'Дължина',                  'Length',             'ENUM',    NULL, NULL,         FALSE, 30, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('network-device-type', 'wifi-standard', 'wifi-bands', 'port-speed', 'cellular', 'poe', 'outdoor',
                   'port-count', 'rack-mount', 'sfp', 'nic-type', 'pc-interface', 'cable-category',
                   'cable-shielding', 'cable-length')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 7'
FROM (VALUES
    ('network-device-type', 'Вид продукт',                            107::BIGINT),
    ('wifi-standard',       'Стандарт Wi-Fi',                         NULL),
    ('wifi-standard',       'Стандарти',                              107),
    ('wifi-standard',       'Wireless',                               107),
    ('wifi-standard',       'Съвместимост с мрежови стандарти',       102),
    ('wifi-bands',          'Честота',                                107),
    ('wifi-bands',          'Frequency',                              107),
    ('wifi-bands',          'Честота (GHz)',                          102),
    ('wifi-bands',          'Честотна лента на Wi-Fi',                102),
    ('wifi-bands',          'Честота',                                111),
    ('port-speed',          'Скорост на прехвърляне Ethernet',        107),
    ('port-speed',          'Портове LAN',                            107),
    ('port-speed',          'Портове WAN',                            107),
    ('port-speed',          'Портове',                                102),
    ('port-speed',          'Поддържани протоколи за обмен на данни', 102),
    ('port-speed',          'Стандарти',                              109),
    ('port-speed',          'Стандарти',                              110),
    ('port-speed',          'Скорост на данните',                     109),
    ('port-speed',          'Скорост на трансфер (MB/s)',             111),
    ('cellular',            '3G/4G',                                  107),
    ('poe',                 'PoE',                                    109),
    ('poe',                 'Мрежови характеристики',                 110),
    ('poe',                 'Мрежови характеристики',                 102),
    ('poe',                 'Захранване по Ethernet (PoE)',           102),
    ('poe',                 'Захранване',                             102),
    ('port-count',          'Портове',                                109),
    ('port-count',          'Портове',                                110),
    ('port-count',          'Брой портове',                           110),
    ('port-count',          'LAN',                                    110),
    ('rack-mount',          'Монтаж в шкаф',                          NULL),
    ('rack-mount',          'Видове монтаж',                          110),
    ('sfp',                 'SFP+',                                   110),
    ('nic-type',            'Портове',                                111),
    ('pc-interface',        'Интерфейс',                              111),
    ('cable-category',      'Категория',                              135),
    ('cable-shielding',     'Тип кабел',                              135),
    ('cable-length',        'Дължина (м)',                            135)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('network-device-type', 'Рутер', 'Router', 1), ('network-device-type', 'Mesh система', 'Mesh system', 2),
    ('network-device-type', 'Точка за достъп', 'Access point', 3),
    ('network-device-type', 'Удължител на обхвата', 'Range extender', 4),
    ('wifi-bands', '2.4 GHz', '2.4 GHz', 1), ('wifi-bands', '5 GHz', '5 GHz', 2), ('wifi-bands', '6 GHz', '6 GHz', 3),
    ('port-speed', '100 Mbps', '100 Mbps', 1), ('port-speed', '1 Gbps', '1 Gbps', 2),
    ('port-speed', '2.5 Gbps', '2.5 Gbps', 3), ('port-speed', '10 Gbps', '10 Gbps', 4),
    ('cellular', 'Да', 'Yes', 1), ('cellular', 'Не', 'No', 2),
    ('poe', 'Да', 'Yes', 1),
    ('outdoor', 'Да', 'Yes', 1),
    ('rack-mount', 'Да', 'Yes', 1), ('rack-mount', 'Не', 'No', 2),
    ('sfp', 'Да', 'Yes', 1),
    ('nic-type', 'Ethernet (кабел)', 'Ethernet', 1), ('nic-type', 'Wi-Fi', 'Wi-Fi', 2),
    ('nic-type', 'Bluetooth', 'Bluetooth', 3),
    ('pc-interface', 'PCIe', 'PCIe', 1), ('pc-interface', 'USB', 'USB', 2), ('pc-interface', 'M.2', 'M.2', 3),
    ('cable-category', 'Cat 5e', 'Cat 5e', 1), ('cable-category', 'Cat 6', 'Cat 6', 2),
    ('cable-category', 'Cat 6a', 'Cat 6a', 3), ('cable-category', 'Cat 7', 'Cat 7', 4),
    ('cable-category', 'Cat 8', 'Cat 8', 5),
    ('cable-shielding', 'Неекраниран (UTP)', 'Unshielded (UTP)', 1),
    ('cable-shielding', 'Екраниран (FTP/STP)', 'Shielded (FTP/STP)', 2),
    ('cable-length', 'До 1 м', 'Up to 1 m', 1), ('cable-length', '1.5 – 3 м', '1.5 – 3 m', 2),
    ('cable-length', '4 – 10 м', '4 – 10 m', 3), ('cable-length', 'Над 10 м', 'Over 10 m', 4)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
-- Скорост на портовете: всяка скорост, която устройството има („10 Mbps/ 100 Mbps/ 1 Gbps/ 2.5 Gbps“ е
-- 1 и 2.5 Gbps); 100 Mbps само когато няма гигабит. „2.5 Gigabit“ не е и 1 Gbps. „802.3u“, „100Base-TX“ и
-- „Fast Ethernet“ не значат 100 Mbps: списъците със стандарти на гигабитовите суичове ги съдържат.
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 7'
FROM (VALUES
    ('network-device-type', 'Mesh система', '(мрежест|mesh)'),
    ('network-device-type', 'Рутер',        '^рутер$'),
    ('wifi-bands', '2.4 GHz', '2[.,]4'),
    ('wifi-bands', '5 GHz',   '((^|[^0-9.])5([.,][0-9]+)? ?(ghz|g(?![a-z]))|/ ?5( |/|$)|;5)'),
    ('wifi-bands', '6 GHz',   '((^|[^0-9.])6 ?(ghz|g(?![a-z]))|/ ?6( |/|$))'),
    ('port-speed', '100 Mbps', '^(?!.*(1000|giga|гигабит|2[.,]5|10 ?g|802\.3ab|1000base)).*(10/100|(^|[^0-9.])100 ?(mb|m(?![a-z])))'),
    ('port-speed', '1 Gbps',   '(10/100/1000|(^|[^0-9.])1000 ?(mb|m(?![a-z])|base)|(?<![0-9.,] )(?<![0-9.,])(?<![0-9.,]-)gigabit|гигабит|(^|[^0-9.])1 ?(gbps|gb/s|gbit)|802\.3ab|(^|[^[:alnum:]])ge([^[:alnum:]]|$))'),
    ('port-speed', '2.5 Gbps', '(2[.,]5 ?(g|gb|gbps|gbe|gigabit)(?![0-9])|2500 ?(mb|base|m(?![a-z]))|802\.3bz)'),
    ('port-speed', '10 Gbps',  '((^|[^0-9.,])10 ?(gbps|gb/s|gbe|gbit|gigabit|g)(?![0-9a-z])|10g ?base|10000|sfp\+|802\.3ae)'),
    ('cellular', 'Да', '^да$'),
    ('cellular', 'Не', '^не$'),
    ('poe', 'Да', '(^да$|poe|power over ethernet|802\.3a[ft]|802\.3bt|[0-9] ?-? ?57 ?v)'),
    ('rack-mount', 'Да', '(^да$|rack)'),
    ('rack-mount', 'Не', '(^не$|^десктоп)'),
    ('sfp', 'Да', '^[1-9][0-9]*$'),
    ('nic-type', 'Ethernet (кабел)', '(rj-?45|sfp|(^|[^[:alnum:]])lan([^[:alnum:]]|$)|ethernet)'),
    ('nic-type', 'Wi-Fi',            '(802\.11|wi-?fi|wireless)'),
    ('nic-type', 'Bluetooth',        'bluetooth'),
    ('pc-interface', 'PCIe', '(pci ?-?e|pci express|pci-ex)'),
    ('pc-interface', 'USB',  'usb'),
    ('pc-interface', 'M.2',  '(m\.2|2230|ngff)'),
    ('cable-category', 'Cat 5e', 'cat[- .]?5[eе]?([^0-9]|$)'),
    ('cable-category', 'Cat 6',  'cat[- .]?6(?![aа0-9])'),
    ('cable-category', 'Cat 6a', 'cat[- .]?6[aа]'),
    ('cable-category', 'Cat 7',  'cat[- .]?7'),
    ('cable-category', 'Cat 8',  'cat[- .]?8'),
    ('cable-shielding', 'Екраниран (FTP/STP)', '(ftp|stp|екраниран|shielded|(^|[^[:alnum:]])[fs]/)'),
    ('cable-shielding', 'Неекраниран (UTP)',   '^(?!.*(ftp|stp|екраниран|shielded|(^|[^[:alnum:]])[fs]/)).*utp'),
    ('cable-length', 'До 1 м',    '^(0[.,][0-9]+|1([.,]0+)?) ?(м|m)'),
    ('cable-length', '1.5 – 3 м', '^(1[.,][1-9][0-9]*|2|3)([.,][0-9]+)? ?(м|m)'),
    ('cable-length', '4 – 10 м',  '^([4-9]|10)([.,][0-9]+)? ?(м|m)'),
    ('cable-length', 'Над 10 м',  '^(1[1-9]|[2-9][0-9]|[1-9][0-9]{2})([.,][0-9]+)? ?(м|m)')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
-- Скорост на портовете, категория и екраниране на кабела: същите изрази като за параметрите.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, c.category_id, r.pattern, r.value_id, '55 партида 7'
FROM filter_value_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
JOIN (VALUES ('port-speed', 107::BIGINT), ('port-speed', 102), ('port-speed', 109), ('port-speed', 110),
             ('port-speed', 111), ('cable-category', 135), ('cable-shielding', 135), ('pc-interface', 111))
     AS c(slug, category_id) ON c.slug = a.slug
WHERE r.note = '55 партида 7';

INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 7'
FROM (VALUES
    ('network-device-type', 107::BIGINT, 'Mesh система',        '(mesh|меш|deco|(^|[^[:alnum:]])[23][- ]?pack|[23] бро[йя])'),
    ('network-device-type', 102, 'Mesh система',         '(mesh|меш|deco|(^|[^[:alnum:]])[23][- ]?pack|[23] бро[йя])'),
    ('network-device-type', 107, 'Рутер',                '(^|[^[:alnum:]])(рутер|router)([^[:alnum:]]|$)'),
    ('network-device-type', 102, 'Рутер',                '(^|[^[:alnum:]])(рутер|router)([^[:alnum:]]|$)'),
    ('network-device-type', 102, 'Точка за достъп',      '(access point|точка за достъп|(^|[^[:alnum:]])ap[0-9]|unifi u[0-9])'),
    ('network-device-type', 102, 'Удължител на обхвата', '(extender|repeater|удължител|усилвател)'),
    ('network-device-type', 107, 'Удължител на обхвата', '(extender|repeater|удължител|усилвател)'),
    ('wifi-bands', 107, '2.4 GHz', '(2[.,]4 ?(ghz|/)|dual[- ]?band|двубанд|tri-?band|3-бандов|трибанд)'),
    ('wifi-bands', 102, '2.4 GHz', '(2[.,]4 ?(ghz|/)|dual[- ]?band|двубанд|tri-?band|3-бандов|трибанд)'),
    ('wifi-bands', 111, '2.4 GHz', '(2[.,]4 ?(ghz|/)|dual[- ]?band|двубанд|tri-?band|3-бандов|трибанд)'),
    ('wifi-bands', 107, '5 GHz',   '((^|[^0-9.])5 ?ghz|2[.,]4 ?/ ?5|dual[- ]?band|двубанд|tri-?band|3-бандов|трибанд)'),
    ('wifi-bands', 102, '5 GHz',   '((^|[^0-9.])5 ?ghz|2[.,]4 ?/ ?5|dual[- ]?band|двубанд|tri-?band|3-бандов|трибанд)'),
    ('wifi-bands', 111, '5 GHz',   '((^|[^0-9.])5 ?ghz|2[.,]4 ?/ ?5|dual[- ]?band|двубанд|tri-?band|3-бандов|трибанд)'),
    ('wifi-bands', 107, '6 GHz',   '((^|[^0-9.])6 ?ghz|/ ?6 ?ghz|wi-?fi ?(6e|7)|(^|[^[:alnum:]])be[0-9]{4,5})'),
    ('wifi-bands', 102, '6 GHz',   '((^|[^0-9.])6 ?ghz|/ ?6 ?ghz|wi-?fi ?(6e|7)|(^|[^[:alnum:]])be[0-9]{4,5})'),
    ('wifi-bands', 111, '6 GHz',   '((^|[^0-9.])6 ?ghz|/ ?6 ?ghz|wi-?fi ?(6e|7)|(^|[^[:alnum:]])be[0-9]{4,5})'),
    ('cellular', 107, 'Да', '((^|[^[:alnum:]])[45]g([^[:alnum:]]|$)|lte|sim)'),
    ('poe', 102, 'Да', '(poe|power over ethernet)'),
    ('poe', 109, 'Да', '(poe|power over ethernet)'),
    ('poe', 110, 'Да', '(poe|power over ethernet)'),
    ('outdoor', 102, 'Да', '(outdoor|външен|external|ip6[5-8])'),
    ('rack-mount', 109, 'Да', '(rack|шкаф|19")'),
    ('rack-mount', 110, 'Да', '(rack|шкаф|19")'),
    ('rack-mount', 109, 'Не', 'desktop'),
    ('rack-mount', 110, 'Не', 'desktop'),
    ('sfp', 109, 'Да', 'sfp'),
    ('sfp', 110, 'Да', 'sfp'),
    ('nic-type', 111, 'Wi-Fi',            '(wi-?fi|wireless|безжичн|802\.11|(^|[^[:alnum:]])(ax|be|ac)[0-9]{3,4})'),
    ('nic-type', 111, 'Ethernet (кабел)', '(rj-?45|gigabit|ethernet|10/100|(^|[^[:alnum:]])lan([^[:alnum:]]|$)|2[.,]5 ?g|10 ?gbe)'),
    ('nic-type', 111, 'Bluetooth',        '(bluetooth|\+ ?bt)'),
    ('cable-length', 135, 'До 1 м',    '(^|[^0-9.,])(0[.,][0-9]+|1([.,]0+)?) ?(m|м)([^[:alnum:]]|$)'),
    ('cable-length', 135, '1.5 – 3 м', '(^|[^0-9.,])(1[.,][1-9][0-9]*|2|3)([.,][0-9]+)? ?(m|м)([^[:alnum:]]|$)'),
    ('cable-length', 135, '4 – 10 м',  '(^|[^0-9.,])([4-9]|10)([.,][0-9]+)? ?(m|м)([^[:alnum:]]|$)'),
    ('cable-length', 135, 'Над 10 м',  '(^|[^0-9.,])(1[1-9]|[2-9][0-9]|[1-9][0-9]{2})([.,][0-9]+)? ?(m|м)([^[:alnum:]]|$)')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Цвят на кабела по името („… Медни проводници, Зелен“).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, 135, '(^|[^[:alpha:]])(' || r.words || ')([^[:alpha:]]|$)', v.id, '55 партида 7'
FROM (VALUES
    ('Черен', 'black|черен|черн[аио]'), ('Бял', 'white|бял|бял[аоие]'), ('Сив', 'gr[ae]y|сив|сив[аои]'),
    ('Червен', 'red|червен|червен[аои]'), ('Син', 'blue|син|синя|синьо'), ('Жълт', 'yellow|жълт|жълт[аои]'),
    ('Зелен', 'green|зелен|зелен[аои]'), ('Оранжев', 'orange|оранжев|оранжев[аои]')
) AS r(value, words)
JOIN filter_attributes a ON a.slug = 'colour' AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Числа от името: Wi-Fi поколение („AX3000“, „WiFi 7“), брой портове („16 портов“, „8-port“).
INSERT INTO filter_name_rules (attribute_id, category_id, use_parser, note)
SELECT a.id, r.category_id, TRUE, '55 партида 7'
FROM (VALUES ('wifi-standard', 107::BIGINT), ('wifi-standard', 102), ('wifi-standard', 111),
             ('port-count', 109), ('port-count', 110)) AS r(slug, category_id)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL';

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (107, 'MANUAL'), (102, 'MANUAL'), (109, 'MANUAL'), (110, 'MANUAL'), (111, 'MANUAL'), (135, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (107::BIGINT, 'network-device-type', 10), (107, 'wifi-standard', 20), (107, 'wifi-bands', 30),
    (107, 'port-speed', 40), (107, 'cellular', 50),
    (102, 'network-device-type', 10), (102, 'wifi-standard', 20), (102, 'wifi-bands', 30), (102, 'poe', 40),
    (102, 'outdoor', 50),
    (109, 'port-count', 10), (109, 'port-speed', 20), (109, 'poe', 30), (109, 'rack-mount', 40),
    (110, 'port-count', 10), (110, 'port-speed', 20), (110, 'poe', 30), (110, 'sfp', 40), (110, 'rack-mount', 50),
    (111, 'nic-type', 10), (111, 'pc-interface', 20), (111, 'port-speed', 30), (111, 'wifi-standard', 40),
    (135, 'cable-category', 10), (135, 'cable-shielding', 20), (135, 'cable-length', 30), (135, 'colour', 40)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

-- Невалиден регулярен израз спира целия rebuild (и нощния) — всеки израз се компилира тук, преди COMMIT.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT pattern FROM filter_value_rules WHERE note = '55 партида 7' AND pattern IS NOT NULL
             UNION ALL
             SELECT pattern FROM filter_name_rules WHERE note = '55 партида 7' AND pattern IS NOT NULL
    LOOP
        PERFORM '' ~* r.pattern;
    END LOOP;
END $$;

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (107, 102, 109, 110, 111, 135) AND origin = 'MANUAL';
    IF n <> 27 THEN
        RAISE EXCEPTION 'Очаквах 27 групи в шестте категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 7 записана: % групи в 6 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (107, 102, 109, 110, 111, 135) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
