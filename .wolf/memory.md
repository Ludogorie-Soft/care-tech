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
| 2026-05-19 | Created 8_asbis_filters.sql — auto-selects Asbis is_filter=true where parameter_options count BETWEEN 2 AND 50 with blacklist of packaging/global params; fixed bug: option_count does not exist on category_parameters, replaced with subquery COUNT(*) from parameter_options | scripts/8_asbis_filters.sql | done | ~200 |
