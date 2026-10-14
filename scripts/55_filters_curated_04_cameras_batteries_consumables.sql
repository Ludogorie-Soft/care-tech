-- ============================================================================
-- 55_filters_curated_04_cameras_batteries_consumables.sql
--
-- Курация на каноничния филтърен слой, партида 4:
--   100 IP камери     199 Батерии
--   86  Консумативи (тонери) за лазерни устройства
--   87  Консумативи за мастиленоструйни устройства
--
-- ИЗИСКВА: V38–V40, скриптове 55_..._01 (ползва „Цвят“) и 55_..._02 (ползва „Wi-Fi“)
--          и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- БАТЕРИИ: VALI дава два параметъра за едно и също — „Химически състав“ и „Технология батерия“;
--   тук те са едно свойство. Размерът идва като списък от синоними („6LF22;9V;6LR61“) и се
--   свежда до едно име (9V).
-- ТОНЕРИ И МАСТИЛА: ресурсът е в диапазони (парсер PAGE_YIELD): „Up to 10 200 pages“, „3150k“ и
--   „(12K)“ са от различни доставчици и не се сравняват като точни числа.
--
-- ПРАВИЛА ПО ИМЕ НА ПРОДУКТА — само когато параметрите на продукта мълчат:
--   тип, резолюция, обектив и нощно виждане на камерата („IP булет камера 2MP 2.8 mm IR-30“),
--   размер и химия на батерията („Алкална батерия GP LR9 625A“), марка на принтера, оригинален/
--   съвместим, цвят и ресурс на консуматива („CANON CRG-701 CYAN“, „UPRINT … 3150k, Black“).
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 4') и MANUAL групите на
--   четирите категории, после ги вмъква наново.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'colour' AND origin = 'MANUAL')
       OR NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'wifi' AND origin = 'MANUAL') THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_01 и 55_filters_curated_02.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((100, 'IP камери'), (199, 'Батерии'),
                                (86, 'Консумативи(тонери) за лазерни устройства'),
                                (87, 'Консумативи за мастиленоструйни устройства'))) <> 4 THEN
        RAISE EXCEPTION 'Някоя от категориите 100, 199, 86, 87 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 4';
