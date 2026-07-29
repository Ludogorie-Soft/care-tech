# anatomy.md

> Auto-maintained by OpenWolf. Last scanned: 2026-07-29T06:25:07.792Z
> Files: 596 tracked | Anatomy hits: 0 | Misses: 0

## ../../../../.claude/projects/-Users-user-Documents-projects-cp-tech-store-api/memory/

- `asbis_price_field.md` (~203 tok)
- `euro_rate_convention.md` — Declares EURO_RATE (~194 tok)
- `MEMORY.md` — Memory Index (~339 tok)
- `personal_offer_flow.md` — Declares stored (~373 tok)
- `project_most_api.md` (~136 tok)
- `session_2026_06_03.md` — Backend (`tech-store-api`) (~574 tok)
- `speedy_autocomplete_pattern.md` (~297 tok)
- `tbi_leasing_integration.md` — Status: Backend + Frontend COMPLETE (2026-06-03 — updated credentials + modal fix) (~881 tok)
- `user_role_and_workflow.md` (~279 tok)

## ../../care-tech-ui/public/

- `index.html` — Care Tech – SEO мета, GA, FB Pixel, Pazaruvaj backlink (статичен, crawler-visible) + тъмен bottom bar с copyright (~1900 tok)
- `leasing-complete.html` — TBI Complete (~184 tok)

## ../../care-tech-ui/src/

- `App.js` — Declares ScrollToTop (~3149 tok)
- `index.css` — Styles: 39 rules (~2126 tok)

## ../../care-tech-ui/src/api/

- `axiosConfigs.js` — Exports setupRequestInterceptor, setAuthToken (~230 tok)

## ../../care-tech-ui/src/components/

- `Breadcrumbs.jsx` — Breadcrumbs (~504 tok)
- `DateTimePicker.jsx` — MONTHS (~2516 tok)
- `Footer.jsx` — categories (~2764 tok)
- `TbiCheckoutModal.jsx` — TBI brand color (~5322 tok)

## ../../care-tech-ui/src/components/compare/

- `CompareTray.jsx` — CompareTray (~1049 tok)

## ../../care-tech-ui/src/components/home/

- `AdImageSectionTwo.jsx` — slides (~993 tok)
- `OurPartnersSection.jsx` — PrevArrow (~1621 tok)
- `PromoProductsSection.jsx` — PromoProductsSection (~2722 tok)
- `ReviewsSlider.jsx` — PrevArrow (~1554 tok)

## ../../care-tech-ui/src/components/navbar/

- `AuthDropDown.jsx` — AuthDropDown — renders form (~3716 tok)
- `NavBar.jsx` — NavBar (~4457 tok)

## ../../care-tech-ui/src/components/products/

- `CompareButton.jsx` — Reusable compare toggle button. (~695 tok)
- `ImageDisplaying.jsx` — NextArrow (~3614 tok)
- `ProductCard.jsx` — ProductCard (~3407 tok)

## ../../care-tech-ui/src/pages/

- `Blog.jsx` — formatDate (~2633 tok)
- `BlogPostPage.jsx` — formatDate (~1953 tok)
- `Cart.jsx` — EURO_RATE (~16680 tok)
- `Category.jsx` — Category (~6600 tok)
- `ComingSoonPage.jsx` — ComingSoonPage (~385 tok)
- `ComparePage.jsx` — EURO_RATE — renders table (~3958 tok)
- `Contact.jsx` — Contact — renders form (~2804 tok)
- `Cookies.jsx` — Responsive Cookies / Cookie Policy component (~3474 tok)
- `ForUs.jsx` — ForUs (~2823 tok)
- `OurClients.jsx` — clients (~2724 tok)
- `Policy.jsx` — sections (~4961 tok)
- `ProductPage.jsx` — EURO_RATE (~10324 tok)

## ../../care-tech-ui/src/pages/admin/

- `Dashboard.jsx` — Dashboard (~622 tok)

## ../../care-tech-ui/src/pages/admin/Blog/

- `BlogCategoryManager.jsx` — CategoryModal — renders form (~4552 tok)
- `BlogLayout.jsx` — formatDate — renders table (~5184 tok)
- `BlogPostModal.jsx` — TagPicker — renders form (~7216 tok)
- `BlogPostPreviewModal.jsx` — formatDate (~1690 tok)
- `BlogTagManager.jsx` — TagModal — renders form (~3398 tok)

## ../../care-tech-ui/src/pages/admin/Customers/

- `CustomersLayout.jsx` — CustomersLayout.jsx (~4646 tok)
- `SendOfferModal.jsx` — EURO_RATE — renders form (~4082 tok)
- `UserCartModal.jsx` — EURO_RATE (~1466 tok)

## ../../care-tech-ui/src/pages/admin/Dashboard/

- `DashboardLayout.jsx` — convertStatsToMetrics (~2511 tok)
- `Sidebar.jsx` — Sidebar (~1269 tok)

## ../../care-tech-ui/src/pages/admin/Leasing/

- `LeasingLayout.jsx` — STATUS_CONFIG (~4989 tok)

## ../../care-tech-ui/src/pages/admin/Offers/

- `ConvertOfferToOrderModal.jsx` — EURO_RATE — renders form (~4254 tok)
- `OffersLayout.jsx` — EURO_RATE — renders table (~4796 tok)

## ../../care-tech-ui/src/pages/admin/Orders/

- `EditOrderModal.jsx` — TABS — renders form (~6619 tok)
- `OrderDetailPage.jsx` — OrderDetailPage (~5639 tok)
- `useOrderUtils.js` — Exports useOrderUtils (~802 tok)

## ../../care-tech-ui/src/pages/admin/Pazaruvaj/

- `PazaruvajLayout.jsx` — TYPES (~6942 tok)

## ../../care-tech-ui/src/pages/admin/Products/

- `ProductForm.jsx` — SearchSelect (~10755 tok)

## ../../care-tech-ui/src/pages/admin/Reviews/

- `ReviewsLayout.jsx` — STATUS_CONFIG — renders table (~2891 tok)

## ../../care-tech-ui/src/pages/admin/Sync/

- `SyncLogsLayout.jsx` — STATUS_LABELS — renders table (~2882 tok)

## ../../care-tech-ui/src/pages/profile/

- `AcceptOfferModal.jsx` — EURO_RATE — renders form (~6210 tok)
- `MyOffers.jsx` — EURO_RATE (~2344 tok)
- `OrderDetails.jsx` — OrderDetails (~3809 tok)
- `ProfileLayout.jsx` — links (~1930 tok)
- `WarrantyCards.jsx` — WarrantyCards (~886 tok)

## ../../care-tech-ui/src/redux/

