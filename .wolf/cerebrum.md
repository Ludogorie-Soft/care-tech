# Cerebrum

> OpenWolf's learning memory. Updated automatically as the AI learns from interactions.
> Do not edit manually unless correcting an error.
> Last updated: 2026-05-15

## User Preferences

- **EURO_RATE е винаги 1.95583** (официален фиксиран курс лев/евро). Никога не използвай 1.96 или друга стойност.
- **Цените се изписват винаги първо в Евро (€), след това в лева (лв.)** — напр. "199.99 €  /  391.12 лв.". Лева цената е обвита в отделен `<span>` или компонент за лесно премахване в бъдеще. Никога не показвай само лева.

- **Plan-first, always.** Before implementing ANY feature, write a detailed plan and get explicit user approval. No code until the plan is approved. No exceptions.
- **Role = Senior Software Developer.** Act as a senior dev on this project — professional, precise, no unnecessary abstractions, no over-engineering. Plan first, implement second.
- **Parameter mapping USES AI (reversed decision 2026-08-21).** Earlier decision "без AI, програмно" за параметри е ОТМЕНЕНО. Ползва се Claude API (haiku model) за batch analysis с confidence scores. ≥0.85 = auto-approve; 0.60-0.85 = needs human review; <0.60 = manual only.

- Never show raw backend error messages (stack traces, `message` fields, Spring error objects) on customer-facing UI. Always replace with a fixed Bulgarian user-friendly string.
- Admin-panel errors (ManufacturerModal, ParamsLayout etc.) may still show technical details — only customer-facing pages need sanitized messages.
- TBI modal button labels should be minimal Bulgarian — e.g. "Продължи", not "Продължи към TBI →".

<!-- How the user likes things done. Code style, tools, patterns, communication. -->

## Key Learnings

- **Project:** tech-store-api
- **Description:** A comprehensive Spring Boot REST API for a tech store, featuring complete product management, admin panel, and advanced e-commerce functionality.
- **Vali.bg category page URL format:** `https://www.vali.bg/bg/category/{id}` — the site is a Vue.js SPA; categories have no slug in the API, only numeric `id`. Homepage hrefs are not useful for scraping (only ~20 static links). All 260 categories are accessible via this pattern.
- **Vali.bg API categories endpoint:** `GET /api/v1/categories` returns a flat list of 260 objects with fields: `id`, `parent`, `show`, `order`, `name` (array of `{language_code, text}`), `description`. No slug field.
- **Vali.bg filter params extraction:** Filter parameter IDs appear as `name="opt[{id}][]"` in input elements on the catalog page. Use regex `name=["']opt\[(\d+)\]` to extract them.
- **Vali.bg categories with no filters:** 54 of 260 categories returned 0 filter params (these are parent/container categories without product listings).

## Key Learnings

- **TBI Leasing flow (Option B):** Order created immediately as `LEASING_PENDING` when customer submits leasing application. Auto-transitions to `PENDING` on `ContractSigned` webhook (sends confirmation email), or `LEASING_REJECTED` on rejection. Shipping address collected via `TbiCheckoutModal` before TBI iframe opens.
- **TBI brand color:** `#F07800` (orange), NOT red. TBI logo in header must use `color="#fff"` since header background is also `#F07800`.
- **TbiCheckoutModal:** Two-step modal — Step 1: Speedy/home address form; Step 2: TBI iframe (`registeredUrl`). Supports both single product (`product` prop) and cart (`cartItems` + `totalLeva` + `totalEuro` props).
- **Cart TBI integration:** `tbiCartItems` maps Redux cart items to `TbiCartItemRequestDto` shape: `priceEuro = finalPrice * 1.2` (with VAT), `priceLeva = finalPrice * 1.2 * EURO_RATE`.
- **leasingSlice registerError:** Stores `true` (flag) on failure, not backend payload — prevents accidental leakage of server error details to UI.
- **Dropdown re-show bug pattern:** In a controlled input where selecting from dropdown also calls `setState` on the input value — any `useEffect` watching that value will re-fire. Use a `programmaticQueryRef` flag to suppress the effect when the change was programmatic (not user typing).

## Do-Not-Repeat

- [2026-08-20] NEVER call `fetchCategoryParameters` (paramSlice) with an object `{ categoryId, language }` — the thunk accepts a plain number as first arg. Passing an object produces URL `parameters/category/[object Object]` → 404. `language=bg` is hardcoded inside the thunk. Correct: `dispatch(fetchCategoryParameters(categoryId))`.

- [2026-08-20] NEVER use `divide(..., 2, RoundingMode.HALF_UP)` when converting a percentage to a decimal ratio in `calculateFinalPrice()`. Scale=2 means 25.5% → 0.26 (26%), destroying precision. Use scale=6. DB column `discount` must also be `NUMERIC(10,6)` (not NUMERIC(8,2)) to store fractional percentages meaningfully.

- [2026-08-20] NEVER test nginx OG endpoints with plain `curl` — the `$bad_bot` map blocks `~*curl/` with `return 444` (empty response). Always use `curl -A "Mozilla/5.0 (compatible; Viber/10.0)"` or another non-blocked UA when testing through nginx on the production server.

- [2026-08-20] Spring JPA Auditing (`@CreatedBy`) overrides manually set `setCreatedBy()` at `@PrePersist`. Admin-created products should NEVER be found via `findByCreatedByOrderByCreatedAtDesc("ADMIN")` — use `findByPlatformIsNull()` instead (platform IS NULL = admin-created).

- [2026-08-20] `showToast(message, duration=2000)` — second arg is ms, NOT a toast type string. Passing `"error"` coerces to NaN → setTimeout(fn, NaN) → 0ms → invisible toast. Use `showToast(msg, 5000)` for errors, `showToast(msg)` for success.

- [2026-08-20] Viber in-app browser and link-preview fetcher share the same cookie jar. The `__og=1` cookie (set by OgMetaController for browsers) causes nginx to serve React SPA to Viber's fetcher → no OG preview. Fix: add `$is_social_crawler` map in nginx to force Viber/Telegram/WhatsApp always through OgMetaController, regardless of cookie.

