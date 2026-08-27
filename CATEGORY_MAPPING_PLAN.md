# План: CareTech Категорийна Йерархия + Distributor Mapping

> Статус: **ОДОБРЕН — в изпълнение**
> Дата: 2026-08-21 (ревизиран след дълбок анализ)
> Обхват: Само категории. Параметри/филтри → отделен план.

---

## Одит резюме (от categories_202608211236.csv — 941 реда)

| Платформа | L1 | L2 parent | L2 leaf | L3 leaf | Видими leaf |
|-----------|-----|-----------|---------|---------|-------------|
| VALI      | 16  | 1         | 112     | 46      | **158**     |
| TEKRA     | 2   | 4         | 1       | 25      | **26**      |
| ASBIS     | 0   | 0         | 2       | 0       | **2**       |
| Скрити ASBIS | — | —        | —       | —       | 661 скрити  |

- Видими leaf (кандидати за mapping): **186**
- Ръчен mapping след seed: **28** (26 TEKRA + 2 ASBIS)
- Seed: всички `show_flag=true` категории → ~207 CareTech + ~186 auto-mappings
- ЗАБЕЛЕЖКА: TEKRA има собствен L1 "Видеонаблюдение" (id=230) — включен в seed (Option B)
- 13 alias категории (id 531–543, скрити) — изключени от seed

---

## Архитектура

```
Сега:    products.category_id → categories (VALI/TEKRA/ASBIS)
                                       ↑ Навигация

След:    products.category_id → categories  (sync НЕ СЕ ПИПА!)
                                       ↓
                          distributor_category_mapping
                                       ↓
                          caretech_categories  (нова таблица)
                                       ↑ Навигация
```

### Поток за зареждане на продукти по CareTech категория:
```
GET /api/caretech-categories/{id}/products
  → CaretechCategoryService.getProductsByCaretech(id)
  → collectCaretechDescendantIds(id)          // рекурсия в caretech_categories
  → mappingRepo.findByCaretechCategoryIdIn()   // → distributor category IDs
  → productSearchService.searchProducts(       // НЕ searchByCategory()!
       ProductSearchRequest{categories: [distIds]}
    )
```

**КРИТИЧНО:** Не се вика `searchByCategory(caretechId)` — тя търси по `categories` таблицата с дистрибуторски ID. CareTech ID-тата не съществуват там.

---

## Бизнес правила (референция)

| Правило | Имплементация |
|---------|--------------|
| Само leaf от двете страни при mapping | Service проверява: дистрибуторска има ли видими деца в `categories`; CareTech има ли видими деца в `caretech_categories` |
| Distributor leaf = няма видими деца | `categoryRepository.countByParentIdAndShowFlagTrue(id) == 0` (**НЕ** `Category.isLeaf()` — тя проверява дали е ROOT!) |
| CareTech leaf = няма видими деца | `caretechRepo.countByParentIdAndShowTrue(id) == 0` |
| 1 дистрибуторска → само 1 CareTech | UNIQUE constraint на `distributor_category_id` |
| Много дистрибуторски → 1 CareTech | Позволено (без ограничение) |
| Не може да добавиш дете на CareTech с mappings | `validateCanAddChild()` — проверява mappings |
| Не може да изтриеш CareTech с видими деца | `deleteCategory()` блокира |
| Не може да изтриеш CareTech с mappings | `deleteCategory()` блокира |
| Max 3 нива | `createCategory()` проверява parent.level ≤ 2 |
| Seed само веднъж | Guard: `caretechRepo.count() > 0` → хвърля грешка |
| Seed е SUPER_ADMIN only | SecurityConfig: `hasRole("SUPER_ADMIN")` |
| `showInFooter` се задава ръчно | Не се задава автоматично при seed — админът го прави след seed |
| Alias категории при mapping | НЕ блокирай, НО include warning в response; alias resolution е автоматична в `searchProducts()` |
| CareTech entity field name | Java field: `show` (не `showFlag`!) → DB column: `show_flag`. Конвенция от Category entity |
| Sitemap при Phase 10 | ЗАМЕСТИ дистрибуторски URLs с CareTech — не добавяй едновременно (duplicate!) |

---

## ФАЗА 1 — База данни
> Нови Flyway migrations. Не засяга нищо съществуващо.

- [ ] **1.1** — `V35__add_caretech_categories.sql`