- `blogSlice.js` — ── Public thunks ────────────────────────────────────────────────────────────── (~4239 tok)
- `compareSlice.js` — Exports selectCompareItems, selectCompareCount, selectIsInCompare (~386 tok)
- `leasingSlice.js` — 4 async thunks: fetchLeasingApplications, fetchLeasingById, fetchLeasingStatistics, registerLeasingApplication. registerError stores true (flag only, not backend payload). State: registerStatus, registeredUrl, registeredApplicationId. (~1100 tok)
- `offersSlice.js` — API routes: GET, PUT, POST (10 endpoints) (~2807 tok)
- `orderSlice.js` — orderSlice.js (~3037 tok)
- `paramSlice.js` — API routes: GET, POST, PUT, PATCH, DELETE (15 endpoints) (~5217 tok)
- `productSlice.js` — API routes: GET, POST, PUT, DELETE (12 endpoints) (~6019 tok)
- `reviewsSlice.js` — API routes: GET, POST, PUT, DELETE (7 endpoints) (~1554 tok)
- `store.js` — Exports store, persistor (~702 tok)

## ../../care-tech-ui/src/utils/

- `loginRedirect.js` — Exports setLoginRedirect, consumeLoginRedirect (~73 tok)
- `partners.js` — Exports partners (~425 tok)
- `slugify.js` — Exports slugify (~217 tok)
- `tokenRefresh.js` — Exports setupTokenRefresh (~568 tok)
- `utils.js` — Exports SITE_URL, specificationsMap, ORDER_FORMS, getStatusBadge + 4 more (~490 tok)

## ./

- `.dockerignore` — Docker ignore rules (~97 tok)
- `.DS_Store` (~2186 tok)
- `.gitattributes` — Git attributes (~11 tok)
- `.gitignore` — Git ignore rules (~124 tok)
- `BLOG_PLAN.md` — План: Блог функционалност (~1556 tok)
- `categories_202605130941.sql` (~14427 tok)
- `CLAUDE.md` — OpenWolf (~57 tok)
- `COMPARE_PLAN.md` — План: Функционалност за Сравнение на Продукти (~1975 tok)
- `database_backup_script.txt` (~121 tok)
- `docker-compose.dev.yml` — Docker Compose: 4 services (~361 tok)
- `docker-compose.yml` — Docker Compose services (~497 tok)
- `Dockerfile` — Docker container definition (~324 tok)
- `How_To_Run_In_AWS.md` — Tech Store API (~536 tok)
- `init-databases.sh` (~35 tok)
- `mvnw` — or more contributor license agreements.  See the NOTICE file (~3144 tok)
- `mvnw.cmd` — Declares Directory (~2262 tok)
- `nginx.conf` — Nginx configuration (~575 tok)
- `nginx.prod.conf` — Production Nginx config — /etc/nginx/conf.d/caretech.bg.conf (~2171 tok)
- `OFFER_TO_ORDER_PLAN.md` — План: Оферти с Продуктова Селекция, Per-Item Отстъпки и Конвертиране в Поръчка (~3263 tok)
- `ORDER_EDIT_PLAN.md` — План: Редактиране на поръчки от администратор (~577 tok)
- `parameters_202605130941.sql` (~43615 tok)
- `pom.xml` (~1688 tok)
- `README.md` — Project documentation (~1777 tok)
- `sync-diagnostics.sql` — ============================================================= (~1278 tok)

## .claude/

- `settings.json` (~441 tok)
- `settings.local.json` (~96 tok)

## .claude/rules/

- `openwolf.md` (~313 tok)

## .mvn/wrapper/

- `maven-wrapper.properties` (~40 tok)

## New files added 2026-06-04

- `../../care-tech-ui/src/pages/admin/Customers/SendOfferModal.jsx` — Admin modal: compose and send personal offer with prefilled cart items (~500 tok)
- `../../care-tech-ui/src/pages/admin/Customers/UserCartModal.jsx` — Admin modal: shows user's cart, "Изпрати оферта" button (~300 tok)
- `../../care-tech-ui/src/pages/profile/MyOffers.jsx` — User profile page: list offers, accept/reject, mark as read (~500 tok)
- `../../care-tech-ui/src/redux/offersSlice.js` — Redux slice: admin (fetchUserCart, fetchUserOffers, sendPersonalOffer) + user (fetchMyOffers, markOfferRead, updateOfferStatus) (~600 tok)
- `../../care-tech-ui/src/utils/loginRedirect.js` — Utility: setLoginRedirect(path) / consumeLoginRedirect() — stores post-login redirect in sessionStorage (~50 tok)
- `src/main/java/com/techstore/dto/request/PersonalOfferCreateDto.java` — Request DTO: title, message, items[], discountPercent, expiresAt (~150 tok)
- `src/main/java/com/techstore/dto/response/PersonalOfferResponseDto.java` — Response DTO with static from() factory, expired flag (~200 tok)
- `src/main/java/com/techstore/entity/PersonalOffer.java` — Entity: PersonalOffer with inner OfferItem @Data class and OfferStatus enum (~250 tok)
- `src/main/java/com/techstore/repository/PersonalOfferRepository.java` — Repository: PersonalOfferRepository (~150 tok)
- `src/main/java/com/techstore/service/PersonalOfferService.java` — Service: createAndSend, getOffersForUser, getCurrentUserOffers, markAsRead, updateStatus (~400 tok)
- `src/main/resources/db/migration/V21__add_personal_offers.sql` — Creates personal_offers table with JSONB offer_items, status ENUM, user FK (~80 tok)
- `src/main/resources/templates/email/personal-offer.html` — Thymeleaf email template for personal offers (~500 tok)

## lucene-indexes/Manufacturer/

- `_4x.cfe` (~111 tok)
- `_4x.cfs` (~594 tok)
- `_4x.si` (~88 tok)
- `_4y.cfe` (~111 tok)
- `_4y.cfs` (~538 tok)
- `_4y.si` (~88 tok)
- `_50.cfe` (~111 tok)
- `_50.cfs` (~551 tok)
- `_50.si` (~88 tok)
- `_52.cfe` (~111 tok)
- `_52.cfs` (~586 tok)
- `_52.si` (~88 tok)
- `_53.cfe` (~111 tok)
- `_53.cfs` (~526 tok)
- `_53.si` (~88 tok)
- `_54.cfe` (~111 tok)
- `_54.cfs` (~578 tok)
- `_54.si` (~88 tok)
- `_56.cfe` (~111 tok)
- `_56.cfs` (~556 tok)
- `_56.si` (~88 tok)
- `_57_Lucene90_0.dvd` (~1885 tok)
- `_57_Lucene90_0.dvm` (~46 tok)
- `_57_Lucene90_0.pos` (~745 tok)
- `_57_Lucene90_0.tim` (~670 tok)
- `_57_Lucene90_0.tip` (~22 tok)
- `_57_Lucene90_0.tmd` (~84 tok)
- `_57.fdm` (~43 tok)
- `_57.fdt` (~54 tok)
- `_57.fdx` (~24 tok)
- `_57.fnm` (~126 tok)
- `_57.nvd` (~600 tok)
- `_57.nvm` (~28 tok)
- `_57.si` (~143 tok)
- `segments_z` (~197 tok)

## lucene-indexes/Parameter/

