-- =============================================================================
-- Скрипт за пренареждане на главните категории (sort_order)
-- Изпълни СЛЕД rename_categories.sql
-- Изпълни ръчно: psql -U <user> -d <db> -f reorder_top_categories.sql
-- НЕ е Flyway миграция — може да се изпълнява многократно (UPDATE е идемпотентен)
-- =============================================================================

BEGIN;

UPDATE categories SET sort_order = 1,  updated_at = NOW() WHERE external_id = 549;          -- КОМПЮТРИ
UPDATE categories SET sort_order = 2,  updated_at = NOW()
  WHERE name_bg = 'Компютърни компоненти' AND parent_id IS NULL;
UPDATE categories SET sort_order = 3,  updated_at = NOW()
  WHERE name_bg ILIKE 'Лаптопи%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 4,  updated_at = NOW() WHERE external_id = 394;           -- Монитори
UPDATE categories SET sort_order = 5,  updated_at = NOW()
  WHERE name_bg ILIKE 'Компютърна периферия%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 6,  updated_at = NOW()
  WHERE name_bg ILIKE 'Геймърска периферия%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 7,  updated_at = NOW()
  WHERE name_bg ILIKE 'Принтери%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 8,  updated_at = NOW() WHERE external_id = 412;           -- Рутери и мрежово оборудване
UPDATE categories SET sort_order = 9,  updated_at = NOW()
  WHERE name_bg ILIKE 'Видеонаблюдение%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 10, updated_at = NOW()
  WHERE name_bg ILIKE 'Непрекъсваеми%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 11, updated_at = NOW()
  WHERE name_bg ILIKE 'Сторидж%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 12, updated_at = NOW()
  WHERE name_bg ILIKE 'Батерии%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 13, updated_at = NOW()
  WHERE name_bg = 'Кабели' AND parent_id IS NULL;
UPDATE categories SET sort_order = 14, updated_at = NOW()
  WHERE name_bg ILIKE 'Фото%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 15, updated_at = NOW()
  WHERE name_bg ILIKE 'TV%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 16, updated_at = NOW()
  WHERE name_bg ILIKE 'Електроника%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 17, updated_at = NOW() WHERE external_id = 469;           -- Навигации, камери и аксесоари за коли
UPDATE categories SET sort_order = 18, updated_at = NOW() WHERE external_id = 459;           -- Смарт часовници, телефони и аксесоари
UPDATE categories SET sort_order = 19, updated_at = NOW()
  WHERE name_bg ILIKE 'Проектори%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 20, updated_at = NOW()
  WHERE name_bg ILIKE 'Софтуер%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 21, updated_at = NOW()
  WHERE name_bg ILIKE 'Водно охлаждане%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 22, updated_at = NOW()
  WHERE name_bg ILIKE 'VR%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 23, updated_at = NOW()
  WHERE name_bg ILIKE 'Офис продукти%' AND parent_id IS NULL;
UPDATE categories SET sort_order = 24, updated_at = NOW() WHERE external_id = 491;           -- Инструменти и аксесоари (Продукти и аксесоари)
UPDATE categories SET sort_order = 25, updated_at = NOW()
  WHERE name_bg = 'STEM' AND parent_id IS NULL;
UPDATE categories SET sort_order = 26, updated_at = NOW() WHERE external_id = 708;           -- СЕРВИЗНИ УСЛУГИ

-- =============================================================================
-- Проверка на резултата
-- =============================================================================
SELECT id, external_id, name_bg, sort_order
FROM categories
WHERE parent_id IS NULL
ORDER BY sort_order, name_bg;

COMMIT;
