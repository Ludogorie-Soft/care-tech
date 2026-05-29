# Cerebrum

> OpenWolf's learning memory. Updated automatically as the AI learns from interactions.
> Do not edit manually unless correcting an error.
> Last updated: 2026-05-15

## User Preferences

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

## Do-Not-Repeat (Tests)

- [2026-05-29] NEVER modify `application.yml` to fix test failures — create `src/test/resources/application.properties` (for all tests) or `application-test.properties` (for @ActiveProfiles("test")) instead.
- [2026-05-29] `@SpringBootTest` context-load test fails locally because `${POSTGRES_DB}` (and other env vars) are unresolved. Fix: @ActiveProfiles("test") + application-test.properties with H2 datasource + spring.flyway.enabled=false.

## Do-Not-Repeat (Security)

- [2026-05-29] NEVER use `hasRole('ADMIN')` alone on controllers that should be accessible to SUPER_ADMIN. `getAuthorities()` returns exactly one role string (`ROLE_ADMIN` or `ROLE_SUPER_ADMIN`) — `hasRole('ADMIN')` will silently block SUPER_ADMIN. Always use `hasAnyRole('ADMIN', 'SUPER_ADMIN')`.
- [2026-05-29] NEVER add `@CrossOrigin(origins = "*")` to individual controllers — it overrides the global CORS config and opens all origins for those endpoints. Use the global config only.
- [2026-05-29] When adding an IDOR fix to a TBI/order endpoint, check whether the frontend actually calls that exact path. `fetchLeasingById` in leasingSlice calls `admin/leasing/{id}` (AdminController), NOT `tbi/application/{id}` (TbiLeasingController) — these are two different endpoints.
- [2026-05-29] Defense in depth: every sensitive endpoint should have BOTH URL-level (`SecurityConfig`) AND method-level (`@PreAuthorize`) guards. URL rules can be accidentally removed or reordered.

- [2026-05-29] **Test infrastructure:** `src/test/resources/application.properties` overrides `spring.profiles.active` and `logging.file.name` for ALL tests (needed because @WebMvcTest also loads application.yml). `src/test/resources/application-test.properties` provides full H2 + disabled Flyway config for @SpringBootTest. `TechStoreApiApplicationTests` uses `@ActiveProfiles("test")`. H2 added as test-scoped dependency in pom.xml.
- [2026-05-29] **UnnecessaryStubbing pattern:** Mockito strict mode throws if a stubbed method is never called. When an entity has `order = null`, `transitionOrderStatus` returns early — `orderRepository.findById()` is never invoked. Don't stub it in those tests.
- [2026-05-29] **@WebMvcTest + @Import(SecurityConfig):** Needs `@MockBean` for `UserRepository` and `JwtUtil` (SecurityConfig constructor deps). Also needs `@MockBean SecurityHelper` if the controller uses it. Test properties must supply `spring.cors.allowedOrigins`.

## Decision Log

<!-- Significant technical decisions with rationale. Why X was chosen over Y. -->

- [2026-05-18] V13 (set Vali is_filter from scraped data) moved to `scripts/` folder. Reason: on a fresh server the DB is empty at startup — Flyway would run V13 before any Vali sync data exists, so it would match 0 rows. The script must be run manually after `POST /api/sync/vali/parameters`.
- [2026-05-29] TBI leasing uses Option B architecture: create `LEASING_PENDING` order at application time (not after TBI approval). This avoids the problem of admin not knowing the customer's shipping address after approval — address is collected before the TBI iframe opens.