- [2026-08-19] NEVER use `show` as a column name in SQL scripts for this project. The DB column is `show_flag` (BOOLEAN NOT NULL DEFAULT true) on BOTH `categories` and `products` tables. The Java entity field is named `show` but the `@Column(name = "show_flag")` annotation means SQL must use `show_flag`. Confirmed in `Category.java` and `Product.java`.

- [2026-08-19] NEVER use `ON CONFLICT (slug)` for the `categories` table — `slug` has only an INDEX (`idx_categories_slug`), NOT a UNIQUE constraint. Use `WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = '...')` for idempotent inserts.

- [2026-08-19] When INSERT-ing into `categories`, always include ALL NOT NULL fields: `name_bg, name_en, slug, parent_id, show_flag, sort_order, is_promo_active, platform, created_at, updated_at, created_by, last_modified_by`. Missing `is_promo_active` or `sort_order` will fail with NOT NULL constraint violation.

- [2026-07-24] NEVER do `collection.clear() + collection.addAll()` in Hibernate without `entityManager.flush()` between them when there is a unique constraint on the table. Hibernate batches the operations and sends INSERTs before DELETEs in the same flush, violating the constraint. Fix: inject `@PersistenceContext EntityManager` and call `entityManager.flush()` after `clear()`.

- [2026-07-20] NEVER leave admin query methods in `TbiLeasingService` (or any service with `open-in-view=false`) without `@Transactional(readOnly = true)` when the entity has LAZY associations. Without it, the Hibernate session closes after the repository call and `Page.map()` triggers `LazyInitializationException` when accessing `e.getOrder()`. Fix: add `@Transactional(readOnly = true)` to `getAllApplications`, `getApplicationsByStatus`, `getApplicationById`.

- [2026-05-20] NEVER use `restTemplate.getForObject(url, String.class)` за Asbis XML файлове — Spring декодира като ISO-8859-1 и разваля UTF-8 Кирилица. Винаги използвай `byte[].class` + `new String(bytes, StandardCharsets.UTF_8)`.
- [2026-05-22] NEVER replace a Hibernate-managed collection that has `orphanRemoval=true` (e.g. `product.setProductParameters(newSet)`). This silently marks the transaction rollback-only at next flush. Always mutate the existing collection: `existing.clear(); existing.addAll(newItems)`. If collection is null, first do `product.setX(new HashSet<>())`. Applies to ALL entities with orphanRemoval.
- [2026-05-20] NEVER разчитай на Lombok `@Builder.Default` за Jackson десериализация — стойностите важат само за builder pattern, не за `@NoArgsConstructor`. Добавяй null/zero guards в service validate методите.

<!-- Mistakes made and corrected. Each entry prevents the same mistake recurring. -->
<!-- Format: [YYYY-MM-DD] Description of what went wrong and what to do instead. -->

- [2026-08-06] NEVER use `ch.qos.logback.classic.filter.MarkerFilter` in logback XML — this class does NOT exist in the logback version used by this project (Spring Boot 3.x with logback-classic). Instead, create a custom `Filter<ILoggingEvent>` subclass that checks `event.getMarkerList()`. See `CriticalMarkerFilter.java`.

- [2026-08-06] ProductStatus convention: ONLY `AVAILABLE` products should be shown on the site. `LIMITED_QUANTITY`, `ON_ROUTE`, `ON_DEMAND` are NOT shown. All repository queries must use `status = AVAILABLE` (not `status <> NOT_AVAILABLE`). This applies to: ProductRepository, ProductSearchRepository, ParameterRepository, and any future queries.

- [2026-05-18] Do NOT put data-dependent SQL in Flyway migrations (db/migration/). Scripts that require pre-existing synced data (e.g. setting filters after Vali sync) belong in `scripts/`. Flyway runs on empty DB at startup — data isn't there yet.
- [2026-05-18] PostgreSQL `UPDATE ... FROM` cannot reference the update target table inside a JOIN in the FROM clause. Use comma-separated tables and move the join condition to WHERE: `FROM p, c WHERE cp.category_id = c.id` instead of `FROM p JOIN c ON c.id = cp.category_id`.

- [2026-05-29] NEVER render `registerError?.message` or raw Redux error payloads in customer-facing components. The `leasingSlice` now stores `true` as the error flag — always show a fixed string in the UI.
- [2026-05-29] When a controlled input's `useEffect` watches `value` and a programmatic setter (from a dropdown click handler) changes that value — the effect fires again and can re-open the dropdown. Always use a `useRef` skip flag when setting the input value programmatically.
- [2026-05-29] SVG `<text fill={TBI_RED}>` is invisible when placed on a `TBI_RED` background. Always pass an explicit `color` prop (e.g. `"#fff"`) when rendering TbiLogo inside the orange header.

- [2026-07-20] NEVER generate Pazaruvaj (or any) product URL with only the slug: `appUrl + "/product/" + slug`. The React Router route is `/product/:productSlug/:productId` — always append the ID: `appUrl + "/product/" + slug + "/" + id`. Missing ID → 404 on all product links → feed rejection.

- [2026-07-06] NEVER write native SQL queries with `p.primary_image_url` — the actual column name in the `products` table is `p.image_url`. The `findForPazaruvajFeedByCategory` query had it right; the other two Pazaruvaj queries were wrong. Always verify column names against the entity `@Column` annotation.
- [2026-07-06] NEVER leave `spring.jpa.open-in-view=true` (the default) on a production app that has long-running I/O in controllers (e.g. image proxy). With open-in-view, a DB connection is held open for the entire HTTP request — a 138-second image proxy request blocks all 10 HikariCP connections. Always set `spring.jpa.open-in-view: false`.
- [2026-07-06] ImageProxyController: NEVER use `connectTimeout=15000` + `readTimeout=30000` + 3 retries for external images — worst case is 138 seconds per image and exhausts the thread pool. Use 5s connect + 10s read + max 1 retry. Add a `Semaphore(5)` to cap concurrent outbound proxy requests.
- [2026-07-06] axios baseURL в care-tech-ui е `${REACT_APP_API_URL}/api/` (trailing slash). Никога не добавяй `/api/` prefix в заявките — `api.get("/api/categories")` → двоен prefix. Правилно: `api.get("categories")`.

