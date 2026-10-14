-- ============================================================================
-- 55_filters_curated_10_mounts_pcs_surveillance.sql
--
-- Курация на каноничния филтърен слой, партида 10:
--   140 Стойки за TV и високоговорители   51  Стойки за монитори    32 Настолни компютри
--   115 Проектори                          58  USB хъбове            237 Аналогови камери
--   962 NVR                                203 Батерии за UPS и СОТ-аларми
--
-- ИЗИСКВА: V38–V40 и скриптове 55_..._01 („Цвят“), 55_..._02 (свойствата на лаптопите), 55_..._04
--          (свойствата на IP камерите, „Напрежение“), 55_..._05 („USB стандарт“), 55_..._07 („PoE“,
--          „Брой портове“) и поне един успешен rebuild.
-- СЛЕД СКРИПТА: POST /api/admin/filters/rebuild (иначе в 01:00).
--
-- VESA е многоопционен параметър на VALI (стойката поддържа 100x100, 200x200, 400x400…) — филтърът
--   показва стойките, които поддържат размера на телевизора.
-- „Настолни компютри“ ползват свойствата на лаптопите; капацитет от името не се чете — „RTX 3050 8GB“
--   е видео памет, не RAM.
-- Проекторите на MOST имат данните в свободен текст („4,000 (standard), 3,200 (eco) ANSI lumens“) — чете
--   се стандартната яркост, не ECO; „Лампа“ всъщност казва източника на светлина (UHP, LED, Laser).
--
-- ИДЕМПОТЕНТНОСТ: трие собствените си правила (note = '55 партида 10') и MANUAL групите на деветте
--   категории, после ги вмъква наново. Правилата по име на камерите и лаптопите се копират от 55_04 и
--   55_02 — пусни скрипта отново, ако те се променят.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF (SELECT count(*) FROM filter_attributes
        WHERE origin = 'MANUAL' AND slug IN ('colour', 'cpu-series', 'camera-type', 'battery-voltage', 'usb-standard',
                                             'poe', 'port-count')) <> 7 THEN
        RAISE EXCEPTION 'Първо пусни 55_filters_curated_01, 02, 04, 05 и 07.';
    END IF;
    IF (SELECT count(*) FROM categories
        WHERE (id, name_bg) IN ((140, 'Стойки за TV и високоговорители'), (51, 'Стойки за монитори'),
                                (32, 'Настолни компютри'), (115, 'Проектори'), (58, 'USB хъбове'),
                                (237, 'Аналогови камери'), (962, 'NVR'), (203, 'Батерии за UPS и СОТ-аларми'))) <> 8 THEN
        RAISE EXCEPTION 'Някоя от категориите 140, 51, 32, 115, 58, 237, 962, 203 не е тази, която очаквам — спирам.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_attribute_sources WHERE note = '55 партида 10';
DELETE FROM filter_value_rules WHERE note = '55 партида 10';
DELETE FROM filter_name_rules WHERE note = '55 партида 10';
DELETE FROM category_filters WHERE category_id IN (140, 51, 32, 115, 58, 237, 962, 203) AND origin = 'MANUAL';

