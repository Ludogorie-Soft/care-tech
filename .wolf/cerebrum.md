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

## Key Learnings (Търсачка — измерено 2026-09-18)

- **Прод база достъп:** PostgreSQL 15.18 на `63.182.239.155:5432`, credentials от `.env` (`POSTGRES_USER/PASSWORD/DB`). Порт 22 е затворен, 5432 е отворен отвън. Потребителят разрешава **само четене** — винаги ползвай `PGOPTIONS='-c default_transaction_read_only=on'`.
- **Мащаб на каталога:** 26 567 продукта общо, но само **7 276 са видими** за търсачката (active + show_flag + status='AVAILABLE' + image_url непразен). 15 573 са NOT_AVAILABLE+скрити (по дизайн), 2 417 LIMITED_QUANTITY+скрити (по дизайн).
- **`deduplicateCrossPlatformBySku()`** (ProductRepository:225) скрива по-скъпите дубликати по `sku` с приоритет VALI > TEKRA > ASBIS > MOST. Ратчет е: `dup_skus` гледа само `show_flag=true`, така че скрит губещ никога не се реактивира ако победителят изчезне.
- **MOST sync е one-way ratchet** (MostSyncService:966-972): за съществуващи продукти може само да СКРИЕ, никога да покаже. Затова 272 MOST продукта са AVAILABLE с цена и картинка, но невидими завинаги.
- **FTS GIN индексът от V5 не се ползва** при търсене. OR-веригата с `m.name ILIKE` (join-нат таблица) кара планера да прави bitmap scan по `idx_products_active_show` и да пресмята `to_tsvector` за всичките 7276 реда → 319ms.
- **`word_similarity(a,b) > 0.35` НЕ ползва trigram индекс** — само операторът `<%` го ползва, но той работи с `pg_trgm.word_similarity_threshold` (default 0.6), не с литерала в заявката. За индексиран fuzzy: `SET pg_trgm.word_similarity_threshold = 0.35` + `<%`.
- **Имената на категориите носят множественото число** ("Лаптопи", "Монитори") — включването на `c.name_bg` в търсенето поправя plural заявките без нужда от stemming.
- **Транслитерацията е реален gap:** 'гейминг' → 16 резултата срещу 'gaming' → 729; 'леново' → 0 срещу 'lenovo' → 80.
- **`.env` редове 39-41 са повредени:** `JAVA_OPTS` е без кавички (чупи `source`), а `ASBIS_USERNAME:detelin` / `ASBIS_PASSWORD:...` ползват `:` вместо `=` → тези две променливи най-вероятно НЕ се подават на контейнера.

## Key Learnings (Валутна конвенция — установено 2026-09-18)

- **`price_client` и `final_price` са ВИНАГИ в ЕВРО без ДДС.** Това е конвенцията на цялата система. Фронтендът прави `finalPrice * 1.2` за евро с ДДС и после `* euroRate` за лева. НИКОГА не конвертирай цена от доставчик към лева при запис в базата.
- **Всички четири sync сервиза пазят цената от доставчика както е** (`ValiSyncService:1016`, `AsbisSyncService:893`, `TekraSyncService:1125`). MOST беше единственото изключение и това беше бъг (bug-466), поправен във фаза 0.1.
- **Как да проверяваш валутна хипотеза:** ASBIS е котвата — `PriceAvail.xml` изрично подава `<CURRENCY_CODE>EUR</CURRENCY_CODE>`. VALI и TEKRA не подават валутно поле, но съвпадат с ASBIS по общ SKU (медиана 1.010 съответно 1.000). Заявката за сравнение е в `SEARCH_AUDIT_PLAN.md` секция 1.2.
- **MOST feed:** `https://portal.mostbg.com/api/product/xml/all?currency=EUR` — XML със структура `<data><productList><product id="..."><price>`, `<currency>`, `<PartNumber>` (мапва се към `products.sku`), `<product_status>` ("В наличност"). 6089 продукта, ~17MB.
- **TEKRA API:** `action=categories` връща 21 root категории със `slug`. Продуктите се вземат с `action=browse&catSlug=<slug>&feed=1`, което връща **XML** (не JSON), игнорира `perPage`. **Rate-limit-ва агресивно — HTTP 429 след ~6 бързи заявки.** Не пускай тестове в цикъл.
- **Няма тестове за sync сервизите.** Цялата тест сюита е 3 файла (`TechStoreApiApplicationTests`, `TbiLeasingControllerTest`, `TbiLeasingServiceTest`).

## User Preferences (потвърдено от потребителя 2026-09-18)

- **Курсът лев/евро е ТВЪРДО ФИКСИРАН на 1.95583.** Не е плаващ, не се взема от API, не се конфигурира. Винаги точно 1.95583 — никога 1.96 или друга стойност. (Потребителят потвърди изрично.)
- **Разграничението, което трябва да се пази в кода:** EUR/BGN е фиксиран peg → уместно е да е константа. USD/EUR плава → трябва да е конфигурация, а не hardcode-нат курс.
- **Къде живее фиксираният курс:** `OgMetaController:42` (`EURO_RATE = 1.95583`) в бекенда и `euroRate` в `care-tech-ui/src/utils/utils.js` за фронтенда. Ползва се САМО за показване (евро → лева). Sync сервизите НЕ конвертират — те пазят евро (виж Key Learnings за валутната конвенция).

## Do-Not-Repeat (продължение)

- [2026-09-18] NEVER пиши SQL скриптове в `scripts/` с psql meta-команди (`\echo`, `\set`, `\timing`, `\d`). Потребителят пуска скриптовете през GUI клиент (DBeaver/DataGrip), който изпраща съдържанието директно към сървъра → `SQL Error [42601] syntax error at or near "\"`. Ползвай чист SQL: `RAISE NOTICE` в `DO $$` блок за съобщения, нормални `SELECT`-и за отчети. За безопасност не разчитай на `ON_ERROR_STOP` — в PostgreSQL грешка в явна транзакция я прави aborted и `COMMIT` действа като `ROLLBACK` автоматично.

## Do-Not-Repeat (SQL скриптове към прод)

- [2026-09-18] NEVER слагай `ALTER TABLE` в SQL скрипт без свой явен `BEGIN/COMMIT` и без `SET lock_timeout`. `ADD COLUMN` взима ACCESS EXCLUSIVE lock върху таблицата — блокира дори четене. Потребителят пуска скриптовете от GUI клиент в **manual-commit** режим, който остави транзакцията отворена и **свали сайта за 6 минути** (22 Hibernate заявки блокирани, connection pool изчерпан). Задължителен шаблон в началото на всеки скрипт, който пипа `products`: `SET lock_timeout = '10s'; SET idle_in_transaction_session_timeout = '60s';` — второто кара сървъра сам да прекъсне забравена транзакция. Утежняващо: чакащ ACCESS EXCLUSIVE блокира и всички последващи четения зад себе си в опашката.

- [2026-09-18] NEVER проверявай точността на числова конверсия чрез ОТНОШЕНИЕ, когато резултатът е закръглен. `round(staro/novo, 3) <> 1.956` даде 272 фалшиви положителни, защото `novo` е закръглено до 2 знака и при малки цени закръглянето доминира: `0.02 -> 0.01` дава отношение точно `2.00`. Сравнявай АБСОЛЮТНИЯ остатък: `abs(staro - novo * kurs) > tolerans`, където `tolerans = 0.005 * kurs` за закръгляне до 2 знака. Границата се проверява предварително read-only с `max(abs(...))`.

## Key Learnings (Видимост на продукти — фаза 1, 2026-09-18)

- **`show_flag` вече НЕ носи ръчното намерение.** Добавена е колона `products.manually_hidden` (V36). Правилото: `show_flag` е изцяло на разположение на sync-а и се преизчислява всеки прогон от наличност/цена/картинка; `manually_hidden` пази решението на админ и sync-ът никога не пре-показва ред с този флаг. Админ скриването минава през `ProductService.applyAdminVisibility()`.
- **НИКОГА не прави sync логиката еднопосочна (ратчет), за да пазиш ръчно намерение.** Това беше коренът на bug-460: MOST sync-ът можеше само да скрива, защото нямаше как да различи „админ го скри" от „sync го скри". Резултатът бяха 273 невидими налични продукта. Правилният подход е отделен флаг за намерението.
- **`deduplicateCrossPlatformBySku()` вече избира от ГОДНИТЕ редове**, не от `show_flag = true`, и присвоява `show_flag` в двете посоки (`SET show_flag = (rn = 1)`). Старата версия беше ратчет: скрит ред беше невидим за следващия прогон и не можеше да спечели пак, а когато победителят излезеше от наличност, целият SKU изчезваше от сайта (91 SKU в това състояние).
- **Пълно преизчисляване на видимостта се задейства с `POST /api/sync/most/products`** (или съответния endpoint за другите платформи) — всичките четири sync сервиза викат dedup-а накрая. Иначе се случва при нощния cron в 03:00.
- **В ASBIS sync `visible` се ползваше и за `setStatus`, и за `setShow`.** Разделено: статусът следва само наличността. Ръчното скриване не бива да прави продукта „неналичен".

## Key Learnings (Търсачка — фаза 2, 2026-09-18)

