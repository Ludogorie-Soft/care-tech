-- ============================================================================
-- 55_filters_curated_13_unmapped_cleanup.sql
--
-- Курация на каноничния филтърен слой, партида 13 — отчетът „Несъпоставени“ (1357 стойности
-- в прод на 2026-09-25) и етикетите на „Яркост“.
--
--   1. „Яркост“ (автоматично свойство): един етикет „N cd/m²“ вместо „cd/m²“, „cd/m2“, „cd/m^2“,
--      подредба по число; „330 cd/㎡“ отива при „330 cd/m²“.
--   2. Истински пропуски → правила: HVA = VA, VESA в обърнат ред (300 x 400 = 400 x 300),
--      размер на таблет с два знака след точката (8.68", 10.36"), касети PGI/CLI на Canon като
--      „Мастилено“ (изключването „gi-[0-9]“ на партида 12 хващаше и „pgi-580“), цветове (антрацид,
--      графит, сребро…), Nexus → Google, Ryzen AI 7 → Ryzen 7, нова стойност „1440p (QHD)“ при
--      кабелите и др.
--   3. Числови свойства — точни правила по текущия отчет: интегрирана графика → нова стойност
--      „Интегрирана графика“, „3200Mbps“ → 3200 MHz, „44 000 dpi“ → 44000 DPI, нови 610 Hz / 1000 Hz.
--   4. Шумът, който остава в отчета (списъци с конектори при захранванията, IEEE стандарти,
--      защити OCP/OVP, „Стандарт“, „USB“ при мишките, дизайнерски имена на цветове…) → точни
--      правила „без стойност“. Само за текстовете, които са в отчета СЕГА: нова непозната стойност
--      на доставчик пак ще се появи в „Несъпоставени“. Нарочно остават: непознати дискретни
--      видеокарти (Arc Pro, RTX A1000, GeForce 210, Radeon AI Pro) и „2.8K“ — за админа.
--
-- ИЗИСКВА: V38–V41, скриптове 55_01…55_12 и поне един успешен rebuild (отчетът се чете от
--          filter_unmapped_values).
-- СЛЕД СКРИПТА: Админ → „Филтри“ → Rebuild (или POST /api/admin/filters/rebuild).
--
-- ИДЕМПОТЕНТНОСТ: правилата-шаблони (note = '55 партида 13') се трият и вмъкват наново. Точните
--   правила по отчета (note = '55 партида 13 — по отчета') НЕ се трият: след rebuild текстовете им
--   вече не са в отчета и нямаше да се създадат пак. Повторно пускане само добавя нови.
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'auto-yarkost') THEN
        RAISE EXCEPTION 'Няма свойство auto-yarkost („Яркост“) — първо rebuild.';
    END IF;
    IF (SELECT count(*) FROM filter_attributes
        WHERE origin = 'MANUAL' AND slug IN ('panel-type', 'vesa', 'print-tech', 'tablet-size', 'colour', 'gpu-model',
                                             'video-resolution', 'refresh-rate', 'memory-speed', 'mouse-dpi',
                                             'cpu-series', 'device-brand', 'cable-connector')) <> 13 THEN
        RAISE EXCEPTION 'Липсва курирано свойство — първо пусни 55_filters_curated_01…12.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM filter_unmapped_values) THEN
        RAISE EXCEPTION 'Отчетът „Несъпоставени“ е празен — първо rebuild.';
    END IF;
END $$;

-- ── 0. Предишно пускане ──────────────────────────────────────────────────────
DELETE FROM filter_value_rules WHERE note = '55 партида 13';

-- ── 1. „Яркост“: един етикет и подредба по число ─────────────────────────────
-- Ключът вече е общ за трите изписвания; грозен беше само етикетът (първото срещнато изписване).
UPDATE filter_values v
SET value_bg   = regexp_replace(v.norm_key, '^([0-9]+)cd/m2$', '\1 cd/m²'),
    value_en   = regexp_replace(v.norm_key, '^([0-9]+)cd/m2$', '\1 cd/m²'),
    sort_order = substring(v.norm_key FROM '^([0-9]+)')::INT,
    origin     = 'MANUAL'