-- ── 1. Свойства ──────────────────────────────────────────────────────────────
INSERT INTO filter_attributes (slug, name_bg, name_en, value_type, unit, parser, auto_values, sort_order, origin)
VALUES
    ('mount-for',             'Стойка за',                'Mount for',            'ENUM', NULL, NULL, FALSE, 5,  'MANUAL'),
    ('mount-location',        'Монтаж',                   'Mounting',             'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('mount-motion',          'Вид стойка',               'Mount type',           'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('max-screen',            'Макс. диагонал',           'Max screen size',      'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('vesa',                  'VESA',                     'VESA',                 'ENUM', NULL, NULL, FALSE, 40, 'MANUAL'),
    ('mount-load',            'Макс. натоварване',        'Max load',             'ENUM', NULL, NULL, FALSE, 50, 'MANUAL'),
    ('pc-type',               'Вид компютър',             'Computer type',        'ENUM', NULL, NULL, FALSE, 5,  'MANUAL'),
    ('projector-resolution',  'Резолюция',                'Resolution',           'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('projector-brightness',  'Яркост',                   'Brightness',           'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('light-source',          'Източник на светлина',     'Light source',         'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('hub-extras',            'Допълнителни портове',     'Extra ports',          'ENUM', NULL, NULL, FALSE, 30, 'MANUAL'),
    ('video-protocol',        'Протокол',                 'Video protocol',       'ENUM', NULL, NULL, FALSE, 50, 'MANUAL'),
    ('nvr-channels',          'Брой канали',              'Channels',             'ENUM', NULL, NULL, FALSE, 10, 'MANUAL'),
    ('nvr-hdd',               'Брой дискове',             'Drive bays',           'ENUM', NULL, NULL, FALSE, 20, 'MANUAL'),
    ('ups-battery-capacity',  'Капацитет',                'Capacity',             'ENUM', NULL, NULL, FALSE, 20, 'MANUAL')
ON CONFLICT (slug) DO UPDATE
    SET name_bg = EXCLUDED.name_bg, name_en = EXCLUDED.name_en, value_type = EXCLUDED.value_type,
        unit = EXCLUDED.unit, parser = EXCLUDED.parser, auto_values = EXCLUDED.auto_values,
        sort_order = EXCLUDED.sort_order, updated_at = now()
    WHERE filter_attributes.origin = 'MANUAL';

DO $$
DECLARE taken TEXT;
BEGIN
    SELECT string_agg(slug, ', ') INTO taken FROM filter_attributes
    WHERE slug IN ('mount-for', 'mount-location', 'mount-motion', 'max-screen', 'vesa', 'mount-load', 'pc-type',
                   'projector-resolution', 'projector-brightness', 'light-source', 'hub-extras', 'video-protocol',
                   'nvr-channels', 'nvr-hdd', 'ups-battery-capacity')
      AND origin <> 'MANUAL';
    IF taken IS NOT NULL THEN
        RAISE EXCEPTION 'Slug зает от AUTO свойство: % — смени slug-а в скрипта.', taken;
    END IF;
END $$;

-- ── 2. Сурови параметри → свойство ───────────────────────────────────────────
INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
SELECT 'MAP', a.id, filter_norm(s.name), s.category_id, 'MANUAL', '55 партида 10'
FROM (VALUES
    ('mount-for',            'Стойка за',                     140::BIGINT),
    ('mount-for',            'Стойка за',                     51),
    ('mount-location',       'Монтиране на',                  140),
    ('mount-location',       'Монтиране на',                  51),
    ('mount-motion',         'Тип стойка',                    140),
    ('mount-motion',         'Тип стойка',                    51),
    ('max-screen',           'Макс.диагонал (инча)',          140),
    ('max-screen',           'Макс.диагонал (инча)',          51),
    ('vesa',                 'VESA стандарт',                 140),
    ('vesa',                 'VESA',                          51),
    ('mount-load',           'Макс.тегло (кг)',               140),
    ('mount-load',           'Максимално поддържано тегло',   51),
    ('pc-type',              'Тип система',                   32),
    ('cpu-series',           'Модел процесор',                32),
    ('cpu-series',           'Тип процесор',                  32),
    ('ram-size',             'RAM памет',                     32),
    ('disk-capacity',        'Тип съхранение',                32),
    ('gpu-model',            'Модел видео карта',             32),
    ('operating-system',     'Операционна система',           32),
    ('projector-resolution', 'Image',                         115),
    ('projector-brightness', 'Brightness',                    115),
    ('light-source',         'Лампа',                         115),
    ('port-count',           'Брой портове',                  58),
    ('usb-standard',         'Интерфейс',                     58),
    ('hub-extras',           'Портове',                       58),
    ('camera-type',          'Корпус',                        237),
    ('camera-resolution',    'Резолюция',                     237),
    ('camera-lens',          'Обектив',                       237),
    ('night-vision-range',   'IR подсветка',                  237),
    ('video-protocol',       'Протокол',                      237),
    ('nvr-channels',         'IP канали',                     962),
    ('nvr-hdd',              'Брой HDD',                      962),
    ('battery-voltage',      'Изходно напрежение',            203),
    ('ups-battery-capacity', 'Капацитет',                     203)
) AS s(slug, name, category_id)
JOIN filter_attributes a ON a.slug = s.slug AND a.origin = 'MANUAL';

-- ── 3. Стойности на ENUM свойствата ──────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
SELECT a.id, v.bg, v.en, filter_norm(v.bg), v.ord, 'MANUAL'
FROM (VALUES
    ('mount-for', 'TV', 'TV', 1), ('mount-for', 'Монитор', 'Monitor', 2), ('mount-for', '2 и повече монитора', 'Two or more monitors', 3),
    ('mount-for', 'Тонколони', 'Speakers', 4),
    ('mount-location', 'Стена', 'Wall', 1), ('mount-location', 'Таван', 'Ceiling', 2), ('mount-location', 'Бюро', 'Desk', 3),
    ('mount-location', 'Под / мобилна', 'Floor / mobile', 4),
    ('mount-motion', 'Фиксирана', 'Fixed', 1), ('mount-motion', 'Подвижна', 'Adjustable', 2),
    ('max-screen', 'До 27"', 'Up to 27"', 1), ('max-screen', '28 – 35"', '28 – 35"', 2), ('max-screen', '36 – 55"', '36 – 55"', 3),
    ('max-screen', '56 – 75"', '56 – 75"', 4), ('max-screen', 'Над 75"', 'Over 75"', 5),
    ('vesa', '75 x 75', '75 x 75', 1), ('vesa', '100 x 100', '100 x 100', 2), ('vesa', '200 x 100', '200 x 100', 3),
    ('vesa', '200 x 200', '200 x 200', 4), ('vesa', '300 x 200', '300 x 200', 5), ('vesa', '300 x 300', '300 x 300', 6),
    ('vesa', '400 x 200', '400 x 200', 7), ('vesa', '400 x 300', '400 x 300', 8), ('vesa', '400 x 400', '400 x 400', 9),
    ('vesa', '600 x 400', '600 x 400', 10), ('vesa', '800 x 400', '800 x 400', 11), ('vesa', '800 x 600', '800 x 600', 12),
    ('mount-load', 'До 15 кг', 'Up to 15 kg', 1), ('mount-load', '16 – 30 кг', '16 – 30 kg', 2),
    ('mount-load', '31 – 50 кг', '31 – 50 kg', 3), ('mount-load', 'Над 50 кг', 'Over 50 kg', 4),
    ('pc-type', 'Настолен (кутия)', 'Desktop tower', 1), ('pc-type', 'Мини компютър', 'Mini PC', 2),
    ('pc-type', 'All-in-One', 'All-in-One', 3),
    ('projector-resolution', 'SVGA (800x600)', 'SVGA (800x600)', 1), ('projector-resolution', 'XGA (1024x768)', 'XGA (1024x768)', 2),
    ('projector-resolution', 'WXGA (1280x800)', 'WXGA (1280x800)', 3),
    ('projector-resolution', 'Full HD (1080p)', 'Full HD (1080p)', 4), ('projector-resolution', '4K', '4K', 5),
    ('projector-brightness', 'До 3 000 lm', 'Up to 3,000 lm', 1), ('projector-brightness', '3 000 – 3 999 lm', '3,000 – 3,999 lm', 2),
    ('projector-brightness', '4 000 – 4 999 lm', '4,000 – 4,999 lm', 3), ('projector-brightness', '5 000 lm и повече', '5,000 lm and more', 4),
    ('light-source', 'Лампа', 'Lamp', 1), ('light-source', 'LED', 'LED', 2), ('light-source', 'Лазер', 'Laser', 3),
    ('hub-extras', 'HDMI', 'HDMI', 1), ('hub-extras', 'Ethernet (LAN)', 'Ethernet (LAN)', 2),
    ('hub-extras', 'Четец за карти (SD)', 'Card reader (SD)', 3), ('hub-extras', 'Аудио жак', 'Audio jack', 4),
    ('hub-extras', 'Зареждане (USB-C PD)', 'Charging (USB-C PD)', 5),
    ('video-protocol', 'HDCVI', 'HDCVI', 1), ('video-protocol', 'AHD', 'AHD', 2), ('video-protocol', 'TVI', 'TVI', 3),
    ('video-protocol', 'CVBS (аналогов)', 'CVBS (analogue)', 4),
    ('nvr-channels', '4', '4', 1), ('nvr-channels', '8', '8', 2), ('nvr-channels', '16', '16', 3),
    ('nvr-channels', '32', '32', 4), ('nvr-channels', '64', '64', 5),
    ('nvr-hdd', '1', '1', 1), ('nvr-hdd', '2', '2', 2), ('nvr-hdd', '4', '4', 3), ('nvr-hdd', '8', '8', 4),
    ('ups-battery-capacity', 'До 10 Ah', 'Up to 10 Ah', 1), ('ups-battery-capacity', '11 – 50 Ah', '11 – 50 Ah', 2),
    ('ups-battery-capacity', 'Над 50 Ah', 'Over 50 Ah', 3)
) AS v(slug, bg, en, ord)
JOIN filter_attributes a ON a.slug = v.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO UPDATE
    SET value_bg = EXCLUDED.value_bg, value_en = EXCLUDED.value_en, sort_order = EXCLUDED.sort_order,
        origin = 'MANUAL';

-- ── 4. Сурова стойност → стойност ────────────────────────────────────────────
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 10'
FROM (VALUES
    ('mount-for', '2 и повече монитора', '((^|[^0-9])[2-6] ?x ?монитор|[2-6] монитора|dual|double|двоен)'),
    ('mount-for', 'Монитор',             '^(?!.*((^|[^0-9])[2-6] ?x ?монитор|[2-6] монитора|dual|double|двоен)).*монитор'),
    ('mount-for', 'TV',                  '((^|[^[:alnum:]])tv([^[:alnum:]]|$)|телевизор)'),
    ('mount-for', 'Тонколони',           '(говорител|тонколон|speaker|google home|nest)'),
    ('mount-location', 'Стена',         '(стена|стенна|wall)'),
    ('mount-location', 'Таван',         '(таван|ceiling)'),
    ('mount-location', 'Бюро',          '(бюро|desk)'),
    ('mount-location', 'Под / мобилна', '(пода|floor|mobile|мобилн|trolley|на колела)'),
    ('mount-motion', 'Фиксирана', '(фиксиран|fixed)'),
    ('mount-motion', 'Подвижна',  '(подвижн|регулируем|tilt|swivel|full[- ]motion|articulat|(^|[^[:alpha:]])arm([^[:alpha:]]|$))'),
    ('max-screen', 'До 27"',   '(^|[^0-9.])(1[0-9]|2[0-7])([.,][0-9])? ?-?("|”|″|inch|инча|'''')(?! ?[-–~])'),
    ('max-screen', '28 – 35"', '(^|[^0-9.])(2[89]|3[0-5])([.,][0-9])? ?-?("|”|″|inch|инча|'''')(?! ?[-–~])'),
    ('max-screen', '36 – 55"', '(^|[^0-9.])(3[6-9]|4[0-9]|5[0-5])([.,][0-9])? ?-?("|”|″|inch|инча|'''')(?! ?[-–~])'),
    ('max-screen', '56 – 75"', '(^|[^0-9.])(5[6-9]|6[0-9]|7[0-5])([.,][0-9])? ?-?("|”|″|inch|инча|'''')(?! ?[-–~])'),
    ('max-screen', 'Над 75"',  '(^|[^0-9.])(7[6-9]|[89][0-9]|1[0-2][0-9])([.,][0-9])? ?-?("|”|″|inch|инча|'''')(?! ?[-–~])'),
    ('vesa', '75 x 75',   '(^|[^0-9])75 ?[x×х*] ?75([^0-9]|$)'),
    ('vesa', '100 x 100', '(^|[^0-9])100 ?[x×х*] ?100([^0-9]|$)'),
    ('vesa', '200 x 100', '(^|[^0-9])200 ?[x×х*] ?100([^0-9]|$)'),
    ('vesa', '200 x 200', '(^|[^0-9])200 ?[x×х*] ?200([^0-9]|$)'),
    ('vesa', '300 x 200', '(^|[^0-9])300 ?[x×х*] ?200([^0-9]|$)'),
    ('vesa', '300 x 300', '(^|[^0-9])300 ?[x×х*] ?300([^0-9]|$)'),
    ('vesa', '400 x 200', '(^|[^0-9])400 ?[x×х*] ?200([^0-9]|$)'),
    ('vesa', '400 x 300', '(^|[^0-9])400 ?[x×х*] ?300([^0-9]|$)'),
    ('vesa', '400 x 400', '(^|[^0-9])400 ?[x×х*] ?400([^0-9]|$)'),
    ('vesa', '600 x 400', '(^|[^0-9])600 ?[x×х*] ?400([^0-9]|$)'),
    ('vesa', '800 x 400', '(^|[^0-9])800 ?[x×х*] ?400([^0-9]|$)'),
    ('vesa', '800 x 600', '(^|[^0-9])800 ?[x×х*] ?600([^0-9]|$)'),
    ('mount-load', 'До 15 кг',    '^(0[.,][0-9]+|[1-9]|1[0-5])([.,][0-9]+)? ?(kg|кг)'),
    ('mount-load', '16 – 30 кг',  '^(1[6-9]|2[0-9]|30)([.,][0-9]+)? ?(kg|кг)'),
    ('mount-load', '31 – 50 кг',  '^(3[1-9]|4[0-9]|50)([.,][0-9]+)? ?(kg|кг)'),
    ('mount-load', 'Над 50 кг',   '^(5[1-9]|[6-9][0-9]|[1-9][0-9]{2})([.,][0-9]+)? ?(kg|кг)'),
    ('pc-type', 'All-in-One',       '(all[- ]?in[- ]?one|(^|[^[:alnum:]])aio([^[:alnum:]]|$))'),
    ('pc-type', 'Мини компютър',    '(мини|mini|micro|brix|nuc)'),
    ('pc-type', 'Настолен (кутия)', '(настолен|персонален|desktop|tower)'),
    ('cpu-series', 'Celeron / Pentium / N', 'n-серия'),
    ('projector-resolution', '4K',              '(4k|3840|2160|uhd)'),
    ('projector-resolution', 'Full HD (1080p)', '^(?!.*(4k|3840|2160|uhd)).*(1080p|1[,.]?920 ?x ?1[,.]?080|full ?hd)'),
    ('projector-resolution', 'WXGA (1280x800)', '(wxga|1[,.]?280 ?x ?800)'),
    ('projector-resolution', 'XGA (1024x768)',  '((^|[^a-z])xga|1[,.]?024 ?x ?768)'),
    ('projector-resolution', 'SVGA (800x600)',  '(svga|(^|[^0-9])800 ?x ?600)'),
    ('projector-brightness', 'До 3 000 lm',       '(^|[^0-9,.])([1-2][,.]?[0-9]{3}|[1-9][0-9]{2})(( ?\(standard\))|( ?\(standard\))?,? ?(ansi )?(lumens?|lm)(?!s)(?! ?\(eco))'),
    ('projector-brightness', '3 000 – 3 999 lm',  '(^|[^0-9,.])3[,.]?[0-9]{3}(( ?\(standard\))|( ?\(standard\))?,? ?(ansi )?(lumens?|lm)(?!s)(?! ?\(eco))'),
    ('projector-brightness', '4 000 – 4 999 lm',  '(^|[^0-9,.])4[,.]?[0-9]{3}(( ?\(standard\))|( ?\(standard\))?,? ?(ansi )?(lumens?|lm)(?!s)(?! ?\(eco))'),
    ('projector-brightness', '5 000 lm и повече', '(^|[^0-9,.])([5-9][,.]?[0-9]{3}|[1-9][0-9][,.]?[0-9]{3})(( ?\(standard\))|( ?\(standard\))?,? ?(ansi )?(lumens?|lm)(?!s)(?! ?\(eco))'),
    ('light-source', 'Лазер', '(laser|лазер)'),
    ('light-source', 'LED',   '(^|[^[:alnum:]])(rgb)?led([^[:alnum:]]|$)'),
    ('light-source', 'Лампа', '^(?!.*(laser|лазер|(^|[^[:alnum:]])(rgb)?led([^[:alnum:]]|$))).*(uhp|lamp|лампа|osram|philips)'),
    ('hub-extras', 'HDMI',                 'hdmi'),
    ('hub-extras', 'Ethernet (LAN)',       '((^|[^[:alnum:]])lan([^[:alnum:]]|$)|ethernet|rj-?45)'),
    ('hub-extras', 'Четец за карти (SD)',  '((^|[^[:alnum:]])(micro ?)?sd([^[:alnum:]]|$)|card reader|четец)'),
    ('hub-extras', 'Аудио жак',            '(3[.,]5 ?mm|audio|аудио)'),
    ('hub-extras', 'Зареждане (USB-C PD)', '((^|[^[:alnum:]])pd([^[:alnum:]]|[0-9]|$)|power delivery)'),
    ('video-protocol', 'HDCVI',           '(hd-?cvi|(^|[^[:alnum:]])cvi([^[:alnum:]]|$))'),
    ('video-protocol', 'AHD',             '(^|[^[:alnum:]])ahd([^[:alnum:]]|$)'),
    ('video-protocol', 'TVI',             '(hd-?tvi|(^|[^[:alnum:]])tvi([^[:alnum:]]|$))'),
    ('video-protocol', 'CVBS (аналогов)', '(cvbs|аналогов)'),
    ('nvr-channels', '4',  '^4$'), ('nvr-channels', '8', '^8$'), ('nvr-channels', '16', '^16$'),
    ('nvr-channels', '32', '^32$'), ('nvr-channels', '64', '^64$'),
    ('nvr-hdd', '1', '^1([^0-9]|$)'), ('nvr-hdd', '2', '^2([^0-9]|$)'), ('nvr-hdd', '4', '^4([^0-9]|$)'),
    ('nvr-hdd', '8', '^8([^0-9]|$)'),
    ('ups-battery-capacity', 'До 10 Ah',   '(^([0-9]|10)([.,][0-9]+)? ?ah|mah)'),
    ('ups-battery-capacity', '11 – 50 Ah', '^(1[1-9]|[2-4][0-9]|50)([.,][0-9]+)? ?ah'),
    ('ups-battery-capacity', 'Над 50 Ah',  '^(5[1-9]|[6-9][0-9]|[1-9][0-9]{2})([.,][0-9]+)? ?ah')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 5. Стойности от името на продукта (само ако параметрите не са дали) ───────
-- Правилата на стойностите важат и за имената, освен закотвените към цялата стойност.
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT r.attribute_id, c.category_id, r.pattern, r.value_id, '55 партида 10'
FROM filter_value_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
JOIN (VALUES ('mount-for', 140::BIGINT), ('mount-for', 51), ('mount-location', 140), ('mount-location', 51),
             ('mount-motion', 140), ('mount-motion', 51), ('max-screen', 140), ('max-screen', 51),
             ('vesa', 140), ('vesa', 51), ('pc-type', 32), ('projector-resolution', 115),
             ('projector-brightness', 115), ('light-source', 115), ('hub-extras', 58), ('video-protocol', 237))
     AS c(slug, category_id) ON c.slug = a.slug
WHERE r.note = '55 партида 10';

-- Аналогови камери: тип, резолюция, обектив и нощно виждане по името — както IP камерите (55_04).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, use_parser, note)
SELECT r.attribute_id, 237, r.pattern, r.value_id, r.use_parser, '55 партида 10'
FROM filter_name_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL'
                        AND a.slug IN ('camera-type', 'camera-resolution', 'camera-lens', 'night-vision-range')
WHERE r.category_id = 100 AND r.note = '55 партида 4';

-- Настолни компютри: серия процесор и видео карта по името — както лаптопите (55_02).
INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, use_parser, note)
SELECT r.attribute_id, 32, r.pattern, r.value_id, r.use_parser, '55 партида 10'
FROM filter_name_rules r
JOIN filter_attributes a ON a.id = r.attribute_id AND a.origin = 'MANUAL' AND a.slug IN ('cpu-series', 'gpu-model')
WHERE r.category_id = 37;

INSERT INTO filter_name_rules (attribute_id, category_id, pattern, value_id, note)
SELECT a.id, r.category_id, r.pattern, v.id, '55 партида 10'
FROM (VALUES
    ('mount-load', 140::BIGINT, 'До 15 кг',   '(^|[^0-9.])([1-9]|1[0-5])([.,][0-9]+)? ?(kg|кг)([^[:alnum:]]|$)'),
    ('mount-load', 140, '16 – 30 кг', '(^|[^0-9.])(1[6-9]|2[0-9]|30)([.,][0-9]+)? ?(kg|кг)([^[:alnum:]]|$)'),
    ('mount-load', 140, '31 – 50 кг', '(^|[^0-9.])(3[1-9]|4[0-9]|50)([.,][0-9]+)? ?(kg|кг)([^[:alnum:]]|$)'),
    ('mount-load', 140, 'Над 50 кг',  '(^|[^0-9.])(5[1-9]|[6-9][0-9]|[1-9][0-9]{2})([.,][0-9]+)? ?(kg|кг)([^[:alnum:]]|$)'),
    ('mount-load', 51,  'До 15 кг',   '(^|[^0-9.])([1-9]|1[0-5])([.,][0-9]+)? ?(kg|кг)([^[:alnum:]]|$)'),
    ('mount-load', 51,  '16 – 30 кг', '(^|[^0-9.])(1[6-9]|2[0-9]|30)([.,][0-9]+)? ?(kg|кг)([^[:alnum:]]|$)'),
    ('mount-for', 51, 'Монитор', '^(?!.*(2 монитора|dual|double|двоен)).*(монитор|monitor)'),
    ('nvr-channels', 962, '4',  '(^|[^0-9])4 ?[xх]? ?-?канал'),
    ('nvr-channels', 962, '8',  '(^|[^0-9])8 ?[xх]? ?-?канал'),
    ('nvr-channels', 962, '16', '(^|[^0-9])16 ?[xх]? ?-?канал'),
    ('nvr-channels', 962, '32', '(^|[^0-9])32 ?[xх]? ?-?канал'),
    ('nvr-channels', 962, '64', '(^|[^0-9])64 ?[xх]? ?-?канал'),
    ('nvr-hdd', 962, '1', '(^|[^0-9])1 ?[xх]? ?hdd'),
    ('nvr-hdd', 962, '2', '(^|[^0-9])2 ?[xх]? ?hdd'),
    ('nvr-hdd', 962, '4', '(^|[^0-9])4 ?[xх]? ?hdd'),
    ('nvr-hdd', 962, '8', '(^|[^0-9])8 ?[xх]? ?hdd'),
    ('poe', 962, 'Да', '(poe|power over ethernet)'),
    -- Настолни компютри: RAM само пред DDR/RAM/запетая („32GB DDR4“, „16 GB,“), диск само пред NVMe/SSD —
    -- „RTX 3050 8GB“ е видео памет.
    ('ram-size', 32, '8',  '(^|[^0-9])8 ?gb ?(ddr|ram|lpddr|,)'),
    ('ram-size', 32, '16', '(^|[^0-9])16 ?gb ?(ddr|ram|lpddr|,)'),
    ('ram-size', 32, '32', '(^|[^0-9])32 ?gb ?(ddr|ram|lpddr|,)'),
    ('ram-size', 32, '64', '(^|[^0-9])64 ?gb ?(ddr|ram|lpddr|,)'),
    ('disk-capacity', 32, '256',  '(^|[^0-9])256 ?gb ?(nvme|ssd|m\.2|pcie)'),
    ('disk-capacity', 32, '512',  '(^|[^0-9])512 ?gb ?(nvme|ssd|m\.2|pcie)'),
    ('disk-capacity', 32, '1000', '(^|[^0-9.])1 ?tb ?(nvme|ssd|m\.2|pcie|hdd)'),
    ('disk-capacity', 32, '2000', '(^|[^0-9.])2 ?tb ?(nvme|ssd|m\.2|pcie|hdd)'),
    ('operating-system', 32, 'Windows', '(windows|(^|[^[:alnum:]])win ?1[01])'),
    ('operating-system', 32, 'Без ОС',  '(free ?dos|no os|без ос)'),
    ('battery-voltage', 203, '12 V', '(^|[^0-9.])12 ?v([^[:alnum:]]|$)'),
    ('battery-voltage', 203, '6 V',  '(^|[^0-9.])6 ?v([^[:alnum:]]|$)'),
    ('ups-battery-capacity', 203, 'До 10 Ah',   '(^|[^0-9.])([0-9]|10)([.,][0-9]+)? ?ah([^[:alnum:]]|$)'),
    ('ups-battery-capacity', 203, '11 – 50 Ah', '(^|[^0-9.])(1[1-9]|[2-4][0-9]|50)([.,][0-9]+)? ?ah([^[:alnum:]]|$)'),
    ('ups-battery-capacity', 203, 'Над 50 Ah',  '(^|[^0-9.])(5[1-9]|[6-9][0-9]|[1-9][0-9]{2})([.,][0-9]+)? ?ah([^[:alnum:]]|$)')
) AS r(slug, category_id, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- Брой портове на хъба от името („4 порта“, „5-портов“).
INSERT INTO filter_name_rules (attribute_id, category_id, use_parser, note)
SELECT a.id, 58, TRUE, '55 партида 10'
FROM filter_attributes a WHERE a.slug = 'port-count' AND a.origin = 'MANUAL';

-- ── 6. Филтрите на категориите ───────────────────────────────────────────────
INSERT INTO category_filter_settings (category_id, mode)
VALUES (140, 'MANUAL'), (51, 'MANUAL'), (32, 'MANUAL'), (115, 'MANUAL'), (58, 'MANUAL'), (237, 'MANUAL'),
       (962, 'MANUAL'), (203, 'MANUAL')
ON CONFLICT (category_id) DO UPDATE SET mode = 'MANUAL';

INSERT INTO category_filters (category_id, attribute_id, sort_order, visible, origin)
SELECT f.category_id, a.id, f.ord, TRUE, 'MANUAL'
FROM (VALUES
    (140::BIGINT, 'mount-for', 5), (140, 'mount-location', 10), (140, 'mount-motion', 20), (140, 'max-screen', 30),
    (140, 'vesa', 40), (140, 'mount-load', 50), (140, 'colour', 60),
    (51, 'mount-for', 5), (51, 'mount-location', 10), (51, 'mount-motion', 20), (51, 'max-screen', 30),
    (51, 'vesa', 40), (51, 'mount-load', 50),
    (32, 'pc-type', 5), (32, 'cpu-series', 10), (32, 'ram-size', 20), (32, 'disk-capacity', 30), (32, 'gpu-model', 40),
    (32, 'operating-system', 50),
    (115, 'projector-resolution', 10), (115, 'projector-brightness', 20), (115, 'light-source', 30),
    (58, 'port-count', 10), (58, 'usb-standard', 20), (58, 'hub-extras', 30), (58, 'colour', 40),
    (237, 'camera-type', 10), (237, 'camera-resolution', 20), (237, 'camera-lens', 30),
    (237, 'night-vision-range', 40), (237, 'video-protocol', 50),
    (962, 'nvr-channels', 10), (962, 'nvr-hdd', 20), (962, 'poe', 30),
    (203, 'battery-voltage', 10), (203, 'ups-battery-capacity', 20)
) AS f(category_id, slug, ord)
JOIN filter_attributes a ON a.slug = f.slug AND a.origin = 'MANUAL'
ON CONFLICT (category_id, attribute_id) DO UPDATE
    SET sort_order = EXCLUDED.sort_order, visible = TRUE, origin = 'MANUAL';

-- Невалиден регулярен израз спира целия rebuild (и нощния) — всеки израз се компилира тук, преди COMMIT.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT pattern FROM filter_value_rules WHERE note = '55 партида 10' AND pattern IS NOT NULL
             UNION ALL
             SELECT pattern FROM filter_name_rules WHERE note = '55 партида 10' AND pattern IS NOT NULL
    LOOP
        PERFORM '' ~* r.pattern;
    END LOOP;
END $$;

DO $$
DECLARE n INT;
BEGIN
    SELECT count(*) INTO n FROM category_filters
    WHERE category_id IN (140, 51, 32, 115, 58, 237, 962, 203) AND origin = 'MANUAL';
    IF n <> 36 THEN
        RAISE EXCEPTION 'Очаквах 36 групи в осемте категории, получих %.', n;
    END IF;
    RAISE NOTICE 'Партида 10 записана: % групи в 8 категории. Следва POST /api/admin/filters/rebuild.', n;
END $$;

COMMIT;

-- ── Контрола (само четене) ───────────────────────────────────────────────────
SELECT c.name_bg AS category, string_agg(a.name_bg, ' | ' ORDER BY cf.sort_order) AS filters
FROM category_filters cf
JOIN categories c ON c.id = cf.category_id
JOIN filter_attributes a ON a.id = cf.attribute_id
WHERE cf.category_id IN (140, 51, 32, 115, 58, 237, 962, 203) AND cf.visible
GROUP BY c.name_bg ORDER BY c.name_bg;