- `_2s_Lucene90_0.dvm` (~63 tok)
- `_2s_Lucene90_0.pos` (~99206 tok)
- `_2s_Lucene90_0.tip` (~7586 tok)
- `_2s_Lucene90_0.tmd` (~115 tok)
- `_2s.fdm` (~43 tok)
- `_2s.fdt` (~748 tok)
- `_2s.fdx` (~131 tok)
- `_2s.fnm` (~151 tok)
- `_2s.nvd` (~114029 tok)
- `_2s.nvm` (~38 tok)
- `_2s.si` (~143 tok)
- `_2u_Lucene90_0.dvm` (~36 tok)
- `_2u_Lucene90_0.pos` (~97092 tok)
- `_2u_Lucene90_0.tim` (~262310 tok)
- `_2u_Lucene90_0.tip` (~7568 tok)
- `_2u_Lucene90_0.tmd` (~119 tok)
- `_2u.fdm` (~43 tok)
- `_2u.fdt` (~657 tok)
- `_2u.fdx` (~118 tok)
- `_2u.fnm` (~152 tok)
- `_2u.nvd` (~99964 tok)
- `_2u.nvm` (~38 tok)
- `_2u.si` (~143 tok)
- `_34_Lucene90_0.dvd` (~125368 tok)
- `_34_Lucene90_0.dvm` (~36 tok)
- `_34_Lucene90_0.pos` (~24021 tok)
- `_34_Lucene90_0.tim` (~110395 tok)
- `_34_Lucene90_0.tip` (~2811 tok)
- `_34_Lucene90_0.tmd` (~114 tok)
- `_34.fdm` (~42 tok)
- `_34.fdt` (~292 tok)
- `_34.fdx` (~60 tok)
- `_34.fnm` (~151 tok)
- `_34.nvd` (~41799 tok)
- `_34.nvm` (~37 tok)
- `_34.si` (~142 tok)
- `categories_202607201226.sql` — PostgreSQL дамп на таблица categories — 511 реда INSERT без explicit id; колони: tekra_id,tekra_slug,asbis_id,asbis_code,external_id,name_en,name_bg,slug,... (~49000 tok)
- `scripts/mismatched_products_analysis.md` — Анализ на грешно категоризирани продукти: 487 несъответствия в 16 групи + карта category_id→name + SQL за корекции (~8000 tok)
- `segments_j` (~85 tok)

## scripts/

- `1_rename_categories.sql` — Преименуване на Vali/Tekra категории по external_id (~2744 tok)
- `10_fix_sort_order_after_reorganization.sql` — Поправя sort_order след скрипт 7: Asbis subcategories → 100+, Vali subcategories → 1-21, top-level re-run (~600 tok)
- `11_fix_product_slugs.sql` — Генерира slug за продукти с empty/null slug от name_en/name_bg; Cyrillic product-id fallback опционален (~400 tok)
- `12_deduplicate_products_by_sku.sql` — ============================================================================= (~777 tok)
- `14_delete_unwanted_categories.sql` — ============================================================ (~2082 tok)
- `15_hide_zero_price_products.sql` — ============================================================ (~315 tok)
- `16_fix_products_in_hidden_categories.sql` — ============================================================ (~780 tok)
- `17_fix_all_hidden_category_products.sql` — ============================================================ (~2213 tok)
- `18_fix_laptop_category_accessories.sql` — ============================================================ (~516 tok)
- `19_fix_laptop_category_accessories_v2.sql` — ============================================================ (~391 tok)
- `2_rebrand_vali_to_caretech.sql` — Ребрандиране: VALI COMPUTERS → CARETECH (~536 tok)
- `20_fix_laptop_category_covers.sql` — ============================================================ (~193 tok)
- `21_fix_laptop_category_remaining.sql` — ============================================================ (~462 tok)
- `22_hide_products_without_images.sql` — ============================================================ (~185 tok)
- `23_add_gaming_periphery_aliases_in_komp_periferiya.sql` — ============================================================================= (~1015 tok)
- `24_fix_miscategorized_products.sql` — ============================================================ (~3552 tok)
- `25_fix_remaining_cat37_mismatches.sql` — ============================================================ (~435 tok)
- `26_fix_cat37_comprehensive.sql` — ============================================================ (~1591 tok)
- `3_reorder_top_categories.sql` — Пренарежда главните категории (sort_order) вкл. 3-те нови Asbis roots (~1086 tok)
- `4_reorder_subcategories.sql` — Пренарежда подкатегориите (sort_order) (~4734 tok)
- `5_vali_filters_by_option_count.sql` — Вмъква Vali filter данни за 206 категории (~8000 tok)
- `6_fix_asbis_category_names_bg.sql` — Превежда English Asbis category names → Bulgarian (~3500 tok)
- `7_reorganize_asbis_categories.sql` — Разпуска 43 Asbis root категории под Vali дървото; "Дребни домакински уреди" остава видим root (~6000 tok)
- `7b_fix_asbis_duplicate_subcategories.sql` — Merge Asbis дублики (ед.ч.) → Vali канонични (мн.ч.): Видео карта→Видео карти, Памет→Памети и др. Скрива Asbis дублика след merge. (~500 tok)
- `8_asbis_filters.sql` — Auto-select Asbis is_filter=true по option_count 2-50, с blacklist на packaging/global параметри (~600 tok)
- `9_hide_empty_categories.sql` — Итеративно скрива категории без видими продукти (show=false); работи рекурсивно нагоре (leaf → parent) (~500 tok)
- `mismatched_products_analysis.md` — Анализ на грешно категоризирани продукти (~13125 tok)

## src/

- `.DS_Store` (~1640 tok)

## src/main/

- `.DS_Store` (~1640 tok)

## src/main/java/com/techstore/

- `TechStoreApiApplication.java` — TechStoreApiApplication: main (~214 tok)

## src/main/java/com/techstore/config/

- `AppProperties.java` — @Configuration (~237 tok)
- `AsyncConfig.java` — Configuration: AsyncConfig (~624 tok)
- `AuditConfig.java` — Configuration: AuditConfig (~689 tok)
- `CacheConfig.java` — Thread-safe cache for Asbis products (~493 tok)
- `CacheInitializer.java` — Component: CacheInitializer (~234 tok)
- `DatabaseConfig.java` — @Configuration (~428 tok)
- `DebugController.java` — RestController: DebugController (5 endpoints). Class-level @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')"). (~4699 tok)
- `JacksonConfig.java` — Configuration: JacksonConfig (~470 tok)
- `JwtAuthenticationFilter.java` — Component: JwtAuthenticationFilter (~1305 tok)
- `OpenApiConfig.java` — Configuration: OpenApiConfig (~712 tok)
- `RestTemplateConfig.java` — Configuration: RestTemplateConfig (~765 tok)
- `S3Config.java` — Configuration: S3Config (~289 tok)
- `SearchConfig.java` — Configuration: SearchConfig (~424 tok)
- `SearchIndexManager.java` — Component: SearchIndexManager (~4470 tok)
- `SecurityConfig.java` — Configuration: SecurityConfig (~2554 tok)
- `ShippingConfig.java` — Изчислява цената на доставка (~354 tok)
- `SlugRegenerationRunner.java` — Component: SlugRegenerationRunner (~401 tok)
- `SpeedyConfig.java` — Configuration: SpeedyConfig (~136 tok)
- `TbiConfig.java` — AES-256-CTR encryption key provided by TBI for the BIVD merchant account. (~402 tok)
- `TekraConfig.java` — import com.fasterxml.jackson.databind.DeserializationFeature; (~177 tok)
- `WebClientConfig.java` — Configuration: WebClientConfig (~802 tok)
- `WebConfig.java` — Configuration: WebConfig (~515 tok)

