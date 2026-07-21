# Cerebrum

> OpenWolf's learning memory. Updated automatically as the AI learns from interactions.
> Do not edit manually unless correcting an error.
> Last updated: 2026-05-15

## User Preferences

- **EURO_RATE е винаги 1.95583** (официален фиксиран курс лев/евро). Никога не използвай 1.96 или друга стойност.
- **Цените се изписват винаги първо в Евро (€), след това в лева (лв.)** — напр. "199.99 €  /  391.12 лв.". Лева цената е обвита в отделен `<span>` или компонент за лесно премахване в бъдеще. Никога не показвай само лева.

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

- [2026-07-20] NEVER leave admin query methods in `TbiLeasingService` (or any service with `open-in-view=false`) without `@Transactional(readOnly = true)` when the entity has LAZY associations. Without it, the Hibernate session closes after the repository call and `Page.map()` triggers `LazyInitializationException` when accessing `e.getOrder()`. Fix: add `@Transactional(readOnly = true)` to `getAllApplications`, `getApplicationsByStatus`, `getApplicationById`.

- [2026-05-20] NEVER use `restTemplate.getForObject(url, String.class)` за Asbis XML файлове — Spring декодира като ISO-8859-1 и разваля UTF-8 Кирилица. Винаги използвай `byte[].class` + `new String(bytes, StandardCharsets.UTF_8)`.
- [2026-05-22] NEVER replace a Hibernate-managed collection that has `orphanRemoval=true` (e.g. `product.setProductParameters(newSet)`). This silently marks the transaction rollback-only at next flush. Always mutate the existing collection: `existing.clear(); existing.addAll(newItems)`. If collection is null, first do `product.setX(new HashSet<>())`. Applies to ALL entities with orphanRemoval.
- [2026-05-20] NEVER разчитай на Lombok `@Builder.Default` за Jackson десериализация — стойностите важат само за builder pattern, не за `@NoArgsConstructor`. Добавяй null/zero guards в service validate методите.

<!-- Mistakes made and corrected. Each entry prevents the same mistake recurring. -->
<!-- Format: [YYYY-MM-DD] Description of what went wrong and what to do instead. -->

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
