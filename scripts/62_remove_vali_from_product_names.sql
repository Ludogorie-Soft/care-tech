-- ============================================================================
-- 62_remove_vali_from_product_names.sql
--
-- Маха думата „VALI" от имената (bg/en) и slug-овете на собствените продукти на VALI:
-- 14 настолни компютъра VALI OFFICE / VALI GAMING и „Хартиени пликчета за CD … комплект VALI".
--   „Настолен компютър VALI OFFICE BASIC" → „Настолен компютър OFFICE BASIC"
--   pc-desktop-vali-office-basic          → pc-desktop-office-basic
-- Плюс slug-овете на 8 скрити тениски (8655–8662), чието име вече е „CARETECH COMPUTERS",
-- но адресът е останал t-shirt-vali-computers-… — името им не се пипа.
--
-- ЧИСТ SQL — без psql meta-команди, работи и в GUI клиент.
--
-- ПУСКА СЕ СЛЕД ДЕПЛОЯ на бекенда с ValiSyncService.removeValiBrand(). Нощният VALI sync
--   записва имената наново всяка нощ — със стария код би върнал „VALI" още същата нощ.
--   Slug-а sync-ът не го пипа (генерира се само за нов продукт).
--
-- Само цялата дума с главни букви — „Lavalier", „Validated" не се засягат.
-- Старите адреси продължават да работят: продуктът се зарежда по ID-то в адреса.
--
-- ЕДНА КОМАНДА: един DO блок + COMMIT накрая (DBeaver в Manual commit).
-- Откат в края — точните стари стойности. Дата: 2026-10-02
-- ============================================================================

SET lock_timeout = '10s';
SET idle_in_transaction_session_timeout = '60s';

DO $$
DECLARE
    v_clashes INT;
    v_updated INT;
BEGIN
    -- Новият slug не бива да съвпада с този на друг продукт
    SELECT count(*) INTO v_clashes
      FROM products p
      JOIN products o
        ON o.id <> p.id
       AND o.slug = regexp_replace(regexp_replace(p.slug, '-vali(-|$)', '\1', 'g'), '^vali-', '')
     WHERE p.name_bg ~ '\mVALI\M' OR p.name_en ~ '\mVALI\M' OR p.slug ~ '(^|-)vali(-|$)';
    IF v_clashes > 0 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % от новите slug-ове вече са заети от други продукти.', v_clashes;
    END IF;

    UPDATE products
       SET name_bg = CASE WHEN name_bg ~ '\mVALI\M'
                          THEN btrim(regexp_replace(regexp_replace(name_bg, '\s*\mVALI\M', '', 'g'), '\s{2,}', ' ', 'g'))
                          ELSE name_bg END,
           name_en = CASE WHEN name_en ~ '\mVALI\M'
                          THEN btrim(regexp_replace(regexp_replace(name_en, '\s*\mVALI\M', '', 'g'), '\s{2,}', ' ', 'g'))
                          ELSE name_en END,
           slug = regexp_replace(regexp_replace(slug, '-vali(-|$)', '\1', 'g'), '^vali-', ''),
           updated_at = NOW()
     WHERE name_bg ~ '\mVALI\M' OR name_en ~ '\mVALI\M' OR slug ~ '(^|-)vali(-|$)';
    GET DIAGNOSTICS v_updated = ROW_COUNT;

    -- На 2026-10-02 са 23. Много повече значи, че данните са се променили — провери ги първо.
    IF v_updated > 30 THEN
        RAISE EXCEPTION 'ПРЕКЪСНАТО: % продукта, очаквани около 23. Нищо не е променено.', v_updated;
    END IF;

    RAISE NOTICE '✓ „VALI" е махнато от % продукта.', v_updated;
END $$;

COMMIT;

-- Проверка: и двете заявки трябва да върнат 0.
SELECT count(*) AS still_with_vali FROM products WHERE name_bg ~ '\mVALI\M' OR name_en ~ '\mVALI\M';
SELECT count(*) AS slugs_with_vali FROM products WHERE slug ~ '(^|-)vali(-|$)';

