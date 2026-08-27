# Анализ на всички DB връзки — преди имплементация

> Дата: 2026-08-21
> Цел: Пълен одит на всички таблици, FK constraints и CASCADE поведения преди
>      стартиране на CATEGORY_MAPPING_PLAN и PARAMETER_MAPPING_PLAN.
> Прочетено: V1–V34 migrations, всички entities, CategoryRepository,
>             ProductRepository, ProductSearchService, ProductSearchRepository.

---

## Секция 1: Пълна ER диаграма (текстова)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           СЪЩЕСТВУВАЩИ ТАБЛИЦИ                              │
└─────────────────────────────────────────────────────────────────────────────┘

users ──────────────────────────────────────────────────────────────────────────
  ├──[CASCADE]──► cart_items (user_id)
  ├──[CASCADE]──► user_favorites (user_id)
  └──[RESTRICT]──► orders (user_id)   ← RESTRICT: не може да се изтрие user с поръчки

orders ─────────────────────────────────────────────────────────────────────────
  ├──[CASCADE]──► order_items (order_id)
  └──[SET NULL]──► leasing_applications (order_id)

categories ─────────────────────────────────────────────────────────────────────
  ├──[SET NULL]──► categories.parent_id (самоотносяща се)
  ├──[SET NULL]──► categories.alias_of_id (самоотносяща се — alias механизъм)
  ├──[SET NULL]──► products.category_id
  ├──[CASCADE]──► category_parameters.category_id
  └──[CASCADE]──► [НОВО] distributor_category_mapping.distributor_category_id

manufacturers ──────────────────────────────────────────────────────────────────
  └──[SET NULL]──► products.manufacturer_id

products ───────────────────────────────────────────────────────────────────────
  ├──[CASCADE]──► product_parameters (product_id)
  ├──[CASCADE]──► product_flags (product_id)
  ├──[CASCADE]──► additional_images (product_id)
  ├──[CASCADE]──► user_favorites (product_id)
  ├──[CASCADE]──► cart_items (product_id)
  └──[RESTRICT]──► order_items.product_id  ← КРИТИЧНО! Не може да се изтрие поръчан продукт

parameters ─────────────────────────────────────────────────────────────────────
  ├──[CASCADE]──► parameter_options (parameter_id)
  ├──[CASCADE]──► product_parameters (parameter_id)
  ├──[CASCADE]──► category_parameters (parameter_id)
  └──[CASCADE]──► [НОВО] distributor_parameter_mapping.distributor_parameter_id

parameter_options ──────────────────────────────────────────────────────────────
  └──[CASCADE]──► product_parameters (parameter_option_id)

┌─────────────────────────────────────────────────────────────────────────────┐
│                              НОВИ ТАБЛИЦИ                                   │
└─────────────────────────────────────────────────────────────────────────────┘

caretech_categories ────────────────────────────────────────────────────────────
  ├──[SET NULL]──► caretech_categories.parent_id (самоотносяща се)
  └──[CASCADE]──► distributor_category_mapping.caretech_category_id

distributor_category_mapping ───────────────────────────────────────────────────
  ├──► categories(id) ON DELETE CASCADE
  └──► caretech_categories(id) ON DELETE CASCADE

caretech_parameters ────────────────────────────────────────────────────────────
  └──[CASCADE]──► distributor_parameter_mapping.caretech_parameter_id

distributor_parameter_mapping ──────────────────────────────────────────────────
  ├──► parameters(id) ON DELETE CASCADE
  └──► caretech_parameters(id) ON DELETE CASCADE

ai_parameter_suggestions ───────────────────────────────────────────────────────
  ├──► parameters(id) ON DELETE CASCADE
  └──► caretech_parameters(id) ON DELETE SET NULL  ← виж RISK-08