```sql
CREATE TABLE caretech_categories (
    id           BIGSERIAL PRIMARY KEY,
    name_bg      VARCHAR(255) NOT NULL,
    name_en      VARCHAR(255),
    slug         VARCHAR(200) NOT NULL,
    parent_id    BIGINT REFERENCES caretech_categories(id) ON DELETE SET NULL,
    sort_order   INTEGER NOT NULL DEFAULT 0,
    show_flag    BOOLEAN NOT NULL DEFAULT true,
    level        SMALLINT NOT NULL DEFAULT 1 CHECK (level BETWEEN 1 AND 3),
    show_in_footer BOOLEAN NOT NULL DEFAULT false,
    source_category_id BIGINT,  -- оригиналният categories.id (за SEO redirect)
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(100) DEFAULT 'system',
    last_modified_by VARCHAR(100) DEFAULT 'system'
);

-- Индекси
CREATE INDEX idx_caretech_categories_slug ON caretech_categories(slug);
CREATE INDEX idx_caretech_categories_parent_id ON caretech_categories(parent_id);
CREATE INDEX idx_caretech_categories_show_flag ON caretech_categories(show_flag);
CREATE INDEX idx_caretech_categories_level ON caretech_categories(level);
CREATE INDEX idx_caretech_categories_parent_show ON caretech_categories(parent_id, show_flag);
-- Composite за isLeaf проверка: всички видими деца на даден parent
CREATE INDEX idx_caretech_categories_source ON caretech_categories(source_category_id);
```

  Без UNIQUE на slug — само INDEX (аналогично на `categories` таблицата).

- [ ] **1.2** — `V36__add_distributor_category_mapping.sql`

```sql
CREATE TABLE distributor_category_mapping (
    id                      BIGSERIAL PRIMARY KEY,
    distributor_category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    caretech_category_id    BIGINT NOT NULL REFERENCES caretech_categories(id) ON DELETE CASCADE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100) DEFAULT 'system',
    CONSTRAINT uq_distributor_category UNIQUE (distributor_category_id)
);

CREATE INDEX idx_dist_mapping_dist_cat   ON distributor_category_mapping(distributor_category_id);
CREATE INDEX idx_dist_mapping_caretech   ON distributor_category_mapping(caretech_category_id);
```

---

## ФАЗА 2 — Backend: Entity и Repository
> Чист JPA код. Нищо съществуващо не се пипа.

- [ ] **2.1** — `CaretechCategory.java` (Entity)
  ```
  Пакет: com.techstore.entity
  Полета:
    - nameBg (NOT NULL)
    - nameEn
    - slug
    - parent @ManyToOne(fetch=LAZY) self-ref
    - children @OneToMany(mappedBy="parent", fetch=LAZY)
    - sortOrder
    - show (@Column(name="show_flag"))  ← задължително show_flag!
    - level (SMALLINT 1-3)
    - showInFooter
    - sourceCategoryId

  НЕ добавяй isLeaf() като метод — опасно е (lazy N+1 при listing).
  Leaf логиката е в Repository/Service.

  @EqualsAndHashCode(exclude = {"parent", "children"})
  Audit fields: createdAt, updatedAt, createdBy, lastModifiedBy
  ```

- [ ] **2.2** — `DistributorCategoryMapping.java` (Entity)
  ```
  Пакет: com.techstore.entity
  - distributorCategory @ManyToOne(fetch=LAZY) → Category
  - caretechCategory @ManyToOne(fetch=LAZY) → CaretechCategory
  - createdAt, createdBy
  ```

- [ ] **2.3** — `CaretechCategoryRepository.java`
  ```java
  // Публичен API — само видими
  List<CaretechCategory> findByShowFlagTrueOrderBySortOrderAsc();

  // Admin — всички
  List<CaretechCategory> findAllByOrderBySortOrderAsc();

  // Tree navigation
  List<CaretechCategory> findByParentIsNullOrderBySortOrderAsc();
  List<CaretechCategory> findByParentIdOrderBySortOrderAsc(Long parentId);

  // Slug lookup
  Optional<CaretechCategory> findBySlug(String slug);
  boolean existsBySlug(String slug);                        // за create
  boolean existsBySlugAndIdNot(String slug, Long id);       // за update

  // Seed helpers
  Optional<CaretechCategory> findBySourceCategoryId(Long sourceCategoryId);
  long count();                                              // seed guard (не само show=true!)

  // isLeaf — batch-able
  long countByParentIdAndShowFlagTrue(Long parentId);        // 0 = leaf

  // За getProductsByCaretech
  List<CaretechCategory> findByParentId(Long parentId);      // за descendant collection
  ```