## Do-Not-Repeat (Tests)

- [2026-05-29] NEVER modify `application.yml` to fix test failures — create `src/test/resources/application.properties` (for all tests) or `application-test.properties` (for @ActiveProfiles("test")) instead.
- [2026-05-29] `@SpringBootTest` context-load test fails locally because `${POSTGRES_DB}` (and other env vars) are unresolved. Fix: @ActiveProfiles("test") + application-test.properties with H2 datasource + spring.flyway.enabled=false.

## Do-Not-Repeat (Security)

- [2026-07-07] NEVER add real credential values as `${ENV_VAR:real_value}` defaults in application.yml — they end up in Git history. All secrets (MAIL_PASSWORD, ASBIS_PASSWORD, TBI_ENCRYPTION_KEY, SPEEDY_PASSWORD) must use `${ENV_VAR}` without fallback.
- [2026-07-07] NEVER remove `addCorsMappings` override from WebConfig and leave `allowedOriginPatterns("*") + allowCredentials(true)` — this is a CORS misconfiguration. SecurityConfig's CorsConfigurationSource is the authoritative CORS config; WebConfig override is redundant and dangerous.
- [2026-07-07] IDOR ownership check in OrderController must use userId (not email): `order.getUserId().equals(currentUser.getId())`. Email is mutable and guest-order fallback only. Pattern: check userId if non-null, fallback to email for guest orders.

- [2026-06-04] NEVER leave `/api/cart/**` as `permitAll()` in SecurityConfig — this was an IDOR: any unauthenticated user could read/write any user's cart. Always use `authenticated()` at URL level + `SecurityHelper.hasAccessToResource(userId)` in the controller.
- [2026-06-04] When adding new entities that store JSON in PostgreSQL, use `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "jsonb"` on the field. Do NOT use plain `@Column` — Hibernate won't know how to serialize/deserialize the Java object.

- [2026-05-29] NEVER use `hasRole('ADMIN')` alone on controllers that should be accessible to SUPER_ADMIN. `getAuthorities()` returns exactly one role string (`ROLE_ADMIN` or `ROLE_SUPER_ADMIN`) — `hasRole('ADMIN')` will silently block SUPER_ADMIN. Always use `hasAnyRole('ADMIN', 'SUPER_ADMIN')`.
- [2026-05-29] NEVER add `@CrossOrigin(origins = "*")` to individual controllers — it overrides the global CORS config and opens all origins for those endpoints. Use the global config only.
- [2026-05-29] When adding an IDOR fix to a TBI/order endpoint, check whether the frontend actually calls that exact path. `fetchLeasingById` in leasingSlice calls `admin/leasing/{id}` (AdminController), NOT `tbi/application/{id}` (TbiLeasingController) — these are two different endpoints.
- [2026-05-29] Defense in depth: every sensitive endpoint should have BOTH URL-level (`SecurityConfig`) AND method-level (`@PreAuthorize`) guards. URL rules can be accidentally removed or reordered.

- [2026-05-29] **Test infrastructure:** `src/test/resources/application.properties` overrides `spring.profiles.active` and `logging.file.name` for ALL tests (needed because @WebMvcTest also loads application.yml). `src/test/resources/application-test.properties` provides full H2 + disabled Flyway config for @SpringBootTest. `TechStoreApiApplicationTests` uses `@ActiveProfiles("test")`. H2 added as test-scoped dependency in pom.xml.
- [2026-05-29] **UnnecessaryStubbing pattern:** Mockito strict mode throws if a stubbed method is never called. When an entity has `order = null`, `transitionOrderStatus` returns early — `orderRepository.findById()` is never invoked. Don't stub it in those tests.
- [2026-05-29] **@WebMvcTest + @Import(SecurityConfig):** Needs `@MockBean` for `UserRepository` and `JwtUtil` (SecurityConfig constructor deps). Also needs `@MockBean SecurityHelper` if the controller uses it. Test properties must supply `spring.cors.allowedOrigins`.

## Key Learnings

- **PersonalOffer entity** uses `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "jsonb"` to store `List<OfferItem>` as PostgreSQL JSONB. The `OfferItem` is a `@Data` inner static class.
- **offersSlice.js** combines both admin-side (fetchUserCart, fetchUserOffers, sendPersonalOffer) and user-side (fetchMyOffers, markOfferRead, updateOfferStatus) actions in one slice. State keys: `myOffers`, `unreadCount`, `userCart`, `userOffers`, `sendStatus`.
- **CartController security fix (2026-06-04):** `/api/cart/**` was `permitAll()` in SecurityConfig — any unauthenticated user could read/write any user's cart. Fixed to `authenticated()` + class-level `@PreAuthorize("isAuthenticated()")` + `requireAccess(userId)` ownership check using `SecurityHelper.hasAccessToResource()`.

## Key Learnings

- **Personal offer → order pricing:** `offerPrice` is VAT-inclusive (what the customer pays). `OrderService.unitPrice` is always pre-VAT. So when converting: `unitPrice = offerPrice / 1.20`. `originalPrice` from `SendOfferModal` = `product.finalPrice` (pre-VAT), so original VAT-inclusive = `originalPrice × 1.20`. Discount% = `(1 - offerPrice / (originalPrice × 1.20)) × 100`. Without the /1.20 division, the customer would be charged 20% extra on top of the agreed price.
- **Regular orders are NOT affected** by the customPriceEuro changes — they send only productId+quantity, so `customPriceEuro == null` and `OrderService` uses `product.getFinalPrice()` as before.
- **OrderItem.discountAmount** stores a PERCENTAGE (not an absolute amount), consistent with `product.getDiscount()`. Frontend uses: `originalPrice = lineTotalWithTax / (1 - discountAmount / 100)`.
- **Cart cleanup on offer conversion:** After `orderService.createOrder()`, `cartItemRepository.deleteByUserIdAndProductIdIn(userId, productIds)` removes only the converted products from the cart. `skipCartClear = true` prevents OrderService from clearing the whole cart.
- **Speedy autocomplete (non-Formik):** Use `useState` + `useRef` for city/office selectors. Dispatch `fetchCities(query)` on city input change; on city select dispatch `fetchOffies(cityId)` and focus the office input. Store both the ID and the display name. Reference: `AcceptOfferModal.jsx`.

