# План: CareTech Параметри + Филтър Дедупликация (AI-assisted)

> Статус: **ОДОБРЕН — в изпълнение**
> Дата: 2026-08-21
> Зависимост: Изпълнява се СЛЕД CATEGORY_MAPPING_PLAN.md (Фаза 3+)
> Предишно решение "без AI" е ОТМЕНЕНО — потребителят одобрява AI-assisted matching.

---

## Проблемът (root cause анализ)

### Защо се появяват 4 "Цвят" филтъра

В `ProductSearchRepository.buildFacets()` и `getAvailableParametersWithCountsForCategory()` facet key-ят е:
```java
String facetKey = paramId + ":" + paramName;  // "42:Цвят", "891:Color", "1234:Цвет"
```

Всеки `parameter.id` е уникален ред в `parameters` таблицата. Когато CareTech категория е свързана с VALI + TEKRA + ASBIS дистрибуторски категории, продуктите имат параметри от три платформи — три различни `parameter.id` → три отделни filter group-а в UI.

### Структура на текущите данни

```
parameters таблица:
  id=42,   name_bg="Цвят",   platform=VALI,   → options: "Черен", "Бял", "Сив"
  id=891,  name_bg="Color",  platform=TEKRA,  → options: "Black", "White", "Gray"
  id=1234, name_bg="Цвет",   platform=ASBIS,  → options: "черный", "белый"

product_parameters таблица:
  product_id=100 (VALI)  → parameter_id=42,   parameter_option_id=X (Черен)
  product_id=200 (TEKRA) → parameter_id=891,  parameter_option_id=Y (Black)
  product_id=300 (ASBIS) → parameter_id=1234, parameter_option_id=Z (черный)

Резултат в UI: три отделни филтъра "Цвят", "Color", "Цвет"
```

### Целевото поведение след имплементацията

```
caretech_parameters:
  id=1, name_bg="Цвят", name_en="Color"

distributor_parameter_mapping:
  parameter.id=42   → caretech_parameter.id=1
  parameter.id=891  → caretech_parameter.id=1
  parameter.id=1234 → caretech_parameter.id=1

buildFacets() след промяна — групира по caretech_parameter.id:
  facetKey = "1:Цвят"
  options: "Черен", "Бял", "Black", "White", "черный" (merged — Phase A)
         → "Черен", "Бял", "Сив" (normalized — Phase B, optional)

Резултат в UI: ЕДИН "Цвят" филтър с обединени стойности
```

---

## Архитектура

```
Phase A: Parameter name deduplication
─────────────────────────────────────────────────────
parameters (VALI/TEKRA/ASBIS/MOST)
     ↓
distributor_parameter_mapping (AI-suggested + human-reviewed)
     ↓
caretech_parameters (нормализирани имена)
     ↓
ProductSearchRepository (групира по caretech_parameter.id)

Phase B: Value normalization (отложена, опционална)
─────────────────────────────────────────────────────
parameter_options (raw distributor values)
     ↓
distributor_value_mapping (AI-suggested + human-reviewed)
     ↓
caretech_parameter_values (нормализирани стойности)
     ↓
ProductSearchRepository (групира и по caretech_value.id)
```

---

## Обхват на параметрите (приблизителна оценка)

По данни от `parameters` таблицата:
- VALI параметри: ~150-200 уникални
- TEKRA параметри: ~50-80 уникални
- ASBIS параметри: ~30-50 уникални
- MOST параметри: ~30-50 уникални
- **Общо: ~300-380 дистрибуторски параметъра за mapping**

След AI анализ очакваме:
- ~80-100 уникални CareTech параметъра (нормализирани)
- ~60-70% auto-approved (ясни съвпадения: RAM=RAM, CPU=Processor, ...)
- ~20-25% needs review (близки но неясни: "Cache" vs "Кеш памет")
- ~10-15% manual mapping (единични параметри без аналог)

---

## ФАЗА A — База данни (Parameter Names)

> Нови Flyway migrations. Не засяга нищо съществуващо.

- [ ] **A.1** — `V37__add_caretech_parameters.sql`

```sql
CREATE TABLE caretech_parameters (
    id           BIGSERIAL PRIMARY KEY,
    name_bg      VARCHAR(255) NOT NULL,
    name_en      VARCHAR(255),
    slug         VARCHAR(200) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_filter    BOOLEAN NOT NULL DEFAULT true,
    show_flag    BOOLEAN NOT NULL DEFAULT true,
    data_type    VARCHAR(50) NOT NULL DEFAULT 'STRING',
    -- STRING = категорийни стойности (Цвят, Марка)
    -- NUMBER  = числови (честота, тегло — за бъдеще)
    -- BOOLEAN = да/не (има ли уеб камера)
    unit         VARCHAR(50),   -- напр. "GB", "Hz", "W", "kg" — за NUMBER тип
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(100) DEFAULT 'system'
);

CREATE INDEX idx_caretech_params_slug      ON caretech_parameters(slug);
CREATE INDEX idx_caretech_params_is_filter ON caretech_parameters(is_filter);
CREATE INDEX idx_caretech_params_show      ON caretech_parameters(show_flag);
```

- [ ] **A.2** — `V38__add_distributor_parameter_mapping.sql`