FROM filter_attributes a
WHERE a.id = v.attribute_id AND a.slug = 'auto-yarkost' AND v.norm_key ~ '^[0-9]+cd/m2$';

UPDATE filter_values v
SET value_bg   = regexp_replace(v.norm_key, '^([0-9]+)lm$', '\1 lm'),
    value_en   = regexp_replace(v.norm_key, '^([0-9]+)lm$', '\1 lm'),
    sort_order = substring(v.norm_key FROM '^([0-9]+)')::INT,
    origin     = 'MANUAL'
FROM filter_attributes a
WHERE a.id = v.attribute_id AND a.slug = 'auto-yarkost' AND v.norm_key ~ '^[0-9]+lm$';

UPDATE filter_values v
SET value_bg = '1000 cd/m² (HDR 3%)', value_en = '1000 cd/m² (HDR 3%)', sort_order = 1001, origin = 'MANUAL'
FROM filter_attributes a
WHERE a.id = v.attribute_id AND a.slug = 'auto-yarkost' AND v.norm_key = '1000cd/m2(hdr3%)';

-- „㎡“ е един знак и не се нормализира към „m2“ — отделна стойност за същите 330 cd/m².
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, '^330 ?cd/㎡$', v.id, '55 партида 13'
FROM filter_attributes a JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = '330cd/m2'
WHERE a.slug = 'auto-yarkost';

DELETE FROM filter_values v USING filter_attributes a
WHERE a.id = v.attribute_id AND a.slug = 'auto-yarkost' AND v.norm_key = '330cd/㎡';

-- ── 2. Нови стойности ────────────────────────────────────────────────────────
INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, numeric_value, sort_order, origin)
-- Числовите стойности носят ключа на парсера (самото число); другите — filter_norm(етикет).
SELECT a.id, n.value_bg, n.value_en, COALESCE(n.norm_key, filter_norm(n.value_bg)), n.num, n.sort_order, 'MANUAL'
FROM (VALUES
    ('gpu-model',        'Интегрирана графика', 'Integrated graphics', NULL::TEXT, NULL::NUMERIC, NULL::INT),
    ('refresh-rate',     '610 Hz',              '610 Hz',              '610',      610,           NULL),
    ('refresh-rate',     '1000 Hz',             '1000 Hz',             '1000',     1000,          NULL),
    ('video-resolution', '1440p (QHD)',         '1440p (QHD)',         NULL,       NULL,          2)
) AS n(slug, value_bg, value_en, norm_key, num, sort_order)
JOIN filter_attributes a ON a.slug = n.slug AND a.origin = 'MANUAL'
ON CONFLICT (attribute_id, norm_key) DO NOTHING;

-- 1440p застава между Full HD и 4K.
UPDATE filter_values v SET sort_order = CASE v.norm_key WHEN '4k' THEN 3 WHEN '8k и повече' THEN 4 END
FROM filter_attributes a
WHERE a.id = v.attribute_id AND a.slug = 'video-resolution' AND v.norm_key IN ('4k', '8k и повече');

