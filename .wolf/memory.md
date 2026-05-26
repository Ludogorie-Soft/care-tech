# Memory

> Chronological action log. Hooks and AI append to this file automatically.
> Old sessions are consolidated by the daemon weekly.

| 2026-05-19 | Created reorganize_asbis_categories.sql — dissolves 43 Asbis roots into Vali tree, 3 new roots kept (Роботизирани решения, Дигитална сигнализация IDS, Мултиборд→Офис) | scripts/reorganize_asbis_categories.sql | created |

| 10:00 | Fixed MostSyncService: extractMostParameters now reads properties map; generateMostKey now handles Cyrillic | MostSyncService.java | done | ~800 |
| 10:01 | Phase 1B SQL: DELETE FROM parameters WHERE platform = 'MOST' (all 16 junk entries, cascades to options/products) | N/A | pending user run | ~100 |
| 14:22 | Scraped Vali.bg filter params for all 260 categories via /bg/category/{id} URLs; 206 categories mapped, 815 total filter refs, 543 unique param IDs | /tmp/vali_filters.json | done | ~1200 |
| 10:00 | Replaced V13 migration with scraped Vali filter data (206 cats, 815 pairs); added normalizeOptionValue() + per-parameter dedup in createOptionsForParameter() | V13__vali_filters_by_option_count.sql, ValiSyncService.java | done | ~800 |
| 10:16 | Moved V13 to scripts/ — data-dependent scripts don't belong in Flyway migrations | scripts/V13__vali_filters_by_option_count.sql | done | ~50 |
| 09:28 | ValiSyncService: added paramNamesByCategory guard + cross-platform allParamsByNameBg adoption | ValiSyncService.java | done | ~400 |
| 09:28 | AsbisSyncService: syncAsbisProducts category lookup replaced with parent-aware maps (rootByName + childByParentAndName) | AsbisSyncService.java | done | ~200 |
| 2026-05-19 | Fixed PriceAvail parser: getElementsByTagName Product→PRICE, fields ProductCode→WIC, Price→MY_PRICE, Stock→AVAIL text ("да"=1/other=0); added RETAIL_PRICE+WARRANTYTERM | AsbisApiService.java | done | ~200 |
| 2026-05-19 | Вариант A: reorganize_asbis_categories.sql updated — "Дребни домакински уреди" kept as visible Asbis root; all household items (Грижа за здравето, Едра домакинска техника, LED осветление, accessories, consumer robots, items from "Други") re-routed to it instead of external_id=491 | scripts/reorganize_asbis_categories.sql | done | ~300 |
| 2026-05-20 | Fixed TekraSyncService: added bulk INSERT into category_parameters at end of syncTekraProducts() — populates missing junction entries for subcategories without tekraSlug (IP Камери, Аналогови камери, NVR/DVR Рекордери) | TekraSyncService.java | done | ~200 |
| 2026-05-21 | Fixed search returning 0 results / NPE: @Builder.Default not applied by Jackson @NoArgsConstructor → language=null, sortBy=null, size=0. Added null/zero guards in validateSearchRequest() | ProductSearchService.java | done | ~200 |
| 2026-05-21 | Fixed Asbis products all hidden: RestTemplate decoded XML as ISO-8859-1, AVAIL='да' became 'Ð´Ð°', inStock always false. Changed getForObject to byte[].class + new String(bytes, UTF_8) in both getRawProductListXML() and getRawPriceAvailXML() | AsbisApiService.java | done | ~300 |
| 2026-05-21 | Added GET /api/og/category/{id} endpoint — OGPreviewController now handles category OG previews (title, logo, redirect) | OGPreviewController.java | done | ~150 |
| 2026-05-21 | Fixed ValiSyncService session poisoning: entityManager.flush() was outside try-catch, failure prevented entityManager.clear() → all subsequent chunks failed. Wrapped in try/finally | ValiSyncService.java | done | ~100 |
| 2026-05-21 | Fixed GlobalExceptionHandler: NoResourceFoundException (Spring Boot 3) not handled → fell to catch-all → 500. Added specific handler returning 404 | GlobalExceptionHandler.java | done | ~100 |
| 2026-05-21 | Fixed ImageProxyController: Broken pipe / AsyncRequestNotUsableException logged as error. Now logged as warn (client disconnect, not server error) | ImageProxyController.java | done | ~100 |
| 2026-05-21 | Set root logging to WARN globally: root: WARN, com.techstore: WARN in application.yml | application.yml | done | ~50 |
| 2026-05-21 | Added Microsoft Clarity script (id: wu2u7fe49v) to frontend index.html | care-tech-ui/public/index.html | done | ~50 |
| 2026-05-19 | Created 8_asbis_filters.sql — auto-selects Asbis is_filter=true where parameter_options count BETWEEN 2 AND 50 with blacklist of packaging/global params; fixed bug: option_count does not exist on category_parameters, replaced with subquery COUNT(*) from parameter_options | scripts/8_asbis_filters.sql | done | ~200 |