- [ ] **2.4** — `DistributorCategoryMappingRepository.java`
  ```java
  Optional<DistributorCategoryMapping> findByDistributorCategoryId(Long distCategoryId);
  List<DistributorCategoryMapping> findByCaretechCategoryId(Long caretechId);
  List<DistributorCategoryMapping> findByCaretechCategoryIdIn(Collection<Long> caretechIds);
  boolean existsByDistributorCategoryId(Long distCategoryId);
  long countByCaretechCategoryId(Long caretechId);

  // За distributor leaf list с mapping статус — native query
  @Query(value = """
      SELECT c.id, c.name_bg, c.platform, c.parent_id,
             (dcm.id IS NOT NULL) AS is_mapped,
             dcm.caretech_category_id AS mapped_to_caretech_id,
             cc.name_bg AS mapped_to_caretech_name
      FROM categories c
      LEFT JOIN distributor_category_mapping dcm ON dcm.distributor_category_id = c.id
      LEFT JOIN caretech_categories cc ON cc.id = dcm.caretech_category_id
      WHERE c.show_flag = true
        AND (:platform IS NULL OR c.platform = :platform)
        AND NOT EXISTS (
            SELECT 1 FROM categories child
            WHERE child.parent_id = c.id AND child.show_flag = true
        )
      ORDER BY c.platform, c.name_bg
      """, nativeQuery = true)
  List<Object[]> findDistributorLeafCategoriesWithStatus(
      @Param("platform") String platform);
  ```

---

## ФАЗА 3 — Backend: Services

### 3.1 — `CaretechCategoryService.java`

- [ ] **3.1.1** `getVisibleCategories()` — `findByShowFlagTrueOrderBySortOrderAsc()` → map to DTO с `hasChildren` поле
  - `hasChildren` се изчислява ефективно: зареди всички видими категории → групирай по parentId → за всяка категория провери дали нейното ID се среща като parentId в групата. Без допълнителни queries.

- [ ] **3.1.2** `getAllCategoriesForAdmin()` — `findAllByOrderBySortOrderAsc()` → map to DTO (включва hidden)
  - Seed бутон активност: frontend проверява `adminCategories.length === 0`

- [ ] **3.1.3** `getCategoryById(Long id)` — `findById` + DTO

- [ ] **3.1.4** `createCategory(CaretechCategoryRequestDto dto)`
  ```
  Валидации:
  1. nameBg != null и не е празно
  2. Ако подаден slug: existsBySlug(slug) → грешка "Slug вече съществува"
     Ако slug не е подаден: auto-генерирай от nameBg (виж секция "Slug generation")
  3. Ако parentId != null:
     a. Намери parent — ако не съществува → грешка
     b. parent.level >= 3 → грешка "Не може да добавиш под level 3"
     c. validateCanAddChild(parentId) — проверява за mappings
     d. Новата category.level = parent.level + 1
  4. Ако parentId == null: level = 1
  Save и върни DTO.
  ```

- [ ] **3.1.5** `updateCategory(Long id, CaretechCategoryRequestDto dto)`
  ```
  Валидации:
  1. Намери категорията или грешка
  2. Ако slug се променя: existsBySlugAndIdNot(newSlug, id) → грешка
  3. Ако parentId се променя:
     a. НЕ позволявай ако категорията има видими деца
        (caretechRepo.countByParentIdAndShowFlagTrue(id) > 0)
     b. Валидирай новия parent (level ≤ 2, validateCanAddChild)
     c. Рекалкулирай level = newParent.level + 1
        (за leaf без деца — само нейното level се сменя)
  Update полетата и save.
  ```

- [ ] **3.1.6** `deleteCategory(Long id)`
  ```
  Проверки (в ред):
  1. Видими деца: countByParentIdAndShowFlagTrue(id) > 0 → грешка
  2. Mappings: mappingRepo.countByCaretechCategoryId(id) > 0 → грешка
  Soft delete: category.setShowFlag(false); save.
  ```

- [ ] **3.1.7** `validateCanAddChild(Long parentId)`
  ```
  Блокира ако:
  1. mappingRepo.existsByDistributorCategoryId не — ами:
     mappingRepo.countByCaretechCategoryId(parentId) > 0 → грешка
     "Категорията има свързани дистрибуторски категории. Махни ги преди да добавяш подкатегория."
  2. parent.level >= 3 → грешка (вече проверено в createCategory, но може да се вика standalone)
  ```