## Key Learnings

- **AdImageSectionTwo.jsx** (`care-tech-ui/src/components/home/AdImageSectionTwo.jsx`) holds a hardcoded `slides` array with `{ src, brandId }` pairs for the homepage ad banners. The `brandId` must match the live DB manufacturer ID. Initial values had `f-1.jpg → 201` (Lian-Li) and `f-3.jpg → 194` (LANDE) — should be `205` (Logitech) and `198` (Lenovo). Always verify these IDs against `/api/manufacturers` when changing ad images.
- **Brand navigation bug pattern:** When a homepage ad image (AdImageSectionTwo) or partner logo (OurPartnersSection) navigates to the wrong brand page, check the hardcoded `brandId` in the `slides`/`partners` array first. These are static JS files, not derived from the DB.
- **Duplicate `language` param bug:** `fetchProductsByBrand` thunk in productSlice.js previously had `?language=bg` hardcoded in the URL AND passed `language` in the `params` object. Axios appended both → Spring's `validateLanguage()` received `bg,bg` → 400 error. Fix: remove the hardcoded query string from the URL, pass only via `params`.

## Do-Not-Repeat

- [2026-06-21] `FileUploadService.ALLOWED_SUBFOLDERS` е whitelist — при нови upload endpoints ВИНАГИ добавяй новия subfolder в него. Субпапки с `/` (напр. `"blog/covers"`) не минават `SAFE_SUBFOLDER_PATTERN = "^[a-zA-Z0-9_-]+$"`. Ползвай flat имена: `"blog-covers"`, `"blog-images"`.
- [2026-06-22] Blog/image uploads ТРЯБВА да минават през `S3Service.uploadProductImage()`, НЕ `FileUploadService.uploadFile()`. FileUploadService записва на локален диск и връща `/blog-covers/...` (relative path) — браузърът го резолвира към frontend origin-а, не към бекенда. Само S3 връща пълен `https://` URL който работи навсякъде. ProductService вече ползва S3 (`uploadImageSafely`), следвай същия pattern.
- [2026-06-21] `dangerouslySetInnerHTML` с admin-въведен HTML съдържание ВИНАГИ изисква DOMPurify sanitization — дори admin-only съдържание, защото компрометиран акаунт е реален вектор.
- [2026-06-21] Redux slice с shared `posts` array за публичен и admin изглед създава state pollution. Ползвай отделни keys: `publicPosts`/`publicStatus` vs `adminPosts`/`adminStatus`.

## Key Learnings

- **Category alias pattern (2026-06-23):** `alias_of_id` on `categories` table allows virtual categories that proxy products from another category. Alias resolution must be applied in EVERY code path that queries products/parameters by category: `ProductService`, `ProductSearchService`, AND `ParameterService`. Raw SQL repositories (`ProductSearchRepository`) do NOT auto-resolve aliases — only the service layer does.
- **ProductSearchService alias coverage:** `searchProducts` resolves `request.categories` (List<String>) via `resolveAliasCategories()`; `getAvailableParametersWithCountsForCategory` and `getFilteredFacets` resolve via `resolveAliasId()` before hitting the repository.
- **ParameterService alias coverage:** `findByCategory` captures the category entity and uses `aliasOf.getId()` for both `findByCategoryIdOrderByOrderAsc` and `getCategoryParameterFilters` calls.
- **Hidden-original guard:** `ProductService.resolveAliasId` checks `target.getShow() == false` — if the original category is hidden, return the alias ID (which has no own products) so the result is correctly empty.

## Key Learnings

- **Production server crash (2026-07-06):** Root cause — PostgreSQL container OOM-killed (Exit 255) by kernel during SYN flood on port 80. Recovery: `docker start techstore-postgres` (crash recovery OK, no data loss). Port 5432 was publicly exposed — external bots were scanning it (`Role "postgres" does not exist` errors). Close port 5432 from public in AWS Security Group.
- **HikariCP exhaustion pattern:** ImageProxyController (no Semaphore) + open-in-view=true → concurrent image requests hold all DB connections → `Connection is not available, request timed out after 30000ms`. Fix: open-in-view=false + Semaphore(5) + reduced timeouts.
- **Nginx hardening (2026-07-06):** Added rate limiting (`limit_req_zone` 20r/s API, 50r/s general), connection limit (`limit_conn 30`), bad bot blocking (empty UA, sqlmap, nikto, masscan, zgrab), scan path blocking (.env, .php, wp-admin), security headers. Config saved as `nginx.prod.conf` in project root.
- **Pazaruvaj generate endpoint (2026-07-06):** Added `format=CSV` parameter + CSV builder in PazaruvajFeedService. Live feed keeps delivery info (required by Heureka format); on-demand generate uses `includeDelivery=false`. Frontend has XML/CSV format selector in generator section.