## Session: 2026-05-22 08:31

| Time | Action | File(s) | Outcome | ~Tokens |
|------|--------|---------|---------|--------|
| 00:07 | Fixed Tekra UnexpectedRollbackException: setTekraParametersToProductSimplified() replaced Hibernate collection with orphanRemoval → transaction rollback-only. Changed to in-place mutation (clear+addAll) | TekraSyncService.java | done | ~400 |
| 2026-05-25 | Most sync rewrite: new URL (portal.mostbg.com), XML parser (PartNumber/pictureUrl/inStock), field mappings, name_en cross-match, orphanRemoval fix, new MOST_CATEGORY_MAPPING | MostApiService.java, MostSyncService.java, application.yml | done | ~1200 |
| 08:41 | Session end: 2 writes across 1 files (TekraSyncService.java) | 2 reads | ~21109 tok |
| 08:43 | Session end: 2 writes across 1 files (TekraSyncService.java) | 2 reads | ~21109 tok |
| 08:46 | Session end: 2 writes across 1 files (TekraSyncService.java) | 2 reads | ~21860 tok |
| 08:47 | Session end: 2 writes across 1 files (TekraSyncService.java) | 3 reads | ~22197 tok |
| 08:48 | Session end: 2 writes across 1 files (TekraSyncService.java) | 3 reads | ~22197 tok |
| 08:49 | Session end: 2 writes across 1 files (TekraSyncService.java) | 3 reads | ~22197 tok |
| 08:51 | Created ../../../../.claude/projects/-Users-user-Documents-projects-cp-tech-store-api/memory/project_most_api.md | — | ~145 |
| 08:51 | Edited ../../../../.claude/projects/-Users-user-Documents-projects-cp-tech-store-api/memory/MEMORY.md | 1→2 lines | ~68 |
| 08:51 | Session end: 4 writes across 3 files (TekraSyncService.java, project_most_api.md, MEMORY.md) | 4 reads | ~22426 tok |
| 08:52 | Session end: 4 writes across 3 files (TekraSyncService.java, project_most_api.md, MEMORY.md) | 4 reads | ~22426 tok |
| 09:02 | Session end: 4 writes across 3 files (TekraSyncService.java, project_most_api.md, MEMORY.md) | 4 reads | ~22426 tok |
| 14:04 | Session end: 4 writes across 3 files (TekraSyncService.java, project_most_api.md, MEMORY.md) | 6 reads | ~37852 tok |
| 14:18 | Session end: 4 writes across 3 files (TekraSyncService.java, project_most_api.md, MEMORY.md) | 9 reads | ~37852 tok |
| 14:21 | Session end: 4 writes across 3 files (TekraSyncService.java, project_most_api.md, MEMORY.md) | 9 reads | ~37852 tok |
| 14:25 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | modified if() | ~179 |
| 14:25 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | setProductParameters() → clear() | ~53 |
| 14:25 | Edited src/main/resources/application.yml | 4→4 lines | ~28 |
| 14:26 | Edited src/main/java/com/techstore/service/MostApiService.java | added 1 import(s) | ~34 |
| 14:26 | Edited src/main/java/com/techstore/service/MostApiService.java | isEmpty() → String() | ~140 |
| 14:27 | Edited src/main/java/com/techstore/service/MostApiService.java | modified extractProductFromElement() | ~769 |
| 14:27 | Edited src/main/java/com/techstore/service/MostApiService.java | modified extractGallery() | ~246 |
| 14:28 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | reduced (-67 lines) | ~511 |
| 14:28 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | 2→2 lines | ~38 |
| 14:28 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | setModel() → SuppressWarnings() | ~205 |
| 14:28 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | convertToInt() → equals() | ~33 |
| 14:28 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | added 1 condition(s) | ~255 |
| 14:28 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | added 2 condition(s) | ~382 |
| 14:29 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | — | ~0 |
| 14:29 | Session end: 18 writes across 6 files (TekraSyncService.java, project_most_api.md, MEMORY.md, MostSyncService.java, application.yml) | 10 reads | ~43547 tok |
| 14:31 | Edited src/main/java/com/techstore/service/CronJobService.java | modified catch() | ~150 |

