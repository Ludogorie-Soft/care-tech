-- V41: More static IGNORE / HIDE rules, found after curating 83 categories (2026-09-24).
--
-- Same shape as V39: keyed by normalized parameter name only. A curated rule scoped to a category
-- (scripts/55_*) still wins over these: "Съвместими модели" feeds the brand filter of phone and tablet
-- cases, "Продукт" the software type.
--   HIDE   — ASBIS service fields that repeat the product name or list other products, and the grams of
--            recycled plastic per packaging part ("ABS 920: 62.92г Recycle").
--   IGNORE — shown in specifications, useless as a filter: package contents, compatibility lists, series.
-- Deliberately absent: "марка" (the franchise of figures — Fortnite, Formula 1) and "категория на
-- продукта" (the tool type — pliers, cable tester).

INSERT INTO filter_attribute_sources (action, name_norm, origin, note)
SELECT 'HIDE', filter_norm(n), 'MANUAL', 'V41 service fields / packaging'
FROM (VALUES
    ('ново име на аксесоар'), ('съкратено описание'), ('свързани продукти(1)'),
    ('abs 920'), ('abs pa757f'), ('pom fm090'), ('tpe'), ('tpu')
) AS v(n)
ON CONFLICT DO NOTHING;

INSERT INTO filter_attribute_sources (action, name_norm, origin, note)
SELECT 'IGNORE', filter_norm(n), 'MANUAL', 'V41 specification only'
FROM (VALUES
    ('съдържание'), ('съдържание на опаковката'), ('включени аксесоари'), ('set includes'),
    ('съвместими модели'), ('съвместим модел'), ('съвместими принтери'),
    ('бранд'), ('продавач'), ('серия'), ('валидност на продукта'), ('разположение на захранването')
) AS v(n)
ON CONFLICT DO NOTHING;