-- ── 3. Истински пропуски → правила ───────────────────────────────────────────
-- Котвите (^…$) пазят от двойно попадение: всички съвпадащи правила дават стойност, така че
-- „cover“ без котва щеше да сложи „Гръб“ и на калъф-тефтер („flip cover“).
INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
SELECT a.id, r.pattern, v.id, '55 партида 13'
FROM (VALUES
    ('panel-type', 'VA', '(^|[^[:alpha:]])(fast-)?hva([^[:alpha:]]|$)'),
    ('vesa', '400 x 300', '(^|[^0-9])300 ?[x×х*] ?400([^0-9]|$)'),
    ('vesa', '200 x 100', '(^|[^0-9])100 ?[x×х*] ?200([^0-9]|$)'),
    ('vesa', '300 x 200', '(^|[^0-9])200 ?[x×х*] ?300([^0-9]|$)'),
    ('vesa', '400 x 200', '(^|[^0-9])200 ?[x×х*] ?400([^0-9]|$)'),
    ('vesa', '600 x 400', '(^|[^0-9])400 ?[x×х*] ?600([^0-9]|$)'),
    ('vesa', '800 x 400', '(^|[^0-9])400 ?[x×х*] ?800([^0-9]|$)'),
    ('vesa', '800 x 600', '(^|[^0-9])600 ?[x×х*] ?800([^0-9]|$)'),
    ('print-tech', 'Мастилено', '(^|[^[:alnum:]])(pgi|cli|pg|cl)-[0-9]{3}'),
    ('print-tech', 'Лазерно',   '(^|[^[:alnum:]])cartridge 0[0-9]{2}([^0-9]|$)'),
    -- Шаблоните на партида 9 приемат една цифра след точката; 8.68" и 10.36" не влизаха никъде.
    ('tablet-size', 'До 8"',        '((^|[^0-9.])[78][.,][0-9]{2} ?("|”|″|inch|инч)|^[78][.,][0-9]{2}"?$)'),
    ('tablet-size', '9 – 11"',      '((^|[^0-9.])(9|10|11)[.,][0-9]{2} ?("|”|″|inch|инч)|^(9|10|11)[.,][0-9]{2}"?$)'),
    ('tablet-size', '12" и повече', '((^|[^0-9.])1[2-5][.,][0-9]{2} ?("|”|″|inch|инч)|^1[2-5][.,][0-9]{2}"?$)'),
    ('colour', 'Сив',      '^(антрацид|антрацит|графит)$'),
    ('colour', 'Сребрист', '^(гланцово сребро|сребро|slvr|алуминий)$'),
    ('colour', 'Кафяв',    '^[tт]ъмнокафяв$'),
    ('colour', 'Бежов',    '^пясъчен$'),
    ('colour', 'Черен',    '^(noir|carbon)$'),
    ('colour', 'Зелен',    '^ментов[оа]?$'),
    ('colour', 'Циан',     '^turq(uo|ou)ise$'),
    ('colour', 'Златист',  '^champagne$'),
    ('colour', 'Червен',   '^bordeaux$'),
    ('device-brand', 'Google',        'nexus'),
    ('device-brand', 'Nokia / Lumia', 'lumi[as]'),
    ('cable-connector', 'USB-A',      'usb ?3\.[0-9] ?a([^[:alnum:]]|$)'),
    ('cable-connector', 'micro USB',  'usb micro ?-?b'),
    ('cpu-series', 'Ryzen 5', 'ryzen ?ai ?5'),
    ('cpu-series', 'Ryzen 7', 'ryzen ?ai ?7'),
    ('cpu-series', 'Ryzen 9', '(ryzen ?ai ?9|hawk point r9)'),
    ('cpu-series', 'Ryzen 3', 'ryzen 3200g'),
    ('camera-resolution', '3 MP',      '^3 ?\+ ?3 ?mp$'),
    ('camera-resolution', '8 MP (4K)', '^8 ?\+ ?4 ?mp$'),
    ('keyboard-backlight', 'Да', '^neon'),
    ('mouse-sensor', 'Оптичен', '(sunplus|a603ep|marksman|aimpoint|alpha 20k|razer 5g|v-track)'),
    ('anc', 'Да', 'adaptive noise cancel'),
    ('battery-chemistry', 'NiMH', 'nickel ?metal'),
    ('battery-size', 'AAA', '^r0?3$'),
    ('charger-type', 'Безжично', 'индукционн'),
    ('ssd-form-factor', 'M.2 2230', 'm\.2 22 ?x ?30'),
    ('keyboard-size', 'Пълен размер', '^100$'),
    ('thermal-conductivity', 'Над 12 W/mK', '^(1[3-9]|[2-9][0-9])([.,][0-9]+)? ?w ?/? ?m ?k$'),
    ('dual-sim', 'Да', '(две sim|nano sim ?\+ ?nano sim|2 nano)'),
    ('laptop-size', 'До 14"', 'yoga ?2? ?1[13]'),
    ('microphone', 'Да', '^built-?in$'),
    ('surround-sound', 'Да', '^да$'),
    ('ups-power', 'Над 3000 VA', '(^|[^0-9])[1-9][0-9] ?000 ?va'),
    ('ups-topology', 'On-line', 'онлайн'),
    ('phone-type', 'Телефон с копчета', 'series ?30'),
    ('network-gen', '2G / 3G', '^900/1800$'),
    ('card-type', 'SD', 'secure digital'),
    ('mount-for', 'TV', 'плазм'),
    ('height-adjustment', 'Да', '[0-9]+([.,][0-9]+)? ?- ?[0-9]+([.,][0-9]+)? ?cm'),
    ('chair-material', 'Изкуствена кожа', '(leatherette|еко кожа)'),
    ('chair-material', 'Текстил', 'linen'),
    ('memory-type', 'DDR4', '(^|[^[:alnum:]])ddr ?iv([^[:alnum:]]|$)'),
    ('usb-standard', 'USB 2.0', '480 ?mbps'),
    ('speaker-type', 'Домашно аудио', '^soundbar$'),
    ('case-type', 'Гръб', '^((wireless charging )?cover|slim crystal (case|cover)|soft touch case)$'),
    ('video-resolution', '1440p (QHD)', '^(?!.*(4k|8k|2160|4320)).*(1440p|wqhd|(^|[^[:alnum:]])qhd|2560 ?x ?1440)')
) AS r(slug, value, pattern)
JOIN filter_attributes a ON a.slug = r.slug AND a.origin = 'MANUAL'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm(r.value);

