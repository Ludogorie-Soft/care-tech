-- ============================================================================
-- 55_filters_curated_15_brightness_and_unmapped.sql
--
-- Курация на каноничния филтърен слой, партида 15 — след първите нощни прогони с новия код
-- (проверка от 2026-09-28).
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
--   1. „Яркост“ при „Монитори“ (50 и CCTV двойника 249). От 26.09 MOST праща параметрите и на
--      мониторите, които преди прескачаше, но с английското име „Brightness“ — то ставаше
--      отделна скрита група и 73 монитора липсваха във филтъра. Правило за името само за тези
--      категории (при „Проектори“ „Brightness“ вече отива в „Яркост (lm)“, партида 10) и правила,
--      които свеждат „250 cd/m?“, „300 nits (Typ.)“, „(Typ.) 250 cd/m² (Min.) 200 cd/m²“,
--      „1,300 cd/m2“… до „N cd/m²“ — по първото (типичното) число. Нови стойности 275 и 380 cd/m².
--   2. Отчетът „Несъпоставени“ (63 в прод на 2026-09-28): истинските стойности → правила
--      (HDD 5,400 → 5400 – 5900 rpm, портове 12/28, 144 Hz, 1/5/8 ms, DDR4/DDR5, Micro-ATX,
--      Class 10, 12+4 pin → PCIe 5, проектори по родната резолюция, нови стойности A6 и 1.65 mm).
--      Останалите 39 са шум (списъци с функции на камери, кабели на захранвания, IEEE, exFAT…)
--      → правила „без стойност“, само за текстовете в отчета СЕГА.
--   3. „Гейминг конзоли“ (автоматични групи от 28.09): „не“ = „Без оптично устройство“,
--      „512 GB SSD M.2 2242…“ = „512 GB“, „N/A“ отпада.
--
-- ИЗИСКВА: V38–V42, скриптове 55_01…55_14 и поне един успешен rebuild след 2026-09-28 (отчетът
--          се чете от filter_unmapped_values).
-- СЛЕД СКРИПТА: Админ → „Филтри“ → Rebuild (или POST /api/admin/filters/rebuild).
--
-- ЕДНА КОМАНДА: целият запис е в един DO блок — или минава целият, или нищо. Накрая има COMMIT
--   (SQL редакторът на DBeaver може да е в Manual commit; в auto-commit/psql е безвреден).
--
-- ИДЕМПОТЕНТНОСТ: правилата-шаблони и правилото за името (note = '55 партида 15') се трият и
--   вмъкват наново. Точните правила по отчета (note = '55 партида 15 — по отчета') НЕ се трият:
--   след rebuild текстовете им вече не са в отчета и нямаше да се създадат пак.
-- Дата: 2026-09-28
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

