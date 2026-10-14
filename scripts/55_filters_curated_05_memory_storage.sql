-- ============================================================================
-- 55_filters_curated_05_memory_storage.sql
--
-- Курация на каноничния филтърен слой, партида 5 — памети и носители:
--   7   Памети за лаптоп        16  Хард дискове - 3.5"      235 Твърди дискове
--   55  Външни дискове          56  Външни SSD               57  USB памети
--   321 Флаш памети             150 Карти памет
--
-- ИЗИСКВА: V38–V40, скриптове 55_..._01 (ползва „Цвят“) и 55_..._02 (ползва „Капацитет“, „Тип памет“,
--          „Честота“, „Латентност“ и правилата им по име от „Памети“) и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- „Памети за лаптоп“ показваше 11 групи — по две за всяко свойство, защото VALI и MOST ги наричат
--   различно („Капацитет“/„Капацитет съхранение“, „Честота“/„Speed“, „Волтаж“/„Напрежение“).
-- „USB памети“ (VALI) и „Флаш памети“ (MOST, ASBIS) са един и същ вид продукт в две категории — тук
--   получават едни и същи филтри; сливането на категориите е отделен въпрос.
-- Скоростта на четене е в диапазони (парсер READ_MBS): „до 150“, „150MB/s“ и „Up to 150 MB/sec“ са
--   една и съща скорост, закръглена различно.
-- Един параметър захранва само едно свойство в категория: „Интерфейс 2“ на външните SSD („USB 3.2
--   Type C“) дава конектора, а USB поколението идва от „Трансфер на данни“ и от името.
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 5') и MANUAL групите на осемте
--   категории, после ги вмъква наново. Правилата по име за „Памети за лаптоп“ се копират от
--   „Памети“ — пусни скрипта отново, ако 55_02 се промени.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'colour' AND origin = 'MANUAL')
       OR NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'memory-type' AND origin = 'MANUAL') THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_01 и 55_filters_curated_02.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((7, 'Памети за лаптоп'), (16, 'Хард дискове - 3.5"'), (235, 'Твърди дискове'),
                                (55, 'Външни дискове'), (56, 'Външни SSD'), (57, 'USB памети'),
                                (321, 'Флаш памети'), (150, 'Карти памет'))) <> 8 THEN
        RAISE EXCEPTION 'Някоя от категориите 7, 16, 235, 55, 56, 57, 321, 150 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 5';