- [ ] **3.1.8** `seedFromAllVisible()` — @Transactional
  ```
  GUARD: if (caretechRepo.count() > 0) throw "Seed вече е изпълнен"

  Алгоритъм:
  Map<Long, Long> oldIdToNewId = new HashMap<>();  // categories.id → caretech_categories.id

  // Стъпка 1: Всички видими L1 (parent IS NULL, show_flag=true)
  List<Category> l1 = categoryRepo.findByParentIsNullAndShowFlagTrue()
                                   .stream().filter(c -> c.getShowFlag()).toList();
  for (Category cat : sortedByOrder(l1)) {
      CaretechCategory ct = buildCaretechFrom(cat, null, 1);
      Long newId = caretechRepo.save(ct).getId();
      oldIdToNewId.put(cat.getId(), newId);
  }

  // Стъпка 2: Видими L2 (parent е visible L1)
  for (Long oldL1Id : oldIdToNewId.keySet()) {
      List<Category> l2Children = categoryRepo.findByParentIdAndShowFlagTrue(oldL1Id);
      for (Category cat : sortedByOrder(l2Children)) {
          Long newParentId = oldIdToNewId.get(oldL1Id);
          CaretechCategory ct = buildCaretechFrom(cat, newParentId, 2);
          Long newId = caretechRepo.save(ct).getId();
          oldIdToNewId.put(cat.getId(), newId);
      }
  }

  // Стъпка 3: Видими L3 (parent е visible L2)
  // ... аналогично, но само за L2 parents

  return SeedResultDto { categoriesCreated: oldIdToNewId.size() };

  // buildCaretechFrom(cat, parentId, level):
  //   ct.setNameBg(cat.getNameBg())
  //   ct.setNameEn(cat.getNameEn())
  //   ct.setSlug(cat.getSlug())   // директно взима slug-а от categories
  //   ct.setSortOrder(cat.getSortOrder())
  //   ct.setShowFlag(true)
  //   ct.setLevel(level)
  //   ct.setShowInFooter(false)   // НЕ се задава автоматично — admin го прави ръчно
  //   ct.setSourceCategoryId(cat.getId())
  //   ct.setParentId(parentId)    // null за L1
  ```

- [ ] **3.1.9** `seedAutoMapping()` — @Transactional, вика се след seedFromAllVisible()
  ```
  int mappingsCreated = 0;
  List<String> warnings = new ArrayList<>();

  // Всички видими категории от categories таблицата без видими деца (distributor leaves)
  List<Category> distributorLeaves = categoryRepo.findVisibleLeafCategories();
  // findVisibleLeafCategories: WHERE show_flag=true AND NOT EXISTS (child WITH show_flag=true)

  for (Category distLeaf : distributorLeaves) {
      Optional<CaretechCategory> ctOpt = caretechRepo.findBySourceCategoryId(distLeaf.getId());
      if (ctOpt.isEmpty()) {
          warnings.add("Не намерена CareTech категория за distributor id=" + distLeaf.getId());
          continue;
      }
      CaretechCategory ct = ctOpt.get();

      // Провери дали CareTech категорията е leaf (няма видими деца)
      if (caretechRepo.countByParentIdAndShowFlagTrue(ct.getId()) > 0) {
          warnings.add("CareTech " + ct.getNameBg() + " не е leaf — пропусната");
          continue;
      }

      DistributorCategoryMapping mapping = new DistributorCategoryMapping();
      mapping.setDistributorCategoryId(distLeaf.getId());
      mapping.setCaretechCategoryId(ct.getId());
      mapping.setCreatedBy("system-seed");
      mappingRepo.save(mapping);
      mappingsCreated++;
  }

  return SeedResultDto { mappingsCreated, warnings };
  ```