DELETE FROM filter_value_rules WHERE note = '55 партида 4';
DELETE FROM filter_name_rules WHERE note = '55 партида 4';
DELETE FROM category_filters WHERE category_id IN (100, 199, 86, 87) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('camera-type',         'Тип камера',               'Camera type',         'ENUM',    NULL, NULL,         FALSE, 10, 'MANUAL'),
    ('camera-resolution',   'Резолюция',                'Resolution',          'ENUM',    NULL, NULL,         FALSE, 20, 'MANUAL'),
    ('camera-lens',         'Обектив',                  'Lens',                'ENUM',    NULL, NULL,         FALSE, 30, 'MANUAL'),
    ('night-vision-range',  'Нощно виждане (IR)',       'Night vision (IR)',   'ENUM',    NULL, NULL,         FALSE, 40, 'MANUAL'),
    ('colour-night-vision', 'Цветно нощно виждане',     'Colour night vision', 'ENUM',    NULL, NULL,         FALSE, 50, 'MANUAL'),
    ('ingress-protection',  'Защита',                   'Protection',          'ENUM',    NULL, NULL,         FALSE, 60, 'MANUAL'),
    ('camera-audio',        'Звук',                     'Audio',               'ENUM',    NULL, NULL,         FALSE, 70, 'MANUAL'),
    ('battery-size',        'Размер',                   'Size',                'ENUM',    NULL, NULL,         FALSE, 10, 'MANUAL'),
    ('rechargeable',        'Акумулаторна',             'Rechargeable',        'ENUM',    NULL, NULL,         FALSE, 20, 'MANUAL'),
    ('battery-chemistry',   'Химичен състав',           'Chemistry',           'ENUM',    NULL, NULL,         FALSE, 30, 'MANUAL'),
    ('battery-voltage',     'Напрежение',               'Voltage',             'ENUM',    NULL, NULL,         FALSE, 40, 'MANUAL'),
    ('printer-brand',       'Марка на принтера',        'Printer brand',       'ENUM',    NULL, NULL,         FALSE, 10, 'MANUAL'),
    ('consumable-origin',   'Оригинален / съвместим',   'Original/compatible', 'ENUM',    NULL, NULL,         FALSE, 20, 'MANUAL'),
    ('consumable-type',     'Вид консуматив',           'Consumable type',     'ENUM',    NULL, NULL,         FALSE, 30, 'MANUAL'),
    ('page-yield',          'Ресурс',                   'Page yield',          'NUMERIC', NULL, 'PAGE_YIELD', FALSE, 40, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('camera-type', 'camera-resolution', 'camera-lens', 'night-vision-range', 'colour-night-vision',
                   'ingress-protection', 'camera-audio', 'battery-size', 'rechargeable', 'battery-chemistry',
                   'battery-voltage', 'printer-brand', 'consumable-origin', 'consumable-type', 'page-yield')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
-- „Капацитет съхранение“ на MOST е обем на диск/памет навсякъде другаде; при консумативите е ресурс.
-- „Съвместими модели“ умишлено НЕ е източник за марката на принтера: VALI пише там „EPSON:“ пред
-- моделите на Canon PIXMA. Марката идва от „Марка принтер“ и от името („CANON CRG-701 CYAN“).
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 4'
FROM (VALUES
    ('camera-type',         'Корпус',                                  100::BIGINT),
    ('camera-type',         'Тип камера',                              100),
    ('camera-resolution',   'Резолюция',                               100),
    ('camera-resolution',   'Резолюция на сензора',                    100),
    ('camera-lens',         'Обектив',                                 100),
    ('camera-lens',         'Обхват на фокуса',                        100),
    ('camera-lens',         'Оптично увеличение',                      100),
    ('night-vision-range',  'IR подсветка',                            100),
    ('colour-night-vision', 'LED подсветка',                           100),
    ('wifi',                'Wi-Fi',                                   100),
    ('wifi',                'Поддържани протоколи за обмен на данни',  100),
    ('wifi',                'Съвместимост с мрежови стандарти',        100),
    ('ingress-protection',  'Степен на защита',                        100),
    ('camera-audio',        'Звук',                                    100),
    ('camera-audio',        'Вградени устройства',                     100),
    ('camera-audio',        'Характеристики на интернет камера',       100),
    ('battery-size',        'Батерия/акумулатор формат',               NULL),
    ('rechargeable',        'Тип аксесоар',                            199),
    ('battery-chemistry',   'Химически състав',                        NULL),
    ('battery-chemistry',   'Технология батерия',                      NULL),
    ('battery-voltage',     'Волтаж',                                  199),
    ('printer-brand',       'Марка принтер',                           NULL),
    ('consumable-origin',   'Консуматив',                              86),
    ('consumable-origin',   'Консуматив',                              87),
    ('consumable-type',     'Тип консуматив',                          86),
    ('page-yield',          'Макс. брой копия',                        NULL),
    ('page-yield',          'Капацитет съхранение',                    86),
    ('page-yield',          'Капацитет съхранение',                    87)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('camera-type', 'Булет', 'Bullet', 1), ('camera-type', 'Куполна', 'Dome', 2),
    ('camera-type', 'Турел', 'Turret', 3), ('camera-type', 'Въртяща (PT/PTZ)', 'Pan/tilt (PT/PTZ)', 4),
    ('camera-type', 'Куб', 'Cube', 5), ('camera-type', 'Панорамна', 'Panoramic', 6),
    ('camera-resolution', '2 MP (1080p)', '2 MP (1080p)', 1), ('camera-resolution', '3 MP', '3 MP', 2),
    ('camera-resolution', '4 MP', '4 MP', 3), ('camera-resolution', '5 MP', '5 MP', 4),
    ('camera-resolution', '6 MP', '6 MP', 5), ('camera-resolution', '8 MP (4K)', '8 MP (4K)', 6),
    ('camera-resolution', '10 MP', '10 MP', 7), ('camera-resolution', '12 MP', '12 MP', 8),
    ('camera-lens', '2.8 mm', '2.8 mm', 1), ('camera-lens', '3.6 mm', '3.6 mm', 2),
    ('camera-lens', '4 mm', '4 mm', 3), ('camera-lens', '6 mm', '6 mm', 4),
    ('camera-lens', 'Варифокален / зуум', 'Varifocal / zoom', 5),
    ('night-vision-range', 'До 20 м', 'Up to 20 m', 1), ('night-vision-range', '25 – 40 м', '25 – 40 m', 2),
    ('night-vision-range', '45 – 60 м', '45 – 60 m', 3), ('night-vision-range', 'Над 60 м', 'Over 60 m', 4),
    ('colour-night-vision', 'Да', 'Yes', 1),
    ('ingress-protection', 'IP67', 'IP67', 1), ('ingress-protection', 'IP66', 'IP66', 2),
    ('ingress-protection', 'IP65', 'IP65', 3),
    ('ingress-protection', 'IK10 (вандалоустойчива)', 'IK10 (vandal-proof)', 4),
    ('camera-audio', 'Микрофон', 'Microphone', 1), ('camera-audio', 'Двупосочно аудио', 'Two-way audio', 2),
    ('camera-audio', 'Аудио вход/изход', 'Audio in/out', 3),
    ('battery-size', 'AA', 'AA', 1), ('battery-size', 'AAA', 'AAA', 2), ('battery-size', 'C', 'C', 3),
    ('battery-size', 'D', 'D', 4), ('battery-size', '9V', '9V', 5), ('battery-size', '23A (12V)', '23A (12V)', 6),
    ('battery-size', '27A (12V)', '27A (12V)', 7), ('battery-size', 'N (LR1)', 'N (LR1)', 8),
    ('battery-size', 'AAAA', 'AAAA', 9),
    ('battery-size', 'CR2032', 'CR2032', 10), ('battery-size', 'CR2025', 'CR2025', 11),
    ('battery-size', 'CR2016', 'CR2016', 12), ('battery-size', 'CR2450', 'CR2450', 13),
    ('battery-size', 'CR2430', 'CR2430', 14), ('battery-size', 'CR1632', 'CR1632', 15),
    ('battery-size', 'CR1620', 'CR1620', 16), ('battery-size', 'CR1616', 'CR1616', 17),
    ('battery-size', 'CR1220', 'CR1220', 18), ('battery-size', 'CR1225', 'CR1225', 18),
    ('battery-size', 'CR1216', 'CR1216', 18), ('battery-size', 'CR2354', 'CR2354', 18),
    ('battery-size', 'CR2477', 'CR2477', 18),
    ('battery-size', 'CR123A', 'CR123A', 20), ('battery-size', 'CR2', 'CR2', 21),
    ('battery-size', 'CR-P2', 'CR-P2', 22), ('battery-size', '2CR5', '2CR5', 23),
    ('battery-size', '1/2 AA', '1/2 AA', 24), ('battery-size', 'CR-1/3N', 'CR-1/3N', 25),
    ('battery-size', 'SC', 'SC', 32), ('battery-size', '4LR44 (PX28)', '4LR44 (PX28)', 39),
    ('battery-size', '18650', '18650', 30), ('battery-size', '21700', '21700', 31),
    ('battery-size', 'LR44 (AG13)', 'LR44 (AG13)', 40), ('battery-size', 'LR41 (AG3)', 'LR41 (AG3)', 41),
    ('battery-size', 'LR1130 (AG10)', 'LR1130 (AG10)', 42), ('battery-size', 'SR626 (AG4)', 'SR626 (AG4)', 43),
    ('battery-size', 'SR927 (AG7)', 'SR927 (AG7)', 44),
    ('battery-size', 'ZA10', 'ZA10', 50), ('battery-size', 'ZA13', 'ZA13', 51),
    ('battery-size', 'ZA312', 'ZA312', 52), ('battery-size', 'ZA675', 'ZA675', 53),
    ('rechargeable', 'Да', 'Yes', 1), ('rechargeable', 'Не', 'No', 2),
    ('battery-chemistry', 'Алкална', 'Alkaline', 1), ('battery-chemistry', 'Литиева', 'Lithium', 2),
    ('battery-chemistry', 'Литиево-йонна', 'Lithium-ion', 3), ('battery-chemistry', 'NiMH', 'NiMH', 4),
    ('battery-chemistry', 'Сребърно-оксидна', 'Silver oxide', 5),
    ('battery-chemistry', 'Цинково-въздушна', 'Zinc-air', 6),
    ('battery-chemistry', 'Цинково-въглеродна', 'Zinc-carbon', 7),
    ('battery-voltage', '1.2 V', '1.2 V', 1), ('battery-voltage', '1.4 V', '1.4 V', 2),
    ('battery-voltage', '1.5 V', '1.5 V', 3), ('battery-voltage', '1.55 V', '1.55 V', 4),
    ('battery-voltage', '3 V', '3 V', 5), ('battery-voltage', '3.6 V', '3.6 V', 6),
    ('battery-voltage', '3.7 V', '3.7 V', 7), ('battery-voltage', '6 V', '6 V', 8),
    ('battery-voltage', '7.4 V', '7.4 V', 9), ('battery-voltage', '9 V', '9 V', 10),
    ('battery-voltage', '12 V', '12 V', 11),
    ('printer-brand', 'HP', 'HP', 1), ('printer-brand', 'Canon', 'Canon', 2),
    ('printer-brand', 'Brother', 'Brother', 3), ('printer-brand', 'Epson', 'Epson', 4),
    ('printer-brand', 'Samsung', 'Samsung', 5), ('printer-brand', 'Kyocera', 'Kyocera', 6),
    ('printer-brand', 'Ricoh', 'Ricoh', 7), ('printer-brand', 'Lexmark', 'Lexmark', 8),
    ('printer-brand', 'Xerox', 'Xerox', 9), ('printer-brand', 'Konica Minolta', 'Konica Minolta', 10),
    ('printer-brand', 'Develop', 'Develop', 11), ('printer-brand', 'OKI', 'OKI', 12),
    ('printer-brand', 'Pantum', 'Pantum', 13),
    ('consumable-origin', 'Оригинален', 'Original', 1), ('consumable-origin', 'Съвместим', 'Compatible', 2),
    ('consumable-type', 'Тонер касета', 'Toner cartridge', 1), ('consumable-type', 'Барабан', 'Drum unit', 2)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
-- Размер на батерия: всеки размер с търговските си синоними (LR6 = AA = Mignon = E91). „1/2AA“ и
-- „4LR44“ са други батерии, затова пред кода не може да има „/“ или цифра.
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 4'
FROM (VALUES
    ('camera-type', 'Булет',            '(булет|bullet)'),
    ('camera-type', 'Куполна',          '(купол|dome)'),
    ('camera-type', 'Турел',            '(турел|turret|eyeball)'),
    ('camera-type', 'Въртяща (PT/PTZ)', '((^|[^[:alnum:]])ptz?([^[:alnum:]]|$)|pan ?[&/-]? ?tilt|въртя)'),
    ('camera-type', 'Куб',              '(^|[^[:alnum:]])(cube|куб)'),
    ('camera-type', 'Панорамна',        '(fisheye|рибе|панорам|panoram)'),
    ('camera-resolution', '2 MP (1080p)', '((^|[^0-9.+])2 ?(mp|mpx|mpix|mpixel|megapixels?|мп)([^[:alnum:]]|$)|1080 ?p)'),
    ('camera-resolution', '3 MP',         '(^|[^0-9.+])3 ?(mp|mpx|mpix|mpixel|megapixels?|мп)([^[:alnum:]]|$)'),
    ('camera-resolution', '4 MP',         '(^|[^0-9.+])4 ?(mp|mpx|mpix|mpixel|megapixels?|мп)([^[:alnum:]]|$)'),
    ('camera-resolution', '5 MP',         '(^|[^0-9.+])5 ?(mp|mpx|mpix|mpixel|megapixels?|мп)([^[:alnum:]]|$)'),
    ('camera-resolution', '6 MP',         '(^|[^0-9.+])6 ?(mp|mpx|mpix|mpixel|megapixels?|мп)([^[:alnum:]]|$)'),
    ('camera-resolution', '8 MP (4K)',    '((^|[^0-9.+])8 ?(mp|mpx|mpix|mpixel|megapixels?|мп)([^[:alnum:]+]|$)|(^|[^[:alnum:]])4k([^[:alnum:]]|$))'),
    ('camera-resolution', '10 MP',        '(^|[^0-9.+])10 ?(mp|mpx|mpix|mpixel|megapixels?|мп)([^[:alnum:]]|$)'),
    ('camera-resolution', '12 MP',        '(^|[^0-9.+])12 ?(mp|mpx|mpix|mpixel|megapixels?|мп)([^[:alnum:]]|$)'),
    ('camera-lens', '2.8 mm', '(?<![0-9.])(?<![-–~] )(?<![-–~])2\.8 ?(mm|мм)(?! ?[-–~])'),
    ('camera-lens', '3.6 mm', '(?<![0-9.])(?<![-–~] )(?<![-–~])3\.6 ?(mm|мм)(?! ?[-–~])'),
    ('camera-lens', '4 mm',   '(?<![0-9.])(?<![-–~] )(?<![-–~])4 ?(mm|мм)(?! ?[-–~])'),
    ('camera-lens', '6 mm',   '(?<![0-9.])(?<![-–~] )(?<![-–~])6 ?(mm|мм)(?! ?[-–~])'),
    ('camera-lens', 'Варифокален / зуум', '([0-9.]+ ?(mm|мм)? ?[-–~] ?[0-9.]+ ?(mm|мм)|(?<![0-9.])([4-9]|[1-9][0-9])x([^[:alnum:]]|$)|моториз|motori|варифокал|varifocal)'),
    ('night-vision-range', 'До 20 м',   '^([5-9]|1[0-9]|20) ?(m|м)$'),
    ('night-vision-range', '25 – 40 м', '^(2[1-9]|3[0-9]|40) ?(m|м)$'),
    ('night-vision-range', '45 – 60 м', '^(4[1-9]|5[0-9]|60) ?(m|м)$'),
    ('night-vision-range', 'Над 60 м',  '^(6[1-9]|[7-9][0-9]|[1-9][0-9]{2}) ?(m|м)$'),
    ('colour-night-vision', 'Да', '[0-9]+ ?(m|м)'),
    ('ingress-protection', 'IP67', 'ip ?67'),
    ('ingress-protection', 'IP66', 'ip ?66'),
    ('ingress-protection', 'IP65', 'ip ?65'),
    ('ingress-protection', 'IK10 (вандалоустойчива)', 'ik ?10'),
    ('camera-audio', 'Микрофон',         '(микрофон|(^|[^[:alnum:]])mic)'),
    ('camera-audio', 'Двупосочно аудио', '(високоговорител|speaker|two-?way|двупосоч)'),
    ('camera-audio', 'Аудио вход/изход', '(вход/изход|audio in|аудио вход)'),
    ('battery-size', 'AA',        '(^|[^[:alnum:]/])(aa|lr6|r6|hr6|mignon|e91|um-?3|mn1500)([^[:alnum:]]|$)'),
    ('battery-size', 'AAA',       '(^|[^[:alnum:]/])(aaa|lr03|r03|hr03|e92|um-?4|mn2400)([^[:alnum:]]|$)'),
    ('battery-size', 'C',         '(^c$|(^|[^[:alnum:]/])(lr14|r14|hr14|baby|e93|um-?2|mn1400)([^[:alnum:]]|$))'),
    ('battery-size', 'D',         '(^d$|(^|[^[:alnum:]/])(lr20|r20|hr20|mono|e95|um-?1|mn1300)([^[:alnum:]]|$))'),
    ('battery-size', '9V',        '(^|[^[:alnum:].])(9 ?v|6lr61|6f22|6lf22|6hr61|e22|1604a?|pp3)([^[:alnum:]]|$)'),
    ('battery-size', '23A (12V)', '(^|[^[:alnum:]/])(23a|a23|а23|lr23a?|v23ga|mn21|8lr932)([^[:alnum:]]|$)'),
    ('battery-size', '27A (12V)', '(^|[^[:alnum:]/])(27a|a27|а27|lr27a?|v27ga|mn27|8lr732)([^[:alnum:]]|$)'),
    ('battery-size', 'N (LR1)',   '(^n$|(^|[^[:alnum:]/])(lr-?1n?|lr01|mn9100|e90|um-?5|am5)([^[:alnum:]]|$))'),
    ('battery-size', 'AAAA',      '(^|[^[:alnum:]/])(aaaa|lr61|lr8d425|e96)([^[:alnum:]]|$)'),
    ('battery-size', 'SC',        '(^|[^[:alnum:]])sc([^[:alnum:]]|$)'),
    ('battery-size', '4LR44 (PX28)', '(4lr44|px28|a544|v4034)'),
    ('battery-size', 'CR2032',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?2032([^0-9]|$)'),
    ('battery-size', 'CR2025',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?2025([^0-9]|$)'),
    ('battery-size', 'CR2016',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?2016([^0-9]|$)'),
    ('battery-size', 'CR2450',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?2450([^0-9]|$)'),
    ('battery-size', 'CR2430',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?2430([^0-9]|$)'),
    ('battery-size', 'CR1632',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?1632([^0-9]|$)'),
    ('battery-size', 'CR1620',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?1620([^0-9]|$)'),
    ('battery-size', 'CR1616',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?1616([^0-9]|$)'),
    ('battery-size', 'CR1220',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?1220([^0-9]|$)'),
    ('battery-size', 'CR1225',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?1225([^0-9]|$)'),
    ('battery-size', 'CR1216',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?1216([^0-9]|$)'),
    ('battery-size', 'CR2354',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?2354([^0-9]|$)'),
    ('battery-size', 'CR2477',    '(^|[^[:alnum:]])(cr|dl|br|ecr) ?-?2477([^0-9]|$)'),
    ('battery-size', 'CR-1/3N',   '((cr|dl) ?-?1/3 ?n|2l76)'),
    ('battery-size', 'CR123A',    '(^|[^[:alnum:]])(cr|dl|el) ?-?123a?([^0-9]|$)'),
    ('battery-size', 'CR2',       '(^|[^[:alnum:]])(cr|dl|el) ?-?2([^0-9a-z/]|$)'),
    ('battery-size', 'CR-P2',     '(^|[^[:alnum:]])(cr|dl|el) ?-?p2([^0-9]|$)'),
    ('battery-size', '2CR5',      '(^|[^[:alnum:]])(2cr ?-?5|el2cr5|dl245)([^0-9]|$)'),
    ('battery-size', '1/2 AA',    '1/2 ?aa([^a]|$)'),
    ('battery-size', '18650',     '(^|[^0-9])18650([^0-9]|$)'),
    ('battery-size', '21700',     '(^|[^0-9])21700([^0-9]|$)'),
    ('battery-size', 'LR44 (AG13)',   '(^|[^[:alnum:]/])(lr-?44|sr-?44w?|ag13|a76|v13ga|l1154|357a?)([^[:alnum:]]|$)'),
    ('battery-size', 'LR41 (AG3)',    '(^|[^[:alnum:]/])(lr-?41|sr-?41(-?sw)?|ag3|392a?|384|v3ga)([^[:alnum:]]|$)'),
    ('battery-size', 'LR1130 (AG10)', '(^|[^[:alnum:]/])(lr-?1130|sr-?1130(-?sw)?|ag10|389|390|v10ga|lr54)([^[:alnum:]]|$)'),
    ('battery-size', 'SR626 (AG4)',   '(^|[^[:alnum:]/])(sr-?626(-?sw)?|lr-?626|ag4|377|376|177)([^[:alnum:]]|$)'),
    ('battery-size', 'SR927 (AG7)',   '(^|[^[:alnum:]/])(sr-?927(-?sw)?|ag7|395|399)([^[:alnum:]]|$)'),
    ('battery-size', 'ZA10',          '(^|[^[:alnum:]])za ?-?10([^0-9]|$)'),
    ('battery-size', 'ZA13',          '(^|[^[:alnum:]])za ?-?13([^0-9]|$)'),
    ('battery-size', 'ZA312',         '(^|[^[:alnum:]])za ?-?312([^0-9]|$)'),
    ('battery-size', 'ZA675',         '(^|[^[:alnum:]])za ?-?675([^0-9]|$)'),
    ('rechargeable', 'Да', '^акумулатор'),
    ('rechargeable', 'Не', '^батерия$'),
    ('battery-chemistry', 'Алкална',            '(alkaline|алкал)'),
    ('battery-chemistry', 'Литиево-йонна',      '(li-?ion|lithium[ -]?ion|li-?po(ly)?|li-?polymer|литиево-йон)'),
    ('battery-chemistry', 'Литиева',            '(lithium(?![ -]?ion)|li/socl|thionyl|li-?mno2|литиев(?!о-йон))'),
    ('battery-chemistry', 'NiMH',               'ni-?mh'),
    ('battery-chemistry', 'Сребърно-оксидна',   '(ag2o|сребър|silver)'),
    ('battery-chemistry', 'Цинково-въздушна',   '(zinc[ -]?air|въздуш)'),
    ('battery-chemistry', 'Цинково-въглеродна', '(zinc[ -]?carbon|zinc chloride|карбон|манган)'),
    ('battery-voltage', '1.2 V',  '^1\.2 ?v$'),
    ('battery-voltage', '1.4 V',  '^1\.4 ?v$'),
    ('battery-voltage', '1.5 V',  '^1\.5 ?v$'),
    ('battery-voltage', '1.55 V', '^1\.55 ?v$'),
    ('battery-voltage', '3 V',    '^3 ?v$'),
    ('battery-voltage', '3.6 V',  '^3\.6 ?v$'),
    ('battery-voltage', '3.7 V',  '^3\.7 ?v$'),
    ('battery-voltage', '6 V',    '^6 ?v$'),
    ('battery-voltage', '7.4 V',  '^7\.4 ?v$'),
    ('battery-voltage', '9 V',    '^9 ?v$'),
    ('battery-voltage', '12 V',   '^12 ?v$'),
    ('printer-brand', 'HP',             '(^|[^[:alnum:]])(hp|laserjet|deskjet|officejet|pagewide|lj|dj)([^[:alnum:]]|$)'),
    ('printer-brand', 'Canon',          '(canon|pixma|i-sensys|imagerunner|imageclass|(^|[^[:alnum:]])lbp)'),
    ('printer-brand', 'Brother',        'brother'),
    ('printer-brand', 'Epson',          '(epson|ecotank|workforce)'),
    ('printer-brand', 'Samsung',        'samsung'),
    ('printer-brand', 'Kyocera',        '(kyocera|ecosys|taskalfa)'),
    ('printer-brand', 'Ricoh',          '(ricoh|aficio)'),
    ('printer-brand', 'Lexmark',        'lexmark'),
    ('printer-brand', 'Xerox',          '(xerox|phaser|workcentre|versalink|altalink)'),
    ('printer-brand', 'Konica Minolta', '(konica|minolta|bizhub)'),
    ('printer-brand', 'Develop',        '(^|[^[:alnum:]])(develop|ineo)([^[:alnum:]]|$)'),
    ('printer-brand', 'OKI',            '(^|[^[:alnum:]])oki([^[:alnum:]]|$)'),
    ('printer-brand', 'Pantum',         'pantum'),
    ('consumable-origin', 'Оригинален', '^оригинал'),
    ('consumable-origin', 'Съвместим',  '^съвмест'),
    ('consumable-type', 'Тонер касета', '^тонер'),
    ('consumable-type', 'Барабан',      '^барабан')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
-- Тип, резолюция и обектив на камерата, размер и химия на батерията: същите изрази като за параметрите.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, c.category_id, r.pattern, r.value_id, '55 партида 4'
FROM filter_value_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
JOIN (VALUES ('camera-type', 100::BIGINT), ('camera-resolution', 100), ('camera-lens', 100),
             ('ingress-protection', 100), ('battery-size', 199), ('battery-chemistry', 199),
             ('printer-brand', 86), ('printer-brand', 87)) AS c(slug, category_id) ON c.slug = a.slug
WHERE r.note = '55 партида 4';

INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 4'
FROM (VALUES
    -- Кодовете на DAHUA казват корпуса: HFW булет, HDBW купол, HDW турел, SD въртяща.
    ('camera-type', 100::BIGINT, 'Булет',            'ipc-hfw'),
    ('camera-type', 100, 'Куполна',          'ipc-hdbw'),
    ('camera-type', 100, 'Турел',            'ipc-hdw'),
    ('camera-type', 100, 'Въртяща (PT/PTZ)', '((^|[^[:alnum:]])sd[0-9][0-9a-z]{3}|ipc-pt|ipc-s[0-9])'),
    ('night-vision-range', 100, 'До 20 м',   '(^|[^[:alnum:]])ir[- ]?([5-9]|1[0-9]|20) ?(m|м)?([^0-9]|$)'),
    ('night-vision-range', 100, '25 – 40 м', '(^|[^[:alnum:]])ir[- ]?(2[1-9]|3[0-9]|40) ?(m|м)?([^0-9]|$)'),
    ('night-vision-range', 100, '45 – 60 м', '(^|[^[:alnum:]])ir[- ]?(4[1-9]|5[0-9]|60) ?(m|м)?([^0-9]|$)'),
    ('night-vision-range', 100, 'Над 60 м',  '(^|[^[:alnum:]])ir[- ]?(6[1-9]|[7-9][0-9]|[1-9][0-9]{2}) ?(m|м)?([^0-9]|$)'),
    ('colour-night-vision', 100, 'Да', '(wizcolor|colorvu|full ?colou?r|dual ?light|dual ?led|hybrid ?light|warm ?light|&led|(^|[^[:alnum:]])led[- ]?[0-9]{2})'),
    ('wifi', 100, 'Да', '(wi-?fi|wireless|безжичн)'),
    ('ingress-protection', 100, 'IK10 (вандалоустойчива)', '(вандал|vandal)'),
    ('camera-audio', 100, 'Микрофон',         '(микрофон|(^|[^[:alnum:]])mic([^[:alnum:]]|$))'),
    ('camera-audio', 100, 'Двупосочно аудио', '(two-?way|двупосоч)'),
    ('rechargeable', 199, 'Да', '(акумулатор|rechargeable|ni-?mh|li-?ion)'),
    -- Кодове на касетите, когато марката на принтера не е в името.
    ('printer-brand', 86, 'HP',      '(^|[^[:alnum:]])(cf|ce|cb|cc|ch|cn|q|w)[0-9]{3,4}[a-z]{1,2}([^[:alnum:]]|$)'),
    ('printer-brand', 87, 'HP',      '(^|[^[:alnum:]])(cf|ce|cb|cc|ch|cn|q|w)[0-9]{3,4}[a-z]{1,2}([^[:alnum:]]|$)'),
    ('printer-brand', 86, 'Canon',   '(^|[^[:alnum:]])(crg|ep|fx|c-exv|npg|gpr)[- ]?[0-9]'),
    ('printer-brand', 87, 'Canon',   '(^|[^[:alnum:]])(cli|pgi|pg|cl|bci|bc|bji|bx)[- ]?[0-9]'),
    ('printer-brand', 86, 'Brother', '(^|[^[:alnum:]])(tn|dr)[- ]?[0-9]{3,4}'),
    ('printer-brand', 87, 'Brother', '(^|[^[:alnum:]])lc[- ]?[0-9]{3,4}'),
    ('printer-brand', 86, 'Kyocera', '(^|[^[:alnum:]])tk[- ]?[0-9]{3,4}'),
    ('printer-brand', 86, 'Samsung', '(^|[^[:alnum:]])(mlt|clt)-'),
    ('printer-brand', 86, 'Ricoh',   '(^|[^[:alnum:]])(sp ?c?[0-9]{3}|im ?c?[0-9]{3,4}|mp ?c?[0-9]{4}|m c[0-9]{3})'),
    ('consumable-origin', 86, 'Съвместим',  '(uprint|orink|generink|съвмест|compatible|static control)'),
    ('consumable-origin', 87, 'Съвместим',  '(uprint|orink|generink|съвмест|compatible|static control)'),
    ('consumable-origin', 86, 'Оригинален', '^(canon|hp|brother|epson|samsung|kyocera|lexmark|xerox|ricoh|oki|konica|develop|pantum)([^[:alnum:]]|$)'),
    ('consumable-origin', 87, 'Оригинален', '^(canon|hp|brother|epson|samsung|kyocera|lexmark|xerox|ricoh|oki|konica|develop|pantum)([^[:alnum:]]|$)'),
    ('consumable-type', 86, 'Барабан',      '(барабан|drum|photo ?conductor|(^|[^[:alnum:]])dr-?[0-9]{3,4})'),
    ('consumable-type', 86, 'Тонер касета', '(тонер|toner|(^|[^[:alnum:]])ton$)')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Цвят на консуматива по името: думата („CYAN“, „Черна“) или буквата след кода („CLI-8BK“, „BCI-6M“,
-- „CRG 069H C“). Буквата се чете само в края на името — „Ricoh M C320H“ е модел, не магента.
-- „Color“ е цветна касета само при мастилата; при тонерите е част от модела („HP Color LaserJet“).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, c.category_id, r.pattern, v.id, '55 партида 4'
FROM (VALUES
    ('Черен',       'BOTH', '((^|[^[:alpha:]])(черен|чер[еe]н|черн[аио]|black)([^[:alpha:]]|$)|([0-9]|xl|[^[:alnum:]])(bk|bl|pgbk)([^[:alnum:]]|$))'),
    ('Циан',        'BOTH', '((^|[^[:alpha:]])(циан|cyan)([^[:alpha:]]|$)|([0-9]|xl|[^[:alnum:]])c$)'),
    ('Магента',     'BOTH', '((^|[^[:alpha:]])(магента|magenta)([^[:alpha:]]|$)|([0-9]|xl|[^[:alnum:]])m$)'),
    ('Жълт',        'BOTH', '((^|[^[:alpha:]])(жълт|жълт[аои]|y[еe]llow|yell)([^[:alpha:]]|$)|([0-9]|xl|[^[:alnum:]])y$)'),
    ('Сив',         'BOTH', '((^|[^[:alpha:]])(сив|сив[аои]|gr[ae]y)([^[:alpha:]]|$)|[0-9]gy$)'),
    ('Многоцветен', 'INK',  '((^|[^[:alpha:]])(colou?r|tri-?colou?r|цветн[аои]?|col)([^[:alpha:]]|$)|[0-9]col|(^|[^[:alnum:]])cl-?[0-9])')
) AS r(value, grp, pattern)
JOIN filter_attributes a ON a.slug = 'colour' AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value)
JOIN (VALUES (86::BIGINT, 'TONER'), (87, 'INK')) AS c(category_id, grp)
  ON r.grp = 'BOTH' OR r.grp = c.grp;

-- Ресурс от името: „4200 копия“, „3150k“, „(12K)“.
INSERT INTO filter_name_rules (attribute_id, category_id, use_parser, note)
SELECT a.id, c.category_id, TRUE, '55 партида 4'
FROM filter_attributes a CROSS JOIN (VALUES (86::BIGINT), (87)) AS c(category_id)
WHERE a.slug = 'page-yield' AND a.origin = 'MANUAL';

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (100, 'MANUAL'), (199, 'MANUAL'), (86, 'MANUAL'), (87, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (100::BIGINT, 'camera-type', 10), (100, 'camera-resolution', 20), (100, 'camera-lens', 30),
    (100, 'night-vision-range', 40), (100, 'colour-night-vision', 50), (100, 'wifi', 60),
    (100, 'ingress-protection', 70), (100, 'camera-audio', 80), (100, 'colour', 90),
    (199, 'battery-size', 10), (199, 'rechargeable', 20), (199, 'battery-chemistry', 30),
    (199, 'battery-voltage', 40),
    (86, 'printer-brand', 10), (86, 'colour', 20), (86, 'consumable-origin', 30), (86, 'page-yield', 40),
    (86, 'consumable-type', 50),
    (87, 'printer-brand', 10), (87, 'colour', 20), (87, 'consumable-origin', 30), (87, 'page-yield', 40)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (100, 199, 86, 87) AND origin = 'MANUAL';
    IF n <> 22 THEN
        RAISE EXCEPTION 'Очаквах 22 групи в четирите категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 4 записана: % групи в 4 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (100, 199, 86, 87) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