## src/main/java/com/techstore/controller/

- `AdminController.java` — Get all orders with pagination and sorting (~5727 tok)
- `AuthController.java` — RestController: AuthController (9 endpoints) (~1192 tok)
- `BlogCategoryController.java` — RestController: BlogCategoryController (6 endpoints) (~750 tok)
- `BlogPostController.java` — RestController: BlogPostController (9 endpoints) (~1487 tok)
- `BlogTagController.java` — RestController: BlogTagController (4 endpoints) (~599 tok)
- `CacheClearController.java` — POST /api/internal/cache/clear-all. @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") + URL-level guard. (~264 tok)
- `CartController.java` — RestController: CartController (6 endpoints) (~2208 tok)
- `CategoryController.java` — RestController: CategoryController (5 endpoints) (~854 tok)
- `CategoryReorganizationController.java` — CategoryReorganizationController (~979 tok)
- `ContactController.java` — RestController: ContactController (2 endpoints) (~331 tok)
- `FileUploadController.java` — RestController: FileUploadController (7 endpoints) (~831 tok)
- `ImageProxyController.java` — RestController: ImageProxyController (3 endpoints) (~1992 tok)
- `ManufacturerController.java` — RestController: ManufacturerController (6 endpoints) (~770 tok)
- `OgMetaController.java` — GET /api/og/product/{id} + /api/og/category/{id}: OG meta HTML за соц. медии ботове. Ползва ProductRepository, CategoryRepository, app.url. Meta refresh за браузъри. (~200 tok)
- `OgMetaController.java` — Returns minimal HTML with Open Graph meta tags for social media crawlers (~1379 tok)
- `OrderController.java` — Създаване на нова поръчка (~1863 tok)
- `ParameterController.java` — RestController: ParameterController (9 endpoints) (~1484 tok)
- `PazaruvajFeedController.java` — Public XML product feed consumed by pazaruvaj.com crawler (every ~12 h). (~1747 tok)
- `PazaruvajFeedController.java` — Public XML product feed consumed by pazaruvaj.com crawler (every ~12 h). (~545 tok)
- `ProductController.java` — Record to hold sort field and direction information (~3884 tok)
- `ProductSearchController.java` — RestController: ProductSearchController (8 endpoints) (~1491 tok)
- `ReviewController.java` — RestController: ReviewController (6 endpoints) (~709 tok)
- `SitemapController.java` — RestController: SitemapController (1 endpoints) (~1292 tok)
- `SpeedyController.java` — RestController: SpeedyController (4 endpoints) (~1716 tok)
- `SubscriptionController.java` — RestController: SubscriptionController (5 endpoints) (~562 tok)
- `TbiLeasingController.java` — POST /api/tbi/register (~846 tok)
- `UserController.java` — RestController: UserController (23 endpoints) (~2406 tok)
- `UserFavoriteController.java` — RestController: UserFavoriteController (7 endpoints) (~2295 tok)

## src/main/java/com/techstore/controller/sync/

- `MostSyncController.java` — Test Most API connection (~2222 tok)
- `TekraSyncController.java` — RestController: TekraSyncController (5 endpoints) (~1230 tok)
- `ValiSyncController.java` — RestController: ValiSyncController (5 endpoints) (~738 tok)

## src/main/java/com/techstore/dto/asbis/

- `AsbisApiInfoDto.java` — Class: AsbisApiInfoDto (~161 tok)
- `AsbisCategoryDto.java` — Class: AsbisCategoryDto (~108 tok)
- `AsbisManufacturerDto.java` — Class: AsbisManufacturerDto (~110 tok)
- `AsbisProductDto.java` — Class: AsbisProductDto (~241 tok)
- `AsbisSyncLogDto.java` — Class: AsbisSyncLogDto (~155 tok)
- `AsbisSyncResultDto.java` — Class: AsbisSyncResultDto (~149 tok)
- `AsbisSyncStats.java` — Class: AsbisSyncStats (~164 tok)
- `AsbisSyncStatusDto.java` — ASBIS SYNC STATUS DTO (~184 tok)

## src/main/java/com/techstore/dto/error/

- `ErrorResponse.java` — Class: ErrorResponse (~381 tok)

## src/main/java/com/techstore/dto/external/

- `CategoryIdDto.java` — Class: CategoryIdDto (~34 tok)
- `DescriptionDto.java` — Class: DescriptionDto (~70 tok)
- `DocumentDto.java` — Class: DocumentDto (~51 tok)
- `FlagDto.java` — Class: FlagDto (~55 tok)
- `ImageDto.java` — Class: ImageDto (~33 tok)
- `NameDto.java` — Class: NameDto (~68 tok)

## src/main/java/com/techstore/dto/filter/

- `AdvancedFilterRequestDTO.java` — AdvancedFilterRequestDTO: hasSpecificationFilters, hasPriceFilter, hasTextSearch (~344 tok)
- `FilterOptionDTO.java` — FilterOptionDTO: isAvailable, getDisplayLabel (~228 tok)
- `PriceRangeDTO.java` — PriceRangeDTO: getDefault, isValid, getFormattedMin, getFormattedMax + 3 more (~648 tok)
- `RangeDTO.java` — RangeDTO: isValid, getFormattedRange, contains (~269 tok)
- `SpecificationFilterValueDTO.java` — SpecificationFilterValueDTO: hasValues, hasTextValue, hasBooleanValue, hasNumericRange + 1 more (~367 tok)

## src/main/java/com/techstore/dto/pazaruvaj/

- `PazaruvajAttributeProjection.java` — Class: PazaruvajAttributeProjection (~45 tok)
- `PazaruvajFeedConfig.java` — ALL | CATEGORY | PRODUCTS (~143 tok)
- `PazaruvajProductProjection.java` — Class: PazaruvajProductProjection (~114 tok)
- `PazaruvajProductProjection.java` — Class: PazaruvajProductProjection (~109 tok)

## src/main/java/com/techstore/dto/request/