- [ ] **3.1.10** `getProductsByCaretech(Long id, String language, int page, int size, String sortBy)`
  ```
  // Стъпка 1: Събери всички CareTech descendant IDs
  List<Long> caretechIds = new ArrayList<>();
  collectCaretechDescendants(id, caretechIds);  // рекурсия в caretech_categories

  // Стъпка 2: Намери дистрибуторски category IDs
  List<Long> distIds = mappingRepo.findByCaretechCategoryIdIn(caretechIds)
                                   .stream()
                                   .map(m -> m.getDistributorCategoryId())
                                   .toList();

  if (distIds.isEmpty()) return emptyPage;

  // Стъпка 3: Търси продукти — НЕ searchByCategory()!
  ProductSearchRequest req = ProductSearchRequest.builder()
      .categories(distIds.stream().map(String::valueOf).toList())
      .language(language)
      .page(page).size(size)
      .sortBy(sortBy != null ? sortBy : "price_asc")
      .build();

  return productSearchService.searchProducts(req);

  // collectCaretechDescendants(Long parentId, List<Long> acc):
  //   acc.add(parentId);
  //   caretechRepo.findByParentId(parentId)
  //               .forEach(child -> collectCaretechDescendants(child.getId(), acc));
  ```

- [ ] **3.1.11** `getParametersByCaretech(Long id, String language)`
  ```
  // Аналогично на getProductsByCaretech:
  // 1. Събери CareTech descendants
  // 2. Вземи distributor category IDs от mapping
  // 3. Извикай parameterService.getParametersForCategories(distIds, language)
  // Очаквани дублирания — ще се обработват в отделния Parameter Mapping план
  ```

---

### 3.2 — `CategoryMappingService.java`

- [ ] **3.2.1** `createMapping(Long distributorCategoryId, Long caretechCategoryId)`
  ```
  Валидации (в ред):
  1. Намери дистрибуторската категория или грешка
  2. Провери дали тя е distributor leaf:
     categoryRepo.countByParentIdAndShowFlagTrue(distributorCategoryId) > 0
     → грешка "Може да се свързват само листови категории"
     ВНИМАНИЕ: НЕ използвай Category.isLeaf() — тя проверява дали е ROOT!
  3. [DB-ANALYSIS RISK-02] Провери дали е alias:
     if (distributorCategory.getAliasOf() != null):
     → НЕ блокирай (alias resolution е автоматична в searchProducts())
     → Добавяй в response: warning "Категорията е alias на '{target}'. Продуктите
       ще се зареждат от alias target категорията."
  4. Намери CareTech категорията или грешка
  5. Провери дали CareTech е leaf:
     caretechRepo.countByParentIdAndShowFlagTrue(caretechCategoryId) > 0
     → грешка "Може да се свързват само листови CareTech категории"
  6. existsByDistributorCategoryId(distributorCategoryId) → грешка "Вече е свързана"
  7. Save mapping
  Върни: MappingResultDto { mapping, warnings: List<String> }
  ```

- [ ] **3.2.2** `deleteMapping(Long mappingId)`

- [ ] **3.2.3** `getMappingsForCaretechCategory(Long caretechId)` → `List<CategoryMappingResponseDto>`

- [ ] **3.2.4** `getDistributorLeafCategoriesWithStatus(String platform, String status)`
  ```
  status enum: ALL, MAPPED, UNMAPPED
  platform: null = всички, иначе филтрира по Platform enum

  Използва нативния query от DistributorCategoryMappingRepository.
  Филтрира резултата по status след зареждане (или добавя WHERE в query).
  Маппира Object[] → DistributorCategoryStatusDto:
    id, nameBg, platform, breadcrumbPath (пресмята рекурсивно), isMapped,
    mappedToCaretechId, mappedToCaretechName
  ```

---

### 3.3 — Slug Generation Utility

- [ ] **3.3.1** `SlugUtil.java` (нов utility клас)
  ```java
  // Ако admin не подаде slug при createCategory — auto-генерирай от nameBg
  // Алгоритъм:
  // 1. Транслитерация: BG → латиница (стандартна таблица а→a, б→b, в→v, г→g, ...)
  // 2. Lowercase
  // 3. Замени интервали и специални символи → "-"
  // 4. Премахни поредни "-"
  // 5. Trim "-" от начало/край
  // Ако auto-slug вече съществува: добавяй суфикс -2, -3, ...
  // ВАЖНО: Slugове от seed-а се взимат ДИРЕКТНО от categories.slug — без транслитерация
  ```

---

## ФАЗА 4 — Backend: DTOs и Controllers