```sql
CREATE TABLE distributor_parameter_mapping (
    id                      BIGSERIAL PRIMARY KEY,
    distributor_parameter_id BIGINT NOT NULL REFERENCES parameters(id) ON DELETE CASCADE,
    caretech_parameter_id   BIGINT NOT NULL REFERENCES caretech_parameters(id) ON DELETE CASCADE,
    confidence_score        DECIMAL(4,3),   -- AI score: 0.000 – 1.000
    mapping_source          VARCHAR(50) NOT NULL,
    -- 'AI_AUTO'   = AI предложи, auto-approved (score ≥ 0.85)
    -- 'AI_REVIEW' = AI предложи, Admin одобри ръчно
    -- 'MANUAL'    = Admin добави ръчно без AI
    review_status           VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
    -- 'PENDING'  = в опашката за преглед
    -- 'APPROVED' = одобрен (активен mapping)
    -- 'REJECTED' = отхвърлен от admin
    reviewed_by             VARCHAR(100),
    reviewed_at             TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100) DEFAULT 'system',
    CONSTRAINT uq_distributor_parameter UNIQUE (distributor_parameter_id)
    -- 1 дистрибуторски параметър → само 1 CareTech параметър
);

CREATE INDEX idx_dist_param_mapping_dist     ON distributor_parameter_mapping(distributor_parameter_id);
CREATE INDEX idx_dist_param_mapping_caretech ON distributor_parameter_mapping(caretech_parameter_id);
CREATE INDEX idx_dist_param_mapping_status   ON distributor_parameter_mapping(review_status);
```

- [ ] **A.3** — `V39__add_ai_parameter_suggestions.sql`

```sql
CREATE TABLE ai_parameter_suggestions (
    id                          BIGSERIAL PRIMARY KEY,
    distributor_parameter_id    BIGINT NOT NULL REFERENCES parameters(id) ON DELETE CASCADE,
    -- Предложение за СЪЩЕСТВУВАЩ CareTech параметър:
    suggested_caretech_param_id BIGINT REFERENCES caretech_parameters(id) ON DELETE SET NULL,
    -- [DB-ANALYSIS RISK-08]: SET NULL при изтрит caretech param, не CASCADE
    -- Suggestion с null suggested_caretech_param_id трябва да се маркира за ресинхронизация
    -- НОВО ИМЕ (ако AI предлага нов caretech параметър, не съществуващ):
    suggested_name_bg           VARCHAR(255),
    suggested_name_en           VARCHAR(255),
    is_new_caretech_param       BOOLEAN NOT NULL DEFAULT false,
    confidence_score            DECIMAL(4,3) NOT NULL,
    ai_reasoning                TEXT,       -- AI обяснение на решението
    status                      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    -- 'PENDING'      = чака преглед от admin
    -- 'AUTO_APPROVED'= auto-approved (score ≥ 0.85), вече има mapping
    -- 'APPROVED'     = одобрен от admin
    -- 'REJECTED'     = отхвърлен от admin (admin ще направи ръчен mapping)
    batch_id                    VARCHAR(100) NOT NULL,  -- UUID на batch run-а
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_suggestions_dist_param ON ai_parameter_suggestions(distributor_parameter_id);
CREATE INDEX idx_ai_suggestions_status     ON ai_parameter_suggestions(status);
CREATE INDEX idx_ai_suggestions_batch      ON ai_parameter_suggestions(batch_id);
CREATE INDEX idx_ai_suggestions_score      ON ai_parameter_suggestions(confidence_score DESC);
```

---

## ФАЗА B — База данни (Value Normalization) — ОТЛОЖЕНА

> Имплементира се само след като Phase A е завършена и тествана в production.
> Phase A решава основния проблем (дублирани filter группи).
> Phase B решава secondary проблем (смесени стойности вътре в группата).

- [ ] **B.1** — `V40__add_caretech_parameter_values.sql`

```sql
CREATE TABLE caretech_parameter_values (
    id                     BIGSERIAL PRIMARY KEY,
    caretech_parameter_id  BIGINT NOT NULL REFERENCES caretech_parameters(id) ON DELETE CASCADE,
    value_bg               VARCHAR(500) NOT NULL,
    value_en               VARCHAR(500),
    display_order          INTEGER NOT NULL DEFAULT 0,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_caretech_values_param ON caretech_parameter_values(caretech_parameter_id);
```

- [ ] **B.2** — `V41__add_distributor_value_mapping.sql`

```sql
-- Маппира конкретна стойност от конкретен дистрибуторски параметър
-- към нормализирана CareTech стойност
CREATE TABLE distributor_value_mapping (
    id                       BIGSERIAL PRIMARY KEY,
    distributor_parameter_id BIGINT NOT NULL REFERENCES parameters(id) ON DELETE CASCADE,
    raw_value                VARCHAR(500) NOT NULL,   -- точната стойност от distributor
    caretech_value_id        BIGINT REFERENCES caretech_parameter_values(id),
    confidence_score         DECIMAL(4,3),
    mapping_source           VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    review_status            VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dist_param_raw_value UNIQUE (distributor_parameter_id, raw_value)
);

CREATE INDEX idx_dist_value_mapping_param ON distributor_value_mapping(distributor_parameter_id);
CREATE INDEX idx_dist_value_mapping_value ON distributor_value_mapping(caretech_value_id);
```

---

## ФАЗА 1 — Backend: Entity и Repository (Phase A)

- [ ] **1.1** — `CaretechParameter.java` (Entity)
  ```
  Пакет: com.techstore.entity
  Полета: nameBg, nameEn, slug, displayOrder, isFilter, show (show_flag),
          dataType (enum: STRING/NUMBER/BOOLEAN), unit
  Audit: createdAt, updatedAt, createdBy

  НЕ добавяй M2M с caretech_categories — вижте ФАЗА 4 за dynamic resolution
  ```

- [ ] **1.2** — `DistributorParameterMapping.java` (Entity)
  ```
  - distributorParameter: @ManyToOne(fetch=LAZY) → Parameter
  - caretechParameter: @ManyToOne(fetch=LAZY) → CaretechParameter
  - confidenceScore (BigDecimal 4,3)
  - mappingSource (enum: AI_AUTO, AI_REVIEW, MANUAL)
  - reviewStatus (enum: PENDING, APPROVED, REJECTED)
  - reviewedBy, reviewedAt
  - createdAt, createdBy
  ```