DELETE FROM filter_value_rules WHERE note = '55 партида 5';
DELETE FROM filter_name_rules WHERE note = '55 партида 5';
DELETE FROM category_filters WHERE category_id IN (7, 16, 235, 55, 56, 57, 321, 150) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('hdd-purpose',       'Предназначение',       'Intended use',     'ENUM',    NULL, NULL,       FALSE, 20, 'MANUAL'),
    ('hdd-rpm',           'Обороти',              'Spindle speed',    'ENUM',    NULL, NULL,       FALSE, 30, 'MANUAL'),
    ('drive-form-factor', 'Размер на диска',      'Drive size',       'ENUM',    NULL, NULL,       FALSE, 20, 'MANUAL'),
    ('usb-standard',      'USB стандарт',         'USB standard',     'ENUM',    NULL, NULL,       FALSE, 30, 'MANUAL'),
    ('usb-connector',     'Конектор',             'Connector',        'ENUM',    NULL, NULL,       FALSE, 40, 'MANUAL'),
    ('read-speed',        'Скорост на четене',    'Read speed',       'NUMERIC', NULL, 'READ_MBS', FALSE, 50, 'MANUAL'),
    ('card-type',         'Тип карта',            'Card type',        'ENUM',    NULL, NULL,       FALSE, 10, 'MANUAL'),
    ('card-speed-class',  'Клас на скорост',      'Speed class',      'ENUM',    NULL, NULL,       FALSE, 30, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('hdd-purpose', 'hdd-rpm', 'drive-form-factor', 'usb-standard', 'usb-connector', 'read-speed',
                   'card-type', 'card-speed-class')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
-- „Speed“ на MOST е MHz при паметите и MB/s при картите; „Скорост“ на VALI е обороти при дисковете.
-- „Тип хард диск“ на VALI умишлено НЕ е източник: IronWolf, SkyHawk и WD Gold са записани като
-- „Настолен компютър“. Предназначението идва от серията в името (т. 5).
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 5'
FROM (VALUES
    ('capacity',          'Капацитет',                                        7::BIGINT),
    ('capacity',          'Капацитет съхранение',                             7),
    ('capacity',          'Капацитет',                                        16),
    ('capacity',          'Капацитет съхранение',                             16),
    ('capacity',          'Капацитет',                                        235),
    ('capacity',          'Капацитет съхранение',                             235),
    ('capacity',          'Капацитет на устройство за съхранение на данни',   235),
    ('capacity',          'Капацитет',                                        55),
    ('capacity',          'Капацитет съхранение',                             55),
    ('capacity',          'Капацитет за съхранение',                          55),
    ('capacity',          'Капацитет',                                        56),
    ('capacity',          'Капацитет съхранение',                             56),
    ('capacity',          'Капацитет на устройство за съхранение на данни',   56),
    ('capacity',          'Капацитет',                                        57),
    ('capacity',          'Капацитет съхранение',                             321),
    ('capacity',          'Капацитет на устройство за съхранение на данни',   321),
    ('capacity',          'Капацитет (GB)',                                   150),
    ('capacity',          'Капацитет съхранение',                             150),
    ('capacity',          'Капацитет на устройство за съхранение на данни',   150),
    ('memory-type',       'Тип памет',                                        7),
    ('memory-type',       'Тип',                                              7),
    ('memory-speed',      'Честота',                                          7),
    ('memory-speed',      'Speed',                                            7),
    ('cas-latency',       'Латентност',                                       7),
    ('hdd-rpm',           'Скорост',                                          16),
    ('hdd-rpm',           'Скорост',                                          235),
    ('hdd-rpm',           'Скорост на въртене',                               16),
    ('hdd-rpm',           'Скорост на въртене',                               235),
    ('hdd-rpm',           'HDD RPM',                                          NULL),
    ('drive-form-factor', 'Форм фактор',                                      55),
    ('drive-form-factor', 'Форм фактор на вътрешния диск',                    55),
    ('drive-form-factor', 'Тип',                                              55),
    ('usb-standard',      'Интерфейс',                                        55),
    ('usb-standard',      'Интерфейси',                                       55),
    ('usb-standard',      'Външен канал за данни',                            55),
    ('usb-standard',      'Външна скорост на данните',                        55),
    ('usb-standard',      'Трансфер на данни',                                55),
    ('usb-standard',      'Интерфейс',                                        56),
    ('usb-standard',      'Поддържан канал за данни',                         56),
    ('usb-standard',      'Поддържана скорост на външен трансфер на данни',   56),
    ('usb-standard',      'Трансфер на данни',                                56),
    ('usb-standard',      'Интерфейс',                                        57),
    ('usb-standard',      'Интерфейс',                                        321),
    ('usb-standard',      'Скорост на пренос на данни',                       321),
    ('usb-connector',     'Конектор',                                         57),
    ('usb-connector',     'Конектор',                                         321),
    ('usb-connector',     'Интерфейс 2',                                      56),
    ('usb-connector',     'USB 3.2 Gen 2',                                    56),
    ('read-speed',        'Скорост при четене',                               57),
    ('read-speed',        'R/W speed',                                        321),
    ('read-speed',        'Флаш памет - скорост на четене',                   NULL),
    ('read-speed',        'Трансфер на данни - четене (MB/s)',                NULL),
    ('read-speed',        'Трансфер на данни (MB/s)',                         150),
    ('read-speed',        'Speed',                                            150),
    ('read-speed',        'Скорост на четене',                                56),
    ('read-speed',        'Производителност',                                 56),
    ('read-speed',        'Макс. скорост на последователно четене',           56),
    ('card-type',         'Тип памет',                                        150),
    ('card-type',         'Форм фактор на паметта',                           150),
    ('card-type',         'Форм фактор',                                      150),
    ('card-speed-class',  'Клас',                                             150),
    ('card-speed-class',  'Class',                                            150),
    ('card-speed-class',  'Стойност за клас скорост',                         150)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('hdd-purpose', 'Настолен компютър', 'Desktop', 1), ('hdd-purpose', 'NAS', 'NAS', 2),
    ('hdd-purpose', 'Видеонаблюдение', 'Surveillance', 3), ('hdd-purpose', 'Сървър', 'Server', 4),
    ('hdd-rpm', '5400 – 5900 rpm', '5400 – 5900 rpm', 1), ('hdd-rpm', '7200 rpm', '7200 rpm', 2),
    ('drive-form-factor', '2.5"', '2.5"', 1), ('drive-form-factor', '3.5"', '3.5"', 2),
    ('usb-standard', 'USB 2.0', 'USB 2.0', 1),
    ('usb-standard', 'USB 3.2 Gen 1 (5 Gbps)', 'USB 3.2 Gen 1 (5 Gbps)', 2),
    ('usb-standard', 'USB 3.2 Gen 2 (10 Gbps)', 'USB 3.2 Gen 2 (10 Gbps)', 3),
    ('usb-standard', 'USB 3.2 Gen 2x2 (20 Gbps)', 'USB 3.2 Gen 2x2 (20 Gbps)', 4),
    ('usb-connector', 'USB-A', 'USB-A', 1), ('usb-connector', 'USB-C', 'USB-C', 2),
    ('usb-connector', 'microUSB', 'microUSB', 3), ('usb-connector', 'Lightning', 'Lightning', 4),
    ('card-type', 'microSD', 'microSD', 1), ('card-type', 'SD', 'SD', 2),
    ('card-type', 'CFexpress', 'CFexpress', 3), ('card-type', 'CompactFlash', 'CompactFlash', 4),
    ('card-speed-class', 'Class 10', 'Class 10', 1), ('card-speed-class', 'U1', 'U1', 2),
    ('card-speed-class', 'U3', 'U3', 3), ('card-speed-class', 'V30', 'V30', 4),
    ('card-speed-class', 'V60', 'V60', 5), ('card-speed-class', 'V90', 'V90', 6),
    ('card-speed-class', 'A1', 'A1', 7), ('card-speed-class', 'A2', 'A2', 8)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
-- „USB 3.2“ без поколение е двусмислено (Samsung T7 е Gen 2, флашките са Gen 1) и не се чете;
-- „USB 3.0“ и „USB 3.1“ без поколение са Gen 1.
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 5'
FROM (VALUES
    ('hdd-rpm', '5400 – 5900 rpm', '(^|[^0-9])5[4-9][0-9]{2}( ?rpm)?([^0-9]|$)'),
    ('hdd-rpm', '7200 rpm',        '(^|[^0-9])7200( ?rpm)?([^0-9]|$)'),
    ('drive-form-factor', '2.5"', '(^|[^0-9.])2[.,]5'),
    ('drive-form-factor', '3.5"', '((^|[^0-9.])3[.,]5|desktop)'),
    ('usb-standard', 'USB 2.0',                   '^(?!.*(usb ?3|gen ?[12]|gbps)).*usb ?2'),
    ('usb-standard', 'USB 3.2 Gen 1 (5 Gbps)',    '(usb ?3(\.[01])?(?![0-9.]| ?gen ?2| ?type)|gen ?1|(^|[^0-9])5 ?gbps)'),
    ('usb-standard', 'USB 3.2 Gen 2 (10 Gbps)',   '(gen ?2(?! ?x ?2)|(^|[^0-9])10 ?gbps)'),
    ('usb-standard', 'USB 3.2 Gen 2x2 (20 Gbps)', '(2 ?x ?2|(^|[^0-9])20 ?gbps)'),
    ('usb-connector', 'USB-C',     '(type[ -]?c|тип c|usb-?c|\(c\))'),
    ('usb-connector', 'USB-A',     '(type[ -]?[aа]|тип [aа]|usb-[aа](?![[:alpha:]])|\([aа]\))'),
    ('usb-connector', 'microUSB',  '(micro ?-?usb|micro-?b)'),
    ('usb-connector', 'Lightning', 'lightning'),
    ('card-type', 'microSD',      '(micro ?-?sd|sdmic|(^|[^[:alnum:]])msd|sd(hc|xc)m)'),
    ('card-type', 'SD',           '^(?!.*(micro|sdmic|sd(hc|xc)m)).*(^|[^[:alnum:]])sd(hc|xc|uc)?([^[:alnum:]]|$)'),
    ('card-type', 'CFexpress',    'cfexpress'),
    ('card-type', 'CompactFlash', '(^cf$|compact ?flash)'),
    ('card-speed-class', 'Class 10', '(class ?10|(^|[^[:alnum:]])cl ?10|(^|[^[:alnum:]])c10([^[:alnum:]]|$))'),
    ('card-speed-class', 'U1',       '(^|[^[:alnum:]])u1([^[:alnum:]]|$)'),
    ('card-speed-class', 'U3',       '((^|[^[:alnum:]])u3([^[:alnum:]]|$)|uhs speed:? class 3|^class 3$)'),
    ('card-speed-class', 'V30',      '((^|[^[:alnum:]])v30([^[:alnum:]]|$)|video (speed )?class (10/)?30)'),
    ('card-speed-class', 'V60',      '(^|[^[:alnum:]])v60([^[:alnum:]]|$)'),
    ('card-speed-class', 'V90',      '(^|[^[:alnum:]])v90([^[:alnum:]]|$)'),
    ('card-speed-class', 'A1',       '((^|[^[:alnum:]])a1([^[:alnum:]]|$)|class a1)'),
    ('card-speed-class', 'A2',       '((^|[^[:alnum:]])a2([^[:alnum:]]|$)|class a2)')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
-- Памети за лаптоп: същите правила като „Памети“ („Памет Kingston 8GB SODIMM DDR4 3200 MHz CL22“).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, use_parser, note)
SELECT r.attribute_id, 7, r.pattern, r.value_id, r.use_parser, '55 партида 5'
FROM filter_name_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
WHERE r.category_id = 6 AND a.slug IN ('memory-type', 'capacity', 'memory-speed', 'cas-latency');

-- USB стандарт, конектор, тип и клас на картата: същите изрази като за параметрите.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, c.category_id, r.pattern, r.value_id, '55 партида 5'
FROM filter_value_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
JOIN (VALUES ('usb-standard', 55::BIGINT), ('usb-standard', 56), ('usb-standard', 57), ('usb-standard', 321),
             ('usb-connector', 56), ('usb-connector', 57), ('usb-connector', 321),
             ('card-type', 150), ('card-speed-class', 150)) AS c(slug, category_id) ON c.slug = a.slug
WHERE r.note = '55 партида 5';

INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 5'
FROM (VALUES
    -- Серията и кодът на модела казват предназначението: IronWolf/Red/ST…VN — NAS, Purple/SkyHawk/ST…VX —
    -- видеонаблюдение, Gold/Exos/Ultrastar — сървър, BarraCuda/Blue/ST…DM — настолен компютър.
    ('hdd-purpose', 16::BIGINT, 'NAS',        '(iron ?wolf|(^|[^[:alnum:]])red([^[:alnum:]]|$)|(^|[^[:alnum:]])nas([^[:alnum:]]|$)|hat[35][0-9]{3}|n300|st[0-9]+(vn|nt|ne)[0-9]|wd[0-9]+(ef|ff)[a-z]x)'),
    ('hdd-purpose', 16, 'Видеонаблюдение',    '(purple|sky ?hawk|surveillance|наблюдение|s300|st[0-9]+v[xe][0-9]|wd[0-9]+pur[a-z])'),
    ('hdd-purpose', 16, 'Сървър',             '((^|[^[:alnum:]])gold([^[:alnum:]]|$)|exos|enterprise|ultrastar|(^|[^[:alnum:]])mg[0-9]{2}|st[0-9]+nm[0-9]|wd[0-9]+[kf]ryz)'),
    ('hdd-purpose', 16, 'Настолен компютър',  '(barracuda|(^|[^[:alnum:]])blue([^[:alnum:]]|$)|desktop|p300|st[0-9]+dm[0-9]|wd[0-9]+e[az][a-z]{2}(?![a-z]))'),
    ('hdd-rpm', 16, '5400 – 5900 rpm', '5[4-9][0-9]{2} ?rpm'),
    ('hdd-rpm', 16, '7200 rpm',        '7200 ?rpm'),
    ('drive-form-factor', 55, '2.5"', '2[.,]5 ?("|in|инч|``)'),
    ('drive-form-factor', 55, '3.5"', '(3[.,]5 ?("|in|инч|``)|desktop)'),
    -- Флашка без USB-C, microUSB или Lightning в името е с USB-A („128GB USB3.2 DTX KINGSTON“).
    ('usb-connector', 57,  'USB-A', '^(?!.*(type[ -]?c|usb-?c|тип c|lightning|micro ?usb)).*usb'),
    ('usb-connector', 321, 'USB-A', '^(?!.*(type[ -]?c|usb-?c|тип c|lightning|micro ?usb)).*usb')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, 235, r.pattern, r.value_id, '55 партида 5'
FROM filter_name_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.slug IN ('hdd-purpose', 'hdd-rpm')
WHERE r.category_id = 16 AND r.note = '55 партида 5';

-- Цвят по името (MOST: „ADATA HD710P USB3.1 YL“, „TEAM S5 BLACK“). „MET“ е метален корпус, не цвят.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, c.category_id, '(^|[^[:alpha:]])(' || r.words || ')([^[:alpha:]]|$)', v.id, '55 партида 5'
FROM (VALUES
    ('Черен',    'black|черен|черн[аио]|bk'),
    ('Бял',      'white|бял|бял[аоие]|whi?'),
    ('Сив',      'gr[ae]y|сив|сив[аои]|anthracite|антрацит|graphite'),
    ('Сребрист', 'silver|сребрист|сребрист[аои]'),
    ('Златист',  'gold|златист|златист[аои]'),
    ('Червен',   'red|червен|червен[аои]'),
    ('Син',      'blue|син|синя|синьо|сини'),
    ('Жълт',     'yellow|жълт|жълт[аои]|yl'),
    ('Зелен',    'green|зелен|зелен[аои]')
) AS r(value, words)
JOIN filter_attributes a ON a.slug = 'colour' AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value)
CROSS JOIN (VALUES (55::BIGINT), (56), (57), (321)) AS c(category_id);

-- Числа от името: капацитет („128GB“, „64G“), скорост на четене („150MB/s“). Не и в „Твърди дискове“:
-- там има NAS устройства, чиито имена носят RAM паметта („QNAP TS-433-4G“).
INSERT INTO filter_name_rules (attribute_id, category_id, use_parser, note)
SELECT a.id, r.category_id, TRUE, '55 партида 5'
FROM (VALUES ('capacity', 16::BIGINT), ('capacity', 55), ('capacity', 56),
             ('capacity', 57), ('capacity', 321), ('capacity', 150),
             ('read-speed', 56), ('read-speed', 57), ('read-speed', 321), ('read-speed', 150)) AS r(slug, category_id)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL';

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (7, 'MANUAL'), (16, 'MANUAL'), (235, 'MANUAL'), (55, 'MANUAL'), (56, 'MANUAL'), (57, 'MANUAL'),
       (321, 'MANUAL'), (150, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (7::BIGINT, 'memory-type', 10), (7, 'capacity', 20), (7, 'memory-speed', 30), (7, 'cas-latency', 40),
    (16, 'capacity', 10), (16, 'hdd-purpose', 20), (16, 'hdd-rpm', 30),
    (235, 'capacity', 10), (235, 'hdd-purpose', 20), (235, 'hdd-rpm', 30),
    (55, 'capacity', 10), (55, 'drive-form-factor', 20), (55, 'usb-standard', 30), (55, 'colour', 40),
    (56, 'capacity', 10), (56, 'read-speed', 20), (56, 'usb-standard', 30), (56, 'usb-connector', 40),
    (56, 'colour', 50),
    (57, 'capacity', 10), (57, 'usb-connector', 20), (57, 'usb-standard', 30), (57, 'read-speed', 40),
    (57, 'colour', 50),
    (321, 'capacity', 10), (321, 'usb-connector', 20), (321, 'usb-standard', 30), (321, 'read-speed', 40),
    (321, 'colour', 50),
    (150, 'card-type', 10), (150, 'capacity', 20), (150, 'card-speed-class', 30), (150, 'read-speed', 40)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (7, 16, 235, 55, 56, 57, 321, 150) AND origin = 'MANUAL';
    IF n <> 33 THEN
        RAISE EXCEPTION 'Очаквах 33 групи в осемте категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 5 записана: % групи в 8 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (7, 16, 235, 55, 56, 57, 321, 150) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