## Session: 2026-05-25 14:34

| Time | Action | File(s) | Outcome | ~Tokens |
|------|--------|---------|---------|--------|
| 14:58 | Edited src/main/resources/application.yml | 2→2 lines | ~14 |
| 14:58 | Session end: 1 writes across 1 files (application.yml) | 2 reads | ~13876 tok |
| 14:59 | Session end: 1 writes across 1 files (application.yml) | 4 reads | ~14624 tok |
| 15:00 | Session end: 1 writes across 1 files (application.yml) | 4 reads | ~14624 tok |
| 15:08 | Session end: 1 writes across 1 files (application.yml) | 4 reads | ~14624 tok |
| 15:12 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | added 4 condition(s) | ~1447 |
| 15:13 | Session end: 2 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~16174 tok |
| 15:13 | Session end: 2 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~16174 tok |
| 15:22 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | modified if() | ~299 |
| 15:22 | Session end: 3 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~16494 tok |
| 15:39 | Session end: 3 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~16494 tok |
| 15:42 | Session end: 3 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~16494 tok |
| 15:43 | Session end: 3 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~16494 tok |
| 15:48 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | added 3 import(s) | ~179 |
| 15:49 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | 9→13 lines | ~145 |
| 15:50 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | modified syncMostProducts() | ~3252 |
| 15:51 | Session end: 6 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~20712 tok |
| 16:00 | Session end: 6 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~20712 tok |
| 16:00 | Session end: 6 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~20712 tok |
| 16:02 | Session end: 6 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~20712 tok |
| 16:04 | Session end: 6 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~20712 tok |
| 16:06 | Session end: 6 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~20712 tok |
| 16:06 | Session end: 6 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~20712 tok |
| 16:07 | Session end: 6 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~20712 tok |
| 16:08 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | expanded (+12 lines) | ~261 |
| 16:08 | Session end: 7 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~19686 tok |
| 16:16 | Session end: 7 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~19686 tok |
| 16:17 | Session end: 7 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~19686 tok |
| 16:18 | Edited src/main/java/com/techstore/service/sync/MostSyncService.java | modified for() | ~599 |
| 16:19 | Session end: 8 writes across 2 files (application.yml, MostSyncService.java) | 4 reads | ~20504 tok |
| 16:24 | Edited src/main/java/com/techstore/service/sync/TekraSyncService.java | added 3 import(s) | ~97 |
| 16:24 | Edited src/main/java/com/techstore/service/sync/TekraSyncService.java | 1→5 lines | ~27 |
| 16:25 | Edited src/main/java/com/techstore/service/sync/TekraSyncService.java | toList() → transactions() | ~407 |
| 16:26 | Edited src/main/java/com/techstore/service/sync/TekraSyncService.java | modified for() | ~734 |
| 16:27 | Edited src/main/java/com/techstore/service/sync/TekraSyncService.java | added error handling | ~1554 |
| 16:27 | Session end: 13 writes across 3 files (application.yml, MostSyncService.java, TekraSyncService.java) | 5 reads | ~44500 tok |
| 16:29 | Session end: 13 writes across 3 files (application.yml, MostSyncService.java, TekraSyncService.java) | 5 reads | ~44500 tok |