-- ============================================================================
-- ОТКАТ (точните стойности преди скрипта)
--   UPDATE products SET name_bg = 'Настолен компютър VALI GAMING AMD Ryzen 5 GEFORCE RTX 3050 8GB', name_en = '', slug = 'nastolen-kompyutar-vali-vr-ready-amd-7700-32gb-ddr5-rtx-5070-12gb-1tb-ssd-nvme', updated_at = NOW() WHERE id = 2538;
--   UPDATE products SET name_bg = 'Настолен компютър VALI OFFICE BASIC', name_en = 'PC Desktop VALI OFFICE BASIC', slug = 'pc-desktop-vali-office-basic', updated_at = NOW() WHERE id = 2543;
--   UPDATE products SET name_bg = 'Настолен компютър VALI OFFICE PREMIUM', name_en = 'PC Desktop VALI OFFICE PREMIUM', slug = 'pc-desktop-vali-office-premium', updated_at = NOW() WHERE id = 2544;
--   UPDATE products SET name_bg = 'Настолен компютър VALI OFFICE PRO', name_en = 'PC Desktop VALI OFFICE PRO', slug = 'pc-desktop-vali-office-pro', updated_at = NOW() WHERE id = 2545;
--   UPDATE products SET name_bg = 'Настолен компютър VALI OFFICE PREMIUM PLUS', name_en = 'PC Desktop VALI OFFICE PREMIUM PLUS', slug = 'pc-desktop-vali-office-premium-plus', updated_at = NOW() WHERE id = 2546;
--   UPDATE products SET name_bg = 'Настолен компютър VALI OFFICE PRO PLUS', name_en = 'PC Desktop VALI OFFICE PRO PLUS', slug = 'pc-desktop-vali-office-pro-plus', updated_at = NOW() WHERE id = 2547;
--   UPDATE products SET name_bg = 'Настолен компютър VALI GAMING AMD RYZEN 5 7500F RТX5060', name_en = 'PC Desktop  VALI GAMING AMD RYZEN 5 7500F RТX5060', slug = 'pc-desktop-vali-gaming-amd-ryzen-5-9600-rtx5060', updated_at = NOW() WHERE id = 2548;
--   UPDATE products SET name_bg = 'Настолен компютър VALI GAMING AMD RYZEN 5 7500F RX9060XT', name_en = 'PC Desktop VALI GAMING AMD RYZEN 5 7500F RX9060XT', slug = 'pc-desktop-vali-gaming-amd-ryzen-5-9500f-rx9060xt', updated_at = NOW() WHERE id = 2549;
--   UPDATE products SET name_bg = 'Настолен компютър VALI GAMING INTEL CORE I5 14400F GEFORCE RTX5060', name_en = 'PC Desktop  VALI GAMING INTEL CORE I5 14400F GEFORCE RTX5060', slug = 'pc-desktop-vali-gaming-intel-core-i5-14400f-geforce-rtx5060', updated_at = NOW() WHERE id = 2550;
--   UPDATE products SET name_bg = 'Настолен компютър VALI GAMING AMD RYZEN 7 7800X3D GEFORCE RTX5070', name_en = 'PC Desktop  VALI GAMING AMD RYZEN 7 7800X3D GEFORCE RTX5070', slug = 'pc-desktop-vali-gaming-amd-ryzen-7-9700x-geforce-rtx5070', updated_at = NOW() WHERE id = 2551;
--   UPDATE products SET name_bg = 'Настолен компютър VALI OFFICE PREMIUM PLUS i7', name_en = 'PC Desktop VALI OFFICE PREMIUM PLUS i7', slug = 'pc-desktop-vali-office-premium-plus-i7', updated_at = NOW() WHERE id = 2554;
--   UPDATE products SET name_bg = 'Настолен компютър VALI OFFICE GT', name_en = 'PC Desktop VALI OFFICE GT', slug = 'pc-desktop-vali-office-gt', updated_at = NOW() WHERE id = 2559;
--   UPDATE products SET name_bg = 'Тениска CARETECH COMPUTERS Gents, размер L, Бяла', name_en = 'T-shirt CARETECH COMPUTERS Gents, L', slug = 't-shirt-vali-computers-gents-l', updated_at = NOW() WHERE id = 8655;
--   UPDATE products SET name_bg = 'Тениска CARETECH COMPUTERS Gents, размер XL, Бяла', name_en = 'T-shirt CARETECH COMPUTERS Gents, XL', slug = 't-shirt-vali-computers-gents-xl', updated_at = NOW() WHERE id = 8656;
--   UPDATE products SET name_bg = 'Тениска CARETECH COMPUTERS Unisex, размер XS, Зелена', name_en = 'T-shirt CARETECH COMPUTERS Unisex, XS', slug = 't-shirt-vali-computers-unisex-xs', updated_at = NOW() WHERE id = 8657;
--   UPDATE products SET name_bg = 'Тениска CARETECH COMPUTERS Unisex, размер S, Зелена', name_en = 'T-shirt CARETECH COMPUTERS Unisex, S', slug = 't-shirt-vali-computers-unisex-s', updated_at = NOW() WHERE id = 8658;
--   UPDATE products SET name_bg = 'Тениска CARETECH COMPUTERS Unisex, размер M, Зелена', name_en = 'T-shirt CARETECH COMPUTERS Unisex, M', slug = 't-shirt-vali-computers-unisex-m', updated_at = NOW() WHERE id = 8659;
--   UPDATE products SET name_bg = 'Тениска CARETECH COMPUTERS Unisex, размер L, Зелена', name_en = 'T-shirt CARETECH COMPUTERS Unisex, L', slug = 't-shirt-vali-computers-unisex-l', updated_at = NOW() WHERE id = 8660;
--   UPDATE products SET name_bg = 'Тениска CARETECH COMPUTERS Unisex, размер XL, Зелена', name_en = 'T-shirt CARETECH COMPUTERS Unisex, XL', slug = 't-shirt-vali-computers-unisex-xl', updated_at = NOW() WHERE id = 8661;
--   UPDATE products SET name_bg = 'Тениска CARETECH COMPUTERS Unisex, размер 2XL, Зелена', name_en = 'T-shirt CARETECH COMPUTERS Unisex, 2XL', slug = 't-shirt-vali-computers-unisex-2xl', updated_at = NOW() WHERE id = 8662;
--   UPDATE products SET name_bg = 'Хартиени пликчета за CD 50 бр. комплект VALI, Бял', name_en = 'CD/DVD Protective Paper Sleeves, pack of 100 VALI, white', slug = 'cddvd-protective-paper-sleeves-pack-of-100-vali-white', updated_at = NOW() WHERE id = 9312;
--   UPDATE products SET name_bg = 'Настолен компютър VALI GAMING INTEL CORE I5 14400F GEFORCE RTX5060TI', name_en = 'PC Desktop  VALI GAMING INTEL CORE I5 14400F GEFORCE RTX5060TI', slug = 'pc-desktop-vali-gaming-intel-core-i5-14400f-geforce-rtx5060ti', updated_at = NOW() WHERE id = 30350;
--   UPDATE products SET name_bg = 'Настолен компютър VALI OFFICE BASIC AMD', name_en = 'PC Desktop VALI OFFICE BASIC AMD', slug = 'pc-desktop-vali-office-basic-amd', updated_at = NOW() WHERE id = 36979;
--   COMMIT;
-- ============================================================================