- [ ] **1.3** — `AiParameterSuggestion.java` (Entity)
  ```
  - distributorParameter: @ManyToOne(fetch=LAZY) → Parameter
  - suggestedCaretechParam: @ManyToOne(fetch=LAZY, optional=true) → CaretechParameter
  - suggestedNameBg, suggestedNameEn (за нов caretech param)
  - isNewCaretechParam (boolean)
  - confidenceScore (BigDecimal 4,3)
  - aiReasoning (TEXT)
  - status (enum: PENDING, AUTO_APPROVED, APPROVED, REJECTED)
  - batchId (String)
  - createdAt
  ```

- [ ] **1.4** — `CaretechParameterRepository.java`
  ```java
  List<CaretechParameter> findByShowFlagTrueOrderByDisplayOrderAsc();
  List<CaretechParameter> findAllByOrderByDisplayOrderAsc();
  Optional<CaretechParameter> findBySlug(String slug);
  boolean existsBySlug(String slug);
  boolean existsBySlugAndIdNot(String slug, Long id);
  boolean existsByNameBgIgnoreCase(String nameBg);
  Optional<CaretechParameter> findByNameBgIgnoreCase(String nameBg);
  ```

- [ ] **1.5** — `DistributorParameterMappingRepository.java`
  ```java
  Optional<DistributorParameterMapping> findByDistributorParameterId(Long paramId);
  List<DistributorParameterMapping> findByCaretechParameterId(Long caretechId);
  List<DistributorParameterMapping> findByCaretechParameterIdIn(Collection<Long> caretechIds);
  boolean existsByDistributorParameterId(Long paramId);
  long countByCaretechParameterId(Long caretechId);

  // За admin listing на unmapped параметри
  // [DB-ANALYSIS RISK-09]: AND EXISTS (ProductParameter) — само параметри с реални продукти
  @Query("SELECT p FROM Parameter p WHERE NOT EXISTS " +
         "(SELECT 1 FROM DistributorParameterMapping m WHERE m.distributorParameter.id = p.id " +
         " AND m.reviewStatus = 'APPROVED') " +
         "AND EXISTS (SELECT 1 FROM ProductParameter pp WHERE pp.parameter.id = p.id)")
  List<Parameter> findUnmappedParameters();

  // За admin listing с mapping статус (platform filter)
  @Query("SELECT p FROM Parameter p " +
         "LEFT JOIN DistributorParameterMapping m ON m.distributorParameter.id = p.id " +
         "WHERE (:platform IS NULL OR p.platform = :platform) " +
         "ORDER BY p.platform, p.nameBg")
  List<Object[]> findAllWithMappingStatus(@Param("platform") Platform platform);
  ```

- [ ] **1.6** — `AiParameterSuggestionRepository.java`
  ```java
  List<AiParameterSuggestion> findByStatusOrderByConfidenceScoreDesc(SuggestionStatus status);
  List<AiParameterSuggestion> findByBatchIdAndStatus(String batchId, SuggestionStatus status);
  List<AiParameterSuggestion> findByDistributorParameterId(Long paramId);
  long countByStatus(SuggestionStatus status);

  // Агрегирана статистика за batch
  @Query("SELECT s.status, COUNT(s) FROM AiParameterSuggestion s " +
         "WHERE s.batchId = :batchId GROUP BY s.status")
  List<Object[]> countByBatchIdGroupByStatus(@Param("batchId") String batchId);
  ```

---

## ФАЗА 2 — Backend: AI Service

- [ ] **2.0** — `DelegatingSecurityContextAsyncTaskExecutor` конфигурация
  ```java
  // [DB-ANALYSIS RISK-15]: @Async AI analysis трябва да прехвърля Spring Security context
  // Добавяй в AsyncConfig.java:
  @Bean("aiAnalysisExecutor")
  public Executor aiAnalysisExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(2);
      executor.setMaxPoolSize(4);
      executor.setThreadNamePrefix("ai-analysis-");
      executor.initialize();
      return new DelegatingSecurityContextAsyncTaskExecutor(executor);
      // Така Security context (и @CreatedBy) работи правилно в async threads
  }
  ```

- [ ] **2.1** — `AnthropicApiProperties.java` (Configuration)
  ```java
  @ConfigurationProperties(prefix = "anthropic")
  @Data
  public class AnthropicApiProperties {
      private String apiKey;      // from application.properties: anthropic.api-key=${ANTHROPIC_API_KEY}
      private String model = "claude-haiku-4-5-20251001";  // fast + cheap за batch analysis
      private int maxTokens = 4096;
      private int batchSize = 60;          // параметри на AI call
      private double autoApproveThreshold = 0.85;  // ≥ това → auto-approve
      private double needsReviewThreshold = 0.60;  // < това → manual only
      // 0.60-0.85 → needs review queue
  }
  ```