- **Pazaruvaj.com интеграция (2026-07-03, коригирано 2026-07-07):** Pazaruvaj е собственост на Heureka Group, но НЕ използва Heureka XML формат. Ползва собствен формат: root `<Products>`, item `<Product>`, полета `Identifier`, `Name`, `ProductUrl`, `ImageUrl`, `Price`, `Category`, `Manufacturer`, `ProductNumber`, `EanCode`, `Description`, `DeliveryTime`, `DeliveryCost`. Разделител на категории: ` > `. Доставка: плосък `<DeliveryCost>6.00 EUR</DeliveryCost>`, не nested блокове. Няма REST API — merchant-ът хоства XML URL, pazaruvaj го дърпа ежедневно след 17:00.
- **Pazaruvaj PRICE_VAT:** Цената е в **EUR с ДДС** = `finalPrice × 1.20`. Без конвертиране към BGN (България е в еврозоната). Полето `CATEGORYTEXT` изисква кирилица.
- **PazaruvajFeedService архитектура:** Feed се кешира в `AtomicReference<String>`, обновява се на 30 сек след старт + на всеки 2 часа. `PazaruvajFeedConfig` (тип ALL/CATEGORY/PRODUCTS) се пази в паметта — нулира се при рестарт. На-demand генерирането (`/generate`) е винаги fresh, без кеш.
- **Native SQL feed query:** Feed query-тата ползват native SQL с `PazaruvajProductProjection` интерфейс за да избегнат N+1 и EAGER loading на `productParameters`. Category query използва recursive CTE (`WITH RECURSIVE cat_tree`) за да включи всички подкатегории.
- **Category tree в React:** `buildTree()` + `flattenTree()` изграждат плосък масив с `depth` поле от flat API response. Tree view: indentation по `depth * 18px` + `└` символ. При активно търсене — превключва към flat view с родителя вдясно.

## Decision Log

<!-- Significant technical decisions with rationale. Why X was chosen over Y. -->

- [2026-05-18] V13 (set Vali is_filter from scraped data) moved to `scripts/` folder. Reason: on a fresh server the DB is empty at startup — Flyway would run V13 before any Vali sync data exists, so it would match 0 rows. The script must be run manually after `POST /api/sync/vali/parameters`.
- [2026-05-29] TBI leasing uses Option B architecture: create `LEASING_PENDING` order at application time (not after TBI approval). This avoids the problem of admin not knowing the customer's shipping address after approval — address is collected before the TBI iframe opens.
- [2026-06-23] Category alias implemented via `alias_of_id` column (not SQL duplication, not many-to-many). Resolution is done in the service layer — transparent to the frontend and repository layer. SQL script 23 creates aliases for "Геймърска периферия" subcategories under "Компютърна периферия" with slug prefix `kp-`.

## Key Learnings (2026-07-21)

- **Shareable filter URL (Category.jsx):** Filter state вече се sync-ва към URL params с `setSearchParams(params, { replace: true })` при всяка промяна. Четенето от URL вече съществуваше (`parseFiltersFromURL`). Формат: `?param_5=101,102&manufacturers=12&minPrice=500`.
- **Share бутон — Web Share API pattern:** `navigator.share` се поддържа на Mac Safari и мобилни. На Windows десктоп не се поддържа → fallback към `clipboard.writeText`. Показваме dropdown само когато `navigator.share` е налично; на Windows директно копираме.
- **Share бутон в ImageDisplaying:** Позициониран `absolute bottom-5 left-5`, симетрично с Любими (`bottom-5 right-5`). Получава `url` prop от ProductPage. Dropdown с: Сподели (native), Facebook, WhatsApp, Viber, Копирай линк.
- **OG meta за React SPA:** React не изпълнява JS за социални медии ботове → OG тагове не се виждат. Решение: Nginx детектира `$is_social_crawler` (User-Agent map) и пренасочва само ботовете към `OgMetaController` в Spring Boot. Контролерът връща минимален HTML с OG тагове + `<meta http-equiv="refresh">` за браузъри.
- **nginx $is_social_crawler bug:** Старият конфиг ползваше `$do_og = 1` (винаги true) → пренасочваше ВСИЧКИ потребители към `/api/og/`. Правилно е: `if ($is_social_crawler) { rewrite ... }` без допълнителна променлива.
- **OgMetaController:** `app.url` от `application.yml` (стойност: `https://www.caretech.bg`). `buildImageUrl()` — ако stored URL е абсолютен го ползва директно, иначе proxy URL. HTML escape задължителен за OG стойности.

## Do-Not-Repeat

- [2026-07-21] NEVER използвай `$do_og = 1` (или подобна винаги-true променлива) в nginx за OG routing — пренасочва всички потребители. Винаги проверявай `$is_social_crawler` директно в `if` блока.

## Key Learnings (2026-08-05)

- **Admin ProductForm category loading:** Uses `fetchAllCategoriesAdmin` thunk (`GET /api/admin/categories/all`) to load ALL categories including hidden ones. Public `fetchCategories` returns only `show=true`. The endpoint is in `AdminController` under `/api/admin/**` (URL-level security from SecurityConfig).
- **Auto-activate category on product assign:** `ProductService.resolveAndActivateCategory(categoryId)` — checks `category.getShow() == false` and sets `show=true` + saves. Called in both `updateProductFieldsFromRest` (create) and `updateProductFieldsByUpdate` (update). Logs the activation with category name.
- **OG meta за социални медии:** `OgMetaController` при `/api/og/product/{id}` — задължителни тагове: `og:image`, `og:image:secure_url` (HTTPS копие), `og:image:alt`, `twitter:image:alt`. Nginx rewrite трябва да има `/?$` в края за optional trailing slash. Тестване: `curl -A "facebookexternalhit/1.1" https://www.caretech.bg/product/slug/123`.
- **nginx rewrite trailing slash:** Без `/?$` в края на regex, URL-и като `/product/slug/123/` (с trailing slash) не matching-ват и ботовете получават React SPA вместо OG HTML.
- **Facebook crawler следва meta-refresh:** `facebookexternalhit` следва `<meta http-equiv="refresh">` → попада на React SPA → чете generic `og:url = https://www.caretech.bg/` от index.html → показва лого на сайта вместо продуктова снимка. Viber НЕ следва meta-refresh. Решение: JS redirect вместо meta-refresh.
- **nginx sub_filter за OG fix без Spring Boot rebuild:** Когато Spring Boot не може да се rebuild-не, nginx може да трансформира response body-то чрез `sub_filter`. Изисква `proxy_set_header Accept-Encoding ""` (disable gzip). Три замени: (1) `'0; url='` → `''`; (2) `'http-equiv="refresh"'` → `'name="js-redir"'`; (3) `</head>` → `<script>...window.location.replace...</script></head>`. Добавя се като отделен `location /api/og` ПРЕДИ общия `location /api`.
- **OG bypass dual-mode map:** `map "$cookie___og:$arg_og_bypass" $og_bypass` — приема `cookie __og=1` (нов Spring Boot) ИЛИ `?og_bypass=1` query param (стар Spring Boot). Backward compatible при deploy.
- **Facebook Sharing Debugger — Response Code 206:** Когато debugger-ът показва code 206 и следва `og:url Meta Tag` към homepage — означава че ботът е прочел React SPA (огледан по meta-refresh) и е взел нейния `og:url`. Fix: премахни meta-refresh от OG страниците.
- **Docker deployment mystery:** Потребителят build-ва и push-ва Docker image 3 пъти, но EC2 продължава да изпълнява стария JAR. Возможни причини: Docker Hub cache, container не се рестартира правилно, или грешен image tag. Диагностика: `docker inspect techstore-api --format='{{.Created}}'` — сравни с времето на последния push.