- [ ] **4.1** — DTOs (пакет: `dto/caretech/`)

  - [ ] `CaretechCategoryRequestDto`
    ```
    nameBg (required), nameEn, slug (optional — auto-gen ако липсва),
    parentId, sortOrder, show, showInFooter
    ```

  - [ ] `CaretechCategoryResponseDto`
    ```
    id, nameBg, nameEn, slug, level, sortOrder, show, showInFooter,
    parentId, parentNameBg,
    hasChildren (boolean — има ли видими деца),
    isLeaf (boolean — !hasChildren, за удобство на frontend),
    mappingsCount (long — от countByCaretechCategoryId)
    ```

  - [ ] `CategoryMappingRequestDto`
    ```
    distributorCategoryId (required), caretechCategoryId (required)
    ```

  - [ ] `CategoryMappingResponseDto`
    ```
    id,
    distributorCategoryId, distributorCategoryName, distributorCategoryPath (breadcrumb),
    distributorPlatform (Platform enum),
    caretechCategoryId, caretechCategoryName, caretechCategoryPath (breadcrumb)
    ```

  - [ ] `DistributorCategoryStatusDto`
    ```
    id, nameBg, platform (Platform enum — не String!),
    path (breadcrumb: "L1 > L2 > L3"),
    isMapped (boolean),
    mappedToCaretechId, mappedToCaretechName,
    isAlias (boolean),                          // [DB-ANALYSIS RISK-02]
    aliasTargetName (String)                    // показва се в [ALIAS] badge в UI
    ```

  - [ ] `SeedResultDto`
    ```
    categoriesCreated (int), mappingsCreated (int),
    warnings (int), warningMessages (List<String>)

    Пример warningMessages:
    - "Не намерена CareTech категория за distributor id=123"
    - "CareTech 'Принтери' не е leaf — пропусната"
    ```

- [ ] **4.2** — `CaretechCategoryController.java`

  **Публични endpoints (permitAll):**
  - [ ] `GET /api/caretech-categories`
    - Връща: **flat list** (не tree!) от `CaretechCategoryResponseDto` с `show=true`
    - Frontend строи tree client-side от `parentId` полето (аналогично на съществуващото `/api/categories`)
    - Query params: none

  - [ ] `GET /api/caretech-categories/{id}` — единична категория

  - [ ] `GET /api/caretech-categories/by-slug/{slug}` — lookup по slug (за breadcrumbs)

  - [ ] `GET /api/caretech-categories/{id}/products` — products по CareTech категория
    - Query params: `page, size, sortBy, language`
    - Делегира на `CaretechCategoryService.getProductsByCaretech()`

  - [ ] `GET /api/caretech-categories/{id}/parameters` — параметри по CareTech категория
    - Query params: `language`

  **Admin endpoints (ADMIN | SUPER_ADMIN):**
  - [ ] `GET /api/admin/caretech-categories` — всички (включва скрити)
  - [ ] `POST /api/admin/caretech-categories` — create
  - [ ] `PUT /api/admin/caretech-categories/{id}` — update
  - [ ] `DELETE /api/admin/caretech-categories/{id}` — soft delete

  **Seed endpoint (SUPER_ADMIN ONLY):**
  - [ ] `POST /api/admin/caretech-categories/seed`
    - `hasRole("SUPER_ADMIN")` — не `hasAnyRole("ADMIN", "SUPER_ADMIN")`!
    - Вика `seedFromAllVisible()` + `seedAutoMapping()`
    - Връща `SeedResultDto`

- [ ] **4.3** — `CategoryMappingController.java`

  **Admin endpoints (ADMIN | SUPER_ADMIN):**
  - [ ] `GET /api/admin/category-mapping/caretech/{id}` — mappings за CareTech категория
  - [ ] `GET /api/admin/category-mapping/distributor` — distributor листа
    - Query params: `platform` (VALI/TEKRA/ASBIS/MOST/null), `status` (ALL/MAPPED/UNMAPPED)
  - [ ] `POST /api/admin/category-mapping` — създай mapping
  - [ ] `DELETE /api/admin/category-mapping/{id}` — изтрий mapping

- [ ] **4.4** — `SecurityConfig.java`
  ```java
  // Публични CareTech endpoints:
  .requestMatchers("/api/caretech-categories/**").permitAll()

  // Admin CareTech endpoints:
  .requestMatchers("/api/admin/caretech-categories/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
  .requestMatchers("/api/admin/category-mapping/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

  // Seed — SUPER_ADMIN ONLY (override на горното):
  // Поради ред на матчинг — постави ПРЕДИ generic admin matcher:
  .requestMatchers(HttpMethod.POST, "/api/admin/caretech-categories/seed").hasRole("SUPER_ADMIN")
  ```

---

## ФАЗА 5 — Frontend: Redux