## Session: 2026-05-25 16:34

| Time | Action | File(s) | Outcome | ~Tokens |
|------|--------|---------|---------|--------|

## Session: 2026-05-25 16:30

| Time | Action | File(s) | Outcome | ~Tokens |
|------|--------|---------|---------|--------|
| 16:35 | Created scripts/13_translate_parameter_names.sql — 884 parameter name_bg translations (MOST:91, TEKRA:11, VALI:10, ASBIS:772) from 1964-row CSV; skips material codes, voltage rails, interface version strings | scripts/13_translate_parameter_names.sql | done | ~8000 |
| 16:54 | Edited src/main/java/com/techstore/service/TekraApiService.java | added 1 import(s) | ~47 |
| 16:54 | Edited src/main/java/com/techstore/service/TekraApiService.java | added 1 condition(s) | ~683 |
| 16:55 | Edited src/main/java/com/techstore/service/sync/TekraSyncService.java | modified for() | ~296 |
| 16:50 | Fixed Tekra 429 rate limit: added retry with exponential backoff (10s/20s/40s) in TekraApiService.getProductsRaw(); added 2s delay between category fetches in TekraSyncService | TekraApiService.java, TekraSyncService.java | done | ~400 |
| 16:55 | Session end: 3 writes across 2 files (TekraApiService.java, TekraSyncService.java) | 3 reads | ~28311 tok |
| 17:03 | Edited scripts/12_deduplicate_products_by_sku.sql | 6→7 lines | ~65 |
| 17:03 | Session end: 4 writes across 3 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql) | 4 reads | ~28381 tok |
| 17:04 | Session end: 4 writes across 3 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql) | 4 reads | ~28381 tok |
| 17:04 | Session end: 4 writes across 3 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql) | 4 reads | ~28381 tok |
| 17:08 | Edited src/main/java/com/techstore/service/TekraApiService.java | modified catch() | ~268 |
| 17:08 | Edited src/main/java/com/techstore/service/sync/TekraSyncService.java | added 1 import(s) | ~60 |
| 17:08 | Edited src/main/java/com/techstore/service/sync/TekraSyncService.java | modified for() | ~404 |
| 17:09 | Session end: 7 writes across 3 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql) | 4 reads | ~29159 tok |
| 17:09 | Session end: 7 writes across 3 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql) | 4 reads | ~29159 tok |
| 17:17 | Session end: 7 writes across 3 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql) | 6 reads | ~43699 tok |
| 17:18 | Session end: 7 writes across 3 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql) | 6 reads | ~43699 tok |
| 17:20 | Session end: 7 writes across 3 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql) | 6 reads | ~43699 tok |
| 10:19 | Session end: 7 writes across 3 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql) | 10 reads | ~64968 tok |
| 11:06 | Edited src/main/resources/application.yml | inline fix | ~26 |
| 11:07 | Edited src/main/java/com/techstore/service/FileUploadService.java | modified validateFileContent() | ~358 |
| 11:08 | Edited src/main/java/com/techstore/service/EmailService.java | modified if() | ~155 |
| 11:08 | Session end: 10 writes across 6 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql, application.yml, FileUploadService.java) | 11 reads | ~67975 tok |
| 11:09 | Session end: 10 writes across 6 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql, application.yml, FileUploadService.java) | 11 reads | ~67975 tok |
| 11:17 | Session end: 10 writes across 6 files (TekraApiService.java, TekraSyncService.java, 12_deduplicate_products_by_sku.sql, application.yml, FileUploadService.java) | 24 reads | ~67975 tok |
| 11:23 | Edited src/main/java/com/techstore/controller/OrderController.java | added 4 import(s) | ~374 |
| 11:24 | Edited src/main/java/com/techstore/controller/OrderController.java | 2→3 lines | ~40 |
| 11:25 | Edited src/main/java/com/techstore/controller/OrderController.java | added 3 condition(s) | ~344 |
| 11:30 | Edited src/main/java/com/techstore/service/FileUploadService.java | added 2 import(s) | ~80 |
| 11:30 | Edited src/main/java/com/techstore/service/FileUploadService.java | added error handling | ~185 |