## Key Learnings (2026-08-03 — Vali sync review)

- **Vali API `/full` endpoint is the ONLY source of complete product data.** Pagination params on the by-category endpoint are silently ignored by Vali. The paginated global `/products?page=X` endpoint returns only basic fields (no descriptions, flags, parameters).
- **Vali `largeResponseWebClient`** already has a 50MB buffer + 5-min timeout. Largest observed category response is ~2.9MB — no need to switch endpoints for size.
- **ValiSyncService flag sync** was wired in DTOs and entity but was never called from `updateProductFieldsFromExternal`. Always verify sync pipelines end-to-end: DTO → mapping method → entity → save.
- **ProductStatus search filter:** Search repository was using `p.status IN ('AVAILABLE', 'LIMITED_QUANTITY')` — this incorrectly excluded ON_ROUTE (3) and ON_DEMAND (4). Correct filter is `p.status <> 'NOT_AVAILABLE'` to match browse behaviour.
- **Vali `show` flag logic:** Product should only be shown if `extProduct.show == true AND finalPrice > 0 AND status != NOT_AVAILABLE`. All three conditions must be checked together when setting `product.setShow()`.
- **Slack webhook disable:** Comment out `SLACK_WEBHOOK_URL` in `.env` with `#` — logback reads it via `${SLACK_WEBHOOK_URL}` and silently skips the appender when null/empty.

## Do-Not-Repeat

- [2026-08-05] NEVER include the "Производител" pseudo-parameter (added by `fetchCategoryParameters` in paramSlice) in product create/update payload. The `make` object has no `id` field → `parameterId` becomes `NaN` → JSON serializes as `null` → `@NotNull` backend validation fails. Fix: filter `state.param.options` by `p.id != null` in ProductForm, and add `!isNaN(id) && id > 0` guard in `buildParametersFromSelection`.

- [2026-08-05] NEVER use `GET /api/categories` (public endpoint) in admin product forms — it returns only `show=true` categories via `findByShowTrue()`. Admin product forms need ALL categories. Use `GET /api/admin/categories/all` → `categoryService.getAllCategoriesForAdmin()` → `findAll()`.

- [2026-08-05] NEVER use `<meta http-equiv="refresh">` in OG meta pages served to social media bots. Facebook's crawler (`facebookexternalhit`) FOLLOWS meta-refresh redirects, lands on React SPA, reads generic homepage OG tags, and shows wrong preview. Fix: use `<script>window.location.replace('...');</script>` — social crawlers do not execute JS, browsers do. If Spring Boot can't be rebuilt, use nginx `sub_filter` to replace meta-refresh with JS redirect at the proxy layer.

- [2026-08-05] `nginx.prod.conf` in project root was emptied (0 bytes) during a session — content was lost. Always verify file size after saving. The deployed EC2 nginx config was unaffected (already deployed), but the local file needed to be recreated from memory.

- [2026-08-03] NEVER look up existing products only by `externalId` in Vali sync without a referenceNumber fallback. Products created before `externalId` was populated (NULL in DB) won't be found → INSERT fails on `products_reference_number_key` unique constraint. Fix: after externalId lookup, batch-query unmatched ref numbers via `findByReferenceNumberIn()`, update those products, and backfill `externalId` for future syncs.
- [2026-08-03] `ProductStatus.fromCode(int)` must accept `Integer` (nullable) and return `NOT_AVAILABLE` for null/unknown codes, not throw. Vali API may omit the status field.
- [2026-08-03] When cancelling an order, always auto-set `paymentStatus = CANCELLED` unless it is already `PAID` or `REFUNDED`. Apply this in all three cancel paths: `updateOrderStatus`, `updateOrder`, `cancelOrder`.

- [2026-08-06] Asbis category sync (`AsbisSyncService`) creates new categories with `show=true` by default for root-level entries (no parent_id). Running it accidentally populates hundreds of unwanted categories visible on the frontend. Fix: `UPDATE categories SET show = false WHERE platform = 'ASBIS' AND DATE(created_at) = '<date>'`. To prevent: add a guard/flag in AsbisSyncService to skip category creation unless explicitly enabled.

- [2026-08-06] NEVER use `onClick={() => navigate(...)}` alone on clickable cards/containers — this breaks middle-click, Ctrl+Click, AND right-click "Open in new tab". Always use `<Link to="...">` (React Router) as the wrapper element — it renders as `<a href>` which gives full browser support natively. Nested action `<div>` buttons still need `e.stopPropagation()` to prevent triggering the Link. For Framer Motion items use `motion(Link)` → `const MotionLink = motion(Link)` declared after all imports.

- [2026-08-06] NEVER place `const X = ...` declarations between `import` statements — ESLint `import/first` rule treats any code before an import as "import in body of module" and fails the build. Always declare module-level constants AFTER all imports.