- [ ] **2.2** — `ParameterMappingAiService.java`

  **Основен метод: `runAnalysis()`**
  ```java
  @Transactional
  public AiBatchResultDto runAnalysis() {
      String batchId = UUID.randomUUID().toString();
      List<Parameter> unmapped = mappingRepo.findUnmappedParameters();

      if (unmapped.isEmpty()) {
          return AiBatchResultDto.empty(batchId);
      }

      // Вземи всички съществуващи CareTech параметри (за да може AI да ги ползва)
      List<CaretechParameter> existing = caretechParamRepo.findAllByOrderByDisplayOrderAsc();

      // Batch по 60 параметъра
      List<List<Parameter>> batches = Lists.partition(unmapped, props.getBatchSize());

      int autoApproved = 0, needsReview = 0, lowConfidence = 0;

      for (List<Parameter> batch : batches) {
          try {
              List<AiSuggestionRaw> suggestions = callClaudeApi(batch, existing, batchId);
              for (AiSuggestionRaw s : suggestions) {
                  processSuggestion(s, batchId);
                  if (s.score >= AUTO_APPROVE) autoApproved++;
                  else if (s.score >= NEEDS_REVIEW) needsReview++;
                  else lowConfidence++;
              }
              Thread.sleep(500); // rate limit пауза между batch-ове
          } catch (Exception e) {
              log.error("Batch {} failed: {}", batchId, e.getMessage());
              // Продължи с следващия batch
          }
      }

      return AiBatchResultDto.of(batchId, autoApproved, needsReview, lowConfidence);
  }
  ```

  **AI prompt (изпраща се към Claude):**
  ```
  SYSTEM: You are a product database specialist analyzing e-commerce filter parameters.
  You will group distributor parameters that represent the same product attribute.

  USER:
  You have the following EXISTING CareTech normalized parameters (use their IDs if applicable):
  {existing_caretech_params_as_json}

  Analyze these distributor parameters and for each one, suggest:
  1. If it matches an EXISTING CareTech parameter → provide its ID
  2. If it should be a NEW CareTech parameter → provide suggested Bulgarian and English names
  3. Confidence score 0.0-1.0 with reasoning

  Confidence guide:
  - 0.90-1.00: Certain (exact same concept, different language/case/spelling)
  - 0.75-0.89: High confidence (very likely same concept, minor ambiguity)
  - 0.60-0.74: Medium confidence (probably same, but needs human review)
  - 0.00-0.59: Low confidence (unclear match, flag for manual mapping)

  Distributor parameters to analyze:
  {parameters_as_json_array}

  IMPORTANT:
  - Prefer matching to EXISTING CareTech params when semantically correct
  - For new CareTech params: use professional Bulgarian terminology
  - DO NOT merge parameters that are genuinely different (e.g., "RAM" ≠ "Storage")
  - Technical abbreviations: RAM, CPU, GPU, SSD, HDD — keep as-is in name_en

  Return ONLY valid JSON array:
  [
    {
      "distributorParameterId": 42,
      "existingCaretechParameterId": 5,
      "isNew": false,
      "suggestedNameBg": null,
      "suggestedNameEn": null,
      "confidenceScore": 0.97,
      "reasoning": "'Цвят' (VALI BG) maps to existing CareTech param 'Цвят' id=5"
    },
    {
      "distributorParameterId": 891,
      "existingCaretechParameterId": null,
      "isNew": true,
      "suggestedNameBg": "Оперативна памет",
      "suggestedNameEn": "RAM",
      "confidenceScore": 0.92,
      "reasoning": "'Memory' (TEKRA EN) clearly refers to RAM/оперативна памет"
    }
  ]
  ```

  **`callClaudeApi()` имплементация:**
  ```java
  // POST https://api.anthropic.com/v1/messages
  // Headers: x-api-key, anthropic-version: 2023-06-01, content-type: application/json
  // Body: { model, max_tokens, system, messages: [{role:"user", content:...}] }
  // Parse response.content[0].text → JSON array → List<AiSuggestionRaw>
  // При JSON parse грешка → log + return empty (не блокира целия batch)
  ```

  **`processSuggestion()` имплементация:**
  ```java
  private void processSuggestion(AiSuggestionRaw s, String batchId) {
      // 1. Намери или създай CaretechParameter
      CaretechParameter ct;
      if (!s.isNew && s.existingCaretechParameterId != null) {
          ct = caretechParamRepo.findById(s.existingCaretechParameterId).orElse(null);
          if (ct == null) {
              // AI hallucinated an ID — treat as new
              s.isNew = true;
          }
      }
      if (s.isNew || ct == null) {
          // Провери дали вече не съществува по name (от предишен batch)
          ct = caretechParamRepo.findByNameBgIgnoreCase(s.suggestedNameBg)
                                .orElseGet(() -> createNewCaretechParam(s));
      }

      // 2. Запази suggestion
      AiParameterSuggestion suggestion = new AiParameterSuggestion();
      suggestion.setDistributorParameterId(s.distributorParameterId);
      suggestion.setSuggestedCaretechParam(ct);
      suggestion.setConfidenceScore(s.confidenceScore);
      suggestion.setAiReasoning(s.reasoning);
      suggestion.setBatchId(batchId);

      if (s.confidenceScore >= props.getAutoApproveThreshold()) {
          // Auto-approve: създай mapping веднага
          suggestion.setStatus(SuggestionStatus.AUTO_APPROVED);
          createMapping(s.distributorParameterId, ct.getId(),
                        s.confidenceScore, MappingSource.AI_AUTO);
      } else {
          suggestion.setStatus(SuggestionStatus.PENDING);
          // Не създавай mapping — чака human review
      }

      suggestionRepo.save(suggestion);
  }
  ```

  **`createMapping()` — централна помощна функция:**
  ```java
  private void createMapping(Long distParamId, Long caretechParamId,
                              BigDecimal score, MappingSource source) {
      // Проверка: вече mapped?
      if (mappingRepo.existsByDistributorParameterId(distParamId)) {
          log.warn("Distributor param {} already mapped — skipping", distParamId);
          return;
      }
      DistributorParameterMapping m = new DistributorParameterMapping();
      m.setDistributorParameterId(distParamId);
      m.setCaretechParameterId(caretechParamId);
      m.setConfidenceScore(score);
      m.setMappingSource(source);
      m.setReviewStatus(ReviewStatus.APPROVED);
      mappingRepo.save(m);
  }
  ```

---

## ФАЗА 3 — Backend: Services

- [ ] **3.1** — `CaretechParameterService.java`

  - [ ] **3.1.1** `getAllParameters()` — `findByShowFlagTrueOrderByDisplayOrderAsc()` → DTOs (публично)
  - [ ] **3.1.2** `getAllParametersForAdmin()` — включва скрити
  - [ ] **3.1.3** `createParameter(dto)` — валидира: nameBg, slug (auto-gen ако липсва), existsBySlug
  - [ ] **3.1.4** `updateParameter(Long id, dto)` — existsBySlugAndIdNot при slug промяна
  - [ ] **3.1.5** `deleteParameter(Long id)` — проверява дали има active mappings → блокира