- `AcceptOfferRequestDto.java` — Class: AcceptOfferRequestDto (~307 tok)
- `BlogCategoryRequestDto.java` — Class: BlogCategoryRequestDto (~254 tok)
- `BlogPostRequestDto.java` — Class: BlogPostRequestDto (~498 tok)
- `BlogTagRequestDto.java` — Class: BlogTagRequestDto (~175 tok)
- `CartItemRequestDto.java` — Class: CartItemRequestDto (~134 tok)
- `CategoryRequestDto.java` — Class: CategoryRequestDto (~289 tok)
- `CategoryRequestFromExternalDto.java` — Class: CategoryRequestFromExternalDto (~96 tok)
- `ChangePasswordDTO.java` — Class: ChangePasswordDTO (~132 tok)
- `ConvertOfferToOrderRequestDto.java` — Class: ConvertOfferToOrderRequestDto (~310 tok)
- `DocumentRequestDto.java` — Class: DocumentRequestDto (~103 tok)
- `FavoriteRequestDto.java` — Class: FavoriteRequestDto (~72 tok)
- `LoginRequestDTO.java` — Class: LoginRequestDTO (~168 tok)
- `ManufacturerEuRepresentativeDto.java` — Class: ManufacturerEuRepresentativeDto (~55 tok)
- `ManufacturerInformationDto.java` — Class: ManufacturerInformationDto (~54 tok)
- `ManufacturerRequestDto.java` — Class: ManufacturerRequestDto (~102 tok)
- `MarkupUpdateDTO.java` — If true, applies markup only to products that currently have markupPercentage = 0 (~269 tok)
- `MessageToAdmin.java` — Class: MessageToAdmin (~100 tok)
- `OrderCreateRequestDTO.java` — Admin-only: when set, overrides the product's current price (used for offer-to-order conversion). (~1011 tok)
- `OrderFilterDTO.java` — Class: OrderFilterDTO (~232 tok)
- `OrderItemCreateDTO.java` — Class: OrderItemCreateDTO (~116 tok)
- `OrderItemUpdateDTO.java` — 0 = remove the item from the order. (~90 tok)
- `OrderStatusUpdateDTO.java` — Class: OrderStatusUpdateDTO (~122 tok)
- `OrderUpdateRequestDTO.java` — Admin-only DTO for full order editing. (~542 tok)
- `ParameterOptionRequestDto.java` — Class: ParameterOptionRequestDto (~128 tok)
- `ParameterOrderDto.java` — Class: ParameterOrderDto (~152 tok)
- `ParameterRequestDto.java` — Class: ParameterRequestDto (~130 tok)
- `ParameterValueRequestDto.java` — Class: ParameterValueRequestDto (~142 tok)
- `PersonalOfferCreateDto.java` — Class: PersonalOfferCreateDto (~277 tok)
- `ProductCreateRequestDTO.java` — Class: ProductCreateRequestDTO (~759 tok)
- `ProductImageOperationsDTO.java` — Class: ProductImageOperationsDTO (~140 tok)
- `ProductImageUpdateDTO.java` — Class: ProductImageUpdateDTO (~94 tok)
- `ProductMarkupUpdateDTO.java` — Class: ProductMarkupUpdateDTO (~171 tok)
- `ProductParameterCreateDTO.java` — Class: ProductParameterCreateDTO (~103 tok)
- `ProductPromoRequest.java` — Class: ProductPromoRequest (~53 tok)
- `ProductRequestDto.java` — Class: ProductRequestDto (~433 tok)
- `ProductSearchRequest.java` — Class: ProductSearchRequest (~269 tok)
- `ProductSearchRequestDto.java` — Class: ProductSearchRequestDto (~227 tok)
- `ProductUpdateRequestDTO.java` — Class: ProductUpdateRequestDTO (~92 tok)
- `ResetPasswordRequestDTO.java` — Class: ResetPasswordRequestDTO (~185 tok)
- `ReviewRequestDto.java` — Class: ReviewRequestDto (~97 tok)
- `ShippingPreviewRequestDTO.java` — Class: ShippingPreviewRequestDTO (~188 tok)
- `UserAddressDTO.java` — Class: UserAddressDTO (~306 tok)
- `UserCompanyDTO.java` — Class: UserCompanyDTO (~285 tok)
- `UserProfileUpdateDTO.java` — Class: UserProfileUpdateDTO (~130 tok)
- `UserRequestDTO.java` — Class: UserRequestDTO (~318 tok)

## src/main/java/com/techstore/dto/response/

- `BlogCategoryResponseDto.java` — BlogCategoryResponseDto: from (~358 tok)
- `BlogPostResponseDto.java` — BlogPostResponseDto: from (~685 tok)
- `BlogPostSummaryDto.java` — BlogPostSummaryDto: from (~478 tok)
- `BlogTagResponseDto.java` — BlogTagResponseDto: from (~144 tok)
- `CartItemResponseDto.java` — Class: CartItemResponseDto (~143 tok)
- `CartSummaryDto.java` — Class: CartSummaryDto (~75 tok)
- `CategoryResponseDTO.java` — Class: CategoryResponseDTO (~197 tok)
- `CategoryStatisticsDto.java` — Class: CategoryStatisticsDto (~94 tok)
- `CategorySummaryDTO.java` — Class: CategorySummaryDTO (~126 tok)
- `FacetValue.java` — The ID of the parameter option (for filtering) (~210 tok)
- `FavoriteCountResponseDto.java` — Class: FavoriteCountResponseDto (~108 tok)
- `LoginResponseDTO.java` — Class: LoginResponseDTO (~107 tok)
- `ManufacturerResponseDto.java` — Class: ManufacturerResponseDto (~161 tok)
- `ManufacturerSummaryDto.java` — Class: ManufacturerSummaryDto (~93 tok)
- `MarkupResponseDTO.java` — Class: MarkupResponseDTO (~185 tok)
- `OrderItemResponseDTO.java` — OrderItemResponseDTO: getLineTotalWithTax, getTotalPrice (~254 tok)
- `OrderResponseDTO.java` — OrderResponseDTO: getFullCustomerName, getFullShippingAddress (~727 tok)
- `OrderStatisticsResponseDTO.java` — Class: OrderStatisticsResponseDTO (~408 tok)
- `ParameterOptionResponseDto.java` — Class: ParameterOptionResponseDto (~149 tok)
- `ParameterResponseDto.java` — Class: ParameterResponseDto (~135 tok)
- `PersonalOfferResponseDto.java` — PersonalOfferResponseDto: from (~412 tok)
- `ProductDocumentResponseDto.java` — Class: ProductDocumentResponseDto (~54 tok)
- `ProductFlagResponseDto.java` — Class: ProductFlagResponseDto (~60 tok)
- `ProductImageResponseDto.java` — Class: ProductImageResponseDto (~61 tok)
- `ProductImageUploadResponseDTO.java` — Class: ProductImageUploadResponseDTO (~121 tok)
- `ProductParameterResponseDto.java` — Class: ProductParameterResponseDto (~162 tok)
- `ProductResponseDTO.java` — Class: ProductResponseDTO (~400 tok)
- `ProductSearchResponse.java` — Class: ProductSearchResponse (~156 tok)
- `ProductSearchResult.java` — Class: ProductSearchResult (~229 tok)
- `ProductSummaryDto.java` — Class: ProductSummaryDto (~95 tok)
- `ReviewResponseDto.java` — ReviewResponseDto: from (~322 tok)
- `ShippingPreviewResponseDTO.java` — Class: ShippingPreviewResponseDTO (~209 tok)
- `SubscriptionDto.java` — Class: SubscriptionDto (~81 tok)
- `SyncLogResponseDTO.java` — Class: SyncLogResponseDTO (~273 tok)
- `UserAddressResponseDTO.java` — Class: UserAddressResponseDTO (~130 tok)
- `UserCompanyResponseDTO.java` — Class: UserCompanyResponseDTO (~131 tok)
- `UserFavoriteResponseDto.java` — Class: UserFavoriteResponseDto (~68 tok)
- `UserResponseDTO.java` — Class: UserResponseDTO (~313 tok)