- [ ] **5.1** — `caretechCategorySlice.js`
  ```javascript
  // State
  {
    categories: [],          // видими (show=true) — flat list за публичното API
    adminCategories: [],     // всички — за admin panel
    status: 'idle',
    error: null
  }

  // Thunks
  fetchCaretechCategories       → GET /api/caretech-categories
  fetchAllCaretechCategoriesAdmin → GET /api/admin/caretech-categories
  createCaretechCategory        → POST /api/admin/caretech-categories
  updateCaretechCategory        → PUT /api/admin/caretech-categories/{id}
  deleteCaretechCategory        → DELETE /api/admin/caretech-categories/{id}
  seedCaretechCategories        → POST /api/admin/caretech-categories/seed

  // Tree building helper (pure function, не е thunk):
  export function buildCategoryTree(flatList) {
    // Сортирай по sortOrder, групирай по parentId, строй nested структура
    // Аналогично на съществуващата логика в categorySlice.js
  }
  ```

- [ ] **5.2** — `categoryMappingSlice.js`
  ```javascript
  // State
  {
    caretechMappings: [],        // mappings за избрана CareTech категория
    distributorCategories: [],   // leaf distributor категории с mapping статус
    status: 'idle',
    error: null
  }

  // Thunks
  fetchCaretechMappings          → GET /api/admin/category-mapping/caretech/{id}
  fetchDistributorCategories     → GET /api/admin/category-mapping/distributor?platform=&status=
  createMapping                  → POST /api/admin/category-mapping
  deleteMapping                  → DELETE /api/admin/category-mapping/{id}
  ```

- [ ] **5.3** — `store.js` — добави `caretechCategories` и `categoryMapping` reducers

---

## ФАЗА 6 — Frontend: Admin UI

- [ ] **6.1** — `CaretechCategoriesLayout.jsx`

  **Tree View:**
  - Зареди `adminCategories` (flat list) → строй tree с `buildCategoryTree()`
  - Indent по level (16px × level)
  - За всяка категория ред:
    - [●N] mappings badge (N = mappingsCount от DTO)
    - [LEAF] / [PARENT] tag
    - Actions: [+дете], [✏ редактирай], [👁 скрий/покажи], [🗑 изтрий]
    - Скритите категории — сив текст

  **Seed секция (само за SUPER_ADMIN):**
  - Бутон "Стартирай Seed" — активен само ако `adminCategories.length === 0`
  - Confirmation modal: "Това е еднократна операция. Сигурен ли си?"
  - След seed: показва SeedResultDto в modal (categoriesCreated, mappingsCreated, warnings)
  - Ако warnings > 0: разгъваем список с warningMessages

  **Create/Edit Modal:**
  - nameBg (required), nameEn, slug (auto-gen от nameBg ако е празно), sortOrder
  - show (toggle), showInFooter (toggle)
  - Parent selector: dropdown от всички категории (само level ≤ 2 са валидни parents)
  - При submit: disable бутон, показва грешки от backend

- [ ] **6.2** — `CategoryMappingLayout.jsx`

  **Split-screen layout:**

  *Ляво — CareTech дърво:*
  - Строи tree от `adminCategories`
  - LEAF категории: кликаеми → избира "target CareTech категория"
  - PARENT категории: disabled + tooltip "Свързват се само leaf категории"
  - Избраната категория: highlight + показва нейните mappings отдясно в info panel
  - Показва текущите mappings за избраната категория (от `caretechMappings`)
  - [×] бутон до всяко mapping → DELETE с confirmation

  *Дясно — Дистрибуторски категории:*
  - Филтри: Platform (All/VALI/TEKRA/ASBIS/MOST) + Status (All/Mapped/Unmapped)
  - Platform color badges: VALI=blue, TEKRA=red, ASBIS=green, MOST=purple
  - За всяка категория: path (breadcrumb), platform badge, mapping статус
  - UNMAPPED категории: [Свържи] бутон → POST mapping към избраната CareTech категория
  - MAPPED категории: показва "→ {caretechCategoryName}", disabled [Свържи]
  - Ако не е избрана CareTech категория: [Свържи] е disabled + tooltip

- [ ] **6.3** — `Sidebar.jsx` — добави 2 нови menu items:
  - "CareTech Категории" → `/admin/caretech-categories`
  - "Свързване на категории" → `/admin/category-mapping`

- [ ] **6.4** — `App.js` — добави routes:
  - `/admin/caretech-categories` → `CaretechCategoriesLayout`
  - `/admin/category-mapping` → `CategoryMappingLayout`

