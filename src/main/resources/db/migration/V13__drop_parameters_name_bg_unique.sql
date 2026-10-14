-- V13: Drop the incorrectly applied unique constraint on parameters.name_bg
--
-- The constraint "uq_parameters_name_bg" was added manually to the database.
-- It is wrong: multiple platforms (Vali, Most, Tekra, Asbis) can legitimately
-- have parameters with the same Bulgarian name (e.g. "Кеш памет" in both Vali
-- and Most). Deduplication is done by external_id + platform, not by name.
--
-- IF EXISTS handles fresh databases where the constraint was never added.

ALTER TABLE parameters
    DROP CONSTRAINT IF EXISTS uq_parameters_name_bg;