## src/main/java/com/techstore/dto/speedy/

- `SpeedyAddressRequest.java` — Class: SpeedyAddressRequest (~46 tok)
- `SpeedyAuthenticationRequest.java` — Class: SpeedyAuthenticationRequest (~48 tok)
- `SpeedyAuthenticationResponse.java` — Class: SpeedyAuthenticationResponse (~48 tok)
- `SpeedyCalculatePriceRequest.java` — Class: SpeedyCalculatePriceRequest (~524 tok)
- `SpeedyCalculatePriceResponse.java` — Class: SpeedyCalculatePriceResponse (~348 tok)
- `SpeedyError.java` — Class: SpeedyError (~55 tok)
- `SpeedyErrorResponse.java` — Class: SpeedyErrorResponse (~47 tok)
- `SpeedyOffice.java` — Class: SpeedyOffice (~779 tok)
- `SpeedyOfficeResponse.java` — Class: SpeedyOfficeResponse (~56 tok)
- `SpeedySite.java` — Class: SpeedySite (~167 tok)
- `SpeedySiteResponse.java` — Class: SpeedySiteResponse (~54 tok)

## src/main/java/com/techstore/dto/tbi/

- `LeasingApplicationResponseDto.java` — Admin-facing representation of a TBI leasing application. (~886 tok)
- `LeasingApplicationResponseDto.java` — Admin-facing leasing application DTO with static from() factory (~180 tok)
- `LeasingApplicationStatisticsDto.java` — Aggregated statistics shown on the admin leasing dashboard. (~214 tok)
- `LeasingApplicationStatisticsDto.java` — Aggregated stats: total, approved, rejected, signed, amounts (~100 tok)
- `LeasingOrderCreateRequestDto.java` — Class: LeasingOrderCreateRequestDto (~266 tok)
- `TbiApplicationDataDto.java` — The plain-text JSON object that gets AES-256-CTR encrypted and placed (~413 tok)
- `TbiApplicationDataDto.java` — Plain-text JSON encrypted and sent as `data` to TBI RegisterApplication (~120 tok)
- `TbiCartItemRequestDto.java` — A single cart item sent from the frontend when initiating a leasing application (~143 tok)
- `TbiDeliveryAddressDto.java` — Delivery address within the TBI application payload. (~295 tok)
- `TbiDeliveryAddressDto.java` — Delivery address for TBI payload; static empty() builder (~100 tok)
- `TbiItemDto.java` — A single item in the TBI RegisterApplication items list. (~214 tok)
- `TbiItemDto.java` — Single item in TBI items list: name, qty, price, sku, category, imagelink (~90 tok)
- `TbiRegisterApplicationResponseDto.java` — Response from TBI POST /api/RegisterApplication. (~259 tok)
- `TbiRegisterApplicationResponseDto.java` — TBI RegisterApplication response: error, order_id, token, url (~90 tok)
- `TbiRegisterRequestDto.java` — Sent by the frontend when the customer clicks "Buy with TBI". (~382 tok)
- `TbiRegisterRequestDto.java` — From frontend: productId, productName, priceEuro, priceLeva, sku, quantity, imageUrl (~80 tok)
- `TbiRegisterResponseDto.java` — Returned to the frontend after a successful RegisterApplication call. (~124 tok)
- `TbiRegisterResponseDto.java` — Returned to frontend: applicationId + TBI iframe url (~50 tok)
- `TbiStatusWebhookDto.java` — Webhook payload POSTed by TBI to our statusURL when an application changes state. (~550 tok)
- `TbiStatusWebhookDto.java` — TBI webhook payload on status change: CreditApplicationId, OrderId, Message, Status (~120 tok)

## src/main/java/com/techstore/dto/tekra/

- `TekraApiResponse.java` — TekraApiResponse: isSuccess (~193 tok)
- `TekraCategory.java` — Class: TekraCategory (~333 tok)
- `TekraManufacturer.java` — Class: TekraManufacturer (~194 tok)
- `TekraParameter.java` — Class: TekraParameter (~218 tok)
- `TekraParameterOption.java` — Class: TekraParameterOption (~180 tok)
- `TekraProduct.java` — Class: TekraProduct (~560 tok)
- `TekraProductAttribute.java` — Class: TekraProductAttribute (~149 tok)
- `TekraProductVariant.java` — Class: TekraProductVariant (~216 tok)
- `TekraSyncRequest.java` — Class: TekraSyncRequest (~233 tok)
- `TekraSyncResponse.java` — Class: TekraSyncResponse (~185 tok)

## src/main/java/com/techstore/entity/

- `BaseEntity.java` — Entity: BaseEntity (~378 tok)
- `BlogCategory.java` — Entity: BlogCategory (~271 tok)
- `BlogPost.java` — Entity: BlogPost (~511 tok)
- `BlogTag.java` — Entity: BlogTag (~99 tok)
- `CartItem.java` — Entity: CartItem (~242 tok)
- `Category.java` — Entity: Category with aliasOf (ManyToOne self-ref, alias_of_id). Alias categories proxy products from target. (~900 tok)
- `LeasingApplication.java` — order_id returned by TBI RegisterApplication (~1252 tok)
- `LeasingApplication.java` — Entity: TBI leasing application; links to Order, stores TBI order_id/token, status, status_history (JSON), financial details (~400 tok)
- `Manufacturer.java` — Entity: Manufacturer (~476 tok)
- `Order.java` — Entity: Order (~1750 tok)
- `OrderItem.java` — Entity: OrderItem (~559 tok)
- `Parameter.java` — Entity: Parameter (~544 tok)
- `ParameterOption.java` — Entity: ParameterOption (~260 tok)
- `PersonalOffer.java` — Entity: PersonalOffer (~665 tok)
- `Product.java` — Entity: Product (~2886 tok)
- `ProductFlag.java` — Entity: ProductFlag (~231 tok)
- `ProductParameter.java` — Entity: ProductParameter (~266 tok)
- `Review.java` — Entity: Review (~219 tok)
- `Subscription.java` — Entity: Subscription (~120 tok)
- `SyncLog.java` — Entity: SyncLog (~337 tok)
- `User.java` — Entity: User (~965 tok)
- `UserAddress.java` — Entity: UserAddress (~283 tok)
- `UserCompany.java` — Entity: UserCompany (~273 tok)
- `UserFavorite.java` — Entity: UserFavorite (~211 tok)

## src/main/java/com/techstore/enums/