## Session: 2026-05-26 11:32

| Time | Action | File(s) | Outcome | ~Tokens |
|------|--------|---------|---------|--------|
| 11:39 | Edited ../../care-tech-ui/src/pages/profile/ProfileLayout.jsx | 3→3 lines | ~52 |
| 11:39 | Edited ../../care-tech-ui/src/pages/profile/ProfileLayout.jsx | 6→11 lines | ~64 |
| 11:39 | Edited ../../care-tech-ui/src/pages/profile/OrderDetails.jsx | added 1 import(s) | ~86 |
| 11:39 | Edited ../../care-tech-ui/src/pages/profile/OrderDetails.jsx | added error handling | ~218 |
| 11:39 | Edited ../../care-tech-ui/src/pages/profile/OrderDetails.jsx | CSS: hover | ~266 |
| 11:39 | Created ../../care-tech-ui/src/pages/profile/WarrantyCards.jsx | — | ~886 |
| 11:39 | Edited ../../care-tech-ui/src/App.js | added 1 import(s) | ~34 |
| 11:40 | Edited ../../care-tech-ui/src/App.js | 2→3 lines | ~47 |
| 11:40 | Session end: 8 writes across 4 files (ProfileLayout.jsx, OrderDetails.jsx, WarrantyCards.jsx, App.js) | 5 reads | ~1653 tok |
| 11:41 | Session end: 8 writes across 4 files (ProfileLayout.jsx, OrderDetails.jsx, WarrantyCards.jsx, App.js) | 6 reads | ~8905 tok |
| 11:46 | Edited src/main/java/com/techstore/service/S3Service.java | added 1 import(s) | ~57 |
| 11:46 | Edited src/main/java/com/techstore/service/S3Service.java | added error handling | ~756 |
| 11:46 | Edited src/main/java/com/techstore/service/OrderService.java | 2→3 lines | ~38 |
| 11:47 | Edited src/main/java/com/techstore/service/OrderService.java | modified uploadWarrantyFile() | ~328 |
| 11:48 | Edited src/main/java/com/techstore/service/EmailService.java | 21→20 lines | ~220 |
| 11:48 | Edited src/main/java/com/techstore/service/EmailService.java | 5→6 lines | ~56 |
| 11:49 | Edited src/main/java/com/techstore/service/EmailService.java | added error handling | ~127 |
| 11:50 | Edited src/main/java/com/techstore/service/EmailService.java | 4→1 lines | ~12 |
| 11:50 | Edited src/main/java/com/techstore/controller/OrderController.java | 28→25 lines | ~327 |
| 11:50 | Edited src/main/java/com/techstore/controller/OrderController.java | 3→3 lines | ~36 |
| 11:52 | Edited src/main/java/com/techstore/controller/OrderController.java | modified downloadWarrantyFile() | ~428 |
| 11:52 | Session end: 19 writes across 8 files (ProfileLayout.jsx, OrderDetails.jsx, WarrantyCards.jsx, App.js, S3Service.java) | 8 reads | ~18709 tok |
| 11:53 | Session end: 19 writes across 8 files (ProfileLayout.jsx, OrderDetails.jsx, WarrantyCards.jsx, App.js, S3Service.java) | 8 reads | ~18709 tok |
| 12:01 | Session end: 19 writes across 8 files (ProfileLayout.jsx, OrderDetails.jsx, WarrantyCards.jsx, App.js, S3Service.java) | 8 reads | ~18709 tok |
| 12:47 | Session end: 19 writes across 8 files (ProfileLayout.jsx, OrderDetails.jsx, WarrantyCards.jsx, App.js, S3Service.java) | 8 reads | ~18709 tok |
| 12:48 | Session end: 19 writes across 8 files (ProfileLayout.jsx, OrderDetails.jsx, WarrantyCards.jsx, App.js, S3Service.java) | 8 reads | ~18709 tok |