```

---

## Секция 2: Всички FK Constraints — пълна таблица

| От таблица | FK колона | Към таблица | ON DELETE | Бележка |
|-----------|-----------|------------|-----------|---------|
| categories | parent_id | categories | SET NULL | Самоотносяща се |
| categories | alias_of_id | categories | SET NULL | Alias механизъм (V32) |
| products | category_id | categories | SET NULL | Продуктите се orphan-ват при изтрит category |
| products | manufacturer_id | manufacturers | SET NULL | |
| category_parameters | category_id | categories | CASCADE | При изтрит category → category_parameters изчезват |
| category_parameters | parameter_id | parameters | CASCADE | При изтрит parameter → category_parameters изчезват |
| parameter_options | parameter_id | parameters | CASCADE | |
| product_parameters | product_id | products | CASCADE | |
| product_parameters | parameter_id | parameters | CASCADE | |
| product_parameters | parameter_option_id | parameter_options | CASCADE | |
| product_flags | product_id | products | CASCADE | |
| additional_images | product_id | products | CASCADE | |
| cart_items | user_id | users | CASCADE | |
| cart_items | product_id | products | CASCADE | |
| user_favorites | user_id | users | CASCADE | |
| user_favorites | product_id | products | CASCADE | |
| orders | user_id | users | RESTRICT | |
| order_items | order_id | orders | CASCADE | |
| order_items | product_id | products | **RESTRICT** | ⚠️ Критично! |
| leasing_applications | order_id | orders | SET NULL | |
| **[НОВО]** distributor_category_mapping | distributor_category_id | categories | CASCADE | |
| **[НОВО]** distributor_category_mapping | caretech_category_id | caretech_categories | CASCADE | |
| **[НОВО]** caretech_categories | parent_id | caretech_categories | SET NULL | |
| **[НОВО]** distributor_parameter_mapping | distributor_parameter_id | parameters | CASCADE | |
| **[НОВО]** distributor_parameter_mapping | caretech_parameter_id | caretech_parameters | CASCADE | |
| **[НОВО]** ai_parameter_suggestions | distributor_parameter_id | parameters | CASCADE | |
| **[НОВО]** ai_parameter_suggestions | suggested_caretech_param_id | caretech_parameters | **SET NULL** | ⚠️ Трябва SET NULL, не CASCADE |

---

## Секция 3: Колони с is_filter — пълна история

```
V2:  ALTER TABLE parameters ADD COLUMN is_filter BOOLEAN NOT NULL DEFAULT TRUE;
     → Глобален флаг, per-parameter

V6:  ALTER TABLE category_parameters ADD COLUMN is_filter BOOLEAN NOT NULL DEFAULT TRUE;
     → Per-category флаг. V6 казва: "The global flag remains but is no longer used
       by the filter queries after this migration."
     → category_parameters.is_filter е SOURCE OF TRUTH от V6 нататък

V7:  UPDATE category_parameters SET is_filter = false;  ← Reset ВСИЧКИ на false
     UPDATE ... SET is_filter = true WHERE (категория VALI, параметър VALI)
     → Само VALI категории (по external_id) са curated
     → TEKRA/ASBIS/MOST категории, добавени СЛЕД V7, ще имат is_filter = TRUE (default)

V33: UPDATE category_parameters SET is_filter = true
     WHERE is_filter IS NULL AND parameter има 2–50 options
     → Работи само за category_parameters добавени СЛЕД V7 с is_filter = NULL
     → На практика почти нищо не се засяга (V6 default е TRUE, не NULL)
```

**Важно за плана:** При CareTech facet query, `cp.is_filter != false` ще включи ВСИЧКИ
TEKRA/ASBIS параметри (защото са с default TRUE от V6). Качеството на CareTech филтрите
ще зависи изцяло от `caretech_parameters.is_filter` — admin трябва да го управлява.

---

## Секция 4: Идентифицирани рискове — пълен списък

### RISK-01 ✅ РЕШЕН В ПЛАНА: `Category.isParentCategory()` грешна семантика
**Описание:** `Category.isParentCategory()` в Category.java:81 = `return parent == null;`
Това означава "дали е root категория", НЕ "дали е parent-на-деца".
**Влияние:** `createMapping()` трябва да проверява видими деца чрез repo query, не entity метода.
**Статус:** Коригирано в CATEGORY_MAPPING_PLAN (createMapping валидация) ✅

---

### RISK-02 ✅ АВТОМАТИЧНО РЕШЕН: Alias категории в `getProductsByCaretech`
**Описание:** `categories` таблицата има `alias_of_id` (V32). Alias категория няма свои продукти —
те се сервират от target категорията. Ако дистрибуторска leaf категория е alias, продуктите
няма да се намерят ако я подадем директно в SQL WHERE.

**Ключово откритие:** `ProductSearchService.searchProducts()` ВЕЧЕ вика `resolveAliasCategories()`
АВТОМАТИЧНО преди търсене:
```java
if (request.getCategories() != null && !request.getCategories().isEmpty()) {
    request.setCategories(resolveAliasCategories(request.getCategories()));
}
```
`resolveAliasId()`: `c.getAliasOf() != null ? c.getAliasOf().getId() : c.getId()`

**Заключение:** Когато `getProductsByCaretech` викa `searchProducts(request)` с листа от
дистрибуторски category IDs, alias resolution СЕ ИЗВЪРШВА АВТОМАТИЧНО. Нищо допълнително
не трябва да се прави в нашия нов код.

**ОБАЧЕ — добавяме предупреждение при mapping:**
```
createMapping() ако distributorCategory.aliasOf != null:
→ НЕ блокирай, но логвай warning
→ Admin UI показва badge [ALIAS → Target: {targetCategoryName}]
  за да знае admin, че продуктите идват от друго място