- `BlogPostStatus.java` — Class: BlogPostStatus (~27 tok)
- `OrderStatus.java` — Class: OrderStatus (~54 tok)
- `PaymentMethod.java` — PaymentMethod: getDisplayName (~133 tok)
- `PaymentStatus.java` — Class: PaymentStatus (~37 tok)
- `Platform.java` — Platform/Source of data integration (~339 tok)
- `ProductStatus.java` — ProductStatus: fromCode, getCode, getNameBg, getNameEn (~281 tok)
- `ReviewStatus.java` — Class: ReviewStatus (~27 tok)
- `ShippingMethod.java` — Class: ShippingMethod (~31 tok)

## src/main/java/com/techstore/event/

- `OnPasswordChangedEvent.java` — Class: OnPasswordChangedEvent (~100 tok)
- `OnPasswordResetRequestEvent.java` — Class: OnPasswordResetRequestEvent (~99 tok)
- `OrderCreatedEvent.java` — Event fired when a new order is created (~117 tok)
- `OrderStatusChangedEvent.java` — Event fired when order status is changed (~195 tok)

## src/main/java/com/techstore/exception/

- `AccountNotActivatedException.java` — Class: AccountNotActivatedException (~56 tok)
- `BusinessLogicException.java` — Class: BusinessLogicException (~52 tok)
- `DuplicateResourceException.java` — Class: DuplicateResourceException (~54 tok)
- `ExternalApiException.java` — ExternalApiException: getStatusCode, getResponseBody (~209 tok)
- `GlobalExceptionHandler.java` — RestController: GlobalExceptionHandler (~5811 tok)
- `InsufficientStockException.java` — Class: InsufficientStockException (~54 tok)
- `InvalidCredentialsException.java` — Class: InvalidCredentialsException (~55 tok)
- `InvalidTokenException.java` — Class: InvalidTokenException (~52 tok)
- `ResourceNotFoundException.java` — Class: ResourceNotFoundException (~54 tok)
- `SyncException.java` — Class: SyncException (~75 tok)
- `TokenExpiredException.java` — Class: TokenExpiredException (~52 tok)
- `UnauthorizedException.java` — Class: UnauthorizedException (~51 tok)
- `ValidationError.java` — Class: ValidationError (~83 tok)
- `ValidationException.java` — Class: ValidationException (~80 tok)

## src/main/java/com/techstore/listener/

- `OrderEventListener.java` — Event listener for order-related events (~700 tok)
- `PasswordChangedEventListener.java` — Event listener for password changed events (~467 tok)
- `PasswordResetEventListener.java` — Event listener for password reset requests (~515 tok)

## src/main/java/com/techstore/mapper/

- `ManufacturerMapper.java` — Class: ManufacturerMapper (~88 tok)
- `ParameterMapper.java` — Get primary category name (first category from the set) (~1154 tok)

## src/main/java/com/techstore/repository/

- `BlogCategoryRepository.java` — Repository: BlogCategoryRepository (~167 tok)
- `BlogPostRepository.java` — Repository: BlogPostRepository (~705 tok)
- `BlogTagRepository.java` — Repository: BlogTagRepository (~270 tok)
- `CartItemRepository.java` — Find users with abandoned carts: (~641 tok)
- `CategoryRepository.java` — Repository: CategoryRepository (~316 tok)
- `LeasingApplicationRepository.java` — Repository: LeasingApplicationRepository (~376 tok)
- `ManufacturerRepository.java` — Repository: ManufacturerRepository (~209 tok)
- `OrderItemRepository.java` — Repository: OrderItemRepository (~87 tok)
- `OrderRepository.java` — Repository: OrderRepository (~1414 tok)
- `ParameterOptionRepository.java` — Repository: ParameterOptionRepository (~420 tok)
- `ParameterRepository.java` — Repository: ParameterRepository (~1143 tok)
- `PersonalOfferRepository.java` — Class: PersonalOfferRepository (~408 tok)
- `ProductParameterRepository.java` — Repository: ProductParameterRepository (~478 tok)
- `ProductRepository.java` — Cross-platform deduplication by SKU. (~3297 tok)
- `ProductSearchRepository.java` — Repository: ProductSearchRepository (~9378 tok)
- `ReviewRepository.java` — Repository: ReviewRepository (~302 tok)
- `SubscriptionRepository.java` — Repository: SubscriptionRepository (~118 tok)
- `SyncLogRepository.java` — Repository: SyncLogRepository (~214 tok)
- `UserAddressRepository.java` — Repository: UserAddressRepository (~261 tok)
- `UserCompanyRepository.java` — Repository: UserCompanyRepository (~261 tok)
- `UserFavoriteRepository.java` — Repository: UserFavoriteRepository (~284 tok)
- `UserRepository.java` — Repository: UserRepository (~252 tok)

## src/main/java/com/techstore/service/

- `AbandonedCartService.java` — Service: AbandonedCartService (~624 tok)
- `AsbisApiService.java` — Extract categories from Asbis products (~8797 tok)
- `AuthService.java` — Service: AuthService (~6388 tok)
- `BlogCategoryService.java` — Service: BlogCategoryService (~1917 tok)
- `BlogPostService.java` — Service: BlogPostService (~3776 tok)
- `BlogTagService.java` — Service: BlogTagService (~1138 tok)
- `CartService.java` — Service: CartService (~1567 tok)
- `CategoryReorganizationService.java` — CategoryReorganizationService - FINAL VERSION (~13424 tok)
- `CategoryService.java` — Service: CategoryService (~4425 tok)
- `CronJobService.java` — Service: CronJobService (~788 tok)
- `EmailService.java` — Service for sending email notifications (~5028 tok)
- `FileUploadService.java` — Service: FileUploadService (~7260 tok)
- `ImageMigrationService.java` — Service: ImageMigrationService (~1434 tok)
- `ManufacturerService.java` — Service: ManufacturerService (~4027 tok)
- `MostApiService.java` — Test API connectivity (~3823 tok)
- `OrderService.java` — Creates a new order (~7410 tok)
- `ParameterService.java` — Service: ParameterService (~9601 tok)
- `PazaruvajFeedService.java` — Thread-safe holder for the pre-generated XML feed. (~3484 tok)
- `PazaruvajFeedService.java` — Thread-safe holder for the pre-generated XML feed. (~1916 tok)
- `PersonalOfferService.java` — Service: PersonalOfferService (~3744 tok)
- `ProductSearchService.java` — Service: alias-aware search. resolveAliasId() + resolveAliasCategories() applied in searchProducts, getAvailableParametersWithCountsForCategory, getFilteredFacets. (~2242 tok)
- `ProductService.java` — Service: ProductService (~13412 tok)
- `ReviewService.java` — Service: ReviewService (~946 tok)
- `S3Service.java` — Downloads an image from a remote URL and uploads it to S3. (~3045 tok)
- `SpeedyService.java` — Взема населени места по име (~2459 tok)
- `SubscriptionService.java` — Service: SubscriptionService (~464 tok)
- `TbiLeasingService.java` — Initiates a TBI leasing application for a product-page "Buy with TBI" click. (~8519 tok)
- `TbiLeasingService.java` — TBI Fusion Pay integration: registerApplication (AES encrypt → TBI API), processStatusWebhook, getStatistics, admin queries (~350 tok)
- `TekraApiService.java` — Get categories using JSON parsing (categories return JSON) (~4698 tok)
- `UserFavoriteService.java` — Service: UserFavoriteService (~3795 tok)
- `UserService.java` — Service: UserService (~6466 tok)
- `ValiApiService.java` — Get categories (no pagination available) (~9746 tok)