- [ ] **3.2** — `ParameterMappingService.java`

  - [ ] **3.2.1** `getReviewQueue()` — всички `status=PENDING` suggestions, сортирани по `confidence_score DESC`
    ```
    Връща: List<AiSuggestionReviewDto> {
      suggestion (AI suggestion детайли),
      distributorParam (name_bg, name_en, platform, external_id),
      suggestedCaretechParam (id, name_bg),
      confidenceScore,
      aiReasoning,
      status
    }
    ```

  - [ ] **3.2.2** `approveSuggestion(Long suggestionId, String reviewer)`
    ```
    1. Намери suggestion
    2. suggestion.status != PENDING → грешка
    3. Ако isNew: увери се че CaretechParam вече е създаден (от processSuggestion)
    4. createMapping(distParamId, caretechParamId, score, AI_REVIEW)
    5. suggestion.status = APPROVED, reviewedBy, reviewedAt = now
    ```

  - [ ] **3.2.3** `rejectAndReassign(Long suggestionId, Long newCaretechParamId, String reviewer)`
    ```
    1. suggestion.status = REJECTED
    2. createMapping(distParamId, newCaretechParamId, null, AI_REVIEW)
    ```

  - [ ] **3.2.4** `rejectSuggestion(Long suggestionId, String reviewer)`
    ```
    1. suggestion.status = REJECTED
    2. НЕ създава mapping — admin ще го направи ръчно от manual mapping UI
    ```

  - [ ] **3.2.5** `createManualMapping(Long distParamId, Long caretechParamId, String creator)`
    ```
    1. Провери дали distParam е вече mapped → грешка
    2. createMapping(distParamId, caretechParamId, null, MANUAL)
    ```

  - [ ] **3.2.6** `deleteMapping(Long mappingId)`
    ```
    Изтрий mapping. Ако имаше AI suggestion за този param → set status = PENDING отново
    (за да може admin да преразгледа)
    ```

  - [ ] **3.2.7** `getParameterMappingStats()` → `ParameterMappingStatsDto`
    ```
    {
      totalDistributorParams: N,
      mapped: N,
      unmapped: N,
      pendingReview: N (AI suggestions PENDING),
      autoApproved: N,
      manuallyMapped: N,
      byPlatform: { VALI: {total, mapped}, TEKRA: {...}, ... }
    }
    ```

  - [ ] **3.2.8** `batchApproveHighConfidence(double minScore, String reviewer)`
    ```
    Approve всички PENDING suggestions с score ≥ minScore.
    Удобен shortcut за admin: "Одобри всички над 0.80"
    Връща int approvedCount.
    ```

  - [ ] **3.2.9** `getAllMappings(Platform platform)` → List<DistributorParameterMappingDto>
    - За admin listing: всички mappings, опционален platform филтър

  - [ ] **3.2.10** `getUnmappedParameters(Platform platform)` → List<UnmappedParameterDto>
    - За manual mapping UI: всички параметри без одобрен mapping

---

## ФАЗА 4 — Backend: Промени в Search (КРИТИЧНА ФАЗА)

> Тук е основната промяна която решава дублирания проблем.
> Засяга `ProductSearchRepository` — внимателно тестване!

- [ ] **4.1** — `ProductSearchRepository.getAvailableParametersWithCountsForCategory()`

  **СЕГА (по категория — 1 дистрибуторска):**
  ```sql
  WHERE cp.category_id = :categoryId
  GROUP BY param.id, param.name, po.id, po.name
  -- Key: "42:Цвят", "891:Color" → duplicate filter groups
  ```

  **СЛЕД (по CareTech категория — N дистрибуторски):**
  ```sql
  -- Новата версия приема List<Long> distributorCategoryIds (не 1 categoryId)
  -- и JOIN-ва с distributor_parameter_mapping + caretech_parameters

  SELECT
      cp_param.id       AS caretech_param_id,
      cp_param.name_bg  AS caretech_param_name,
      po.id             AS option_id,
      po.name_bg        AS option_name,
      COUNT(DISTINCT pp.product_id) AS product_count
  FROM parameters param
  JOIN category_parameters cp ON cp.parameter_id = param.id
  JOIN distributor_parameter_mapping dpm ON dpm.distributor_parameter_id = param.id
       AND dpm.review_status = 'APPROVED'
  JOIN caretech_parameters cp_param ON cp_param.id = dpm.caretech_parameter_id
       AND cp_param.show_flag = true AND cp_param.is_filter = true
  JOIN parameter_options po ON po.parameter_id = param.id
  INNER JOIN product_parameters pp ON pp.parameter_option_id = po.id
  INNER JOIN products p ON pp.product_id = p.id
       AND p.active = true AND p.show_flag = true
       AND p.status = 'AVAILABLE'
       AND (p.image_url IS NOT NULL AND p.image_url <> '')
  WHERE cp.category_id IN (:distributorCategoryIds)
    AND cp.is_filter != false
  GROUP BY cp_param.id, caretech_param_name, po.id, option_name, cp_param.display_order
  HAVING COUNT(DISTINCT pp.product_id) > 0
  ORDER BY cp_param.display_order, caretech_param_name, po.sort_order, option_name
  ```

  **Нов facet key формат:**
  ```java
  String key = caretechParamId + ":" + caretechParamName;
  // напр. "1:Цвят" — уникален по caretech param ID, не distributor param ID
  ```

  **ВАЖНО:** Стойностите в ЕДИН "Цвят" ключ ще съдържат "Черен" + "Black" + "черный" (Phase A).
  Phase B ще ги нормализира до "Черен", "Бял" и т.н.

- [ ] **4.2** — `ProductSearchRepository.buildFacets()` — аналогична промяна

  При активни филтри WHERE клаузата трябва да поддържа:
  - Input filter key: `caretech_param_id` (вместо `distributor_param_id`)
  - Транслация: caretech_param_id → намери всички `distributor_parameter_id` чрез `distributor_parameter_mapping` → `pp.parameter_id IN (distIds) AND pp.parameter_option_id IN (selectedOptionIds)`

