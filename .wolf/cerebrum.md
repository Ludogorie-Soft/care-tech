# Cerebrum

> OpenWolf's learning memory. Updated automatically as the AI learns from interactions.
> Do not edit manually unless correcting an error.
> Last updated: 2026-05-15

## User Preferences

<!-- How the user likes things done. Code style, tools, patterns, communication. -->

## Key Learnings

- **Project:** tech-store-api
- **Description:** A comprehensive Spring Boot REST API for a tech store, featuring complete product management, admin panel, and advanced e-commerce functionality.
- **Vali.bg category page URL format:** `https://www.vali.bg/bg/category/{id}` — the site is a Vue.js SPA; categories have no slug in the API, only numeric `id`. Homepage hrefs are not useful for scraping (only ~20 static links). All 260 categories are accessible via this pattern.
- **Vali.bg API categories endpoint:** `GET /api/v1/categories` returns a flat list of 260 objects with fields: `id`, `parent`, `show`, `order`, `name` (array of `{language_code, text}`), `description`. No slug field.
- **Vali.bg filter params extraction:** Filter parameter IDs appear as `name="opt[{id}][]"` in input elements on the catalog page. Use regex `name=["']opt\[(\d+)\]` to extract them.
- **Vali.bg categories with no filters:** 54 of 260 categories returned 0 filter params (these are parent/container categories without product listings).

## Do-Not-Repeat

<!-- Mistakes made and corrected. Each entry prevents the same mistake recurring. -->
<!-- Format: [YYYY-MM-DD] Description of what went wrong and what to do instead. -->

- [2026-05-18] Do NOT put data-dependent SQL in Flyway migrations (db/migration/). Scripts that require pre-existing synced data (e.g. setting filters after Vali sync) belong in `scripts/`. Flyway runs on empty DB at startup — data isn't there yet.
- [2026-05-18] PostgreSQL `UPDATE ... FROM` cannot reference the update target table inside a JOIN in the FROM clause. Use comma-separated tables and move the join condition to WHERE: `FROM p, c WHERE cp.category_id = c.id` instead of `FROM p JOIN c ON c.id = cp.category_id`.

## Decision Log

<!-- Significant technical decisions with rationale. Why X was chosen over Y. -->

- [2026-05-18] V13 (set Vali is_filter from scraped data) moved to `scripts/` folder. Reason: on a fresh server the DB is empty at startup — Flyway would run V13 before any Vali sync data exists, so it would match 0 rows. The script must be run manually after `POST /api/sync/vali/parameters`.