- [2026-08-06] NEVER add `onClick` to a child element inside a `<label>` that wraps a hidden `<input type="checkbox">`. Clicking the child fires the onClick once, then the event bubbles to `<label>` which toggles the checkbox again — double-toggle = no visible change. The `<label>` wrapper already handles all clicks on its children. Pattern in `CustomCheckbox.jsx`: remove `onClick` from the icon `<div>`, let `<label>` manage the toggle.

- [2026-08-11] **CORRECTION — ProductStatus filter:** Earlier entry (2026-08-03) said the correct filter is `p.status <> 'NOT_AVAILABLE'`. THIS IS WRONG. User explicitly confirmed: **ONLY `status = AVAILABLE` (status Наличен) should be shown anywhere on the site.** `LIMITED_QUANTITY`, `ON_ROUTE`, `ON_DEMAND` are NOT shown. Always use `status = AVAILABLE`, never `<> NOT_AVAILABLE` or `IN ('AVAILABLE', 'LIMITED_QUANTITY')`.

- [2026-08-11] **Product availability — full checklist of places that MUST filter `status = AVAILABLE`:**
  - `ProductRepository.findActiveByCategoryExcludingNotAvailable` ✅ (has `AND p.status = AVAILABLE`)
  - `ProductRepository.findActiveByManufacturerExcludingNotAvailable` ✅
  - `ProductRepository.findRelatedProducts` ✅
  - `ProductRepository.findSitemapEntries` ✅ (fixed 2026-08-11)
  - `ProductSearchRepository.searchProducts` ✅ (`WHERE p.status = 'AVAILABLE'`)
  - `ProductSearchRepository.searchProductsFuzzy` ✅
  - `ProductSearchRepository.getAvailableParametersWithCountsForCategory` ✅
  - `ProductSearchRepository.getFilteredFacets` ✅
  - `ProductService.getProductById` ✅ (runtime check added 2026-08-11 — throws 404 if not AVAILABLE/active/show)
  - `UserFavoriteRepository.findByUserIdWithProducts` ✅ (fixed 2026-08-11)
  - `CartService.addToCart` ✅ (throws BusinessLogicException if not AVAILABLE — fixed 2026-08-11)
  - `CartService.getCartSummary` ✅ (filters unavailable items from stream — fixed 2026-08-11)
  - **Still NOT filtered (admin-only or intentional):** `ProductRepository.findByActiveTrue` (used in `getAllProducts` — admin endpoint)

- [2026-08-11] **Price sorting in Category.jsx:** `fetchProducts` (no-filter path) previously used `GET /api/products/category/{id}` → JPA `Sort.by("finalPrice")` with `@Query` JPQL — sort was unreliable. Fixed: now uses `GET /api/products/categories/{id}/products` → `ProductSearchController.searchByCategory` → `ProductSearchRepository` → raw SQL `ORDER BY p.final_price`. Both filter-active and no-filter paths now use the same SQL sort infrastructure. `handleClearAll` no longer explicitly dispatches `fetchProducts` — the `isFilterActive` useEffect handles the refetch.

- [2026-08-11] **`fetchProducts` response format:** Now maps `ProductSearchResponse` (same as `filterProducts`) — the content array contains objects with `finalPrice`, `discount`, `primaryImageUrl`, `nameEn`/`nameBg`, `category`, `manufacturer`, `specifications` mapped from `ProductSearchResult`. The Redux state shape is identical for both paths.

## Key Learnings (2026-08-19/20 — Tekra sync + Nav/Footer)

- **Tekra API product depth:** Tekra API връща продукти САМО на level-2 (sub-category slug, напр. `hd-analogovi-sistemi`). Level-3 (leaf) категории имат `count: 0` в categories API → `getProductsRaw()` за тях връща 0 продукта. Решение: само level-2 категории имат `tekra_slug`; level-3 имат само `category_path` за routing чрез `findMostSpecificCategory()`.
- **Tekra category_path routing:** `TekraSyncService.findMostSpecificCategory()` изгражда `expectedPath = category_1/category_2/category_3` от XML и търси точно съвпадение в `categoriesByCategoryPath` map. Leaf категориите (level-3) трябва да имат коректен `category_path` дори без `tekra_slug`.
- **Tekra rate limiting (429):** Tekra lock-ва за по-дълго от 10s. `retryDelayMs` е 60_000 (60s initial, удвоява се, 3 опита). При 3 последователни 429 — изчакай минимум 10-15 мин преди следващ sync.
- **NavDropDown section header pattern:** Ако `si.hasChildren == true`, секцията се рендерира като `<Link to="/category/list/${slug}/${id}">` (кликаем). Ако `false` — като `<Link to="/category/${slug}/${id}">`. НЕ добавяй selfChild hack за да правиш parent категория кликаема — вместо това направи хедъра директно Link.
- **Footer dynamic pattern:** `Footer.jsx` чете от Redux (`state.categories.categories`); App.js вече е заредил категориите при старт. Филтрира по `FOOTER_CATEGORY_IDS = new Set([...])` за контрол кои категории се показват. hasChildren check определя `/category/list/` vs `/category/` prefix.
- **Category routing convention (frontend):** Категория с деца → `/category/list/${slug}/${id}` (CategoryList). Leaf категория → `/category/${slug}/${id}` (Category + product grid). Линкове в NavDropDown и Footer ТРЯБВА да спазват това — директен линк към `/category/` за parent категория причинява spinner + redirect hop.

## Key Learnings (2026-08-27 — Filter Consolidation Execution)