- [ ] **4.3** — `ProductSearchService` — нов method signature

  ```java
  // Сегашен (остава за backward compat):
  public Map<String, List<FacetValue>> getAvailableParametersWithCountsForCategory(
      Long categoryId, String language)

  // Нов (за CareTech):
  public Map<String, List<FacetValue>> getAvailableParametersForCaretechCategory(
      Long caretechCategoryId, String language)
  // Вика CaretechCategoryService.resolveDistributorCategoryIds(caretechCategoryId)
  // След това вика новия ProductSearchRepository метод с List<Long> distCategoryIds
  ```

- [ ] **4.4** — `ProductSearchRequest` — промяна на filters тип

  ```java
  // Сегашен:
  private Map<Long, Set<Long>> filters;  // distributorParamId → Set<optionId>

  // След Phase A: добави ново поле (backward compat):
  private Map<Long, Set<Long>> caretechFilters;  // caretechParamId → Set<optionId>
  // OR: нов endpoint за CareTech search с нов DTO
  ```

  **Препоръка:** Нов `/api/caretech-categories/{id}/products` endpoint с нов DTO `CaretechProductSearchRequest` вместо промяна на съществуващия `ProductSearchRequest`. По-чисто, не счупва съществуващото API.

- [ ] **4.5** — `CaretechCategoryService.getParametersByCaretech()` — update

  ```java
  // Сегашната имплементация (placeholder от CATEGORY_MAPPING_PLAN.md):
  // → resolves CareTech descendants → dist IDs → връща параметри с дублиране (ВРЕМЕННО)

  // След Phase A — правилната имплементация:
  public Map<String, List<FacetValue>> getParametersByCaretech(Long id, String language) {
      // Вика getAvailableParametersForCaretechCategory(id, language)
      // Вече WITHOUT дублиране!
  }
  ```

---

## ФАЗА 5 — Backend: DTOs и Controllers

- [ ] **5.1** — DTOs (пакет: `dto/caretech/parameters/`)

  - [ ] `CaretechParameterRequestDto`
    ```
    nameBg (required), nameEn, slug (optional — auto-gen), displayOrder,
    isFilter (default true), show (default true), dataType, unit
    ```

  - [ ] `CaretechParameterResponseDto`
    ```
    id, nameBg, nameEn, slug, displayOrder, isFilter, show, dataType, unit,
    mappingsCount (long — от countByCaretechParameterId),
    distributorParameters (List<DistributorParamSummary> — ако поискани)
    ```

  - [ ] `AiSuggestionReviewDto`
    ```
    id (suggestion id), batchId, createdAt,
    distributorParam: { id, nameBg, nameEn, platform, externalId },
    suggestedCaretechParam: { id, nameBg, nameEn } (null ако isNew=true),
    suggestedNameBg, suggestedNameEn (само ако isNew=true),
    isNew (boolean),
    confidenceScore (BigDecimal),
    aiReasoning (String),
    status
    ```

  - [ ] `DistributorParameterMappingDto`
    ```
    id, distributorParamId, distributorParamNameBg, distributorParamNameEn,
    distributorPlatform (Platform), externalId,
    caretechParamId, caretechParamNameBg,
    confidenceScore, mappingSource, reviewStatus
    ```

  - [ ] `UnmappedParameterDto`
    ```
    id, nameBg, nameEn, platform (Platform), externalId,
    hasPendingAiSuggestion (boolean — има ли PENDING suggestion)
    ```

  - [ ] `ParameterMappingStatsDto`
    ```
    totalDistributorParams, mapped, unmapped, pendingReview,
    autoApproved, manuallyMapped,
    byPlatform: Map<String, PlatformStats>
    ```

  - [ ] `AiBatchResultDto`
    ```
    batchId, totalAnalyzed, autoApproved, needsReview,
    lowConfidence, errors, startedAt, completedAt
    ```

  - [ ] `BatchApproveResultDto`
    ```
    approved (int), skipped (int), errors (int)
    ```

- [ ] **5.2** — `CaretechParameterController.java`

  **Публични endpoints (permitAll):**
  - [ ] `GET /api/caretech-parameters` — всички видими filter параметри

  **Admin endpoints (ADMIN | SUPER_ADMIN):**
  - [ ] `GET /api/admin/caretech-parameters` — всички (включва скрити)
  - [ ] `POST /api/admin/caretech-parameters` — create
  - [ ] `PUT /api/admin/caretech-parameters/{id}` — update
  - [ ] `DELETE /api/admin/caretech-parameters/{id}` — soft delete

- [ ] **5.3** — `ParameterMappingController.java`

  **Admin endpoints (ADMIN | SUPER_ADMIN):**
  - [ ] `GET /api/admin/parameter-mapping/stats` — `ParameterMappingStatsDto`
  - [ ] `GET /api/admin/parameter-mapping/review-queue` — AI suggestions за преглед
    - Query params: `page, size` (пагинация — може да са стотици)
  - [ ] `POST /api/admin/parameter-mapping/review/{suggestionId}/approve` — одобри suggestion
  - [ ] `POST /api/admin/parameter-mapping/review/{suggestionId}/reject` — отхвърли (без reassign)
  - [ ] `POST /api/admin/parameter-mapping/review/{suggestionId}/reassign` — отхвърли + ново mapping
    - Body: `{ newCaretechParameterId: Long }`
  - [ ] `POST /api/admin/parameter-mapping/review/batch-approve` — одобри всички над threshold
    - Body: `{ minScore: double }` (напр. 0.80)
    - Връща: `BatchApproveResultDto`
  - [ ] `GET /api/admin/parameter-mapping/mappings` — всички approved mappings
    - Query params: `platform` (optional filter)
  - [ ] `GET /api/admin/parameter-mapping/unmapped` — unmapped параметри
    - Query params: `platform` (optional filter)
  - [ ] `POST /api/admin/parameter-mapping/manual` — ръчен mapping
    - Body: `CategoryMappingRequestDto { distributorParameterId, caretechParameterId }`
  - [ ] `DELETE /api/admin/parameter-mapping/{mappingId}` — изтрий mapping

  **AI Analysis endpoints (SUPER_ADMIN ONLY):**
  - [ ] `POST /api/admin/parameter-mapping/analyze` — стартирай AI analysis batch
    - Асинхронно (`@Async`) — връща `{ batchId: "uuid", message: "Анализът е стартиран" }` веднага
    - Frontend polling на status endpoint
  - [ ] `GET /api/admin/parameter-mapping/analyze/{batchId}/status` — статус на batch
    - Връща `AiBatchResultDto` (прогрес + резултати)