```

---

### RISK-03 ✅ ВЕЧЕ РЕШЕНО: Cross-platform SKU дедупликация
**Описание:** `deduplicateCrossPlatformBySku()` прави: ако един продукт съществува в VALI
и TEKRA с еднакъв SKU → само по-евтиният е `show_flag=true`.

**Заключение:** Когато CareTech категория събира продукти от VALI "Лаптопи" + TEKRA "Laptops",
дублираните продукти вече са скрити от sync-а. CareTech системата просто ще търси по
дистрибуторски category IDs и ще вижда само видимите (не-дублирани) продукти. ✅

---

### RISK-04 ⚠️ КОРЕКЦИЯ В ПЛАН: `category_parameters` CASCADE при изтрит category
**Описание:** Ако дистрибуторска категория се изтрие (hard delete) от sync-а:
- `category_parameters` за нея → CASCADE DELETE ✅
- `distributor_category_mapping` за нея → CASCADE DELETE ✅ (заложено в плана)
- Продукти с тази категория → `category_id = NULL` (SET NULL)

**Риск:** Sync services НЕ hard-delete категории — само update `show_flag = false`.
На практика CASCADE няма да се задейства. Но все пак:

**Добавяме в CaretechCategoryService:**
```
Ако sync изтрие дистрибуторска категория (CASCADE → mapping изчезва):
→ CareTech категорията ОСТАВА но ще има 1 по-малко mapping
→ Admin трябва да провери "Unmapped ≥ 1" badge в CategoryMappingLayout
→ Не е нужна допълнителна логика — CASCADE се грижи за clean-up
```

---

### RISK-05 ✅ ВЕЧЕ В ПЛАНА: Seed guard
**Описание:** Seed може да се пусне само веднъж.
**Guard:** `caretechRepo.count() > 0` → хвърля грешка.
**Важно:** Ползваме `count()` (не `countByShowFlagTrue()`) за да блокираме дори ако
всички са hidden. Ако admin скрие всички CareTech категории → seed НЕ трябва да се позволи
пак (това би дублирало данните).
**Статус:** Заложено в CATEGORY_MAPPING_PLAN ✅

---

### RISK-06 ⚠️ НОВА КОРЕКЦИЯ: Pazaruvaj feed след Phase 8
**Описание:** `ProductRepository.findForPazaruvajFeedByCategory(Long categoryId)` използва
рекурсивна CTE директно в `categories` таблицата:
```sql
WITH RECURSIVE cat_tree AS (
    SELECT id FROM categories WHERE id = :categoryId
    UNION ALL
    SELECT c.id FROM categories c
    INNER JOIN cat_tree ct ON c.parent_id = ct.id
)
WHERE p.category_id IN (SELECT id FROM cat_tree)
```

**Проблем:** След Phase 8, навигацията минава през CareTech категории. Ако PazaruvajController
все още вика `findForPazaruvajFeedByCategory(distributorCategoryId)`, то ще работи за
VALI категории. НО: ако Pazaruvaj feed трябва да следва CareTech категорийната структура
(не дистрибуторската), нужен е нов метод.

**Решение (добавяме в CATEGORY_MAPPING_PLAN Phase 10):**
```
[ ] 10.5 — PazaruvajController — добави нов endpoint:
    GET /api/pazaruvaj/feed-caretech.xml (optional)
    Или: запази съществуващия feed с distributor category IDs (приемливо за сега)