- **DBeaver autocommit е критично за multi-DO-block скриптове.** Без autocommit ON всички DO блокове са в 1 транзакция — при грешка в един, всичко след него пада с "current transaction is aborted". Включва се от toolbar бутона (не от Connection settings — там е greyed out без да се отметне datasource checkbox-а).
- **Pre-flight dedup pattern:** Преди multi-group consolidation скрипт да се re-run след грешка, трябва да се изпълни DELETE на конфликтиращи product_parameters: `DELETE FROM product_parameters pp USING parameter_options nc_opt, parameter_options c_opt WHERE pp.parameter_option_id = nc_opt.id AND nc_opt.parameter_id != c_opt.parameter_id AND LOWER(TRIM(nc_opt.name_bg)) = LOWER(TRIM(c_opt.name_bg)) AND EXISTS (SELECT 1 FROM product_parameters pp2 WHERE pp2.product_id = pp.product_id AND pp2.parameter_option_id = c_opt.id AND pp2.parameter_id = c_opt.parameter_id)`.
- **Cross-script canonical invalidation:** Script 43 може да ползва canonical IDs, които Script 40 е изтрил като non-canonical. Преди изпълнение на 43, верифицирай всички canonicals: `SELECT id FROM parameters WHERE id IN (...)`. Ако липсва → намери кой canonical го е погълнал в Script 40 (grep в DO блока) и го замени.
- **ASBIS logistics параметри:** IDs 4027, 4045, 4040, 4031, 4061, 4041, 4049, 4043, 4056 и ~50 още са логистични/опаковъчни полета от ASBIS — НЕ трябва да са is_filter=true. Деактивирани с Script 45.
- **Финален резултат:** Цвят = 8599 продукта в 155 категории (merged от 91 VALI + TEKRA + ASBIS + "Външен цвят" sources). Всички активни филтри имат ≥2 опции.
- **410 Gone за OG endpoints:** Изтрити продукти → OGPreviewController трябва да върне 410 Gone (не 302) — 302 причинява безкраен crawler loop (~10 req/sec). Facebook/WhatsApp спират crawling при 410.
- **AdminService lazy init:** Методи, които викат `convertToResponseDTO()` (достъпва lazy колекции като `additionalImages`), трябва да имат `@Transactional`. Засяга: `createPromo`, `createPromoByManufacturer`, `createPromoByCategory`.

## Key Learnings (2026-08-26 — Filter Parameter Consolidation)

- **VALI per-category parameter architecture:** VALI sync създава нов `parameters` ред за ВСЯКА категория. "Цвят" = 91 отделни parameter rows, "Размери" = 53, "Интерфейс" = 27. Старият план (caretech_parameters + distributor_parameter_mapping) не решаваше options дедупликацията (Phase B = отложена).
- **Нов подход: директна SQL консолидация** — скриптове 40-44 физически сливат дублиращите rows. Canonical = param с най-много category linkages (тie-breaker: max products → min id). Options се сливат по `LOWER(TRIM(name_bg))`. Без code промени в backend/frontend.
- **Canonical selection pitfall:** Canonical по max products може да избере принтер/тонер категория (id=6032 "Цвят" има "Yellow", "Magenta" опции). ОК защото filter query показва само опции с продукти в текущата категория (HAVING COUNT > 0).
- **Ред на изпълнение:** Script 40 (VALI) → Script 41 (ASBIS) → Script 42 (is_filter cleanup) → Script 43 (cross-platform top-20) → Script 44 (post-cleanup). Всеки в отделна BEGIN/COMMIT транзакция.
- **is_filter junk:** MOST params "Гаранция" (3265 products!), "URL на производителя" (2938), "Weight", "Manufacturer" са is_filter=true но НЕ трябва да са филтри → Script 42 ги деактивира.
- **cross-platform дублирания след 40+41:** 52 кроссплатформени групи с еднакви имена. Script 43 мержва топ 20 (Интерфейс, Цвят, Тип продукт, ...). OPTIONS не се сливат при cross-platform (различни езици).

## Key Learnings (2026-08-26 — Product soft delete)

- **Product delete = soft delete.** `DELETE /api/products/{id}` НЕ трие физически. Сет-ва `deleted=true, active=false, show=false, status=NOT_AVAILABLE`. `order_items` FK е `ON DELETE RESTRICT` → hard delete е невъзможен за продукти в поръчки. `deleted=true` е авторитетният флаг — sync-ът не го reset-ва.
- **`findByPlatformIsNullAndDeletedFalse`** и **`findByMarkupPercentageGreaterThanAndDeletedFalse`** — единствените admin repo методи без active/show/status filter, затова изискват explicit `deleted=false`. Всички останали query-та филтрират `active=true AND show=true AND status=AVAILABLE`.
- **Flyway V35** добавя `deleted BOOLEAN NOT NULL DEFAULT FALSE` към `products` таблицата.

## Do-Not-Repeat

- [2026-08-27] NEVER return 302 redirect from OGPreviewController when product/category is not found — social media crawlers (Facebook, WhatsApp, Viber) loop ~10 req/sec on 302. Always return 410 Gone for deleted/missing resources.
- [2026-08-27] NEVER call `convertToResponseDTO()` from AdminService methods without `@Transactional` — the method accesses `product.getAdditionalImages()` (lazy collection) which fails after the Hibernate session closes. Add `@Transactional` to `createPromo`, `createPromoByManufacturer`, `createPromoByCategory`.
- [2026-08-27] When running multi-group consolidation SQL scripts in DBeaver, ALWAYS enable autocommit first. Each DO block must be its own transaction — without autocommit, a single failure aborts the entire session and all subsequent blocks fail with "current transaction is aborted".

- [2026-08-26] NEVER hard-delete products. `order_items.product_id` е `ON DELETE RESTRICT` — FK violation → 409 Conflict. Винаги ползвай soft delete: `product.setDeleted(true)` + `active=false` + `show=false` + `status=NOT_AVAILABLE` + `productRepository.save()`.

- [2026-08-19] NEVER add hardcoded `si.id === N` special cases in NavDropDown to make a parent category linkable. This creates a visual duplicate (category appears as its own subcategory) and causes a spinner/redirect loop. Instead: render the section header itself as a `<Link to="/category/list/...">`.
- [2026-08-19] NEVER link to `/category/${slug}/${id}` for a category that has children. Category.jsx detects hasChildren, calls navigate() to /category/list/..., causing an unnecessary spinner + redirect. Always link directly to `/category/list/${slug}/${id}` for parent categories.