DO $$
DECLARE
    -- Префикс преди типичното число: „typ.:“, „(typ.)“, „typical:“, „sdr:“ или „min. 200 cd/m2 / typ.“.
    prefix CONSTANT TEXT := '^((\(?typ(ical)?\.?\)?|sdr):? ?|min\.? ?[0-9]{2,4} ?cd/m[2?²]? ?/ ?typ(ical)?\.?:? ?)?';
    -- След числото: единица (cd/m², „cd/m?“ с изгубен знак, nits) или „(typ“, или нищо.
    suffix CONSTANT TEXT := ' ?(cd|nits?([^[:alpha:]]|$)|\(typ|$)';
    name_rules INT; new_values INT; brightness_rules INT; fixed_rules INT; exact_maps INT; superseded INT; drops INT;
    compiled RECORD;
BEGIN
    -- ── Проверки ─────────────────────────────────────────────────────────────
    IF NOT EXISTS (SELECT 1 FROM filter_attributes WHERE slug = 'auto-yarkost') THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: няма свойство auto-yarkost („Яркост“) — първо rebuild.';
    END IF;
    IF (SELECT count(*) FROM filter_attributes
        WHERE origin = 'MANUAL' AND slug IN ('board-form-factor', 'card-speed-class', 'connection', 'memory-type',
                                             'port-count', 'refresh-rate', 'response-time', 'read-speed', 'wifi',
                                             'camera-lens', 'paper-size', 'projector-resolution',
                                             'psu-pcie5-connector', 'hdd-rpm')) <> 14 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: липсва курирано свойство — първо пусни 55_filters_curated_01…14.';
    END IF;
    IF (SELECT count(*) FROM filter_attributes
        WHERE slug IN ('auto-optichno-ustroystvo', 'auto-kapatsitet-na-sahranenie')) <> 2 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: няма автоматичните групи на „Гейминг конзоли“ — първо rebuild.';
    END IF;
    IF (SELECT count(*) FROM categories WHERE id IN (50, 249) AND name_bg = 'Монитори') <> 2 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: категориите 50 и 249 не са „Монитори“.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM filter_unmapped_values) THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: отчетът „Несъпоставени“ е празен — първо rebuild.';
    END IF;

    -- ── 0. Предишно пускане ──────────────────────────────────────────────────
    DELETE FROM filter_value_rules WHERE note = '55 партида 15';
    DELETE FROM filter_attribute_sources WHERE note = '55 партида 15';

    -- ── 1. „Яркост“ при мониторите ───────────────────────────────────────────
    INSERT INTO filter_attribute_sources (action, attribute_id, name_norm, category_id, origin, note)
    SELECT 'MAP', a.id, 'brightness', c.id, 'MANUAL', '55 партида 15'
    FROM filter_attributes a CROSS JOIN (VALUES (50), (249)) AS c(id)
    WHERE a.slug = 'auto-yarkost';
    GET DIAGNOSTICS name_rules = ROW_COUNT;

    -- Стойност за всяко типично число, което мониторите дават (сега: 275 и 380 са нови).
    INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
    SELECT DISTINCT a.id, x.n || ' cd/m²', x.n || ' cd/m²', x.n || 'cd/m2', x.n::INT, 'MANUAL'
    FROM parameter_options po
    JOIN parameters p ON p.id = po.parameter_id
    CROSS JOIN LATERAL (SELECT regexp_replace(substring(filter_norm(po.name_bg) FROM
            '^(?:(?:\(?typ(?:ical)?\.?\)?|sdr):? ?|min\.? ?[0-9]{2,4} ?cd/m[2?²]? ?/ ?typ(?:ical)?\.?:? ?)?'
            || '([0-9][,.]?[0-9]{2,3}) ?(?:cd|nits?(?:[^[:alpha:]]|$)|\(typ|$)'), '[,.]', '') AS n) x
    JOIN filter_attributes a ON a.slug = 'auto-yarkost'
    WHERE filter_norm(p.name_bg) IN ('brightness', 'яркост')
      AND x.n IS NOT NULL
      AND EXISTS (SELECT 1 FROM product_parameters pp JOIN products pr ON pr.id = pp.product_id
                  WHERE pp.parameter_option_id = po.id AND pr.category_id IN (50, 249) AND NOT pr.deleted)
    ON CONFLICT (attribute_id, norm_key) DO NOTHING;
    GET DIAGNOSTICS new_values = ROW_COUNT;

    -- Шаблон за всяка стойност „N cd/m²“: текстът започва с N (след „typ.“ и сродни), N е цялото
    -- число (котвата пази 250 от „2500“), хилядите може да са с „,“ или „.“. Първото число решава,
    -- затова „350 … (typical) / 400 … (peak)“ дава само 350. Лумените („500 lm“) не се хващат.
    INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
    SELECT v.attribute_id,
           prefix || regexp_replace(substring(v.norm_key FROM '^([0-9]+)'), '^([0-9])([0-9]{3})$', '\1[,.]?\2') || suffix,
           v.id, '55 партида 15'
    FROM filter_values v
    JOIN filter_attributes a ON a.id = v.attribute_id AND a.slug = 'auto-yarkost'
    WHERE v.norm_key ~ '^[0-9]+cd/m2$';
    GET DIAGNOSTICS brightness_rules = ROW_COUNT;

    -- ── 2. Нови стойности ────────────────────────────────────────────────────
    INSERT INTO filter_values (attribute_id, value_bg, value_en, norm_key, sort_order, origin)
    SELECT a.id, n.value_bg, n.value_bg, n.norm_key, n.sort_order, 'MANUAL'
    FROM (VALUES
        ('camera-lens', '1.65 mm', filter_norm('1.65 mm'), 0),                   -- рибешко око, преди 2.8 mm
        ('paper-size',  'A6',      filter_norm('A6'),      3),
        ('auto-kapatsitet-na-sahranenie', '512 GB', filter_value_key('512 GB'), NULL)
    ) AS n(slug, value_bg, norm_key, sort_order)
    JOIN filter_attributes a ON a.slug = n.slug
    ON CONFLICT (attribute_id, norm_key) DO NOTHING;

    -- ── 3. Шаблони за бъдещи изписвания ──────────────────────────────────────
    INSERT INTO filter_value_rules (attribute_id, pattern, value_id, note)
    SELECT a.id, r.pattern, v.id, '55 партида 15'
    FROM (VALUES
        -- Хилядите със запетая („5,400“) — шаблоните на партида 5 искат 5400 слято.
        ('hdd-rpm', '(^|[^0-9])5[,.][4-9][0-9]{2}( ?rpm)?([^0-9]|$)', '5400 – 5900 rpm'),
        ('hdd-rpm', '(^|[^0-9])7[,.]200( ?rpm)?([^0-9]|$)',           '7200 rpm'),
        ('card-speed-class', '^10$', 'class 10'),
        -- 12+4 pin = 12VHPWR, само в списъците с кабели на захранванията.
        ('psu-pcie5-connector', '(^|[^0-9])12 ?\+ ?4 ?-?pin', 'да'),
        ('auto-kapatsitet-na-sahranenie', '^512 ?gb', '512gb')
    ) AS r(slug, pattern, value_key)
    JOIN filter_attributes a ON a.slug = r.slug
    JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = r.value_key;
    GET DIAGNOSTICS fixed_rules = ROW_COUNT;

    -- ── 4. Точни правила по отчета ───────────────────────────────────────────
    -- Числовите свойства приемат само точен текст, не шаблон. Селекторът избира реда от отчета.
    INSERT INTO filter_value_rules (attribute_id, raw_norm, value_id, note)
    SELECT u.attribute_id, u.raw_norm, v.id, '55 партида 15 — по отчета'
    FROM filter_unmapped_values u
    JOIN filter_attributes a ON a.id = u.attribute_id
    JOIN (VALUES
        ('board-form-factor', '^24[.,]4 ?cm ?x ?22[.,]9 ?cm$', 'micro-atx'),
        ('connection', '^usb 2\.0; usb speed: full speed$', 'кабелна'),
        ('memory-type', '^up to 3200 ?mhz$', 'ddr4'),
        ('memory-type', '^memory channels: ?2 max memory speed: 1x1r-5200', 'ddr5'),
        ('port-count', '^12$', '12'),
        ('port-count', '^28$', '28'),
        ('refresh-rate', '^144 \(o/c\)$', '144'),
        -- Мониторите изписват най-бързия режим като време за реакция.
        ('response-time', '^8m \(g to g\)$', '8'),
        ('response-time', '^gtg\(5,3,1\)ms$', '1'),
        ('response-time', '^od（10,7,5）$', '5'),
        ('read-speed', '^seq read/write rate up to 95/90 \(mb/s\)', 'from-0'),
        -- Само слот за карта — самата платка е без Wi-Fi.
        ('wifi', 'm\.2 slot only \(key e', 'не'),
        ('camera-lens', '^1[.,]65 ?mm$', '1.65 mm'),
        ('paper-size', '^a6$', 'a6'),
        -- Проекторите — по родната резолюция, не по максималната.
        ('projector-resolution', '^native resolution: ?1920 ?x ?1200', 'full hd (1080p)'),
        ('projector-resolution', '^native resolution: ?1280 ?x ?720', 'wxga (1280x800)'),
        ('projector-resolution', '^native resolution: ?854 ?x ?480', 'svga (800x600)')
    ) AS m(slug, selector, value_key) ON m.slug = a.slug AND u.raw_norm ~ m.selector
    JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = m.value_key
    ON CONFLICT DO NOTHING;
    GET DIAGNOSTICS exact_maps = ROW_COUNT;

    -- „Гейминг конзоли“ — автоматичните групи нямат отчет, текстовете са известни.
    INSERT INTO filter_value_rules (attribute_id, raw_norm, value_id, note)
    SELECT a.id, r.raw_norm, v.id, '55 партида 15 — по отчета'
    FROM (VALUES
        ('auto-optichno-ustroystvo', 'не', filter_value_key('Без оптично устройство')),
        ('auto-kapatsitet-na-sahranenie', 'n/a', NULL)
    ) AS r(slug, raw_norm, value_key)
    JOIN filter_attributes a ON a.slug = r.slug
    LEFT JOIN filter_values v ON v.attribute_id = a.id AND v.norm_key = r.value_key
    WHERE r.value_key IS NULL OR v.id IS NOT NULL
    ON CONFLICT DO NOTHING;

    -- Точно правило „без стойност“ от по-ранна партида засенчва новите шаблони (точното печели) —
    -- новото решение е с предимство. В прод на 2026-09-28 няма такива; пази повторни пускания.
    DELETE FROM filter_value_rules d
    WHERE d.value_id IS NULL AND d.raw_norm IS NOT NULL AND d.note NOT LIKE '55 партида 15%'
      AND (EXISTS (SELECT 1 FROM filter_value_rules p
                   WHERE p.note = '55 партида 15' AND p.attribute_id = d.attribute_id AND d.raw_norm ~* p.pattern)
           OR EXISTS (SELECT 1 FROM filter_value_rules e
                      WHERE e.note = '55 партида 15 — по отчета' AND e.value_id IS NOT NULL
                        AND e.attribute_id = d.attribute_id AND e.raw_norm = d.raw_norm));
    GET DIAGNOSTICS superseded = ROW_COUNT;

    -- ── 5. Шумът в отчета → правила „без стойност“ ───────────────────────────
    -- Само текстовете от отчета сега, които не са хванати от правило със стойност (иначе точното
    -- правило щеше да засенчи шаблона).
    INSERT INTO filter_value_rules (attribute_id, raw_norm, value_id, note)
    SELECT u.attribute_id, u.raw_norm, NULL, '55 партида 15 — по отчета'
    FROM filter_unmapped_values u
    WHERE u.raw_norm = filter_norm(u.raw_norm)
      AND NOT EXISTS (SELECT 1 FROM filter_value_rules r
                      WHERE r.attribute_id = u.attribute_id AND r.raw_norm = u.raw_norm)
      AND NOT EXISTS (SELECT 1 FROM filter_value_rules r
                      WHERE r.attribute_id = u.attribute_id AND r.pattern IS NOT NULL AND r.value_id IS NOT NULL
                        AND u.raw_norm ~* r.pattern)
    ON CONFLICT DO NOTHING;
    GET DIAGNOSTICS drops = ROW_COUNT;

    -- Невалиден регулярен израз спира целия rebuild (и нощния) — всеки израз се компилира тук.
    FOR compiled IN SELECT pattern FROM filter_value_rules WHERE note = '55 партида 15' AND pattern IS NOT NULL
    LOOP
        PERFORM '' ~* compiled.pattern;
    END LOOP;

    IF name_rules <> 2 OR fixed_rules <> 5 OR brightness_rules < 30 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: очаквах 2 правила за име, 5 шаблона и поне 30 шаблона за яркост; има %, %, %.',
            name_rules, fixed_rules, brightness_rules;
    END IF;

    RAISE NOTICE 'Партида 15: % шаблона за яркост (% нови стойности), % други шаблона, % точни правила по отчета, % без стойност (% стари заменени). Следва Rebuild.',
        brightness_rules, new_values, fixed_rules, exact_maps, drops, superseded;
