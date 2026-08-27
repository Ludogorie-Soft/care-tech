# План: Консолидация на филтър параметри (SQL-базиран)

> Статус: **ОДОБРЕН — скриптовете са генерирани**
> Дата: 2026-08-26
> Заменя: `PARAMETER_MAPPING_PLAN.md` (стар план — реализиран частично, Фаза 4 НЕ е имплементирана)

---

## Защо нов план

Старият план (`PARAMETER_MAPPING_PLAN.md`) добавяше mapping слой:
`parameters → distributor_parameter_mapping → caretech_parameters`

**Проблемите на стария подход:**
1. **Options НЕ са дедуплицирани** — "Черен" продължаваше да съществува в 81 отделни `parameter_options` реда под 81 различни parameter ID-та. Phase B (value normalization) беше "отложена, опционална".
2. **91 mapping реда за "Цвят"** — AI-assisted matching за всеки от 91-те VALI "Цвят" params.
3. **JOIN overhead** в ProductSearchRepository при всеки filter query.
4. **Canonical изборът** беше по max products — избра printer/toner param (id=6032 "Цвят" с опции "Yellow", "Magenta") вместо най-свързания с реални продуктови категории.

**Текущо състояние на стария план:**
- Фаза A (DB миграции V37-V39): изпълнена — таблиците `caretech_parameters` и `distributor_parameter_mapping` съществуват
- Фаза 1 (entities/repos): изпълнена
- **Фаза 4 (промени в search queries): НЕ е имплементирана** — кодът все още query-ва директно `parameters`/`parameter_options`

---

## Анализ на данните (от CSV snapshot 2026-08-26)

### Мащаб на дублиранията

| Платформа | Общо параметри | Дублирани групи | Излишни rows |
|-----------|---------------|-----------------|--------------|
| VALI | 1690 | 203 | 810 |
| ASBIS | 2008 | 78 | 81 |
| MOST | 130 | 2 | 2 |
| TEKRA | 41 | **0** | 0 |
| **ОБЩО** | **3872** | **283** | **893** |

### Топ дублирания (VALI)

| Параметър | Брой дублирани rows | Засегнати продукта |
|-----------|--------------------|--------------------|
| Цвят | 91 | ~2851 |
| Други | 75 | ~1449 |
| Размери | 53 | ~1007 |
| Тип аксесоар | 26 | ~987 |
| Размери (мм) | 26 | ~752 |
| Интерфейс | 27 | ~1187 |

### Cross-platform дублирания (след вътрешна консолидация)

52 групи с еднакви имена от различни платформи. Топ 20 се решават от Script 43.

---

## Новият подход: Директна SQL консолидация

**Принцип:** Физически сливаме дублираните редове. Без нови таблици, без mapping слой. Съществуващите queries работят без промяна.

### Правило за избор на canonical параметър

За всяка група с еднакъв `name_bg`:

1. **Максимален брой category linkages** (най-свързан с категории = най-генеричен)
2. Tiebreaker: **максимален брой product usages**
3. Final tiebreaker: **минимален id**

> **Важно:** Canonical може да има "смесени" опции (напр. printer "Yellow" + обичайни цветове). Това е ОК — filter query показва само опции с `HAVING COUNT(products) > 0` за конкретната категория. Admin views ще показват всички опции под canonical.

### Логика за всяка group (PL/pgSQL DO блок)

```sql
DO $$ BEGIN
  -- 1. Re-parent options от non-canonical → canonical (само ако name_bg НЕ съществува в canonical)
  UPDATE parameter_options SET parameter_id = {canonical}
  WHERE parameter_id = ANY({non_canonical_ids})
    AND NOT EXISTS (SELECT 1 FROM parameter_options c
      WHERE c.parameter_id = {canonical}
        AND LOWER(TRIM(c.name_bg)) = LOWER(TRIM(parameter_options.name_bg)));

  -- 2. Redirect product_parameters → canonical option (WHERE name match exists)
  UPDATE product_parameters pp
  SET parameter_id = {canonical}, parameter_option_id = c.id
  FROM parameter_options old_opt
  JOIN parameter_options c ON LOWER(TRIM(c.name_bg)) = LOWER(TRIM(old_opt.name_bg))
    AND c.parameter_id = {canonical}
  WHERE pp.parameter_option_id = old_opt.id
    AND old_opt.parameter_id = ANY({non_canonical_ids});

  -- 3. Update remaining product_parameters (options re-parented в стъпка 1)
  UPDATE product_parameters SET parameter_id = {canonical}
  WHERE parameter_id = ANY({non_canonical_ids});

  -- 4. Dedup product_parameters (при продукт с 2+ Цвят params → след merge са дублирани)
  DELETE FROM product_parameters WHERE id IN (
    SELECT id FROM (SELECT id,
      ROW_NUMBER() OVER (PARTITION BY product_id, parameter_id, parameter_option_id ORDER BY id) rn
      FROM product_parameters WHERE parameter_id = {canonical}) t WHERE rn > 1);

  -- 5. Разширяване на canonical category linkages
  INSERT INTO category_parameters (category_id, parameter_id, is_filter)
  SELECT DISTINCT cp.category_id, {canonical}, cp.is_filter
  FROM category_parameters cp
  WHERE cp.parameter_id = ANY({non_canonical_ids})
    AND NOT EXISTS (SELECT 1 FROM category_parameters
      WHERE category_id = cp.category_id AND parameter_id = {canonical})
  ON CONFLICT (category_id, parameter_id) DO NOTHING;

  -- 6. Изтриване на non-canonical category linkages
  DELETE FROM category_parameters WHERE parameter_id = ANY({non_canonical_ids});

  -- 7. Изтриване на non-canonical parameters (CASCADE → parameter_options → orphaned product_parameters)
  DELETE FROM parameters WHERE id = ANY({non_canonical_ids});
END $$;
```