- [ ] **5.4** — `SecurityConfig.java` — добави:
  ```java
  .requestMatchers("/api/caretech-parameters/**").permitAll()
  .requestMatchers("/api/admin/caretech-parameters/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
  .requestMatchers("/api/admin/parameter-mapping/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
  // AI analyze — SUPER_ADMIN only:
  .requestMatchers(HttpMethod.POST, "/api/admin/parameter-mapping/analyze")
      .hasRole("SUPER_ADMIN")
  ```

---

## ФАЗА 6 — Frontend: Redux

- [ ] **6.1** — `caretechParameterSlice.js`
  ```javascript
  State: { parameters: [], adminParameters: [], status, error }
  Thunks:
    fetchCaretechParameters          → GET /api/caretech-parameters
    fetchAllCaretechParametersAdmin  → GET /api/admin/caretech-parameters
    createCaretechParameter          → POST /api/admin/caretech-parameters
    updateCaretechParameter          → PUT /api/admin/caretech-parameters/{id}
    deleteCaretechParameter          → DELETE /api/admin/caretech-parameters/{id}
  ```

- [ ] **6.2** — `parameterMappingSlice.js`
  ```javascript
  State: {
    stats: null,
    reviewQueue: [],           // AI suggestions за преглед
    mappings: [],              // approved mappings
    unmapped: [],              // unmapped параметри за manual
    batchStatus: null,         // текущ AI batch статус
    status, error
  }
  Thunks:
    fetchMappingStats          → GET /api/admin/parameter-mapping/stats
    fetchReviewQueue           → GET /api/admin/parameter-mapping/review-queue
    approveSuggestion          → POST .../review/{id}/approve
    rejectSuggestion           → POST .../review/{id}/reject
    reassignSuggestion         → POST .../review/{id}/reassign
    batchApprove               → POST .../review/batch-approve
    fetchMappings              → GET .../mappings?platform=
    fetchUnmapped              → GET .../unmapped?platform=
    createManualMapping        → POST .../manual
    deleteMapping              → DELETE .../{id}
    startAiAnalysis            → POST .../analyze (async)
    pollBatchStatus(batchId)   → GET .../analyze/{batchId}/status
  ```

- [ ] **6.3** — `store.js` — добави `caretechParameters` и `parameterMapping` reducers

---

## ФАЗА 7 — Frontend: Admin UI

- [ ] **7.1** — `CaretechParametersLayout.jsx` — управление на CareTech параметри
  ```
  Таблица с всички caretech_parameters:
    Колони: ID, Имe (BG/EN), Slug, Тип, Филтър (badge), Показан (badge),
            Mappings (брой), Действия

  Действия на ред: [✏ редактирай], [👁 скрий/покажи], [🗑 изтрий]

  Create/Edit Modal:
    nameBg (required), nameEn, slug (auto-gen), displayOrder, isFilter toggle,
    show toggle, dataType selector, unit

  Stats header: "X параметъра | Y видими | Z с mappings"
  ```

- [ ] **7.2** — `ParameterMappingLayout.jsx` — главен UI за mapping и AI review
  ```
  TABS: [Review Queue (N)] [Mappings] [Unmapped (N)] [Stats]
  ```

  **Tab: Review Queue**
  ```
  Header: "AI предложи X mapping-а. Под 0.85: N за преглед."
  [Одобри всички над 0.80] бутон (confirmation modal → batchApprove)

  Таблица с пагинация (50 на страница):
    Колона 1: Дистрибуторски параметър
      - Платформа badge (VALI=blue, TEKRA=red, ASBIS=green, MOST=purple)
      - nameBg + nameEn
      - external_id

    Колона 2: AI предложение
      - [СЪЩЕСТВУВАЩ] или [НОВ] badge
      - Предложено CareTech имe (BG + EN)
      - Ако [НОВ]: "(ще се създаде нов параметър)"

    Колона 3: Увереност
      - Цветна progress bar: ≥0.85=зелено, 0.60-0.85=жълто, <0.60=червено
      - Числова стойност: "0.82"
      - Reasoning tooltip (ℹ) → показва AI обяснението при hover

    Колона 4: Действия
      [✓ Одобри] → approve
      [↩ Преназначи] → dropdown от всички caretech_parameters → reassign
      [✗ Отхвърли] → reject (отива в Unmapped tab)

  Филтри над таблицата:
    Platform: All | VALI | TEKRA | ASBIS | MOST
    Sort: По увереност ↓ (default) | По платформа | По ime
  ```

  **Tab: Mappings**
  ```
  Списък на всички approved mappings.
  Филтър по платформа.
  [✗ Изтрий] до всеки mapping (confirmation modal).
  Source badge: [AI AUTO] / [AI REVIEW] / [MANUAL]
  Confidence badge (ако е AI mapping).
  ```

  **Tab: Unmapped**
  ```
  Параметри без одобрен mapping (отхвърлени от AI или с много нисък score).
  За всеки: [Свържи с →] dropdown от caretech_parameters → createManualMapping
  Или: [+ Нов CareTech параметър] → Create modal → след създаване → auto-link
  [AI suggestion pending] badge ако има PENDING suggestion (може да одобри от там)
  ```

  **Tab: Stats**
  ```
  Dashboard с ParameterMappingStatsDto:
  - Pie chart: Mapped vs Unmapped vs Pending Review
  - Table by platform: VALI (X/Y mapped), TEKRA, ASBIS, MOST
  - [▶ Стартирай AI Анализ] бутон (само SUPER_ADMIN, само ако unmapped > 0)
    → confirmation: "Ще се анализират N параметъра. Процесът може да отнеме ~2-5 минути."
    → след стартиране: показва progress с polling на batch status
    → при завършване: refresh stats + review queue
  ```