---

## ФАЗА 7 — Admin действие: Seed и ръчни mappings
> Само след като Фази 1–6 са завършени и тествани.

- [ ] **7.1** — Отвори `/admin/caretech-categories` — seed бутонът трябва да е активен
- [ ] **7.2** — Пусни seed (SUPER_ADMIN акаунт)
- [ ] **7.3** — Провери SeedResultDto: categoriesCreated ~207, mappingsCreated ~186, warnings = 0
- [ ] **7.4** — Прегледай CareTech дървото — структурата трябва да съвпада с текущата навигация
- [ ] **7.5** — В `/admin/category-mapping`: филтрирай по "Unmapped" → трябва да показва 28 категории (26 TEKRA + 2 ASBIS)
- [ ] **7.6** — Направи 28-те ръчни mappings за TEKRA и ASBIS leaf категории
- [ ] **7.7** — Провери: "Unmapped" = 0
- [ ] **7.8** — В CareTech дървото: ръчно задай `showInFooter=true` за категориите, които трябва да се показват в footer

---

## ФАЗА 8 — Frontend: Смяна на навигация и routing
> **Production-breaking промяна.** Прави се само след успешен seed, 28 ръчни mappings, и тестване в staging.
> URL pattern се сменя: `/category/{dist-slug}/{dist-id}` → `/category/{caretech-slug}/{caretech-id}`
> SEO redirect-ите (Фаза 10) трябва да са готови преди или едновременно с тази фаза.

- [ ] **8.1** — `App.js` — `dispatch(fetchCategories())` → `dispatch(fetchCaretechCategories())`
- [ ] **8.2** — `NavDropDown.jsx` — data source: `state.categories.categories` → `state.caretechCategories.categories`
  - `buildCategoryTree()` е вече имплементиран в `caretechCategorySlice.js`
- [ ] **8.3** — `Footer.jsx` — смени логика: `FOOTER_CATEGORY_IDS` whitelist → `showInFooter === true` от CareTech категориите
- [ ] **8.4** — `Category.jsx`
  - Смени API: `GET /api/products/categories/{id}/products` → `GET /api/caretech-categories/{id}/products`
  - Смени parameters API аналогично
  - `{id}` вече е CareTech category ID (идва от URL)
- [ ] **8.5** — `CategoryList.jsx` — аналогично на 8.4
- [ ] **8.6** — `Breadcrumbs.jsx` — провери работи ли с CareTech категории (логиката е идентична ако DTO-то има parentId)

---

## ФАЗА 9 — Backend: ProductForm (ръчни продукти)

- [ ] **9.1** — `ProductService.java`
  ```
  При create/update с подаден caretechCategoryId:
  - mappingRepo.findByDistributorCategoryId(???)
  
  ВНИМАНИЕ: Логиката е обратна — admin избира CareTech категория → backend намира
  дистрибуторска чрез mapping. Но 1 CareTech може да има МНОГО дистрибуторски!
  
  Решение: Ако за избраната CareTech категория има точно 1 mapping → автоматично.
  Ако има повече от 1 → admin трябва да избере конкретната дистрибуторска.
  Ако 0 mappings → грешка "Категорията няма свързани дистрибуторски категории".
  ```

- [ ] **9.2** — `ProductCreateRequestDTO.java` — добави `caretechCategoryId` (optional)
- [ ] **9.3** — `ProductForm.jsx` — category dropdown: `GET /api/admin/caretech-categories` вместо `/api/admin/categories/all`

---

## ФАЗА 10 — URL Redirects и SEO

- [ ] **10.1** — Spring Boot endpoint: `GET /api/category-redirect/{sourceDistributorId}`
  ```
  Намери caretech_category WHERE source_category_id = sourceDistributorId
  Ако намери → 301 Redirect към /category/{caretech-slug}/{caretech-id}
  Ако не → 404
  ```

- [ ] **10.2** — Nginx: redirect от стари URLs
  ```nginx
  # /category/{old-slug}/{old-dist-id} → 301 → /api/category-redirect/{old-dist-id}
  # Backend отговаря с 301 към новия URL
  ```

- [ ] **10.3** — `SitemapController.java` — добави CareTech категории (заместват дистрибуторските)
- [ ] **10.4** — Submit нов sitemap в Google Search Console

---

*Параметри и filter deduplication → `PARAMETER_MAPPING_PLAN.md` (отделен план).*
*Там: програмно rule-based matching с confidence score, без AI.*