---

## Скриптове (ред на изпълнение)

| # | Файл | Платформи | Действие | Rows елиминирани |
|---|------|-----------|----------|-----------------|
| 1 | `scripts/40_consolidate_vali_parameters.sql` | VALI | 203 name-групи → по 1 canonical | -810 параметри |
| 2 | `scripts/41_consolidate_asbis_parameters.sql` | ASBIS + MOST | 78+2 name-групи | -81 -2 параметри |
| 3 | `scripts/42_cleanup_is_filter.sql` | Всички | is_filter=false за безполезни filтри | ~97 деактивирани |
| 4 | `scripts/43_crossplatform_parameter_merges.sql` | Cross-platform | Топ 20 кроссплатформени мержа | ~22 параметри |
| 5 | `scripts/44_post_consolidation_cleanup.sql` | Всички | Финален cleanup: casing, empty options | — |

### Как да се изпълни

```bash
# Влез в PostgreSQL:
psql -U <user> -d <db>

# Всеки скрипт в отделна транзакция. Скриптовете са с BEGIN; в началото.
# Последният ред е "-- COMMIT;" (закоментиран). Прегледай VERIFY SELECT-ите,
# след което UNCOMMENT и изпълни COMMIT; ако резултатите са коректни.

\i scripts/40_consolidate_vali_parameters.sql
-- провери броя: SELECT COUNT(*) FROM parameters WHERE platform = 'VALI'; -- трябва да е ~880
-- COMMIT;

\i scripts/41_consolidate_asbis_parameters.sql
-- провери: SELECT COUNT(*) FROM parameters WHERE platform IN ('ASBIS','MOST'); -- ~2009
-- COMMIT;

\i scripts/42_cleanup_is_filter.sql
-- провери: SELECT COUNT(*) FROM parameters WHERE is_filter = true; -- намалял
-- COMMIT;

\i scripts/43_crossplatform_parameter_merges.sql
-- провери: категории с дублирани filter names
-- COMMIT;

\i scripts/44_post_consolidation_cleanup.sql
-- COMMIT;
```

### Финална верификация (след всички скриптове)

```sql
-- Трябва да е 0 — категории с 2+ filter params с еднакъв name_bg
SELECT COUNT(*) FROM (
  SELECT cp.category_id, LOWER(TRIM(p.name_bg)) AS param_name, COUNT(*) AS cnt
  FROM category_parameters cp
  JOIN parameters p ON p.id = cp.parameter_id
  WHERE cp.is_filter = true
  GROUP BY cp.category_id, LOWER(TRIM(p.name_bg))
  HAVING COUNT(*) > 1
) x;

-- Статистика преди/след:
SELECT platform, COUNT(*) FROM parameters GROUP BY platform ORDER BY platform;
SELECT COUNT(*) AS total_options FROM parameter_options;
SELECT COUNT(*) AS is_filter_true FROM parameters WHERE is_filter = true;
```

---

## Какво НЕ се засяга

- **Frontend код** (`Category.jsx`, Redux, filter rendering) — без промени
- **Backend search queries** (`ProductSearchRepository`) — без промени
- **Sync services** (VALI, TEKRA, ASBIS, MOST) — без промени
- **`caretech_parameters` таблица** — съществува, не се трие. При следващ sync новите параметри ще се вмъкват нормално (ON DELETE CASCADE от `distributor_parameter_mapping` ще изчисти старите mappings).

---

## Известни ограничения

1. **"Цвят." vs "Цвят"** (с точка) — VALI има и двете като отделни групи. Script 40 консолидира всяка поотделно, но не ги слива помежду им. Това може да остави 2 отделни filter groups. Решение: ръчен merge след изпълнение или добавяне на normalization в script 44.

2. **Cross-platform options** — Script 43 НЕ слива options по стойност при cross-platform merge (английски "Black" ≠ "Черен"). Потребителят ще вижда смесени стойности в един filter group. Решение: Phase B (value normalization) — ако е необходимо.

3. **Нови параметри от sync** — Следващ sync може да добави нови дублирани параметри. Дългосрочното решение е sync services да търсят canonical по name преди INSERT. Краткосрочно: периодично повторно изпълнение на скриптовете.

---

## Очакван резултат

| Метрика | Преди | След |
|---------|-------|------|
| Категории с дублиращи filter names | 85 | **0** |
| Общо `parameters` rows | 3872 | ~2977 |
| VALI параметри | 1690 | ~880 |
| Безполезни filтри (Размери, Гаранция...) | видими | скрити |
| Промени в backend/frontend код | — | **нито една** |
