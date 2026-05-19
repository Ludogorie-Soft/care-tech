# Memory

> Chronological action log. Hooks and AI append to this file automatically.
> Old sessions are consolidated by the daemon weekly.

| 10:00 | Fixed MostSyncService: extractMostParameters now reads properties map; generateMostKey now handles Cyrillic | MostSyncService.java | done | ~800 |
| 10:01 | Phase 1B SQL: DELETE FROM parameters WHERE platform = 'MOST' (all 16 junk entries, cascades to options/products) | N/A | pending user run | ~100 |
| 14:22 | Scraped Vali.bg filter params for all 260 categories via /bg/category/{id} URLs; 206 categories mapped, 815 total filter refs, 543 unique param IDs | /tmp/vali_filters.json | done | ~1200 |
| 10:00 | Replaced V13 migration with scraped Vali filter data (206 cats, 815 pairs); added normalizeOptionValue() + per-parameter dedup in createOptionsForParameter() | V13__vali_filters_by_option_count.sql, ValiSyncService.java | done | ~800 |
| 10:16 | Moved V13 to scripts/ — data-dependent scripts don't belong in Flyway migrations | scripts/V13__vali_filters_by_option_count.sql | done | ~50 |