- [ ] **7.3** — `Sidebar.jsx` — добави 2 нови menu items:
  - "CareTech Параметри" → `/admin/caretech-parameters`
  - "Свързване на параметри" → `/admin/parameter-mapping`

- [ ] **7.4** — `App.js` — добави routes за двете нови admin pages

---

## ФАЗА 8 — Admin действие: AI Analysis + Review

> Само след като Фази A и 1-7 са завършени и тествани.

- [ ] **8.1** — Влез в `/admin/parameter-mapping` → Tab "Stats"
- [ ] **8.2** — Провери Stats: totalDistributorParams = очакван брой (~350)
- [ ] **8.3** — Кликни "Стартирай AI Анализ" (SUPER_ADMIN)
- [ ] **8.4** — Изчакай да завърши (~3-7 мин за ~350 параметъра в ~6 batch-а)
- [ ] **8.5** — Провери AiBatchResultDto: autoApproved≥60%, needsReview≤30%
- [ ] **8.6** — Прегледай Review Queue: batch-одобри всички над 0.80 ако изглеждат коректни
- [ ] **8.7** — Прегледай remaining PENDING един по един — approve / reassign / reject
- [ ] **8.8** — Провери Unmapped tab — ръчно map-ни оставащите
- [ ] **8.9** — Провери Stats: unmapped = 0 (или приемливо ниско ниво)
- [ ] **8.10** — Тествай: отиди на категория страница → провери дали филтрите са дедупликирани

---

## ФАЗА 9 — Интеграция с Frontend Filter UI

> Засяга съществуващи компоненти. Тествай внимателно!

- [ ] **9.1** — `Category.jsx` — смени filter API endpoint след CATEGORY_MAPPING_PLAN Phase 8:
  - Сегашен: `GET /api/parameters/category/{distCategoryId}`
  - Нов: `GET /api/caretech-categories/{caretechId}/parameters`
  - Filter request: `caretechFilters: { caretechParamId: [optionIds] }` вместо `filters: { distParamId: [optionIds] }`

- [ ] **9.2** — `FilterPanel.jsx` (или еквивалентния компонент) — провери дали работи с новия facet key формат `"caretechParamId:caretechParamName"` (числото вляво е вече CareTech ID, не distributor ID)

- [ ] **9.3** — Тест: провери "Цвят" показва се 1 път (не 4) при смесена категория

---

## ФАЗА B-impl — Value Normalization (ОТЛОЖЕНА)

> Тази фаза е отложена. Имплементира се само след Phase A е стабилна в production.
> Решава: "Черен" vs "Black" vs "черный" в един филтър.

- [ ] **B-impl.1** — AI analysis за стойности (аналогично на параметри)
  - Input: всички `parameter_options` групирани по caretech_parameter
  - AI групира семантично еднакви стойности: ("Черен", "Black", "черный") → "Черен"
  - Confidence threshold: по-нисък (0.75 за auto-approve) защото стойностите са по-конкретни

- [ ] **B-impl.2** — Admin review UI за value mappings (нов Tab в ParameterMappingLayout)

- [ ] **B-impl.3** — `ProductSearchRepository` update — нормализира option names при facet building

---

## Бизнес правила

| Правило | Имплементация |
|---------|--------------|
| 1 дистрибуторски параметър → само 1 CareTech | UNIQUE constraint + service check |
| Много дистрибуторски → 1 CareTech | Позволено |
| AI auto-approve при score ≥ 0.85 | `ParameterMappingAiService.processSuggestion()` |
| PENDING review при 0.60 ≤ score < 0.85 | Admin преглежда от Review Queue |
| Manual only при score < 0.60 | AI suggestion се записва REJECTED → Unmapped tab |
| Не може да изтриеш CareTech param с mappings | `deleteParameter()` блокира |
| AI analyze — SUPER_ADMIN only | SecurityConfig: `hasRole("SUPER_ADMIN")` |
| Re-run AI анализ | Позволен — само за unmapped параметри (mapped се пропускат) |
| Phase B е незадължителна | Phase A решава основния проблем (дублирани групи) |

---

## AI Конфигурация

```properties
# application.properties
anthropic.api-key=${ANTHROPIC_API_KEY}
anthropic.model=claude-haiku-4-5-20251001
anthropic.max-tokens=4096
anthropic.batch-size=60
anthropic.auto-approve-threshold=0.85
anthropic.needs-review-threshold=0.60
```

**Защо claude-haiku-4-5:**
- ~350 параметъра / 60 на batch = ~6 API calls
- Haiku е достатъчно интелигентен за имена (RAM = Оперативна памет е очевидно)
- Значително по-евтин от Sonnet за batch анализ
- Ако accuracy е недостатъчна → смени на `claude-sonnet-4-6` в config без code change

**Rate limits:**
- Пауза от 500ms между batch-ове
- Retry с exponential backoff при 429 (rate limit) или 529 (overloaded)
- Ако batch се провали → log + продължи с следващия (не abort)

---

*Категорийна йерархия → `CATEGORY_MAPPING_PLAN.md`*
*Параметри за ръчно добавени продукти → ProductForm (обхванато в CATEGORY_MAPPING_PLAN Phase 9)*