-- ── 4. Числови свойства: точни правила по текущия отчет ──────────────────────
-- Шаблоните важат само за свойства с изброени стойности; числовите приемат само точен текст.
INSERT INTO filter_value_rules (attribute_id, raw_norm, value_id, note)
SELECT u.attribute_id, u.raw_norm, v.id, '55 партида 13 — по отчета'
FROM filter_unmapped_values u
JOIN filter_attributes a ON a.id = u.attribute_id AND a.slug = 'gpu-model'
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = filter_norm('Интегрирана графика')
WHERE u.raw_norm ~* '(integrat|iris|uhd graphics|intel graphics|intel arc graphics|arc (graphics|1[0-9]{2}[tv])|xe lpg|mali|aspeed|vega|radeon [0-9]{3,4}[ms]|radeon graphics|^amd radeon;?$)'
  AND u.raw_norm !~* '(arc pro|rtx|geforce|ai pro|rdna)'
ON CONFLICT DO NOTHING;

-- Обикновени GeForce, които парсерът изпуска: без „RTX“ („geforce 3060 ti“) или с „™“ („rtxtm 4050“).
-- Професионалните (RTX A…, RTX PRO) остават за админа.
INSERT INTO filter_value_rules (attribute_id, raw_norm, value_id, note)
SELECT u.attribute_id, u.raw_norm, v.id, '55 партида 13 — по отчета'
FROM filter_unmapped_values u
JOIN filter_attributes a ON a.id = u.attribute_id AND a.slug = 'gpu-model'
JOIN filter_values v ON v.attribute_id = a.id
 AND v.norm_key = 'rtx ' || substring(u.raw_norm FROM 'geforce (?:rtx(?:tm)? ?)?([2-5]0[5-9]0(?: ti)?)(?![0-9])')
WHERE u.raw_norm !~* '(pro|(^|[^[:alnum:]])a[0-9]{4})'
ON CONFLICT DO NOTHING;