## src/main/java/com/techstore/service/admin/

- `AdminService.java` — Service: AdminService (~4391 tok)

## src/main/java/com/techstore/service/sync/

- `AsbisSyncService.java` — AsbisSyncService (~14027 tok)
- `MostSyncService.java` — MostSyncService - COMPLETELY REWRITTEN VERSION 3.0 (~14623 tok)
- `TekraSyncService.java` — Service: TekraSyncService (~23065 tok)
- `ValiSyncService.java` — ValiSyncService - VERSION 4.3 - FINAL FIX (~14124 tok)

## src/main/java/com/techstore/util/

- `ExceptionHelper.java` — Wraps database operations and converts common exceptions to business exceptions (~1811 tok)
- `FilterUtils.java` — FilterUtils: calculatePriceRange, calculateNumericRange, createFilterOptions (~861 tok)
- `JwtUtil.java` — Component: JwtUtil (~1188 tok)
- `LogHelper.java` — Service: LogHelper (~398 tok)
- `SecurityHelper.java` — Component: SecurityHelper (~640 tok)
- `SlugUtils.java` — SlugUtils: generateSlug (~624 tok)
- `SyncHelper.java` — Find category by hierarchical path (e.g., "zahranvaniya-i-baterii/za-postoyanno-naprezhenie") (~2631 tok)
- `TbiEncryptionUtil.java` — TBI Fusion Pay encryption utility. (~1015 tok)

## src/main/resources/

- `.DS_Store` (~1640 tok)
- `application.yml` (~2652 tok)
- `logback-spring.xml` (~264 tok)

## src/main/resources/db/

- `.DS_Store` (~1640 tok)

## src/main/resources/db/migration/

- `V1__initial_schema.sql` — V1__initial_schema.sql (~6089 tok)
- `V10__add_audit_columns_to_user_tables.sql` — Add missing BaseEntity audit columns to user_addresses (~137 tok)
- `V11__add_seat_management_address_to_user_companies.sql` — SQL: 1 alter(s) (~39 tok)
- `V12__add_tbi_leasing_applications.sql` — V12: TBI Fusion Pay leasing applications (~714 tok)
- `V16__add_shipping_to_leasing_applications.sql` — Add shipping address fields to leasing_applications table (~190 tok)
- `V19__make_order_shipping_nullable.sql` — Allow shipping address fields to be null for leasing orders (~89 tok)
- `V2__add_isFilter_to_product_parameters.sql` — SQL: 1 alter(s) (~48 tok)
- `V20__add_pending_order_json_to_leasing.sql` — Stores the serialised OrderCreateRequestDTO so the order can be created (~64 tok)
- `V21__add_personal_offers.sql` — SQL: tables: personal_offers (~191 tok)
- `V22__fix_personal_offers_audit_columns.sql` — Fix audit column name: BaseEntity uses last_modified_by, not updated_by (~40 tok)
- `V23__add_abandoned_cart_reminder_to_users.sql` (~20 tok)
- `V24__add_product_reviews.sql` — SQL: tables: product_reviews (~219 tok)
- `V25__review_status_to_varchar.sql` (~73 tok)
- `V26__add_shipping_details_to_personal_offers.sql` (~22 tok)
- `V27__add_blog_posts.sql` — SQL: tables: blog_posts (~174 tok)
- `V28__drop_redundant_blog_slug_index.sql` — The UNIQUE constraint on blog_posts.slug already creates an implicit B-tree index. (~61 tok)
- `V29__add_blog_categories.sql` — SQL: tables: blog_categories (~168 tok)
- `V3__add_most_key_to_product.sql` — SQL: 1 alter(s) (~42 tok)
- `V30__add_blog_tags.sql` — SQL: tables: blog_tags, blog_post_tags (~165 tok)
- `V31__upgrade_blog_posts.sql` — Add new columns (~267 tok)
- `V32__add_alias_to_categories.sql` — V32: Add alias_of_id to categories to support "virtual" categories that (~134 tok)
- `V33__fix_sync_parameters_isfilter.sql` — Phase 1a: Remove junk MOST parameters created before the properties-map fix. (~521 tok)
- `V4__add_filter_order_to_parameter.sql` — SQL: 1 alter(s) (~48 tok)
- `V5__update_fts_combined_index.sql` — V5__update_fts_combined_index.sql (~209 tok)
- `V6__add_isfilter_to_category_parameters.sql` — V6: Add per-category is_filter flag to category_parameters junction table (~198 tok)
- `V7__set_category_parameter_filters_by_usage.sql` — V7: Industry-standard per-category filter configuration (~3966 tok)
- `V8__add_user_addresses_and_companies.sql` — User delivery addresses (~354 tok)
- `V9__add_speedy_fields_to_user_addresses.sql` — SQL: 1 alter(s) (~30 tok)

## src/main/resources/templates/email/

- `abandoned-cart.html` — Забравихте нещо в количката (~1715 tok)
- `admin-new-order.html` — Нова поръчка (~1787 tok)
- `leasing-rejected.html` — Заявка за лизинг (~3009 tok)
- `message-to-admin.html` (~142 tok)
- `order-cancelled.html` — Поръчката е отменена (~3845 tok)
- `order-confirmation.html` — Потвърждение на поръчка (~4507 tok)
- `order-delivered.html` — Поръчката е доставена (~4032 tok)
- `order-shipped.html` — Поръчката е изпратена (~3849 tok)
- `order-status-update.html` — Промяна в статуса на поръчка (~2598 tok)
- `password-changed-confirmation.html` — Парола променена (~2113 tok)
- `password-reset-email.html` — Нулиране на парола (~2270 tok)
- `personal-offer.html` — Лична оферта (~1925 tok)

## src/test/java/com/techstore/

- `TechStoreApiApplicationTests.java` — @SpringBootTest @ActiveProfiles("test") context-load smoke test. Uses H2 + application-test.properties. (~78 tok)

## src/test/java/com/techstore/controller/

- `TbiLeasingControllerTest.java` — @WebMvcTest security tests: POST /register (401/200), POST /webhook (public 200), GET /application/{id} IDOR (401/200 owner/403 non-owner/200 admin/200 SUPER_ADMIN). 8 tests. (~2081 tok)

## src/test/java/com/techstore/service/

- `TbiLeasingServiceTest.java` — Pure Mockito unit tests: ResellerCode validation, resolveApplication lookup order, Approval (ContractSigned/approved&signed), Rejection (Rejected/Canceled/rejected), EdgeCases. 15 tests. (~3912 tok)

## src/test/resources/

- `application-test.properties` — ── Datasource — H2 in-memory (replaces PostgreSQL for context-load tests) ── (~580 tok)
- `application.properties` — Overrides for ALL tests — resolves placeholders that have no default in application.yml (~40 tok)