END $$;

-- Контрола (само чете): правилата на партидата и какво остава в отчета до rebuild.
SELECT note, count(*) AS rules, count(*) FILTER (WHERE value_id IS NULL) AS without_value
FROM filter_value_rules WHERE note LIKE '55 партида 15%' GROUP BY note ORDER BY note;

SELECT count(*) AS unmapped_left_until_rebuild
FROM filter_unmapped_values u
WHERE NOT EXISTS (SELECT 1 FROM filter_value_rules r WHERE r.attribute_id = u.attribute_id AND r.raw_norm = u.raw_norm)
  AND NOT EXISTS (SELECT 1 FROM filter_value_rules r WHERE r.attribute_id = u.attribute_id AND r.pattern IS NOT NULL
                                                       AND u.raw_norm ~* r.pattern);

-- В DBeaver с Manual commit горното чака потвърждение и след 60 s сървърът го отменя.
-- В auto-commit/psql COMMIT е безвреден.
COMMIT;

-- ============================================================================
-- ОТКАТ:
--   DELETE FROM filter_value_rules WHERE note LIKE '55 партида 15%';
--   DELETE FROM filter_attribute_sources WHERE note = '55 партида 15';
--   (новите стойности 275/380 cd/m², A6, 1.65 mm, 512 GB остават безвредни; после Rebuild)
-- ============================================================================