```

**Краткосрочно:** Pazaruvaj feed може да остане с дистрибуторски категории.
`findForPazaruvajFeedByCategory` работи с VALI category_id.
CareTech миграцията НЕ счупва Pazaruvaj feed. ✅

---

### RISK-07 ⚠️ НОВА КОРЕКЦИЯ: Sitemap след Phase 8
**Описание:** `CategoryRepository.findSitemapEntries()`:
```java
@Query("SELECT c.id AS id, c.slug AS slug, c.updatedAt AS updatedAt
        FROM Category c WHERE c.show = true AND c.slug IS NOT NULL AND c.slug <> ''")
List<SitemapEntry> findSitemapEntries();
```
**Проблем:** Sitemap-ът ще съдържа ДИСТРИБУТОРСКИ category slugs и IDs след Phase 8
(CATEGORY_MAPPING_PLAN Phase 10.3 казва да се добавят CareTech категории).

**Решение:**
```
[ ] 10.3 — SitemapController — ЗАМЕСТИ дистрибуторски URLs с CareTech URLs:
    - caretechCategoryRepository.findByShowFlagTrue() → /category/{caretech-slug}/{caretech-id}
    - НЕ включвай дистрибуторски категории в новия sitemap
    - Nginx redirect правила (Phase 10.2) ще поемат старите URLs → 301
```
Вече е в плана, но уточняваме: **замести, не добавяй** (иначе ще имаш duplicate URLs).

---

### RISK-08 ⚠️ КОРЕКЦИЯ В МИГРАЦИЯ: `ai_parameter_suggestions.suggested_caretech_param_id`
**Описание:** Ако admin изтрие CareTech параметър, AI suggestions сочещи към него:
- ON DELETE CASCADE → suggestion изчезва (агресивно — губи review history)
- ON DELETE SET NULL → suggestion остава, `suggested_caretech_param_id = NULL` (по-безопасно)

**Решение:** Трябва `ON DELETE SET NULL`. Suggestion с null `suggested_caretech_param_id`
и `is_new_caretech_param = false` трябва да се третира като "нуждае се от ресинхронизация".

**Корекция в `V39__add_ai_parameter_suggestions.sql`:**
```sql
suggested_caretech_param_id BIGINT
    REFERENCES caretech_parameters(id) ON DELETE SET NULL,  ← добавяме ON DELETE SET NULL
```

---

### RISK-09 ⚠️ НОВА КОРЕКЦИЯ: `findUnmappedParameters()` — трябва AND has products
**Описание:** Планираното `findUnmappedParameters()`:
```java
@Query("SELECT p FROM Parameter p WHERE NOT EXISTS
       (SELECT 1 FROM DistributorParameterMapping m WHERE m.distributorParameter.id = p.id
        AND m.reviewStatus = 'APPROVED')")
```
**Проблем:** Ще върне и параметри, които нямат `product_parameters` записи — т.е.
параметри без нито един продукт. Тези са безсмислени за AI анализ.

**Корекция:**
```java
@Query("SELECT p FROM Parameter p
        WHERE NOT EXISTS (
            SELECT 1 FROM DistributorParameterMapping m
            WHERE m.distributorParameter.id = p.id
            AND m.reviewStatus = 'APPROVED'
        )
        AND EXISTS (
            SELECT 1 FROM ProductParameter pp WHERE pp.parameter.id = p.id
        )")
List<Parameter> findUnmappedParameters();
```
Така AI анализира само параметри с реални продукти.

---

### RISK-10 ⚠️ НОВА КОРЕКЦИЯ: `category_parameters.is_filter` и CareTech
**Описание:** V7 migration ресетва ВСИЧКИ `category_parameters.is_filter = false`, след което
включва само curated VALI категории. TEKRA/ASBIS категории, добавени след V7 чрез sync,
имат `is_filter = TRUE` (default от V6). Това означава: при CareTech facet query с
`AND cp.is_filter != false`, ВСИЧКИ TEKRA/ASBIS параметри ще се включат (дори нереlevantни).

**Влияние:** Качеството на CareTech филтрите е изцяло определено от `caretech_parameters.is_filter`.
Ако admin не curate-ва CareTech параметрите, могат да се покажат прекалено много filter groups.

**Решение за плана:**
```
CATEGORY_MAPPING_PLAN Phase 3.1.9 — seedAutoMapping:
Добавяме стъпка: за всяка CareTech параметър, наследена чрез mapping:
  → is_filter = source category_parameters.is_filter
  → Ако distributor param е в CURATED списъка (VALI) → inherit is_filter
  → Ако TEKRA/ASBIS → is_filter = true (admin curate-ва след seed)

PARAMETER_MAPPING_PLAN Phase 3.1.3 createParameter:
Hint за admin в UI: "Внимание: TEKRA/ASBIS параметрите са с is_filter=true по
default. Проверете и изключете нерелевантни."
```

---

### RISK-11 ✅ ВЕЧЕ ПОКРИТО: `parameters` нямат UNIQUE на name_bg
**Описание:** V13 и V14 DROP-наха unique constraint на `parameters.name_bg`.
Многозначност: "RAM" може да е различен параметър за VALI, TEKRA, ASBIS.
AI трябва да ги обедини.
**Статус:** Ключова причина за AI mapping. Вече описано в PARAMETER_MAPPING_PLAN. ✅

---

### RISK-12 ⚠️ НОВА КОРЕКЦИЯ: Alias в `createMapping` — добавяме warning
**Описание:** Ако дистрибуторска категория е alias (`aliasOf != null`), продуктите й
всъщност идват от target категорията. Admin може да не знае това при mapping.

**Корекция в CATEGORY_MAPPING_PLAN `createMapping()`:**
```java
// Проверка: дали е alias?
if (distributorCategory.getAliasOf() != null) {
    // НЕ блокирай — alias resolution се прави автоматично от searchProducts()
    // НО: включи warning в response
    warnings.add("Внимание: Категория '" + distributorCategory.getNameBg() +
                 "' е alias на '" + distributorCategory.getAliasOf().getNameBg() +
                 "'. Продуктите ще се зареждат от alias target категорията.");
}
```
В `DistributorCategoryStatusDto` добавяме: `isAlias (boolean)`, `aliasTargetName (String)`.
В `CategoryMappingLayout.jsx`: показвай [ALIAS] badge за alias категории в дясната колона.

---

### RISK-13 ⚠️ НОВА КОРЕКЦИЯ: `product_parameters` е EAGER в Product entity
**Описание:** В `Product.java`:
```java
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
private Set<ProductParameter> productParameters = new HashSet<>();
```
ProductParameters се зарежда EAGER при всяко зареждане на Product entity.
**Влияние за нашия план:** Новите ни таблици не добавят нови EAGER колекции. Но при
`CaretechCategoryService.getProductsByCaretech()` → `searchProducts()` → SQL query
(не JPA) → не засяга EAGER loading.
**Статус:** Не е директен риск за нашия план. Съществуващ design issue. ✅ (no action)

---

### RISK-14 ⚠️ НОВА КОРЕКЦИЯ: `generate_category_path` SQL функция
**Описание:** V1 дефинира SQL функция:
```sql
CREATE OR REPLACE FUNCTION generate_category_path(cat_id BIGINT) RETURNS TEXT
-- Ползва COALESCE(tekra_slug, slug, CONCAT('cat-', id)) за path building
```
Тази функция е за `categories` таблицата. В `Category.generateCategoryPath()` Java методът
прави аналогично рекурсивно обхождане.

**За CareTech категории:** Нямаме нужда от SQL функция. CareTech категориите нямат
`tekra_slug`. Пътят се изгражда рекурсивно в Java в `CaretechCategoryService.buildPath()`.

**Добавяме в CATEGORY_MAPPING_PLAN Phase 3:**
```java
// Helper в CaretechCategoryService:
private String buildCaretechPath(CaretechCategory category) {
    List<String> parts = new ArrayList<>();
    CaretechCategory current = category;
    while (current != null) {
        parts.add(0, current.getSlug());
        current = current.getParent();  // LAZY — изисква transaction
    }
    return String.join("/", parts);
}
// Ползва се при buildng CategoryMappingResponseDto.caretechCategoryPath
// ВНИМАНИЕ: Трябва @Transactional контекст за LAZY parent traversal
```

---

### RISK-15 ⚠️ НОВА КОРЕКЦИЯ: `BaseEntity` Spring Auditing и System operations
**Описание:** `BaseEntity` ползва `@CreatedBy` и `@LastModifiedBy` от Spring Security context.
При seed (`seedFromAllVisible()`, `seedAutoMapping()`) и AI analysis, операциите се
изпълняват от SUPER_ADMIN user.

**Потенциален проблем:** Ако seed-ът се изпълнява в `@Async` контекст (за бъдеще), Spring
Security context може да не се прехвърли автоматично.

**За сега (Phase A — синхронен seed):** OK — SUPER_ADMIN context е наличен. ✅

**За AI analysis (async):** Трябва `DelegatingSecurityContextAsyncTaskExecutor` или
ръчно прехвърляне на SecurityContext. Добавяме бележка в PARAMETER_MAPPING_PLAN Phase 2.1.

---

### RISK-16 ✅ ПОКРИТО: `order_items.product_id` RESTRICT
**Описание:** Не може да се изтрие продукт с поръчки.
**Нашите планове:** Не изтриват продукти. ✅

---

### RISK-17 ⚠️ НОВА КОРЕКЦИЯ: V7 migration ползва `categories.external_id` (VALI IDs)
**Описание:** V7 curate-ва filter параметри по (category.external_id, parameter.external_id).
External IDs са VALI специфични. TEKRA/ASBIS нямат такава curation.

**Последствие за seed:** При `seedFromAllVisible()` (Option B — VALI + TEKRA + ASBIS),
seed-натите CareTech категории ще имат:
- VALI категории: curated is_filter параметри ✅
- TEKRA/ASBIS категории: all is_filter = TRUE (непочистени) ⚠️

**Решение:** Admin трябва да curate-ва `caretech_parameters.is_filter` след mapping.
Добавяме note в CATEGORY_MAPPING_PLAN Phase 7 и PARAMETER_MAPPING_PLAN UI.

---

### RISK-18 ⚠️ НОВА КОРЕКЦИЯ: `Category.show` field name vs `show_flag` column
**Описание:** В Category entity: `@Column(name = "show_flag") private Boolean show = true;`
Spring Data `findByShowTrue()` работи правилно (Hibernate maps field `show` → column `show_flag`).

**За CaretechCategory entity:** Трябва същата конвенция:
```java
@Column(name = "show_flag", nullable = false)
private Boolean show = true;   // НЕ showFlag! Конвенцията е `show` → `show_flag`
```
В Repository: `findByShowTrue()`, `countByParentIdAndShowTrue()` (не ShowFlag!)
В SQL: `show_flag` (не `show`)

**КРИТИЧНО:** В CATEGORY_MAPPING_PLAN V35 миграцията ползваме `show_flag` в SQL ✅.
В Java entity трябва да е `show` (field name) за Spring Data query derivation.

---

### RISK-19 ✅ ВЕЧЕ ПОКРИТО: `parameters.is_filter` глобален флаг (V2)
**Описание:** Глобален `parameters.is_filter` (V2) е superseded от per-category
`category_parameters.is_filter` (V6). Facet queries вече ползват само V6 флага.
**За CareTech:** `caretech_parameters.is_filter` ще е новото per-CareTech-param флаг.
Съществуващата `parameters.is_filter` не се пипа. ✅

---

### RISK-20 ⚠️ НОВА КОРЕКЦИЯ: `ProductSearchRequest.filters` — формат промяна
**Описание:** `ProductSearchRequest.filters: Map<Long, Set<Long>>` където ключът е
`distributor parameter_id`. След Phase A (PARAMETER_MAPPING_PLAN), filter requests
към CareTech endpoints ще ползват `caretech_parameter_id` като ключ.

**Критично:** Съществуващите endpoints (напр. `GET /api/products/categories/{id}/products`)
продължават да ползват `distributor parameter_id`. Новите CareTech endpoints ще ползват
`caretech_parameter_id`.

**Решение (вече в плана):**
```
Нов DTO: CaretechProductSearchRequest {
    caretechCategoryId: Long,
    caretechFilters: Map<Long, Set<Long>>,   ← caretechParamId → Set<optionId>
    page, size, sortBy, language
}
Нов endpoint: POST /api/caretech-categories/{id}/products (или GET с query params)
СТАР endpoint: GET /api/products/categories/{id}/products — НЕ се засяга
```
✅ Вече е планирано, но важно да се разбере: ДВАТА endpoint-а работят паралелно.

---

## Секция 5: Промени по таблица при Sync

| Таблица | VALI Sync | TEKRA Sync | ASBIS Sync | MOST Sync | Нашите нови таблици |
|---------|-----------|------------|-----------|-----------|---------------------|
| categories | INSERT/UPDATE | INSERT/UPDATE | INSERT/UPDATE | INSERT/UPDATE | НЕ СЕ ПИПАТ |
| manufacturers | INSERT/UPDATE | INSERT/UPDATE | INSERT/UPDATE | INSERT/UPDATE | НЕ СЕ ПИПАТ |
| products | INSERT/UPDATE/hide | INSERT/UPDATE/hide | INSERT/UPDATE/hide | INSERT/UPDATE/hide | НЕ СЕ ПИПАТ |
| parameters | INSERT/UPDATE | INSERT/UPDATE | INSERT/UPDATE | INSERT/UPDATE | НЕ СЕ ПИПАТ |
| parameter_options | INSERT/UPDATE/DELETE | INSERT/UPDATE | INSERT/UPDATE | INSERT/UPDATE | НЕ СЕ ПИПАТ |
| product_parameters | INSERT/UPDATE/DELETE | INSERT/UPDATE | INSERT/UPDATE | INSERT/UPDATE | НЕ СЕ ПИПАТ |
| category_parameters | INSERT/UPDATE | INSERT/UPDATE | INSERT/UPDATE | INSERT/UPDATE | НЕ СЕ ПИПАТ |
| **caretech_categories** | - | - | - | - | САМО ADMIN + ONE-TIME SEED |
| **distributor_category_mapping** | - | - | - | - | САМО ADMIN |
| **caretech_parameters** | - | - | - | - | ADMIN + AI |
| **distributor_parameter_mapping** | - | - | - | - | ADMIN + AI |

**КРИТИЧНА ГАРАНЦИЯ:** Нашите нови таблици НЕ се пипат от sync services.
Sync services пипат само оригиналните таблици. Новите са САМО за admin management.

---

## Секция 6: Влияние на CASCADE при типични Sync операции

### Сценарий: VALI Sync добавя нова категория
```
INSERT INTO categories → Нова категория
→ distributor_category_mapping: нищо (нова категория, незасегнато)
→ Admin вижда я в CategoryMappingLayout → "Unmapped" count +1
→ Admin ръчно прави mapping
```
**Влияние:** Минимално. Admin трябва ръчно да mapping-не новата категория. ✅

### Сценарий: VALI Sync скрива категория (show_flag = false)
```
UPDATE categories SET show_flag = false WHERE id = X
→ distributor_category_mapping: ОСТАВА (не се изтрива!)
→ Продуктите в тази категория: show_flag вероятно също false
→ CareTech система: mapping съществува но категорията е скрита
→ getProductsByCaretech → includes X's distributor_category_id → searchProducts → 0 visible products
```
**Риск:** Mapping остава за скрита категория. Не е грешка — ако VALI реши да покаже
категорията отново, mapping-ът е запазен. Admin може ръчно да изтрие mapping ако иска.

### Сценарий: VALI Sync hard-delete категория (рядко/никога)
```
DELETE FROM categories WHERE id = X
→ category_parameters: CASCADE DELETE ✅
→ distributor_category_mapping: CASCADE DELETE ✅ (plan коректен)
→ products: category_id = NULL (SET NULL — orphaned products)
→ CareTech категорията ОСТАВА (mapping изчезна)
→ Admin вижда CareTech категория с 0 mappings
```
**Риск:** НИСЪК. Sync services НЕ hard-delete категории. Само hide.

### Сценарий: Нов TEKRA параметър е добавен от sync
```
INSERT INTO parameters (platform=TEKRA, ...)
INSERT INTO category_parameters (is_filter = TRUE by default)
→ distributor_parameter_mapping: нищо (нов параметър)
→ Admin вижда го в ParameterMappingLayout → "Unmapped" count +1
→ Admin пуска AI анализ (или ръчно mapping)
```
**Влияние:** Минимално. ✅

---

## Секция 7: Корекции и допълнения към плановете

### Корекции към CATEGORY_MAPPING_PLAN.md

| # | Фаза | Корекция |
|---|------|---------|
| C1 | Фаза 3.2.1 | `createMapping()` добавя alias warning в response (не блокира) |
| C2 | Фаза 4.1 | `DistributorCategoryStatusDto` добавя `isAlias (boolean)`, `aliasTargetName (String)` |
| C3 | Фаза 6.2 | `CategoryMappingLayout` показва [ALIAS] badge за alias категории |
| C4 | Фаза 3.1 | `buildCaretechPath()` helper — изисква `@Transactional` за LAZY parent traversal |
| C5 | Фаза 10 | Sitemap: **замести** дистрибуторски URLs с CareTech (не добавяй!) |
| C6 | Фаза 10 | Добави: Pazaruvaj feed остава с дистрибуторски категории (не е production-breaking) |
| C7 | Entity | `CaretechCategory.show` (field) → `show_flag` (column) — НЕ `showFlag` |

### Корекции към PARAMETER_MAPPING_PLAN.md

| # | Фаза | Корекция |
|---|------|---------|
| P1 | V39 | `ai_parameter_suggestions.suggested_caretech_param_id` — добави `ON DELETE SET NULL` |
| P2 | Repo 1.5 | `findUnmappedParameters()` добавя `AND EXISTS (ProductParameter)` |
| P3 | Service 2.1 | `@Async` AI analysis — добавя `DelegatingSecurityContextAsyncTaskExecutor` за Security context |
| P4 | Admin UI | Hint: "TEKRA/ASBIS параметрите са is_filter=true по default — curate ги" |
| P5 | Phase 2 | AI analysis трябва да използва и `category_parameters.is_filter` като сигнал при prompting |

---

## Секция 8: Go/No-Go Checklist преди имплементация

```
✅ ЗЕЛЕНО — напълно ясно и решено:
   □ V35/V36 SQL миграции — схемата е финализирана
   □ CASCADE/SET NULL поведения за нови FK — всички правилно дефинирани
   □ Alias resolution в ProductSearchService — автоматично ✅
   □ Cross-platform SKU deduplication — работи автоматично ✅
   □ Sync services — НЕ засягат нови таблици ✅
   □ Java field `show` → DB column `show_flag` конвенция ✅
   □ order_items RESTRICT — не засяга плановете ✅

⚠️ ЖЪЛТО — изисква внимание при имплементация:
   □ ai_parameter_suggestions.suggested_caretech_param_id → трябва ON DELETE SET NULL
   □ findUnmappedParameters() → добавя AND EXISTS (ProductParameter)
   □ createMapping() → добавя alias warning (не блокира)
   □ DistributorCategoryStatusDto → добавя isAlias, aliasTargetName
   □ buildCaretechPath() → изисква @Transactional
   □ Sitemap замества (не добавя) distributor URLs
   □ @Async AI analysis → DelegatingSecurityContextAsyncTaskExecutor

🔴 ЧЕРВЕНО — не трябва да се имплементира преди решение:
   □ Phase 8 (Frontend routing switch) — само след успешен seed + 28 ручни mappings + тестване
   □ Phase B (Value normalization) — само след Phase A е стабилна в production
   □ Pazaruvaj feed с CareTech категории — not required, current feed остава functional
```

---

## Секция 9: Пълен списък на таблиците след имплементация

| Таблица | Статус | Пипат ли я sync services? |
|---------|--------|--------------------------|
| users | Съществуваща | Не |
| categories | Съществуваща | **ДА** |
| manufacturers | Съществуваща | **ДА** |
| products | Съществуваща | **ДА** |
| additional_images | Съществуваща | **ДА** |
| parameters | Съществуваща | **ДА** |
| category_parameters | Съществуваща | **ДА** |
| parameter_options | Съществуваща | **ДА** |
| product_parameters | Съществуваща | **ДА** |
| product_flags | Съществуваща | **ДА** |
| cart_items | Съществуваща | Не |
| user_favorites | Съществуваща | Не |
| orders | Съществуваща | Не |
| order_items | Съществуваща | Не |
| subscription | Съществуваща | Не |
| sync_logs | Съществуваща | **ДА** |
| leasing_applications | Съществуваща | Не |
| personal_offers | Съществуваща | Не |
| blog_posts | Съществуваща | Не |
| blog_categories | Съществуваща | Не |
| blog_tags | Съществуваща | Не |
| **caretech_categories** | **НОВА (V35)** | **НИКОГА** |
| **distributor_category_mapping** | **НОВА (V36)** | **НИКОГА** |
| **caretech_parameters** | **НОВА (V37)** | **НИКОГА** |
| **distributor_parameter_mapping** | **НОВА (V38)** | **НИКОГА** |
| **ai_parameter_suggestions** | **НОВА (V39)** | **НИКОГА** |
| **caretech_parameter_values** | **НОВА (V40, Phase B)** | **НИКОГА** |
| **distributor_value_mapping** | **НОВА (V41, Phase B)** | **НИКОГА** |

---

*Файлът е за справка. Не се изпълнява — само документация.*
*При нови открития по DB структурата, добавяй нови RISK entries.*