INSERT INTO filter_value_rules (attribute_id, raw_norm, value_id, note)
SELECT u.attribute_id, u.raw_norm, v.id, '55 партида 13 — по отчета'
FROM filter_unmapped_values u
JOIN filter_attributes a ON a.id = u.attribute_id
JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = CASE a.slug
        WHEN 'memory-speed' THEN substring(u.raw_norm FROM '^([0-9]{4}) ?mbps$')
        WHEN 'mouse-dpi'    THEN regexp_replace(substring(u.raw_norm FROM '^([0-9]{1,3} [0-9]{3}) ?dpi$'), ' ', '')
        WHEN 'refresh-rate' THEN substring(u.raw_norm FROM '^([0-9]{3,4}) ?hz$')
    END
WHERE a.slug IN ('memory-speed', 'mouse-dpi', 'refresh-rate')
ON CONFLICT DO NOTHING;

-- ── 5. Шумът в отчета → правила „без стойност“ ───────────────────────────────
-- Само текстовете от отчета сега, които не са хванати от правило със стойност (иначе точното
-- правило щеше да засенчи шаблона). Нарочно остават непознатите дискретни видеокарти и „2.8K“.
INSERT INTO filter_value_rules (attribute_id, raw_norm, value_id, note)
SELECT u.attribute_id, u.raw_norm, NULL, '55 партида 13 — по отчета'
FROM filter_unmapped_values u
JOIN filter_attributes a ON a.id = u.attribute_id
WHERE u.raw_norm = filter_norm(u.raw_norm)
  AND NOT EXISTS (SELECT 1 FROM filter_value_rules r
                  WHERE r.attribute_id = u.attribute_id AND r.raw_norm = u.raw_norm)
  AND NOT EXISTS (SELECT 1 FROM filter_value_rules r
                  WHERE r.attribute_id = u.attribute_id AND r.pattern IS NOT NULL AND r.value_id IS NOT NULL
                    AND u.raw_norm ~* r.pattern)
  AND NOT (a.slug = 'gpu-model' AND u.raw_norm ~* '(arc pro|rtx|geforce|ai pro)')
  AND NOT (a.slug = 'screen-resolution' AND u.raw_norm = '2.8k')
ON CONFLICT DO NOTHING;

-- Невалиден регулярен израз спира целия rebuild (и нощния) — всеки израз се компилира тук, преди COMMIT.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT pattern FROM filter_value_rules WHERE note = '55 партида 13' AND pattern IS NOT NULL
    LOOP
        PERFORM '' ~* r.pattern;
    END LOOP;
END $$;

DO $$
DECLARE patterns INT; exact_maps INT; drops INT; remaining INT;
BEGIN
    SELECT count(*) INTO patterns FROM filter_value_rules WHERE note = '55 партида 13';
    SELECT count(*) FILTER (WHERE value_id IS NOT NULL), count(*) FILTER (WHERE value_id IS NULL)
      INTO exact_maps, drops FROM filter_value_rules WHERE note = '55 партида 13 — по отчета';
    SELECT count(*) INTO remaining FROM filter_unmapped_values u
    WHERE NOT EXISTS (SELECT 1 FROM filter_value_rules r WHERE r.attribute_id = u.attribute_id AND r.raw_norm = u.raw_norm)
      AND NOT EXISTS (SELECT 1 FROM filter_value_rules r WHERE r.attribute_id = u.attribute_id AND r.pattern IS NOT NULL
                                                           AND u.raw_norm ~* r.pattern);
    IF patterns <> 60 THEN
        RAISE EXCEPTION 'Очаквах 60 правила-шаблона, вмъкнати са %: някоя стойност не е намерена.', patterns;
    END IF;
    RAISE NOTICE 'Партида 13: % шаблона, % точни правила към стойност, % правила без стойност; в отчета остават ~% (до rebuild).',
        patterns, exact_maps, drops, remaining;
END $$;

COMMIT;

-- Контрола (след COMMIT, само чете): правилата на партидата.
SELECT note, count(*) AS rules, count(*) FILTER (WHERE value_id IS NULL) AS without_value
FROM filter_value_rules WHERE note LIKE '55 партида 13%' GROUP BY note ORDER BY note;