- **`NamedParameterJdbcTemplate` се ползва САМО от `ProductSearchRepository`.** Затова query timeout-ът (15 s) е сложен на този bean в `SearchConfig`, а не глобално на datasource-а — глобален `statement_timeout` би прекъснал дългите bulk заявки на sync-овете, които минават през JPA.
- **`buildFacets` е премахнат изцяло** (не зад флаг). Отговорът на `/api/products/search` вече връща `facets` като празна карта. Ако някога потрябва faceting на страницата за търсене — ползвай `getFilteredFacets`, не възстановявай стария метод.
- **`care-tech-ui` е Create React App (`react-scripts`), НЕ vite.** Билдва се с `npm run build`. Внимание: с `CI=true` react-scripts третира warnings като грешки и билдът пада заради множество предварително съществуващи `no-unused-vars` в други файлове. За проверка на собствените промени пускай `npm run build` без `CI`.
- **RTK stale-response шаблон:** `createAsyncThunk` дава `action.meta.requestId` и `signal`. Пази `currentRequestId` в state-а при `pending`, сравнявай го в `fulfilled`/`rejected` и игнорирай несъвпадащите. Отменена заявка (`action.meta.aborted`) НЕ е грешка — не я показвай в UI.

## Key Learnings (Търсачка — фаза 3, 2026-09-18)

- **Категорията е част от търсенето.** Множественото число в този каталог живее в имената на категориите („Лаптопи", „Монитори"), а не в имената на продуктите („Lenovo IdeaPad Slim 3"). Затова per-word ILIKE се прави върху blob, който включва `c.name_bg`/`c.name_en` и `m.name`. Без тях „лаптоп lenovo" връщаше 1 резултат, с тях — 50.
- **Евристика за разграничаване на продукт от аксесоар:** категориите-аксесоари са именувани „X **за** Y" („Чанти за лаптопи", „Стойки и основи за камери"), основните не са („Лаптопи", „IP камери"). Скорингът ползва наличието на „ за " в името на категорията, за да свали бонуса от 3.0 на 0.5. Това е евристика върху конкретните данни — ако именуването се промени, преразгледай я.
- **Нормализация без интервали** (`replace(lower(...), ' ', '')`) при сравнение на имена — така „rtx 5080" намира продукт „GB RTX5080 GAMING OC 16G". Без нея тези карти получаваха ранг 0.
- **Trigram индекси по колони НЕ помагат на per-word ILIKE върху конкатениран blob** от няколко таблици — никакъв индекс по колона не обслужва такъв израз. `EXPLAIN ANALYZE`: достъпът до таблицата е 12 ms, оценката на изразите върху 7540 реда — 320 ms. Тесното място е преизчисляването на `to_tsvector` на всеки ред. Реалното решение е материализирана `tsvector` колона (`GENERATED ALWAYS AS ... STORED`) + GIN върху нея, но това иска миграция с пренаписване на таблицата.
- **`word_similarity(a,b) > 0.35` не ползва индекс.** Само операторът `<%` го ползва, но той чете прага от GUC `pg_trgm.word_similarity_threshold` (default 0.6), не от литерала в заявката. За индексиран fuzzy трябва `SET LOCAL pg_trgm.word_similarity_threshold` в транзакцията.
- **Магазинът НЕ продава iPhone-и** — само аксесоари (кабели, USB памети). Заявка „iphone 15" няма как да върне телефон; това не е проблем на подреждането.

## Key Learnings (Фаза 4 — чистене, 2026-09-18)

- **`spring.hikari` НЕ е валиден ключ.** Реалната конфигурация на пула е `spring.datasource.hikari` (20 връзки). Втори блок седеше като `spring.hikari` и стойностите му се игнорираха мълчаливо. Премахнат, а не слят — `leak-detection-threshold` би се задействал постоянно при sync прогоните, които легитимно държат връзка минути наред.
- **`app.search.postgresql.performance-test` вече реално се чете** от `SearchIndexManager` и е `false`. Преди това двете probe заявки се пускаха при всеки старт, независимо от настройката.
- **`.env` е gitignored и локалното копие е повредено** (редове 40-41 с `:` вместо `=`, `JAVA_OPTS` без кавички). Прод копието е коректно — знае се, защото `application.yml` резолвира `${ASBIS_USERNAME}` без default и приложението нямаше да стартира иначе. Не пипай този файл.
- **`@Cacheable` с `unless = "#result == null || #result.isEmpty()"`** — задължително, когато методът гълта изключения и връща празна колекция. Иначе една преходна грешка се кешира за целия TTL (тук 1 час).

## Key Learnings (Транслитерация — фаза 3.8, 2026-09-18)

- **`CyrillicTransliterator` (`com.techstore.util`)** превръща кирилска дума в латинската форма, с която каталогът реално е записан. Две стъпки в този ред: (1) таблица `ALIASES` за фонетични изключения, (2) официална българска транслитерация. Връща `null`, ако думата няма кирилица — така латинските заявки не плащат нищо.
- **Правилата НЕ стигат за фонетичните марки:** сони→soni (не sony), филипс→filips (не philips), логитек→logitek (не logitech), гейминг→geyming (не gaming). Тези задължително минават през `ALIASES`. Разширяването е един ред — добавяй марка, щом изскочи в логовете с нула резултати.
- **Транслитерация ≠ превод.** Българските нарицателни вече работят през името на категорията (мишка 260, клавиатура 170, принтер 191), затова в таблицата няма „мишка→mouse".
- **`lower()` + `LIKE` вместо `ILIKE`** в `SEARCH_BLOB`: ILIKE прави case-folding при всяко сравнение, а кирилска дума струва две сравнения. На „гейминг лаптоп" (4 сравнения) разликата е ~800 ms спрямо ~550 ms. Патърните се подават вече снижени с `toLowerCase()`.
- **Има unit тест** — `CyrillicTransliteratorTest`, 28 теста. Това е първият тест за търсачката; функцията е чиста и затова евтина за покриване.

## Do-Not-Repeat (търсачка)

- [2026-09-18] Когато добавяш нов начин за СЪВПАДЕНИЕ в търсачката (транслитерация, синоними, стеминг), задължително го добави и в СКОРИНГА, не само в `WHERE`. Иначе всички редове, които съвпадат само по новия начин, получават ранг 0 и падат обратно на подреждане по цена — тоест намираш продукта, но той е заровен. Конкретно: транслитерацията влезе само в WHERE и 728 от 744 резултата за „гейминг" бяха с ранг 0. Проверката е една заявка: `GROUP BY round(score,2)` върху резултатите — ако почти всичко е 0, скорингът не вижда как са съвпаднали.

## Do-Not-Repeat (диагностика)

- [2026-09-18] NEVER заключавай „sync-ът не работи" от `max(products.updated_at)`. Hibernate не издава UPDATE, когато никое поле не се е променило, затова `updated_at` остава стар дори след успешен прогон. Заключих грешно, че TEKRA не е синхронизирана от 11 септември — всъщност върви всяка нощ със SUCCESS и 98 от 98 цени съвпадат точно с живия feed. Правилният източник е таблицата `sync_logs` (`sync_type`, `status`, `created_at`, `error_message` съдържа обобщението), а за потвърждение — сверка на извадка срещу feed-а на доставчика.

## Key Learnings (Sync — запазване на категорията, 2026-09-18)

- **Правило за всички sync сервизи:** ако мапингът на категория се провали за продукт, който ВЕЧЕ съществува при нас, ползвай съществуващата му категория, вместо да го пропускаш. Пропускането не е безобидно — продуктът никога не минава през update-а, затова цената и наличността му замръзват от последния успешен мач и налични продукти стоят NOT_AVAILABLE безкрайно.
- Състояние по платформи след 2026-09-18: **ASBIS** го прави отдавна (`AsbisSyncService:601-613`, референтната имплементация); **VALI** задава категория само при създаване, тоест съществуващите я пазят по дизайн; **TEKRA** и **MOST** бяха поправени.
- Ползвай `ProductRepository.findCategoryIdsBySkuAndPlatform()` — връща id, а не entity, за да не се докосва lazy асоциация при `open-in-view=false`.
- **`processedSkus` в TekraSyncService се пълни на ред 744, по време на ЧЕТЕНЕТО от feed-а**, не след обработката. Значи `markNotAvailableByPlatformSkuNotIn` вижда пропуснатите продукти като „видени" и НЕ ги маркира неналични. Не обвинявай mark-unseen стъпката, без да провериш къде се пълни списъкът.

## Key Learnings (Тестове, 2026-09-18)

- **Сюитата е зелена: 52/52.** Ако видиш червено, първо провери дали не е от липсваща променлива в `src/test/resources/application-test.properties` — там трябва да има стойност за ВСЯКА `${VAR}` без default в `application.yml`. Само тези, свързани към не-String тип, чупят контекста; String-овите мълчаливо приемат литерала `${VAR}`.
- **`SPEEDY_SENDER_ID` задължително е числова** — `SpeedyConfig.senderSiteId` е `Long`.
- **`npm run build` с `CI=true` пада** заради предварително съществуващи `no-unused-vars` в care-tech-ui. За проверка на собствени промени пускай без `CI`.

## Key Learnings (Категории и sync, 2026-09-18)

- **Три от четирите sync сервиза пре-задават `category` на всеки прогон**, не само при създаване: `MostSyncService:917` и `TekraSyncService:1124` са ИЗВЪН `isNew` блока, а `AsbisSyncService:625-628` задава и в двата клона. Само `ValiSyncService:990` е в `isNew`. Следствие: ръчна SQL корекция на `category_id` се връща от нощния sync до 24 часа за MOST, TEKRA и ASBIS — поправяй МАПИНГА, не данните.
- **Изискване на потребителя (2026-09-18):** мапингът към категория трябва да е верен и при СЪЗДАВАНЕ на продукт, не само при обновяване. Затова флаг тип `manually_hidden` за категорията е недостатъчен — той пази ръчна корекция, но не прави новите продукти правилни.

- **Състояние на категорийното дърво (измерено 2026-09-18):** 941 категории, само 221 видими, **591 празни (63%)**, **245 дублирани имена** засягащи 652 категории (69%), 60 имена с 4+ копия. 17 имена са разцепени — реални продукти разделени между копия („флаш памети" id 321 със 128 и id 517 с 442; „IP камери" id 100 с 362 и id 964 с 42). Шаблонът „4 копия, 3 празни" сочи, че всяка платформа е създала свое копие на дървото. Докато това е така, никой мапинг към категория не може да е еднозначен.

- **Категорийното дърво има ръчно направена подредба и точно 3 нива — не ги чупи.** 219 от 221 видими категории имат `sort_order > 0`; кореновите са курирани (КОМПЮТРИ 1, Компютърни компоненти 2, Лаптопи 3, Компютърна периферия 5, Монитори и дисплеи 5, Геймърска периферия 6). Нива: 129 главни / 785 подкатегории / 27 под-подкатегории. При сливане на дубликати оцелява `sort_order` на ОСТАВАЩАТА категория. `category_path` се преизчислява с `generateCategoryPath()` при смяна на родител — обновявай го при всяка структурна промяна.

## Do-Not-Repeat (анализ на категории)

- [2026-09-18] NEVER мери дублирани категории само по ИМЕ. Еднакво име на различни места в дървото не е дубликат — родителят ги различава. „Монитори" съществува под „Монитори и дисплеи" (компютърни) и под „Монитори и аксесоари → Видеонаблюдение" (за видеонаблюдение); „IP камери" и „NVR" съществуват веднъж под „IP системи" и веднъж под „NDAA, NIS2" (регулаторен стандарт). Всички са УМИШЛЕНИ. Правилната метрика е име + родител: така истинските дубликати са 33 групи / 102 категории, и НИТО ЕДНА не е разцепена — празни копия са. Ако бях действал по метриката „по име", щях да слея монитори за видеонаблюдение с компютърни.

## Key Learnings (MOST категориен мапинг — Фаза 5, 2026-09-18)

- **MOST feed-ът има `<subcategory>`, което до 2026-09-18 изобщо не се парсваше** в `MostApiService`. 222 двойки `(category, subcategory)` се свиваха в 29 кошници. Сега `MostSyncService.resolveTargetCategoryName()` гледа двойката първо, после само категорията.
- **`LAPTOP_CATEGORY_NAME_OVERRIDES` имат ПРИОРИТЕТ над мапинга по двойки** и се прилагат и за „Аксесоари за лаптопи/таблети", не само за „Лаптопи". Причина: името е по-специфично от feed-а — 43 продукта са правилно в „Чанти за лаптопи" и 22 в „Зарядни за лаптопи" именно заради тях. Логиката е в `applyLaptopNameOverride()` и приоритетът е заключен с тестове. Не обръщай реда.
- **Имената на категории НЕ са уникални** — „Слушалки" и „Мрежови кабели" съществуват по 3 пъти (една видима VALI + две скрити ASBIS). Всяко търсене на категория по име трябва да подрежда **видими първо, после най-малко id**, иначе продукт попада в категория, до която не се стига.
- **Override правилото `→ „Таблети"` сочи към id 744 — ASBIS, НЕВИДИМА**, с 19 продукта. Таблетите вече се пращат там, където не се стига. Чака решение за създаване на видима „Таблети".
- **Винаги прогнозирай преместванията срещу прод преди комит.** Първата версия на този мапинг щеше да влоши 65 продукта, за да поправи 3 — видя се само защото сверих SKU-тата от feed-а с текущите им категории в базата.

## Key Learnings (ASBIS категории — 2026-09-18)

- **`AsbisSyncService` търси категории от ниво 1 САМО сред кореновите** (`rootByName`, строен от `parent == null`). Ако категория, създадена от ASBIS, бъде осиновена в курираното дърво (сменен родител), ASBIS няма да я намери и ще **създаде нова коренова** — а `createCategory` слага `setShow(true)` и `setSortOrder(0)`, тоест дубликатът излиза ВИДИМ най-отгоре в менюто. Добавен е fallback `anyByName` (видими първо, после най-малко id), който трябва да се пази при всяко бъдещо преместване на ASBIS категория.
- **„Таблети" (id 744) е ASBIS коренова категория**, родител на `PC таблет` (745) и `PC таблет с Windows` (746). Продуктите в самата 744 са MOST (попадат там през `LAPTOP_CATEGORY_NAME_OVERRIDES → „Таблети"`), а в децата са ASBIS. ASBIS подава `productcategory = Таблети`, затова тя се пресъздава, ако изчезне от кореновите.
- **`category_path` е попълнен САМО за ASBIS/TEKRA категории** — VALI категориите (36, 37, 39, 47…) имат празен път. Не приемай, че е задължителен.
- **Свободни слотове в `sort_order`** — под родител 36 редът минава 6 → 8, тоест 7 е свободен. При добавяне на категория търси свободен слот, вместо да пренареждаш съществуващите.

- **`CONTR I/O` в MOST feed-а няма подкатегория изобщо** — всичките 112 продукта идват с празен `<subcategory>`. Затова „Входно-изходни контролери" (id 22, VALI, под „Компютърни компоненти") си остава кошница с TV стойки и LED кабели, и мапингът по двойки не може да помогне. 169 от 254-те продукта там са VALI, тоест проблемът е и от тяхна страна.

- **7 от 35-те цели на `MOST_CATEGORY_MAPPING` сочат към категории, които съществуват САМО скрити** (мултимедиен хардуер 658, игри и мултимедия 492, ssd 259, охладители 176, мрежов хардуер 110, настолен nas, компютър всичко в едно) — общо 1705 продукта. Поправката „видими първо" в `categoryIdsByName` НЕ помага там, защото няма видима алтернатива със същото име. При добавяне на правило в MOST мапинга ВИНАГИ проверявай дали целевата категория е видима.
- **„Мрежов хардуер" (342) и „Охладители" (402) са 100% MOST продукти** — въпреки че са ASBIS категории. Тоест не всеки продукт в скрита ASBIS категория е ASBIS проблем; проверявай `p.platform`, преди да го причислиш към ASBIS миграцията.

## Do-Not-Repeat (търсене на целеви категории)

- [2026-09-18] NEVER търси кандидат-категория с един корен на думата. Ползвах `LIKE '%охлажд%'` и пропуснах **„Охладители за процесори"** (съдържа „охладител") и **„Термо пасти и подложки"** — и двете видими, уникални и с по 251 и 124 продукта. Резултатът беше, че 61 процесорни охладителя отидоха в обща „Вентилатори", а 6 термопасти останаха скрити. Проверявай с няколко варианта на корена (охлажд/охладител/вентилатор/термо) или изброй всички деца на очаквания родител, преди да заключиш, че категория няма.

- **`ValiSyncService.updateCategoryParentsOptimized()` задава родител САМО когато текущият е `NULL`.** Значи щом VALI категория получи родител (ръчно или със скрипт), VALI повече не го пипа. `show_flag` и `sort_order` се задават единствено в `createCategoryFromExternal()`, тоест при СЪЗДАВАНЕ. Преместване на VALI категория в дървото е трайно — за разлика от ASBIS, където важи обратното (виж пазача `anyByName`).

## Do-Not-Repeat (SQL)

- [2026-09-18] ВНИМАНИЕ с `NOT (... OR column = value)`, когато колоната може да е NULL. `NOT (c.id IN (454,556) OR c.parent_id = 454)` изключва и всички редове с `parent_id IS NULL`, защото `FALSE OR NULL = NULL` и `NOT NULL = NULL` → редът не минава филтъра. Това ми даде симулация „495 → 18" вместо вярното „495 → 420". Ползвай `coalesce(c.parent_id, -1) = 454` или `c.parent_id IS NOT DISTINCT FROM 454`. В положителен контекст (`WHERE ... OR c.parent_id = 454`) няма проблем — там NULL просто не съвпада, което е желаното.

## Key Learnings (ASBIS дърво — стъпка 3, 2026-09-18)

- **`CronJobService` (нощният прогон 01:00) НЕ вика `syncAsbisCategories`** — само `syncAsbisParameters`, `syncAsbisProducts`, `syncAsbisPriceAvail`. Затова ASBIS категорийният sync последно е вървял на 2026-08-06. Пуска се само ръчно.
- **Изтриването на празни ASBIS категории е опасно**: от 519-те празни листни, **303 още се рефират от `ProductList.xml`** (41 стойности `PRODUCTCATEGORY` + 253 двойки с `PRODUCTTYPE`). При изтриване следващият категориен прогон би ги пресъздал с `show=true, sortOrder=0`, тоест като видими корени най-отгоре в менюто.
- **216 са безопасни за изтриване, но НИТО ЕДНА не се сблъсква по име с видима категория** — тоест изтриването им не решава двусмислието при търсене на категория по име, което беше единствената реална полза. Остава само козметика.
- **FK-ите към `categories`**: `products.category_id` SET NULL, `categories.parent_id` SET NULL, `categories.alias_of_id` SET NULL, **`category_parameters.category_id` CASCADE**. Изтриването на категория трие и параметърните ѝ връзки.

## Key Learnings (възпроизводимост, 2026-09-18)

- **Категорийната структура НЕ е възпроизводима от кода.** Няма нито една Flyway миграция, която създава категории или производители. Днешното дърво (941 категории, 221 видими с ръчна подредба) е натрупано състояние в прод базата от sync-овете + **20 от 51-те скрипта в `scripts/`** + ръчни редакции. Деплой от нула не дава същото меню. Виж `CATEGORY_REPRODUCIBILITY_PLAN.md`.
- Производителите също се създават само от sync-овете (`MostSyncService:410`, `AsbisSyncService:88`, `ValiSyncService:100`), без seed.

## Do-Not-Repeat (категоризация)

- [2026-09-18] Груповото преместване на цяла категория („преместѝ всичко от X в Y") работи САМО ако изходната категория е хомогенна. Преместих „Мрежови продукти" изцяло в „USB хъбове" и вкарах там Ubiquiti суич и DAC кабел — „USB хъбове" се появи в списъка с подозрителни категории заради собствената ми миграция. Преди групово преместване провери състава с повече от 3-5 реда извадка, или раздели по ключови думи.
- [2026-09-18] Не представяй работа с правила на ниво категория като „мапване на продуктите". От 7469 видими продукта тази работа докосна 341 (~5%); останалите не са поглеждани. Структурните причини са премахнати, но per-product класификацията е отделна и незапочната задача.

### [2026-09-18] Синхронизациите презаписват категорията на съществуващи продукти
`MostSyncService:1018` и `TekraSyncService:1124/:1375` присвояват категория **безусловно**
при всеки прогон. `AsbisSyncService:663` — винаги когато мапингът от feed-а успее (пази
съществуващата само при провал). `ValiSyncService:990` — само при създаване.
Следствие: всяка ръчна корекция на категория се губи до 24 часа, ако не е защитена.
Решение: `products.manually_categorized` (V37), огледален на `manually_hidden` (V36).
Sync-овете прескачат присвояването при вдигнат флаг; `ProductService.applyAdminCategory`
го вдига САМО при реална промяна на категорията.

### [2026-09-18] Потребителят иска ръчно мапване, не AI в приложението
На предложение за AI класификация: „не искам AI мапинг в приложението, а ти да го
направиш - продукт по продукт". Тоест: обхождане на целия каталог лично, не вграждане
на класификатор. Груповите премествания по категория са показали, че внасят собствени
грешки (суич и DAC кабел в „USB хъбове", скрипт 51).

### [2026-09-18] Невидимите продукти са част от обхвата, не отделен етап
На „първо невидимите, за да пуснем един скрипт наведнъж": когато поправяш категории,
НЕ спирай при видимите. Продукт в категория с `show_flag`, който е активен и неизтрит,
но без наличност или снимка, се появява в каталога в деня, в който доставчикът я върне —
в грешната категория. Каталогът се цапа обратно, само по-бавно. В този магазин съотношението
е 7 371 видими към 14 565 невидими-но-застрашени; вторите дадоха 3 032 поправки срещу
1 114 при видимите. Доставяй едното с другото в един скрипт.

### [2026-09-18] Съкратените имена в работния файл крият цели категории грешки
Работният файл с каталога държеше `left(name, 56)`. За повечето категории това стига —
разпознаваемата дума е в началото („Видео карта …", „Тонер касета …"). Но при геймърската
периферия имената са само модели: `CORSAIR K70 MAX RGB Magnetic-Mechanical Backlit RGB LED
MGX Black PBT Keycaps` — думата, която издава клавиатурата, е чак след 56-ия знак. Заради
това първият проход пропусна 61 стола, 9 бюра, 22 мишки и 17 клавиатури в кат. 179.
Правило: преди да обявиш категория за прегледана, направи поне една кръстосана проверка
върху **пълните** имена, извлечени от базата.

### [2026-09-18] Правило за спорна категория: „там, където са сестринските продукти"
Когато един продукт може да отиде в две еднакво защитими категории (CANYON OnRiff —
жични слушалки в 66 или Bluetooth в 159), решаващото е къде вече седят другите продукти
от същата серия. Проверка: `SELECT category_id, count(*) ... WHERE name ILIKE '%серия%'`.
При CANYON слушалките: 24 в кат. 66, 18 в 59 (грешно), 5 в 35 (грешно) → целта е 66.
Иначе разкъсваш сестрински продукти и създаваш нова непоследователност.

### [2026-09-18] Не местѝ през структурна граница, която самият магазин не спазва
Кат. 67 е чисто жични тапи, 159 е всичко безжично, но 66 „Слушалки" съдържа и двете —
и във ВИДИМИЯ набор. Да преместя безжичните слушалки от 66 в 159 само в скрития набор
би разделило еднакви продукти. Правилният ход е да извадиш само това, което изобщо не е
от този вид (тонколони и микрофони в „Слушалки"), а структурния въпрос да го запишеш
като отворен въпрос за потребителя.

### [2026-09-18] Проектът се билдва САМО с Java 17 — с 21 гърми в Lombok
Системният `java` на тази машина е OpenJDK 21.0.12 и `mvn compile` умира с
`java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN` —
това е несъвместимост на Lombok с javac от 21, а не грешка в кода. `pom.xml` иска
`<java.version>17</java.version>`. Преди всеки mvn:
`export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home`
Пълният `mvn test` върви ~17 минути (TechStoreApiApplicationTests ~1018 s и
TbiLeasingControllerTest ~634 s) — пускай го на заден план.

### [2026-09-21] Три различни проверки хващат три различни класа грешки
Кръстосаната проверка по ключови думи (продукт с дума X в грешна категория) НЕ хваща
продукти, чието име изобщо няма разпознаваема дума — само модел. Това е отделен клас и
иска отделен метод: филтрирай имената без кирилица и без общи съществителни, после ги
прегледай по категория. Трета проверка (отрицателна: продукт в кат. X без нито една
характерна за X дума) хваща трети клас. Върху видимия набор от 7 371 продукта:
кръстосаната даде 2 находки, непрозрачните имена — 41, отрицателната — 9. Ако беше
само първата, 50 от 52-те грешки оставаха.

### [2026-09-21] Кат. 47 „Аксесоари за лаптопи/таблети" е кофа за ASUS/ACER калъфи
41 калъфа за таблет (ASUS VERSASLEAVE/TRICOVER/MAGSMART/TRAVEL COVER, ACER PORTFOLIO
CASE/ICONIA POCKET) стояха в 47, докато „Калъфи за таблети" (41) държеше само HAMA и
Hannspree. Имената са само модели, затова не изплуваха при нито един текстов филтър.
Защитните фолиа (`ACER AG PROTECT FILM`) СА на място в 47 — за тях няма категория.

### [2026-09-21] Кат. 165 приема фолиа за таблети, не само за телефони
На въпроса къде да идат 6-те защитни фолиа ACER за таблет (в 47 „Аксесоари за
лаптопи/таблети", защото 41 е „Калъфи", а 165 се казва „…за телефони"):
„мисля че спокойно могат да са в 165". Тоест: когато името на категория е
по-тясно от разумното ѝ съдържание, потребителят предпочита да сложи стоката
там, а не да измисля нова категория. Преименуването остава отделен въпрос.

### [2026-09-21] Кат. 165 = ВСИЧКИ протектори за екран (телефон, таблет, конзола)
След „и Switch протекторите в 165" правилото е окончателно: протектор за екран →
165, независимо от устройството. Защитните КАЛЪФИ (Switch Polycarbonate Case,
Silicon Glove) НЕ влизат — те са калъфи. Кат. 165 расте от 1 на 14 продукта.
Следствие: името ѝ „…за телефони" вече е по-тясно от съдържанието — отбелязано
като отворен въпрос, дървото не се пипа без разрешение.

### [2026-09-21] Разширяване на правило → пусни проверката наново по-широко
Когато потребителят разшири правило („и Switch протекторите"), не мести само
посочените — пусни търсенето наново с по-широки термини. Така изплува
`NOKIA 8.3 GLASS EDGE PROT /BLK`, който нито „protector", нито „фолио", нито
„защитно стъкло" хващаха. Същевременно провери за припокриване с вече записаното:
`27439 NOKIA LUMIA 800 P. FILM` изглеждаше нова находка, но първият обход вече
го беше хванал — щях да го отчета два пъти.

### [2026-09-21] Преименуване на категория е промяна по КОДА, не само по базата
Кат. 165 се казваше „Защитни фолиа / стъкла за телефони". Преди да я преименувам
намерих две места, които държат името ТВЪРДО КОДИРАНО и търсят категорията ПО ИМЕ:
  • `CategoryReorganizationService:683` (VALI_SUBCATEGORIES) — `reorganizeValiCategories`
    търси по нормализирано име и СЪЗДАВА категорията, ако не я намери. Разминаване
    между кода и базата — В КОЯТО И ДА Е ПОСОКА — прави дубликат при POST /reorganize.
    Ендпойнтът е ръчен, не в cron.
  • `MostSyncService:124-125` — override-ите „protect film" и „screen protector"
    сочеха „Аксесоари за лаптопи/таблети". Резолвват се по име (`MostSyncService:986`,
    `categoryIdsByName.get(name.toLowerCase())`).
Правило: преди преименуване на категория — `grep` за името в целия src/, и провери
кой sync пипа `categories.name_bg` за СЪЩЕСТВУВАЩИ редове.

### [2026-09-21] Кои sync-ове пипат имена на СЪЩЕСТВУВАЩИ категории
  • VALI  — НЕ. `ValiSyncService:186-195` за съществуваща прави само `skipped++`.
            `updateCategoryParentsOptimized` пипа parent само ако е NULL.
  • TEKRA — НЕ. `TekraSyncService:1819` е `if (getNameBg() == null || isNew)`.
  • ASBIS — НЕ. `createCategory` се вика само за нови.
  • MOST  — не пише имена, но ЧЕТЕ по име (`findByNameBg`, `categoryIdsByName`).
Тоест ръчно преименуване на категория ОЦЕЛЯВА нощния sync — не трябва флаг като
`manually_categorized`. Рискът е само от кода, който чете по име.

### [2026-09-21] `categories.slug` се ползва за рутиране — не го пипай при преименуване
`GET /categories/slug/{slug}` (CategoryController:67). Смяна на slug чупи
съществуващи URL-и и е SEO решение, не езиково. При преименуване на 165 оставих
`screen-protector-for-mobile-phone` непокътнат — съзнателно разминаване с името.

### [2026-09-23] Тих провал при външни feed-ове — моделът се повтаря в целия проект
`MostApiService.getAllProducts()` връщаше празен списък при ВСЯКА грешка, а
`MostSyncService` записваше това като `LOG_STATUS_SUCCESS` с „No products found".
Прекъснат fetch беше неразличим от доставчик без стока и никой не разбираше.
Същият модел: [[vali_sync_status_bug]] и `AsbisApiService` (там празно тяло без
Exception не увеличава `attempt` → безкраен цикъл).
**Правило:** при 0 записа от външен източник питай първо `duration_ms` в
`sync_logs` — под ~1 s значи мрежова грешка, не празен feed. Работен MOST прогон
е ~70 000 ms.

### [2026-09-23] Всеки доставчик трябва да има собствен RestTemplate
`WebConfig:18` дава `@Primary new RestTemplate()` — БЕЗ timeout и БЕЗ retry.
MOST го ползваше, затова един преходен отказ отменяше цялата нощна синхронизация.
Образецът е `AsbisApiService`: изричен конструктор с `@Qualifier`, отделен bean в
`RestTemplateConfig`, ръчен retry цикъл с експоненциален backoff. Проектът НЯМА
spring-retry — не посягай към `@Retryable`. VALI ползва WebClient + Reactor
`Retry.backoff`, което е трети образец; не ги смесвай.

### [2026-09-23] Всеки sync метод в MostSyncService вече препраща изключението
Външните `catch (Exception e)` в `syncMostManufacturers/Parameters/Products`
записват `LOG_STATUS_FAILED` и правят `throw`. Затова е достатъчно дадена грешка
просто да се хвърли — логването и Slack известието (`Markers.CRITICAL` в
`CronJobService`) идват сами. Не дублирай `updateSyncLogSimple` преди `throw`.

### [2026-09-24] MOST feed-ът се тегли ДВА пъти на нощ
`MostApiService` кешира 10 минути (`CACHE_DURATION_MS`), но `syncMostParameters`
върви ~23 минути. Затова: MANUFACTURERS тегли (fetch #1), PARAMETERS хваща кеша,
а PRODUCTS стартира 23 мин по-късно — кешът вече е изтекъл и тегли пак.
Два пъти по 17 MB. Не е счупено, но ако се пипа кешът, това е причината.

## Do-Not-Repeat (продуктови линкове, 2026-09-24)
- [2026-09-24] `care-tech-ui/src/redux/productSlice.js` → `fetchProducts` и `filterProducts` изброяват полетата на продукта изрично. Ново поле в backend `ProductSearchResult` трябва да се добави и там, иначе се губи тихо. Точно така `slug` е липсвал от 2025-11 до 2026-09 и е дал линкове `/product/{id}/{id}` (bug-530).
- [2026-09-24] При URL `/product/{id}/{id}` първо провери дали API отговорът съдържа `slug`, преди да пипаш базата. bug-011 е „оправил“ празни slug-ове със SQL, а истинската причина е била във фронтенда.
- [2026-09-24] Ефект в `ProductPage`, който сравнява `item` с URL параметрите, ТРЯБВА да проверява `String(item.id) === productId`. При преход продукт→продукт `item` за кратко е старият продукт и иначе ще пренасочи обратно към него.

## Key Learnings (продуктови URL-и, 2026-09-24)
- `ProductPage` зарежда продукта само по `productId`; `productSlug` е козметичен. Canonical е `${feURL}/product/${item.slug}/${item.id}`. Стари `/id/id` линкове се пренасочват към slug-адреса с `navigate(..., {replace:true})`.
- 74 slug-а в прод съдържат не-ASCII символи (`ø`, `è`, `δ`), но нито един не съдържа `/ % ? #`. React Router 7 декодира `useParams`, затова сравнението с `item.slug` работи и за тях.
- `.claude/launch.json` → конфигурация `care-tech-ui` стартира фронтенда на :3000 (сочи към прод API `https://www.caretech.bg`) за проверка в браузъра.

## User Preferences (git клонове, 2026-09-24)
- Backend (`tech-store-api`) се работи на клон `v2`. Фронтендът (`care-tech-ui`) се работи на `main` и там няма `v2`. Когато потребителят каже „push-ни в v2“, става дума за backend-а. Преди push в клон, който не съществува, питай, а не го създавай.

## Key Learnings (код/модел на продукта по доставчик, 2026-09-24)
- Всеки доставчик пази номера на производителя в различно поле. **VALI:** `reference_number` = вътрешен код на Vali (`HAMA-200696`), `model` = модел на производителя (понякога празен). **TEKRA:** `reference_number` = числов код на Tekra (`31038659`, спрян за нови продукти от 5d7371b), `model` = модел. **ASBIS:** `model` = `sku` = `asbis_code`, `reference_number` празен. **MOST:** само `sku` (PartNumber).
- Показваният код е `model || sku || referenceNumber` (`productCode` в ProductPage.jsx). Решено с потребителя 2026-09-24: клиентът иска номера, по който се разпознава продуктът, а не вътрешния код на доставчика.
- `discount` в базата може да е дробен (7 от 271 промо продукта, напр. 16.21797). Показвай го с `Math.round`. Минималната отстъпка е 5%, така че „-0%“ не се случва.

## Do-Not-Repeat (код на продукта, 2026-09-24)
- [2026-09-24] Не показвай само `referenceNumber` като код на продукта: празен е при ASBIS, MOST и новите Tekra. Не разчитай на `item.code`, такова поле няма в ProductResponseDTO.

## User Preferences (параметри и филтри, 2026-09-24)
- **Съпоставянето на параметрите от доставчиците към наши, единни филтри го прави Claude, версионирано в репото** (скриптове/seed), потребителят преглежда. Не AI в приложението, не админ ръчно като основен път. Замества решението от 2026-08-21 за AI мапинг в приложението.
- **Стойностите на ключовите филтри се нормализират изцяло**: цветове към базови семейства („Black“ → „Черен“, „Черен/Сив“ → Черен + Сив), числа през парсери с единици („68.6 cm“ → 27"). За останалите свойства само текстово сливане.

## Key Learnings (одит параметри и филтри, 2026-09-24)
- **Коренът на дублираните филтри е архитектурен:** филтрите се показват директно от суровите параметри на доставчиците, групирани по `parameters.id`. Никъде не се слива по име. Решението е собствен каноничен слой (план `~/.claude/plans/buzzing-dancing-hare.md`), в който sync-ът никога не пише.
- **Скриптове 40–46 (консолидация от 2026-08-27) НЕ са в сила в прод** — завършват със закоментиран `-- COMMIT;`. `memory.md` твърди, че са пуснати, но в прод има 102 VALI „Цвят“ реда, 4053 „Външен цвят“ не е слят, 4027… са `is_filter=true`. Таблиците `caretech_parameters`/`distributor_parameter_mapping` също не съществуват в прод.
- **На 2026-06-02 VALI sync изтри 1169 параметъра** (`sync_logs`: „Reused: 619 … Deleted: 1169“) след непълен fetch — `ValiSyncService` трие всичко, което липсва в текущия отговор, без праг; каскадата изтри опции, стойности и ръчната подредба на филтрите. Оттам са 80-те „Цвят“ реда с дата 03.06.
- **VALI дава отделен параметър за всяка категория** — „Цвят“ има 93 различни external_id. Мапинг по ID е безполезен за VALI; правилата трябва да са по нормализирано име.
- **Суровите `parameters` редове смесват доставчици:** ASBIS/TEKRA/MOST „осиновяват“ съществуващи параметри по име, затова `parameters.platform` не казва откъде идва стойността. Ползвай `products.platform`.
- **Защитата „ADMIN“ в sync-овете никога не се задейства** — Spring auditing записва имейла на админа (`d.eshanovski@gmail.com`), не „ADMIN“. Ръчен sync, пуснат от админ, също записва имейла.
- **`category_parameters.is_filter` е `NOT NULL DEFAULT TRUE`** — всяко `UPDATE ... WHERE is_filter IS NULL` е мъртъв код, а всяка нова връзка от sync става видим филтър.
- **Филтрите реално се четат от `category_parameters.is_filter`**, не от `parameters.is_filter` (глобалният флаг не се ползва от нито една заявка за клиента).
- **Измерено:** средно 23 групи филтри на видима категория, максимум 118 („Bluetooth слушалки“); 53 категории с едноименни групи. Автоматичен базов слой (нормализирани имена + junk blacklist + покритие ≥20% + ≥2 стойности) дава средно 8.5.

## Do-Not-Repeat (параметри и филтри, 2026-09-24)
- [2026-09-24] NEVER пиши SQL скрипт за прод със закоментиран `-- COMMIT;` в края. Потребителят го пуска от GUI клиент и транзакцията остава некомитната → нищо не влиза в сила, а в `memory.md` се записва като „изпълнено“. Винаги изричен `COMMIT;` и контролна заявка след него.
- [2026-09-24] NEVER „поправяй“ дублирани филтри чрез сливане/триене на редове в `parameters`/`parameter_options`. Това е суровият слой на sync-а — редовете носят ключа на доставчика и следващата нощ се пресъздават. Корекциите отиват в каноничния слой.
- [2026-09-24] NEVER позволявай sync да трие параметри само защото липсват в текущия отговор на API. Непълен fetch = масово изтриване с каскада (2026-06-02: 1169 параметъра).
- [2026-09-24] `/api/parameters/**` и `/api/products/**` бяха `permitAll()` за ВСИЧКИ HTTP методи, без `@PreAuthorize` в контролерите → анонимно триене на параметри и продукти. При ново URL правило `permitAll()` проверявай кои методи минават през него.

## Key Learnings (каноничен филтърен слой — Фаза 1, 2026-09-24)
- **Архитектура:** V38 добавя собствен слой (`filter_attributes`, `filter_values`, `filter_attribute_sources`, `filter_value_rules`, `category_filter_settings`, `category_filters`) + производни таблици (`filter_param_map`, `filter_option_map`, `product_filter_values`, `filter_unmapped_values`, `filter_rebuild_runs`). Sync-овете НИКОГА не пишат в тях. `FilterIndexService.rebuild()` ги преизчислява детерминистично след нощния cron (в края на `CronJobService.syncApis`) и през `POST /api/admin/filters/rebuild?dryRun=`.
- **Една нормализация:** SQL функцията `filter_norm(text)` (lower, всякакви интервали вкл. NBSP/zero-width → един, пунктуация в края махната). Правилата пазят нормализиран текст — CHECK го налага. Java не нормализира текст; Java прави само числовите парсери (`service/filter/parser`).
- **Правила по ИМЕ, не по ID:** VALI дава отделен parameter id за всяка категория, затова `filter_attribute_sources.name_norm` е основният ключ. `parameter_id` правилата нямат FK нарочно (пресъздаден параметър не бива да трие правило) — висящите се виждат в `GET /api/admin/filters/report`.
- **Приоритет на правилата:** MANUAL +32 > parameter_id 16 > category 8 > platform 4 > name_bg 2 > name_en 1; равенство → по-старото. `origin` е в уникалния индекс, за да може MANUAL правило да седне до AUTO правилото за същото име.
- **AUTO базов слой:** име, което в поне една категория покрива ≥30% от видимите продукти с 2–40 стойности, става AUTO свойство + AUTO правило по име (така „Цвят“ от 4-те доставчика става една група). До 12 групи на категория, хистерезис 0.8. Измерено локално върху копие на прод: 609 AUTO свойства, средно 5.5 групи, макс 12, 0 едноименни.
- **Стойности:** EXACT правило за целия текст → split по `split_pattern` → EXACT за частта → REGEX (всички съвпадения, мултистойност) → AUTO стойност (ако `auto_values`) → иначе `filter_unmapped_values`. NUMERIC свойствата минават през парсер по `filter_attributes.parser` (DIAGONAL_INCH, REFRESH_HZ, CAPACITY_GB, RESOLUTION).
- **Rebuild:** една транзакция, `pg_try_advisory_xact_lock`, temp таблици с ANALYZE, `product_filter_values` се прилага като diff. ~6–12 s локално. Ползва auto-configured `JdbcTemplate` — НЕ този на търсачката (15 s timeout).
- **Локална среда за тестове:** postgres:15 в docker на :5433 (`techstore-filter-pg`), схемата от Flyway миграциите + ръчните прод-разлики (2 резервни колони в products, `uq_product_parameters_unique`, trgm индекси), данни само от каталожните таблици (без лични данни — пълен pg_dump беше отказан). Бекендът локално: `./mvnw -o spring-boot:run` с `--spring.config.additional-location=file:/tmp/techstore-local/application-localaudit.properties` на :8081; preview_start няма достъп до `Documents/projects/cp` (macOS права).

## Do-Not-Repeat (филтри, 2026-09-24)
- [2026-09-24] NEVER прави pg_dump на цялата прод база локално — съдържа лични данни на клиенти и данни на друго приложение (face_descriptors, attendance). Тегли само изрично изброени каталожни таблици с `--data-only -t ...`.
- [2026-09-24] При всяка стъпка, която избира „по една група на категория“, дедуплицирай и между кандидатите в самия INSERT, не само срещу вече записаните редове.

## Key Learnings (курация партида 2, 2026-09-24)
- **Фийдът на MOST** (`portal.mostbg.com/api/product/xml/all?currency=EUR`, 17 MB, публичен) носи спецификациите в `<property name="…">`; `<subcategory>` съдържа сокета на платките/процесорите („AMD AM5“, „LGA1700 /Intel 12th and 13th Gen“) и типа памет/диска; `<searchStringParts><description>` дублира част от свойствата с превод. Ние четем само `<property>`.
- **Новите MOST параметри идват с АНГЛИЙСКИ имена** („Socket“, „Threads“, „Response time“) — script 13 превежда само съществуващите. Каноничният слой трябва да съпоставя и английските имена (правилата по `name_en` също работят с тегло 1).
- **Правила по име на продукта (V40, `filter_name_rules`):** regex → стойност или `use_parser` (парсерът на свойството чете името). Прилагат се САМО ако параметрите не са дали стойност за това свойство. Сокет на процесор по поколение (Ryzen 7000+ → AM5, i5-14xxx → LGA1700), сокет/форм фактор на платка по чипсет (B550M → AM4 + Micro-ATX).
- **Slug конвенция:** ръчните свойства — чист английски („chipset“, „cpu-socket“); автоматичните — „auto-…“. Така никога не се сблъскват.
- **Общи имена се ограничават до категория:** „Интерфейс“, „Форм фактор“, „Тип памет“, „Чипсет“, „Frequency“, „Графика“, „Размер на паметта“ означават различни неща в различни категории (чипсет на видео карта = „nVIDIA“, тип памет на видео карта = GDDR6). Глобални са само недвусмислените („Сокет“, „Брой ядра“, „Цвят“).
- **PostgreSQL 15 поддържа lookbehind** (`(?<!micro[ -])atx`) — прод е 15.18.
- **Покритие след партиди 1–2 (локално, с поправения MOST sync):** Монитори диагонал/резолюция 99%; Процесори серия/сокет/ядра 99%; Дънни платки сокет 100%, чипсет 97%; Памети 92–100%; SSD 96%; Видео карти модел 93%; Лаптопи процесор 94%, RAM 90%, диск 97%.

## Do-Not-Repeat (локална среда и скриптове, 2026-09-24)
- [2026-09-24] NEVER пиши `\u` escape-и през bash heredoc в SQL/Java файлове — стават невидими символи в файла. Сглобявай низа в Python с `chr(92)`.
- [2026-09-24] NEVER компилирай (`mvnw compile/test`), докато локално върви sync — devtools рестартира приложението и убива sync-а по средата.
- [2026-09-24] След промяна в кода проверявай с ПЪЛЕН рестарт на локалния бекенд; devtools понякога остава на стария клас (признак: статистиката на rebuild не се мени).

## Key Learnings (курация партида 3, 2026-09-24)
- **Цвят по име ≠ цвят по параметър.** В параметрите „Gold“/„RGB“ са цвят; в името на захранване „80+ Gold“ е ниво, в името на охладител „A-RGB“ е подсветка, в името на клавиатура „Red/Brown“ е суич. Правилата по име за цвят са отделни и тесни (55_03, т. 5).
- **„Интерфейс: USB“ при мишки/клавиатури е портът на приемника** — не значи кабелна. Кабелна само при изрично „кабел/wired/жичн“ (с `(?<!без)`).
- **Флаг-свойства с една стойност** („12V-2x6: Да“, „RGB: Да“) се показват, когато стесняват; CategoryFilterService вече не изисква ≥2 стойности за това.
- **Помощни скриптове:** `/tmp/techstore-local/rawvals.sh <кат> <slug>` — суровите стойности зад свойство в категория и към какво се съпоставят; бърз начин да се хванат грешни regex съвпадения.
- **Покритие партида 3 (локално):** Захранвания мощност 92%; Кутии дънни платки 84%; Охладители тип/сокет 91–92%; Мишки свързване/сензор 91–94%; Геймърски клавиатури тип клавиши 90%.

## Do-Not-Repeat (regex за филтри, 2026-09-24)
- [2026-09-24] NEVER пиши regex за „жичн/кабел“ без `(?<!без)` — „безжична“ съдържа „жичн“.
- [2026-09-24] NEVER копирай правилата за параметри като правила по ИМЕ без преглед — думите в името имат друг смисъл (Gold, RGB, Red).
- [2026-09-24] При Python-замяна по първо съвпадение провери, че низът не се среща и в друга секция (VALUES на стойностите съвпадна с началото на правило и го счупи).

## Key Learnings (курация партида 4, 2026-09-24)
- **Батерии (VALI):** „Химически състав“ и „Технология батерия“ са един и същ смисъл — едно свойство. Размерът е списък от синоними („LR03;AAA;24AUP;E92“); правилата са по синоними, а пред кода не може да има „/“ или цифра („1/2AA“, „4LR44“, „6LR61“ са други батерии).
- **Ресурс на консуматив:** точните числа не се сравняват между доставчиците („3150k“ = 3150 стр. при VALI, „(12K)“ = 12000 при MOST) — PAGE_YIELD връща диапазон като стойност (NUMERIC с долна граница за подредба).
- **Цвят на касета по име:** буквата след кода („CLI-8BK“, „BCI-6M“, „CRG 069H C“) се чете само в КРАЯ на името; „Color“ е цветна касета само при мастилата.
- **Покритие партида 4 (локално):** IP камери тип/резолюция/обектив 89–96%; Батерии размер 84%, химия 99%; Тонери марка 100%, ресурс 92%; Мастила марка 99%, ресурс 90%.

## Do-Not-Repeat (данни на доставчици, 2026-09-24)
- [2026-09-24] NEVER ползвай „Съвместими модели“ (VALI/MOST) като източник за марка на принтера — VALI бърка марката пред списъка („EPSON:“ пред Canon PIXMA).

## Key Learnings (курация партида 5, 2026-09-24)
- **Един параметър → едно свойство в категория** (`filter_param_map` PK = parameter, category, platform). Ако суровият параметър носи две неща („USB 3.2 Type C“ = поколение + конектор), избери едното и вземи другото от друг параметър или от името.
- **„USB памети“ (57, VALI) и „Флаш памети“ (321, MOST/ASBIS) са дублирани категории** — филтрите им са еднакви; сливането им е отделна задача.
- **„Твърди дискове“ (235) съдържа и NAS устройства (QNAP)** — капацитет от името там не се чете („TS-433-4G“ е RAM).
- **„USB 3.2“ без поколение е двусмислено** (T7 = Gen 2, флашките = Gen 1) — не се чете; „USB 3.0/3.1“ без поколение = Gen 1.
- **Покритие партида 5 (локално):** Памети за лаптоп 73–100% (беше 49% на група, 11 групи → 4); HDD 3.5" 100%; Карти памет тип/капацитет/клас 97–100%; USB памети капацитет/конектор 99–100%.

## Do-Not-Repeat (данни на доставчици, продължение)
- [2026-09-24] NEVER ползвай VALI „Тип хард диск“ — многозначен и грешен (IronWolf = „Настолен компютър“). Серията/кодът в името е надежден.

## Key Learnings (курация партида 6 — аудио, 2026-09-24)
- **VALI има имена на параметри със смесена азбука** („Mощност RMS“ с латинско M) — провери с `encode(convert_to(left(name_bg,1),'UTF8'),'hex')` и след всеки скрипт пускай проверката „източник без суров параметър“ (0 реда = всички имена съвпадат).
- **VALI пише Bluetooth и на кирилица** („Блутут“) — правило към `connection`.
- **Група, в която всички продукти имат една и съща стойност, не се показва** (не стеснява) — не я слагай в категорията („Преносими тонколони“: всички „Преносима“).
- **„Blue“ не е цвят по име в аудиото** (Blue Yeti, BLUE VO!CE).

## Key Learnings (курация партида 7 — мрежово, 2026-09-24)
- **VALI многоопционни параметри („Честота“, „Стандарти“) имат по ред за всяка опция** — правилата се прилагат на опция, затова изключващи правила („само 2.4 GHz“, „100 Mbps без гигабит“) дават грешки. Правило: стойностите да са „поддържа X“ (2.4 / 5 / 6 GHz), не „брой/само“.
- **Невалиден regex в правило проваля целия rebuild** (атомарен → старите данни остават, но нощният спира). Всеки нов 55_* скрипт завършва с guard `PERFORM '' ~* pattern` за своите правила преди COMMIT. Преди Фаза 5 (админ UI) FilterIndexService/админ API трябва да валидират израза при запис.

## Do-Not-Repeat (regex, 2026-09-24)
- [2026-09-24] След всяка ръчна редакция на regex в скрипт — пусни скрипта и rebuild; 409 „Data integrity“ от rebuild = невалиден израз (виж run.out: „invalid regular expression“).

## Key Learnings (курация партида 8 — кабели и зареждане, 2026-09-24)
- **Два параметъра за двата края на кабела („Конектор 1/2“, „Ляв/Десен конектор“) → едно свойство „Конектори“** (обединение) — купувачът търси „кабел с Lightning“.
- **Многоопционна „Макс. резолюция“ на VALI (4K и 8K като отделни опции) = „поддържана резолюция“** — наименувай групата според това, което данните реално казват.
- **Напредък 2026-09-24 (локално):** 47 от 226 категории са MANUAL = 65% от видимите продукти (4808/7448). Останалите са дълга опашка в AUTO (≤12 групи, без дубли).

## Key Learnings (курация партида 9 — аксесоари, 2026-09-24)
- **Размери „Ш x В x Д“ в имената: чети само ПЪРВАТА мярка** — пред числото не може да има „x“ или „цифра+запетая“ („750 x 300 x 3mm“ = 750; „1,200 x 550“ = 1200). Lookbehind: `(?<![x×х*] )(?<![x×х*])(?<![0-9][,.])`.
- **Единични букви за размер (S/M/L) в имената са рискови** — „s“/„m“ се срещат случайно; S само като „small“, M само в края на името.
- **Изключващи regex-и (`^(?!…)`) важат и за имена**, но закотвените към цялата стойност (`^…$`, `^[0-9]…`) — не; при копиране на правила от стойности към имена филтрирай по префикса.

## Do-Not-Repeat (редакции на скриптове, 2026-09-24 — ВТОРИ път)
- [2026-09-24] NEVER прави замяна `s[s.index(start):s.index(end)]` в SQL скрипт, когато стойностите и правилата ползват едни и същи етикети (напр. „'projector-brightness', 'До 3 000 lm'“ има и в т. 3, и в т. 4). Ползвай уникален низ (с шаблона/regex частта) и `assert s.count(x)==1`; при блокови замени провери, че start и end са в една и съща секция (`start < end` и броят на „INSERT INTO“ между тях е 0).
- [2026-09-24] Regex `(lumens?|lm)(?! ?\(eco)` backtrack-ва до „lumen“ и пропуска lookahead-а — добавяй `(?!s)`.

## Key Learnings (курация партида 10, 2026-09-24)
- **Сборните категории („Аксесоари за компютри“ 35, „Геймърски аксесоари“ 179) остават AUTO** — съдържат несвързани продукти (ASBIS: четки за зъби, отварачка за вино, филтри за прахосмукачка); смислен е само „вид продукт“.
- **MOST проекторите:** яркостта е в „Brightness“, технологията на светлината — в „Лампа“.
- **Напредък след партида 10:** виж memory.md — ~63 от 226 категории MANUAL.

## Key Learnings (курация партида 11, 2026-09-24)
- **Напредък:** 71/226 категории MANUAL = 81% от видимите продукти (6024/7448). Останалите ~155 категории имат средно под 15 продукта — курацията им е с малка отдача; AUTO режимът (≤12 групи, без дубли/боклук) е достатъчен до админ UI.
- **Копиране на правила по име между категории** (`INSERT … SELECT FROM filter_name_rules WHERE category_id = <източник>`) е начинът за преизползване — скриптът, който копира, трябва да се пуска отново, ако източникът се промени (записано в шапката).

## Key Learnings (курация партида 12, 2026-09-24)
- **Напредък:** 83/226 категории MANUAL = 84% от видимите продукти (6266/7448). Останалите: сборни категории (35, 179, 234, 47, 152), фигурки и ~140 категории под 15 продукта.
- **Стойности могат да се добавят към съществуващо свойство от по-късна партида** (напр. „M.2“ към „Размер на диска“, „Подсветка“ към „Функции“) — стойността се показва само където има продукти с нея; бележката на правилото е на новата партида.
- **Кофите на „Размер на таблета“ (партида 9) имат дупка 8–9"** — 8.68" таблети не попадат никъде; при нужда смени кофите на „До 9"“ / „9–11"“ / „12"+“ (засяга и калъфите за таблети).

## Key Learnings (след курацията, 2026-09-24)
- **AUTO лимит:** 12 групи, но 5 за категории под 10 видими продукта (`app.filters.auto.small-category-products`, `max-groups-small`). След V41: 328 AUTO групи, средно 3.8 на категория.
- **Дублирани категории с продукти в двете копия** (Процесори 3/322, Монитори 50/249, IP камери 100/964, NVR 962/240, Аксесоари 234/298, USB 57 / Флаш 321) — продуктите във второто копие не получават курираните филтри. Отделна задача (сливане/alias), не филтри.
- **Глобално IGNORE по име не пречи на курирано правило, ограничено до категория** (категорията дава +8 приоритет) — „Съвместими модели“ е IGNORE глобално, но захранва „За марка“ при калъфите.

## Key Learnings (Фаза 5 — админ UI за филтрите, 2026-09-24)
- **Админ API:** `/api/admin/filters/**` (FilterConfigController) + съществуващите rebuild/report/visibility (FilterAdminController). Фронтенд: `care-tech-ui/src/pages/admin/Filters/` (табове Категории, Свойства и стойности, Несъпоставени, Параметри на доставчиците), маршрут `/admin/dashboard/filters`; старата страница е „Параметри (доставчици)“ до Фаза 6.
- **Правилата/стойностите влизат след rebuild; групите на категорията (режим, подредба, видимост, добави/махни) — веднага** (evict на кеша).
- **ВНИМАНИЕ: повторно пускане на 55_* скрипт трие MANUAL `category_filters` на своите категории** — изтрива и админ промените по групите на тези категории. След деплоя админ UI-то е собственик; скриптовете 55_* са еднократни.
- **Локален вход като админ в браузъра без парола:** redux-persist `persist:root` → `auth` = {user{role:'ADMIN'}, token} от `/tmp/techstore-local/admin.token` (локален тестов акаунт admin@local.test).
- **`max-width` на `<td>` се игнорира при auto table layout** — ограничавай с вътрешен `<div className="max-w-xs break-all">`; дясната колона на grid трябва `min-w-0`.


## Key Learnings (Фаза 6 — почистване на стария слой за филтри, 2026-09-25)
- **Филтрите в магазина идват САМО от каноничния слой:** `POST /api/products/categories/{id}/filters`, търсене с `attributeFilters`, стари `param_` линкове през `…/filters/translate-legacy`. Старите `/products/parameters/category`, `/categories/{id}/parameters`, `/filter-facets`, `filter-activate`, `PATCH …/filter`, `…/reorder` и полето `filters` в търсенето са махнати (клон `v2-phase6-cleanup`).
- **`is_filter` / `filter_order` / `sort_order` остават в базата** (sync-овете ги пишат, `sort_order` подрежда спецификациите), но нищо в магазина не чете `is_filter`.
- **Старата страница „Параметри на доставчиците“ (`src/pages/admin/Params/`) е махната** заедно с DELETE `/api/parameters/{id}` и `/api/parameters/{p}/options/{o}` (само тя ги ползваше). Остават GET `/api/parameters/category/{id}`, POST и PUT `/api/parameters` — за ProductForm и ParameterSelector; `ParamsModal` и `CreateParameterForm` са преместени в `src/pages/admin/Products/`. Суровият слой се пипа само от sync-а; филтрите се настройват в „Филтри“.
- **Защита на остарял SQL скрипт: `BEGIN;` + `DO $$ BEGIN RAISE EXCEPTION … END $$;` най-отгоре** — проваля транзакцията, така че и psql без ON_ERROR_STOP, и собствен `COMMIT;` по-надолу (той става ROLLBACK) не записват нищо. Само коментар не е защита.

## User Preferences (2026-09-25)
- Деплоят е ръчен (няма CI/CD) — push не деплойва; не е нужно да държим отделни клонове само заради реда на деплой.

## Decision Log (2026-09-25)
- **[2026-09-25 по-късно] Потребителят няма автоматичен деплой → v2 е fast-forward-нат до `v2-phase6-cleanup`; бекенд v2 и фронтенд main трябва да се деплойнат ЗАЕДНО.** Първоначално: **Фаза 6 е в отделен клон `v2-phase6-cleanup`** — v2 (фази 0–5) може да се деплойне без нея; старите endpoint-и падат чак след като новият фронтенд е на живо (иначе старият фронтенд губи филтрите). Фронтенд промените на Фаза 6 са само мъртъв код → безопасни в main в произволен ред.

## Key Learnings (TEKRA фийд, 2026-09-25)
- **Фийдът с продукти на TEKRA (`action=browse&feed=1`) връща празен `<items/>` от IP, различно от прод сървъра** (категориите `action=categories` работят). Суровият XML не може да се види локално — измервай през `sync_logs` в прод (само четене).
- **Свойствата във фийда са тагове `prop_<ключ>`** (затова `merna`/`model` в systemFields не ги спират — проверката е преди махането на префикса). `prop_merna` = „бр.“ за всички продукти (единица за продажба, не мярка) → V39 я скрива; не е проблем.
- **TEKRA_PARAMETERS в `sync_logs` пише броячи на фийда:** „Feed: N repeated tags, N multi-value properties, N values over 2000 chars skipped“.
- **[ПОПРАВЕНО 2026-09-25, bug-564 — пагинация, не е деплойнато] Беше: `getProductsRaw` взима само page=1, perPage=100 на категория.** TEKRA: Видеонаблюдение 695 продукта (IP системи 391), ние обработваме ~358 SKU на прогон. Slug-овете на TEKRA не са уникални („aksesoari“ × много), `mrezhovo` е под-под-категория с count 0 (Мрежово оборудване = `mrezhovo-oborudvane` → `aktivno` 210 / `pasivno` 352). Пагинацията не може да се провери локално (IP).
- **TEKRA лимитът се проявява и като HTTP 200 с празен `<items/>`, не само като 429** (наблюдавано 2026-09-25: първо празни отговори, после 429 „Too many requests“ след ~6 заявки за ~50 мин). Затова syncTekraProducts пропуска mark-unseen при изключение в категория ИЛИ ако би свалил >25% от наличните TEKRA продукти наведнъж (MAX_UNSEEN_SHARE).
- **Nightly TEKRA:** параметрите теглят всички категории (без пауза между категории), продуктите ползват кеша (30 мин) и спят 30 s между категориите.

## Key Learnings (ASBIS фийд, 2026-09-25)
- **ASBIS `ProductList.xml` се тегли локално** (GET с USERNAME/PASSWORD от .env, редовете са с „:“ вместо „=“; ~58 MB, 8283 продукта, 275 250 атрибута). Мерено: 972 стойности >200 знака, 0 >2000.
- **ASBIS праща списъци в една стойност, слепени с `<br/>`, `<br>` и счупеното `</br>`, понякога с `<b>`, `<a href>` (EPREL), `<img>`.** Спецификациите ги делят на редове в DisplaySpecificationService.displayLines; филтрите не ги делят (правилата с шаблони намират няколко стойности в целия текст).
- **[ПОПРАВЕНО 2026-09-25, bug-566] Повторени имена на атрибути в ASBIS** (сега attrlist = Map<име, List<стойност>>, по една опция на стойност) — `extractAttrList` е HashMap → последната печели. 1170 случая в 669 продукта: Височина/Дълбочина (продукт и опаковка), LAN (259: „Gigabit Ethernet / 10G Ethernet“ + „1 (RJ-45)“), WAN, Ширина, USB тип C.
- **[2026-09-25, ~50 мин след 429] Фийдът с продукти на TEKRA пак връща HTTP 200 + празен `<items/>` от тази машина** (страници 1 и 2 на ip-sistemi, при 391 продукта по categories API). Или е IP-ограничен, или блокировката е дълга — семантиката на `page` НЕ е проверена на живо. Проверка само през sync_logs (TEKRA_PRODUCTS „Fetched: {…}“) след деплоя.
- **Прод API (www.caretech.bg/api) отрязва curl по user-agent (празен отговор / „Empty reply“)** — за ръчни проверки ползвай браузърен UA.

## Key Learnings (партида 13 — „Несъпоставени“, 2026-09-25)
- **Отчетът „Несъпоставени“ се пълни само от курирани свойства (auto_values = false) и от числови парсери, които не разпознават текста.** Шаблоните важат само за ENUM; числовите приемат само точни правила (raw_norm) — те се проверяват преди парсера.
- **Всички съвпадащи шаблони дават стойност** (не само първият) — правило „без стойност“ никога не маха съществуващо съпоставяне, но шаблон без котва може да сложи втора стойност („cover“ → Гръб и на „flip cover“). Котви (^…$) за точечни поправки.
- **Точно правило засенчва шаблоните** — затова правилата „без стойност“ по отчета се генерират само за текстове, които не са хванати от шаблон със стойност.
- **Правилата, генерирани от текущия отчет, трябва да оцеляват при повторно пускане:** след rebuild текстовете им вече не са в отчета. Отделна бележка ('55 партида 13 — по отчета'), не се трият.
- **Прод API и локален тест:** локален админ токен се подписва с JWT_SECRET от application-localaudit.properties (HS384, claims role/userId/email/sub) — изтича след 24 ч.

## Key Learnings (партида 14 и почистване, 2026-09-25)
- **„Дублираните“ категории в Видеонаблюдение НЕ са дубликати:** 249 (Видеонаблюдение › Монитори) / 50, 964 (NDAA › IP камери) / 100, 240 (NDAA › NVR) / 962, 243/239 — нарочни клонове. Не се сливат; получават курацията на двойника (партида 14).
- **Източниците и правилата по име на курацията са вързани с категория** — нова категория със същия вид продукти иска копие на filter_attribute_sources + filter_name_rules + category_filters (шаблон в 55_14, секция 1). Автоматичните свойства (auto-*) в MANUAL категория се копират отделно (join по auto_key).
- **Истинският проблем с категориите: ~100 видими продукта в 32 СКРИТИ категории** (почти всички ASBIS корени: Мултимедиен хардуер 30, Аксесоари 298, Air Fryers, Процесори 322…). ASBIS sync запазва старата категория на съществуващ продукт, ако новата не е видима → остават там завинаги. Чака решение на потребителя.
- **Най-бавната стъпка на rebuild е правилата по име** (INSERT INTO tmp_pfv … JOIN tmp_names ON r.category_id IS NULL OR …): ~13 s локално от ~34 s; OR в join-а спира hash join, а tmp_names е всички 26k неизтрити продукта. Оптимизация, ако rebuild мине 60 s: UNION ALL вместо OR и/или анти-join преди regex.
- **V42 трие is_filter/filter_order** — тестовият профил е H2 + ddl-auto (Flyway изключен), миграцията се проверява само срещу локалната PostgreSQL.
- **Решение от 2026-09-18 (скрипт 50): дребната бяла техника НЕ се предлага; за нея няма и няма да има видима категория.** Скритите ASBIS корени Air Fryers, Прахосмукачка прътова, Electric Fans, Иригатори, Massage Guns, Dehumidifiers, Lint Removers, Compressor, торбички за вакуумиране са останали извън поддървото „Дребни домакински уреди“ (454) и продуктите им са още видими (search). Смарт дом (Aqara, Realme крушка/кантар, Xiaomi брави, Ubiquiti Access) също няма категория.
- **Продукт, преместен със скрипт преди V37 (2026-09-23), може да е върнат от MOST/TEKRA** — пример: 4 подаръка на MSI (скрипт 51 → върнати в 443). При всяко ръчно местене по скрипт: manually_categorized = TRUE.
- **category_id_pre_phase5 не е надежден за откат на по-късни скриптове** (пази първата стойност) — откатът се пише по изричен списък (id, от, към).
- **[2026-09-25] Потребителят потвърди: дребната техника И смарт домът без категория се спират от продажба (скрипт 57).** Отворено: 76 продукта в 500 „Смарт устройства“ (вкл. камери Aqara) и 17 в 548 „Контрол на достъп“ са невидими сега, но могат да изплуват при наличност — не са скрити с manually_hidden.
